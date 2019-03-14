package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.getFilesByNames
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// TODO If we there is a way to fetch the latest Android plugin version, test against the latest version
class KotlinAndroid32GradleIT : KotlinAndroid3GradleIT(androidGradlePluginVersion = AGPVersion.v3_2_0) {
    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("4.6")

    @Test
    fun testAndroidWithNewMppApp() = with(Project("new-mpp-android", GradleVersionRequired.AtLeast("4.7"))) {
        build("assemble", "compileDebugUnitTestJavaWithJavac", "printCompilerPluginOptions") {
            assertSuccessful()

            assertContains("KT-29964 OK") // Output from lib/build.gradle

            assertTasksExecuted(
                ":lib:compileDebugKotlinAndroidLib",
                ":lib:compileReleaseKotlinAndroidLib",
                ":lib:compileKotlinJvmLib",
                ":lib:compileKotlinJsLib",
                ":lib:compileKotlinMetadata",
                ":app:compileDebugKotlinAndroidApp",
                ":app:compileReleaseKotlinAndroidApp",
                ":app:compileKotlinJvmApp",
                ":app:compileKotlinJsApp",
                ":app:compileKotlinMetadata",
                ":lib:compileDebugUnitTestJavaWithJavac",
                ":app:compileDebugUnitTestJavaWithJavac"
            )

            listOf("debug", "release").forEach { variant ->
                assertFileExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/ExpectedLibClass.class")
                assertFileExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/CommonLibClass.class")
                assertFileExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/AndroidLibClass.class")

                assertFileExists("app/build/tmp/kotlin-classes/$variant/com/example/app/AKt.class")
                assertFileExists("app/build/tmp/kotlin-classes/$variant/com/example/app/KtUsageKt.class")
            }

            // Check that Android extensions arguments are available only in the Android source sets:
            val compilerPluginArgsRegex = "(\\w+)${Regex.escape("=args=>")}(.*)".toRegex()
            val compilerPluginOptionsBySourceSet =
                compilerPluginArgsRegex.findAll(output).associate { it.groupValues[1] to it.groupValues[2] }

            compilerPluginOptionsBySourceSet.entries.forEach { (sourceSetName, argsString) ->
                val shouldHaveAndroidExtensionArgs = sourceSetName.startsWith("androidApp")
                if (shouldHaveAndroidExtensionArgs)
                    assertTrue("$sourceSetName is an Android source set and should have Android Extensions in the args") {
                        "plugin:org.jetbrains.kotlin.android" in argsString
                    }
                else
                    assertEquals(
                        "[]",
                        argsString,
                        "$sourceSetName is not an Android source set and should not have Android Extensions in the args"
                    )
            }
        }

        // By default, no Android variant should be published in 1.3.20:
        val groupDir = "lib/build/repo/com/example/"
        build("publish") {
            assertSuccessful()
            assertFileExists(groupDir + "lib-jvmlib")
            assertFileExists(groupDir + "lib-jslib")
            assertNoSuchFile(groupDir + "lib-androidlib")
            assertNoSuchFile(groupDir + "lib-androidlib-debug")
            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Choose a single variant to publish, check that it's there:
        gradleBuildScript("lib").appendText("\nkotlin.android('androidLib').publishLibraryVariants = ['release']")
        build("publish") {
            assertSuccessful()
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0.aar")
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0-sources.jar")
            assertNoSuchFile(groupDir + "lib-androidlib-debug")
            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Enable publishing for all Android variants:
        gradleBuildScript("lib").appendText("\nkotlin.android('androidLib') { publishAllLibraryVariants() }")
        build("publish") {
            assertSuccessful()
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0.aar")
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0-sources.jar")
            assertFileExists(groupDir + "lib-androidlib-debug/1.0/lib-androidlib-debug-1.0.aar")
            assertFileExists(groupDir + "lib-androidlib-debug/1.0/lib-androidlib-debug-1.0-sources.jar")
            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Then group the variants by flavor and check that only one publication is created:
        gradleBuildScript("lib").appendText("\nkotlin.android('androidLib').publishLibraryVariantsGroupedByFlavor = true")
        build("publish") {
            assertSuccessful()
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0.aar")
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0-sources.jar")
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0-debug.aar")
            assertFileExists(groupDir + "lib-androidlib/1.0/lib-androidlib-1.0-debug-sources.jar")
            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Add one flavor dimension with two flavors, check that the flavors produce grouped publications:
        gradleBuildScript("lib").appendText(
            "\nandroid { flavorDimensions('foo'); productFlavors { fooBar { dimension 'foo' }; fooBaz { dimension 'foo' } } }"
        )
        build("publish") {
            assertSuccessful()
            listOf("foobar", "foobaz").forEach { flavor ->
                assertFileExists(groupDir + "lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0.aar")
                assertFileExists(groupDir + "lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0-sources.jar")
                assertFileExists(groupDir + "lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0-debug.aar")
                assertFileExists(groupDir + "lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0-debug-sources.jar")
            }
            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Disable the grouping and check that all the variants are published under separate artifactIds:
        gradleBuildScript("lib").appendText(
            "\nkotlin.android('androidLib') { publishLibraryVariantsGroupedByFlavor = false }"
        )
        build("publish") {
            assertSuccessful()
            listOf("foobar", "foobaz").forEach { flavor ->
                listOf("-debug", "").forEach { buildType ->
                    assertFileExists(groupDir + "lib-androidlib-$flavor$buildType/1.0/lib-androidlib-$flavor$buildType-1.0.aar")
                    assertFileExists(groupDir + "lib-androidlib-$flavor$buildType/1.0/lib-androidlib-$flavor$buildType-1.0-sources.jar")
                }
            }
            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Convert the 'app' project to a library, publish the two without metadata,
        // check that the dependencies in the POMs are correctly rewritten:
        val appGroupDir = "app/build/repo/com/example/"

        gradleSettingsScript().modify { it.replace("enableFeaturePreview", "//") }
        gradleBuildScript("app").modify {
            it.replace("com.android.application", "com.android.library")
                .replace("applicationId", "//") + "\n" + """
                    apply plugin: 'maven-publish'
                    publishing { repositories { maven { url = uri("${'$'}buildDir/repo") } } }
                    kotlin.android('androidApp') { publishAllLibraryVariants() }
                    android { flavorDimensions('foo'); productFlavors { fooBar { dimension 'foo' }; fooBaz { dimension 'foo' } } }
                """.trimIndent()
        }
        build("publish") {
            assertSuccessful()
            listOf("foobar", "foobaz").forEach { flavor ->
                listOf("-debug", "").forEach { buildType ->
                    assertFileExists(appGroupDir + "app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0.aar")
                    assertFileExists(appGroupDir + "app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0-sources.jar")
                    val pomText = projectDir.resolve(
                        appGroupDir + "app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0.pom"
                    ).readText().replace("\\s+".toRegex(), "")
                    assertTrue {
                        "<artifactId>lib-androidlib-$flavor$buildType</artifactId><version>1.0</version><scope>runtime</scope>" in pomText
                    }
                }
            }
            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Also check that api and runtimeOnly MPP dependencies get correctly published with the appropriate scope, KT-29476:
        gradleBuildScript("app").modify {
            it.replace("implementation project(':lib')", "api project(':lib')") + "\n" + """
                kotlin.sourceSets.commonMain.dependencies {
                    runtimeOnly(kotlin('reflect'))
                }
            """.trimIndent()
        }
        build("publish") {
            assertSuccessful()
            listOf("foobar", "foobaz").forEach { flavor ->
                listOf("-debug", "").forEach { buildType ->
                    val pomText = projectDir.resolve(
                        appGroupDir + "app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0.pom"
                    ).readText().replace("\\s+".toRegex(), "")
                    assertTrue {
                        "<artifactId>lib-androidlib-$flavor$buildType</artifactId><version>1.0</version><scope>compile</scope>" in pomText
                    }
                    assertTrue {
                        val kotlinVersion = defaultBuildOptions().kotlinVersion
                        "<artifactId>kotlin-reflect</artifactId><version>$kotlinVersion</version><scope>runtime</scope>" in pomText
                    }
                }
            }
        }
    }

    @Test
    fun testCustomAttributesInAndroidTargets() = with(Project("new-mpp-android", GradleVersionRequired.AtLeast("4.7"))) {
        // Test the fix for KT-27714

        setupWorkingDir()

        // Enable publishing for all Android variants:
        gradleBuildScript("lib").appendText("\nkotlin.android('androidLib') { publishAllLibraryVariants() }")

        val groupDir = "lib/build/repo/com/example/"

        build("publish") {
            assertSuccessful()

            // Also check that custom user-specified attributes are written in all Android modules metadata:
            assertFileContains(
                groupDir + "lib-androidlib/1.0/lib-androidlib-1.0.module",
                "\"com.example.target\": \"androidLib\"",
                "\"com.example.compilation\": \"release\""
            )
            assertFileContains(
                groupDir + "lib-androidlib-debug/1.0/lib-androidlib-debug-1.0.module",
                "\"com.example.target\": \"androidLib\"",
                "\"com.example.compilation\": \"debug\""
            )

            projectDir.resolve(groupDir).deleteRecursively()
        }

        // Check that the consumer side uses custom attributes specified in the target and compilations:
        run {
            val appBuildScriptBackup = gradleBuildScript("app").readText()

            gradleBuildScript("app").appendText(
                "\n" + """
                    kotlin.targets.androidApp.attributes.attribute(
                        Attribute.of("com.example.target", String),
                        "notAndroidLib"
                    )
                """.trimIndent()
            )

            build(":app:compileDebugKotlinAndroidApp") {
                assertFailed() // dependency resolution should fail
                assertContains("Required com.example.target 'notAndroidLib'")
            }

            gradleBuildScript("app").writeText(
                appBuildScriptBackup + "\n" + """
                    kotlin.targets.androidApp.compilations.all {
                        attributes.attribute(
                            Attribute.of("com.example.compilation", String),
                            "notDebug"
                        )
                    }
                """.trimIndent()
            )

            build(":app:compileDebugKotlinAndroidApp") {
                assertFailed()
                assertContains("Required com.example.compilation 'notDebug'")
            }
        }
    }

    @Test
    fun testKaptUsingApOptionProvidersAsNestedInputOutput() = with(Project("AndroidProject")) {
        setupWorkingDir()

        gradleBuildScript(subproject = "Android").appendText(
            """

            apply plugin: 'kotlin-kapt'

            class MyNested implements org.gradle.process.CommandLineArgumentProvider {

                @InputFile
                File inputFile = null

                @Override
                @Internal
                Iterable<String> asArguments() {
                    // Read the arguments from a file, because changing them in a build script is treated as an
                    // implementation change by Gradle:
                    return [new File('args.txt').text]
                }
            }

            def nested = new MyNested()
            nested.inputFile = file("${'$'}projectDir/in.txt")

            android.applicationVariants.all {
                it.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(nested)
            }
            """.trimIndent()
        )

        File(projectDir, "Android/in.txt").appendText("1234")
        File(projectDir, "args.txt").appendText("1234")

        val kaptTasks = listOf(":Android:kaptFlavor1DebugKotlin")
        val javacTasks = listOf(":Android:compileFlavor1DebugJavaWithJavac")

        val buildTasks = (kaptTasks + javacTasks).toTypedArray()

        build(*buildTasks) {
            assertSuccessful()
            assertTasksExecuted(kaptTasks + javacTasks)
        }

        File(projectDir, "Android/in.txt").appendText("5678")

        build(*buildTasks) {
            assertSuccessful()
            assertTasksExecuted(kaptTasks)
            assertTasksUpToDate(javacTasks)
        }

        // Changing only the annotation provider arguments should not trigger the tasks to run, as the arguments may be outputs,
        // internals or neither:
        File(projectDir, "args.txt").appendText("5678")

        build(*buildTasks) {
            assertSuccessful()
            assertTasksUpToDate(javacTasks + kaptTasks)
        }
    }
}

class KotlinAndroid30GradleIT : KotlinAndroid3GradleIT(androidGradlePluginVersion = AGPVersion.v3_0_0) {
    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.Until("4.10.2")
}

abstract class KotlinAndroid3GradleIT(androidGradlePluginVersion: AGPVersion) : AbstractKotlinAndroidGradleTests(androidGradlePluginVersion) {
    @Test
    fun testApplyWithFeaturePlugin() {
        val project = Project("AndroidProject")

        project.setupWorkingDir()
        File(project.projectDir, "Lib/build.gradle").modify { text ->
            // Change the applied plugin to com.android.feature
            text.replace("com.android.library", "com.android.feature")
                .replace("compileSdkVersion 22", "compileSdkVersion 26")
                .apply { assert(!equals(text)) }
                .plus("\nandroid { baseFeature true }")
        }

        // Check that Kotlin tasks were created for both lib and feature variants:
        val kotlinTaskNames =
            listOf("Debug").flatMap { buildType ->
                listOf("Flavor1", "Flavor2").flatMap { flavor ->
                    listOf("", "Feature").map { isFeature -> ":Lib:compile$flavor$buildType${isFeature}Kotlin" }
                }
            }

        project.build(":Lib:assembleDebug") {
            assertSuccessful()
            assertTasksExecuted(*kotlinTaskNames.toTypedArray())
        }
    }
}

abstract class AbstractKotlinAndroidGradleTests(val androidGradlePluginVersion: AGPVersion) : BaseGradleIT() {

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion
        )

    @Test
    fun testSimpleCompile() {
        val project = Project("AndroidProject")

        val modules = listOf("Android", "Lib")
        val flavors = listOf("Flavor1", "Flavor2")
        val buildTypes = listOf("Debug")

        val tasks = arrayListOf<String>()
        for (module in modules) {
            for (flavor in flavors) {
                for (buildType in buildTypes) {
                    tasks.add(":$module:compile$flavor${buildType}Kotlin")
                }
            }
        }

        project.build("assembleDebug", "test") {
            assertSuccessful()
            // Before 3.0 AGP test only modules are compiled only against one flavor and one build type,
            // and contain only the compileDebugKotlin task.
            // After 3.0 AGP test only modules contain a compile<Variant>Kotlin task for each variant.
            tasks.addAll(findTasksByPattern(":Test:compile[\\w\\d]+Kotlin"))
            assertTasksExecuted(tasks)
            assertContains("InternalDummyTest PASSED")
            checkKotlinGradleBuildServices()
        }

        // Run the build second time, assert everything is up-to-date
        project.build("assembleDebug") {
            assertSuccessful()
            assertTasksUpToDate(tasks)
        }

        // Run the build third time, re-run tasks

        project.build("assembleDebug", "--rerun-tasks") {
            assertSuccessful()
            assertTasksExecuted(tasks)
            checkKotlinGradleBuildServices()
        }
    }

    @Test
    fun testAssembleAndroidTestFirst() {
        val project = Project("AndroidProject", minLogLevel = LogLevel.INFO)

        // Execute 'assembleAndroidTest' first, without 'build' side effects
        project.build("assembleAndroidTest") {
            assertSuccessful()
        }
    }

    @Test
    fun testIncrementalCompile() {
        val project = Project("AndroidIncrementalSingleModuleProject")
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val getSomethingKt = project.projectDir.walk().filter { it.isFile && it.name.endsWith("getSomething.kt") }.first()
        getSomethingKt.writeText(
            """
package com.example

fun getSomething() = 10
"""
        )

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedKotlinFiles = listOf(
                "app/src/main/kotlin/com/example/KotlinActivity1.kt",
                "app/src/main/kotlin/com/example/getSomething.kt"
            )
            assertCompiledKotlinSources(affectedKotlinFiles)
            assertCompiledJavaSources(listOf("app/src/main/java/com/example/JavaActivity.java"), weakTesting = true)
        }
    }

    @Test
    fun testMultiModuleIC() {
        val project = Project("AndroidIncrementalMultiModule")
        val options = defaultBuildOptions().copy(incremental = true, kotlinDaemonDebugPort = null)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val libAndroidUtilKt = project.projectDir.getFileByName("libAndroidUtil.kt")
        libAndroidUtilKt.modify { it.replace("fun libAndroidUtil(): String", "fun libAndroidUtil(): CharSequence") }
        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("libAndroidUtil.kt", "useLibAndroidUtil.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources))
        }

        val libAndroidClassesOnlyUtilKt = project.projectDir.getFileByName("LibAndroidClassesOnlyUtil.kt")
        libAndroidClassesOnlyUtilKt.modify { it.replace("fun libAndroidClassesOnlyUtil(): String", "fun libAndroidClassesOnlyUtil(): CharSequence") }
        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("LibAndroidClassesOnlyUtil.kt", "useLibAndroidClassesOnlyUtil.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources))
        }

        val libJvmUtilKt = project.projectDir.getFileByName("LibJvmUtil.kt")
        libJvmUtilKt.modify { it.replace("fun libJvmUtil(): String", "fun libJvmUtil(): CharSequence") }
        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames("LibJvmUtil.kt", "useLibJvmUtil.kt")
            assertCompiledKotlinSources(project.relativize(affectedSources))
        }
    }

    @Test
    fun testIncrementalBuildWithNoChanges() {
        val project = Project("AndroidIncrementalSingleModuleProject")
        val tasksToExecute = listOf(
            ":app:compileDebugKotlin",
            ":app:compileDebugJavaWithJavac"
        )

        project.build("assembleDebug") {
            assertSuccessful()
            assertTasksExecuted(tasksToExecute)
        }

        project.build("assembleDebug") {
            assertSuccessful()
            assertTasksUpToDate(tasksToExecute)
        }
    }

    @Test
    fun testAndroidDaggerIC() {
        val project = Project("AndroidDaggerProject")
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }

        val androidModuleKt = project.projectDir.getFileByName("AndroidModule.kt")
        androidModuleKt.modify {
            it.replace(
                "fun provideApplicationContext(): Context {",
                "fun provideApplicationContext(): Context? {"
            )
        }

        project.build(":app:assembleDebug", options = options) {
            assertSuccessful()
            assertCompiledKotlinSources(project.relativize(androidModuleKt))
            assertTasksExecuted(
                ":app:kaptGenerateStubsDebugKotlin",
                ":app:kaptDebugKotlin",
                ":app:compileDebugKotlin",
                ":app:compileDebugJavaWithJavac"
            )
        }
    }

    @Test
    fun testAndroidIcepickProject() {
        val project = Project("AndroidIcepickProject")
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testAndroidExtensions() {
        val project = Project("AndroidExtensionsProject")
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testAndroidExtensionsIncremental() {
        val project = Project("AndroidExtensionsProject")
        val options = defaultBuildOptions().copy(incremental = true)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            val affectedSources = project.projectDir.getFilesByNames(
                "MyActivity.kt", "noLayoutUsages.kt"
            )
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }

        val activityLayout = File(project.projectDir, "app/src/main/res/layout/activity_main.xml")
        activityLayout.modify { it.replace("textView", "newTextView") }

        project.build("assembleDebug", options = options) {
            assertFailed()
            val affectedSources = project.projectDir.getFilesByNames("MyActivity.kt")
            val relativePaths = project.relativize(affectedSources)
            assertCompiledKotlinSources(relativePaths)
        }
    }


    @Test
    fun testAndroidExtensionsManyVariants() {
        val project = Project("AndroidExtensionsManyVariants")
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assemble", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testAndroidExtensionsSpecificFeatures() {
        val project = Project("AndroidExtensionsSpecificFeatures")
        val options = defaultBuildOptions().copy(incremental = false)

        if (this is KotlinAndroid30GradleIT) {
            project.setupWorkingDir()
            project.gradleBuildScript("app").modify {
                """
                def projectEvaluated = false

                configurations.all { configuration ->
                    incoming.beforeResolve {
                        if (!projectEvaluated) {
                            throw new RuntimeException("${'$'}configuration resolved during project configuration phase.")
                        }
                    }
                }

                $it

                afterEvaluate {
                    projectEvaluated = true
                }
                """.trimIndent()
            }
        }

        project.build("assemble", options = options) {
            assertFailed()
            assertContains("Unresolved reference: textView")
        }

        File(project.projectDir, "app/build.gradle").modify { it.replace("[\"parcelize\"]", "[\"views\"]") }

        project.build("assemble", options = options) {
            assertFailed()
            assertContains("Class 'User' is not abstract and does not implement abstract member public abstract fun writeToParcel")
        }

        File(project.projectDir, "app/build.gradle").modify { it.replace("[\"views\"]", "[\"parcelize\", \"views\"]") }

        project.build("assemble", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testMultiplatformAndroidCompile() = with(Project("multiplatformAndroidProject")) {
        setupWorkingDir()

        // Check that the common module is not added to the deprecated configuration 'compile' (KT-23719):
        gradleBuildScript("libAndroid").appendText(
            """${'\n'}
                configurations.compile.dependencies.all { aDependencyExists ->
                    throw GradleException("Check failed")
                }
                """.trimIndent()
        )

        build("build") {
            assertSuccessful()
            assertTasksExecuted(
                ":lib:compileKotlinCommon",
                ":lib:compileTestKotlinCommon",
                ":libJvm:compileKotlin",
                ":libJvm:compileTestKotlin",
                ":libAndroid:compileDebugKotlin",
                ":libAndroid:compileReleaseKotlin",
                ":libAndroid:compileDebugUnitTestKotlin",
                ":libAndroid:compileReleaseUnitTestKotlin"
            )

            assertFileExists("lib/build/classes/kotlin/main/foo/PlatformClass.kotlin_metadata")
            assertFileExists("lib/build/classes/kotlin/test/foo/PlatformTest.kotlin_metadata")
            assertFileExists("libJvm/build/classes/kotlin/main/foo/PlatformClass.class")
            assertFileExists("libJvm/build/classes/kotlin/test/foo/PlatformTest.class")

            assertFileExists("libAndroid/build/tmp/kotlin-classes/debug/foo/PlatformClass.class")
            assertFileExists("libAndroid/build/tmp/kotlin-classes/release/foo/PlatformClass.class")
            assertFileExists("libAndroid/build/tmp/kotlin-classes/debugUnitTest/foo/PlatformTest.class")
            assertFileExists("libAndroid/build/tmp/kotlin-classes/debugUnitTest/foo/PlatformTest.class")
        }
    }

    @Test
    fun testDetectAndroidJava8() = with(Project("AndroidProject")) {
        setupWorkingDir()

        val kotlinJvmTarget18Regex = Regex("Kotlin compiler args: .* -jvm-target 1.8")

        build(":Lib:assemble") {
            assertSuccessful()
            assertNotContains(kotlinJvmTarget18Regex)
        }

        gradleBuildScript("Lib").appendText(
            "\n" + """
            android.compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
            """.trimIndent()
        )

        build(":Lib:assemble") {
            assertSuccessful()
            assertContainsRegex(kotlinJvmTarget18Regex)
        }
    }
}