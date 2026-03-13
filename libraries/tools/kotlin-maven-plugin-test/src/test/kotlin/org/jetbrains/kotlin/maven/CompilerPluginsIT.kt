/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.KotlinMavenTestBase
import org.jetbrains.kotlin.maven.test.MavenTest
import org.jetbrains.kotlin.maven.test.TestVersions
import org.jetbrains.kotlin.maven.test.assertBuildLogContains
import org.jetbrains.kotlin.maven.test.assertFilesExist

class CompilerPluginsIT : KotlinMavenTestBase() {
    @MavenTest
    fun testDebugLogsKt77036(mavenVersion: TestVersions.Maven) {
        testProject("kotlin-no-arg", mavenVersion) {
            build("compile", "-X") {
                assertFilesExist(
                    "target/classes/org/jetbrains/example/NoArg.class",
                    "target/classes/org/jetbrains/example/SomeClass.class"
                )
                assertBuildLogContains(
                    "Loaded Maven plugin org.jetbrains.kotlin.test.KotlinNoArgMavenPluginExtension",
                    "Plugin options are: plugin:org.jetbrains.kotlin.noarg:annotation=com.my.Annotation"
                )
            }
        }
    }
}
