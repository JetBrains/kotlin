/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@JsGradlePluginTests
class KotlinJsDomApiDependencyIT : KGPBaseTest() {

    private val defaultJsOptions = BuildOptions.JsOptions(
        useIrBackend = true,
        jsCompilerType = KotlinJsCompilerType.IR
    )

    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            jsOptions = defaultJsOptions,
            warningMode = WarningMode.Summary
        )

    @DisplayName("Kotlin/JS DOM API automatically added as dependency")
    @GradleTest
    fun testJsDomApiCompat(gradleVersion: GradleVersion) {
        project("kotlin-js-dom-api-compat", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":compileKotlinJs")
            }

            var added: String? = null

            buildGradleKts.modify {
                it + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                        }
                        """.trimIndent().also { added = it }
            }

            build("assemble") {
                assertTasksUpToDate(":compileKotlinJs")
            }

            buildGradleKts.modify {
                val replaced = it.replace(added!!, "")
                replaced + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                        """.trimIndent().also { added = it }
            }

            build("assemble") {
                assertTasksUpToDate(":compileKotlinJs")
            }

            buildGradleKts.modify {
                val replaced = it.replace(added!!, "")
                replaced + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                        """.trimIndent().also { added = it }
            }

            build("assemble") {
                assertTasksUpToDate(":compileKotlinJs")
            }
        }
    }

    @DisplayName("Kotlin/JS DOM API automatically not added as dependency with disabled adding of stdlib")
    @GradleTest
    fun testJsDomApiCompatWithDisabledAddingStdlib(gradleVersion: GradleVersion) {
        project("kotlin-js-dom-api-compat", gradleVersion) {
            var added: String? = null

            gradleProperties.modify {
                it + "\n" +
                        """
                        kotlin.stdlib.default.dependency=false
                        """.trimIndent()
            }

            buildAndFail("assemble") {
                assertTasksFailed(":compileKotlinJs")
            }

            buildGradleKts.modify {
                it + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                        }
                        """.trimIndent().also { added = it }
            }

            buildAndFail("assemble") {
                assertTasksFailed(":compileKotlinJs")
            }

            buildGradleKts.modify {
                val replaced = it.replace(added!!, "")
                replaced + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                        """.trimIndent().also { added = it }
            }

            build("assemble") {
                assertTasksExecuted(":compileKotlinJs")
            }

            buildGradleKts.modify {
                val replaced = it.replace(added!!, "")
                replaced + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                        """.trimIndent().also { added = it }
            }

            build("assemble") {
                assertTasksUpToDate(":compileKotlinJs")
            }
        }
    }

    @DisplayName("Kotlin/JS DOM API automatically not added as dependency with disabled adding of DOM API")
    @GradleTest
    fun testJsDomApiCompatWithDisabledAddingDomApi(gradleVersion: GradleVersion) {
        project("kotlin-js-dom-api-compat", gradleVersion) {
            var added: String? = null

            gradleProperties.modify {
                it + "\n" +
                        """
                        kotlin.js.stdlib.dom.api.included=false
                        """.trimIndent()
            }

            buildAndFail("assemble") {
                assertTasksFailed(":compileKotlinJs")
            }

            buildGradleKts.modify {
                it + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                        }
                        """.trimIndent().also { added = it }
            }

            buildAndFail("assemble") {
                assertTasksFailed(":compileKotlinJs")
            }

            buildGradleKts.modify {
                val replaced = it.replace(added!!, "")
                replaced + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                        """.trimIndent().also { added = it }
            }

            build("assemble") {
                assertTasksExecuted(":compileKotlinJs")
            }

            buildGradleKts.modify {
                val replaced = it.replace(added!!, "")
                replaced + "\n" +
                        """
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                        """.trimIndent().also { added = it }
            }

            build("assemble") {
                assertTasksUpToDate(":compileKotlinJs")
            }
        }
    }
}