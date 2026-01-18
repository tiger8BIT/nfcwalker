package ge.tiger8bit.spec

import ge.tiger8bit.domain.AttachmentEntityType
import ge.tiger8bit.dto.*
import ge.tiger8bit.spec.common.BaseApiSpec
import ge.tiger8bit.spec.common.TestData.Emails
import ge.tiger8bit.spec.common.withAuth
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import jakarta.inject.Inject
import java.math.BigDecimal
import java.time.Instant

@MicronautTest(transactional = false)
class IncidentFlowSpec : BaseApiSpec() {

    @Inject
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    override fun StringSpec.registerTests() {
        "create incidents during scan" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-INCIDENT", BigDecimal("41.7"), BigDecimal("44.8"), BigDecimal("100"))
                ).withAuth(bossToken),
                CheckpointResponse::class.java
            )

            val route = fixtures.createRoute(org.id!!, site.id!!)
            client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/routes/${route.id}/points",
                    BulkAddRouteCheckpointsRequest(listOf(AddRouteCheckpointRequest(cp.id, 1, 0, 3600)))
                ).withAuth(bossToken),
                Map::class.java
            )

            fixtures.createPatrolRun(route.id!!, org.id!!)

            val (workerToken, workerId) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))
            val deviceId = "device-incident-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            val finish = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/scan/finish",
                    FinishScanRequest(
                        challenge = start.challenge,
                        scannedAt = Instant.now(),
                        incidents = listOf(
                            IncidentCreateRequest(description = "Broken window", severity = IncidentSeverity.HIGH),
                            IncidentCreateRequest(description = "Graffiti", severity = IncidentSeverity.LOW)
                        )
                    )
                ).withAuth(workerToken),
                FinishScanResponse::class.java
            )

            finish.verdict shouldBe ScanVerdict.OK

            val page = getPage("/api/incidents?organizationId=${org.id}", bossToken, IncidentResponse::class.java)
            println("[DEBUG_LOG] Page content: ${page.content}")

            page.content.shouldHaveSize(2)
            page.content.any { it.description == "Broken window" && it.severity == IncidentSeverity.HIGH } shouldBe true
            page.content.any { it.description == "Graffiti" && it.severity == IncidentSeverity.LOW } shouldBe true
        }

        "create standalone incident with photos" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val request = IncidentCreateRequest(
                organizationId = org.id,
                siteId = site.id,
                description = "Leaking pipe",
                severity = IncidentSeverity.MEDIUM
            )

            val body = MultipartBody.builder()
                .addPart("metadata", "metadata.json", MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(request))
                .addPart("photos", "pipe.jpg", MediaType.IMAGE_JPEG_TYPE, "fake-image-content".toByteArray())
                .build()

            val response = try {
                client.toBlocking().retrieve(
                    HttpRequest.POST("/api/incidents", body)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .withAuth(workerToken),
                    IncidentResponse::class.java
                )
            } catch (e: io.micronaut.http.client.exceptions.HttpClientResponseException) {
                println("[DEBUG_LOG] Create incident failed: ${e.response.body()}")
                throw e
            }

            response.description shouldBe "Leaking pipe"
            response.status shouldBe IncidentStatus.OPEN
            response.id shouldNotBe null

            val attachments = fixtures.getAttachments(AttachmentEntityType.incident, response.id)
            attachments.shouldHaveSize(1)
            attachments[0].originalName shouldBe "pipe.jpg"
        }

        "patch incident status" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val created = try {
                client.toBlocking().retrieve(
                    HttpRequest.POST(
                        "/api/incidents",
                        MultipartBody.builder().addPart(
                            "metadata", "metadata.json", MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(
                                IncidentCreateRequest(org.id, site.id, description = "Test incident")
                            )
                        ).build()
                    ).contentType(MediaType.MULTIPART_FORM_DATA).withAuth(workerToken),
                    IncidentResponse::class.java
                )
            } catch (e: io.micronaut.http.client.exceptions.HttpClientResponseException) {
                println("[DEBUG_LOG] Create incident for patch failed: ${e.response.body()}")
                throw e
            }

            val patched = try {
                client.toBlocking().retrieve(
                    HttpRequest.PATCH(
                        "/api/incidents/${created.id}",
                        IncidentPatchRequest(status = IncidentStatus.IN_PROGRESS)
                    ).withAuth(bossToken),
                    IncidentResponse::class.java
                )
            } catch (e: io.micronaut.http.client.exceptions.HttpClientResponseException) {
                println("[DEBUG_LOG] Patch incident failed: ${e.response.body()}")
                throw e
            }

            patched.status shouldBe IncidentStatus.IN_PROGRESS
        }

        "delete incident" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val created = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/incidents",
                    MultipartBody.builder().addPart(
                        "metadata", "metadata.json", MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(
                            IncidentCreateRequest(org.id, site.id, description = "To be deleted")
                        )
                    ).build()
                ).contentType(MediaType.MULTIPART_FORM_DATA).withAuth(workerToken),
                IncidentResponse::class.java
            )

            client.toBlocking().exchange<Any, Any>(
                HttpRequest.DELETE<Any>("/api/incidents/${created.id}").withAuth(bossToken)
            )

            val page = getPage("/api/incidents?organizationId=${org.id}", bossToken, IncidentResponse::class.java)

            page.content.none { it.id == created.id } shouldBe true
        }

        "add photos to existing incident" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val created = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/incidents",
                    MultipartBody.builder().addPart(
                        "metadata", "metadata.json", MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(
                            IncidentCreateRequest(org.id, site.id, description = "Incident with photos")
                        )
                    ).build()
                ).contentType(MediaType.MULTIPART_FORM_DATA).withAuth(workerToken),
                IncidentResponse::class.java
            )

            val initialAttachments = fixtures.getAttachments(AttachmentEntityType.incident, created.id)
            initialAttachments.shouldHaveSize(0)

            val body = MultipartBody.builder()
                .addPart("photos", "photo1.jpg", MediaType.IMAGE_JPEG_TYPE, "content1".toByteArray())
                .addPart("photos", "photo2.jpg", MediaType.IMAGE_JPEG_TYPE, "content2".toByteArray())
                .build()

            client.toBlocking().exchange<Any, Any>(
                HttpRequest.POST<Any>("/api/incidents/${created.id}/photos", body)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .withAuth(workerToken)
            )

            val attachments = fixtures.getAttachments(AttachmentEntityType.incident, created.id)
            attachments.shouldHaveSize(2)
            attachments.map { it.originalName }.toSet() shouldBe setOf("photo1.jpg", "photo2.jpg")
        }

        "delete photo from incident" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val body = MultipartBody.builder()
                .addPart(
                    "metadata", "metadata.json", MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(
                        IncidentCreateRequest(org.id, site.id, description = "Incident with photo")
                    )
                )
                .addPart("photos", "photo.jpg", MediaType.IMAGE_JPEG_TYPE, "content".toByteArray())
                .build()

            val created = client.toBlocking().retrieve(
                HttpRequest.POST("/api/incidents", body)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .withAuth(workerToken),
                IncidentResponse::class.java
            )

            val attachments = fixtures.getAttachments(AttachmentEntityType.incident, created.id)
            attachments.shouldHaveSize(1)
            val photoId = attachments[0].id!!

            client.toBlocking().exchange<Any, Any>(
                HttpRequest.DELETE<Any>("/api/incidents/${created.id}/photos/$photoId").withAuth(workerToken)
            )

            val remainingAttachments = fixtures.getAttachments(AttachmentEntityType.incident, created.id)
            remainingAttachments.shouldHaveSize(0)
        }

        "edit incident description and severity" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))
            val (workerToken, _) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))

            val created = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/incidents",
                    MultipartBody.builder().addPart(
                        "metadata", "metadata.json", MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(
                            IncidentCreateRequest(org.id, site.id, description = "Original description", severity = IncidentSeverity.LOW)
                        )
                    ).build()
                ).contentType(MediaType.MULTIPART_FORM_DATA).withAuth(workerToken),
                IncidentResponse::class.java
            )

            created.description shouldBe "Original description"
            created.severity shouldBe IncidentSeverity.LOW

            val patched = client.toBlocking().retrieve(
                HttpRequest.PATCH(
                    "/api/incidents/${created.id}",
                    IncidentPatchRequest(
                        description = "Updated description",
                        severity = IncidentSeverity.CRITICAL
                    )
                ).withAuth(bossToken),
                IncidentResponse::class.java
            )

            patched.description shouldBe "Updated description"
            patched.severity shouldBe IncidentSeverity.CRITICAL
            patched.id shouldBe created.id
        }
    }
}
