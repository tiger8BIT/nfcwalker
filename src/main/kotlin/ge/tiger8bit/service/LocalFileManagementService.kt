package ge.tiger8bit.service

import io.micronaut.context.annotation.Requires
import io.micronaut.http.multipart.CompletedFileUpload
import jakarta.inject.Singleton
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Singleton
@Requires(property = "local", value = "true")
class LocalFileManagementService : FileManagementService {
    private val storageDir = "storage"

    init {
        File(storageDir).mkdirs()
    }

    override fun uploadFile(file: CompletedFileUpload, path: String): String {
        val destFile = File(storageDir, path)
        destFile.parentFile.mkdirs()
        Files.copy(file.inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return destFile.absolutePath
    }

    override fun uploadFile(inputStream: InputStream, path: String, originalName: String?, contentType: String?): String {
        val destFile = File(storageDir, path)
        destFile.parentFile.mkdirs()
        Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return destFile.absolutePath
    }

    override fun deleteFile(path: String) {
        File(storageDir, path).delete()
    }

    override fun getDownloadUrl(path: String): String {
        return "/storage/$path"
    }
}
