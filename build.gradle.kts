import io.micronaut.gradle.testresources.StartTestResourcesService

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.6.1"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.test-resources") version "4.6.1"
    id("io.micronaut.aot") version "4.6.1"
}

version = "0.1"
group = "ge.tiger8bit"

val kotlinVersion = project.properties.get("kotlinVersion")
val reactorVersion = project.properties.get("reactorVersion")
val logstashEncoderVersion = project.properties.get("logstashEncoderVersion")
repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.data:micronaut-data-processor")
    ksp("io.micronaut.openapi:micronaut-openapi")
    ksp("io.micronaut.security:micronaut-security-annotations")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut.aws:micronaut-aws-sdk-v2")
    implementation("software.amazon.awssdk:s3:2.29.43")
    implementation("com.google.cloud:google-cloud-storage:2.48.2")

    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut.openapi:micronaut-openapi")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut.aws:micronaut-function-aws-api-proxy")
    implementation("io.micronaut.gcp:micronaut-gcp-function-http")
    implementation("com.google.cloud.functions:functions-framework-api:1.1.0")
    implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
    implementation("io.micronaut.data:micronaut-data-tx-hibernate")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.sql:micronaut-hibernate-jpa")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("io.micronaut.tracing:micronaut-tracing-opentelemetry")
    implementation("io.projectreactor:reactor-core:${reactorVersion}")
    implementation("io.micronaut.security:micronaut-security-oauth2")
    implementation("io.micronaut.email:micronaut-email-javamail")
    runtimeOnly("org.eclipse.angus:angus-mail")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:${logstashEncoderVersion}")
    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.yaml:snakeyaml")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-framework-engine:5.9.1")
    testImplementation("com.buralotech.oss.testcontainers:mock-oauth2:1.0.0")
    aotPlugins(platform("io.micronaut.platform:micronaut-platform:4.10.1"))
    aotPlugins("io.micronaut.security:micronaut-security-aot")
}

// Exclude cloud function runtime jars from test runtime classpath to avoid multiple
// ServerRequestBinderRegistry candidates during tests. Netty should be the only
// registry used in unit/integration tests.
configurations {
    named("testRuntimeClasspath") {
        exclude(group = "io.micronaut.aws", module = "micronaut-function-aws-api-proxy")
        exclude(group = "io.micronaut.gcp", module = "micronaut-gcp-function-http")
    }
}

// Build profiles: -Plocal, -Plambda, -Pgcf
val isLocalBuild = project.hasProperty("local") || System.getenv("MICRONAUT_ENV") == "local"
val isLambdaBuild = project.hasProperty("lambda")
val isGcfBuild = project.hasProperty("gcf")

if (isLocalBuild) {
    // Local: exclude all cloud dependencies, use Netty only
    configurations {
        named("runtimeClasspath") {
            exclude(group = "io.micronaut.aws", module = "micronaut-function-aws-api-proxy")
            exclude(group = "io.micronaut.gcp", module = "micronaut-gcp-function-http")
            exclude(group = "com.google.cloud.functions", module = "functions-framework-api")
        }
    }
}

if (isLambdaBuild) {
    // AWS Lambda: exclude GCP, exclude Netty server
    configurations {
        named("runtimeClasspath") {
            exclude(group = "io.micronaut.gcp", module = "micronaut-gcp-function-http")
            exclude(group = "com.google.cloud.functions", module = "functions-framework-api")
            exclude(group = "io.micronaut", module = "micronaut-http-server-netty")
        }
    }
}

if (isGcfBuild) {
    // GCP Cloud Functions: exclude AWS, exclude Netty server
    configurations {
        named("runtimeClasspath") {
            exclude(group = "io.micronaut.aws", module = "micronaut-function-aws-api-proxy")
            exclude(group = "io.micronaut", module = "micronaut-http-server-netty")
        }
    }
}


application {
    mainClass = "ge.tiger8bit.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}


graalvmNative.toolchainDetection = false


tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    isZip64 = true
}

micronaut {
    runtime("netty")
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("ge.tiger8bit.*")
    }
    testResources {
        additionalModules.add("jdbc-postgresql")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
        configurationProperties.put("micronaut.security.jwks.enabled", "false")
    }
}

tasks.withType<StartTestResourcesService>().configureEach {
    useClassDataSharing.set(false)
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}

tasks.withType<Test> {
    // Ensure tests run with Java 21 if not already set externally
    jvmArgs("-Dfile.encoding=UTF-8")
    // 32-byte secret (64 hex chars) to satisfy HS256 256-bit minimum key length
    environment("JWT_SECRET", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
}

val openApiTargetDir = "$projectDir/docs"

tasks.register<Copy>("copyOpenApi") {
    from(layout.buildDirectory.dir("generated/ksp/main/resources/META-INF/swagger"))
    into(openApiTargetDir)
    rename { "openapi.yml" }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named("build") {
    finalizedBy("copyOpenApi")
}
