package com.soulknight.matrixai.capture

import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Uploads collected JPEGs to a user-selected repository using a fine-grained PAT. */
object GitHubDatasetUploader {
    fun upload(
        files: List<File>,
        token: String,
        repository: String,
        branch: String,
        onProgress: (String) -> Unit
    ) {
        Thread {
            if (token.isBlank() || !repository.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))) {
                onProgress("Thiếu token hoặc tên repository không hợp lệ")
                return@Thread
            }
            var uploaded = 0
            for (file in files) {
                try {
                    val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                    val path = "matrix-dataset/${file.name}"
                    val url = URL("https://api.github.com/repos/$repository/contents/$path")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "PUT"
                        connectTimeout = 15000
                        readTimeout = 30000
                        doOutput = true
                        setRequestProperty("Authorization", "Bearer $token")
                        setRequestProperty("Accept", "application/vnd.github+json")
                        setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                    val body = JSONObject()
                        .put("message", "Add Matrix room-change dataset: ${file.name}")
                        .put("content", encoded)
                        .put("branch", branch)
                        .toString()
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    connection.disconnect()
                    if (code !in 200..299) {
                        onProgress("GitHub từ chối ${file.name} (HTTP $code): ${response.take(180)}")
                        return@Thread
                    }
                    uploaded++
                    onProgress("Đã tải lên $uploaded/${files.size}: ${file.name}")
                } catch (e: Exception) {
                    onProgress("Lỗi tải ${file.name}: ${e.message}")
                    return@Thread
                }
            }
            onProgress("Hoàn tất: đã tải lên $uploaded ảnh")
        }.start()
    }
}
