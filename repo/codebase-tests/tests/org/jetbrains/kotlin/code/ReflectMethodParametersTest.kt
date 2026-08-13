/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * `:kotlin-reflect:dexMethodCount` dexes the whole `kotlin-reflect` jar with the D8 that
 * `com.jakewharton.dex:dex-member-list` brings along, and that D8 predates the `MethodParameters` attribute: it fails
 * with a bare `NullPointerException` the moment a single class carries one.
 *
 * A modern `javac` emits `MethodParameters` for the parameters of bridge methods even under `--release 8`, while
 * `javac` 8 does not. The modules shaded into `kotlin-reflect` therefore pin their toolchain to JDK 8, and this test
 * makes that requirement visible: without it, adding a covariant override to any of those modules breaks the build
 * with a diagnostic that points nowhere near the cause.
 */
class ReflectMethodParametersTest {

    private val reflectJar = Path("dist/kotlinc/lib/kotlin-reflect.jar")

    private val classFileMagic = 0xCAFEBABE.toInt()

    @Test
    fun `kotlin-reflect carries no MethodParameters attribute`() {
        if (!reflectJar.exists()) fail("'$reflectJar' is missing. This test has to run after the ':dist' task.")

        val offenders = ZipFile(reflectJar.toFile()).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .filter { entry -> zip.getInputStream(entry).use(::referencesMethodParameters) }
                .map { it.name }
                .toList()
        }

        if (offenders.isNotEmpty()) {
            fail(
                "${offenders.size} class file(s) of '$reflectJar' carry a `MethodParameters` attribute, which breaks " +
                        "`:kotlin-reflect:dexMethodCount`. They come from a module that is shaded into kotlin-reflect " +
                        "and no longer compiles on JDK 8 — give it back its " +
                        "`configureJvmToolchain(JdkMajorVersion.JDK_1_8)`:\n\n" +
                        offenders.sorted().joinToString("\n").prependIndent("  ")
            )
        }
    }

    /**
     * Whether the constant pool of a class file holds the `MethodParameters` string. Scanning the pool over-approximates
     * — a class could name it without using it — but nothing in kotlin-reflect does, and it keeps the check to a single
     * cheap pass that needs no bytecode library.
     */
    private fun referencesMethodParameters(entry: InputStream): Boolean = with(DataInputStream(entry)) {
        if (readInt() != classFileMagic) return false
        readUnsignedShort() // minor_version
        readUnsignedShort() // major_version
        val constantPoolCount = readUnsignedShort()
        var index = 1
        while (index < constantPoolCount) {
            when (val tag = readUnsignedByte()) {
                CONSTANT_UTF8 -> if (readUTF() == "MethodParameters") return true
                CONSTANT_INTEGER, CONSTANT_FLOAT, CONSTANT_FIELD_REF, CONSTANT_METHOD_REF,
                CONSTANT_INTERFACE_METHOD_REF, CONSTANT_NAME_AND_TYPE, CONSTANT_DYNAMIC,
                CONSTANT_INVOKE_DYNAMIC,
                    -> skipBytes(4)
                // A long or a double takes two constant pool entries. See JVMS §4.4.5.
                CONSTANT_LONG, CONSTANT_DOUBLE -> { skipBytes(8); index++ }
                CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_METHOD_TYPE, CONSTANT_MODULE,
                CONSTANT_PACKAGE,
                    -> skipBytes(2)
                CONSTANT_METHOD_HANDLE -> skipBytes(3)
                else -> error("Unknown constant pool tag $tag")
            }
            index++
        }
        return false
    }

    private companion object {
        const val CONSTANT_UTF8 = 1
        const val CONSTANT_INTEGER = 3
        const val CONSTANT_FLOAT = 4
        const val CONSTANT_LONG = 5
        const val CONSTANT_DOUBLE = 6
        const val CONSTANT_CLASS = 7
        const val CONSTANT_STRING = 8
        const val CONSTANT_FIELD_REF = 9
        const val CONSTANT_METHOD_REF = 10
        const val CONSTANT_INTERFACE_METHOD_REF = 11
        const val CONSTANT_NAME_AND_TYPE = 12
        const val CONSTANT_METHOD_HANDLE = 15
        const val CONSTANT_METHOD_TYPE = 16
        const val CONSTANT_DYNAMIC = 17
        const val CONSTANT_INVOKE_DYNAMIC = 18
        const val CONSTANT_MODULE = 19
        const val CONSTANT_PACKAGE = 20
    }
}
