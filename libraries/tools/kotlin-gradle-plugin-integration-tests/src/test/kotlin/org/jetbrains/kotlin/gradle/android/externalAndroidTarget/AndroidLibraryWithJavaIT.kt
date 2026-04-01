/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.androidLibrary
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import java.util.zip.ZipFile
import kotlin.test.assertNotNull

@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_813)
@AndroidGradlePluginTests
class AndroidLibraryWithJavaIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `test - androidLibrary - withJava enabled`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample"
                        withJava()
                    }
                }
            }

            javaSourcesDir("androidMain").resolve("sample/JavaClass.java").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample;
                public class JavaClass {
                    public String ping() {
                        return "java";
                    }
                    public String callKotlin() {
                        return new KotlinClass().ping() + ":" + ping();
                    }
                }
                """.trimIndent()
                )
            }

            kotlinSourcesDir("androidMain").resolve("sample/KotlinClass.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample
                class KotlinClass {
                    fun ping(): String = "kotlin"
                    fun callJava(): String = JavaClass().ping()
                }
                """.trimIndent()
                )
            }

            build("assemble") {
                assertTasksExecuted(":compileAndroidMainJavaWithJavac")
                assertFileInProjectExists("build/outputs/aar/empty.aar")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/JavaClass.class")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/KotlinClass.class")
            }
        }
    }

    @GradleAndroidTest
    fun `test - androidLibrary - withJava disabled`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.options"
                    }
                }
            }

            javaSourcesDir("androidMain").resolve("sample/JavaClass.java").apply {
                parent.toFile().mkdirs()
                toFile().writeText("package sample; public class JavaClass {}")
            }

            kotlinSourcesDir("androidMain").resolve("sample/KotlinClass.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample
                class KotlinClass {
                    fun useJava(): String = JavaClass().toString()
                }
                """.trimIndent()
                )
            }

            buildAndFail("assemble") {
                assertTasksFailed(":compileAndroidMain")
                assertTasksAreNotInTaskGraph(":compileAndroidMainJavaWithJavac")
                assertFileInProjectNotExists("build/outputs/aar/empty.aar")
            }
        }
    }

    @GradleAndroidTest
    fun `test - withJava enabled without Java sources`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.nojava"
                        withJava()
                    }
                }
            }

            kotlinSourcesDir("androidMain").resolve("sample/OnlyKotlin.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample
                class OnlyKotlin { fun ok() = "ok" }
                """.trimIndent()
                )
            }

            build("assemble") {
                assertFileInProjectExists("build/outputs/aar/empty.aar")
                assertTasksNoSource(":compileAndroidMainJavaWithJavac")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/OnlyKotlin.class")
            }
        }
    }

    @GradleAndroidTest
    fun `test - withJava enabled - androidMain Java sees declarations from commonMain and sharedMain`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.shared"
                        withJava()
                    }
                    iosArm64()

                    val sharedMain = sourceSets.create("sharedMain").apply {
                        dependsOn(sourceSets.getByName("commonMain"))
                    }
                    sourceSets.getByName("androidMain").dependsOn(sharedMain)
                    sourceSets.getByName("iosArm64Main").dependsOn(sharedMain)
                }
            }

            kotlinSourcesDir("commonMain").resolve("sample/CommonBase.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample

                open class CommonBase {
                    fun commonValue(): String = "common"
                }
                """.trimIndent()
                )
            }

            kotlinSourcesDir("sharedMain").resolve("sample/SharedBase.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample

                open class SharedBase : CommonBase() {
                    fun sharedValue(): String = "shared:" + commonValue()
                }
                """.trimIndent()
                )
            }

            javaSourcesDir("androidMain").resolve("sample/JavaClass.java").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample;

                public class JavaClass extends SharedBase {
                    public String ping() {
                        return sharedValue() + ":" + commonValue();
                    }
                }
                """.trimIndent()
                )
            }

            build("assemble") {
                assertTasksExecuted(":compileAndroidMainJavaWithJavac")
                assertFileInProjectExists("build/outputs/aar/empty.aar")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/CommonBase.class")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/SharedBase.class")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/JavaClass.class")
            }
        }
    }

    @GradleAndroidTest
    fun `test - withJava enabled - androidMain actual typealias can point to Java class`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.expectactual"
                        withJava()
                    }
                }
            }

            kotlinSourcesDir("commonMain").resolve("sample/PlatformGreeter.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample

                expect class PlatformGreeter() {
                    fun ping(): String
                }

                class KotlinCaller {
                    fun call(): String = PlatformGreeter().ping()
                }
                """.trimIndent()
                )
            }

            javaSourcesDir("androidMain").resolve("sample/JavaPlatformGreeter.java").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample;

                public final class JavaPlatformGreeter {
                    public String ping() {
                        return "java";
                    }
                }
                """.trimIndent()
                )
            }

            kotlinSourcesDir("androidMain").resolve("sample/PlatformGreeter.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package sample

                actual typealias PlatformGreeter = JavaPlatformGreeter
                """.trimIndent()
                )
            }

            build("assemble") {
                assertTasksExecuted(":compileAndroidMainJavaWithJavac")
                assertFileInProjectExists("build/outputs/aar/empty.aar")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/KotlinCaller.class")
                assertAarContainsClass("build/outputs/aar/empty.aar", "sample/JavaPlatformGreeter.class")
            }
        }
    }

    @GradleAndroidTest
    fun `test - withJava enabled - androidHostTest Java test sees declarations from commonTest`(
        gradleVersion: GradleVersion, androidVersion: String, jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.hosttest"
                        withJava()
                        withHostTest {}
                    }
                    iosArm64()
                    sourceSets.getByName("androidHostTest").dependencies {
                        implementation("junit:junit:4.13.2")
                    }
                }
            }

            kotlinSourcesDir("commonTest").resolve("test/CommonTestBase.kt").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package test
                open class CommonTestBase {
                    fun commonTestValue(): String = "common-test"
                }
                """.trimIndent()
                )
            }

            javaSourcesDir("androidHostTest").resolve("test/HostTestJava.java").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package test;

                import org.junit.Test;
                import static org.junit.Assert.assertEquals;

                public class HostTestJava extends CommonTestBase {
                    @Test
                    public void javaTestCanSeeCommonTestDeclarations() {
                        assertEquals("common-test", commonTestValue());
                    }
                }
                """.trimIndent()
                )
            }

            build(":testAndroidHostTest") {
                assertTasksExecuted(":compileAndroidHostTest")
                assertTasksExecuted(":testAndroidHostTest")
            }
        }
    }

    @GradleAndroidTest
    fun `test - withJava enabled - androidDeviceTest supports Java sources`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = androidVersion),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }

            buildScriptInjection {
                kotlinMultiplatform.apply {
                    androidLibrary {
                        compileSdk = 34
                        namespace = "org.jetbrains.sample.devicetest"
                        withJava()
                        withDeviceTest {}
                    }

                    sourceSets.getByName("androidDeviceTest").dependencies {
                        implementation("junit:junit:4.13.2")
                    }
                }
            }

            javaSourcesDir("androidDeviceTest").resolve("test/DeviceTestJava.java").apply {
                parent.toFile().mkdirs()
                toFile().writeText(
                    """
                package test;

                public class DeviceTestJava {
                    public String value() {
                        return "okie";
                    }
                }
                """.trimIndent()
                )
            }

            build("compileAndroidDeviceTestJavaWithJavac") {
                assertTasksExecuted(":compileAndroidDeviceTestJavaWithJavac")
            }
        }
    }

    private fun TestProject.assertAarContainsClass(aarPath: String, classPath: String) {
        val aarFile = projectPath.resolve(aarPath).toFile()
        check(aarFile.exists()) { "AAR file does not exist: $aarPath" }

        ZipFile(aarFile).use { aarZip ->
            val classesJarEntry = aarZip.getEntry("classes.jar")
            check(classesJarEntry != null) { "classes.jar not found inside AAR: $aarPath" }

            val tempJar = kotlin.io.path.createTempFile(suffix = ".jar").toFile().apply {
                deleteOnExit()
            }

            aarZip.getInputStream(classesJarEntry).use { input ->
                tempJar.outputStream().use { output -> input.copyTo(output) }
            }

            ZipFile(tempJar).use { classesJar ->
                val classEntry = classesJar.getEntry(classPath)
                assertNotNull(
                    classEntry,
                    "Class $classPath not found inside classes.jar of $aarPath"
                )
            }
        }
    }
}
