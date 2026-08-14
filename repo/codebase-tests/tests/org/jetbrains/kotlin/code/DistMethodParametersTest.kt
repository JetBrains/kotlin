/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries

/**
 * A modern `javac` writes a `MethodParameters` attribute for parameters that carry a flag worth recording —
 * the mandated outer instance of an inner class constructor, the parameters of a bridge method — even under
 * `--release 8`, and even though nothing in this build asks for parameter names. `javac` 8 wrote nothing.
 *
 * Two tools this repository still depends on cannot read those entries, because they have no name
 * (`name_index = 0`, legal per JVMS §4.7.24):
 *
 * * the D8 that `dex-member-list` brings along for `dexMethodCount` fails with a bare `NullPointerException`;
 * * JDK 8 before 8u4xx turns the missing name into `""` rather than `null`, so `getParameters()` throws
 *   `MalformedParametersException` — which breaks every test task still running on JDK 8, via JUnit's
 *   reflection over `@Nested` class constructors.
 *
 * `stripMethodParameters` removes the attribute from all `javac` output, so the distribution should carry
 * none. This test is what keeps that true: both failures above were multi-hour investigations that pointed
 * nowhere near the cause, and a class arriving in the distribution from outside a `JavaCompile` task would
 * bypass the stripper entirely.
 */
class DistMethodParametersTest {

    private val distLibraries = Path("dist/kotlinc/lib")

    @Test
    fun `distribution carries no MethodParameters attribute`() {
        val jars = distLibraries.listDirectoryEntries("*.jar").sorted()
        if (jars.isEmpty()) fail("No jar found in '$distLibraries'. This test has to run after the ':dist' task.")

        val offenders = jars.flatMap { jar ->
            ZipFile(jar.toFile()).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.endsWith(".class") }
                    .mapNotNull { entry ->
                        val node = ClassNode()
                        zip.getInputStream(entry).use { ClassReader(it.readBytes()) }.accept(node, ClassReader.SKIP_CODE)
                        // `MethodNode.parameters` is the `MethodParameters` attribute, and stays null without one.
                        // The attribute itself has to be checked rather than the constant pool: `stripMethodParameters`
                        // leaves the now-unused `MethodParameters` string behind, so the pool is not evidence.
                        val methods = node.methods.orEmpty().filter { !it.parameters.isNullOrEmpty() }
                        if (methods.isEmpty()) null
                        else "${jar.fileName}!/${entry.name}: ${methods.joinToString { it.name }}"
                    }
                    .toList()
            }
        }

        if (offenders.isNotEmpty()) {
            fail(
                "${offenders.size} class file(s) of the distribution carry a `MethodParameters` attribute. It breaks " +
                        "`dexMethodCount` and `getParameters()` on older JDK 8 builds — see the KDoc of this test. If " +
                        "these come from a Gradle `JavaCompile` task, `stripMethodParameters` should have removed it; " +
                        "otherwise the producing step needs the same treatment:\n\n" +
                        offenders.sorted().joinToString("\n").prependIndent("  ")
            )
        }
    }
}
