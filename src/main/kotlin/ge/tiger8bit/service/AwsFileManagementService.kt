package ge.tiger8bit.service

import io.micronaut.context.annotation.Requires
import io.micronaut.http.multipart.CompletedFileUpload
import jakarta.inject.Singleton
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream

@Singleton
@Requires(property = "aws.s3.bucket")
class AwsFileManagementService(
    private val s3Client: S3Client,
    @io.micronaut.context.annotation.Value("\${aws.s3.bucket}") private val bucket: String
) : FileManagementService {
    override fun uploadFile(file: CompletedFileUpload, path: String): String {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(path)
            .contentType(file.contentType.orElse(null)?.toString())
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(file.inputStream, file.size))
        return "s3://$bucket/$path"
    }

    override fun uploadFile(inputStream: InputStream, path: String, originalName: String?, contentType: String?): String {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(path)
            .contentType(contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available().toLong()))
        return "s3://$bucket/$path"
    }

    override fun deleteFile(path: String) {
        s3Client.deleteObject { it.bucket(bucket).key(path) }
    }

    override fun getDownloadUrl(path: String): String {
        return "https://$bucket.s3.amazonaws.com/$path"
    }
}
