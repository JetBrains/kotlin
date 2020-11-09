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

import org.jetbrains.kotlin.konan.util.KonanHomeProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

internal fun decodeFromUtf8(bytes: ByteArray) = String(bytes)
internal fun encodeToUtf8(str: String) = str.toByteArray()

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

fun loadKonanLibrary(name: String) {
    try {
        System.loadLibrary(name)
    } catch (e: UnsatisfiedLinkError) {
        val fullLibraryName = System.mapLibraryName(name)
        val dir = "${KonanHomeProvider.determineKonanHome()}/konan/nativelib"
        try {
            System.load("$dir/$fullLibraryName")
        } catch (e: UnsatisfiedLinkError) {
            val tempDir = Files.createTempDirectory(Paths.get(dir), null).toAbsolutePath().toString()
            Files.createLink(Paths.get(tempDir, fullLibraryName), Paths.get(dir, fullLibraryName))
            // TODO: Does not work on Windows. May be use FILE_FLAG_DELETE_ON_CLOSE?
            File(tempDir).deleteOnExit()
            File("$tempDir/$fullLibraryName").deleteOnExit()
            System.load("$tempDir/$fullLibraryName")
        }
    }
}