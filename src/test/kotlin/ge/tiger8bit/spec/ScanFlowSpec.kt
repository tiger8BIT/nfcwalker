package ge.tiger8bit.spec

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
class ScanFlowSpec : BaseApiSpec() {

    @Inject
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    override fun StringSpec.registerTests() {
        "complete start/finish" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(
                        org.id!!,
                        site.id!!,
                        "CP-SCAN",
                        BigDecimal("41.7151377"),
                        BigDecimal("44.8270903"),
                        BigDecimal("100.00")
                    )
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

            val run = fixtures.createPatrolRun(route.id!!, org.id!!)

            val (workerToken, workerId) = specHelpers.createWorkerToken(org.id!!, email = Emails.unique("worker"))
            val deviceId = "device-scan-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            start.challenge shouldNotBe null
            start.policy.checkpointId shouldBe cp.id
            start.policy.runId shouldBe run.id

            val finish = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/scan/finish",
                    FinishScanRequest(
                        challenge = start.challenge,
                        scannedAt = Instant.now().toString(),
                        lat = BigDecimal("41.7151377"),
                        lon = BigDecimal("44.8270903")
                    )
                ).withAuth(workerToken),
                FinishScanResponse::class.java
            )

            finish.verdict shouldBe ScanVerdict.OK
            finish.eventId shouldNotBe null
        }

        "complete start/finish with photos" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(
                        org.id!!,
                        site.id!!,
                        "CP-SCAN-PHOTO",
                        BigDecimal("41.7151377"),
                        BigDecimal("44.8270903"),
                        BigDecimal("100.00")
                    )
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
            val deviceId = "device-photo-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            val finishRequest = FinishScanRequest(
                challenge = start.challenge,
                scannedAt = Instant.now().toString(),
                lat = BigDecimal("41.7151377"),
                lon = BigDecimal("44.8270903"),
                checkStatus = CheckStatus.PROBLEMS_FOUND,
                checkNotes = "Broken glass"
            )

            val body = io.micronaut.http.client.multipart.MultipartBody.builder()
                .addPart(
                    "metadata",
                    "metadata.json",
                    io.micronaut.http.MediaType.APPLICATION_JSON_TYPE,
                    objectMapper.writeValueAsBytes(finishRequest)
                )
                .addPart("photos", "test-photo.jpg", io.micronaut.http.MediaType.IMAGE_JPEG_TYPE, "fake-image-content".toByteArray())
                .build()

            val finish = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/finish", body)
                    .contentType(io.micronaut.http.MediaType.MULTIPART_FORM_DATA)
                    .withAuth(workerToken),
                FinishScanResponse::class.java
            )

            finish.verdict shouldBe ScanVerdict.OK
            finish.eventId shouldNotBe null
        }

        "complete start/finish with sub-checks and multiple photos" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val subChecks = listOf(
                SubCheckRequest("Check lights", "Description 1", true),
                SubCheckRequest("Check doors", "Description 2", false)
            )

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(
                        org.id!!,
                        site.id!!,
                        "CP-SUB-CHECKS",
                        BigDecimal("41.7151377"),
                        BigDecimal("44.8270903"),
                        BigDecimal("100.00"),
                        subChecks = subChecks
                    )
                ).withAuth(bossToken),
                CheckpointResponse::class.java
            )

            cp.subChecks?.size shouldBe 2
            val subCheck1Id = cp.subChecks!![0].id
            val subCheck2Id = cp.subChecks!![1].id

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
            val deviceId = "device-sub-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            val finishRequest = FinishScanRequest(
                challenge = start.challenge,
                scannedAt = Instant.now().toString(),
                lat = BigDecimal("41.7151377"),
                lon = BigDecimal("44.8270903"),
                subCheckResults = listOf(
                    SubCheckResultRequest(subCheck1Id, CheckStatus.OK, "all good"),
                    SubCheckResultRequest(subCheck2Id, CheckStatus.PROBLEMS_FOUND, "needs repair")
                )
            )

            val fileName1 = "sub_${subCheck1Id}_1.jpg"
            val fileName2 = "sub_${subCheck1Id}_2.jpg"

            val body = MultipartBody.builder()
                .addPart(
                    "metadata",
                    "metadata.json",
                    MediaType.APPLICATION_JSON_TYPE,
                    objectMapper.writeValueAsBytes(finishRequest)
                )
                .addPart("photos", "parent.jpg", MediaType.IMAGE_JPEG_TYPE, "parent-photo".toByteArray())
                .addPart("photos", fileName1, MediaType.IMAGE_JPEG_TYPE, "sub1-photo".toByteArray())
                .addPart("photos", fileName2, MediaType.IMAGE_JPEG_TYPE, "sub2-photo".toByteArray())
                .build()

            val finish = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/finish", body)
                    .contentType(io.micronaut.http.MediaType.MULTIPART_FORM_DATA)
                    .withAuth(workerToken),
                FinishScanResponse::class.java
            )

            finish.verdict shouldBe ScanVerdict.OK
            finish.eventId shouldNotBe null

            // Verify sub-check events in DB
            val savedSubEvents = fixtures.getPatrolSubCheckEvents(finish.eventId)
            println("[DEBUG_LOG] Saved SubEvents: ${savedSubEvents.map { "id=${it.subCheckId}, status=${it.status}, notes=${it.notes}" }}")
            savedSubEvents.size shouldBe 2

            val s1 = savedSubEvents.find { it.subCheckId == subCheck1Id }!!
            s1.status shouldBe "ok"
            s1.notes shouldBe "all good"

            val s2 = savedSubEvents.find { it.subCheckId == subCheck2Id }!!
            s2.status shouldBe "problems_found"
            s2.notes shouldBe "needs repair"

            // Verify attachments
            val mainAttachments = fixtures.getAttachments("scan_event", finish.eventId)
            mainAttachments.size shouldBe 1
            mainAttachments[0].originalName shouldBe "parent.jpg"

            val sub1Attachments = fixtures.getAttachments("sub_check_event", s1.id!!)
            sub1Attachments.size shouldBe 2
            sub1Attachments[0].originalName shouldBe fileName1
            sub1Attachments[1].originalName shouldBe fileName2
        }

        "add photos to scan event" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(org.id!!, site.id!!, "CP-SCAN-PHOTO", BigDecimal("41.7"), BigDecimal("44.8"), BigDecimal("100"))
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
            val deviceId = "device-scan-photo-test"
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
                        scannedAt = Instant.now().toString()
                    )
                ).withAuth(workerToken),
                FinishScanResponse::class.java
            )

            val initialAttachments = fixtures.getAttachments("scan_event", finish.eventId)
            initialAttachments.shouldHaveSize(0)

            val body = MultipartBody.builder()
                .addPart("photos", "scan1.jpg", MediaType.IMAGE_JPEG_TYPE, "content1".toByteArray())
                .addPart("photos", "scan2.jpg", MediaType.IMAGE_JPEG_TYPE, "content2".toByteArray())
                .build()

            client.toBlocking().exchange<Any, Any>(
                HttpRequest.POST<Any>("/api/scan/events/${finish.eventId}/photos", body)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .withAuth(workerToken)
            )

            val attachments = fixtures.getAttachments("scan_event", finish.eventId)
            attachments.shouldHaveSize(2)
            attachments.map { it.originalName }.toSet() shouldBe setOf("scan1.jpg", "scan2.jpg")
        }

        "delete photo from scan event" {
            val (org, site) = fixtures.seedOrgAndSite()
            val (bossToken, _) = specHelpers.createBossToken(org.id!!, email = Emails.unique("boss"))

            val cp = client.toBlocking().retrieve(
                HttpRequest.POST(
                    "/api/admin/checkpoints",
                    CreateCheckpointRequest(
                        org.id!!,
                        site.id!!,
                        "CP-SCAN-DELETE",
                        BigDecimal("41.7"),
                        BigDecimal("44.8"),
                        BigDecimal("100")
                    )
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
            val deviceId = "device-scan-delete-test"
            fixtures.createDevice(workerId, org.id!!, deviceId = deviceId)

            val start = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/start", StartScanRequest(org.id!!, deviceId, cp.code)).withAuth(workerToken),
                StartScanResponse::class.java
            )

            val finishRequest = FinishScanRequest(
                challenge = start.challenge,
                scannedAt = Instant.now().toString()
            )

            val body = MultipartBody.builder()
                .addPart("metadata", "metadata.json", MediaType.APPLICATION_JSON_TYPE, objectMapper.writeValueAsBytes(finishRequest))
                .addPart("photos", "scan.jpg", MediaType.IMAGE_JPEG_TYPE, "content".toByteArray())
                .build()

            val finish = client.toBlocking().retrieve(
                HttpRequest.POST("/api/scan/finish", body).contentType(MediaType.MULTIPART_FORM_DATA).withAuth(workerToken),
                FinishScanResponse::class.java
            )

            val attachments = fixtures.getAttachments("scan_event", finish.eventId)
            attachments.shouldHaveSize(1)
            val photoId = attachments[0].id!!

            client.toBlocking().exchange<Any, Any>(
                HttpRequest.DELETE<Any>("/api/scan/events/${finish.eventId}/photos/$photoId").withAuth(workerToken)
            )

            val remainingAttachments = fixtures.getAttachments("scan_event", finish.eventId)
            remainingAttachments.shouldHaveSize(0)
        }
    }
}
