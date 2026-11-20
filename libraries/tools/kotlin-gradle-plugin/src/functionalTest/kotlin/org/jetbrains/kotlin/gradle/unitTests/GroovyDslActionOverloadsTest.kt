/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * KT-91482: Groovy closures passed to the DSL methods with `Action` overloads must be applied with the correct delegate.
 */
class GroovyDslActionOverloadsTest {

    class Environment(
        val target: String,
        val enable: String,
        val whenConfigured: String,
        val testTaskName: String,
    ) {
        override fun toString() = "$target.$whenConfigured"
    }

    companion object {
        @JvmStatic
        fun environments(): Stream<Arguments> = Stream.of(
            Environment("js", "browser()", "whenBrowserConfigured", "jsBrowserTest"),
            Environment("js", "nodejs()", "whenNodejsConfigured", "jsNodeTest"),
            Environment("wasmJs", "d8()", "whenD8Configured", "wasmJsD8Test"),
        ).map { Arguments.of(it) }
    }

    private fun Project.applyGroovy(script: String) {
        val file = file("groovy-${script.hashCode()}.gradle")
        file.writeText(script.trimIndent())
        apply(mapOf("from" to file))
    }

    @ParameterizedTest
    @MethodSource("environments")
    fun `groovy callbacks registered before and after enabling the environment reach its test task`(env: Environment) {
        val project = buildProjectWithMPP()
        project.applyGroovy(
            """
            kotlin {
                ${env.target} {
                    ${env.whenConfigured} {
                        testTask {
                            description = 'groovy-before-callback'
                        }
                    }
                    ${env.enable}
                    ${env.whenConfigured} {
                        testTask {
                            group = 'groovy-after-callback'
                        }
                    }
                }
            }
            """
        )
        project.evaluate()

        val task = assertNotNull(project.tasks.findByName(env.testTaskName), "Missing task '${env.testTaskName}'")
        assertEquals("groovy-before-callback", task.description)
        assertEquals("groovy-after-callback", task.group)
    }

    @ParameterizedTest
    @MethodSource("environments")
    fun `groovy callback alone does not enable the environment`(env: Environment) {
        val project = buildProjectWithMPP()
        project.applyGroovy(
            """
            kotlin {
                ${env.target} {
                    ${env.whenConfigured} {
                        testTask {
                            description = 'groovy-callback-only'
                        }
                    }
                }
            }
            """
        )
        project.evaluate()

        assertNull(project.tasks.findByName(env.testTaskName), "Task '${env.testTaskName}' must not exist")
    }

    @Test
    fun `groovy mainRun configures mainClass and args during project evaluation`() {
        val project = buildProjectWithMPP()
        project.applyGroovy(
            """
            kotlin {
                jvm {
                    mainRun {
                        mainClass.set('org.example.GroovyMain')
                        args('first', 'second')
                    }
                }
            }
            """
        )
        project.evaluate()

        val task = project.tasks.getByName("jvmRun") as JavaExec
        assertEquals("org.example.GroovyMain", task.mainClass.get())
        assertEquals(listOf("first", "second"), task.args)
    }
}
