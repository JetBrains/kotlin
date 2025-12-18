/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName

@JsGradlePluginTests
class KotlinJsDomApiDependencyIT : KGPBaseTest() {

    private val defaultJsOptions = BuildOptions.JsOptions(
    )

    override val defaultBuildOptions =
        super.defaultBuildOptions.copy(
            jsOptions = defaultJsOptions,
        ).disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("Kotlin/JS DOM API automatically added as dependency")
    @GradleTest
    fun testJsDomApiCompat(gradleVersion: GradleVersion) {
        project("kotlin-js-dom-api-compat", gradleVersion) {
            build("assemble") {
                assertTasksExecuted(":compileKotlinJs")
            }
        }
    }

    @DisplayName("Kotlin/JS DOM API automatically not added as dependency with disabled adding of stdlib")
    @GradleTest
    fun testJsDomApiCompatWithDisabledAddingStdlib(gradleVersion: GradleVersion) {
        project("kotlin-js-dom-api-compat", gradleVersion) {
            gradleProperties.modify {
                it + "\n" +
                        """
                        kotlin.stdlib.default.dependency=false
                        """.trimIndent()
            }

            val addStdlibProperty = "addStdlib"
            val addDomApiCompatProperty = "addDomApiCompat"

            buildScriptInjection {
                project.applyMultiplatform {
                    sourceSets.jsMain.dependencies {
                        if (project.hasProperty(addStdlibProperty)) {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib")
                        }
                        if (project.hasProperty(addDomApiCompatProperty)) {
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                    }
                }
            }

            build("assemble") {
                assertTasksExecuted(":compileKotlinJs")
            }

            build("assemble", "-P${addStdlibProperty}") {
                assertTasksUpToDate(":compileKotlinJs")
            }

            build("assemble", "-P${addDomApiCompatProperty}") {
                assertTasksUpToDate(":compileKotlinJs")
            }

            build("assemble", "-P${addStdlibProperty}", "-P${addDomApiCompatProperty}") {
                assertTasksUpToDate(":compileKotlinJs")
            }
        }
    }

    @DisplayName("Kotlin/JS DOM API automatically not added as dependency with disabled adding of DOM API")
    @GradleTest
    fun testJsDomApiCompatWithDisabledAddingDomApi(gradleVersion: GradleVersion) {
        project("kotlin-js-dom-api-compat", gradleVersion) {
            gradleProperties.modify {
                it + "\n" +
                        """
                        kotlin.js.stdlib.dom.api.included=false
                        """.trimIndent()
            }

            val addStdlibProperty = "addStdlib"
            val addDomApiCompatProperty = "addDomApiCompat"

            buildScriptInjection {
                project.applyMultiplatform {
                    sourceSets.jsMain.dependencies {
                        if (project.hasProperty(addStdlibProperty)) {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib")
                        }
                        if (project.hasProperty(addDomApiCompatProperty)) {
                            implementation("org.jetbrains.kotlin:kotlin-dom-api-compat")
                        }
                    }
                }
            }

            buildAndFail("assemble") {
                assertTasksFailed(":compileKotlinJs")
            }

            buildAndFail("assemble", "-P${addStdlibProperty}") {
                assertTasksFailed(":compileKotlinJs")
            }

            build("assemble", "-P${addDomApiCompatProperty}") {
                assertTasksExecuted(":compileKotlinJs")
            }

            build("assemble", "-P${addStdlibProperty}", "-P${addDomApiCompatProperty}") {
                assertTasksUpToDate(":compileKotlinJs")
            }
        }
    }
}
