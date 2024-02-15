/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.exceptions

import java.io.File

/**
 * A base exception class for file system exceptions.
 * @property file the file on which the failed operation was performed.
 * @property other the second file involved in the operation, if any (for example, the target of a copy or move)
 * @property reason the description of the error
 */
@Suppress("DEPRECATION_ERROR")
open public class FileSystemException(
    file: File,
    other: File? = null,
    reason: String? = null
) : kotlin.io.FileSystemException(file, other, reason)

/**
 * An exception class which is used when some file to create or copy to already exists.
 */
@Suppress("DEPRECATION_ERROR")
public class FileAlreadyExistsException(
    file: File,
    other: File? = null,
    reason: String? = null
) : kotlin.io.FileAlreadyExistsException(file, other, reason)

/**
 * An exception class which is used when we have not enough access for some operation.
 */
@Suppress("DEPRECATION_ERROR")
public class AccessDeniedException(
    file: File,
    other: File? = null,
    reason: String? = null
) : kotlin.io.AccessDeniedException(file, other, reason)

/**
 * An exception class which is used when file to copy does not exist.
 */
@Suppress("DEPRECATION_ERROR")
public class NoSuchFileException(
    file: File,
    other: File? = null,
    reason: String? = null
) : kotlin.io.NoSuchFileException(file, other, reason)

