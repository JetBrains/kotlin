/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.library.KLIB_PROPERTY_SHORT_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.inputStream
import kotlin.test.assertEquals

@MppGradlePluginTests
class MppKlibIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "kt-36721-mpp-klibs-with-same-name")
    @DisplayName("KT-36721: Klibs with the same project name")
    fun testKlibsWithTheSameProjectName(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-36721-mpp-klibs-with-same-name",
            gradleVersion = gradleVersion,
        ) {
            build("assemble") {
                assertTasksExecuted(
                    ":foo:foo:compileKotlinJs",
                    ":foo:foo:compileKotlinLinux",
                    ":foo:compileKotlinJs",
                    ":foo:compileKotlinLinux",
                    ":compileKotlinJs",
                    ":compileKotlinLinux",
                )

                val interopManifest = subProject("foo").kotlinClassesDir(targetName = "linux").resolve("cinterop/foo-cinterop-bar.klib")
                    .useAsZipFile { zipFile ->
                        zipFile.readKLibManifest()
                    }
                assertEquals("org.sample.one:foo-cinterop-bar", interopManifest[KLIB_PROPERTY_UNIQUE_NAME])

                val nativeManifest = projectPath.resolve("foo/build/classes/kotlin/linux/main/klib/foo.klib")
                    .useAsZipFile { zipFile ->
                        zipFile.readKLibManifest()
                    }
                assertEquals("org.sample.one:foo", nativeManifest[KLIB_PROPERTY_UNIQUE_NAME])

                // Check the short name that is used as a prefix in generated ObjC headers.
                assertEquals("foo", nativeManifest[KLIB_PROPERTY_SHORT_NAME])

                val jsManifest = projectPath.resolve("foo/build/classes/kotlin/js/main/default/manifest")
                    .inputStream()
                    .useToLoadProperties()
                assertEquals("org.sample.one:foo", jsManifest[KLIB_PROPERTY_UNIQUE_NAME])
            }
        }
    }
}
