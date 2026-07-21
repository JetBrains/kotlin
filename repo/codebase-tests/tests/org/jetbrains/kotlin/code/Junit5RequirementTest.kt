/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.fail

/**
 * This test will scan over all .class files produced by this repository.
 * Classes, which contain methods annotated with `@org.junit.Test` will be reported as failure,
 * ensuring that no junit4 tests are present.
 */
@EnabledIfEnvironmentVariable(
    named = "Junit5RequirementTest", matches = "true",
    disabledReason = "Requires all commits associated with 'https://youtrack.jetbrains.com/issue/KTI-3471' to be merged"
)
class Junit5RequirementTest {

    private val junit4TestAnnotationDesc = "Lorg/junit/Test;"
    private val junit3TestCaseName = "junit/framework/TestCase"

    /*
    Packages listed here are not checked, as they have a valid reason for still using junit5
     */
    private val packageWhitelist = listOf(
        "kotlin/test" // We still ship a junit4 variant
    )

    @Test
    fun `no junit4 annotations or junit3 TestCase is used`() {
        val violations = mutableListOf<String>()

        forEachCompiledClass { _, node ->
            packageWhitelist.forEach { packageName ->
                if (node.name.startsWith(packageName)) return@forEachCompiledClass
            }

            /* Check for junit3 TestCase */
            if (node.superName == junit3TestCaseName) {
                violations.add(buildString {
                    appendLine("${node.name}: Inherits from junit3 'TestCase'")
                })
            }

            /* Check for junit4 test annotation */
            node.methods.forEach { methodNode ->
                (methodNode.visibleAnnotations.orEmpty())
                    .forEach { annotationNode ->
                        if (annotationNode.desc == junit4TestAnnotationDesc) {
                            violations.add(buildString {
                                appendLine("${node.name}.${methodNode.name} is using a junit4 'Test' annotation")
                                appendLine("  - junit4 is not supported anymore, please migrate to junit5")
                            })
                        }
                    }
            }
        }

        if (violations.isNotEmpty()) {
            fail(buildString {
                appendLine("Found ${violations.size} violation(s)")
                violations.forEach { violation ->
                    appendLine(violation.prependIndent("  "))
                }
            })
        }
    }
}
