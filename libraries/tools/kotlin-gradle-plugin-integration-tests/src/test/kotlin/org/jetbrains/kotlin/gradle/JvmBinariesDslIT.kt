/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@DisplayName("KMP JVM target binaries DSL")
@MppGradlePluginTests
class JvmBinariesDslIT : KGPBaseTest() {

    @DisplayName("Default binary is runnable")
    @GradleTest
    fun defaultBinaryIsRunnable(gradleVersion: GradleVersion) {
        project("mppRunJvm", gradleVersion) {
            subProject("multiplatform").buildScriptInjection {
                kotlinMultiplatform.jvm {
                    binaries {
                        executable {
                            mainClass.set("JvmMainKt")
                        }
                    }
                }
            }

            build("runJvmMain") {
                assertTasksExecuted(":multiplatform:runJvmMain")
            }
        }
    }

    @DisplayName("Default binary with JPMS is runnable")
    @GradleTest
    fun defaultBinaryWithJpmsIsRunnable(gradleVersion: GradleVersion) {
        project("mppRunJvm", gradleVersion) {
            val jvmModuleInfoFile = subProject("jvm").javaSourcesDir().resolve("module-info.java")
            jvmModuleInfoFile.parent.toFile().mkdirs()
            jvmModuleInfoFile.writeText(
                """
                |module com.util {
                |    exports com.util;
                |    requires kotlin.stdlib;
                |    requires kotlinx.coroutines.core;
                |}
                """.trimMargin()
            )
            subProject("jvm").kotlinSourcesDir().resolve("Jvm.kt").modify {
                """
                |package com.util
                |$it
                """.trimMargin()
            }
            subProject("jvm").javaSourcesDir().resolve("com/util/Empty.java")
                .apply { parent.toFile().mkdirs() }
                .writeText(
                    """
                    package com.util;
                    public class Empty {}
                    """.trimIndent()
                )

            val kmpModuleInfoFile = subProject("multiplatform").javaSourcesDir("jvmMain").resolve("module-info.java")
            kmpModuleInfoFile.parent.toFile().mkdirs()
            kmpModuleInfoFile.writeText(
                //language=java
                """
                |module org.example {
                |    exports org.example;
                |    requires kotlin.stdlib;
                |    requires com.util;
                |}
                """.trimMargin()
            )

            subProject("multiplatform")
                .kotlinSourcesDir("jvmMain")
                .resolve("JvmMain.kt")
                .modify {
                    """
                    package org.example
                    
                    import com.util.Jvm
                    $it
                    """.trimIndent()
                }
            subProject("multiplatform")
                .kotlinSourcesDir("commonMain")
                .resolve("CommonMain.kt")
                .modify {
                    """
                    package org.example
                    
                    import com.util.Jvm
                    """.trimIndent()
                }

            subProject("multiplatform").javaSourcesDir("jvmMain").resolve("org/example/Empty.java")
                .apply { parent.toFile().mkdirs() }
                .writeText(
                    //language=java
                    """
                    package org.example;
                    public class Empty {}
                    """.trimIndent()
                )


            subProject("multiplatform").buildScriptInjection {
                kotlinMultiplatform.jvm {
                    withJava()
                    binaries {
                        executable {
                            mainClass.set("org.example.JvmMainKt")
                            mainModule.set("org.example")
                        }
                    }
                }

                java.modularity.inferModulePath.set(true)
            }

            build("runJvmMain") {
                assertTasksExecuted(":multiplatform:runJvmMain")
            }
        }
    }
}