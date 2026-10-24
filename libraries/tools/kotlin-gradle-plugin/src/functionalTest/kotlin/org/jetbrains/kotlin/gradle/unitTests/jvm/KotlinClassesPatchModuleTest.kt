/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.jvm

import org.gradle.api.Project
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinClassesPatchModuleArgumentProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinClassesPatchModuleArgumentProvider.Companion.isModulePatched
import org.jetbrains.kotlin.gradle.plugin.KotlinClassesPatchModuleArgumentProvider.Companion.parseJavaModuleName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KotlinClassesPatchModuleTest {

    @Test
    fun javaModuleIsPatchedWithKotlinClasses() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()

        assertEquals(
            listOf("--patch-module", "com.example.app=${project.kotlinClassesDir("compileKotlin")}"),
            project.javaCompile("compileJava").options.allCompilerArgs,
        )
    }

    @Test
    fun testJavaModuleIsPatchedWithKotlinTestClasses() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/test/java", "open module com.example.app.test {}")
        project.evaluate()

        assertEquals(
            listOf("--patch-module", "com.example.app.test=${project.kotlinClassesDir("compileTestKotlin")}"),
            project.javaCompile("compileTestJava").options.allCompilerArgs,
        )
        assertEquals(emptyList(), project.javaCompile("compileJava").patchModuleProviders())
    }

    @Test
    fun noProviderWithoutModuleInfo() {
        val project = buildProjectWithJvm()
        project.evaluate()

        assertEquals(emptyList(), project.javaCompile("compileJava").patchModuleProviders())
    }

    @Test
    fun nothingIsPatchedWithoutKotlinClasses() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()

        assertEquals(emptyList(), project.javaCompile("compileJava").options.allCompilerArgs)
    }

    @Test
    fun inputsAreDeclared() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()
        val kotlinClassesDir = project.kotlinClassesDir("compileKotlin")
        val provider = project.javaCompile("compileJava").options.compilerArgumentProviders
            .filterIsInstance<KotlinClassesPatchModuleArgumentProvider>().single()

        assertEquals(setOf(project.file("src/main/java/module-info.java")), provider.moduleInfo.files)
        assertEquals(setOf(project.file(kotlinClassesDir)), provider.kotlinClasses.files)
        val providerClass = KotlinClassesPatchModuleArgumentProvider::class.java
        assertTrue(providerClass.getMethod("getModuleInfo").isAnnotationPresent(InputFiles::class.java))
        assertTrue(providerClass.getMethod("getKotlinClasses").isAnnotationPresent(CompileClasspath::class.java))
    }

    @Test
    fun nothingIsPatchedWithSharedDestinationDirectory() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()
        val compileJava = project.javaCompile("compileJava")
        (project.tasks.getByName("compileKotlin") as KotlinCompile).destinationDirectory.set(compileJava.destinationDirectory)
        project.kotlinClassesDir("compileKotlin")

        assertEquals(emptyList(), compileJava.options.allCompilerArgs)
    }

    @Test
    fun patchModuleInArgumentFileIsKept() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()
        project.kotlinClassesDir("compileKotlin")
        val argumentFile = project.file("javac.args").apply { writeText("--patch-module \"com.example.app=custom\"\n") }
        val compileJava = project.javaCompile("compileJava")
        compileJava.options.compilerArgs.add("@$argumentFile")

        assertEquals(listOf("@$argumentFile"), compileJava.options.allCompilerArgs)
    }

    @Test
    fun immutableCompilerArgsAreKept() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()
        val compileJava = project.javaCompile("compileJava")
        compileJava.options.compilerArgs = listOf("-Xlint:all")

        assertEquals(
            listOf("-Xlint:all", "--patch-module", "com.example.app=${project.kotlinClassesDir("compileKotlin")}"),
            compileJava.options.allCompilerArgs,
        )
    }

    @Test
    fun patchModuleInCompilerArgsIsKept() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()
        project.kotlinClassesDir("compileKotlin")
        val compileJava = project.javaCompile("compileJava")
        compileJava.options.compilerArgs.addAll(listOf("--patch-module", "com.example.app=custom"))

        assertEquals(listOf("--patch-module", "com.example.app=custom"), compileJava.options.allCompilerArgs)
    }

    @Test
    fun patchModuleFromAnotherArgumentProviderIsKept() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.evaluate()
        project.kotlinClassesDir("compileKotlin")
        val compileJava = project.javaCompile("compileJava")
        compileJava.options.compilerArgumentProviders.add(CommandLineArgumentProvider { listOf("--patch-module=com.example.app=custom") })

        assertEquals(listOf("--patch-module=com.example.app=custom"), compileJava.options.allCompilerArgs)
    }

    @Test
    fun replacedJavaSourcesAreRead() {
        val project = buildProjectWithJvm()
        project.writeModuleInfo("src/main/java", "module com.example.app {}")
        project.writeModuleInfo("src/other/java", "module com.example.other {}")
        project.evaluate()
        val compileJava = project.javaCompile("compileJava")
        compileJava.setSource(project.file("src/other/java"))

        assertEquals(
            listOf("--patch-module", "com.example.other=${project.kotlinClassesDir("compileKotlin")}"),
            compileJava.options.allCompilerArgs,
        )
    }

    @Test
    fun multiplatformJvmModuleIsPatchedWithKotlinClasses() {
        val project = buildProjectWithMPP {
            kotlin { jvm() }
        }
        project.writeModuleInfo("src/jvmMain/java", "module com.example.mpp {}")
        project.evaluate()

        val compilation = project.multiplatformExtension.jvm().compilations.getByName("main")
        assertEquals(
            listOf("--patch-module", "com.example.mpp=${project.kotlinClassesDir(compilation.compileKotlinTaskName)}"),
            compilation.compileJavaTaskProvider!!.get().options.allCompilerArgs,
        )
    }

    // Each expected name is the one javac 25 compiles into module-info.class for the same text
    @Test
    fun moduleNameIsParsed() {
        assertEquals("a.b", parseJavaModuleName("module a.b {}"))
        assertEquals("a.b", parseJavaModuleName("open module a.b{\n}"))
        assertEquals(
            "a.b",
            parseJavaModuleName(
                """
                import java.lang.Deprecated;
                // module commented.out {}
                /* module commented.out {} */
                @Deprecated(since = "module x {")
                module a.b {
                    requires transitive java.logging;
                }
                """.trimIndent()
            )
        )

        // Unicode escapes (JLS 3.3)
        assertEquals("a.b", parseJavaModuleName("module a.\\u0062 {}"))
        assertEquals("a.b", parseJavaModuleName("module a.\\uuuu0062 {}"))
        assertEquals("caf\u00e9", parseJavaModuleName("module caf\\u00e9 {}"))
        assertEquals("app", parseJavaModuleName("\\u002f* module x { *\\u002f \\u006dodule app {}"))
        assertEquals("app", parseJavaModuleName("@Deprecated(since = \"\\\\u0022 module impostor { //\")\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("// \\u005cu000a module x {\nmodule app {}"))

        // Line terminators and white space (JLS 3.4, 3.6)
        assertEquals("a.b", parseJavaModuleName("// comment\rmodule a.b {}"))
        assertEquals("app", parseJavaModuleName("// module x {\r\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("module\u000Capp {}"))

        // Comments (JLS 3.7)
        assertEquals("a.b", parseJavaModuleName("module a . /* c */ b {}"))
        assertEquals("app", parseJavaModuleName("/*/ module x { */ module app {}"))
        assertEquals("app", parseJavaModuleName("/* /* */ module app {}"))
        assertEquals("app", parseJavaModuleName("/* // */ module app {}"))

        // Identifiers and contextual keywords (JLS 3.8, 3.9)
        assertEquals("caf\u00e9.b", parseJavaModuleName("module caf\u00e9.b {}"))
        assertEquals("a\$.b_c", parseJavaModuleName("module a\$.b_c {}"))
        assertEquals("ab", parseJavaModuleName("module a\\u0000b {}"))
        assertEquals("open.module", parseJavaModuleName("module open.module {}"))
        assertEquals("module", parseJavaModuleName("open module module {}"))

        // Character, string, and text block literals (JLS 3.10.4 to 3.10.6)
        assertEquals("app", parseJavaModuleName("@SuppressWarnings({\"\" + '\"', \"module  {\"})\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@SuppressWarnings({\"a\" + '\\''})\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@Deprecated(since = \"// /* module x {\")\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@Deprecated(since = \"\"\"\n  module x {\n  \"\"\")\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@Deprecated(since = \"\"\"\n  \\\"\"\" module x {\n  \"\"\")\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@Deprecated(since = \"\"\"\n  \"\" module x {\n  \"\"\")\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@Deprecated(since = \"\"\"\n  a \\\n  module x {\n  \"\"\")\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@Deprecated(since = \"\"\"\n  ends with a backslash \\\\\"\"\")\nmodule app {}"))

        // Imports and annotation element values before the declaration (JLS 7.5, 9.7.1)
        assertEquals("app", parseJavaModuleName("import module java.base;\nmodule app {}"))
        assertEquals("app", parseJavaModuleName("@SuppressWarnings({})\nmodule app {}"))
        assertEquals(
            "app",
            parseJavaModuleName(
                "@SuppressWarnings({\"a\", \"module x {\" + \"b\"})\n@Deprecated(since = \"1\", forRemoval = false)\nmodule app {}"
            )
        )
    }

    @Test
    fun noModuleNameWithoutDeclaration() {
        assertNull(parseJavaModuleName("module {}"))
        assertNull(parseJavaModuleName("// module a.b {}"))
        assertNull(parseJavaModuleName("/* module a.b {} */"))
        assertNull(parseJavaModuleName("@Deprecated(since = \"module a.b {}\")"))
    }

    @Test
    fun patchModuleArgumentsAreRecognized() {
        assertTrue(isModulePatched("a", listOf("--patch-module", "a=dir")))
        assertTrue(isModulePatched("a", listOf("-Xlint", "--patch-module=a=dir")))
        assertFalse(isModulePatched("a", listOf("--patch-module", "a.b=dir")))
        assertFalse(isModulePatched("a", listOf("--patch-module")))
        assertFalse(isModulePatched("a", listOf("a=dir")))
    }

    private fun Project.writeModuleInfo(sourceDir: String, text: String) {
        file(sourceDir).mkdirs()
        file("$sourceDir/module-info.java").writeText(text)
    }

    private fun Project.javaCompile(name: String) = tasks.getByName(name) as JavaCompile

    private fun JavaCompile.patchModuleProviders() =
        options.compilerArgumentProviders.filterIsInstance<KotlinClassesPatchModuleArgumentProvider>()


    // Creates the directory, as compileKotlin does when it has sources
    private fun Project.kotlinClassesDir(kotlinCompileName: String): String =
        (tasks.getByName(kotlinCompileName) as KotlinCompile).destinationDirectory.get().asFile.apply { mkdirs() }.absolutePath

}
