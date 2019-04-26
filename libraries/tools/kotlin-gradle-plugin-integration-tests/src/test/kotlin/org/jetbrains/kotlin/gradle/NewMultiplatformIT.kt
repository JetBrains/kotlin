/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.ProjectLocalConfigurations
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmWithJavaTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.UnusedSourceSetsChecker
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.plugin.sources.SourceSetConsistencyChecks
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assert
import org.junit.Test
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class NativeTargets(val current: String, val supported: List<String>, val unsupported: List<String>)

fun configure(): NativeTargets {
    val all = listOf("linux64", "macos64", "mingw64", "wasm32")

    val current = when {
        HostManager.hostIsMingw -> "mingw64"
        HostManager.hostIsLinux -> "linux64"
        HostManager.hostIsMac -> "macos64"
        else -> error("Unknown host")
    }

    val unsupported = when {
        HostManager.hostIsMingw -> listOf("macos64")
        HostManager.hostIsLinux -> listOf("macos64", "mingw64")
        HostManager.hostIsMac -> listOf("mingw64")
        else -> error("Unknown host")
    }

    val supported = all.filter { !unsupported.contains(it) }

    return NativeTargets(current, supported, unsupported)
}

class NewMultiplatformIT : BaseGradleIT() {
    val gradleVersion = GradleVersionRequired.AtLeast("4.7")

    val nativeHostTargetName = configure().current
    val supportedNativeTargets = configure().supported
    val unsupportedNativeTargets = configure().unsupported

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    @Test
    fun testLibAndApp() = doTestLibAndApp(
        "sample-lib",
        "sample-app"
    )

    @Test
    fun testLibAndAppWithGradleKotlinDsl() = doTestLibAndApp(
        "sample-lib-gradle-kotlin-dsl",
        "sample-app-gradle-kotlin-dsl",
        GradleVersionRequired.AtLeast("4.9") // earlier Gradle versions fail at accessors codegen
    )

    private fun doTestLibAndApp(
        libProjectName: String, appProjectName: String,
        gradleVersionRequired: GradleVersionRequired = gradleVersion
    ) {
        val libProject = transformProjectWithPluginsDsl(libProjectName, gradleVersionRequired, "new-mpp-lib-and-app")
        val appProject = transformProjectWithPluginsDsl(appProjectName, gradleVersionRequired, "new-mpp-lib-and-app")
        val oldStyleAppProject = Project("sample-old-style-app", gradleVersionRequired, "new-mpp-lib-and-app")

        val compileTasksNames =
            listOf("Jvm6", "NodeJs", "Metadata", "Wasm32", nativeHostTargetName.capitalize()).map { ":compileKotlin$it" }

        with(libProject) {
            build("publish") {
                assertSuccessful()
                assertTasksExecuted(*compileTasksNames.toTypedArray(), ":jvm6Jar", ":nodeJsJar", ":metadataJar")

                val groupDir = projectDir.resolve("repo/com/example")
                val jvmJarName = "sample-lib-jvm6/1.0/sample-lib-jvm6-1.0.jar"
                val jsJarName = "sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.jar"
                val metadataJarName = "sample-lib-metadata/1.0/sample-lib-metadata-1.0.jar"
                val wasmKlibName = "sample-lib-wasm32/1.0/sample-lib-wasm32-1.0.klib"
                val nativeKlibName = "sample-lib-$nativeHostTargetName/1.0/sample-lib-$nativeHostTargetName-1.0.klib"

                listOf(jvmJarName, jsJarName, metadataJarName, "sample-lib/1.0/sample-lib-1.0.module").forEach {
                    Assert.assertTrue("$it should exist", groupDir.resolve(it).exists())
                }

                val gradleMetadata = groupDir.resolve("sample-lib/1.0/sample-lib-1.0.module").readText()
                assertFalse(gradleMetadata.contains(ProjectLocalConfigurations.ATTRIBUTE.name))

                listOf(jvmJarName, jsJarName, metadataJarName, wasmKlibName, nativeKlibName).forEach {
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
                Assert.assertTrue("com/example/lib/CommonKt.kotlin_metadata" in metadataJarEntries)

                Assert.assertTrue(groupDir.resolve(wasmKlibName).exists())
                Assert.assertTrue(groupDir.resolve(nativeKlibName).exists())
            }
        }

        val libLocalRepoUri = libProject.projectDir.resolve("repo").toURI()

        with(appProject) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nrepositories { maven { setUrl(\"$libLocalRepoUri\") } }")

            fun CompiledProject.checkProgramCompilationCommandLine(check: (String) -> Unit) {
                output.lineSequence().filter {
                    it.contains("Run tool: konanc") && it.contains("-p program")
                }.toList().also {
                    assertTrue(it.isNotEmpty())
                }.forEach(check)
            }

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

                projectDir.resolve(targetClassesDir("metadata")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.kotlin_metadata").exists())
                }

                projectDir.resolve(targetClassesDir("nodeJs")).resolve("sample-app.js").readText().run {
                    Assert.assertTrue(contains("console.info"))
                    Assert.assertTrue(contains("function nodeJsMain("))
                }

                projectDir.resolve(targetClassesDir("wasm32")).run {
                    Assert.assertTrue(resolve("sample-app.klib").exists())
                }

                assertFileExists("build/bin/wasm32/mainDebugExecutable/main.wasm.js")
                assertFileExists("build/bin/wasm32/mainDebugExecutable/main.wasm")
                assertFileExists("build/bin/wasm32/mainReleaseExecutable/main.wasm.js")
                assertFileExists("build/bin/wasm32/mainReleaseExecutable/main.wasm")

                val nativeExeName = if (isWindows) "main.exe" else "main.kexe"
                assertFileExists("build/bin/$nativeHostTargetName/mainReleaseExecutable/$nativeExeName")
                assertFileExists("build/bin/$nativeHostTargetName/mainDebugExecutable/$nativeExeName")

                // Check that linker options were correctly passed to the K/N compiler.
                checkProgramCompilationCommandLine {
                    assertTrue(it.contains("-linker-option -L."))
                }
            }

            build("assemble", "resolveRuntimeDependencies") {
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

            build("clean", "assemble", "--rerun-tasks") {
                checkAppBuild()
            }

            // Check that binary getters initially introduced in 1.3 work.
            build("checkBinaryGetters") {
                assertTrue(output.contains("Wasm binary file: main.wasm"))
                assertTrue(output.contains("Wasm link task: linkMainReleaseExecutableWasm32"))
                assertTrue(output.contains("Wasm link task name: linkMainReleaseExecutableWasm32"))

                val testFiles = listOf(
                    "MacOS" to "test.kexe",
                    "Windows" to "test.exe",
                    "Linux" to "test.kexe"
                )

                val testLinkTasks = listOf(
                    "MacOS" to "linkTestDebugExecutableMacos64",
                    "Windows" to "linkTestDebugExecutableMingw64",
                    "Linux" to "linkTestDebugExecutableLinux64"
                )

                testFiles.forEach { (platform, file) ->
                    assertTrue(output.contains("$platform test file: $file"), "Cannot get test binary for platform $platform")
                }

                testLinkTasks.forEach { (platform, task) ->
                    assertTrue(output.contains("$platform test link task: $task"), "Cannot get test link task for platform $platform")
                }
            }
        }

        with(oldStyleAppProject) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nallprojects { repositories { maven { url '$libLocalRepoUri' } } }")

            build("assemble") {
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

    @Test
    fun testMavenPublishAppliedBeforeMultiplatformPlugin() =
        with(Project("sample-lib", GradleVersionRequired.AtLeast("5.0"), "new-mpp-lib-and-app")) {
            setupWorkingDir()

            gradleBuildScript().modify { "apply plugin: 'maven-publish'\n$it" }

            build {
                assertSuccessful()
            }
        }

    @Test
    fun testResourceProcessing() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        val targetsWithResources = listOf("jvm6", "nodeJs", "wasm32", nativeHostTargetName)
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

    @Test
    fun testSourceSetCyclicDependencyDetection() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()
        gradleBuildScript().appendText("\n" + """
            kotlin.sourceSets {
                a
                b { dependsOn a }
                c { dependsOn b }
                a.dependsOn(c)
            }
        """.trimIndent())

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
        with(Project("sample-lib", GradleVersionRequired.AtLeast("5.0"), "new-mpp-lib-and-app")) {
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
                    it + "\nkotlin.jvm(\"jvm6\").${KotlinJvmTarget::withJava.name}()"
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
                    
                    mainClassName = 'com.example.lib.CommonKt'
                    """.trimIndent()
                )
            }

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
                        "kotlin/jvm6/main/com/example/lib/KotlinClassInJava.class",
                        "java/main/com/example/lib/JavaClassInJava.class",
                        "java/test/com/example/lib/JavaTest.class"
                    )
                val actualClasses = getFilePathsSet("build/classes")
                Assert.assertEquals(expectedMainClasses, actualClasses)

                val jvmTestTaskName = if (testJavaSupportInJvmTargets) "jvm6Test" else "test"
                assertTasksExecuted(":$jvmTestTaskName")
                assertFileExists("build/reports/tests/$jvmTestTaskName/classes/com.example.lib.JavaTest.html")

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
    fun testLibWithTests() = doTestLibWithTests(Project("new-mpp-lib-with-tests", gradleVersion))

    @Test
    fun testLibWithTestsKotlinDsl() = with(Project("new-mpp-lib-with-tests", gradleVersion)) {
        setupWorkingDir()
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
                kotlinClassesDir(sourceSet = "js/main") + "new-mpp-lib-with-tests.js",
                kotlinClassesDir(sourceSet = "js/test") + "new-mpp-lib-with-tests_test.js",
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
        }
    }

    @Test
    fun testLanguageSettingsApplied() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets.all {
                    it.languageSettings {
                        languageVersion = '1.3'
                        apiVersion = '1.3'
                        enableLanguageFeature('InlineClasses')
                        useExperimentalAnnotation('kotlin.ExperimentalUnsignedTypes')
                        useExperimentalAnnotation('kotlin.contracts.ExperimentalContracts')
                        progressiveMode = true
                    }
                }
            """.trimIndent()
        )

        listOf("compileKotlinJvm6", "compileKotlinNodeJs", "compileKotlin${nativeHostTargetName.capitalize()}").forEach {
            build(it) {
                assertSuccessful()
                assertTasksExecuted(":$it")
                assertContains(
                    "-language-version 1.3", "-api-version 1.3", "-XXLanguage:+InlineClasses",
                    " -progressive", "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes",
                    "-Xuse-experimental=kotlin.contracts.ExperimentalContracts"
                )
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

        fun testMonotonousCheck(sourceSetConfigurationChange: String, expectedErrorHint: String) {
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

        testMonotonousCheck(
            "languageSettings.languageVersion = '1.4'",
            SourceSetConsistencyChecks.languageVersionCheckHint
        )

        testMonotonousCheck(
            "languageSettings.enableLanguageFeature('InlineClasses')",
            SourceSetConsistencyChecks.unstableFeaturesHint
        )

        testMonotonousCheck(
            "languageSettings.useExperimentalAnnotation('kotlin.ExperimentalUnsignedTypes')",
            SourceSetConsistencyChecks.experimentalAnnotationsInUseHint
        )

        // check that enabling a bugfix feature and progressive mode or advancing API level
        // don't require doing the same for dependent source sets:
        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets.foo.languageSettings {
                    apiVersion = '1.3'
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
    fun testPublishWithoutGradleMetadata() {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")

        with(libProject) {
            setupWorkingDir()
            projectDir.resolve("settings.gradle").modify { it.replace("enableFeaturePreview", "// enableFeaturePreview") }

            build("publish") {
                assertSuccessful()
                val groupRepoDir = "repo/com/example"

                // No root publication:
                assertNoSuchFile("$groupRepoDir/sample-lib")

                // Check that the platform publications have the metadata dependency
                listOf("jvm6", "nodejs", "wasm32", nativeHostTargetName.toLowerCase()).forEach { targetAppendix ->
                    val targetPomPath = "$groupRepoDir/sample-lib-$targetAppendix/1.0/sample-lib-$targetAppendix-1.0.pom"
                    assertFileContains(
                        targetPomPath,
                        "<groupId>com.example</groupId>",
                        "<artifactId>sample-lib-metadata</artifactId>",
                        "<version>1.0</version>"
                    )
                }
            }
        }
    }

    @Test
    fun testDependenciesOnMppLibraryPartsWithNoMetadata() {
        val repoDir = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            projectDir.resolve("settings.gradle").modify { it.replace("enableFeaturePreview", "// enableFeaturePreview") }
            build("publish") { assertSuccessful() }
            projectDir.resolve("repo")
        }

        val dependencies = buildString {
            append("dependencies {\n")
            append("    allJvmImplementation 'com.example:sample-lib-jvm6:1.0'\n")
            append("    nodeJsMainImplementation 'com.example:sample-lib-nodejs:1.0'\n")
            for (target in supportedNativeTargets) {
                append("    ${target}MainImplementation 'com.example:sample-lib-$target:1.0'\n")
            }
            append("}")
        }

        with(Project("sample-app", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            gradleBuildScript().modify {
                it.replace("implementation 'com.example:sample-lib:1.0'", "implementation 'com.example:sample-lib-metadata:1.0'") +
                "\nrepositories { maven { url '${repoDir.toURI()}' } }\n\n" + dependencies
            }

            build("assemble") {
                assertSuccessful()
                assertTasksExecuted(listOf("Jvm6", "NodeJs", "Wasm32", nativeHostTargetName.capitalize()).map { ":compileKotlin$it" })
            }
        }
    }

    @Test
    fun testPublishingOnlySupportedNativeTargets() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        val publishedVariant = nativeHostTargetName
        val nonPublishedVariant = unsupportedNativeTargets[0]

        build("publish") {
            assertSuccessful()

            assertFileExists("repo/com/example/sample-lib-$publishedVariant/1.0/sample-lib-$publishedVariant-1.0.klib")
            assertNoSuchFile("repo/com/example/sample-lib-$nonPublishedVariant") // check that no artifacts are published for that variant

            // but check that the module metadata contains all variants:
            val gradleModuleMetadata = projectDir.resolve("repo/com/example/sample-lib/1.0/sample-lib-1.0.module").readText()
            assertTrue(""""name": "linux64-api"""" in gradleModuleMetadata)
            assertTrue(""""name": "mingw64-api"""" in gradleModuleMetadata)
            assertTrue(""""name": "macos64-api"""" in gradleModuleMetadata)
        }
    }

    @Test
    fun testNonMppConsumersOfLibraryPublishedWithNoMetadataOptIn() {
        val repoDir = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            projectDir.resolve("settings.gradle").modify { it.replace("enableFeaturePreview", "// enableFeaturePreview") }
            build("publish") { assertSuccessful() }
            projectDir.resolve("repo")
        }

        with(Project("sample-old-style-app", gradleVersion, "new-mpp-lib-and-app")) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nallprojects { repositories { maven { url '${repoDir.toURI()}' } } }")
            gradleBuildScript("app-common").modify { it.replace("com.example:sample-lib:", "com.example:sample-lib-metadata:") }
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
    fun testOptionalExpectations() = with(Project("new-mpp-lib-with-tests", gradleVersion)) {
        setupWorkingDir()

        projectDir.resolve("src/commonMain/kotlin/Optional.kt").writeText(
            """
            @file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")
            @OptionalExpectation
            expect annotation class Optional(val value: String)

            @Optional("optionalAnnotationValue")
            class OptionalCommonUsage
            """.trimIndent()
        )

        build("compileKotlinJvmWithoutJava", "compileKotlin${nativeHostTargetName.capitalize()}") {
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

        projectDir.resolve("src/${nativeHostTargetName}Main/kotlin/").also {
            it.mkdirs()
            it.resolve("OptionalImpl.kt").writeText(optionalImplText)
        }

        build("compileKotlin${nativeHostTargetName.capitalize()}") {
            assertFailed()
            assertContains("Declaration annotated with '@OptionalExpectation' can only be used in common module sources", ignoreCase = true)
        }
    }

    @Test
    fun testCanProduceNativeLibraries() = with(Project("new-mpp-native-libraries", gradleVersion)) {
        val baseName = "main"

        val sharedPrefix = CompilerOutputKind.DYNAMIC.prefix(HostManager.host)
        val sharedSuffix = CompilerOutputKind.DYNAMIC.suffix(HostManager.host)
        val sharedPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugShared/$sharedPrefix$baseName$sharedSuffix",
            "build/bin/$nativeHostTargetName/mainReleaseShared/$sharedPrefix$baseName$sharedSuffix"
        )

        val staticPrefix = CompilerOutputKind.STATIC.prefix(HostManager.host)
        val staticSuffix = CompilerOutputKind.STATIC.suffix(HostManager.host)
        val staticPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugStatic/$staticPrefix$baseName$staticSuffix",
            "build/bin/$nativeHostTargetName/mainReleaseStatic/$staticPrefix$baseName$staticSuffix"
        )

        val headerPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugShared/$sharedPrefix${baseName}_api.h",
            "build/bin/$nativeHostTargetName/mainReleaseShared/$sharedPrefix${baseName}_api.h",
            "build/bin/$nativeHostTargetName/mainDebugStatic/$staticPrefix${baseName}_api.h",
            "build/bin/$nativeHostTargetName/mainReleaseStatic/$staticPrefix${baseName}_api.h"
        )

        val klibPrefix = CompilerOutputKind.LIBRARY.prefix(HostManager.host)
        val klibSuffix = CompilerOutputKind.LIBRARY.suffix(HostManager.host)
        val klibPath = "${targetClassesDir(nativeHostTargetName)}${klibPrefix}native-lib$klibSuffix"

        val frameworkPrefix = CompilerOutputKind.FRAMEWORK.prefix(HostManager.host)
        val frameworkSuffix = CompilerOutputKind.FRAMEWORK.suffix(HostManager.host)
        val frameworkPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix.dSYM",
            "build/bin/$nativeHostTargetName/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix",
            "build/bin/$nativeHostTargetName/mainReleaseFramework/$frameworkPrefix$baseName$frameworkSuffix"
        )
            .takeIf { HostManager.hostIsMac }
            .orEmpty()

        val taskSuffix = nativeHostTargetName.capitalize()
        val linkTasks = listOf(
            ":linkMainDebugShared$taskSuffix",
            ":linkMainReleaseShared$taskSuffix",
            ":linkMainDebugStatic$taskSuffix",
            ":linkMainReleaseStatic$taskSuffix"
        )

        val klibTask = ":compileKotlin$taskSuffix"

        val frameworkTasks = listOf(":linkMainDebugFramework$taskSuffix", ":linkMainReleaseFramework$taskSuffix")
            .takeIf { HostManager.hostIsMac }
            .orEmpty()

        // Building
        build("assemble") {
            assertSuccessful()

            sharedPaths.forEach { assertFileExists(it) }
            staticPaths.forEach { assertFileExists(it) }
            headerPaths.forEach { assertFileExists(it) }
            frameworkPaths.forEach { assertFileExists(it) }
            assertFileExists(klibPath)
        }

        // Test that all up-to date checks are correct
        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks)
            assertTasksUpToDate(frameworkTasks)
            assertTasksUpToDate(klibTask)
        }

        // Remove outputs and check that they are rebuilt.
        assertTrue(projectDir.resolve(headerPaths[0]).delete())
        assertTrue(projectDir.resolve(klibPath).delete())
        if (HostManager.hostIsMac) {
            assertTrue(projectDir.resolve(frameworkPaths[0]).deleteRecursively())
        }

        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks.drop(1))
            assertTasksExecuted(linkTasks[0])
            assertTasksExecuted(klibTask)

            if (HostManager.hostIsMac) {
                assertTasksUpToDate(frameworkTasks.drop(1))
                assertTasksExecuted(frameworkTasks[0])
            }
        }
    }

    @Test
    fun testNativeBinaryKotlinDSL() = doTestNativeBinaryDSL("kotlin-dsl")

    @Test
    fun testNativeBinaryGroovyDSL() = doTestNativeBinaryDSL("groovy-dsl")

    private fun doTestNativeBinaryDSL(
        projectName: String,
        gradleVersionRequired: GradleVersionRequired = gradleVersion
    ) = with(transformProjectWithPluginsDsl(projectName, gradleVersionRequired, "new-mpp-native-binaries")) {
        val hostSuffix = nativeHostTargetName.capitalize()
        val binaries = mutableListOf(
            "debugExecutable" to "native-binary",
            "releaseExecutable" to "native-binary",
            "fooDebugExecutable" to "foo",
            "fooReleaseExecutable" to "foo",
            "barReleaseExecutable" to "bar",
            "bazReleaseExecutable" to "my-baz",
            "testDebugExecutable" to "test",
            "test2ReleaseExecutable" to "test2",
            "releaseStatic" to "native_binary",
            "releaseShared" to "native_binary"
        )

        val linkTasks = binaries.map { (name, _) -> "link${name.capitalize()}$hostSuffix" }
        val outputFiles = binaries.map { (name, fileBaseName) ->
            val outputKind = NativeOutputKind.values().single { name.endsWith(it.taskNameClassifier, true) }.compilerOutputKind
            val prefix = outputKind.prefix(HostManager.host)
            val suffix = outputKind.suffix(HostManager.host)
            val fileName = "$prefix$fileBaseName$suffix"
            "build/bin/$nativeHostTargetName/$name/$fileName"
        }

        val runTasks = listOf(
            "runDebugExecutable",
            "runReleaseExecutable",
            "runFooDebugExecutable",
            "runFooReleaseExecutable",
            "runBarReleaseExecutable",
            "runBazReleaseExecutable",
            "runTest2ReleaseExecutable"
        ).map { "$it$hostSuffix" }.toMutableList()

        val binariesTasks = arrayOf("${nativeHostTargetName}MainBinaries", "${nativeHostTargetName}TestBinaries")

        // Check that all link and run tasks are generated.
        build(*binariesTasks) {
            assertSuccessful()
            assertTasksExecuted(linkTasks.map { ":$it" })
            outputFiles.forEach {
                assertFileExists(it)
            }
            // Check that getters work fine.
            assertTrue(output.contains("Check link task: linkReleaseShared$hostSuffix"))
            assertTrue(output.contains("Check run task: runFooReleaseExecutable$hostSuffix"))
        }

        build("tasks") {
            assertSuccessful()
            runTasks.forEach {
                assertTrue(output.contains(it), "The 'tasks' output doesn't contain a task ${it}")
            }
        }

        // Clean the build to check that run tasks build corresponding binaries.
        build("clean") {
            assertSuccessful()
        }

        // Check that run tasks work fine and an entry point can be specified.
        build("runDebugExecutable$hostSuffix") {
            assertSuccessful()
            assertTrue(output.contains("<root>.main"))
        }

        build("runBazReleaseExecutable$hostSuffix") {
            assertSuccessful()
            assertTrue(output.contains("foo.main"))
        }

        build("runTest2ReleaseExecutable$hostSuffix") {
            assertSuccessful()
            assertTrue(output.contains("tests.foo"))
        }

        // Check that we still have a default test task and it can be executed properly.
        build("${nativeHostTargetName}Test") {
            assertSuccessful()
            assertTrue(output.contains("tests.foo"))
        }

        fun CompiledProject.checkFrameworkCompilationCommandLine(check: (String) -> Unit) {
            output.lineSequence().filter {
                it.contains("Run tool: konanc") && it.contains("-p framework")
            }.toList().also {
                assertTrue(it.isNotEmpty())
            }.forEach(check)
        }
        if (HostManager.hostIsMac) {

            // Check dependency exporting and bitcode embedding in frameworks.
            // For release builds.
            build("linkReleaseFrameworkIos") {
                assertSuccessful()
                assertFileExists("build/bin/ios/releaseFramework/native_binary.framework")
                fileInWorkingDir("build/bin/ios/releaseFramework/native_binary.framework/Headers/native_binary.h")
                    .readText().contains("+ (int32_t)exported")
                // Check that by default release frameworks have bitcode embedded.
                checkFrameworkCompilationCommandLine {
                    assertTrue(it.contains("-Xembed-bitcode"))
                    assertTrue(it.contains("-opt"))
                }
            }

            // For debug builds.
            build("linkDebugFrameworkIos") {
                assertSuccessful()
                assertFileExists("build/bin/ios/debugFramework/native_binary.framework")
                fileInWorkingDir("build/bin/ios/debugFramework/native_binary.framework/Headers/native_binary.h")
                    .readText().contains("+ (int32_t)exported")
                // Check that by default debug frameworks have bitcode marker embedded.
                checkFrameworkCompilationCommandLine {
                    assertTrue(it.contains("-Xembed-bitcode-marker"))
                    assertTrue(it.contains("-g"))
                }
            }

            // Check manual disabling bitcode embedding, custom command line args and building a static framework.
            build("linkCustomReleaseFrameworkIos") {
                assertSuccessful()
                checkFrameworkCompilationCommandLine {
                    assertTrue(it.contains("-linker-option -L."))
                    assertTrue(it.contains("-Xtime"))
                    assertTrue(it.contains("-Xstatic-framework"))
                    assertFalse(it.contains("-Xembed-bitcode-marker"))
                    assertFalse(it.contains("-Xembed-bitcode"))
                }
            }

            // Check that bitcode is disabled for iOS simulator.
            build("linkReleaseFrameworkIosSim", "linkDebugFrameworkIosSim") {
                assertSuccessful()
                assertFileExists("build/bin/iosSim/releaseFramework/native_binary.framework")
                assertFileExists("build/bin/iosSim/debugFramework/native_binary.framework")
                checkFrameworkCompilationCommandLine {
                    assertFalse(it.contains("-Xembed-bitcode"))
                    assertFalse(it.contains("-Xembed-bitcode-marker"))
                }
            }


            // Check that plugin doesn't allow exporting dependencies not added in the API configuration.
            val buildFile = listOf("build.gradle", "build.gradle.kts").map { projectDir.resolve(it) }.single { it.exists() }
            buildFile.modify {
                it.replace("api(project(\":exported\"))", "")
            }
            build("linkReleaseFrameworkIos") {
                assertFailed()
                val failureMsg = "Following dependencies exported in the releaseFramework binary " +
                        "are not specified as API-dependencies of a corresponding source set"
                assertTrue(output.contains(failureMsg))
            }
        }
    }

    @Test
    fun testSourceJars() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        build("publish") {
            assertSuccessful()

            val groupDir = projectDir.resolve("repo/com/example/")
            val targetArtifactIdAppendices = listOf("metadata", "jvm6", "nodejs", "wasm32", nativeHostTargetName)

            val sourceJarSourceRoots = targetArtifactIdAppendices.associate { artifact ->
                val sourcesJar = JarFile(groupDir.resolve("sample-lib-$artifact/1.0/sample-lib-$artifact-1.0-sources.jar"))
                val sourcesDirs = sourcesJar.entries().asSequence().map { it.name.substringBefore("/") }.toSet() - "META-INF"
                artifact to sourcesDirs
            }

            assertEquals(setOf("commonMain"), sourceJarSourceRoots["metadata"])
            assertEquals(setOf("commonMain", "jvm6Main"), sourceJarSourceRoots["jvm6"])
            assertEquals(setOf("commonMain", "nodeJsMain"), sourceJarSourceRoots["nodejs"])
            assertEquals(setOf("commonMain", "wasm32Main"), sourceJarSourceRoots["wasm32"])
            assertEquals(setOf("commonMain", "${nativeHostTargetName}Main"), sourceJarSourceRoots[nativeHostTargetName])
        }
    }

    @Test
    fun testConsumeMppLibraryFromNonKotlinProject() {
        val libRepo = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
            build("publish") { assertSuccessful() }
            projectDir.resolve("repo")
        }

        with(Project("sample-app-without-kotlin", gradleVersion, "new-mpp-lib-and-app")) {
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
    fun testNativeTests() = with(Project("new-mpp-native-tests", gradleVersion)) {
        val testTasks = listOf("macos64Test", "linux64Test", "mingw64Test")
        val hostTestTask = "${nativeHostTargetName}Test"
        build("tasks") {
            assertSuccessful()
            println(output)
            testTasks.forEach {
                // We need to create tasks for all hosts
                assertTrue(output.contains("$it - "), "There is no test task '$it' in the task list.")
            }
        }
        build("check") {
            assertSuccessful()
            assertTasksExecuted(":$hostTestTask")
            assertTestResults("testProject/new-mpp-native-tests/TEST-TestKt.xml", hostTestTask)
        }
    }

    @Test
    fun testCinterop() {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        libProject.build("publish") {
            assertSuccessful()
        }
        val repo = libProject.projectDir.resolve("repo").absolutePath.replace('\\', '/')

        with(Project("new-mpp-native-cinterop", gradleVersion)) {

            setupWorkingDir()
            listOf(gradleBuildScript(), gradleBuildScript("publishedLibrary")).forEach {
                it.appendText("""
                    repositories {
                        maven { url '$repo' }
                    }
                """.trimIndent())
            }

            val targetsToBuild = if (HostManager.hostIsMingw) {
                listOf(nativeHostTargetName, "mingw86")
            } else {
                listOf(nativeHostTargetName)
            }

            val libraryCinteropTasks = targetsToBuild.map { ":projectLibrary:cinteropStdio${it.capitalize()}" }
            val libraryCompileTasks = targetsToBuild.map { ":projectLibrary:compileKotlin${it.capitalize()}" }

            build(":projectLibrary:build") {
                assertSuccessful()
                assertTasksExecuted(libraryCinteropTasks)
                assertTrue(output.contains("Project test"), "No test output found")
                targetsToBuild.forEach {
                    assertFileExists("projectLibrary/build/classes/kotlin/$it/main/projectLibrary-cinterop-stdio.klib")
                }
            }

            build(":publishedLibrary:build", ":publishedLibrary:publish") {
                assertSuccessful()
                assertTasksExecuted(
                    targetsToBuild.map { ":publishedLibrary:cinteropStdio${it.capitalize()}" }
                )
                assertTrue(output.contains("Published test"), "No test output found")
                targetsToBuild.forEach {
                    assertFileExists("publishedLibrary/build/classes/kotlin/$it/main/publishedLibrary-cinterop-stdio.klib")
                    assertFileExists("publishedLibrary/build/classes/kotlin/$it/test/test-cinterop-stdio.klib")
                    assertFileExists("repo/org/example/publishedLibrary-$it/1.0/publishedLibrary-$it-1.0-cinterop-stdio.klib")
                }
            }

            build(":build") {
                assertSuccessful()
                assertTrue(output.contains("Dependent: Project print"), "No test output found")
                assertTrue(output.contains("Dependent: Published print"), "No test output found")
            }

            // Check that changing the compiler version in properties causes interop reprocessing and source recompilation.
            val hostLibraryTasks = listOf(
                ":projectLibrary:cinteropStdio${nativeHostTargetName.capitalize()}",
                ":projectLibrary:compileKotlin${nativeHostTargetName.capitalize()}"
            )
            build(":projectLibrary:build") {
                assertSuccessful()
                assertTasksUpToDate(hostLibraryTasks)
            }

            build(*hostLibraryTasks.toTypedArray(), "-Porg.jetbrains.kotlin.native.version=1.1.0") {
                assertSuccessful()
                assertTasksExecuted(hostLibraryTasks)
            }
        }
    }

    @Test
    fun testNativeCompilerDownloading() {
        // The plugin shouldn't download the K/N compiler if there is no corresponding targets in the project.
        with(Project("sample-old-style-app", gradleVersion, "new-mpp-lib-and-app")) {
            build("tasks") {
                assertSuccessful()
                assertFalse(output.contains("Kotlin/Native distribution: "))
            }
        }
        with(Project("new-mpp-native-libraries", gradleVersion)) {
            build("tasks") {
                assertSuccessful()
                assertTrue(output.contains("Kotlin/Native distribution: "))
            }
        }
    }

    @Test
    fun testPublishMultimoduleProjectWithNoMetadata() = doTestPublishMultimoduleProject(withMetadata = false)

    @Test
    fun testPublishMultimoduleProjectWithMetadata() = doTestPublishMultimoduleProject(withMetadata = true)

    private fun doTestPublishMultimoduleProject(withMetadata: Boolean) {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        libProject.setupWorkingDir()

        val externalLibProject = Project("sample-external-lib", gradleVersion, "new-mpp-lib-and-app").apply {
            if (withMetadata) {
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
        }

        val appProject = Project("sample-app", gradleVersion, "new-mpp-lib-and-app")

        with(libProject) {
            setupWorkingDir()
            appProject.setupWorkingDir()
            appProject.projectDir.copyRecursively(projectDir.resolve("sample-app"))

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
            if (withMetadata) {
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
            }

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

            if (withMetadata) {
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
            }

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
                if (withMetadata) {
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
            }

            // Check that a user can disable rewriting of MPP dependencies in the POMs:
            build("publish", "-Pkotlin.mpp.keepMppDependenciesIntactInPoms=true") {
                assertSuccessful()
                assertFileContains(
                    "repo/com/exampleapp/sample-app-nodejs/1.0/sample-app-nodejs-1.0.pom",
                    "<groupId>com.example</groupId>",
                    if (withMetadata)
                        "<artifactId>sample-lib-multiplatform</artifactId>"
                    else
                        "<artifactId>sample-lib</artifactId>"
                    ,
                    "<version>1.0</version>"
                )
                assertFileContains(
                    "repo/com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom",
                    "<groupId>com.example</groupId>",
                    if (withMetadata)
                        "<artifactId>sample-lib-multiplatform</artifactId>"
                    else
                        "<artifactId>sample-lib</artifactId>"
                    ,
                    "<version>1.0</version>"
                )
                if (withMetadata) {
                    assertFileContains(
                        "repo/com/exampleapp/sample-app-jvm8/1.0/sample-app-jvm8-1.0.pom",
                        "<groupId>com.external.dependency</groupId>",
                        "<artifactId>external</artifactId>",
                        "<version>1.2.3</version>"
                    )
                }
            }
        }
    }

    @Test
    fun testMppBuildWithCompilerPlugins() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

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
            assertTasksExecuted(*listOf("Jvm6", "NodeJs", nativeHostTargetName.capitalize()).map { ":compileKotlin$it" }.toTypedArray())
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
        build("runRhino") {
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
    fun testStaleOutputCleanup() = with(Project("new-mpp-lib-with-tests", gradleVersion)) {
        setupWorkingDir()
        // Check that output directories of Kotlin compilations are registered for Gradle stale outputs cleanup.
        // One way to check that is to run a Gradle build with no Gradle history (no .gradle directory) and see that the compilation
        // output directories are cleaned up, even those outside the project's buildDir

        gradleBuildScript().appendText(
            "\n" + """
            kotlin.targets.js.compilations.main.output.classesDirs.from("foo") // should affect Gradle's behavior wrt stale output cleanup
            task('foo') {
                outputs.dir("foo")
                doFirst {
                    println 'hello'
                    file("foo/2.txt").text = System.currentTimeMillis()
                }
            }
            """.trimIndent()
        )

        val staleFilePath = "foo/1.txt"
        projectDir.resolve(staleFilePath).run { parentFile.mkdirs(); createNewFile() }

        build("foo") {
            assertSuccessful()
            assertNoSuchFile(staleFilePath)
            assertFileExists("foo/2.txt")
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
                "jvm6", "nodeJs", "wasm32", "mingw64", "mingw86", "linux64", "macos64"
            ).flatMapTo(mutableSetOf()) { target ->
                listOf("main", "test").map { compilation ->
                    Triple(target, compilation, "$target${compilation.capitalize()}")
                }
            } + Triple("metadata", "main", "commonMain")

            assertEquals(expectedDefaultSourceSets, actualDefaultSourceSets)
        }
    }

    @Test
    fun testDependenciesDsl() = with(transformProjectWithPluginsDsl("newMppDependenciesDsl", GradleVersionRequired.AtLeast("4.10"))) {
        val originalBuildscriptContent = gradleBuildScript("app").readText()

        fun testDependencies() = testResolveAllConfigurations("app") {
            assertContains(">> :app:testNonTransitiveStringNotationApiDependenciesMetadata --> junit-4.12.jar")
            assertEquals(1, (Regex.escape(">> :app:testNonTransitiveStringNotationApiDependenciesMetadata") + " .*").toRegex().findAll(output).count())

            assertContains(">> :app:testNonTransitiveDependencyNotationApiDependenciesMetadata --> kotlin-reflect-${defaultBuildOptions().kotlinVersion}.jar")
            assertEquals(1, (Regex.escape(">> :app:testNonTransitiveStringNotationApiDependenciesMetadata") + " .*").toRegex().findAll(output).count())

            assertContains(">> :app:testExplicitKotlinVersionApiDependenciesMetadata --> kotlin-reflect-1.3.0.jar")
            assertContains(">> :app:testExplicitKotlinVersionImplementationDependenciesMetadata --> kotlin-reflect-1.2.71.jar")
            assertContains(">> :app:testExplicitKotlinVersionCompileOnlyDependenciesMetadata --> kotlin-reflect-1.2.70.jar")
            assertContains(">> :app:testExplicitKotlinVersionRuntimeOnlyDependenciesMetadata --> kotlin-reflect-1.2.60.jar")

            assertContains(">> :app:testProjectWithConfigurationApiDependenciesMetadata --> output.txt")
        }

        testDependencies()

        // Then run with Gradle Kotlin DSL; the build script needs only one correction to be a valid GK DSL script:
        gradleBuildScript("app").run {
            modify { originalBuildscriptContent.replace(": ", " = ") }
            renameTo(projectDir.resolve("app/build.gradle.kts"))
        }

        testDependencies()
    }

    @Test
    fun testMultipleTargetsSamePlatform() = with(Project("newMppMultipleTargetsSamePlatform", gradleVersion)) {
        testResolveAllConfigurations("app") {
            assertContains(">> :app:junitCompileClasspath --> lib-junit.jar")
            assertContains(">> :app:junitCompileClasspath --> junit-4.12.jar")

            assertContains(">> :app:mixedJunitCompileClasspath --> lib-junit.jar")
            assertContains(">> :app:mixedJunitCompileClasspath --> junit-4.12.jar")

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
    fun testKt29725() {
        with(Project("new-mpp-native-libraries", GradleVersionRequired.Exact("5.2"))) {
            // Assert that a project with a native target can be configured with Gradle 5.2
            build("tasks") {
                assertSuccessful()
            }
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
    fun testPomRewritingInSinglePlatformProject() = with(Project("kt-27059-pom-rewriting", GradleVersionRequired.AtLeast("4.10.2"))) {
        setupWorkingDir()
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        val groupDir = "build/repo/com/example/"

        build(":mpp-lib:publish") {
            assertSuccessful()
            assertFileExists(groupDir + "mpp-lib")
            assertFileExists(groupDir + "mpp-lib-myjvm")
        }

        fun doTestPomRewriting(mppProjectDependency: Boolean, legacyPublishing: Boolean, keepPomIntact: Boolean? = null) {

            val params = mutableListOf("clean", ":jvm-app:publish", ":js-app:publish").apply {
                if (mppProjectDependency)
                    add("-PmppProjectDependency=true")
                if (legacyPublishing)
                    add("-PlegacyPublishing=true")
                if (keepPomIntact == true)
                    add("-Pkotlin.mpp.keepMppDependenciesIntactInPoms=true")
            }.toTypedArray()

            build(*params) {
                assertSuccessful()
                if (legacyPublishing) {
                    assertTasksExecuted(":jvm-app:uploadArchives")
                    assertTasksExecuted(":js-app:uploadArchives")
                } else {
                    assertTasksExecuted(":jvm-app:publishMainPublicationToMavenRepository")
                    assertTasksExecuted(":js-app:publishMainPublicationToMavenRepository")
                }

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

        doTestPomRewriting(mppProjectDependency = false, legacyPublishing = false)
        doTestPomRewriting(mppProjectDependency = false, legacyPublishing = true)
        doTestPomRewriting(mppProjectDependency = true, legacyPublishing = false)

        // This case doesn't work and never did; TODO investigate KT-29975
        // doTestPomRewriting(mppProjectDependency = true, legacyPublishing = true)

        // Also check that the flag for keeping POMs intact works:
        doTestPomRewriting(mppProjectDependency = false, legacyPublishing = false, keepPomIntact = true)
        doTestPomRewriting(mppProjectDependency = false, legacyPublishing = true, keepPomIntact = true)
    }
}