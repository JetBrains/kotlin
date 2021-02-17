package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assume
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class KotlinAndroid36GradleIT : KotlinAndroid34GradleIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_6_0

    @Test
    fun testAndroidMppSourceSets(): Unit = with(
        Project("new-mpp-android-source-sets")
    ) {
        // AbstractReportTask#generate() task action was removed in Gradle 6.8+,
        // that SourceSetTask is using: https://github.com/gradle/gradle/commit/4dac91ab87ea33ee8689d2a62b691b119198e7c7
        // leading to the issue that ":sourceSets" task is always in 'UP-TO-DATE' state.
        // Skipping this check until test will start using AGP 7.0-alpha03+
        if (GradleVersion.version(chooseWrapperVersionOrFinishTest()) < GradleVersion.version("6.8")) {
            build("sourceSets") {
                assertSuccessful()

                assertContains("Android resources: [lib/src/main/res, lib/src/androidMain/res]")
                assertContains("Assets: [lib/src/main/assets, lib/src/androidMain/assets]")
                assertContains("AIDL sources: [lib/src/main/aidl, lib/src/androidMain/aidl]")
                assertContains("RenderScript sources: [lib/src/main/rs, lib/src/androidMain/rs]")
                assertContains("JNI sources: [lib/src/main/jni, lib/src/androidMain/jni]")
                assertContains("JNI libraries: [lib/src/main/jniLibs, lib/src/androidMain/jniLibs]")
                assertContains("Java-style resources: [lib/src/main/resources, lib/src/androidMain/resources]")

                assertContains("Android resources: [lib/src/androidTestDebug/res, lib/src/androidAndroidTestDebug/res]")
                assertContains("Assets: [lib/src/androidTestDebug/assets, lib/src/androidAndroidTestDebug/assets]")
                assertContains("AIDL sources: [lib/src/androidTestDebug/aidl, lib/src/androidAndroidTestDebug/aidl]")
                assertContains("RenderScript sources: [lib/src/androidTestDebug/rs, lib/src/androidAndroidTestDebug/rs]")
                assertContains("JNI sources: [lib/src/androidTestDebug/jni, lib/src/androidAndroidTestDebug/jni]")
                assertContains("JNI libraries: [lib/src/androidTestDebug/jniLibs, lib/src/androidAndroidTestDebug/jniLibs]")
                assertContains("Java-style resources: [lib/src/androidTestDebug/resources, lib/src/androidAndroidTestDebug/resources]")

                assertContains("Java-style resources: [lib/betaSrc/paidBeta/resources, lib/src/androidPaidBeta/resources]")
                assertContains("Java-style resources: [lib/betaSrc/paidBetaDebug/resources, lib/src/androidPaidBetaDebug/resources]")
                assertContains("Java-style resources: [lib/betaSrc/paidBetaRelease/resources, lib/src/androidPaidBetaRelease/resources]")

                assertContains("Java-style resources: [lib/betaSrc/freeBeta/resources, lib/src/androidFreeBeta/resources]")
                assertContains("Java-style resources: [lib/betaSrc/freeBetaDebug/resources, lib/src/androidFreeBetaDebug/resources]")
                assertContains("Java-style resources: [lib/betaSrc/freeBetaRelease/resources, lib/src/androidFreeBetaRelease/resources]")
            }
        }

        build("testFreeBetaDebug") {
            assertFailed()
            assertContains("CommonTest > fail FAILED")
            assertContains("TestKotlin > fail FAILED")
            assertContains("AndroidTestKotlin > fail FAILED")
            assertContains("TestJava > fail FAILED")
        }

        build("assemble") {
            assertSuccessful()
        }

        // Test for KT-35016: MPP should recognize android instrumented tests correctly
        // TODO: https://issuetracker.google.com/issues/173770818 enable after fix in AGP
        /*
        build("connectedAndroidTest") {
            assertFailed()
            assertContains("No connected devices!")
        }
         */
    }

    @Test
    fun testAndroidWithNewMppApp() = with(Project("new-mpp-android")) {
        build("assemble", "compileDebugUnitTestJavaWithJavac", "printCompilerPluginOptions") {
            assertSuccessful()

            // KT-30784
            assertNotContains("API 'variant.getPackageLibrary()' is obsolete and has been replaced")

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
                val shouldHaveAndroidExtensionArgs =
                    sourceSetName.startsWith("androidApp") && (
                            androidGradlePluginVersion < AGPVersion.v7_0_0 || !sourceSetName.contains("AndroidTestRelease")
                            )
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
    fun testAndroidMppProductionDependenciesInTests() = with(Project("new-mpp-android")) {
        // Test the fix for KT-29343
        setupWorkingDir()

        gradleBuildScript("lib").appendText(
            "\n" + """
            kotlin.sourceSets {
                commonMain {
                    dependencies {
                        implementation kotlin("stdlib-common")
                    }
                }
                androidLibDebug {
                    dependencies {
                        implementation kotlin("reflect")
                    }
                }
                androidLibRelease {
                    dependencies {
                        implementation kotlin("test-junit")
                    }
                }
            }
            """.trimIndent()
        )

        val kotlinVersion = defaultBuildOptions().kotlinVersion
        testResolveAllConfigurations("lib") {
            assertSuccessful()

            // commonMain:
            assertContains(">> :lib:debugCompileClasspath --> kotlin-stdlib-common-$kotlinVersion.jar")
            assertContains(">> :lib:releaseCompileClasspath --> kotlin-stdlib-common-$kotlinVersion.jar")
            assertContains(">> :lib:debugAndroidTestCompileClasspath --> kotlin-stdlib-common-$kotlinVersion.jar")
            assertContains(">> :lib:debugUnitTestCompileClasspath --> kotlin-stdlib-common-$kotlinVersion.jar")
            assertContains(">> :lib:releaseUnitTestCompileClasspath --> kotlin-stdlib-common-$kotlinVersion.jar")

            // androidLibDebug:
            assertContains(">> :lib:debugCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
            assertNotContains(">> :lib:releaseCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
            assertContains(">> :lib:debugAndroidTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
            assertContains(">> :lib:debugUnitTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
            assertNotContains(">> :lib:releaseUnitTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")

            // androidLibRelease:
            assertNotContains(">> :lib:debugCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
            assertContains(">> :lib:releaseCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
            assertNotContains(">> :lib:debugAndroidTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
            assertNotContains(">> :lib:debugUnitTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
            assertContains(">> :lib:releaseUnitTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
        }
    }

    @Test
    fun testCustomAttributesInAndroidTargets() = with(Project("new-mpp-android")) {
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

            gradleBuildScript("lib").appendText(
                "\n" + """
                    kotlin.targets.all { 
                        attributes.attribute(
                            Attribute.of("com.example.target", String),
                            targetName
                        )
                    }
                """.trimIndent()
            )
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
                assertTrue(
                    "Required com.example.target 'notAndroidLib'" in output ||
                            "attribute 'com.example.target' with value 'notAndroidLib'" in output
                )
            }

            gradleBuildScript("lib").writeText(
                appBuildScriptBackup + "\n" + """
                    kotlin.targets.all {
                        compilations.all {
                            attributes.attribute(
                                Attribute.of("com.example.compilation", String),
                                targetName + compilationName.capitalize()
                            )
                        }
                    }
                """.trimIndent()
            )
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
                assertTrue(
                    "Required com.example.compilation 'notDebug'" in output ||
                            "attribute 'com.example.compilation' with value 'notDebug'" in output
                )
            }
        }
    }

    @Test
    fun testLintInAndroidProjectsDependingOnMppWithoutAndroid() = with(Project("AndroidProject")) {
        embedProject(Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app"))
        gradleBuildScript("Lib").appendText(
            "\ndependencies { implementation(project(':sample-lib')) }"
        )
        val lintTask = ":Lib:lintFlavor1Debug"
        build(lintTask) {
            assertSuccessful()
            assertTasksExecuted(lintTask) // Check that the lint task ran successfully, KT-27170
        }
    }

    @Test
    fun testJvmWithJava() = with(Project("mppJvmWithJava")) {
        setupWorkingDir()

        build("build") {
            assertSuccessful()
        }

        build("assemble") {
            assertSuccessful()
        }
    }
}

open class KotlinAndroid70GradleIT : KotlinAndroid36GradleIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v7_0_0

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("6.8")

    override fun defaultBuildOptions(): BuildOptions {
        val javaHome = File(System.getProperty("jdk11Home")!!)
        Assume.assumeTrue("JDK 11 should be available", javaHome.isDirectory)
        return super.defaultBuildOptions().copy(javaHome = javaHome, warningMode = WarningMode.Summary)
    }
}

open class KotlinAndroid34GradleIT : KotlinAndroid3GradleIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_4_1

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

    @Test
    fun testOmittedStdlibVersion() = Project("AndroidProject").run {
        setupWorkingDir()

        gradleBuildScript("Lib").modify {
            it.checkedReplace("kotlin-stdlib:\$kotlin_version", "kotlin-stdlib") + "\n" + """
            apply plugin: 'maven'
            group 'com.example'
            version '1.0'
            android {
                defaultPublishConfig 'flavor1Debug'
            }
            uploadArchives {
                repositories {
                    mavenDeployer {
                        repository(url: "file://${'$'}buildDir/repo")
                    }
                }
            }
            """.trimIndent()
        }

        build(":Lib:assembleFlavor1Debug", ":Lib:uploadArchives") {
            assertSuccessful()
            assertTasksExecuted(":Lib:compileFlavor1DebugKotlin", ":Lib:uploadArchives")
            val pomLines = File(projectDir, "Lib/build/repo/com/example/Lib/1.0/Lib-1.0.pom").readLines()
            val stdlibVersionLineNumber = pomLines.indexOfFirst { "<artifactId>kotlin-stdlib</artifactId>" in it } + 1
            val versionLine = pomLines[stdlibVersionLineNumber]
            assertTrue { "<version>${defaultBuildOptions().kotlinVersion}</version>" in versionLine }
        }
    }
}

abstract class KotlinAndroid3GradleIT : AbstractKotlinAndroidGradleTests() {
    @Test
    fun testApplyWithFeaturePlugin() {
        Assume.assumeTrue(
            "The com.android.feature plugin has been deprecated and removed in newer versions",
            androidGradlePluginVersion < AGPVersion.v3_6_0
        )

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

abstract class AbstractKotlinAndroidGradleTests : BaseGradleIT() {

    abstract val androidGradlePluginVersion: AGPVersion

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KtTestUtil.findAndroidSdk(),
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
        libAndroidClassesOnlyUtilKt.modify {
            it.replace(
                "fun libAndroidClassesOnlyUtil(): String",
                "fun libAndroidClassesOnlyUtil(): CharSequence"
            )
        }
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
            assertContains("The 'kotlin-android-extensions' Gradle plugin is deprecated")
        }
    }

    @Test
    fun testParcelize() {
        val project = Project("AndroidParcelizeProject")
        val options = defaultBuildOptions().copy(incremental = false)

        project.build("assembleDebug", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testAndroidExtensionsIncremental() {
        Assume.assumeTrue(
            "Ignored for newer AGP versions because of KT-38622",
            androidGradlePluginVersion < AGPVersion.v3_6_0
        )

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

        project.build("assemble", options = options) {
            assertFailed()
            assertContains("Unresolved reference: textView")
        }

        File(project.projectDir, "app/build.gradle").modify { it.replace("[\"parcelize\"]", "[\"views\"]") }

        project.build("assemble", options = options) {
            assertFailed()
            assertContainsRegex("Class 'User' is not abstract and does not implement abstract member public abstract fun (writeToParcel|describeContents)".toRegex())
        }

        File(project.projectDir, "app/build.gradle").modify { it.replace("[\"views\"]", "[\"parcelize\", \"views\"]") }

        project.build("assemble", options = options) {
            assertSuccessful()
        }
    }

    @Test
    fun testDetectAndroidJava8() = with(Project("AndroidProject")) {
        setupWorkingDir()

        val kotlinJvmTarget16Regex = Regex("Kotlin compiler args: .* -jvm-target 1.6")

        gradleBuildScript("Lib").appendText(
            "\n" + """
            android.compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
            """.trimIndent()
        )

        build(":Lib:assembleDebug", "-Pkotlin.setJvmTargetFromAndroidCompileOptions=true") {
            assertSuccessful()
            assertNotContains(kotlinJvmTarget16Regex)
        }

        gradleBuildScript("Lib").appendText(
            "\n" + """
            android.compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_6
                targetCompatibility JavaVersion.VERSION_1_6
            }
            """.trimIndent()
        )

        build("clean", ":Lib:assembleDebug") {
            assertSuccessful()
            assertNotContains(kotlinJvmTarget16Regex)
        }

        build(":Lib:assembleDebug", "-Pkotlin.setJvmTargetFromAndroidCompileOptions=true") {
            assertSuccessful()
            assertContainsRegex(kotlinJvmTarget16Regex)
        }
    }
}
