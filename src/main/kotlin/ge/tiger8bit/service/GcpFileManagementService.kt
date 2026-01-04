package ge.tiger8bit.service

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.http.multipart.CompletedFileUpload
import jakarta.inject.Singleton
import java.io.InputStream

@Singleton
@Requires(property = "gcp.storage.bucket")
class GcpFileManagementService(
    private val storage: Storage,
    @Value("\${gcp.storage.bucket}") private val bucketName: String
) : FileManagementService {
    override fun uploadFile(file: CompletedFileUpload, path: String): String {
        val blobId = BlobId.of(bucketName, path)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.contentType.orElse(null)?.toString())
            .build()
        storage.create(blobInfo, file.bytes)
        return "gs://$bucketName/$path"
    }

    override fun uploadFile(inputStream: InputStream, path: String, originalName: String?, contentType: String?): String {
        val blobId = BlobId.of(bucketName, path)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(contentType)
            .build()
        storage.create(blobInfo, inputStream.readAllBytes())
        return "gs://$bucketName/$path"
    }

    override fun deleteFile(path: String) {
        storage.delete(BlobId.of(bucketName, path))
    }

    override fun getDownloadUrl(path: String): String {
        return "https://storage.googleapis.com/$bucketName/$path"
    }
}
