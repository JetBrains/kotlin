/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan

/**
 * This is a common ancestor of all Kotlin/Native exceptions.
 */
open class KonanException(message: String = "", cause: Throwable? = null) : Exception(message, cause)

/**
 * An error occurred during external tool invocation. Such as non-zero exit code.
 */
class KonanExternalToolFailure(message: String, val toolName: String, cause: Throwable? = null) : KonanException(message, cause)

/**
 * An exception indicating a failed attempt to access some parts of Xcode (e.g. get SDK paths or version).
 */
class MissingXcodeException(message: String, cause: Throwable? = null) : KonanException(message, cause)

/**
 * Native exception handling in Kotlin: terminate, wrap, etc.
 * Foreign exceptionMode mode is per library option: controlled by cinterop command-line option or def file property
 * than stored in klib manifest and used by compiler to generate appropriate handler.
 */
class ForeignExceptionMode {
    companion object {
        val manifestKey = "foreignExceptionMode"
        val default = Mode.TERMINATE
        fun byValue(value: String?): Mode = value?.let {
            Mode.values().find { it.value == value }
                    ?: throw IllegalArgumentException("Illegal ForeignExceptionMode $value")
        } ?: default
    }
    enum class Mode(val value: String) {
        TERMINATE("terminate"),
        OBJC_WRAP("objc-wrap")
    }
}
