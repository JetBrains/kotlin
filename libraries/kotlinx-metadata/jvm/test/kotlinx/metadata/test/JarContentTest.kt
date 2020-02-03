/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.ASM4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.text.Charsets.UTF_8

class JarContentTest {
    @Test
    fun testJarContents() {
        // println("Patterns: $PREDEFINED_STRINGS")
        LIBS_PATH.toFile().walk().filter { it.name.endsWith(JAR_EXT) }.forEach(::checkJarContents)
    }

    private fun checkJarContents(jar: File) {
        val zipFile = ZipFile(jar, UTF_8)
        zipFile.entries().asSequence().filter { it.name.endsWith(CLASS_EXT) }.forEach {
            val loadedConstants = mutableListOf<Any>()
            val classReader = ClassReader(zipFile.getInputStream(it))
            classReader.accept(object : ClassVisitor(ASM4) {
                override fun visitMethod(p0: Int, p1: String?, p2: String?, p3: String?, p4: Array<out String>?): MethodVisitor {
                    return object : MethodVisitor(ASM4) {
                        override fun visitLdcInsn(cst: Any?) {
                            if (cst != null) loadedConstants.add(cst)
                        }
                    }
                }
            }, SKIP_FRAMES)

            for (constant in loadedConstants) {
                if (constant !is String || constant.isNullOrEmpty()) {
                    continue
                }
                // println("checking: $constant")
                // Explicitly checking types that are programmatically built don't appear as string constants.
                assertNull("$constant found at ${it.name}", findPattern(constant))
                // Implicitly checking none of string constants starts with "kotlin/" prefix, just in case.
                assertFalse("$constant found at ${it.name}", constant.startsWith("kotlin/"))
            }
        }
    }

    private fun findPattern(constant: String): String? {
        return PREDEFINED_STRINGS.find { it in constant }
    }

    companion object {
        const val JAR_EXT = ".jar"
        const val CLASS_EXT = ".class"
        val LIBS_PATH: Path = Paths.get("build", "libs")

        private val INTERNAL_COMPANIONS =
            listOf("Char", "Byte", "Short", "Int", "Float", "Long", "Double", "String", "Enum")
                .map { "kotlin/jvm/internal/${it}CompanionObject" }
        val PREDEFINED_STRINGS =
            JvmNameResolver.PREDEFINED_STRINGS + listOf("kotlin/jvm/functions", "kotlin/reflect/KFunction") + INTERNAL_COMPANIONS
    }
}
