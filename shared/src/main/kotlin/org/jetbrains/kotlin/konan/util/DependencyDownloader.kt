package org.jetbrains.kotlin.konan.util

import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DependencyDownloader(
        var maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        var attemptIntervalMs: Long = DEFAULT_ATTEMPT_INTERVAL_MS) {

    enum class ReplacingMode {
        /** Redownload the file and replace the existing one. */
        REPLACE,
        /** Throw FileAlreadyExistsException */
        THROW,
        /** Don't download the file and return the existing one*/
        RETURN_EXISTING
    }

    /** Performs an attempt to download a specified file into the specified location */
    fun tryDownload(url: URL, dstFile: File) {
        val connection = url.openConnection()
        val totalBytes = connection.contentLengthLong
        var currentBytes = 0L

        try {
            url.openStream().use { from ->
                FileOutputStream(dstFile, false).use { to ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = from.read(buffer)
                    while (read != -1) {
                        to.write(buffer, 0, read)
                        currentBytes += read
                        updateProgressMsg(url.toString(), currentBytes, totalBytes)
                        read = from.read(buffer)
                    }
                    if (currentBytes != totalBytes) {
                        throw EOFException("The stream closed before end of downloading.")
                    }
                }
            }
        } catch (e: Throwable) {
            dstFile.delete()
            throw e
        }
    }

    /** Downloads a file from [source] url to [destination]. Returns [destination]. */
    fun download(source: URL,
                 destination: File,
                 replace: ReplacingMode = ReplacingMode.RETURN_EXISTING): File {

        if (destination.exists()) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (replace) {
                ReplacingMode.RETURN_EXISTING -> return destination
                ReplacingMode.THROW -> throw FileAlreadyExistsException(destination)
                // TODO: What if dst is a directory?
            }
        }
        // TODO: resuming must be here
        val tmpFile = File("${destination.canonicalPath}.$TMP_SUFFIX")

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
                        "Wait ${waitTime.toDouble() / 1000} sec and try again (attempt: $attempt/$maxAttempts).")
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
        print("\rDownload dependency: $url (${currentBytes.humanReadable}/${totalBytes.humanReadable}). ")
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 10
        const val DEFAULT_ATTEMPT_INTERVAL_MS = 3000L

        const val TMP_SUFFIX = "part"
    }
}
