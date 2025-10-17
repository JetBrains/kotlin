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

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlinx.cinterop

import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.lang.System
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.InvalidPathException
import java.security.MessageDigest

private fun decodeFromUtf8(bytes: ByteArray) = String(bytes)
internal fun encodeToUtf8(str: String) = str.toByteArray()

internal fun CPointer<ByteVar>.toKStringFromUtf8Impl(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toByte()) {
        ++length
    }

    val bytes = ByteArray(length)
    nativeMemUtils.getByteArray(nativeBytes.pointed, bytes, length)
    return decodeFromUtf8(bytes)
}

fun bitsToFloat(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)
fun bitsToDouble(bits: Long): Double = java.lang.Double.longBitsToDouble(bits)

// TODO: the functions below should eventually be intrinsified

inline fun <reified R : Number> Byte.signExtend(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Short.signExtend(): R = when (R::class.java) {
    java.lang.Short::class.java -> this as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Int.signExtend(): R = when (R::class.java) {
    java.lang.Integer::class.java -> this as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Long.signExtend(): R = when (R::class.java) {
    java.lang.Long::class.java -> this as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Number.invalidSignExtension(): R {
    throw Error("unable to sign extend ${this.javaClass.simpleName} \"${this}\" to ${R::class.java.simpleName}")
}

inline fun <reified R : Number> Byte.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Short.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Int.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Long.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Number.invalidNarrowing(): R {
    throw Error("unable to narrow ${this.javaClass.simpleName} \"${this}\" to ${R::class.java.simpleName}")
}

// Ported from ClassLoader::initializePath.
private fun initializePath() =
        System.getProperty("java.library.path", "")
                .split(File.pathSeparatorChar)
                .map { if (it == "") "." else it }

// Track the libraries that we have already loaded.
private var loadedLibraries = mutableSetOf<String>()

private fun tryLoadKonanLibrary(dir: String, fullLibraryName: String, runFromDaemon: Boolean): Boolean {
    if (loadedLibraries.contains(fullLibraryName)) {
        // Already loaded a library with this name.
        return true
    }
    var fullLibraryPath = try {
        Paths.get(dir, fullLibraryName)
    } catch (ignored: InvalidPathException) {
        return false
    }
    if (!Files.exists(fullLibraryPath)) return false

    fun createTemporaryCopyOfLibrary(): Path {
        // Create a temporary directory and copy the library into it.
        val tmpDirRoot = if (runFromDaemon) System.getProperty("java.io.tmpdir") else dir
        val tmpDirPath = Files.createTempDirectory(Paths.get(tmpDirRoot), "konanc")
        val dest = tmpDirPath.resolve(fullLibraryName)
        if (runFromDaemon) {
            Files.copy(fullLibraryPath, dest)
        } else {
            Files.createLink(dest, fullLibraryPath)
        }
        // TODO: File(..).deleteOnExit() does not always work on Windows with DLLs. Maybe use FILE_FLAG_DELETE_ON_CLOSE?
        tmpDirPath.toFile().deleteOnExit()
        dest.toFile().deleteOnExit()
        return dest
    }

    val safeLoadKonanLibrary = runFromDaemon && System.getProperty("kotlin.native.tool.safeLoadKonanLibrary") == "true"
    if (safeLoadKonanLibrary) {
        // The block below came in https://github.com/JetBrains/kotlin/commit/f724b5c29da0122a1391af6e28ef3823aa2cb831
        // Not sure what Platform/OS this issue was seen on, but we have been seeing issues where `renameTo` will fail
        // on macOS. See the commit above to see the original change.
        // Added the `safeLoadKonanLibrary` property to allow this block to be executed, but in general avoiding the copy is far better.
        // The copy will trigger virus scans on macOS, and can leave files scattered in temp directories on Windows.
        // Original comment:
        // Sometimes loading library from its original place doesn't work (it gets 'half-loaded'
        // with relocation table haven't been substituted by the system loader without reporting any error).
        // We work around this by copying the library to some temporary place.
        // For now this behaviour have only been observed for compilations run from the Gradle daemon on Team City.
        fullLibraryPath = createTemporaryCopyOfLibrary()
    }

    // System load will throw `UnsatisfiedLinkError` if fullLibraryPath isn't resolved.
    val realPath = fullLibraryPath.toRealPath().toString()
    try {
        System.load(realPath)
    } catch (e: UnsatisfiedLinkError) {
        if (realPath.endsWith(".dylib") && e.message?.contains("library load disallowed by system policy") == true) {
            throw UnsatisfiedLinkError("""
                    |Library $realPath can't be loaded.
                    |${'\t'}This can happen because the library file is marked as untrusted (e.g because it was downloaded from browser).
                    |${'\t'}You can trust this library by running:
                    |${'\t'}${'\t'}xattr -d com.apple.quarantine '$realPath'
                    |${'\t'}in the terminal.
                    |${'\t'}Original exception message:
                    |${'\t'}${e.message}
                    """.trimMargin())
        }
        // Try copying the library to a temp directory and then loading it as a fallback.
        // Note that this will cause the macOS virus scanner (XProtect) to scan it, and may leave files in temp directories on Windows.
        // Not done in `safeLoadKonanLibrary` case because we would've already attempted to load a copy above.
        if (!safeLoadKonanLibrary) {
            fullLibraryPath = createTemporaryCopyOfLibrary()
            System.load(fullLibraryPath.toRealPath().toString())
        } else {
            rethrow(e)
        }
    }
    loadedLibraries.add(fullLibraryName)
    return true
}

fun loadKonanLibrary(name: String) {
    val runFromDaemon = System.getProperty("kotlin.native.tool.runFromDaemon") == "true"
    val fullLibraryName = System.mapLibraryName(name)
    val paths = initializePath()
    for (dir in paths) {
        if (tryLoadKonanLibrary(dir, fullLibraryName, runFromDaemon)) return
    }
    val defaultNativeLibsDir = "${KotlinNativePaths.homePath.absolutePath}/konan/nativelib"
    if (tryLoadKonanLibrary(defaultNativeLibsDir, fullLibraryName, runFromDaemon))
        return
    error("Lib $fullLibraryName was not found in $defaultNativeLibsDir and ${paths.joinToString { it }}")
}
