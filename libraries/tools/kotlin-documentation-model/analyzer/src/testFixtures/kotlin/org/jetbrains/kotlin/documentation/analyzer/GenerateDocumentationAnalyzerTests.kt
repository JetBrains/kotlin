/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(
            testsRoot = "libraries/tools/kotlin-documentation-model/analyzer/src/tests-gen",
            testDataRoot = "libraries/tools/kotlin-documentation-model/analyzer/src/test/resources/testData/analyzer",
        ) {
            testClass<AbstractProjectsGoldenTest> {
                model("projects")
            }
            testClass<AbstractKdmTypesGoldenTest> {
                model("types")
            }
            // ElementFieldsGoldenTestGenerated compares one selected KdElement field per fixture.
            testClass<AbstractElementFieldsGoldenTest> {
                model("elementFields")
            }
            testClass<AbstractKdmKotlinSourceRoundTripTest> {
                model("roundTrip")
            }
            testClass<AbstractKdmJavaSourceRoundTripTest> {
                model("roundTrip", extension = "java")
            }
        }
    }
}
