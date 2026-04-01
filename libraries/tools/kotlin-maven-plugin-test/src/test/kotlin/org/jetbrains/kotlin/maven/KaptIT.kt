/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName

class KaptIT : KotlinMavenTestBase() {

    @MavenTest
    @DisplayName("KAPT annotationProcessorPaths is applied without explicit version in pom.xml")
    fun testKaptAnnotationProcessorPathsWithoutVersion(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-kapt-annotationProcessorPaths-without-version", mavenVersion, buildOptions) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertJarExistsAndNotEmpty("app-with-kapt/target/app-with-kapt-1.0-SNAPSHOT.jar")
                assertFileExists(
                    "app-with-kapt/target/generated-sources/kaptKotlin/compile/MyClass.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }
            }
        }
    }

    @MavenTest
    @DisplayName("KAPT generates Kotlin code from annotation processor")
    fun testKaptGenerateKotlinCode(mavenVersion: TestVersions.Maven) {
        val buildOptions = if (isWindowsHost) buildOptions.copy(useKotlinDaemon = false) else buildOptions
        testProject("test-kapt-generateKotlinCode", mavenVersion, buildOptions) {
            build(
                "package", "-X",
                expectedToFail = false
            ) {
                assertBuildLogContains(
                    "[INFO] [kapt] Kapt is enabled.",
                    "[INFO] [kapt] Annotation processors: example.ExampleAnnotationProcessor"
                )
                assertJarExistsAndNotEmpty("app/target/app-1.0-SNAPSHOT.jar")
                assertFileExists(
                    "app/target/generated-sources/kaptKotlin/compile/MyClass.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }
            }
        }
    }

    @MavenTest
    @DisplayName("KAPT processes Dagger annotations on JDK 8")
    fun testDaggerAnnotationProcessingJdk8(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-dagger-maven-example", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                ).withoutKotlinDaemon(
                    "https://youtrack.jetbrains.com/issue/KT-71048: Dagger 2.9 hits ConcurrentModificationException in HashMap.computeIfAbsent on JDK 9+. " +
                            "The daemon may reuse a JDK 17+ process, so we force in-process compilation on JDK 8."
                )
            ) {
                assertJarExistsAndNotEmpty("target/dagger-maven-example-1.0-SNAPSHOT.jar")
            }
        }
    }

    @MavenTest
    @DisplayName("KAPT with allopen generates Dagger sources on JDK 8")
    fun testKaptWithAllopenOnJdk8(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-kapt-allopen", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                ).withoutKotlinDaemon(
                    "https://youtrack.jetbrains.com/issue/KT-71048: Dagger 2.9 hits ConcurrentModificationException in HashMap.computeIfAbsent on JDK 9+. " +
                            "The daemon may reuse a JDK 17+ process, so we force in-process compilation on JDK 8."
                )
            ) {
                assertJarExistsAndNotEmpty("target/dagger-maven-example-1.0-SNAPSHOT.jar")
                assertFileExists("target/generated-sources/kapt/compile/coffee/CoffeeMaker_Factory.java")
            }
        }
    }

    @MavenTest
    @DisplayName("Lombok with KAPT produces generated Java helpers, Kotlin extensions, and stubs")
    fun testLombokWithKapt(mavenVersion: TestVersions.Maven) {
        testProject("test-lombok-with-kapt", mavenVersion) {
            build(
                "package",
                expectedToFail = false
            ) {
                assertPluginApplied("lombok")
                assertJarExistsAndNotEmpty("annotation-processor/target/lombok-kapt-annotation-processor-1.0-SNAPSHOT.jar")
                assertJarExistsAndNotEmpty("app/target/lombok-kapt-app-1.0-SNAPSHOT.jar")
                assertFileExists(
                    "app/target/generated-sources/kapt/compile/cats/CatHouseHelper.java"
                ) { "KAPT-generated Java helper class was not found" }
                assertFileExists(
                    "app/target/generated-sources/kaptKotlin/compile/cats/CatHouseExtensions.kt"
                ) { "KAPT-generated Kotlin extension file was not found" }
                assertFileExists(
                    "app/target/kaptStubs/compile/cats/CatHouse.java"
                ) { "KAPT stub for CatHouse was not found" }
            }
        }
    }

    @MavenTest
    @DisplayName("KAPT with allopen and extensions enabled generates Dagger sources on JDK 8")
    fun testKaptWithAllopenAndSmartDefaultsOnJdk8(mavenVersion: TestVersions.Maven) {
        testProject("java8/test-enable-extensions-kapt-allopen", mavenVersion) {
            build(
                "package",
                expectedToFail = false,
                buildOptions = buildOptions.copy(
                    javaVersion = TestVersions.Java.JDK_1_8,
                    extraMavenProperties = mapOf("kotlinCompilerJdk" to context.getJavaHomeString(TestVersions.Java.JDK_1_8))
                ).withoutKotlinDaemon(
                    "https://youtrack.jetbrains.com/issue/KT-71048: Dagger 2.9 hits ConcurrentModificationException in HashMap.computeIfAbsent on JDK 9+. " +
                            "The daemon may reuse a JDK 17+ process, so we force in-process compilation on JDK 8."
                )
            ) {
                assertJarExistsAndNotEmpty("target/test-enable-extensions-kapt-allopen-1.0-SNAPSHOT.jar")
                assertFileExists("target/generated-sources/kapt/compile/coffee/CoffeeMaker_Factory.java")
            }
        }
    }
}
