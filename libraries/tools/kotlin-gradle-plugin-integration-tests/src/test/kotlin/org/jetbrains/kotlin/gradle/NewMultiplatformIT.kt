/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.testbase.MPPNativeTargets
import org.jetbrains.kotlin.gradle.testbase.TestVersions
import org.jetbrains.kotlin.gradle.testbase.assertHasDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertNoDiagnostic
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KLIB_PROPERTY_SHORT_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.junit.Assert
import org.junit.Test
import java.util.*
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.test.*

open class NewMultiplatformIT : BaseGradleIT() {
    private val gradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    private val nativeHostTargetName = MPPNativeTargets.current

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    private data class HmppFlags(
        val hmppSupport: Boolean,
        val name: String,
    ) {
        override fun toString() = name
    }

    private val noHMPP = HmppFlags(
        name = "No HMPP",
        hmppSupport = false
    )

    private val hmppWoCompatibilityMetadataArtifact = HmppFlags(
        name = "HMPP without Compatibility Metadata Artifact",
        hmppSupport = true
    )

    private val HmppFlags.buildOptions
        get() = defaultBuildOptions().copy(
            hierarchicalMPPStructureSupport = hmppSupport
        )

    @Test
    fun testLibAndApp() = doTestLibAndApp(
        "sample-lib",
        "sample-app",
        hmppWoCompatibilityMetadataArtifact
    )

    @Test
    fun testLibAndAppWithoutHMPP() = doTestLibAndApp(
        "sample-lib",
        "sample-app",
        noHMPP
    )

    @Test
    fun testLibAndAppWithGradleKotlinDsl() = doTestLibAndApp(
        "sample-lib-gradle-kotlin-dsl",
        "sample-app-gradle-kotlin-dsl",
        hmppWoCompatibilityMetadataArtifact
    )

    private fun doTestLibAndApp(
        libProjectName: String,
        appProjectName: String,
        hmppFlags: HmppFlags,
    ) {
        val libProject = transformNativeTestProjectWithPluginDsl(libProjectName, directoryPrefix = "new-mpp-lib-and-app")
        val appProject = transformNativeTestProjectWithPluginDsl(appProjectName, directoryPrefix = "new-mpp-lib-and-app")

        val buildOptions = hmppFlags.buildOptions
        val compileTasksNames =
            listOf("Jvm6", "NodeJs", "Linux64").map { ":compileKotlin$it" }

        with(libProject) {
            build(
                "publish",
                options = buildOptions
            ) {
                assertSuccessful()
                assertTasksExecuted(*compileTasksNames.toTypedArray(), ":jvm6Jar", ":nodeJsJar", ":compileCommonMainKotlinMetadata")

                val groupDir = projectDir.resolve("repo/com/example")
                val jvmJarName = "sample-lib-jvm6/1.0/sample-lib-jvm6-1.0.jar"
                val jsExtension = "klib"
                val jsKlibName = "sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.$jsExtension"
                val metadataJarName = "sample-lib/1.0/sample-lib-1.0.jar"
                val nativeKlibName = "sample-lib-linux64/1.0/sample-lib-linux64-1.0.klib"

                listOf(jvmJarName, jsKlibName, metadataJarName, "sample-lib/1.0/sample-lib-1.0.module").forEach {
                    Assert.assertTrue("$it should exist", groupDir.resolve(it).exists())
                }

                val gradleMetadata = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module").readText()
                assertFalse(gradleMetadata.contains(ProjectLocalConfigurations.ATTRIBUTE.name))

                listOf(jvmJarName, jsKlibName, nativeKlibName).forEach {
                    val pom = groupDir.resolve(it.replaceAfterLast('.', "pom"))
                    Assert.assertTrue(
                        "$pom should contain a name section.",
                        pom.readText().contains("<name>Sample MPP library</name>")
                    )
                    Assert.assertFalse(
                        "$pom should not contain standard K/N libraries as dependencies.",
                        pom.readText().contains("<groupId>Kotlin/Native</groupId>")
                    )
                }

                val jvmJarEntries = ZipFile(groupDir.resolve(jvmJarName)).entries().asSequence().map { it.name }.toSet()
                Assert.assertTrue("com/example/lib/CommonKt.class" in jvmJarEntries)
                Assert.assertTrue("com/example/lib/MainKt.class" in jvmJarEntries)

                Assert.assertTrue(groupDir.resolve(jsKlibName).exists())

                Assert.assertTrue(groupDir.resolve(nativeKlibName).exists())
            }
        }

        val libLocalRepoUri = libProject.projectDir.resolve("repo").toURI()

        with(appProject) {
            setupWorkingDir(false)

            // we use `maven { setUrl(...) }` because this syntax actually works both for Groovy and Kotlin DSLs in Gradle
            gradleBuildScript().appendText("\nrepositories { maven { setUrl(\"$libLocalRepoUri\") } }")

            fun CompiledProject.checkAppBuild() {
                assertSuccessful()
                assertTasksExecuted(*compileTasksNames.toTypedArray())

                projectDir.resolve(targetClassesDir("jvm6")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/UseBothIdsKt.class").exists())
                }

                projectDir.resolve(targetClassesDir("jvm8")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/UseBothIdsKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/Jdk8ApiUsageKt.class").exists())
                }

                val nativeExeName = if (isWindows) "main.exe" else "main.kexe"
                assertFileExists("build/bin/linux64/mainDebugExecutable/$nativeExeName")

                // Check that linker options were correctly passed to the K/N compiler.
                withNativeCommandLineArguments(":linkMainDebugExecutableLinux64") { arguments ->
                    val parsedArguments = parseCommandLineArguments<K2NativeCompilerArguments>(arguments)
                    assertEquals(listOf("-L."), parsedArguments.singleLinkerArguments?.toList())
                    assertTrue(arguments.containsSequentially("-linker-option", "-L."))
                }
            }

            build(
                "assemble",
                "resolveRuntimeDependencies",
                options = buildOptions
            ) {
                checkAppBuild()
                assertTasksExecuted(":resolveRuntimeDependencies") // KT-26301
            }

            // Now run again with a project dependency instead of a module one:
            libProject.projectDir.copyRecursively(projectDir.resolve(libProject.projectDir.name))
            gradleSettingsScript().appendText("\ninclude(\"${libProject.projectDir.name}\")")
            gradleBuildScript().modify { it.replace("\"com.example:sample-lib:1.0\"", "project(\":${libProject.projectDir.name}\")") }

            gradleBuildScript(libProjectName).takeIf { it.extension == "kts" }?.modify {
                it.replace(Regex("""\.version\(.*\)"""), "")
            }

            build(
                "clean",
                "assemble",
                "--rerun-tasks",
                options = buildOptions
            ) {
                checkAppBuild()
            }
        }
    }

    @Test
    fun testLibAndAppJsIr() = doTestLibAndAppJsBothCompilers(
        "sample-lib",
        "sample-app",
    )

    @Test
    fun testLibAndAppWithGradleKotlinDslJsIr() = doTestLibAndAppJsBothCompilers(
        "sample-lib-gradle-kotlin-dsl",
        "sample-app-gradle-kotlin-dsl",
    )

    private fun doTestLibAndAppJsBothCompilers(
        libProjectName: String,
        appProjectName: String,
    ) {
        val libProject = transformProjectWithPluginsDsl(libProjectName, directoryPrefix = "both-js-lib-and-app")
        val appProject = transformProjectWithPluginsDsl(appProjectName, directoryPrefix = "both-js-lib-and-app")

        @Suppress("DEPRECATION")
        val compileTasksNames = listOf(":compileKotlinNodeJs")

        with(libProject) {
            gradleProperties().appendText(
                """
                
                kotlin.compiler.execution.strategy=in-process
                """.trimIndent()
            )
            build(
                "publish",
                options = defaultBuildOptions()
            ) {
                assertSuccessful()
                assertTasksNotExecuted(":compileCommonMainKotlinMetadata")
                assertTasksExecuted(*compileTasksNames.toTypedArray(), ":allMetadataJar")

                val groupDir = projectDir.resolve("repo/com/example")

                @Suppress("DEPRECATION")
                val jsExtension = "klib"
                val jsJarName = "sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.$jsExtension"
                val metadataJarName = "sample-lib/1.0/sample-lib-1.0.jar"

                listOf(jsJarName, metadataJarName, "sample-lib/1.0/sample-lib-1.0.module").forEach {
                    Assert.assertTrue("$it should exist", groupDir.resolve(it).exists())
                }

                val gradleMetadata = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module").readText()
                assertFalse(gradleMetadata.contains(ProjectLocalConfigurations.ATTRIBUTE.name))

                jsJarName.let {
                    val pom = groupDir.resolve(it.replaceAfterLast('.', "pom"))
                    Assert.assertTrue(
                        "$pom should contain a name section.",
                        pom.readText().contains("<name>Sample MPP library</name>")
                    )
                    Assert.assertFalse(
                        "$pom should not contain standard K/N libraries as dependencies.",
                        pom.readText().contains("<groupId>Kotlin/Native</groupId>")
                    )
                }

                groupDir.resolve(jsJarName).exists()
            }
        }

        val libLocalRepoUri = libProject.projectDir.resolve("repo").toURI()

        with(appProject) {
            setupWorkingDir()

            gradleProperties().appendText(
                """
                
                kotlin.compiler.execution.strategy=in-process
                """.trimIndent()
            )

            // we use `maven { setUrl(...) }` because this syntax actually works both for Groovy and Kotlin DSLs in Gradle
            gradleBuildScript().appendText("\nrepositories { maven { setUrl(\"$libLocalRepoUri\") } }")

            fun CompiledProject.checkAppBuild() {
                assertSuccessful()

                assertTasksExecuted(compileTasksNames)
            }

            build(
                "assemble",
                options = defaultBuildOptions()
            ) {
                checkAppBuild()
            }

            // Now run again with a project dependency instead of a module one:
            libProject.projectDir.copyRecursively(projectDir.resolve(libProject.projectDir.name))
            gradleSettingsScript().appendText("\ninclude(\"${libProject.projectDir.name}\")")
            gradleBuildScript().modify { it.replace("\"com.example:sample-lib:1.0\"", "project(\":${libProject.projectDir.name}\")") }

            gradleBuildScript(libProjectName).takeIf { it.extension == "kts" }?.modify {
                it.replace(Regex("""\.version\(.*\)"""), "")
            }

            build(
                "clean",
                "assemble",
                "--rerun-tasks",
                options = defaultBuildOptions()
            ) {
                checkAppBuild()
            }
        }
    }

    @Test
    fun testMavenPublishAppliedBeforeMultiplatformPlugin() =
        with(transformNativeTestProject("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
            gradleBuildScript().modify { "apply plugin: 'maven-publish'\n$it" }

            build {
                assertSuccessful()
            }
        }

    @Test
    fun testResourceProcessing() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        val targetsWithResources = listOf("jvm6", "nodeJs", "linux64")
        val processResourcesTasks =
            targetsWithResources.map { ":${it}ProcessResources" }

        build(*processResourcesTasks.toTypedArray()) {
            assertSuccessful()
            assertTasksExecuted(*processResourcesTasks.toTypedArray())

            targetsWithResources.forEach {
                assertFileExists("build/processedResources/$it/main/commonMainResource.txt")
                assertFileExists("build/processedResources/$it/main/${it}MainResource.txt")
            }
        }
    }

    override val defaultGradleVersion: GradleVersionRequired
        get() = gradleVersion

    @Test
    fun testSourceSetCyclicDependencyDetection() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()
        gradleBuildScript().appendText(
            "\n" + """
            kotlin.sourceSets {
                a
                b { dependsOn a }
                c { dependsOn b }
                a.dependsOn(c)
            }
        """.trimIndent()
        )

        build("assemble") {
            assertFailed()
            assertContains("a -> c -> b -> a")
        }
    }

    @Test
    @Ignore // KT-60745
    fun testJvmWithJavaEquivalence() = doTestJvmWithJava(testJavaSupportInJvmTargets = false)

    @Test
    fun testJavaSupportInJvmTargets() = doTestJvmWithJava(testJavaSupportInJvmTargets = true)

    private fun doTestJvmWithJava(testJavaSupportInJvmTargets: Boolean) =
        with(Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
            val embeddedProject = Project("sample-lib-gradle-kotlin-dsl", directoryPrefix = "new-mpp-lib-and-app")
            embedProject(embeddedProject)
            gradleProperties().apply {
                configureJvmMemory()
            }

            lateinit var classesWithoutJava: Set<String>

            fun getFilePathsSet(inDirectory: String): Set<String> {
                val dir = projectDir.resolve(inDirectory)
                return dir.walk().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }.toSet()
            }

            gradleBuildScript(embeddedProject.projectName).modify {
                it.replace("val shouldBeJs = true", "val shouldBeJs = false")
            }

            gradleBuildScript().modify {
                it.replace("def shouldBeJs = true", "def shouldBeJs = false")
            }

            build("assemble") {
                assertSuccessful()
                classesWithoutJava = getFilePathsSet("build/classes")
            }

            gradleBuildScript().modify {
                """
                    apply plugin: 'com.github.johnrengelman.shadow'
                    apply plugin: 'application'
                    apply plugin: 'kotlin-kapt' // Check that Kapts works, generates and compiles sources
                """.trimIndent() + if (testJavaSupportInJvmTargets) {
                    it + "\nkotlin.jvm(\"jvm6\") { " +
                            "${
                                KotlinJvmTarget::withJava.name.plus("();").repeat(2)
                            } " + // also check that the function is idempotent
                            "}"
                } else {
                    it.replace("presets.jvm", "presets.jvmWithJava").replace("jvm(", "targetFromPreset(presets.jvmWithJava, ")
                }.plus(
                    "\n" + """
                    buildscript {
                        repositories {
                            maven { url 'https://plugins.gradle.org/m2/' }
                        }
                        dependencies {
                            classpath 'com.github.johnrengelman:shadow:${TestVersions.ThirdPartyDependencies.SHADOW_PLUGIN_VERSION}'
                        }
                    }
                    
                    application {
                        mainClass = 'com.example.lib.CommonKt'
                    }
                    
                    dependencies {
                        jvm6MainImplementation("com.google.dagger:dagger:2.24")
                        kapt("com.google.dagger:dagger-compiler:2.24")
                        kapt(project(":sample-lib-gradle-kotlin-dsl"))
                        
                        // also check incremental Kapt class structure configurations, KT-33105
                        jvm6MainImplementation(project(":sample-lib-gradle-kotlin-dsl")) 
                    }
                    """.trimIndent()
                )
            }
            // also check incremental Kapt class structure configurations, KT-33105
            projectDir.resolve("gradle.properties").appendText("\nkapt.incremental.apt=true")

            // Check Kapt:
            projectDir.resolve("src/jvm6Main/kotlin/Main.kt").appendText(
                "\n" + """
                interface Iface
                
                @dagger.Module
                object Module {
                    @JvmStatic @dagger.Provides
                    fun provideHeater(): Iface = object : Iface { }
                }
            """.trimIndent()
            )

            fun javaSourceRootForCompilation(compilationName: String) =
                if (testJavaSupportInJvmTargets) "src/jvm6${compilationName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}/java" else "src/$compilationName/java"

            val javaMainSrcDir = javaSourceRootForCompilation("main")
            val javaTestSrcDir = javaSourceRootForCompilation("test")

            projectDir.resolve(javaMainSrcDir).apply {
                mkdirs()
                // Check that Java can access the dependencies (kotlin-stdlib):
                resolve("JavaClassInJava.java").writeText(
                    """
                    package com.example.lib;
                    import kotlin.sequences.Sequence;
                    class JavaClassInJava {
                        Sequence<String> makeSequence() { throw new UnsupportedOperationException(); }
                    }
                    """.trimIndent()
                )

                // Add a Kotlin source file in the Java source root and check that it is compiled:
                resolve("KotlinClassInJava.kt").writeText(
                    """
                    package com.example.lib
                    class KotlinClassInJava
                    """.trimIndent()
                )
            }

            projectDir.resolve(javaTestSrcDir).apply {
                mkdirs()
                resolve("JavaTest.java").writeText(
                    """
                    package com.example.lib;
                    import org.junit.*;
                    public class JavaTest {
                        @Test
                        public void testAccessKotlin() {
                            MainKt.expectedFun();
                            MainKt.x();
                            new KotlinClassInJava();
                            new JavaClassInJava();
                        }
                    }
                    """.trimIndent()
                )
            }

            build(
                "clean", "build", "run", "shadowJar",
                options = defaultBuildOptions().suppressDeprecationWarningsOn("KT-66542: withJava() produces deprecation warning") {
                    GradleVersion.version(chooseWrapperVersionOrFinishTest()) >= GradleVersion.version(TestVersions.Gradle.G_8_7)
                }
            ) {
                assertSuccessful()
                val expectedMainClasses =
                    classesWithoutJava + setOf(
                        // classes for Kapt test:
                        "java/main/com/example/lib/Module_ProvideHeaterFactory.class",
                        "kotlin/jvm6/main/com/example/lib/Module\$provideHeater\$1.class",
                        "kotlin/jvm6/main/com/example/lib/Iface.class",
                        "kotlin/jvm6/main/com/example/lib/Module.class",
                        // other added classes:
                        "kotlin/jvm6/main/com/example/lib/KotlinClassInJava.class",
                        "java/main/com/example/lib/JavaClassInJava.class",
                        "java/test/com/example/lib/JavaTest.class"
                    )
                val actualClasses = getFilePathsSet("build/classes")
                Assert.assertEquals(expectedMainClasses, actualClasses)

                val jvmTestTaskName = if (testJavaSupportInJvmTargets) "jvm6Test" else "test"
                assertTasksExecuted(":$jvmTestTaskName")

                if (testJavaSupportInJvmTargets) {
                    assertFileExists("build/reports/tests/allTests/classes/com.example.lib.JavaTest.html")
                }

                if (testJavaSupportInJvmTargets) {
                    assertNoDiagnostic(KotlinToolingDiagnostics.DeprecatedJvmWithJavaPresetDiagnostic)
                } else {
                    assertHasDiagnostic(KotlinToolingDiagnostics.DeprecatedJvmWithJavaPresetDiagnostic)
                }

                assertTasksExecuted(":run")
                assertContains(">>> Common.kt >>> main()")

                assertTasksExecuted(":shadowJar")
                val entries = ZipFile(projectDir.resolve("build/libs/sample-lib-1.0-all.jar")).use { zip ->
                    zip.entries().asSequence().map { it.name }.toSet()
                }
                assertTrue { "kotlin/Pair.class" in entries }
                assertTrue { "com/example/lib/CommonKt.class" in entries }
                assertTrue { "com/example/lib/MainKt.class" in entries }
                assertTrue { "com/example/lib/JavaClassInJava.class" in entries }
                assertTrue { "com/example/lib/KotlinClassInJava.class" in entries }
            }
        }

    private val targetName = when (HostManager.host) {
        KonanTarget.LINUX_X64 -> "linux64"
        KonanTarget.MACOS_X64 -> "macos64"
        KonanTarget.MACOS_ARM64 -> "macosArm64"
        KonanTarget.MINGW_X64 -> "mingw64"
        else -> fail("Unsupported host")
    }

    @Test
    fun testLibWithTests() = doTestLibWithTests(transformNativeTestProject("new-mpp-lib-with-tests", gradleVersion))

    @Test
    fun testLibWithTestsKotlinDsl() = with(transformNativeTestProject("new-mpp-lib-with-tests", gradleVersion)) {
        gradleBuildScript().delete()
        projectDir.resolve("build.gradle.kts.alternative").renameTo(projectDir.resolve("build.gradle.kts"))
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        doTestLibWithTests(this)
    }

    private fun doTestLibWithTests(project: Project) = with(project) {
        build("check") {
            assertSuccessful()
            assertTasksExecuted(
                // compilation tasks:
                ":compileKotlinJs",
                ":compileTestKotlinJs",
                ":compileKotlinJvmWithoutJava",
                ":compileTestKotlinJvmWithoutJava",
                // test tasks:
                ":jsTest", // does not run any actual tests for now
                ":jvmWithoutJavaTest",
            )

            val expectedKotlinOutputFiles = listOf(
                *kotlinClassesDir(sourceSet = "jvmWithoutJava/main").let {
                    arrayOf(
                        it + "com/example/lib/CommonKt.class",
                        it + "com/example/lib/MainKt.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module"
                    )
                },
                *kotlinClassesDir(sourceSet = "jvmWithoutJava/test").let {
                    arrayOf(
                        it + "com/example/lib/TestCommonCode.class",
                        it + "com/example/lib/TestWithoutJava.class",
                        it + "META-INF/new-mpp-lib-with-tests_test.kotlin_module"
                    )
                }
            )

            expectedKotlinOutputFiles.forEach { assertFileExists(it) }
            val expectedTestResults = projectDir.resolve("TEST-all.xml")

            expectedTestResults.replaceText("<target>", targetName)

            assertTestResults(
                expectedTestResults,
                "jsNodeTest",
                "${targetName}Test"
            )
        }
    }

    @Test
    fun testLanguageSettingsClosureForKotlinDsl() =
        with(transformNativeTestProjectWithPluginDsl("sample-lib-gradle-kotlin-dsl", gradleVersion, "new-mpp-lib-and-app")) {
            gradleBuildScript().appendText(
                "\n" + """
            kotlin.sourceSets.all {
                languageSettings {
                    languageVersion = "1.4"
                    apiVersion = "1.4" 
                }
            }
        """.trimIndent()
            )

            listOf("compileCommonMainKotlinMetadata", "compileKotlinJvm6", "compileKotlinNodeJs").forEach {
                build(it) {
                    assertSuccessful()
                    assertTasksExecuted(":$it")
                    assertContains("-language-version 1.4", "-api-version 1.4")
                }
            }
        }

    @Test
    fun testLanguageSettingsApplied() = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets.all {
                    it.languageSettings {
                        // languageVersion = '1.4'
                        // apiVersion = '1.4'
                        enableLanguageFeature('InlineClasses')
                        optIn('kotlin.ExperimentalUnsignedTypes')
                        optIn('kotlin.contracts.ExperimentalContracts')
                        progressiveMode = true
                    }
                    project.ext.set("kotlin.mpp.freeCompilerArgsForSourceSet.${'$'}name", ["-Xno-inline"])
                }
            """.trimIndent()
        )

        listOf(
            "compileCommonMainKotlinMetadata", "compileKotlinJvm6", "compileKotlinNodeJs"
        ).forEach {
            build(it) {
                assertSuccessful()
                assertTasksExecuted(":$it")
                assertContains(
                    "-XXLanguage:+InlineClasses",
                    "-progressive",
                    "-opt-in kotlin.ExperimentalUnsignedTypes,kotlin.contracts.ExperimentalContracts",
                    "-Xno-inline"
                )
            }
        }

        listOf("compileNativeMainKotlinMetadata", "compileKotlinLinux64").forEach { task ->
            build(task) {
                assertSuccessful()
                assertTasksExecuted(":$task")
                val arguments = parseCompilerArguments<K2NativeCompilerArguments>()
                assertTrue(arguments.progressiveMode, "Expected progressiveMode")
                assertTrue(arguments.noInline, "Expected no-inline")
                assertEquals(
                    setOf("kotlin.ExperimentalUnsignedTypes", "kotlin.contracts.ExperimentalContracts"),
                    arguments.optIn?.toSet()
                )
                assertContains(
                    "-XXLanguage:+InlineClasses",
                    "-progressive",
                    "-Xno-inline"
                )
            }
        }

        gradleBuildScript().appendText(
            "\n" + """
            kotlin.sourceSets.all {
                it.languageSettings {
                    languageVersion = '1.4'
                    apiVersion = '1.4' 
                }
            }
        """.trimIndent()
        )

        listOf("compileCommonMainKotlinMetadata", "compileKotlinJvm6", "compileKotlinNodeJs").forEach {
            build(it) {
                assertSuccessful()
                assertTasksExecuted(":$it")
                assertContains("-language-version 1.4", "-api-version 1.4")
            }
        }
    }

    @Test
    fun testLanguageSettingsConsistency() = with(
        Project("sample-lib", gradleVersion, "new-mpp-lib-and-app", minLogLevel = LogLevel.INFO)
    ) {
        setupWorkingDir()

        fun testMonotonousCheck(
            sourceSetConfigurationChange: String,
            expectedErrorHint: String,
            initialSetupForSourceSets: String? = null,
        ) {
            if (initialSetupForSourceSets != null) {
                gradleBuildScript().appendText(
                    """
                    |
                    |kotlin.sourceSets.commonMain.${initialSetupForSourceSets}
                    |kotlin.sourceSets.nativeMain.${initialSetupForSourceSets}
                    |kotlin.sourceSets.linux64Main.${initialSetupForSourceSets}
                    |kotlin.sourceSets.macos64Main.${initialSetupForSourceSets}
                    |kotlin.sourceSets.macosArm64Main.${initialSetupForSourceSets}
                    |kotlin.sourceSets.jvm6Main.${initialSetupForSourceSets}
                    |kotlin.sourceSets.mingw64Main.${initialSetupForSourceSets}
                    |kotlin.sourceSets.nodeJsMain.${initialSetupForSourceSets}
                    |kotlin.sourceSets.wasmJsMain.${initialSetupForSourceSets}
                    """.trimMargin()
                )
            }
            gradleBuildScript().appendText("\nkotlin.sourceSets.commonMain.${sourceSetConfigurationChange}")
            build("tasks") {
                assertFailed()
                assertContains(expectedErrorHint)
            }
            gradleBuildScript().appendText(
                """
                |
                |kotlin.sourceSets.nativeMain.${sourceSetConfigurationChange}
                |kotlin.sourceSets.linux64Main.${sourceSetConfigurationChange}
                |kotlin.sourceSets.macos64Main.${sourceSetConfigurationChange}
                |kotlin.sourceSets.macosArm64Main.${sourceSetConfigurationChange}
                |kotlin.sourceSets.jvm6Main.${sourceSetConfigurationChange}
                |kotlin.sourceSets.mingw64Main.${sourceSetConfigurationChange}
                |kotlin.sourceSets.nodeJsMain.${sourceSetConfigurationChange}
                |kotlin.sourceSets.wasmJsMain.${sourceSetConfigurationChange}
                """.trimMargin()
            )
            build("tasks") {
                assertSuccessful()
            }
        }


        testMonotonousCheck(
            "languageSettings.languageVersion = '1.4'",
            "The language version of the dependent source set must be greater than or equal to that of its dependency.",
            "languageSettings.languageVersion = '1.3'",
        )

        testMonotonousCheck(
            "languageSettings.enableLanguageFeature('InlineClasses')",
            "The dependent source set must enable all unstable language features that its dependency has."
        )

        testMonotonousCheck(
            "languageSettings.optIn('kotlin.ExperimentalUnsignedTypes')",
            "The dependent source set must use all opt-in annotations that its dependency uses."
        )

        // check that enabling a bugfix feature and progressive mode or advancing API level
        // don't require doing the same for dependent source sets:
        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets.commonMain.languageSettings {
                    apiVersion = '1.4'
                    enableLanguageFeature('SoundSmartcastForEnumEntries')
                    progressiveMode = true
                }
            """.trimIndent()
        )
        build("tasks") {
            assertSuccessful()
        }
    }

    @Test
    fun testResolveMppLibDependencyToMetadata() {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val appProject = Project("sample-app", gradleVersion, "new-mpp-lib-and-app")

        libProject.build("publish") { assertSuccessful() }
        val localRepo = libProject.projectDir.resolve("repo")
        val localRepoUri = localRepo.toURI()

        with(appProject) {
            setupWorkingDir()

            val pathPrefix = "metadataDependency: "

            gradleBuildScript().appendText(
                "\n" + """
                    repositories { maven { url '$localRepoUri' } }

                    kotlin.sourceSets {
                        commonMain {
                            dependencies {
                                // add these dependencies to check that they are resolved to metadata
                                api 'com.example:sample-lib:1.0'
                                compileOnly 'com.example:sample-lib:1.0'
                            }
                        }
                    }

                    task('printMetadataFiles') {
                        doFirst {
                            def configuration = configurations.getByName("commonMainResolvable" + '$METADATA_CONFIGURATION_NAME_SUFFIX')
                            configuration.files.each { println '$pathPrefix' + configuration.name + '->' + it.name }                            
                        }
                    }
                """.trimIndent()
            )
            val metadataDependencyRegex = "$pathPrefix(.*?)->(.*)".toRegex()

            build("printMetadataFiles") {
                assertSuccessful()

                val expectedFileName = "sample-lib-metadata-1.0.jar"

                val paths = metadataDependencyRegex
                    .findAll(output).map { it.groupValues[1] to it.groupValues[2] }
                    .filter { (_, f) -> "sample-lib" in f }
                    .toSet()

                Assert.assertEquals(
                    setOf("commonMainResolvable$METADATA_CONFIGURATION_NAME_SUFFIX" to expectedFileName),
                    paths
                )
            }
        }
    }

    @Test
    fun testResolveJsPartOfMppLibDependencyToMetadataWithHmpp() =
        testResolveJsPartOfMppLibDependencyToMetadata(hmppWoCompatibilityMetadataArtifact)

    @Test
    fun testResolveJsPartOfMppLibDependencyToMetadataWithoutHmpp() =
        testResolveJsPartOfMppLibDependencyToMetadata(noHMPP)


    private fun testResolveJsPartOfMppLibDependencyToMetadata(hmppFlags: HmppFlags) {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val appProject = Project("sample-app", gradleVersion, "new-mpp-lib-and-app")

        val buildOptions = hmppFlags.buildOptions
        libProject.setupWorkingDir()
        libProject.gradleProperties().appendText(
            """
                
                kotlin.compiler.execution.strategy=in-process
                """.trimIndent()
        )
        @Suppress("DEPRECATION")
        libProject.build(
            "publish",
            options = buildOptions
        ) {
            assertSuccessful()
        }
        val localRepo = libProject.projectDir.resolve("repo")
        val localRepoUri = localRepo.toURI()

        with(appProject) {
            setupWorkingDir()

            gradleProperties().appendText(
                """
                
                kotlin.compiler.execution.strategy=in-process
                """.trimIndent()
            )

            val pathPrefix = "metadataDependency: "

            gradleBuildScript().appendText(
                "\n" + """
                    repositories { maven { url '$localRepoUri' } }

                    kotlin.sourceSets {
                        nodeJsMain {
                            dependencies {
                                // add these dependencies to check that they are resolved to metadata
                                api 'com.example:sample-lib-nodejs:1.0'
                                implementation 'com.example:sample-lib-nodejs:1.0'
                                compileOnly 'com.example:sample-lib-nodejs:1.0'
                            }
                        }
                    }

                    task('printMetadataFiles') {
                        doFirst {
                            def configuration = configurations.getByName("nodeJsMainResolvable" + '$METADATA_CONFIGURATION_NAME_SUFFIX')
                            configuration.files.each { println '$pathPrefix' + configuration.name + '->' + it.name }
                        }
                    }
                """.trimIndent()
            )
        }
    }

    @Test
    fun testResolveMppProjectDependencyToMetadata() {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val appProject = Project("sample-app", gradleVersion, "new-mpp-lib-and-app")

        val pathPrefix = "metadataDependency: "

        with(appProject) {
            setupWorkingDir()
            libProject.setupWorkingDir()

            libProject.projectDir.copyRecursively(projectDir.resolve(libProject.projectDir.name))
            projectDir.resolve("settings.gradle").appendText("\ninclude '${libProject.projectDir.name}'")
            gradleBuildScript().modify {
                it.replace("'com.example:sample-lib:1.0'", "project(':${libProject.projectDir.name}')") +
                        "\n" + """
                        task('printMetadataFiles') {
                           doFirst {
                               configurations.getByName('commonMainResolvable$METADATA_CONFIGURATION_NAME_SUFFIX')
                                   .files.each { println '$pathPrefix' + it.name }
                           }
                        }
                        """.trimIndent()
            }

            build("printMetadataFiles") {
                assertSuccessful()
                assertContains(pathPrefix + "sample-lib-metadata-1.0.jar")
            }
        }
    }

    @Test
    fun testPublishingOnlySupportedNativeTargets() = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        val publishedVariants = MPPNativeTargets.supported
        val nonPublishedVariants = MPPNativeTargets.unsupported

        build("publish") {
            assertSuccessful()

            assertTrue(publishedVariants.isNotEmpty())
            publishedVariants.forEach {
                assertFileExists("repo/com/example/sample-lib-$it/1.0/sample-lib-$it-1.0.klib")
            }
            nonPublishedVariants.forEach {
                assertNoSuchFile("repo/com/example/sample-lib-$it") // check that no artifacts are published for that variant
            }

            // but check that the module metadata contains all variants:
            val gradleModuleMetadata = projectDir.resolve("repo/com/example/sample-lib/1.0/sample-lib-1.0.module").readText()
            assertTrue(""""name": "linux64ApiElements-published"""" in gradleModuleMetadata)
            assertTrue(""""name": "mingw64ApiElements-published"""" in gradleModuleMetadata)
            assertTrue(""""name": "macos64ApiElements-published"""" in gradleModuleMetadata)
        }
    }

    @Test
    fun testOptionalExpectations() = with(transformNativeTestProject("new-mpp-lib-with-tests", gradleVersion)) {
        projectDir.resolve("src/commonMain/kotlin/Optional.kt").writeText(
            """
            @file:Suppress("OPT_IN_USAGE_ERROR", "EXPERIMENTAL_API_USAGE_ERROR")
            @OptionalExpectation
            expect annotation class Optional(val value: String)

            @Optional("optionalAnnotationValue")
            class OptionalCommonUsage
            """.trimIndent()
        )

        build("compileCommonMainKotlinMetadata") {
            assertSuccessful()
            val compilerArgsLine = output.lines().singleOrNull { ":compileCommonMainKotlinMetadata Kotlin compiler args" in it }
            assertNotNull(compilerArgsLine, "The debug log should contain the compiler args for the task :compileCommonMainKotlinMetadata")
            val args = compilerArgsLine.split(" ")
            val xCommonSourcesArg = args.singleOrNull { it.startsWith("-Xcommon-sources=") }
            assertNotNull(xCommonSourcesArg, "The compiler args for K2Metadata should contain the -Xcommon-sources argument")
            val xCommonSourcesFiles = xCommonSourcesArg.substringAfter("-Xcommon-sources=").split(",")
            assertTrue { xCommonSourcesFiles.any { it.endsWith("Optional.kt") } }
        }

        build("compileKotlinJvmWithoutJava", "compileKotlinLinux64") {
            assertSuccessful()
            assertFileExists(targetClassesDir("jvmWithoutJava") + "OptionalCommonUsage.class")
        }

        val optionalImplText = "\n" + """
            @Optional("should fail, see KT-25196")
            class OptionalPlatformUsage
        """.trimIndent()

        projectDir.resolve("src/jvmWithoutJavaMain/kotlin/OptionalImpl.kt").writeText(optionalImplText)

        build("compileKotlinJvmWithoutJava") {
            assertFailed()
            assertContains("Declaration annotated with '@OptionalExpectation' can only be used in common module sources", ignoreCase = true)
        }

        projectDir.resolve("src/linux64Main/kotlin/").also {
            it.mkdirs()
            it.resolve("OptionalImpl.kt").writeText(optionalImplText)
        }

        build("compileKotlinLinux64") {
            assertFailed()
            assertContains("Declaration annotated with '@OptionalExpectation' can only be used in common module sources", ignoreCase = true)
        }
    }

    @Test
    fun testSourceJars() = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        build("publish") {
            assertSuccessful()

            val groupDir = projectDir.resolve("repo/com/example/")
            val targetArtifactIdAppendices = listOf(null, "jvm6", "nodejs", "linux64")

            val sourceJarSourceRoots = targetArtifactIdAppendices.associateWith { artifact ->
                val sourcesJarPath = if (artifact != null) "sample-lib-$artifact/1.0/sample-lib-$artifact-1.0-sources.jar"
                else "sample-lib/1.0/sample-lib-1.0-sources.jar"
                val sourcesJar = JarFile(groupDir.resolve(sourcesJarPath))
                val sourcesDirs = sourcesJar.entries().asSequence().map { it.name.substringBefore("/") }.toSet() - "META-INF"
                sourcesDirs
            }

            assertEquals(
                setOf("commonMain", "nativeMain"),
                sourceJarSourceRoots[null]
            )
            assertEquals(setOf("commonMain", "jvm6Main"), sourceJarSourceRoots["jvm6"])
            assertEquals(setOf("commonMain", "nodeJsMain"), sourceJarSourceRoots["nodejs"])
            assertEquals(setOf("commonMain", "nativeMain", "linux64Main"), sourceJarSourceRoots["linux64"])
        }
    }

    @Test
    fun testConsumeMppLibraryFromNonKotlinProject() {
        val libRepo = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
            build("publish") { assertSuccessful() }
            projectDir.resolve("repo")
        }

        with(transformNativeTestProject("sample-app-without-kotlin", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nrepositories { maven { url '${libRepo.toURI()}' } }")

            build("assemble") {
                assertSuccessful()
                assertTasksExecuted(":compileJava")
                assertFileExists("build/classes/java/main/A.class")
            }
        }
    }

    @Test
    fun testPublishMultimoduleProjectWithMetadata() {
        val libProject = transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        libProject.setupWorkingDir()

        transformNativeTestProject("sample-external-lib", gradleVersion, "new-mpp-lib-and-app").apply {
            setupWorkingDir()
            // Publish it into local repository of adjacent lib:
            gradleBuildScript().appendText(
                "\n" + """
                publishing {
                    repositories {
                        maven { url "${'$'}{rootProject.projectDir.absolutePath.replace('\\', '/')}/../sample-lib/repo" }
                    }
                }
                """.trimIndent()
            )
            build("publish") {
                assertSuccessful()
            }
        }

        val appProject = transformNativeTestProject("sample-app", gradleVersion, "new-mpp-lib-and-app")

        with(libProject) {
            setupWorkingDir()
            appProject.setupWorkingDir(false)
            appProject.projectDir.copyRecursively(projectDir.resolve("sample-app"))

            gradleSettingsScript().writeText("include 'sample-app'")
            gradleBuildScript("sample-app").modify {
                it.replace("'com.example:sample-lib:1.0'", "project(':')") + "\n" + """
                apply plugin: 'maven-publish'
                group = "com.exampleapp"
                version = "1.0"
                publishing {
                    repositories {
                        maven { url "${'$'}{rootProject.projectDir.absolutePath.replace('\\', '/')}/repo" }
                    }
                }
                """.trimIndent()
            }

            // Add a dependency that is resolved with metadata:
            gradleBuildScript("sample-app").appendText(
                "\n" + """
                repositories {
                    maven { url "${'$'}{rootProject.projectDir.absolutePath.replace('\\', '/')}/repo" }
                }
                dependencies {
                    commonMainApi 'com.external.dependency:external:1.2.3'
                }
                """.trimIndent()
            )

            gradleBuildScript().appendText(
                "\n" + """
                publishing {
                    publications {
                        jvm6 {
                            groupId = "foo"
                            artifactId = "bar"
                            version = "42"
                        }
                    }
                }
                """.trimIndent()
            )

            gradleBuildScript().appendText(
                "\n" + """
                publishing {
                    publications {
                        kotlinMultiplatform {
                            // KT-29485
                            artifactId = 'sample-lib-multiplatform'
                        }
                    }
                }
                """.trimIndent()
            )

            build("clean", "publish", options = defaultBuildOptions().copy(configurationCache = true)) {
                assertSuccessful()
                assertFileContains(
                    "repo/com/exampleapp/sample-app-nodejs/1.0/sample-app-nodejs-1.0.pom",
                    "<groupId>com.example</groupId>",
                    "<artifactId>sample-lib-nodejs</artifactId>",
                    "<version>1.0</version>"
                )
                assertFileContains(
                    "repo/com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom",
                    "<groupId>foo</groupId>",
                    "<artifactId>bar</artifactId>",
                    "<version>42</version>"
                )
                assertFileContains(
                    "repo/com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom",
                    "<groupId>com.external.dependency</groupId>",
                    "<artifactId>external-jvm6</artifactId>",
                    "<version>1.2.3</version>"
                )

                // Check that, despite the rewritten POM, the module metadata contains the original dependency:
                val moduleMetadata = projectDir.resolve("repo/com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.module").readText()
                    .replace("\\s+".toRegex(), "").replace("\n", "")
                assertTrue { "\"group\":\"com.example\",\"module\":\"sample-lib-multiplatform\"" in moduleMetadata }
                assertTrue { "\"group\":\"com.external.dependency\",\"module\":\"external\"" in moduleMetadata }
            }

            // Check that a user can disable rewriting of MPP dependencies in the POMs:
            build("publish", "-Pkotlin.mpp.keepMppDependenciesIntactInPoms=true") {
                assertSuccessful()
                assertFileContains(
                    "repo/com/exampleapp/sample-app-nodejs/1.0/sample-app-nodejs-1.0.pom",
                    "<groupId>com.example</groupId>",
                    "<artifactId>sample-lib-multiplatform</artifactId>",
                    "<version>1.0</version>"
                )
                assertFileContains(
                    "repo/com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom",
                    "<groupId>com.example</groupId>",
                    "<artifactId>sample-lib-multiplatform</artifactId>",
                    "<version>1.0</version>"
                )
                assertFileContains(
                    "repo/com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom",
                    "<groupId>com.external.dependency</groupId>",
                    "<artifactId>external</artifactId>",
                    "<version>1.2.3</version>"
                )
            }
        }
    }

    @Test
    fun testMppBuildWithCompilerPlugins() = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        val printOptionsTaskName = "printCompilerPluginOptions"
        val argsMarker = "=args=>"
        val classpathMarker = "=cp=>"
        val compilerPluginArgsRegex = "(\\w+)${Regex.escape(argsMarker)}(.*)".toRegex()
        val compilerPluginClasspathRegex = "(\\w+)${Regex.escape(classpathMarker)}(.*)".toRegex()

        gradleBuildScript().appendText(
            "\n" + """
            buildscript {
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-allopen:${'$'}kotlin_version"
                    classpath "org.jetbrains.kotlin:kotlin-noarg:${'$'}kotlin_version"
                }
            }
            apply plugin: 'kotlin-allopen'
            apply plugin: 'kotlin-noarg'

            allOpen { annotation 'com.example.Annotation' }
            noArg { annotation 'com.example.Annotation' }

            task $printOptionsTaskName {
                // if the tasks are not configured during evaluation phase, configuring them during execution
                // leads to new dependencies unsuccessfully added to the resolved compilerPluginsClasspath configuration
                kotlin.targets.all { compilations.all { /*force to configure the*/ compileKotlinTask } }
                doFirst {
                    kotlin.sourceSets.each { sourceSet ->
                        def args = sourceSet.languageSettings.compilerPluginArguments
                        def cp = sourceSet.languageSettings.compilerPluginClasspath.files
                        println sourceSet.name + '$argsMarker' + args
                        println sourceSet.name + '$classpathMarker' + cp
                    }
                }
            }
            """.trimIndent()
        )

        projectDir.resolve("src/commonMain/kotlin/Annotation.kt").writeText(
            """
            package com.example
            annotation class Annotation
            """.trimIndent()
        )
        projectDir.resolve("src/commonMain/kotlin/Annotated.kt").writeText(
            """
            package com.example
            @Annotation
            open class Annotated(var y: Int) { var x = 2 }
            """.trimIndent()
        )
        // TODO once Kotlin/Native properly supports compiler plugins, move this class to the common sources
        listOf("jvm6", "nodeJs").forEach {
            projectDir.resolve("src/${it}Main/kotlin/Override.kt").writeText(
                """
                package com.example
                @Annotation
                class Override : Annotated(0) {
                    override var x = 3
                }
                """.trimIndent()
            )
        }

        // Do not use embeddable compiler in Kotlin/Native, otherwise it would effectively enable allopen & noarg plugins for Native, and
        // we'd be testing that the latest versions of allopen/noarg work with the fixed version of Kotlin/Native (defined in the root
        // build.gradle.kts), which is generally not guaranteed.
        build("assemble", "-Pkotlin.native.useEmbeddableCompilerJar=false", printOptionsTaskName) {
            assertSuccessful()
            assertTasksExecuted(*listOf("Jvm6", "NodeJs", "Linux64").map { ":compileKotlin$it" }.toTypedArray())
            assertFileExists("build/classes/kotlin/jvm6/main/com/example/Annotated.class")
            assertFileExists("build/classes/kotlin/jvm6/main/com/example/Override.class")

            val (compilerPluginArgsBySourceSet, compilerPluginClasspathBySourceSet) =
                listOf(compilerPluginArgsRegex, compilerPluginClasspathRegex)
                    .map { marker ->
                        marker.findAll(output).associate { it.groupValues[1] to it.groupValues[2] }
                    }

            // TODO once Kotlin/Native properly supports compiler plugins, expand this to all source sets:
            listOf("commonMain", "commonTest", "jvm6Main", "jvm6Test", "nodeJsMain", "nodeJsTest").forEach {
                val expectedArgs = "[plugin:org.jetbrains.kotlin.allopen:annotation=com.example.Annotation, " +
                        "plugin:org.jetbrains.kotlin.noarg:annotation=com.example.Annotation]"

                assertEquals(expectedArgs, compilerPluginArgsBySourceSet[it], "Expected $expectedArgs as plugin args for $it")
                assertTrue { compilerPluginClasspathBySourceSet[it]!!.contains("kotlin-allopen") }
                assertTrue { compilerPluginClasspathBySourceSet[it]!!.contains("kotlin-noarg") }
            }
        }
    }

    @Test
    fun testDefaultSourceSetsDsl() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        val testOutputPrefix = "# default source set "
        val testOutputRegex = Regex("${Regex.escape(testOutputPrefix)} (.*?) (.*?) (.*)")

        gradleBuildScript().appendText(
            "\n" + """
            kotlin.targets.each { target ->
                target.compilations.each { compilation ->
                    println "$testOutputPrefix ${'$'}{target.name} ${'$'}{compilation.name} ${'$'}{compilation.defaultSourceSet.name}"
                }
            }
            """.trimIndent()
        )

        build {
            assertSuccessful()

            val actualDefaultSourceSets = testOutputRegex.findAll(output).mapTo(mutableSetOf()) {
                it.groupValues.let { (_, target, compilation, sourceSet) -> Triple(target, compilation, sourceSet) }
            }

            val expectedDefaultSourceSets = listOf(
                "jvm6", "nodeJs", "mingw64", "linux64", "macos64", "macosArm64", "wasmJs"
            ).flatMapTo(mutableSetOf()) { target ->
                listOf("main", "test").map { compilation ->
                    Triple(
                        target, compilation,
                        "$target${compilation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
                    )
                }
            } + Triple("metadata", "main", "commonMain")

            assertEquals(expectedDefaultSourceSets, actualDefaultSourceSets)
        }
    }

    @Test
    fun testMultipleTargetsSamePlatform() = with(Project("newMppMultipleTargetsSamePlatform", gradleVersion)) {
        testResolveAllConfigurations("app") {
            assertContains(">> :app:junitCompileClasspath --> lib-junit.jar")
            assertContains(">> :app:junitCompileClasspath --> junit-4.13.2.jar")

            assertContains(">> :app:mixedJunitCompileClasspath --> lib-junit.jar")
            assertContains(">> :app:mixedJunitCompileClasspath --> junit-4.13.2.jar")

            assertContains(">> :app:testngCompileClasspath --> lib-testng.jar")
            assertContains(">> :app:testngCompileClasspath --> testng-6.14.3.jar")

            assertContains(">> :app:mixedTestngCompileClasspath --> lib-testng.jar")
            assertContains(">> :app:mixedTestngCompileClasspath --> testng-6.14.3.jar")
        }
    }

    @Test
    fun testUnusedSourceSetsReport() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        gradleBuildScript().appendText("\nkotlin { sourceSets { foo { } } }")

        build {
            assertSuccessful()
            assertHasDiagnostic(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
        }

        gradleBuildScript().appendText("\nkotlin { sourceSets { bar { dependsOn foo } } }")

        build {
            assertSuccessful()
            assertHasDiagnostic(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
        }

        gradleBuildScript().appendText("\nkotlin { sourceSets { jvm6Main { dependsOn bar } } }")

        build {
            assertSuccessful()
            assertNoDiagnostic(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
        }
    }

    @Test
    fun testPomRewritingInSinglePlatformProject() = with(Project("kt-27059-pom-rewriting")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        gradleProperties().appendText(
            """
                
                kotlin.compiler.execution.strategy=in-process
                """.trimIndent()
        )

        val groupDir = "build/repo/com/example/"

        build(":mpp-lib:publish") {
            assertSuccessful()
            assertFileExists(groupDir + "mpp-lib")
            assertFileExists(groupDir + "mpp-lib-myjvm")
        }

        fun doTestPomRewriting(mppProjectDependency: Boolean, keepPomIntact: Boolean? = null) {

            val params = mutableListOf("clean", ":jvm-app:publish", ":js-app:publish").apply {
                if (mppProjectDependency)
                    add("-PmppProjectDependency=true")
                if (keepPomIntact == true)
                    add("-Pkotlin.mpp.keepMppDependenciesIntactInPoms=true")
            }.toTypedArray()

            build(*params) {
                assertSuccessful()
                assertTasksExecuted(":jvm-app:publishMainPublicationToMavenRepository")
                assertTasksExecuted(":js-app:publishMavenPublicationToMavenRepository")

                val jvmModuleDir = groupDir + "jvm-app/1.0/"
                val jsModuleDir = groupDir + "js-app/1.0/"
                val jvmPom = fileInWorkingDir(jvmModuleDir + "jvm-app-1.0.pom").readText().replace("\\s+".toRegex(), "")
                val jsPom = fileInWorkingDir(jsModuleDir + "js-app-1.0.pom").readText().replace("\\s+".toRegex(), "")

                if (keepPomIntact != true) {
                    assertTrue("The JVM POM should contain the dependency on 'mpp-lib' rewritten as 'mpp-lib-myjvm'") {
                        jvmPom.contains(
                            "<groupId>com.example</groupId><artifactId>mpp-lib-myjvm</artifactId><version>1.0</version><scope>compile</scope>"
                        )
                    }
                } else {
                    assertTrue("The JVM POM should contain the original dependency on 'mpp-lib'") {
                        jvmPom.contains(
                            "<groupId>com.example</groupId><artifactId>mpp-lib</artifactId><version>1.0</version><scope>compile</scope>"
                        )
                    }
                    assertTrue("The JS POM should contain the original dependency on 'mpp-lib'") {
                        jsPom.contains(
                            "<groupId>com.example</groupId><artifactId>mpp-lib</artifactId><version>1.0</version><scope>compile</scope>"
                        )
                    }
                }
            }
        }

        doTestPomRewriting(mppProjectDependency = false)
        doTestPomRewriting(mppProjectDependency = true)

        // This case doesn't work and never did; TODO investigate KT-29975
        // doTestPomRewriting(mppProjectDependency = true, legacyPublishing = true)

        // Also check that the flag for keeping POMs intact works:
        doTestPomRewriting(mppProjectDependency = false, keepPomIntact = true)
    }

    @Test
    fun testAssociateCompilations() {
        testAssociateCompilationsImpl()
    }

    private fun testAssociateCompilationsImpl() {
        with(Project("new-mpp-associate-compilations")) {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            val tasks = listOf("jvm", "js", "linux64").map {
                ":compileIntegrationTestKotlin${
                    it.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }
                }"
            }

            build(
                *tasks.toTypedArray()
            ) {
                assertSuccessful()
                assertTasksExecuted(*tasks.toTypedArray())

                // JVM:
                checkBytecodeContains(
                    projectDir.resolve("build/classes/kotlin/jvm/integrationTest/com/example/HelloIntegrationTest.class"),
                    "Hello.internalFun\$new_mpp_associate_compilations",
                    "HelloTest.internalTestFun\$new_mpp_associate_compilations"
                )
                assertFileExists("build/classes/kotlin/jvm/integrationTest/META-INF/new-mpp-associate-compilations_integrationTest.kotlin_module")

                // JS:
                assertFileExists(
                    "build/classes/kotlin/js/integrationTest/default/manifest"
                )

                // Native:
                assertFileExists("build/classes/kotlin/linux64/integrationTest/klib/new-mpp-associate-compilations_integrationTest.klib")
            }
        }
    }

    @Test
    fun testTestRunsApi() = with(Project("new-mpp-associate-compilations")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        // TOOD: add Kotlin/JS tests once they can be tested without much performance overhead
        val targetsToTest = listOf("jvm", nativeHostTargetName) + when (HostManager.host) {
            KonanTarget.MACOS_X64 -> listOf("iosX64")
            KonanTarget.MACOS_ARM64 -> listOf("iosSimulatorArm64")
            else -> emptyList()
        }
        val testTasks = targetsToTest.flatMap { listOf(":${it}Test", ":${it}IntegrationTest") }.toTypedArray()

        build(*testTasks) {
            assertSuccessful()

            assertTasksExecuted(
                *testTasks,
                ":compileIntegrationTestKotlinJvm",
                ":linkIntegrationDebugTest${nativeHostTargetName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
            )

            fun checkUnitTestOutput(targetName: String) {
                val classReportHtml = projectDir
                    .resolve("build/reports/tests/${targetName}Test/classes/com.example.HelloTest.html")
                    .readText()

                assertTrue("secondTest" !in classReportHtml, "Test report should not contain 'secondTest':\n$classReportHtml")
            }
            targetsToTest.forEach {
                checkUnitTestOutput(it)
            }

            fun checkIntegrationTestOutput(targetName: String) {
                val classReportHtml = projectDir
                    .resolve("build/reports/tests/${targetName}IntegrationTest/classes/com.example.HelloIntegrationTest.html")
                    .readText()

                assertTrue("test[$targetName]" in classReportHtml, "Test report should contain 'test[$targetName]':\n$classReportHtml")
                assertTrue("secondTest" !in classReportHtml, "Test report should not contain 'secondTest':\n$classReportHtml")
                assertTrue("thirdTest" !in classReportHtml, "Test report should not contain 'thirdTest':\n$classReportHtml")
            }
            targetsToTest.forEach {
                checkIntegrationTestOutput(it)
            }
        }
    }

    @Test
    fun testKlibsWithTheSameProjectName() = with(transformProjectWithPluginsDsl("new-mpp-klibs-with-same-name")) {
        // KT-36721.
        build("assemble") {
            assertSuccessful()
            assertTasksExecuted(
                ":foo:foo:compileKotlinJs",
                ":foo:foo:compileKotlinLinux",
                ":foo:compileKotlinJs",
                ":foo:compileKotlinLinux",
                ":compileKotlinJs",
                ":compileKotlinLinux"
            )

            fun getManifest(relativePath: String): Properties =
                with(ZipFile(projectDir.resolve(relativePath))) {
                    return this.getInputStream(getEntry("default/manifest")).use { stream ->
                        Properties().apply { load(stream) }
                    }
                }

            val interopManifest = getManifest("foo/build/classes/kotlin/linux/main/cinterop/foo-cinterop-bar.klib")
            assertEquals("org.sample.one:foo-cinterop-bar", interopManifest[KLIB_PROPERTY_UNIQUE_NAME])

            val nativeManifest = getManifest("foo/build/classes/kotlin/linux/main/klib/foo.klib")
            assertEquals("org.sample.one:foo", nativeManifest[KLIB_PROPERTY_UNIQUE_NAME])
            // Check the short name that is used as a prefix in generated ObjC headers.
            assertEquals("foo", nativeManifest[KLIB_PROPERTY_SHORT_NAME])

            val jsManifest = projectDir.resolve("foo/build/classes/kotlin/js/main/default/manifest")
                .inputStream().use { stream ->
                    Properties().apply { load(stream) }
                }
            assertEquals("org.sample.one:foo", jsManifest[KLIB_PROPERTY_UNIQUE_NAME])
        }
    }

    @Test
    fun testNativeCompilationShouldNotProduceAnyWarningsForAssociatedCompilations() {
        with(Project("native-common-dependencies-warning", minLogLevel = LogLevel.INFO)) {
            setupWorkingDir()
            build("help") {
                assertSuccessful()
                assertNotContains("A compileOnly dependency is used in the Kotlin/Native target '${detectNativeEnabledCompilation()}':")
            }
        }
    }

    @Test
    fun testNativeCompilationShouldProduceWarningOnCompileOnlyCommonDependency() {
        with(Project("native-common-dependencies-warning", minLogLevel = LogLevel.INFO)) {
            setupWorkingDir()
            gradleBuildScript().modify {
                it.replaceFirst("//compileOnly:", "")
            }
            build("help") {
                assertSuccessful()
                assertContains("A compileOnly dependency is used in the Kotlin/Native target '${detectNativeEnabledCompilation()}':")
            }
        }
    }

    @Test
    fun testNativeCompilationCompileOnlyDependencyWarningCouldBeDisabled() {
        with(Project("native-common-dependencies-warning", minLogLevel = LogLevel.INFO)) {
            setupWorkingDir()
            gradleBuildScript().modify {
                it.replaceFirst("//compileOnly:", "")
            }
            projectDir.resolve("gradle.properties").writeText(
                """
                kotlin.native.ignoreIncorrectDependencies = true
                """.trimIndent()
            )
            build("help") {
                assertSuccessful()
                assertNotContains("A compileOnly dependency is used in the Kotlin/Native target '${detectNativeEnabledCompilation()}':")
            }
        }
    }

    @Test
    fun testErrorInClasspathMode() {
        val classpathModeOptions = defaultBuildOptions().copy(
            freeCommandLineArgs = listOf("-Dorg.gradle.kotlin.dsl.provider.mode=classpath")
        )

        with(Project("kotlin-mpp-classpathMode")) {
            build("tasks") {
                assertFailed()
                assertContains("ERROR DURING CONFIGURATION PHASE")
            }

            build("tasks", options = classpathModeOptions) {
                assertSuccessful()
            }

            build("listCollectedErrors", options = classpathModeOptions) {
                assertSuccessful()
                assertContains("Collected 1 exception(s)")
                assertContains("ERROR DURING CONFIGURATION PHASE")
            }
        }
    }

    @Test
    fun testWasmJs() = with(
        Project(
            "new-mpp-wasm-js",
            // TODO: this test fails with deprecation error on Gradle <7.0
            // Should be fixed via planned fixes in Kotlin/JS plugin: https://youtrack.jetbrains.com/issue/KFC-252
            gradleVersionRequirement = GradleVersionRequired.AtLeast(TestVersions.Gradle.G_7_0)
        )
    ) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        gradleBuildScript().modify {
            it.replace("<JsEngine>", "d8")
        }
        build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlinJs")
            assertTasksExecuted(":compileKotlinWasmJs")

            val outputPrefix = "build/js/packages/"

            val jsOutput = outputPrefix + "redefined-js-module-name/kotlin/"
            assertFileExists(jsOutput + "redefined-js-module-name.js")

            val wasmOutput = outputPrefix + "redefined-wasm-module-name/kotlin/"
            assertFileExists(wasmOutput + "redefined-wasm-module-name.mjs")
            assertFileExists(wasmOutput + "redefined-wasm-module-name.wasm")
        }
    }

    private fun testWasmTest(engine: String, name: String) = with(
        Project("new-mpp-wasm-test", gradleVersionRequirement = GradleVersionRequired.AtLeast(TestVersions.Gradle.G_7_0))
    ) {
        setupWorkingDir()
        gradleBuildScript().modify {
            transformBuildScriptWithPluginsDsl(it)
                .replace("<JsEngine>", engine)
        }
        build(":wasmJs${name}Test") {
            assertTasksExecuted(":compileKotlinWasmJs")
            assertTasksNotExecuted(":compileTestDevelopmentExecutableKotlinWasmJsOptimize")
            assertTasksFailed(":wasmJs${name}Test")
            assertTestResults(
                "testProject/new-mpp-wasm-test/TEST-${engine}.xml",
                "wasmJs${name}Test"
            )
        }
    }

    @Test
    fun testWasmNodeTest() = testWasmTest("nodejs", "Node")

    @Test
    fun testWasmD8Test() = testWasmTest("d8", "D8")

    @Test
    fun testResolveMetadataCompileClasspathKt50925() {
        Project("lib", directoryPrefix = "kt-50925-resolve-metadata-compile-classpath").apply {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            build("publish") {
                assertSuccessful()
            }
        }

        Project("app", directoryPrefix = "kt-50925-resolve-metadata-compile-classpath").apply {
            setupWorkingDir()
            gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

            testResolveAllConfigurations { unresolvedConfigurations ->
                assertTrue(unresolvedConfigurations.isEmpty(), "Unresolved configurations: $unresolvedConfigurations")

                assertContains(">> :metadataCompileClasspath --> lib-metadata-1.0.jar")
                assertContains(">> :metadataCompileClasspath --> subproject-metadata.jar")
                assertContains(">> :metadataCommonMainCompileClasspath --> lib-metadata-1.0.jar")
                assertContains(">> :metadataCommonMainCompileClasspath --> subproject-metadata.jar")
            }
        }
    }

    @Test
    fun testPublishEmptySourceSets() = with(Project("mpp-empty-sources")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        build("publish") {
            assertSuccessful()
        }
    }

    private fun detectNativeEnabledCompilation(): String = when {
        HostManager.hostIsLinux -> "linuxX64"
        HostManager.hostIsMingw -> "mingwX64"
        HostManager.hostIsMac -> "macosX64"
        else -> throw AssertionError("Host ${HostManager.host} is not supported for this test")
    }

    companion object {
        fun List<String>.containsSequentially(vararg elements: String): Boolean {
            check(elements.isNotEmpty())
            return Collections.indexOfSubList(this, elements.toList()) != -1
        }
    }
}
