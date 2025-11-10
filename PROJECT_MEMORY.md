# Project Memory: NFC Walker Patrol System
_Last updated: 2025-11-11_

This file is for the assistant's fast recall. Keep it **concise**, **actionable**, and **current**. Avoid marketing fluff or duplication of the README. Update when architecture, invariants, or active tasks change.

---
## 1. Invariants (Stable Facts)
- Language / Runtime: Kotlin (1.9.25), Java 21 (mandatory)
- Framework: Micronaut 4.x (HTTP server + DI + security)
- Persistence: PostgreSQL + Hibernate (Micronaut Data JPA) + Flyway migrations (V1, V2 existing)
- **ID Strategy: UUID (all entities use `GenerationType.UUID` with `gen_random_uuid()` default in DB)**
- Security: JWT authentication with role-based access control
- Anti-replay: Challenge-response mechanism for NFC scanning

---
## 2. Domain Model & Hierarchy

```
Organization (Организация - охранная компания)
    └── Site (Площадка/Объект - конкретное место охраны: склад, офис, территория)
        ├── Checkpoint (Контрольная точка - физическая NFC метка)
        └── PatrolRoute (Маршрут патрулирования - набор точек в порядке обхода)
            └── PatrolRouteCheckpoint (Связь точки с маршрутом + временные ограничения)
                └── PatrolRun (Запуск патрулирования - конкретный обход)
                    └── PatrolScanEvent (Событие сканирования NFC метки охранником)
```

### Key Entities

1. **Organization** - организация (верхний уровень иерархии)
2. **Site** - охраняемый объект/площадка (принадлежит организации)
   - `siteId` = UUID конкретного объекта
3. **Checkpoint** - контрольная точка с NFC меткой
   - Имеет уникальный `code` (NFC/QR)
   - Опционально: GPS координаты (`geoLat`, `geoLon`, `radiusM`)
4. **PatrolRoute** - маршрут (принадлежит site)
5. **PatrolRouteCheckpoint** - точка в маршруте
   - `seq` - порядковый номер
   - `minOffsetSec`, `maxOffsetSec` - временные ограничения между точками
6. **PatrolRun** - запуск патрулирования (создается при первом scan)
7. **PatrolScanEvent** - факт сканирования (время, GPS, userId)
8. **ChallengeUsed** - использованные challenge (защита от replay-атак)

---
## 3. Role-Based Access Control (RBAC)

### Роли и их назначение

**ROLE_APP_OWNER** - владелец приложения / суперадмин
- Управление организациями (CRUD)
- Полный доступ ко всем данным
- Контроллер: `OrganizationController` (`/api/organizations`)

**ROLE_BOSS** - менеджер организации / диспетчер
- Управление объектами (sites) в своей организации
- Создание/редактирование checkpoints и routes
- Просмотр отчетов по патрулированию
- Контроллеры: 
  - `SiteController` (`/api/sites`)
  - `AdminController` (`/api/admin`)
  - `ScanController` (`/api/scan`) - может сканировать

**ROLE_WORKER** - охранник / патрульный
- Только сканирование NFC меток
- Выполнение патрулирования
- Контроллер: `ScanController` (`/api/scan`)

### API Endpoints по ролям

```
ROLE_APP_OWNER only:
  POST   /api/organizations              - создать организацию
  GET    /api/organizations              - список всех организаций
  GET    /api/organizations/{id}         - получить организацию
  PUT    /api/organizations/{id}         - обновить организацию
  DELETE /api/organizations/{id}         - удалить организацию

ROLE_BOSS:
  POST   /api/sites                      - создать объект
  GET    /api/sites?organizationId=UUID  - список объектов организации
  GET    /api/sites/{id}                 - получить объект
  PUT    /api/sites/{id}                 - обновить объект
  DELETE /api/sites/{id}                 - удалить объект
  
  POST   /api/admin/checkpoints          - создать контрольную точку
  GET    /api/admin/checkpoints?siteId=UUID - список точек на объекте
  
  POST   /api/admin/routes               - создать маршрут
  POST   /api/admin/routes/{id}/points   - добавить точки в маршрут

ROLE_WORKER + ROLE_BOSS:
  POST   /api/scan/start                 - начать сканирование (получить challenge)
  POST   /api/scan/finish                - завершить сканирование
```

---
## 4. Security & Anti-Replay Protection

### JWT Authentication
- Токены содержат `subject` (userId) и `roles`
- Роли проверяются через `@Secured` аннотации на контроллерах

### Challenge-Response для сканирования
1. Клиент сканирует NFC → `POST /api/scan/start` с `checkpointCode`
2. Сервер генерирует уникальный `challenge` (UUID)
3. Сервер возвращает `challenge` + `policy` (правила сканирования)
4. Клиент подтверждает → `POST /api/scan/finish` с `challenge` + данными
5. Сервер проверяет:
   - Challenge не использован ранее (таблица `challenge_used`)
   - Временные ограничения соблюдены
   - Геолокация корректна (если задана)
6. Сервер сохраняет событие в `patrol_scan_events`
7. Challenge помечается как использованный → запись в `challenge_used`

**Защита:** нельзя переиспользовать challenge, нельзя подделать сканирование

---
## 5. Controllers

| Controller | Path | Role | Purpose |
|------------|------|------|---------|
| OrganizationController | /api/organizations | APP_OWNER | CRUD организаций |
| SiteController | /api/sites | BOSS | CRUD площадок |
| AdminController | /api/admin | BOSS | CRUD checkpoints & routes |
| ScanController | /api/scan | WORKER, BOSS | Сканирование NFC |

---
## 6. Процесс патрулирования

1. Охранник начинает обход маршрута
2. Подходит к контрольной точке, сканирует NFC метку
3. **START**: `POST /api/scan/start` с кодом метки
   - Сервер находит checkpoint по коду
   - Определяет маршрут и текущий PatrolRun
   - Генерирует challenge
   - Возвращает challenge + policy (временные окна, GPS ограничения)
4. **FINISH**: `POST /api/scan/finish` с challenge + userId + timestamp + GPS
   - Проверка challenge (не использован)
   - Проверка времени (в пределах временного окна)
   - Проверка GPS (если задана)
   - Сохранение события сканирования
   - Пометка challenge как использованный
5. Охранник переходит к следующей точке маршрута

---
## 7. Database Schema (PostgreSQL)

Tables:
- `organizations` - организации
- `sites` - объекты/площадки
- `checkpoints` - контрольные точки (NFC метки)
- `patrol_routes` - маршруты
- `patrol_route_checkpoints` - связь маршрут-точка (M:N + доп.данные)
- `patrol_runs` - запуски патрулирования
- `patrol_scan_events` - события сканирования
- `challenge_used` - использованные challenge (anti-replay)

All IDs: UUID with `gen_random_uuid()` default

Migrations:
- V1__core.sql - основные таблицы
- V2__challenge_used.sql - таблица защиты от replay

---
## 8. Tech Stack

- **Kotlin** 1.9.25
- **Micronaut** 4.x
- **PostgreSQL** (with UUIDs)
- **Hibernate / Micronaut Data JPA**
- **Flyway** migrations
- **JWT** authentication
- **Gradle** build tool

---
## 9. Testing

### Test Structure
- Unit tests: спецификации в `src/test/kotlin/ge/tiger8bit/spec/`
- Test framework: Kotest (StringSpec style)
- Test fixtures: `TestFixtures.kt` - helper methods для создания тестовых данных
- Test auth: `TestAuth.kt` - генерация JWT токенов для разных ролей

### Authorization Testing Strategy
**Минималистичный подход:** для каждой неправильной роли проверяем доступ только к ОДНОМУ эндпоинту контроллера, чтобы убедиться что `@Secured` работает.

### Test Helpers
```kotlin
TestAuth.generateAppOwnerToken()  // для APP_OWNER
TestAuth.generateBossToken()      // для BOSS
TestAuth.generateWorkerToken()    // для WORKER
TestAuth.generateToken(subject, roles)  // кастомная генерация

TestFixtures.seedOrgAndSite(...)  // создает Organization + Site
TestFixtures.createRoute(...)     // создает PatrolRoute
```

### Test Coverage
- ✅ OrganizationSpec - CRUD organizations (APP_OWNER only)
- ✅ SiteSpec - CRUD sites (BOSS only)
- ✅ CheckpointSpec - create/list checkpoints (BOSS only)
- ✅ RouteSpec - create routes, add checkpoints (BOSS only)
- ✅ ScanFlowSpec - полный flow сканирования
- ✅ ReplaySpec - защита от replay-атак
- ✅ HealthSpec - health check endpoint

Подробнее: `docs/TEST_COVERAGE.md`

---
## 10. Current State (2025-11-11)

### ✅ Implemented
- Core domain model (Organization → Site → Checkpoint → Route → Run → Event)
- JWT authentication with roles
- Challenge-response anti-replay protection
- Controllers:
  - ✅ ScanController (WORKER, BOSS)
  - ✅ AdminController (BOSS) - checkpoints, routes
  - ✅ SiteController (BOSS) - sites management
  - ✅ OrganizationController (APP_OWNER) - organizations management
- DTOs for all entities
- Database migrations (V1, V2)
- Repositories (JPA)

### 📋 Design Decisions
- **siteId** - это UUID конкретного охраняемого объекта (площадки)
- **ROLE_APP_OWNER** управляет организациями
- **ROLE_BOSS** управляет всем внутри организации (sites, checkpoints, routes)
- **ROLE_WORKER** только сканирует
- Все entity ID - UUID для distributed systems

### 🔄 Next Steps (if needed)
- [ ] Добавить тесты для новых контроллеров (SiteController, OrganizationController)
- [ ] Реализовать фильтрацию данных по organizationId для BOSS (чтобы не видел чужие данные)
- [ ] Добавить отчеты по патрулированию
- [ ] Websockets для real-time мониторинга (опционально)

---
## 11. Code Conventions

- Логирование: используем `getLogger()` extension из `LoggerExt.kt`
- Транзакции: `@Transactional` на методах, изменяющих данные
- Response helpers: extension функции `toResponse()` внутри контроллеров
- DTOs: все в `dto/Dtos.kt` с аннотацией `@Serdeable`
- Репозитории: интерфейсы в `repository/`, наследуют `JpaRepository<Entity, UUID>`

