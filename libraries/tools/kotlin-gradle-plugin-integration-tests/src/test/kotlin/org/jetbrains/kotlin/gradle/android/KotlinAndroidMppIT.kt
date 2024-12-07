/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testbase.TestVersions.AgpCompatibilityMatrix
import org.jetbrains.kotlin.gradle.tooling.BuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.streams.toList
import kotlin.test.*

@DisplayName("kotlin-android with mpp")
@AndroidGradlePluginTests
class KotlinAndroidMppIT : KGPBaseTest() {

    @DisplayName("KotlinToolingMetadataArtifact is bundled into apk")
    @GradleAndroidTest
    fun testKotlinToolingMetadataBundle(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "kotlinToolingMetadataAndroid",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                assertTasksAreNotInTaskGraph(":${BuildKotlinToolingMetadataTask.defaultTaskName}")

                val debugApk = projectPath.resolve("build/outputs/apk/debug/project-debug.apk")
                assertFileExists(debugApk)
                ZipFile(debugApk.toFile()).use { zip ->
                    assertNull(zip.getEntry("kotlin-tooling-metadata.json"), "Expected metadata *not* being packaged into debug apk")
                }
            }

            build("assembleRelease") {
                assertTasksExecuted(":${BuildKotlinToolingMetadataTask.defaultTaskName}")
                val releaseApk = projectPath.resolve("build/outputs/apk/release/project-release-unsigned.apk")

                assertFileExists(releaseApk)
                ZipFile(releaseApk.toFile()).use { zip ->
                    assertNotNull(zip.getEntry("kotlin-tooling-metadata.json"), "Expected metadata being packaged into release apk")
                }
            }
        }
    }

    @DisplayName("mpp source sets are registered in AGP")
    @GradleAndroidTest
    fun testAndroidMppSourceSets(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android-source-sets",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("sourceSets") {
                fun assertOutputContainsOsIndependent(expectedString: String) {
                    assertOutputContains(expectedString.replace("/", File.separator))
                }
                assertOutputContainsOsIndependent("Android resources: [lib/src/main/res, lib/src/androidMain/res]")
                assertOutputContainsOsIndependent("Assets: [lib/src/main/assets, lib/src/androidMain/assets]")
                assertOutputContainsOsIndependent("AIDL sources: [lib/src/main/aidl, lib/src/androidMain/aidl]")
                assertOutputContainsOsIndependent("RenderScript sources: [lib/src/main/rs, lib/src/androidMain/rs]")
                assertOutputContainsOsIndependent("JNI sources: [lib/src/main/jni, lib/src/androidMain/jni]")
                assertOutputContainsOsIndependent("JNI libraries: [lib/src/main/jniLibs, lib/src/androidMain/jniLibs]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/src/main/resources, lib/src/androidMain/resources]")

                assertOutputContainsOsIndependent("Android resources: [lib/src/androidTestDebug/res, lib/src/androidInstrumentedTestDebug/res]")
                assertOutputContainsOsIndependent("Assets: [lib/src/androidTestDebug/assets, lib/src/androidInstrumentedTestDebug/assets]")
                assertOutputContainsOsIndependent("AIDL sources: [lib/src/androidTestDebug/aidl, lib/src/androidInstrumentedTestDebug/aidl]")
                assertOutputContainsOsIndependent("RenderScript sources: [lib/src/androidTestDebug/rs, lib/src/androidInstrumentedTestDebug/rs]")
                assertOutputContainsOsIndependent("JNI sources: [lib/src/androidTestDebug/jni, lib/src/androidInstrumentedTestDebug/jni]")
                assertOutputContainsOsIndependent("JNI libraries: [lib/src/androidTestDebug/jniLibs, lib/src/androidInstrumentedTestDebug/jniLibs]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/src/androidTestDebug/resources, lib/src/androidInstrumentedTestDebug/resources]")

                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/paidBeta/resources, lib/src/androidPaidBeta/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/paidBetaDebug/resources, lib/src/androidPaidBetaDebug/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/paidBetaRelease/resources, lib/src/androidPaidBetaRelease/resources]")

                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/freeBeta/resources, lib/src/androidFreeBeta/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/freeBetaDebug/resources, lib/src/androidFreeBetaDebug/resources]")
                assertOutputContainsOsIndependent("Java-style resources: [lib/betaSrc/freeBetaRelease/resources, lib/src/androidFreeBetaRelease/resources]")
            }

            buildAndFail("testFreeBetaDebug") {
                assertOutputContains("CommonTest > fail FAILED")
                assertOutputContains("TestKotlin > fail FAILED")
                assertOutputContains("AndroidTestKotlin > fail FAILED")
                assertOutputContains("TestJava > fail FAILED")
            }

            build("assemble")

            buildAndFail("connectedAndroidTest") {
                assertOutputContains("No connected devices!")
            }
        }
    }

    @DisplayName("android mpp lib flavors publication can be configured")
    @GradleAndroidTest
    fun testMppAndroidLibFlavorsPublication(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val androidSourcesElementsAttributes = arrayOf(
            "org.gradle.category" to "documentation",
            "org.gradle.dependency.bundling" to "external",
            "org.gradle.docstype" to "sources",
            "org.gradle.libraryelements" to "jar",
            "org.gradle.jvm.environment" to "android",
            "org.gradle.usage" to "java-runtime",
            "org.jetbrains.kotlin.platform.type" to "androidJvm",

            // user-specific attributes that added manually in build script
            "com.example.compilation" to "release",
            "com.example.target" to "androidLib",
        )

        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val groupDir = subProject("lib").projectPath.resolve("build/repo/com/example")
            build("publish") {
                assertDirectoryExists(groupDir.resolve("lib-jvmlib"))
                assertDirectoryExists(groupDir.resolve("lib-jslib"))
                assertDirectoryExists(groupDir.resolve("lib-androidlib"))
                assertDirectoryExists(groupDir.resolve("lib-androidlib-debug"))
            }
            groupDir.deleteRecursively()

            // Choose a single variant to publish, check that it's there:
            subProject("lib").buildGradleKts.appendText(
                """
                    
                kotlin.androidTarget("androidLib").publishLibraryVariants("release")
                """.trimIndent()
            )
            build("publish") {
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.aar"))
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0-sources.jar"))
                assertGradleVariant(
                    groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.module"),
                    "releaseSourcesElements-published"
                ) {
                    assertAttributesEquals(*androidSourcesElementsAttributes)
                }
                assertFileInProjectNotExists("$groupDir/lib-androidlib-debug")
            }
            groupDir.deleteRecursively()

            // Enable publishing for all Android variants:
            subProject("lib").buildGradleKts.appendText(
                """

                kotlin.androidTarget("androidLib") { publishAllLibraryVariants() }
                """.trimIndent()
            )
            build("publish") {
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.aar"))
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0-sources.jar"))
                assertGradleVariant(
                    groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.module"),
                    "releaseSourcesElements-published"
                ) {
                    assertAttributesEquals(*androidSourcesElementsAttributes)
                }
                assertFileExists(groupDir.resolve("lib-androidlib-debug/1.0/lib-androidlib-debug-1.0.aar"))
                assertFileExists(groupDir.resolve("lib-androidlib-debug/1.0/lib-androidlib-debug-1.0-sources.jar"))
                assertGradleVariant(
                    groupDir.resolve("lib-androidlib-debug/1.0/lib-androidlib-debug-1.0.module"),
                    "debugSourcesElements-published"
                ) {
                    assertAttributesEquals(
                        *androidSourcesElementsAttributes,
                        "com.example.compilation" to "debug",
                        "com.android.build.api.attributes.BuildTypeAttr" to "debug"
                    )
                }
            }
            groupDir.deleteRecursively()

            // Then group the variants by flavor and check that only one publication is created:
            subProject("lib").buildGradleKts.appendText(
                """

                kotlin.androidTarget("androidLib").publishLibraryVariantsGroupedByFlavor = true
                """.trimIndent()
            )
            build("publish") {
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.aar"))
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0-sources.jar"))
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0-debug.aar"))
                assertFileExists(groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0-debug-sources.jar"))
                assertGradleVariant(
                    groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.module"),
                    "releaseSourcesElements-published"
                ) {
                    assertAttributesEquals(*androidSourcesElementsAttributes)
                }
                assertGradleVariant(
                    groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.module"),
                    "debugSourcesElements-published"
                ) {
                    assertAttributesEquals(
                        *androidSourcesElementsAttributes,
                        "com.example.compilation" to "debug",
                        "com.android.build.api.attributes.BuildTypeAttr" to "debug"
                    )
                }
            }
            groupDir.deleteRecursively()

            // Add one flavor dimension with two flavors, check that the flavors produce grouped publications:
            subProject("lib").buildGradleKts.appendText(
                """

                android { 
                    flavorDimensions("foo") 
                    productFlavors {
                        create("fooBar") {
                            dimension = "foo"
                        }

                        create("fooBaz") {
                            dimension = "foo"
                        }
                    }
                }                    
                """.trimIndent()
            )
            build("publish") {
                listOf("fooBar", "fooBaz").forEach { flavorName ->
                    val flavor = flavorName.lowercase()
                    val flavorAttributes = arrayOf(
                        "foo" to flavorName,
                        "com.android.build.api.attributes.ProductFlavor:foo" to flavorName
                    )

                    assertFileExists(groupDir.resolve("lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0.aar"))
                    assertFileExists(groupDir.resolve("lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0-sources.jar"))
                    assertFileExists(groupDir.resolve("lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0-debug.aar"))
                    assertFileExists(groupDir.resolve("lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0-debug-sources.jar"))
                    assertGradleVariant(
                        groupDir.resolve("lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0.module"),
                        "${flavorName}ReleaseSourcesElements-published"
                    ) {
                        assertAttributesEquals(
                            *androidSourcesElementsAttributes,
                            *flavorAttributes,
                            "com.example.compilation" to "${flavorName}Release",
                        )
                    }
                    assertGradleVariant(
                        groupDir.resolve("lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0.module"),
                        "${flavorName}DebugSourcesElements-published"
                    ) {
                        assertAttributesEquals(
                            *androidSourcesElementsAttributes,
                            *flavorAttributes,
                            "com.example.compilation" to "${flavorName}Debug",
                            "com.android.build.api.attributes.BuildTypeAttr" to "debug"
                        )
                    }
                }
            }
            groupDir.deleteRecursively()

            // Disable the grouping and check that all the variants are published under separate artifactIds:
            subProject("lib").buildGradleKts.appendText(
                """
                    
                kotlin.androidTarget("androidLib") { publishLibraryVariantsGroupedByFlavor = false }    
                """.trimIndent()
            )
            build("publish") {
                listOf("fooBar", "fooBaz").forEach { flavorName ->
                    val flavor = flavorName.lowercase()

                    val flavorAttributes = arrayOf(
                        "foo" to flavorName,
                        "com.android.build.api.attributes.ProductFlavor:foo" to flavorName
                    )

                    listOf("-debug", "").forEach { buildType ->
                        assertFileExists(groupDir.resolve("lib-androidlib-$flavor$buildType/1.0/lib-androidlib-$flavor$buildType-1.0.aar"))
                        assertFileExists(groupDir.resolve("lib-androidlib-$flavor$buildType/1.0/lib-androidlib-$flavor$buildType-1.0-sources.jar"))
                    }

                    assertGradleVariant(
                        groupDir.resolve("lib-androidlib-$flavor/1.0/lib-androidlib-$flavor-1.0.module"),
                        "${flavorName}ReleaseSourcesElements-published"
                    ) {
                        assertAttributesEquals(
                            *androidSourcesElementsAttributes,
                            *flavorAttributes,
                            "com.example.compilation" to "${flavorName}Release",
                        )
                    }

                    assertGradleVariant(
                        groupDir.resolve("lib-androidlib-$flavor-debug/1.0/lib-androidlib-$flavor-debug-1.0.module"),
                        "${flavorName}DebugSourcesElements-published"
                    ) {
                        assertAttributesEquals(
                            *androidSourcesElementsAttributes,
                            *flavorAttributes,
                            "com.example.compilation" to "${flavorName}Debug",
                            "com.android.build.api.attributes.BuildTypeAttr" to "debug"
                        )
                    }
                }
            }
        }
    }

    @DisplayName("Sources publication can be disabled")
    @GradleAndroidTest
    fun testDisableSourcesPublication(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            subProject("lib").buildGradleKts.appendText(
                """
                    
                    kotlin.androidTarget("androidLib") {
                        withSourcesJar(publish = false)
                        publishLibraryVariants("release")
                    }
                """.trimIndent()
            )

            val groupDir = subProject("lib").projectPath.resolve("build/repo/com/example")
            build("publish") {
                val sourcesJarFile = groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0-sources.jar").toFile()
                if (sourcesJarFile.exists()) fail("Release sources jar should not be published")

                val gradleMetadataFileContent = groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.module").readText()
                if (gradleMetadataFileContent.contains("releaseSourcesElements-published")) {
                    fail("'releaseSourcesElements-published' variant should not be published")
                }
            }
        }
    }

    @DisplayName("android mpp lib dependencies are properly rewritten")
    @GradleAndroidTest
    fun testMppAndroidLibDependenciesRewriting(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
                .disableConfigurationCache_KT70416(),
            buildJdk = jdkVersion.location
        ) {
            // Convert the 'app' project to a library, publish two flavors without metadata,
            // check that the dependencies in the POMs are correctly rewritten:
            val appGroupDir = subProject("app").projectPath.resolve("build/repo/com/example")

            subProject("lib").buildGradleKts.appendText(
                """
                
                android { 
                    flavorDimensions("foo") 
                    productFlavors {
                        create("fooBar") {
                            dimension = "foo"
                        }

                        create("fooBaz") {
                            dimension = "foo"
                        }
                    }
                }
                """.trimIndent()
            )

            subProject("app").buildGradleKts.modify {
                it.replace("com.android.application", "com.android.library")
                    .replace("applicationId", "//")
                    .replace("versionCode", "//")
                    .replace("versionName", "//")
                    .replace("plugins {\n", "plugins {\n `maven-publish`\n") +
                        """

                        publishing {
                            repositories {
                                maven {
                                    url = uri("${'$'}buildDir/repo")
                                }
                            }
                        }
                        kotlin.androidTarget("androidApp") { publishAllLibraryVariants() }
                        android { 
                            flavorDimensions("foo") 
                            productFlavors {
                                create("fooBar") {
                                    dimension = "foo"
                                }
            
                                create("fooBaz") {
                                    dimension = "foo"
                                }
                            }
                        }                        
                        """.trimIndent()
            }
            build("publish") {
                listOf("foobar", "foobaz").forEach { flavor ->
                    listOf("-debug", "").forEach { buildType ->
                        assertFileExists(appGroupDir.resolve("app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0.aar"))
                        assertFileExists(appGroupDir.resolve("app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0-sources.jar"))
                        val pomText =
                            appGroupDir.resolve("app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0.pom").readText()
                                .replace("\\s+".toRegex(), "")
                        assertContains(
                            pomText,
                            "<artifactId>lib-androidlib-$flavor$buildType</artifactId><version>1.0</version><scope>runtime</scope>"
                        )
                    }
                }
            }
            appGroupDir.deleteRecursively()

            // Also check that api and runtimeOnly MPP dependencies get correctly published with the appropriate scope, KT-29476:
            subProject("app").buildGradleKts.modify {
                it.replace("implementation(project(\":lib\")", "api(project(\":lib\")") +
                        """

                        kotlin.sourceSets.getByName("commonMain").dependencies {
                            runtimeOnly(kotlin("reflect"))
                        }
                        """.trimIndent()
            }
            build("publish") {
                listOf("foobar", "foobaz").forEach { flavor ->
                    listOf("-debug", "").forEach { buildType ->
                        val pomText =
                            appGroupDir.resolve("app-androidapp-$flavor$buildType/1.0/app-androidapp-$flavor$buildType-1.0.pom").readText()
                                .replace("\\s+".toRegex(), "")
                        assertContains(
                            pomText,
                            "<artifactId>lib-androidlib-$flavor$buildType</artifactId><version>1.0</version><scope>compile</scope>"
                        )
                        assertContains(
                            pomText,
                            "<artifactId>kotlin-reflect</artifactId><scope>runtime</scope>"
                        )
                    }
                }
            }
        }
    }

    @DisplayName("KT-69585: kmp + android depends on another kmp + android project via included build should not fail on pom rewrite action")
    @GradleAndroidTest
    fun kt69585PublishWithDependencyOnIncludedBuildsDoesntFail(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions
                .copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            settingsGradle.replaceText("include ':app', ':lib'", "include ':lib'")
            includeOtherProjectAsIncludedBuild("lib", "new-mpp-android", "libFromIncluded")
            subProject("lib").buildGradleKts.appendText("""
                
                kotlin { 
                  sourceSets.getByName("androidLibMain").dependencies {
                    implementation("com.example:libFromIncluded:1.0")
                  }
                }
                """.trimIndent()
            )
            build(":lib:generatePomFileForAndroidLibReleasePublication") {
                val pomText = projectPath
                    .resolve("lib/build/publications/androidLibRelease/pom-default.xml")
                    .readText()
                    .replace("""\s+""".toRegex(), "")

                assertContains(
                    pomText,
                    """<groupId>com.example</groupId><artifactId>libFromIncluded-androidlib</artifactId><version>1.0</version>"""
                )
            }
        }
    }

    @DisplayName("android app can depend on mpp lib")
    @GradleAndroidTest
    fun testAndroidWithNewMppApp(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assemble", "compileDebugUnitTestJavaWithJavac", "printCompilerPluginOptions") {
                // KT-30784
                assertOutputDoesNotContain("API 'variant.getPackageLibrary()' is obsolete and has been replaced")

                assertOutputContains("KT-29964 OK") // Output from lib/build.gradle

                assertTasksExecuted(
                    ":lib:compileDebugKotlinAndroidLib",
                    ":lib:compileReleaseKotlinAndroidLib",
                    ":lib:compileKotlinJvmLib",
                    ":lib:compileKotlinJsLib",
                    ":lib:compileCommonMainKotlinMetadata",
                    ":app:compileDebugKotlinAndroidApp",
                    ":app:compileReleaseKotlinAndroidApp",
                    ":app:compileKotlinJvmApp",
                    ":app:compileKotlinJsApp",
                    ":app:compileCommonMainKotlinMetadata",
                    ":lib:compileDebugUnitTestJavaWithJavac",
                    ":app:compileDebugUnitTestJavaWithJavac"
                )

                listOf("debug", "release").forEach { variant ->
                    assertFileInProjectExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/ExpectedLibClass.class")
                    assertFileInProjectExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/CommonLibClass.class")
                    assertFileInProjectExists("lib/build/tmp/kotlin-classes/$variant/com/example/lib/AndroidLibClass.class")

                    assertFileInProjectExists("app/build/tmp/kotlin-classes/$variant/com/example/app/AKt.class")
                    assertFileInProjectExists("app/build/tmp/kotlin-classes/$variant/com/example/app/KtUsageKt.class")
                }
            }
        }
    }

    @DisplayName("KT-29343: mpp source set dependencies are propagated to android tests")
    @GradleAndroidTest
    fun testAndroidMppProductionDependenciesInTests(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            // Test the fix for KT-29343
            subProject("lib").buildGradleKts.appendText(
                """

                kotlin.sourceSets {
                    commonMain {
                        dependencies {
                            implementation(kotlin("stdlib-common"))
                        }
                    }
                    val androidLibDebug by creating {
                        dependencies {
                            implementation(kotlin("reflect"))
                        }
                    }
                    val androidLibRelease by creating {
                        dependencies {
                            implementation(kotlin("test-junit"))
                        }
                    }
                }
                """.trimIndent()
            )

            val kotlinVersion = buildOptions.kotlinVersion
            testResolveAllConfigurations("lib") { _, buildResult ->
                // androidLibDebug:
                buildResult.assertOutputContains(">> :lib:debugCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:releaseCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:debugAndroidTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:debugUnitTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:releaseUnitTestCompileClasspath --> kotlin-reflect-$kotlinVersion.jar")

                // androidLibRelease:
                buildResult.assertOutputDoesNotContain(">> :lib:debugCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:releaseCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:debugAndroidTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputDoesNotContain(">> :lib:debugUnitTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
                buildResult.assertOutputContains(">> :lib:releaseUnitTestCompileClasspath --> kotlin-test-junit-$kotlinVersion.jar")
            }
        }
    }

    @DisplayName("KT-27714: custom attributes are copied to android compilation configurations")
    @GradleAndroidTest
    fun testCustomAttributesInAndroidTargets(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val libBuildScript = subProject("lib").buildGradleKts
            val appBuildScript = subProject("app").buildGradleKts

            // Enable publishing for all Android variants:
            libBuildScript.appendText(
                """

                kotlin.androidTarget("androidLib") { publishAllLibraryVariants() }
                """.trimIndent()
            )

            val groupDir = subProject("lib").projectPath.resolve("build/repo/com/example")

            build("publish") {
                // Also check that custom user-specified attributes are written in all Android modules metadata:
                assertFileContains(
                    groupDir.resolve("lib-androidlib/1.0/lib-androidlib-1.0.module"),
                    "\"com.example.target\": \"androidLib\"",
                    "\"com.example.compilation\": \"release\""
                )
                assertFileContains(
                    groupDir.resolve("lib-androidlib-debug/1.0/lib-androidlib-debug-1.0.module"),
                    "\"com.example.target\": \"androidLib\"",
                    "\"com.example.compilation\": \"debug\""
                )
            }
            groupDir.deleteRecursively()

            // Check that the consumer side uses custom attributes specified in the target and compilations:
            val appBuildScriptBackup = appBuildScript.readText()
            val libBuildScriptBackup = libBuildScript.readText()

            libBuildScript.appendText(
                """

                kotlin.targets.all { 
                    attributes.attribute(
                        Attribute.of("com.example.target", String::class.java),
                        targetName
                    )
                }
                """.trimIndent()
            )
            appBuildScript.appendText(
                """

                kotlin.targets.getByName("androidApp").attributes.attribute(
                    Attribute.of("com.example.target", String::class.java),
                    "notAndroidLib"
                )
                """.trimIndent()
            )

            buildAndFail(":app:compileDebugKotlinAndroidApp") {
                // dependency resolution should fail
                assertOutputContainsAny(
                    "Required com.example.target 'notAndroidLib'",
                    "attribute 'com.example.target' with value 'notAndroidLib'",
                )
            }

            libBuildScript.writeText(
                libBuildScriptBackup +
                        """

                        kotlin.targets.all {
                            compilations.all {
                                attributes.attribute(
                                    Attribute.of("com.example.compilation", String::class.java),
                                    target.name + compilationName.capitalize()
                                )
                            }
                        }
                        """.trimIndent()
            )
            appBuildScript.writeText(
                appBuildScriptBackup +
                        """

                        kotlin.targets.getByName("androidApp").compilations.all {
                            attributes.attribute(
                                Attribute.of("com.example.compilation", String::class.java),
                                "notDebug"
                            )
                        }
                        """.trimIndent()
            )

            buildAndFail(":app:compileDebugKotlinAndroidApp") {
                assertOutputContainsAny(
                    "Required com.example.compilation 'notDebug'",
                    "attribute 'com.example.compilation' with value 'notDebug'",
                )
            }
        }
    }

    @DisplayName("KT-27170: android lint works with dependency on non-android mpp project")
    @GradleAndroidTest
    fun testLintInAndroidProjectsDependingOnMppWithoutAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        disabledOnWindowsWhenAgpVersionIsLowerThan(agpVersion, "7.4.0", "Lint leaves opened file descriptors")
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            includeOtherProjectAsSubmodule(pathPrefix = "new-mpp-lib-and-app", otherProjectName = "sample-lib")
            subProject("Lib").buildGradle.appendText(
                //language=Gradle
                """

                dependencies { implementation(project(':sample-lib')) }
                """.trimIndent()
            )
            val lintTask = ":Lib:lintFlavor1Debug"
            build(lintTask) {
                assertTasksExecuted(lintTask) // Check that the lint task ran successfully, KT-27170
            }
        }
    }

    @DisplayName("MPP allTests task depending on Android unit tests")
    @GradleAndroidTest
    fun testMppAllTests(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build(":lib:allTests", "--dry-run") {
                assertOutputContains(":lib:testDebugUnitTest SKIPPED")
                assertOutputContains(":lib:testReleaseUnitTest SKIPPED")
            }
        }
    }

    /**
     * Starting from AGP version 7.1.0-alpha13, a new attribute com.android.build.api.attributes.AgpVersionAttr was added.
     * This attribute is *not intended* to be published.
     */
    @DisplayName("KT-49798: com.android.build.api.attributes.AgpVersionAttr is not published")
    @GradleAndroidTest
    fun testKT49798AgpVersionAttrNotPublished(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "new-mpp-android",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("publish") {
                val libProject = subProject("lib")
                val debugPublicationDirectory = libProject.projectPath.resolve("build/repo/com/example/lib-androidlib-debug")
                val releasePublicationDirectory = libProject.projectPath.resolve("build/repo/com/example/lib-androidlib")

                listOf(debugPublicationDirectory, releasePublicationDirectory).forEach { publicationDirectory ->
                    assertDirectoryExists(publicationDirectory)
                    val moduleFiles = Files.walk(publicationDirectory).use { it.filter { file -> file.extension == "module" }.toList() }
                    assertTrue(moduleFiles.isNotEmpty(), "Missing .module file in $publicationDirectory")
                    assertTrue(moduleFiles.size == 1, "Multiple .module files in $publicationDirectory: $moduleFiles")

                    val moduleFile = moduleFiles.single()
                    val moduleFileText = moduleFile.readText()
                    assertTrue("AgpVersionAttr" !in moduleFileText, ".module file $moduleFile leaks AgpVersionAttr")
                }
            }
        }
    }

    // TODO: improve it via KT-63409
    @DisplayName("produced artifacts are consumable by projects with various AGP versions")
    @GradleAndroidTest
    fun testAndroidMultiplatformPublicationAGPCompatibility(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        @TempDir tempDir: Path
    ) {
        project(
            "new-mpp-android-agp-compatibility",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location,
            localRepoDir = tempDir
        ) {
            /* Publish a producer library with the current version of AGP */
            build(":producer:publishAllPublicationsToBuildDirRepository") {
                /* Check expected publication layout */
                assertDirectoryExists(tempDir.resolve("com/example/producer-android"))
                assertDirectoryExists(tempDir.resolve("com/example/producer-android-debug"))
                assertDirectoryExists(tempDir.resolve("com/example/producer-jvm"))
            }
        }

        val checkedConsumerAGPVersions = AgpCompatibilityMatrix.entries
            .filter { agp ->
                AgpCompatibilityMatrix.fromVersion(agp.version) < AgpCompatibilityMatrix.fromVersion(TestVersions.AGP.MAX_SUPPORTED)
            }

        checkedConsumerAGPVersions.forEach { consumerAgpVersion ->
            println(
                "Testing compatibility for AGP consumer version $consumerAgpVersion on Gradle" +
                        " ${consumerAgpVersion.minSupportedGradleVersion} (Producer: $agpVersion)"
            )
            project(
                "new-mpp-android-agp-compatibility",
                consumerAgpVersion.minSupportedGradleVersion,
                buildOptions = defaultBuildOptions.copy(androidVersion = consumerAgpVersion.version),
                buildJdk = File(System.getProperty("jdk${consumerAgpVersion.requiredJdkVersion.majorVersion}Home")),
                localRepoDir = tempDir
            ) {
                /*
                Project: multiplatformAndroidConsumer is a mpp project with jvm and android targets.
                This project depends on the previous publication as 'commonMainImplementation' dependency
                */
                build(":multiplatformAndroidConsumer:assemble")

                /*
                Project: plainAndroidConsumer only uses the 'kotlin("android")' plugin
                This project depends on the previous publication as 'implementation' dependency
                 */
                build(":plainAndroidConsumer:assemble")
            }
            println(
                "Successfully tested compatibility for AGP consumer version $consumerAgpVersion on Gradle" +
                        " ${consumerAgpVersion.minSupportedGradleVersion} (Producer: $agpVersion)"
            )
        }
    }

    @DisplayName("KT-49877, KT-35916: associate compilation dependencies are passed correctly to android test compilations")
    @GradleAndroidTest
    fun testAssociateCompilationDependenciesArePassedToAndroidTestCompilations(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "kt-49877",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build(":compileDebugUnitTestKotlinAndroid", ":compileReleaseUnitTestKotlinAndroid") {
                assertTasksExecuted(
                    ":compileDebugKotlinAndroid",
                    ":compileReleaseKotlinAndroid",
                    ":compileDebugUnitTestKotlinAndroid",
                    ":compileReleaseUnitTestKotlinAndroid",
                )
            }

            // instrumented tests don't work without a device, so we only compile them
            build("packageDebugAndroidTest") {
                assertTasksExecuted(
                    ":compileDebugAndroidTestKotlinAndroid",
                )
            }
        }
    }

    @GradleAndroidTest
    fun mppAndroidRenameDiagnosticReportedOnKts(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) = testAndroidRenameReported(gradleVersion, agpVersion, jdkVersion, "mppAndroidRenameKts")

    @GradleAndroidTest
    fun mppAndroidRenameDiagnosticReportedOnGroovy(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) = testAndroidRenameReported(gradleVersion, agpVersion, jdkVersion, "mppAndroidRenameGroovy")

    private fun testAndroidRenameReported(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
        projectName: String
    ) {
        project(
            projectName,
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            val assertions: BuildResult.() -> Unit = {
                val errors = output.lines().filter { it.startsWith("e:") }.toSet()
                assert(
                    errors.any { error -> error.contains("androidTarget") }
                )
            }

            if (buildGradleKts.exists()) {
                buildAndFail("tasks", assertions = assertions)
            } else {
                build("tasks", assertions = assertions)
            }
        }
    }


    // https://youtrack.jetbrains.com/issue/KT-48436
    @GradleAndroidTest
    fun testUnusedSourceSetsReportAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "new-mpp-android", gradleVersion,
            defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug") {
                output.assertNoDiagnostic(KotlinToolingDiagnostics.UnusedSourceSetsWarning)
            }
        }
    }

    @GradleAndroidTest
    fun smokeTestWithIcerockMobileMultiplatformGradlePlugin(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "kgp-with-icerock-mobile-multiplatform", gradleVersion,
            defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            settingsGradleKts.replaceText(
                "resolutionStrategy {",
                """
                    resolutionStrategy {
                        eachPlugin {
                            if (requested.id.id.startsWith("dev.icerock.mobile.multiplatform")) {
                                useModule("dev.icerock:mobile-multiplatform:0.14.2")
                            }
                        }
                """.trimIndent()
            )
            build("assemble", "-Pmobile.multiplatform.useIosShortcut=false")
        }
    }

    @DisplayName("KT-63753: K2 File \"does not belong to any module\" when it is generated by `registerJavaGeneratingTask` in AGP")
    @GradleAndroidTest
    fun sourceGenerationTaskAddedToAndroidVariant(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            "new-mpp-android", gradleVersion,
            defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            // Code copied from the reproducer from KT-63753
            subProject("app").buildGradleKts.appendText(
                """
                    
                    abstract class FileGeneratingTask : DefaultTask() {
                        @get:OutputDirectory
                        abstract val outputDir: DirectoryProperty

                        @TaskAction
                        fun taskAction() {
                            val outputDirFile = outputDir.asFile.get()
                            outputDirFile.mkdirs()
                            val file = File(outputDirFile, "Test.kt")
                            val text = ""${'"'}
                                val hello = "World!"
                            ""${'"'}
                            file.writeText(text)
                        }
                    }
                    
                    android {
                        applicationVariants.configureEach {
                            val variant = this
                            val outputDir = File(buildDir, "generateExternalFile/${'$'}{variant.dirName}")
                            val task = project.tasks.register("generateExternalFile${'$'}{variant.name.capitalize()}", FileGeneratingTask::class.java) {
                                this.outputDir.set(outputDir)
                            }
                            variant.registerJavaGeneratingTask(task, outputDir)
                        }                    
                    }
                """.trimIndent()
            )
            build(":app:compileDebugKotlinAndroidApp") {
                assertTasksExecuted(":app:compileDebugKotlinAndroidApp")
            }
        }
    }

    @DisplayName("KT-70380: KMM App failed to consume android binary lib when published incorrectly")
    @GradleAndroidTest
    @AndroidTestVersions(additionalVersions = [TestVersions.AGP.AGP_81])
    @GradleTestVersions(additionalVersions = [TestVersions.Gradle.G_8_1, TestVersions.Gradle.G_8_2, TestVersions.Gradle.G_8_3])
    fun kotlinAndroidHasBuildTypeAttribute(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        kotlinAndroidLibraryProject(gradleVersion, agpVersion, jdkVersion).apply {
            buildScriptInjection {
                applyMavenPublishPlugin()
                publishing.publications.create("default", MavenPublication::class.java) { publication ->
                    publication.groupId = "com.example"
                    publication.artifactId = "lib"
                    publication.version = "1.0"

                    project.afterEvaluate {
                        publication.from(project.components.getByName("release"))
                    }
                }
            }

            build("publish") {
                if (agpVersion == TestVersions.AGP.AGP_73) {
                    // AGP 7.3 configures Publication automatically, so no diagnostic should be reported
                    assertNoDiagnostic(KotlinToolingDiagnostics.AndroidPublicationNotConfigured)
                } else {
                    assertHasDiagnostic(KotlinToolingDiagnostics.AndroidPublicationNotConfigured)
                }
            }
        }
    }
}
