package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assume
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class KotlinAndroid33GradleIT : KotlinAndroid32GradleIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_3_2
}

open class KotlinAndroid36GradleIT : KotlinAndroid33GradleIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_6_0

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("6.0")

    @Test
    fun testAndroidWithNewMppApp() = with(Project("new-mpp-android", GradleVersionRequired.FOR_MPP_SUPPORT)) {
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


        build("connectedAndroidTest") {
            // Test for KT-35016: MPP should recognize android instrumented tests correctly
            build("connectedAndroidTest") {
                assertFailed()
                assertContains("No connected devices!")
            }
        }
    }

    @Test
    fun testAndroidMppProductionDependenciesInTests() = with(Project("new-mpp-android", GradleVersionRequired.FOR_MPP_SUPPORT)) {
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
    fun testCustomAttributesInAndroidTargets() = with(Project("new-mpp-android", GradleVersionRequired.FOR_MPP_SUPPORT)) {
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
}

open class KotlinAndroid32GradleIT : KotlinAndroid3GradleIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_2_0

    //android build tool 28.0.3 use org.gradle.api.file.ProjectLayout#fileProperty(org.gradle.api.provider.Provider) that was deleted in gradle 6.0
    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.Until("5.6.4")

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

    @Test
    fun testExperimentalMppAndroidSourceSets() {
        Project(
            "new-mpp-experimental-android-source-sets",
            minLogLevel = LogLevel.INFO,
            gradleVersionRequirement = GradleVersionRequired.AtLeast("5.0")
        ).build("sourceSets") {
            assertSuccessful()
            assertContains(
                """
                    androidTest
                    -----------
                    Compile configuration: androidTestCompile
                    build.gradle name: android.sourceSets.androidTest
                    Java sources: [lib/src/androidTest/java, lib/src/androidDeviceTest/kotlin]
                    Manifest file: lib/src/androidDeviceTest/AndroidManifest.xml
                    Android resources: [lib/src/androidTest/res, lib/src/androidDeviceTest/res]
                    Assets: [lib/src/androidTest/assets, lib/src/androidDeviceTest/assets]
                    AIDL sources: [lib/src/androidTest/aidl, lib/src/androidDeviceTest/aidl]
                    RenderScript sources: [lib/src/androidTest/rs, lib/src/androidDeviceTest/rs]
                    JNI sources: [lib/src/androidTest/jni, lib/src/androidDeviceTest/jni]
                    JNI libraries: [lib/src/androidTest/jniLibs, lib/src/androidDeviceTest/jniLibs]
                    Java-style resources: [lib/src/androidTest/resources, lib/src/androidDeviceTest/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    androidTestDebug
                    ----------------
                    Compile configuration: androidTestDebugCompile
                    build.gradle name: android.sourceSets.androidTestDebug
                    Java sources: [lib/src/androidTestDebug/java, lib/src/androidDeviceTestDebug/kotlin]
                    Manifest file: lib/src/androidDeviceTestDebug/AndroidManifest.xml
                    Android resources: [lib/src/androidTestDebug/res, lib/src/androidDeviceTestDebug/res]
                    Assets: [lib/src/androidTestDebug/assets, lib/src/androidDeviceTestDebug/assets]
                    AIDL sources: [lib/src/androidTestDebug/aidl, lib/src/androidDeviceTestDebug/aidl]
                    RenderScript sources: [lib/src/androidTestDebug/rs, lib/src/androidDeviceTestDebug/rs]
                    JNI sources: [lib/src/androidTestDebug/jni, lib/src/androidDeviceTestDebug/jni]
                    JNI libraries: [lib/src/androidTestDebug/jniLibs, lib/src/androidDeviceTestDebug/jniLibs]
                    Java-style resources: [lib/src/androidTestDebug/resources, lib/src/androidDeviceTestDebug/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    androidTestFree
                    ---------------
                    Compile configuration: androidTestFreeCompile
                    build.gradle name: android.sourceSets.androidTestFree
                    Java sources: [lib/src/androidTestFree/java, lib/src/androidDeviceTestFree/kotlin]
                    Manifest file: lib/src/androidDeviceTestFree/AndroidManifest.xml
                    Android resources: [lib/src/androidTestFree/res, lib/src/androidDeviceTestFree/res]
                    Assets: [lib/src/androidTestFree/assets, lib/src/androidDeviceTestFree/assets]
                    AIDL sources: [lib/src/androidTestFree/aidl, lib/src/androidDeviceTestFree/aidl]
                    RenderScript sources: [lib/src/androidTestFree/rs, lib/src/androidDeviceTestFree/rs]
                    JNI sources: [lib/src/androidTestFree/jni, lib/src/androidDeviceTestFree/jni]
                    JNI libraries: [lib/src/androidTestFree/jniLibs, lib/src/androidDeviceTestFree/jniLibs]
                    Java-style resources: [lib/src/androidTestFree/resources, lib/src/androidDeviceTestFree/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    androidTestFreeDebug
                    --------------------
                    Compile configuration: androidTestFreeDebugCompile
                    build.gradle name: android.sourceSets.androidTestFreeDebug
                    Java sources: [lib/src/androidTestFreeDebug/java, lib/src/androidDeviceTestFreeDebug/kotlin]
                    Manifest file: lib/src/androidDeviceTestFreeDebug/AndroidManifest.xml
                    Android resources: [lib/src/androidTestFreeDebug/res, lib/src/androidDeviceTestFreeDebug/res]
                    Assets: [lib/src/androidTestFreeDebug/assets, lib/src/androidDeviceTestFreeDebug/assets]
                    AIDL sources: [lib/src/androidTestFreeDebug/aidl, lib/src/androidDeviceTestFreeDebug/aidl]
                    RenderScript sources: [lib/src/androidTestFreeDebug/rs, lib/src/androidDeviceTestFreeDebug/rs]
                    JNI sources: [lib/src/androidTestFreeDebug/jni, lib/src/androidDeviceTestFreeDebug/jni]
                    JNI libraries: [lib/src/androidTestFreeDebug/jniLibs, lib/src/androidDeviceTestFreeDebug/jniLibs]
                    Java-style resources: [lib/src/androidTestFreeDebug/resources, lib/src/androidDeviceTestFreeDebug/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    androidTestPaid
                    ---------------
                    Compile configuration: androidTestPaidCompile
                    build.gradle name: android.sourceSets.androidTestPaid
                    Java sources: [lib/src/androidTestPaid/java, lib/src/androidDeviceTestPaid/kotlin]
                    Manifest file: lib/src/androidDeviceTestPaid/AndroidManifest.xml
                    Android resources: [lib/src/androidTestPaid/res, lib/src/androidDeviceTestPaid/res]
                    Assets: [lib/src/androidTestPaid/assets, lib/src/androidDeviceTestPaid/assets]
                    AIDL sources: [lib/src/androidTestPaid/aidl, lib/src/androidDeviceTestPaid/aidl]
                    RenderScript sources: [lib/src/androidTestPaid/rs, lib/src/androidDeviceTestPaid/rs]
                    JNI sources: [lib/src/androidTestPaid/jni, lib/src/androidDeviceTestPaid/jni]
                    JNI libraries: [lib/src/androidTestPaid/jniLibs, lib/src/androidDeviceTestPaid/jniLibs]
                    Java-style resources: [lib/src/androidTestPaid/resources, lib/src/androidDeviceTestPaid/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    androidTestPaidDebug
                    --------------------
                    Compile configuration: androidTestPaidDebugCompile
                    build.gradle name: android.sourceSets.androidTestPaidDebug
                    Java sources: [lib/src/androidTestPaidDebug/java, lib/src/androidDeviceTestPaidDebug/kotlin]
                    Manifest file: lib/src/androidDeviceTestPaidDebug/AndroidManifest.xml
                    Android resources: [lib/src/androidTestPaidDebug/res, lib/src/androidDeviceTestPaidDebug/res]
                    Assets: [lib/src/androidTestPaidDebug/assets, lib/src/androidDeviceTestPaidDebug/assets]
                    AIDL sources: [lib/src/androidTestPaidDebug/aidl, lib/src/androidDeviceTestPaidDebug/aidl]
                    RenderScript sources: [lib/src/androidTestPaidDebug/rs, lib/src/androidDeviceTestPaidDebug/rs]
                    JNI sources: [lib/src/androidTestPaidDebug/jni, lib/src/androidDeviceTestPaidDebug/jni]
                    JNI libraries: [lib/src/androidTestPaidDebug/jniLibs, lib/src/androidDeviceTestPaidDebug/jniLibs]
                    Java-style resources: [lib/src/androidTestPaidDebug/resources, lib/src/androidDeviceTestPaidDebug/resources]
                """.trimIndent()
            )

            assertContains(
                """
                    debug
                    -----
                    Compile configuration: debugCompile
                    build.gradle name: android.sourceSets.debug
                    Java sources: [lib/src/debug/java, lib/src/androidDebug/kotlin]
                    Manifest file: lib/src/androidDebug/AndroidManifest.xml
                    Android resources: [lib/src/debug/res, lib/src/androidDebug/res]
                    Assets: [lib/src/debug/assets, lib/src/androidDebug/assets]
                    AIDL sources: [lib/src/debug/aidl, lib/src/androidDebug/aidl]
                    RenderScript sources: [lib/src/debug/rs, lib/src/androidDebug/rs]
                    JNI sources: [lib/src/debug/jni, lib/src/androidDebug/jni]
                    JNI libraries: [lib/src/debug/jniLibs, lib/src/androidDebug/jniLibs]
                    Java-style resources: [lib/src/debug/resources, lib/src/androidDebug/resources]
                """.trimIndent()
            )

            assertContains(
                """
                    free
                    ----
                    Compile configuration: freeCompile
                    build.gradle name: android.sourceSets.free
                    Java sources: [lib/src/free/java, lib/src/androidFree/kotlin]
                    Manifest file: lib/src/androidFree/AndroidManifest.xml
                    Android resources: [lib/src/free/res, lib/src/androidFree/res]
                    Assets: [lib/src/free/assets, lib/src/androidFree/assets]
                    AIDL sources: [lib/src/free/aidl, lib/src/androidFree/aidl]
                    RenderScript sources: [lib/src/free/rs, lib/src/androidFree/rs]
                    JNI sources: [lib/src/free/jni, lib/src/androidFree/jni]
                    JNI libraries: [lib/src/free/jniLibs, lib/src/androidFree/jniLibs]
                    Java-style resources: [lib/src/free/resources, lib/src/androidFree/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    freeDebug
                    ---------
                    Compile configuration: freeDebugCompile
                    build.gradle name: android.sourceSets.freeDebug
                    Java sources: [lib/src/freeDebug/java, lib/src/androidFreeDebug/kotlin]
                    Manifest file: lib/src/androidFreeDebug/AndroidManifest.xml
                    Android resources: [lib/src/freeDebug/res, lib/src/androidFreeDebug/res]
                    Assets: [lib/src/freeDebug/assets, lib/src/androidFreeDebug/assets]
                    AIDL sources: [lib/src/freeDebug/aidl, lib/src/androidFreeDebug/aidl]
                    RenderScript sources: [lib/src/freeDebug/rs, lib/src/androidFreeDebug/rs]
                    JNI sources: [lib/src/freeDebug/jni, lib/src/androidFreeDebug/jni]
                    JNI libraries: [lib/src/freeDebug/jniLibs, lib/src/androidFreeDebug/jniLibs]
                    Java-style resources: [lib/src/freeDebug/resources, lib/src/androidFreeDebug/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    freeRelease
                    -----------
                    Compile configuration: freeReleaseCompile
                    build.gradle name: android.sourceSets.freeRelease
                    Java sources: [lib/src/freeRelease/java, lib/src/androidFreeRelease/kotlin]
                    Manifest file: lib/src/androidFreeRelease/AndroidManifest.xml
                    Android resources: [lib/src/freeRelease/res, lib/src/androidFreeRelease/res]
                    Assets: [lib/src/freeRelease/assets, lib/src/androidFreeRelease/assets]
                    AIDL sources: [lib/src/freeRelease/aidl, lib/src/androidFreeRelease/aidl]
                    RenderScript sources: [lib/src/freeRelease/rs, lib/src/androidFreeRelease/rs]
                    JNI sources: [lib/src/freeRelease/jni, lib/src/androidFreeRelease/jni]
                    JNI libraries: [lib/src/freeRelease/jniLibs, lib/src/androidFreeRelease/jniLibs]
                    Java-style resources: [lib/src/freeRelease/resources, lib/src/androidFreeRelease/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    freeStaging
                    -----------
                    Compile configuration: freeStagingCompile
                    build.gradle name: android.sourceSets.freeStaging
                    Java sources: [lib/src/freeStaging/java, lib/src/androidFreeStaging/kotlin]
                    Manifest file: lib/src/androidFreeStaging/AndroidManifest.xml
                    Android resources: [lib/src/freeStaging/res, lib/src/androidFreeStaging/res]
                    Assets: [lib/src/freeStaging/assets, lib/src/androidFreeStaging/assets]
                    AIDL sources: [lib/src/freeStaging/aidl, lib/src/androidFreeStaging/aidl]
                    RenderScript sources: [lib/src/freeStaging/rs, lib/src/androidFreeStaging/rs]
                    JNI sources: [lib/src/freeStaging/jni, lib/src/androidFreeStaging/jni]
                    JNI libraries: [lib/src/freeStaging/jniLibs, lib/src/androidFreeStaging/jniLibs]
                    Java-style resources: [lib/src/freeStaging/resources, lib/src/androidFreeStaging/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    main
                    ----
                    Compile configuration: compile
                    build.gradle name: android.sourceSets.main
                    Java sources: [lib/src/main/java, lib/src/androidMain/kotlin]
                    Manifest file: lib/src/androidMain/AndroidManifest.xml
                    Android resources: [lib/src/main/res, lib/src/androidMain/res]
                    Assets: [lib/src/main/assets, lib/src/androidMain/assets]
                    AIDL sources: [lib/src/main/aidl, lib/src/androidMain/aidl]
                    RenderScript sources: [lib/src/main/rs, lib/src/androidMain/rs]
                    JNI sources: [lib/src/main/jni, lib/src/androidMain/jni]
                    JNI libraries: [lib/src/main/jniLibs, lib/src/androidMain/jniLibs]
                    Java-style resources: [lib/src/main/resources, lib/src/androidMain/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    paid
                    ----
                    Compile configuration: paidCompile
                    build.gradle name: android.sourceSets.paid
                    Java sources: [lib/src/paid/java, lib/src/androidPaid/kotlin]
                    Manifest file: lib/src/androidPaid/AndroidManifest.xml
                    Android resources: [lib/src/paid/res, lib/src/androidPaid/res]
                    Assets: [lib/src/paid/assets, lib/src/androidPaid/assets]
                    AIDL sources: [lib/src/paid/aidl, lib/src/androidPaid/aidl]
                    RenderScript sources: [lib/src/paid/rs, lib/src/androidPaid/rs]
                    JNI sources: [lib/src/paid/jni, lib/src/androidPaid/jni]
                    JNI libraries: [lib/src/paid/jniLibs, lib/src/androidPaid/jniLibs]
                    Java-style resources: [lib/src/paid/resources, lib/src/androidPaid/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    paidDebug
                    ---------
                    Compile configuration: paidDebugCompile
                    build.gradle name: android.sourceSets.paidDebug
                    Java sources: [lib/src/paidDebug/java, lib/src/androidPaidDebug/kotlin]
                    Manifest file: lib/src/androidPaidDebug/AndroidManifest.xml
                    Android resources: [lib/src/paidDebug/res, lib/src/androidPaidDebug/res]
                    Assets: [lib/src/paidDebug/assets, lib/src/androidPaidDebug/assets]
                    AIDL sources: [lib/src/paidDebug/aidl, lib/src/androidPaidDebug/aidl]
                    RenderScript sources: [lib/src/paidDebug/rs, lib/src/androidPaidDebug/rs]
                    JNI sources: [lib/src/paidDebug/jni, lib/src/androidPaidDebug/jni]
                    JNI libraries: [lib/src/paidDebug/jniLibs, lib/src/androidPaidDebug/jniLibs]
                    Java-style resources: [lib/src/paidDebug/resources, lib/src/androidPaidDebug/resources]
                """.trimIndent()
            )

            assertContains(
                """
                    paidRelease
                    -----------
                    Compile configuration: paidReleaseCompile
                    build.gradle name: android.sourceSets.paidRelease
                    Java sources: [lib/src/paidRelease/java, lib/src/androidPaidRelease/kotlin]
                    Manifest file: lib/src/androidPaidRelease/AndroidManifest.xml
                    Android resources: [lib/src/paidRelease/res, lib/src/androidPaidRelease/res]
                    Assets: [lib/src/paidRelease/assets, lib/src/androidPaidRelease/assets]
                    AIDL sources: [lib/src/paidRelease/aidl, lib/src/androidPaidRelease/aidl]
                    RenderScript sources: [lib/src/paidRelease/rs, lib/src/androidPaidRelease/rs]
                    JNI sources: [lib/src/paidRelease/jni, lib/src/androidPaidRelease/jni]
                    JNI libraries: [lib/src/paidRelease/jniLibs, lib/src/androidPaidRelease/jniLibs]
                    Java-style resources: [lib/src/paidRelease/resources, lib/src/androidPaidRelease/resources]

                """.trimIndent()
            )
            assertContains(
                """
                    paidStaging
                    -----------
                    Compile configuration: paidStagingCompile
                    build.gradle name: android.sourceSets.paidStaging
                    Java sources: [lib/src/paidStaging/java, lib/src/androidPaidStaging/kotlin]
                    Manifest file: lib/src/androidPaidStaging/AndroidManifest.xml
                    Android resources: [lib/src/paidStaging/res, lib/src/androidPaidStaging/res]
                    Assets: [lib/src/paidStaging/assets, lib/src/androidPaidStaging/assets]
                    AIDL sources: [lib/src/paidStaging/aidl, lib/src/androidPaidStaging/aidl]
                    RenderScript sources: [lib/src/paidStaging/rs, lib/src/androidPaidStaging/rs]
                    JNI sources: [lib/src/paidStaging/jni, lib/src/androidPaidStaging/jni]
                    JNI libraries: [lib/src/paidStaging/jniLibs, lib/src/androidPaidStaging/jniLibs]
                    Java-style resources: [lib/src/paidStaging/resources, lib/src/androidPaidStaging/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    release
                    -------
                    Compile configuration: releaseCompile
                    build.gradle name: android.sourceSets.release
                    Java sources: [lib/src/release/java, lib/src/androidRelease/kotlin]
                    Manifest file: lib/src/androidRelease/AndroidManifest.xml
                    Android resources: [lib/src/release/res, lib/src/androidRelease/res]
                    Assets: [lib/src/release/assets, lib/src/androidRelease/assets]
                    AIDL sources: [lib/src/release/aidl, lib/src/androidRelease/aidl]
                    RenderScript sources: [lib/src/release/rs, lib/src/androidRelease/rs]
                    JNI sources: [lib/src/release/jni, lib/src/androidRelease/jni]
                    JNI libraries: [lib/src/release/jniLibs, lib/src/androidRelease/jniLibs]
                    Java-style resources: [lib/src/release/resources, lib/src/androidRelease/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    staging
                    -------
                    Compile configuration: stagingCompile
                    build.gradle name: android.sourceSets.staging
                    Java sources: [lib/src/staging/java, lib/src/androidStaging/kotlin]
                    Manifest file: lib/src/androidStaging/AndroidManifest.xml
                    Android resources: [lib/src/staging/res, lib/src/androidStaging/res]
                    Assets: [lib/src/staging/assets, lib/src/androidStaging/assets]
                    AIDL sources: [lib/src/staging/aidl, lib/src/androidStaging/aidl]
                    RenderScript sources: [lib/src/staging/rs, lib/src/androidStaging/rs]
                    JNI sources: [lib/src/staging/jni, lib/src/androidStaging/jni]
                    JNI libraries: [lib/src/staging/jniLibs, lib/src/androidStaging/jniLibs]
                    Java-style resources: [lib/src/staging/resources, lib/src/androidStaging/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    test
                    ----
                    Compile configuration: testCompile
                    build.gradle name: android.sourceSets.test
                    Java sources: [lib/src/test/java, lib/src/androidLocalTest/kotlin]
                    Java-style resources: [lib/src/test/resources, lib/src/androidLocalTest/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testDebug
                    ---------
                    Compile configuration: testDebugCompile
                    build.gradle name: android.sourceSets.testDebug
                    Java sources: [lib/src/testDebug/java, lib/src/androidLocalTestDebug/kotlin]
                    Java-style resources: [lib/src/testDebug/resources, lib/src/androidLocalTestDebug/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testFree
                    --------
                    Compile configuration: testFreeCompile
                    build.gradle name: android.sourceSets.testFree
                    Java sources: [lib/src/testFree/java, lib/src/androidLocalTestFree/kotlin]
                    Java-style resources: [lib/src/testFree/resources, lib/src/androidLocalTestFree/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testFreeDebug
                    -------------
                    Compile configuration: testFreeDebugCompile
                    build.gradle name: android.sourceSets.testFreeDebug
                    Java sources: [lib/src/testFreeDebug/java, lib/src/androidLocalTestFreeDebug/kotlin]
                    Java-style resources: [lib/src/testFreeDebug/resources, lib/src/androidLocalTestFreeDebug/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testFreeRelease
                    ---------------
                    Compile configuration: testFreeReleaseCompile
                    build.gradle name: android.sourceSets.testFreeRelease
                    Java sources: [lib/src/testFreeRelease/java, lib/src/androidLocalTestFreeRelease/kotlin]
                    Java-style resources: [lib/src/testFreeRelease/resources, lib/src/androidLocalTestFreeRelease/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testFreeStaging
                    ---------------
                    Compile configuration: testFreeStagingCompile
                    build.gradle name: android.sourceSets.testFreeStaging
                    Java sources: [lib/src/testFreeStaging/java, lib/src/androidLocalTestFreeStaging/kotlin]
                    Java-style resources: [lib/src/testFreeStaging/resources, lib/src/androidLocalTestFreeStaging/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testPaid
                    --------
                    Compile configuration: testPaidCompile
                    build.gradle name: android.sourceSets.testPaid
                    Java sources: [lib/src/testPaid/java, lib/src/androidLocalTestPaid/kotlin]
                    Java-style resources: [lib/src/testPaid/resources, lib/src/androidLocalTestPaid/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testPaidDebug
                    -------------
                    Compile configuration: testPaidDebugCompile
                    build.gradle name: android.sourceSets.testPaidDebug
                    Java sources: [lib/src/testPaidDebug/java, lib/src/androidLocalTestPaidDebug/kotlin]
                    Java-style resources: [lib/src/testPaidDebug/resources, lib/src/androidLocalTestPaidDebug/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testPaidRelease
                    ---------------
                    Compile configuration: testPaidReleaseCompile
                    build.gradle name: android.sourceSets.testPaidRelease
                    Java sources: [lib/src/testPaidRelease/java, lib/src/androidLocalTestPaidRelease/kotlin]
                    Java-style resources: [lib/src/testPaidRelease/resources, lib/src/androidLocalTestPaidRelease/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testPaidStaging
                    ---------------
                    Compile configuration: testPaidStagingCompile
                    build.gradle name: android.sourceSets.testPaidStaging
                    Java sources: [lib/src/testPaidStaging/java, lib/src/androidLocalTestPaidStaging/kotlin]
                    Java-style resources: [lib/src/testPaidStaging/resources, lib/src/androidLocalTestPaidStaging/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testRelease
                    -----------
                    Compile configuration: testReleaseCompile
                    build.gradle name: android.sourceSets.testRelease
                    Java sources: [lib/src/testRelease/java, lib/src/androidLocalTestRelease/kotlin]
                    Java-style resources: [lib/src/testRelease/resources, lib/src/androidLocalTestRelease/resources]
                """.trimIndent()
            )
            assertContains(
                """
                    testStaging
                    -----------
                    Compile configuration: testStagingCompile
                    build.gradle name: android.sourceSets.testStaging
                    Java sources: [lib/src/testStaging/java, lib/src/androidLocalTestStaging/kotlin]
                    Java-style resources: [lib/src/testStaging/resources, lib/src/androidLocalTestStaging/resources]
                """.trimIndent()
            )
        }

        Project(
            "new-mpp-experimental-android-source-sets",
            gradleVersionRequirement = GradleVersionRequired.AtLeast("5.0")
        ).run {
            build("test") {
                assertSuccessful()
                assertContains("CommonTest PASSED")
                assertContains("AndroidLocalTest PASSED")
                assertContains("AndroidLocalTestStaging PASSED")
                assertContains("AndroidLocalTestDebug PASSED")
                assertContains("AndroidLocalTestRelease PASSED")
                assertContains("AndroidLocalTestFreeDebug PASSED")
                assertContains("AndroidLocalTestFreeRelease PASSED")
                assertContains("AndroidLocalTestFreeStaging PASSED")
                assertContains("AndroidLocalTestPaidDebug PASSED")
                assertContains("AndroidLocalTestPaidRelease PASSED")
                assertContains("AndroidLocalTestPaidStaging PASSED")
            }

            build("clean", "testFreeDebugUnitTest") {
                assertSuccessful()

                // Tests that are expected to be executed
                assertContains("CommonTest PASSED")
                assertContains("AndroidLocalTest PASSED")
                assertContains("AndroidLocalTestDebug PASSED")
                assertContains("AndroidLocalTestFreeDebug PASSED")

                // Tests that are not expected to be executed
                assertNotContains("AndroidLocalTestStaging PASSED")
                assertNotContains("AndroidLocalTestRelease PASSED")
                assertNotContains("AndroidLocalTestFreeRelease PASSED")
                assertNotContains("AndroidLocalTestFreeStaging PASSED")
                assertNotContains("AndroidLocalTestPaidDebug PASSED")
                assertNotContains("AndroidLocalTestPaidRelease PASSED")
                assertNotContains("AndroidLocalTestPaidStaging PASSED")
            }

            build("clean", "testFreeReleaseUnitTest") {
                assertSuccessful()

                assertContains("CommonTest PASSED")
                assertContains("AndroidLocalTest PASSED")
                assertContains("AndroidLocalTestRelease PASSED")
                assertContains("AndroidLocalTestFreeRelease PASSED")

                assertNotContains("AndroidLocalTestStaging PASSED")
                assertNotContains("AndroidLocalTestDebug PASSED")
                assertNotContains("AndroidLocalTestFreeDebug PASSED")
                assertNotContains("AndroidLocalTestFreeStaging PASSED")
                assertNotContains("AndroidLocalTestPaidDebug PASSED")
                assertNotContains("AndroidLocalTestPaidRelease PASSED")
                assertNotContains("AndroidLocalTestPaidStaging PASSED")
            }

            build("clean", "testFreeStagingUnitTest") {
                assertSuccessful()

                assertContains("CommonTest PASSED")
                assertContains("AndroidLocalTest PASSED")
                assertContains("AndroidLocalTestStaging PASSED")
                assertContains("AndroidLocalTestFreeStaging PASSED")

                assertNotContains("AndroidLocalTestDebug PASSED")
                assertNotContains("AndroidLocalTestRelease PASSED")
                assertNotContains("AndroidLocalTestFreeDebug PASSED")
                assertNotContains("AndroidLocalTestFreeRelease PASSED")
                assertNotContains("AndroidLocalTestPaidDebug PASSED")
                assertNotContains("AndroidLocalTestPaidRelease PASSED")
                assertNotContains("AndroidLocalTestPaidStaging PASSED")
            }

            build("clean", "testPaidDebugUnitTest") {
                assertSuccessful()

                assertContains("CommonTest PASSED")
                assertContains("AndroidLocalTest PASSED")
                assertContains("AndroidLocalTestDebug PASSED")
                assertContains("AndroidLocalTestPaidDebug PASSED")

                assertNotContains("AndroidLocalTestStaging PASSED")
                assertNotContains("AndroidLocalTestRelease PASSED")
                assertNotContains("AndroidLocalTestFreeDebug PASSED")
                assertNotContains("AndroidLocalTestFreeRelease PASSED")
                assertNotContains("AndroidLocalTestFreeStaging PASSED")
                assertNotContains("AndroidLocalTestPaidRelease PASSED")
                assertNotContains("AndroidLocalTestPaidStaging PASSED")
            }

            build("clean", "testPaidReleaseUnitTest") {
                assertSuccessful()

                assertContains("CommonTest PASSED")
                assertContains("AndroidLocalTest PASSED")
                assertContains("AndroidLocalTestRelease PASSED")
                assertContains("AndroidLocalTestPaidRelease PASSED")

                assertNotContains("AndroidLocalTestStaging PASSED")
                assertNotContains("AndroidLocalTestDebug PASSED")
                assertNotContains("AndroidLocalTestFreeDebug PASSED")
                assertNotContains("AndroidLocalTestFreeRelease PASSED")
                assertNotContains("AndroidLocalTestFreeStaging PASSED")
                assertNotContains("AndroidLocalTestPaidDebug PASSED")
                assertNotContains("AndroidLocalTestPaidStaging PASSED")
            }

            build("clean", "testPaidStagingUnitTest") {
                assertSuccessful()
                assertContains("CommonTest PASSED")
                assertContains("AndroidLocalTest PASSED")
                assertContains("AndroidLocalTestStaging PASSED")
                assertContains("AndroidLocalTestPaidStaging PASSED")

                assertNotContains("AndroidLocalTestDebug PASSED")
                assertNotContains("AndroidLocalTestRelease PASSED")
                assertNotContains("AndroidLocalTestFreeDebug PASSED")
                assertNotContains("AndroidLocalTestFreeRelease PASSED")
                assertNotContains("AndroidLocalTestFreeStaging PASSED")
                assertNotContains("AndroidLocalTestPaidDebug PASSED")
                assertNotContains("AndroidLocalTestPaidRelease PASSED")
            }

            build("connectedAndroidTest") {
                // Test for KT-35016: MPP should recognize android instrumented tests correctly
                build("connectedAndroidTest") {
                    assertFailed()
                    assertContains("No connected devices!")
                }
            }
        }
    }
}

class KotlinAndroid30GradleIT : KotlinAndroid3GradleIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_0_0

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.Until("4.10.2")

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

        if (androidGradlePluginVersion >= AGPVersion.v3_6_0) {

        }

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

        build(":Lib:assembleDebug", "-Pkotlin.setJvmTargetFromAndroidCompileOptions=true") {
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

        build("clean", ":Lib:assembleDebug") {
            assertSuccessful()
            assertNotContains(kotlinJvmTarget18Regex)
        }

        build(":Lib:assembleDebug", "-Pkotlin.setJvmTargetFromAndroidCompileOptions=true") {
            assertSuccessful()
            assertContainsRegex(kotlinJvmTarget18Regex)
        }
    }
}