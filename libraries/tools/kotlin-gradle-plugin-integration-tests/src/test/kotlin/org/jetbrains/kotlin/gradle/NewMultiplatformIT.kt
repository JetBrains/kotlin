/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.withNativeCommandLineArguments
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.containsSequentially
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.native.*
import org.jetbrains.kotlin.gradle.native.MPPNativeTargets
import org.jetbrains.kotlin.gradle.native.transformNativeTestProject
import org.jetbrains.kotlin.gradle.native.transformNativeTestProjectWithPluginDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.*
import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations
import org.jetbrains.kotlin.gradle.plugin.lowerName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmWithJavaTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.UnusedSourceSetsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.plugin.sources.UnsatisfiedSourceSetVisibilityException
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.KLIB_PROPERTY_SHORT_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.junit.Assert
import org.junit.Test
import java.util.*
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NewMultiplatformIT : BaseGradleIT() {
    private val gradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    private val nativeHostTargetName = MPPNativeTargets.current
    private val unsupportedNativeTargets = MPPNativeTargets.unsupported

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    private data class HmppFlags(
        val hmppSupport: Boolean,
        val enableCompatibilityMetadataArtifact: Boolean,
        val name: String
    ) {
        override fun toString() = name
    }

    private val noHMPP = HmppFlags(
        name = "No HMPP",
        hmppSupport = false,
        enableCompatibilityMetadataArtifact = false
    )

    private val hmppWoCompatibilityMetadataArtifact = HmppFlags(
        name = "HMPP without Compatibility Metadata Artifact",
        hmppSupport = true,
        enableCompatibilityMetadataArtifact = false
    )

    private val hmppWithCompatibilityMetadataArtifact = HmppFlags(
        name = "HMPP with Compatibility Metadata Artifact",
        hmppSupport = true,
        enableCompatibilityMetadataArtifact = true
    )

    private val HmppFlags.buildOptions get() = defaultBuildOptions().copy(
        hierarchicalMPPStructureSupport = hmppSupport,
        enableCompatibilityMetadataVariant = enableCompatibilityMetadataArtifact,
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
    fun testLibAndAppWithCompatibilityArtifact() = doTestLibAndApp(
        "sample-lib",
        "sample-app",
        hmppWithCompatibilityMetadataArtifact
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
        val oldStyleAppProject = Project("sample-old-style-app", directoryPrefix = "new-mpp-lib-and-app")

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

                if (hmppFlags.enableCompatibilityMetadataArtifact) {
                    assertTasksExecuted(":compileKotlinMetadata", ":metadataJar")
                }

                val groupDir = projectDir.resolve("repo/com/example")
                val jvmJarName = "sample-lib-jvm6/1.0/sample-lib-jvm6-1.0.jar"
                val jsExtension = "jar"
                val jsJarName = "sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.$jsExtension"
                val metadataJarName = "sample-lib/1.0/sample-lib-1.0.jar"
                val nativeKlibName = "sample-lib-linux64/1.0/sample-lib-linux64-1.0.klib"

                listOf(jvmJarName, jsJarName, metadataJarName, "sample-lib/1.0/sample-lib-1.0.module").forEach {
                    Assert.assertTrue("$it should exist", groupDir.resolve(it).exists())
                }

                val gradleMetadata = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module").readText()
                assertFalse(gradleMetadata.contains(ProjectLocalConfigurations.ATTRIBUTE.name))

                listOf(jvmJarName, jsJarName, nativeKlibName).forEach {
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

                val jsJar = ZipFile(groupDir.resolve(jsJarName))
                val compiledJs = jsJar.getInputStream(jsJar.getEntry("sample-lib.js")).reader().readText()
                Assert.assertTrue("function id(" in compiledJs)
                Assert.assertTrue("function idUsage(" in compiledJs)
                Assert.assertTrue("function expectedFun(" in compiledJs)
                Assert.assertTrue("function main(" in compiledJs)

                val metadataJarEntries = ZipFile(groupDir.resolve(metadataJarName)).entries().asSequence().map { it.name }.toSet()
                val metadataFileFound = "com/example/lib/CommonKt.kotlin_metadata" in metadataJarEntries
                Assert.assertEquals(hmppFlags.enableCompatibilityMetadataArtifact, metadataFileFound)

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

                if (hmppFlags.enableCompatibilityMetadataArtifact) {
                    projectDir.resolve(targetClassesDir("metadata")).run {
                        Assert.assertTrue(resolve("com/example/app/AKt.kotlin_metadata").exists())
                    }
                }

                projectDir.resolve(targetClassesDir("nodeJs")).resolve("sample-app.js").readText().run {
                    Assert.assertTrue(contains("console.info"))
                    Assert.assertTrue(contains("function nodeJsMain("))
                }

                val nativeExeName = if (isWindows) "main.exe" else "main.kexe"
                assertFileExists("build/bin/linux64/mainDebugExecutable/$nativeExeName")

                // Check that linker options were correctly passed to the K/N compiler.
                withNativeCommandLineArguments(":linkMainDebugExecutableLinux64") { arguments ->
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
            gradleBuildScript(subproject = libProject.projectDir.name).modify {
                it.lines().dropLast(5).joinToString(separator = "\n")
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

        if (hmppFlags.enableCompatibilityMetadataArtifact) {
            with(oldStyleAppProject) {
                setupWorkingDir()
                gradleBuildScript().appendText("\nallprojects { repositories { maven { url '$libLocalRepoUri' } } }")

                build("assemble", options = buildOptions) {
                    assertSuccessful()
                    assertTasksExecuted(":app-js:compileKotlin2Js", ":app-jvm:compileKotlin", ":app-common:compileKotlinCommon")

                    assertFileExists(kotlinClassesDir("app-common") + "com/example/app/CommonAppKt.kotlin_metadata")

                    val jvmClassFile = projectDir.resolve(kotlinClassesDir("app-jvm") + "com/example/app/JvmAppKt.class")
                    checkBytecodeContains(jvmClassFile, "CommonKt.id", "MainKt.expectedFun")

                    val jsCompiledFilePath = kotlinClassesDir("app-js") + "app-js.js"
                    assertFileContains(jsCompiledFilePath, "lib.expectedFun", "lib.id")
                }
            }
        }
    }

    @Test
    fun testLibAndAppJsLegacy() = doTestLibAndAppJsBothCompilers(
        "sample-lib",
        "sample-app",
        LEGACY
    )

    @Test
    fun testLibAndAppJsIr() = doTestLibAndAppJsBothCompilers(
        "sample-lib",
        "sample-app",
        IR
    )

    @Test
    fun testLibAndAppJsBoth() = doTestLibAndAppJsBothCompilers(
        "sample-lib",
        "sample-app",
        BOTH
    )

    @Test
    fun testLibAndAppWithGradleKotlinDslJsLegacy() = doTestLibAndAppJsBothCompilers(
        "sample-lib-gradle-kotlin-dsl",
        "sample-app-gradle-kotlin-dsl",
        LEGACY
    )

    @Test
    fun testLibAndAppWithGradleKotlinDslJsIr() = doTestLibAndAppJsBothCompilers(
        "sample-lib-gradle-kotlin-dsl",
        "sample-app-gradle-kotlin-dsl",
        IR
    )

    @Test
    fun testLibAndAppWithGradleKotlinDslJsBoth() = doTestLibAndAppJsBothCompilers(
        "sample-lib-gradle-kotlin-dsl",
        "sample-app-gradle-kotlin-dsl",
        BOTH
    )

    private fun doTestLibAndAppJsBothCompilers(
        libProjectName: String,
        appProjectName: String,
        jsCompilerType: KotlinJsCompilerType
    ) {
        val libProject = transformProjectWithPluginsDsl(libProjectName, directoryPrefix = "both-js-lib-and-app")
        val appProject = transformProjectWithPluginsDsl(appProjectName, directoryPrefix = "both-js-lib-and-app")

        val compileTasksNames =
            listOf(
                *(if (jsCompilerType != BOTH) {
                    arrayOf("NodeJs")
                } else {
                    arrayOf(
                        "NodeJs${LEGACY.lowerName.capitalize()}",
                        "NodeJs${IR.lowerName.capitalize()}",
                    )
                }),
            ).map { ":compileKotlin$it" }

        with(libProject) {
            build(
                "publish",
                options = defaultBuildOptions().copy(jsCompilerType = jsCompilerType)
            ) {
                assertSuccessful()
                assertTasksSkipped(":compileCommonMainKotlinMetadata")
                assertTasksExecuted(*compileTasksNames.toTypedArray(), ":allMetadataJar")

                val groupDir = projectDir.resolve("repo/com/example")
                val jsExtension = if (jsCompilerType == LEGACY) "jar" else "klib"
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

                when (jsCompilerType) {
                    LEGACY -> {
                        val jsJar = ZipFile(groupDir.resolve(jsJarName))
                        val compiledJs = jsJar.getInputStream(jsJar.getEntry("sample-lib.js")).reader().readText()
                        Assert.assertTrue("function id(" in compiledJs)
                        Assert.assertTrue("function idUsage(" in compiledJs)
                        Assert.assertTrue("function expectedFun(" in compiledJs)
                        Assert.assertTrue("function main(" in compiledJs)
                    }
                    IR -> {
                        groupDir.resolve(jsJarName).exists()
                    }
                    BOTH -> {}
                }
            }
        }

        val libLocalRepoUri = libProject.projectDir.resolve("repo").toURI()

        with(appProject) {
            setupWorkingDir()

            // we use `maven { setUrl(...) }` because this syntax actually works both for Groovy and Kotlin DSLs in Gradle
            gradleBuildScript().appendText("\nrepositories { maven { setUrl(\"$libLocalRepoUri\") } }")

            fun CompiledProject.checkAppBuild(compilerType: KotlinJsCompilerType) {
                assertSuccessful()
                val compileTaskNames = if (jsCompilerType == compilerType) {
                    compileTasksNames.toTypedArray()
                } else {
                    arrayOf(":compileKotlinNodeJs")
                }
                assertTasksExecuted(*compileTaskNames)

                if (jsCompilerType == LEGACY) {
                    projectDir.resolve(targetClassesDir("nodeJs")).resolve("sample-app.js").readText().run {
                        Assert.assertTrue(contains("console.info"))
                        Assert.assertTrue(contains("function nodeJsMain("))
                    }
                }
            }

            build(
                "assemble",
                options = defaultBuildOptions().copy(jsCompilerType = jsCompilerType)
            ) {
                checkAppBuild(jsCompilerType)
            }

            if (jsCompilerType == BOTH) {
                listOf(
                    LEGACY,
                    IR
                ).forEach {
                    build(
                        "assemble",
                        "--rerun-tasks",
                        options = defaultBuildOptions().copy(jsCompilerType = it)
                    ) {
                        checkAppBuild(it)
                    }
                }
            }

            // Now run again with a project dependency instead of a module one:
            libProject.projectDir.copyRecursively(projectDir.resolve(libProject.projectDir.name))
            gradleSettingsScript().appendText("\ninclude(\"${libProject.projectDir.name}\")")
            gradleBuildScript().modify { it.replace("\"com.example:sample-lib:1.0\"", "project(\":${libProject.projectDir.name}\")") }

            gradleBuildScript(libProjectName).takeIf { it.extension == "kts" }?.modify {
                it.replace(Regex("""\.version\(.*\)"""), "")
            }
            gradleBuildScript(subproject = libProject.projectDir.name).modify {
                it.lines().dropLast(5).joinToString(separator = "\n")
            }

            build(
                "clean",
                "assemble",
                "--rerun-tasks",
                options = defaultBuildOptions().copy(jsCompilerType = jsCompilerType)
            ) {
                checkAppBuild(jsCompilerType)
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
    fun testJvmWithJavaEquivalence() = doTestJvmWithJava(testJavaSupportInJvmTargets = false)

    @Test
    fun testJavaSupportInJvmTargets() = doTestJvmWithJava(testJavaSupportInJvmTargets = true)

    private fun doTestJvmWithJava(testJavaSupportInJvmTargets: Boolean) =
        with(Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
            embedProject(Project("sample-lib-gradle-kotlin-dsl", directoryPrefix = "new-mpp-lib-and-app"))
            gradleProperties().apply {
                configureJvmMemory()
            }

            lateinit var classesWithoutJava: Set<String>

            fun getFilePathsSet(inDirectory: String): Set<String> {
                val dir = projectDir.resolve(inDirectory)
                return dir.walk().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }.toSet()
            }

            build("assemble") {
                assertSuccessful()
                classesWithoutJava = getFilePathsSet("build/classes")
            }

            gradleBuildScript().modify {
                if (testJavaSupportInJvmTargets) {
                    it + "\nkotlin.jvm(\"jvm6\") { " +
                            "${KotlinJvmTarget::withJava.name.plus("();").repeat(2)} " + // also check that the function is idempotent
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
                            classpath 'com.github.jengelman.gradle.plugins:shadow:5.0.0'
                        }
                    }
                    
                    apply plugin: 'com.github.johnrengelman.shadow'
                    apply plugin: 'application'
                    apply plugin: 'kotlin-kapt' // Check that Kapts works, generates and compiles sources
                    
                    mainClassName = 'com.example.lib.CommonKt'
                    
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
                if (testJavaSupportInJvmTargets) "src/jvm6${compilationName.capitalize()}/java" else "src/$compilationName/java"

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

            build("clean", "build", "run", "shadowJar") {
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
                    assertNotContains(KotlinJvmWithJavaTargetPreset.DEPRECATION_WARNING)
                } else {
                    assertContains(KotlinJvmWithJavaTargetPreset.DEPRECATION_WARNING)
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
                ":compileKotlinJvmWithJava",
                ":compileJava",
                ":compileTestKotlinJvmWithJava",
                ":compileTestJava",
                // test tasks:
                ":jsTest", // does not run any actual tests for now
                ":jvmWithoutJavaTest",
                ":test"
            )

            val expectedKotlinOutputFiles = listOf(
                *kotlinClassesDir(sourceSet = "jvmWithJava/main").let {
                    arrayOf(
                        it + "com/example/lib/JavaClassUsageKt.class",
                        it + "com/example/lib/CommonKt.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module"
                    )
                },
                *kotlinClassesDir(sourceSet = "jvmWithJava/test").let {
                    arrayOf(
                        it + "com/example/lib/TestCommonCode.class",
                        it + "com/example/lib/TestWithJava.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module" // Note: same name as in main
                    )
                },
                *kotlinClassesDir(sourceSet = "jvmWithoutJava/main").let {
                    arrayOf(
                        it + "com/example/lib/CommonKt.class",
                        it + "com/example/lib/MainKt.class",
                        it + "Script.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module"
                    )
                },
                *kotlinClassesDir(sourceSet = "jvmWithoutJava/test").let {
                    arrayOf(
                        it + "com/example/lib/TestCommonCode.class",
                        it + "com/example/lib/TestWithoutJava.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module" // Note: same name as in main
                    )
                }
            )

            expectedKotlinOutputFiles.forEach { assertFileExists(it) }

            // Gradle 6.6+ slightly changed format of xml test results
            // If, in the test project, preset name was updated,
            // update accordingly test result output for Gradle 6.6+
            val testGradleVersion = chooseWrapperVersionOrFinishTest()
            val expectedTestResults = if (GradleVersion.version(testGradleVersion) < GradleVersion.version("6.6")) {
                "testProject/new-mpp-lib-with-tests/TEST-all-pre6.6.xml"
            } else {
                "testProject/new-mpp-lib-with-tests/TEST-all.xml"
            }

            assertTestResults(
                expectedTestResults,
                "jsNodeTest",
                "test", // jvmTest
                "${nativeHostTargetName}Test"
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
            "compileCommonMainKotlinMetadata", "compileKotlinJvm6", "compileKotlinNodeJs", "compileKotlinLinux64"
        ).forEach {
            build(it) {
                assertSuccessful()
                assertTasksExecuted(":$it")
                assertContains(
                    "-XXLanguage:+InlineClasses",
                    "-progressive", "-opt-in=kotlin.ExperimentalUnsignedTypes",
                    "-opt-in=kotlin.contracts.ExperimentalContracts",
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
    fun testLanguageSettingsConsistency() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets {
                    foo { }
                    bar { dependsOn foo }
                }
            """.trimIndent()
        )

        fun testMonotonousCheck(
            initialSetupForSourceSets: String?,
            sourceSetConfigurationChange: String,
            expectedErrorHint: String
        ) {
            if (initialSetupForSourceSets != null) {
                gradleBuildScript().appendText(
                    "\nkotlin.sourceSets.foo.${initialSetupForSourceSets}\n" + "" +
                            "kotlin.sourceSets.bar.${initialSetupForSourceSets}",
                )
            }
            gradleBuildScript().appendText("\nkotlin.sourceSets.foo.${sourceSetConfigurationChange}")
            build("tasks") {
                assertFailed()
                assertContains(expectedErrorHint)
            }
            gradleBuildScript().appendText("\nkotlin.sourceSets.bar.${sourceSetConfigurationChange}")
            build("tasks") {
                assertSuccessful()
            }
        }

        fun testMonotonousCheck(sourceSetConfigurationChange: String, expectedErrorHint: String): Unit =
            testMonotonousCheck(null, sourceSetConfigurationChange, expectedErrorHint)

        testMonotonousCheck(
            "languageSettings.languageVersion = '1.3'",
            "languageSettings.languageVersion = '1.4'",
            "The language version of the dependent source set must be greater than or equal to that of its dependency."
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
                kotlin.sourceSets.foo.languageSettings {
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
                                runtimeOnly 'com.example:sample-lib:1.0'
                            }
                        }
                    }

                    task('printMetadataFiles') {
                        doFirst {
                            ['Api', 'Implementation', 'CompileOnly', 'RuntimeOnly'].each { kind ->
                                def configuration = configurations.getByName("commonMain${'$'}kind" + '$METADATA_CONFIGURATION_NAME_SUFFIX')
                                configuration.files.each { println '$pathPrefix' + configuration.name + '->' + it.name }
                            }
                        }
                    }
                """.trimIndent()
            )
            val metadataDependencyRegex = "$pathPrefix(.*?)->(.*)".toRegex()

            build("printMetadataFiles") {
                assertSuccessful()

                val expectedFileName = "sample-lib-${KotlinMultiplatformPlugin.METADATA_TARGET_NAME}-1.0.jar"

                val paths = metadataDependencyRegex
                    .findAll(output).map { it.groupValues[1] to it.groupValues[2] }
                    .filter { (_, f) -> "sample-lib" in f }
                    .toSet()

                Assert.assertEquals(
                    listOf("Api", "Implementation", "CompileOnly", "RuntimeOnly").map {
                        "commonMain$it$METADATA_CONFIGURATION_NAME_SUFFIX" to expectedFileName
                    }.toSet(),
                    paths
                )
            }
        }
    }

    @Test
    fun testResolveJsPartOfMppLibDependencyToMetadataWithHmpp() =
        testResolveJsPartOfMppLibDependencyToMetadata(hmppWoCompatibilityMetadataArtifact)

    @Test
    fun testResolveJsPartOfMppLibDependencyToMetadataWithHmppAndCompatibilityMetadataArtifact() =
        testResolveJsPartOfMppLibDependencyToMetadata(hmppWithCompatibilityMetadataArtifact)

    @Test
    fun testResolveJsPartOfMppLibDependencyToMetadataWithoutHmpp() =
        testResolveJsPartOfMppLibDependencyToMetadata(noHMPP)


    private fun testResolveJsPartOfMppLibDependencyToMetadata(hmppFlags: HmppFlags) {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val appProject = Project("sample-app", gradleVersion, "new-mpp-lib-and-app")

        val buildOptions = hmppFlags.buildOptions
        libProject.build(
            "publish",
            options = buildOptions.copy(jsCompilerType = BOTH)
        ) {
            assertSuccessful()
        }
        val localRepo = libProject.projectDir.resolve("repo")
        val localRepoUri = localRepo.toURI()

        with(appProject) {
            setupWorkingDir()

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
                                runtimeOnly 'com.example:sample-lib-nodejs:1.0'
                            }
                        }
                    }

                    task('printMetadataFiles') {
                        doFirst {
                            ['Api', 'Implementation', 'CompileOnly', 'RuntimeOnly'].each { kind ->
                                def configuration = configurations.getByName("nodeJsMain${'$'}kind" + '$METADATA_CONFIGURATION_NAME_SUFFIX')
                                configuration.files.each { println '$pathPrefix' + configuration.name + '->' + it.name }
                            }
                        }
                    }
                """.trimIndent()
            )
            val metadataDependencyRegex = "$pathPrefix(.*?)->(.*)".toRegex()

            build(
                "printMetadataFiles",
                options = buildOptions.copy(jsCompilerType = IR)
            ) {
                assertSuccessful()

                val expectedFileName = "sample-lib-nodejsir-1.0.klib"

                val paths = metadataDependencyRegex
                    .findAll(output).map { it.groupValues[1] to it.groupValues[2] }
                    .filter { (_, f) -> "sample-lib" in f }
                    .toSet()

                Assert.assertEquals(
                    listOf("Api", "Implementation", "CompileOnly", "RuntimeOnly").map {
                        "nodeJsMain$it$METADATA_CONFIGURATION_NAME_SUFFIX" to expectedFileName
                    }.toSet(),
                    paths
                )
            }
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
            gradleBuildScript(libProject.projectDir.name).modify {
                it.lines().dropLast(5).joinToString(separator = "\n")
            }
            projectDir.resolve("settings.gradle").appendText("\ninclude '${libProject.projectDir.name}'")
            gradleBuildScript().modify {
                it.replace("'com.example:sample-lib:1.0'", "project(':${libProject.projectDir.name}')") +
                        "\n" + """
                        task('printMetadataFiles') {
                           doFirst {
                               configurations.getByName('commonMainImplementation$METADATA_CONFIGURATION_NAME_SUFFIX')
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
        val publishedVariant = nativeHostTargetName
        val nonPublishedVariant = unsupportedNativeTargets[0]

        build("publish") {
            assertSuccessful()

            assertFileExists("repo/com/example/sample-lib-$publishedVariant/1.0/sample-lib-$publishedVariant-1.0.klib")
            assertNoSuchFile("repo/com/example/sample-lib-$nonPublishedVariant") // check that no artifacts are published for that variant

            // but check that the module metadata contains all variants:
            val gradleModuleMetadata = projectDir.resolve("repo/com/example/sample-lib/1.0/sample-lib-1.0.module").readText()
            assertTrue(""""name": "linux64ApiElements-published"""" in gradleModuleMetadata)
            assertTrue(""""name": "mingw64ApiElements-published"""" in gradleModuleMetadata)
            assertTrue(""""name": "macos64ApiElements-published"""" in gradleModuleMetadata)
        }
    }

    @Test
    fun testNonMppConsumersOfLibraryPublishedWithNoMetadataOptIn() {
        val repoDir = with(transformNativeTestProject("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
            build(
                "publish",
                options = hmppWithCompatibilityMetadataArtifact.buildOptions
            ) { assertSuccessful() }
            projectDir.resolve("repo")
        }

        with(Project("sample-old-style-app", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nallprojects { repositories { maven { url '${repoDir.toURI()}' } } }")
            gradleBuildScript("app-jvm").modify { it.replace("com.example:sample-lib:", "com.example:sample-lib-jvm6:") }
            gradleBuildScript("app-js").modify { it.replace("com.example:sample-lib:", "com.example:sample-lib-nodejs:") }

            build("assemble", "run") {
                assertSuccessful()
                assertTasksExecuted(":app-common:compileKotlinCommon", ":app-jvm:compileKotlin", ":app-jvm:run", ":app-js:compileKotlin2Js")
            }

            // Then run again without even reading the metadata from the repo:
            projectDir.resolve("settings.gradle").modify { it.replace("enableFeaturePreview('GRADLE_METADATA')", "") }

            build("assemble", "run", "--rerun-tasks") {
                assertSuccessful()
                assertTasksExecuted(":app-common:compileKotlinCommon", ":app-jvm:compileKotlin", ":app-jvm:run", ":app-js:compileKotlin2Js")
            }
        }

        with(Project("sample-app-without-kotlin", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            gradleBuildScript().modify {
                it.replace("com.example:sample-lib:1.0", "com.example:sample-lib-jvm6:1.0") + "\n" + """
                    repositories { maven { url '${repoDir.toURI()}' } }
                """.trimIndent()
            }

            build("run") {
                assertSuccessful()
                assertTasksExecuted(":compileJava", ":run")
            }

            // Then run again without even reading the metadata from the repo:
            projectDir.resolve("settings.gradle").modify { it.replace("enableFeaturePreview('GRADLE_METADATA')", "") }
            build("run", "--rerun-tasks") {
                assertSuccessful()
                assertTasksExecuted(":compileJava", ":run")
            }
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
                setOf("commonMain", "jvm6Main", "linux64Main", "linuxMipsel32Main", "macos64Main", "mingw64Main", "mingw86Main", "nodeJsMain", "wasmMain"),
                sourceJarSourceRoots[null]
            )
            assertEquals(setOf("commonMain", "jvm6Main"), sourceJarSourceRoots["jvm6"])
            assertEquals(setOf("commonMain", "nodeJsMain"), sourceJarSourceRoots["nodejs"])
            assertEquals(setOf("commonMain", "linux64Main"), sourceJarSourceRoots["linux64"])
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
                        maven { url "file://${'$'}{rootProject.projectDir.absolutePath.replace('\\', '/')}/../sample-lib/repo" }
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
            gradleBuildScript("sample-app").modify {
                it.lines().dropLast(5).joinToString(separator = "\n")
            }

            gradleSettingsScript().writeText("include 'sample-app'") // disables feature preview 'GRADLE_METADATA', resets rootProject name
            gradleBuildScript("sample-app").modify {
                it.replace("'com.example:sample-lib:1.0'", "project(':')") + "\n" + """
                apply plugin: 'maven-publish'
                group = "com.exampleapp"
                version = "1.0"
                publishing {
                    repositories {
                        maven { url "file://${'$'}{rootProject.projectDir.absolutePath.replace('\\', '/')}/repo" }
                    }
                }
                """.trimIndent()
            }

            gradleSettingsScript().appendText("\nenableFeaturePreview(\"GRADLE_METADATA\")")
            // Add a dependency that is resolved with metadata:
            gradleBuildScript("sample-app").appendText(
                "\n" + """
                repositories {
                    maven { url "file://${'$'}{rootProject.projectDir.absolutePath.replace('\\', '/')}/repo" }
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

            build("clean", "publish") {
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

        build("assemble", printOptionsTaskName) {
            assertSuccessful()
            assertTasksExecuted(*listOf("Jvm6", "NodeJs", "Linux64").map { ":compileKotlin$it" }.toTypedArray())
            assertFileExists("build/classes/kotlin/jvm6/main/com/example/Annotated.class")
            assertFileExists("build/classes/kotlin/jvm6/main/com/example/Override.class")
            assertFileContains("build/classes/kotlin/nodeJs/main/sample-lib.js", "Override")

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
    fun testJsDceInMpp() = with(Project("new-mpp-js-dce", gradleVersion)) {
        build("runRhino", options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)) {
            assertSuccessful()
            assertTasksExecuted(":mainProject:runDceNodeJsKotlin")

            val pathPrefix = "mainProject/build/kotlin-js-min/nodeJs/main"
            assertFileExists("$pathPrefix/exampleapp.js.map")
            assertFileExists("$pathPrefix/examplelib.js.map")
            assertFileContains("$pathPrefix/exampleapp.js.map", "\"../../../../src/nodeJsMain/kotlin/exampleapp/main.kt\"")

            assertFileExists("$pathPrefix/kotlin.js")
            assertTrue(fileInWorkingDir("$pathPrefix/kotlin.js").length() < 500 * 1000, "Looks like kotlin.js file was not minified by DCE")
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
                "jvm6", "nodeJs", "mingw64", "mingw86", "linux64", "macos64", "linuxMipsel32", "wasm"
            ).flatMapTo(mutableSetOf()) { target ->
                listOf("main", "test").map { compilation ->
                    Triple(target, compilation, "$target${compilation.capitalize()}")
                }
            } + Triple("metadata", "main", "commonMain")

            assertEquals(expectedDefaultSourceSets, actualDefaultSourceSets)
        }
    }

    @Test
    fun testDependenciesDsl() = with(transformProjectWithPluginsDsl("newMppDependenciesDsl")) {
        val originalBuildscriptContent = gradleBuildScript("app").readText()

        fun testDependencies() = testResolveAllConfigurations("app") {
            assertContains(">> :app:testNonTransitiveStringNotationApiDependenciesMetadata --> junit-4.13.2.jar")
            assertEquals(
                1,
                (Regex.escape(">> :app:testNonTransitiveStringNotationApiDependenciesMetadata") + " .*").toRegex().findAll(output).count()
            )

            assertContains(">> :app:testNonTransitiveDependencyNotationApiDependenciesMetadata --> kotlin-reflect-${defaultBuildOptions().kotlinVersion}.jar")
            assertEquals(
                1,
                (Regex.escape(">> :app:testNonTransitiveStringNotationApiDependenciesMetadata") + " .*").toRegex().findAll(output)
                    .count()
            )

            assertContains(">> :app:testExplicitKotlinVersionApiDependenciesMetadata --> kotlin-reflect-1.3.0.jar")
            assertContains(">> :app:testExplicitKotlinVersionImplementationDependenciesMetadata --> kotlin-reflect-1.2.71.jar")
            assertContains(">> :app:testExplicitKotlinVersionCompileOnlyDependenciesMetadata --> kotlin-reflect-1.2.70.jar")
            assertContains(">> :app:testExplicitKotlinVersionRuntimeOnlyDependenciesMetadata --> kotlin-reflect-1.2.60.jar")

            assertContains(">> :app:testProjectWithConfigurationApiDependenciesMetadata --> output.txt")
        }

        testDependencies()

        // Then run with Gradle Kotlin DSL; the build script needs some correction to be a valid GK DSL script:
        gradleBuildScript("app").run {
            modify {
                originalBuildscriptContent
                    .replace(": ", " = ")
                    .replace("def ", " val ")
                    .replace("new File(cacheRedirectorFile)", "File(cacheRedirectorFile)")
                    .replace("id \"org.jetbrains.kotlin.test.fixes.android\"", "id(\"org.jetbrains.kotlin.test.fixes.android\")")
            }
            renameTo(projectDir.resolve("app/build.gradle.kts"))
        }

        testDependencies()
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
            assertContains(UnusedSourceSetsChecker.WARNING_PREFIX_ONE, UnusedSourceSetsChecker.WARNING_INTRO)
        }

        gradleBuildScript().appendText("\nkotlin { sourceSets { bar { dependsOn foo } } }")

        build {
            assertSuccessful()
            assertContains(UnusedSourceSetsChecker.WARNING_PREFIX_MANY, UnusedSourceSetsChecker.WARNING_INTRO)
        }

        gradleBuildScript().appendText("\nkotlin { sourceSets { jvm6Main { dependsOn bar } } }")

        build {
            assertSuccessful()
            assertNotContains(
                UnusedSourceSetsChecker.WARNING_PREFIX_ONE,
                UnusedSourceSetsChecker.WARNING_PREFIX_MANY,
                UnusedSourceSetsChecker.WARNING_INTRO
            )
        }
    }

    @Test
    fun testIncrementalCompilation() = with(Project("new-mpp-jvm-js-ic", gradleVersion)) {
        build("build") {
            assertSuccessful()
        }

        val libCommonClassKt = projectDir.getFileByName("LibCommonClass.kt")
        val libCommonClassJsKt = projectDir.getFileByName("LibCommonClassJs.kt")
        libCommonClassJsKt.modify { it.checkedReplace("platform = \"js\"", "platform = \"Kotlin/JS\"") }

        val libCommonClassJvmKt = projectDir.getFileByName("LibCommonClassJvm.kt")
        libCommonClassJvmKt.modify { it.checkedReplace("platform = \"jvm\"", "platform = \"Kotlin/JVM\"") }
        build("build") {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(libCommonClassKt, libCommonClassJsKt, libCommonClassJvmKt))
        }

        val libJvmPlatformUtilKt = projectDir.getFileByName("libJvmPlatformUtil.kt")
        libJvmPlatformUtilKt.modify {
            it.checkedReplace("fun libJvmPlatformUtil", "inline fun libJvmPlatformUtil")
        }
        build("build") {
            assertSuccessful()
            val useLibJvmPlatformUtilKt = projectDir.getFileByName("useLibJvmPlatformUtil.kt")
            assertCompiledKotlinSources(project.relativize(libJvmPlatformUtilKt, useLibJvmPlatformUtilKt))
        }

        val libJsPlatformUtilKt = projectDir.getFileByName("libJsPlatformUtil.kt")
        libJsPlatformUtilKt.modify {
            it.checkedReplace("fun libJsPlatformUtil", "inline fun libJsPlatformUtil")
        }
        build("build") {
            assertSuccessful()
            val useLibJsPlatformUtilKt = projectDir.getFileByName("useLibJsPlatformUtil.kt")
            assertCompiledKotlinSources(project.relativize(libJsPlatformUtilKt, useLibJsPlatformUtilKt))
        }
    }

    @Test
    fun testPomRewritingInSinglePlatformProject() = with(Project("kt-27059-pom-rewriting")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        val groupDir = "build/repo/com/example/"

        build(":mpp-lib:publish", options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)) {
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

            build(*params, options = defaultBuildOptions().copy(warningMode = WarningMode.Summary)) {
                assertSuccessful()
                assertTasksExecuted(":jvm-app:publishMainPublicationToMavenRepository")
                assertTasksExecuted(":js-app:publishMainPublicationToMavenRepository")

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
                    assertTrue("The JS POM should contain the dependency on 'mpp-lib' rewritten as 'mpp-lib-js'") {
                        jsPom.contains(
                            "<groupId>com.example</groupId><artifactId>mpp-lib-js</artifactId><version>1.0</version><scope>compile</scope>"
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

            val tasks = listOf("jvm", "js", "linux64").map { ":compileIntegrationTestKotlin${it.capitalize()}" }

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
                assertFileExists("build/classes/kotlin/jvm/integrationTest/META-INF/new-mpp-associate-compilations.kotlin_module")

                // JS:
                assertFileExists(
                    "build/classes/kotlin/js/integrationTest/new-mpp-associate-compilations_integrationTest.js"
                )

                // Native:
                assertFileExists("build/classes/kotlin/linux64/integrationTest/klib/new-mpp-associate-compilations_integrationTest.klib")
            }

            gradleBuildScript().appendText(
                "\nkotlin.sourceSets { getByName(\"commonTest\").requiresVisibilityOf(getByName(\"commonIntegrationTest\")) }"
            )
            build {
                assertFailed()
                assertContains(UnsatisfiedSourceSetVisibilityException::class.java.simpleName)
            }
        }
    }

    @Test
    fun testTestRunsApi() = with(Project("new-mpp-associate-compilations")) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        // TOOD: add Kotlin/JS tests once they can be tested without much performance overhead
        val targetsToTest = listOf("jvm", nativeHostTargetName) + listOf("ios").takeIf { HostManager.hostIsMac }.orEmpty()
        val testTasks = targetsToTest.flatMap { listOf(":${it}Test", ":${it}IntegrationTest") }.toTypedArray()

        build(*testTasks) {
            assertSuccessful()

            assertTasksExecuted(
                *testTasks,
                ":compileIntegrationTestKotlinJvm",
                ":linkIntegrationDebugTest${nativeHostTargetName.capitalize()}"
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
    fun testWasmJs() = with(Project("new-mpp-wasm-js", gradleVersion)) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)
        build("build") {
            assertSuccessful()
            assertTasksExecuted(":compileKotlinJs")
            assertTasksExecuted(":compileKotlinWasm")

            val outputPrefix = "build/js/packages/"

            val jsOutput = outputPrefix + "redefined-js-module-name/kotlin/"
            assertFileExists(jsOutput + "redefined-js-module-name.js")

            val wasmOutput = outputPrefix + "redefined-wasm-module-name/kotlin/"
            assertFileExists(wasmOutput + "redefined-wasm-module-name.js")
            assertFileExists(wasmOutput + "redefined-wasm-module-name.wasm")
        }
    }

    private fun detectNativeEnabledCompilation(): String = when {
        HostManager.hostIsLinux -> "linuxX64"
        HostManager.hostIsMingw -> "mingwX64"
        HostManager.hostIsMac -> "macosX64"
        else -> throw AssertionError("Host ${HostManager.host} is not supported for this test")
    }
}
