package ge.tiger8bit.service

import io.micronaut.http.multipart.CompletedFileUpload
import java.io.InputStream

interface FileManagementService {
    fun uploadFile(file: CompletedFileUpload, path: String): String
    fun uploadFile(inputStream: InputStream, path: String, originalName: String?, contentType: String?): String
    fun deleteFile(path: String)
    fun getDownloadUrl(path: String): String
}
