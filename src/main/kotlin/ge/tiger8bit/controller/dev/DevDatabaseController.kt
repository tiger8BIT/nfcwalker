package ge.tiger8bit.controller.dev

import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

/**
 * Dev-only endpoint for database cleanup during testing.
 * WARNING: This endpoint deletes all data except the System Root organization!
 */
@Controller("/api/dev/database")
@Requires(env = ["local"])
@Secured(SecurityRule.IS_ANONYMOUS)
open class DevDatabaseController(
    private val entityManager: EntityManager
) {
    private val logger = LoggerFactory.getLogger(DevDatabaseController::class.java)

    @Delete("/reset")
    @Transactional
    open fun resetDatabase(): HttpResponse<Map<String, String>> {
        logger.warn("⚠️  DEV: Resetting database - deleting all test data!")

        try {
            // Order matters due to foreign key constraints
            entityManager.createNativeQuery("DELETE FROM attachments").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM incidents").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM challenge_used").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM patrol_sub_check_events").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM patrol_scan_events").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM patrol_runs").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM patrol_route_checkpoints").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM patrol_routes").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM checkpoint_sub_checks").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM checkpoints").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM devices").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM sites").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM invitations").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM user_roles WHERE organization_id != '00000000-0000-0000-0000-000000000000'")
                .executeUpdate()
            entityManager.createNativeQuery("DELETE FROM organizations WHERE id != '00000000-0000-0000-0000-000000000000'").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM users WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_roles.user_id = users.id AND user_roles.organization_id = '00000000-0000-0000-0000-000000000000')")
                .executeUpdate()

            logger.info("✅ Database reset completed successfully")
            return HttpResponse.ok(
                mapOf(
                    "status" to "success",
                    "message" to "Database reset completed. All test data deleted (System Root preserved)."
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to reset database", e)
            return HttpResponse.serverError(
                mapOf(
                    "status" to "error",
                    "message" to "Failed to reset database: ${e.message}"
                )
            )
        }
    }
}
