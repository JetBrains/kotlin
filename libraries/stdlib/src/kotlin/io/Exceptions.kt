package kotlin.io

import java.io.IOException
import java.io.File

private fun constructMessage(file: String, other: String?, reason: String?): String {
    val sb = StringBuilder(file)
    if (other != null) {
        sb.append(" -> $other")
    }
    if (reason != null) {
        sb.append(": $reason")
    }
    return sb.toString()
}

open public class FileSystemException(public val file: String,
                                      private val other: String? = null,
                                      public val reason: String? = null
) : IOException(constructMessage(file, other, reason)) {
    public val otherFile: String? = other
}

public class FileAlreadyExistsException(file: String,
                                        other: String? = null,
                                        reason: String? = null) : FileSystemException(file, other, reason)

public class DirectoryNotEmptyException(file: String,
                                        other: String? = null,
                                        reason: String? = null) : FileSystemException(file, other, reason)

public class AccessDeniedException(file: String,
                                   other: String? = null,
                                   reason: String? = null) : FileSystemException(file, other, reason)

public class NoSuchFileException(file: String,
                                 other: String? = null,
                                 reason: String? = null) : FileSystemException(file, other, reason)

public class FileIsDirectoryException(file: String,
                                      other: String? = null,
                                      reason: String? = null) : FileSystemException(file, other, reason)
