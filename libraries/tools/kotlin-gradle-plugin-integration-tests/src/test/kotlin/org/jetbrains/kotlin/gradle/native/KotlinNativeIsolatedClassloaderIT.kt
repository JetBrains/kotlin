/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText

@DisplayName("KotlinNative isolated class loader test")
@NativeGradlePluginTests
internal class KotlinNativeIsolatedClassloaderIT : KGPBaseTest() {

    @DisplayName("KT-65761: K2Native isolated class loader should be able to load platform classes")
    @JdkVersions(versions = [JavaVersion.VERSION_1_8, JavaVersion.VERSION_21])
    @GradleWithJdkTest
    @OsCondition(
        supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS],
        enabledOnCI = [OS.LINUX], // Compiler plugin is leaking file descriptors preventing cleaning the project on Windows: KT-50545
    )
    fun shouldLoadPlatformClasses(gradleVersion: GradleVersion, providedJdk: JdkVersions.ProvidedJdk) {
        /*
         * Ensure that the class is accessible here.
         * We try to access it in the isolated classpath and ensure that it's not accessible there.
         */
        org.gradle.launcher.bootstrap.EntryPoint::class.java
        nativeProject("compilerPlugins/pluginUsesJdkClass", gradleVersion, buildJdk = providedJdk.location) {
            build(":library:compileKotlinLinuxX64")
        }
    }
}
