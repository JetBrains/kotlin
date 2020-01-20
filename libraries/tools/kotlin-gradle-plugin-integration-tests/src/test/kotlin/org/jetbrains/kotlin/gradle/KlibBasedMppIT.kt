/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import kotlin.test.Ignore
import kotlin.test.Test

/** FIXME (sergey.igushkin): please enable these tests back as soon as the Kotlin/Native version that is bundled with the
 *        Kotlin distribution supports compilation to klib and targetless klibs.
 */
@Ignore
class KlibBasedMppIT : BaseGradleIT() {
    override val defaultGradleVersion = GradleVersionRequired.AtLeast("6.0")

    @Test
    fun testBuildWithProjectDependency() = testBuildWithDependency {
        gradleBuildScript().appendText("\n" + """
            dependencies {
                commonMainImplementation(project("$embeddedModuleName"))
            }
        """.trimIndent())
    }

    @Test
    fun testBuildWithPublishedDependency() = testBuildWithDependency {
        build(":$embeddedModuleName:publish") {
            assertSuccessful()
        }

        gradleBuildScript().appendText("\n" + """
            repositories {
                maven("${'$'}rootDir/repo")
            }
            dependencies {
                commonMainImplementation("com.example:$embeddedModuleName:1.0")
            }
        """.trimIndent())

        // prevent Gradle from linking the above dependency to the project:
        gradleBuildScript(embeddedModuleName).appendText("\ngroup = \"some.other.group\"")
    }

    private val embeddedModuleName = "project-dep"

    private fun testBuildWithDependency(configureDependency: Project.() -> Unit) = with(Project("common-klib-lib-and-app")) {
        Assume.assumeTrue(HostManager.hostIsMac)

        embedProject(Project("common-klib-lib-and-app"), renameTo = embeddedModuleName)
        gradleBuildScript().modify(::transformBuildScriptWithPluginsDsl)

        projectDir.resolve(embeddedModuleName + "/src").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            file.modify { it.replace("package com.h0tk3y.hmpp.klib.demo", "package com.projectdep") }
        }

        configureDependency()

        projectDir.resolve("src/commonMain/kotlin/LibUsage.kt").appendText("\n" + """
            package com.h0tk3y.hmpp.klib.demo.test
            
            import com.projectdep.LibCommonMainExpect as ProjectDepExpect
            
            private fun useProjectDep() {
                ProjectDepExpect()
            }
        """.trimIndent())

        projectDir.resolve("src/iosMain/kotlin/LibIosMainUsage.kt").appendText("\n" + """
            package com.h0tk3y.hmpp.klib.demo.test
            
            import com.projectdep.libIosMainFun as libFun
            
            private fun useProjectDep() {
                libFun()
            }
        """.trimIndent())

        val tasksToExecute = listOf(
            ":compileJvmAndJsMainKotlinMetadata",
            ":compileIosMainKotlinMetadata"
        )

        build("assemble") {
            assertSuccessful()

            assertTasksExecuted(*tasksToExecute.toTypedArray())
        }
    }
}