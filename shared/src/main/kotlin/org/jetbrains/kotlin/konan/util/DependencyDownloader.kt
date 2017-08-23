package org.jetbrains.kotlin.konan.util

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.*

class DependencyDownloader(
        var maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        var attemptIntervalMs: Long = DEFAULT_ATTEMPT_INTERVAL_MS
) {
    val executor = ExecutorCompletionService<Unit>(Executors.newSingleThreadExecutor())

    enum class ReplacingMode {
        /** Redownload the file and replace the existing one. */
        REPLACE,
        /** Throw FileAlreadyExistsException */
        THROW,
        /** Don't download the file and return the existing one*/
        RETURN_EXISTING
    }

    class DownloadingProgress(@Volatile var currentBytes: Long) {
        fun update(readBytes: Int) { currentBytes += readBytes }
    }

    private fun doDownload(originalUrl: URL,
                           connection: URLConnection,
                           tmpFile: File,
                           currentBytes: Long,
                           totalBytes: Long,
                           append: Boolean) {
        val progress = DownloadingProgress(currentBytes)

        // TODO: Implement multi-thread downloading.
        executor.submit {
            connection.getInputStream().use { from ->
                FileOutputStream(tmpFile, append).use { to ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = from.read(buffer)
                    while (read != -1) {
                        if (Thread.interrupted()) {
                            throw InterruptedException()
                        }
                        to.write(buffer, 0, read)
                        progress.update(read)
                        read = from.read(buffer)
                    }
                    if (progress.currentBytes != totalBytes) {
                        throw EOFException("The stream closed before end of downloading.")
                    }
                }
            }
        }

        var result: Future<Unit>?
        do {
            updateProgressMsg(originalUrl.toString(), progress.currentBytes, totalBytes)
            result = executor.poll(1, TimeUnit.SECONDS)
        } while(result == null)
        updateProgressMsg(originalUrl.toString(), progress.currentBytes, totalBytes)

        try {
            result.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    private fun tryHttpDownload(originalUrl: URL, connection: HttpURLConnection, tmpFile: File) {
        @Suppress("NAME_SHADOWING")
        var connection = connection
        connection.connect()
        val totalBytes = connection.contentLengthLong
        var currentBytes = 0L
        if (tmpFile.exists()) {
            currentBytes = tmpFile.length()
            if (currentBytes < totalBytes) {
                connection.disconnect()
                connection = originalUrl.openConnection() as HttpURLConnection
                connection.setRequestProperty("range", "bytes=$currentBytes-")
                connection.connect()
            } else {
                currentBytes = 0
                tmpFile.delete()
            }
        }

        doDownload(originalUrl, connection, tmpFile, currentBytes, totalBytes, true)
    }

    private fun tryOtherDownload(originalUrl: URL, connection: URLConnection, tmpFile: File) {
        connection.connect()
        val currentBytes = 0L
        val totalBytes = connection.contentLengthLong

        try {
            doDownload(originalUrl, connection, tmpFile, currentBytes, totalBytes, false)
        } catch(e: Throwable) {
            tmpFile.delete()
            throw e
        }
    }

    /** Performs an attempt to download a specified file into the specified location */
    private fun tryDownload(url: URL, tmpFile: File) {
        val connection = url.openConnection()
        if (connection is HttpURLConnection) {
            tryHttpDownload(url, connection, tmpFile)
        } else {
            tryOtherDownload(url, connection, tmpFile)
        }
    }

    /** Downloads a file from [source] url to [destination]. Returns [destination]. */
    fun download(source: URL,
                 destination: File,
                 replace: ReplacingMode = ReplacingMode.RETURN_EXISTING): File {

        if (destination.exists()) {
            when (replace) {
                ReplacingMode.RETURN_EXISTING -> return destination
                ReplacingMode.THROW -> throw FileAlreadyExistsException(destination)
                ReplacingMode.REPLACE -> Unit // Just continue with downloading.
            }
        }
        val tmpFile = File("${destination.canonicalPath}.$TMP_SUFFIX")

        check(!tmpFile.isDirectory) {
            "A temporary file is a directory: ${tmpFile.canonicalPath}. Remove it and try again."
        }
        check(!destination.isDirectory) {
            "The destination file is a directory: ${destination.canonicalPath}. Remove it and try again."
        }

        var attempt = 1
        var waitTime = 0L
        while (true) {
            try {
                tryDownload(source, tmpFile)
                break
            } catch (e: IOException) {
                if (attempt >= maxAttempts) {
                    throw e
                }
                attempt++
                waitTime += attemptIntervalMs
                println("Cannot download a dependency: $e\n" +
                        "Waiting ${waitTime.toDouble() / 1000} sec and trying again (attempt: $attempt/$maxAttempts).")
                // TODO: Wait better
                Thread.sleep(waitTime)
            }
        }

        Files.move(tmpFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        println("Done.")
        return destination
    }

    private val Long.humanReadable: String
        get() {
            if (this < 0) {
                return "-"
            }
            if (this < 1024) {
                return "$this bytes"
            }
            val exp = (Math.log(this.toDouble()) / Math.log(1024.0)).toInt()
            val prefix = "kMGTPE"[exp-1]
            return "%.1f %siB".format(this / Math.pow(1024.0, exp.toDouble()), prefix)
        }

    private fun updateProgressMsg(url: String, currentBytes: Long, totalBytes: Long) {
        print("\rDownloading dependency: $url (${currentBytes.humanReadable}/${totalBytes.humanReadable}). ")
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 10
        const val DEFAULT_ATTEMPT_INTERVAL_MS = 3000L

        const val TMP_SUFFIX = "part"
    }
}
