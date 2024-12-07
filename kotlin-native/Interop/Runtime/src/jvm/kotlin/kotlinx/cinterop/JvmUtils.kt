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

package kotlinx.cinterop

import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
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
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Short.signExtend(): R = when (R::class.java) {
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Int.signExtend(): R = when (R::class.java) {
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Long.signExtend(): R = when (R::class.java) {
    java.lang.Long::class.java -> this.toLong() as R
    else -> this.invalidSignExtension()
}

inline fun <reified R : Number> Number.invalidSignExtension(): R {
    throw Error("unable to sign extend ${this.javaClass.simpleName} \"${this}\" to ${R::class.java.simpleName}")
}

inline fun <reified R : Number> Byte.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Short.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Int.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    else -> this.invalidNarrowing()
}

inline fun <reified R : Number> Long.narrow(): R = when (R::class.java) {
    java.lang.Byte::class.java -> this.toByte() as R
    java.lang.Short::class.java -> this.toShort() as R
    java.lang.Integer::class.java -> this.toInt() as R
    java.lang.Long::class.java -> this.toLong() as R
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

private val sha256 = MessageDigest.getInstance("SHA-256")
private val systemTmpDir = System.getProperty("java.io.tmpdir")

// TODO: File(..).deleteOnExit() does not work on Windows. May be use FILE_FLAG_DELETE_ON_CLOSE?
private fun tryLoadKonanLibrary(dir: String, fullLibraryName: String, runFromDaemon: Boolean): Boolean {
    val fullLibraryPath = try {
        Paths.get(dir, fullLibraryName)
    } catch (ignored: InvalidPathException) {
        return false
    }
    if (!Files.exists(fullLibraryPath)) return false

    fun createTempDirWithLibrary() = if (runFromDaemon) {
        Files.createTempDirectory(null).toAbsolutePath().toString().also {
            Files.copy(fullLibraryPath, Paths.get(it, fullLibraryName))
        }
    } else {
        Files.createTempDirectory(Paths.get(dir), null).toAbsolutePath().toString().also {
            Files.createLink(Paths.get(it, fullLibraryName), fullLibraryPath)
        }
    }

    val defaultTempDir = if (!runFromDaemon)
        dir
    else {
        // Sometimes loading library from its original place doesn't work (it gets 'half-loaded'
        // with relocation table haven't been substituted by the system loader without reporting any error).
        // We workaround this by copying the library to some temporary place.
        // For now this behaviour have only been observed for compilations run from the Gradle daemon on Team City.
        val hash = sha256.digest(Files.readAllBytes(fullLibraryPath))
        val defaultTempDirName = buildString {
            append(fullLibraryName)
            append('_')
            hash.forEach {
                val hex = it.toUByte().toString(16)
                if (hex.length == 1)
                    append('0')
                append(hex)
            }
        }
        val defaultTempDir = Paths.get(systemTmpDir, defaultTempDirName).toAbsolutePath().toString()
        val tempDir = File(createTempDirWithLibrary())
        if (tempDir.renameTo(File(defaultTempDir))) {
            File(defaultTempDir).deleteOnExit()
            File("$defaultTempDir/$fullLibraryName").deleteOnExit()
        } else {
            tempDir.deleteRecursively()
        }
        defaultTempDir
    }

    try {
        System.load("$defaultTempDir/$fullLibraryName")
    } catch (e: UnsatisfiedLinkError) {
        if (fullLibraryName.endsWith(".dylib") && e.message?.contains("library load disallowed by system policy") == true) {
            throw UnsatisfiedLinkError("""
                    |Library $dir/$fullLibraryName can't be loaded.
                    |${'\t'}This can happen because library file is marked as untrusted (e.g because it was downloaded from browser).
                    |${'\t'}You can trust libraries in distribution by running
                    |${'\t'}${'\t'}xattr -d com.apple.quarantine '$dir'/*
                    |${'\t'}command in terminal
                    |${'\t'}Original exception message:
                    |${'\t'}${e.message}
                    """.trimMargin())
        }
        val tempDir = createTempDirWithLibrary()

        File(tempDir).deleteOnExit()
        File("$tempDir/$fullLibraryName").deleteOnExit()
        System.load("$tempDir/$fullLibraryName")
    }

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
    error("Lib $fullLibraryName is not found in $defaultNativeLibsDir and ${paths.joinToString { it }}")
}