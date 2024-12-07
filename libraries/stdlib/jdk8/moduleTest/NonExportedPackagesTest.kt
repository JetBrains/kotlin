/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package test.jdk9

import org.objectweb.asm.*
import java.io.File
import java.lang.module.ModuleDescriptor
import java.util.jar.JarFile
import kotlin.test.*

class NonExportedPackagesTest {

    @Test
    fun stdlib() {
        checkNonExportedPackages("kotlin-stdlib", setOf(
            "kotlin.collections.builders",
            "kotlin.js",
            "kotlin.jvm.internal.unsafe",
            "kotlin.internal.jdk7",
            "kotlin.internal.jdk8",
            "kotlin.random.jdk8",
        ))
    }

    @Test
    fun stdlibJdk7() {
        checkNonExportedPackages("kotlin-stdlib-jdk7", setOf())
    }

    @Test
    fun stdlibJdk8() {
        checkNonExportedPackages("kotlin-stdlib-jdk8", setOf())
    }

    private fun checkNonExportedPackages(jarShortName: String, expectedPackages: Set<String>) {
        val file = findJar(jarShortName)
        JarFile(file).use { jar ->
            val moduleInfoEntry = jar.getJarEntry("META-INF/versions/9/module-info.class") ?: error("module-info is not found in $file")
            val descriptor = jar.getInputStream(moduleInfoEntry).use { infoStream -> ModuleDescriptor.read(infoStream) }
            val packages = mutableSetOf<String>()

            for (entry in jar.entries()) {
                if (entry.isDirectory) continue
                val name = entry.name
                if (name == moduleInfoEntry.name) continue
                if (name.endsWith(".class", ignoreCase = true) &&
                    (!name.startsWith("META-INF", ignoreCase = true)) ||
                    name.startsWith("META-INF/versions", ignoreCase = true)
                ) {
                    jar.getInputStream(entry).use { classStream ->
                        val visitor = ClassFqnVisitor()
                        ClassReader(classStream).accept(visitor, ClassReader.SKIP_CODE)
                        visitor.fqname?.run { substringBeforeLast('/').replace('/', '.') }?.let(packages::add)
                    }
                }
            }

            val nonExported = packages - descriptor.exports().filter { it.targets().isEmpty() }.map { it.source() }
            assertEquals(expectedPackages, nonExported)
        }
    }



    private fun findJar(shortName: String): File {
        val jars = System.getProperty("stdlibJars").split(File.pathSeparator)
        return jars.map(::File).single { it.name.matches(Regex("""${Regex.escape(shortName)}(?!-[a-z]).+\.jar""")) }
    }

    private class ClassFqnVisitor : ClassVisitor(Opcodes.ASM9) {
        var fqname: String? = null

        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
            super.visit(version, access, name, signature, superName, interfaces)
            fqname = name
        }
    }
}