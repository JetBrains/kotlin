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
            testDataRoot = "libraries/tools/kotlin-documentation-model/analyzer/src/testData",
        ) {
            testClass<AbstractProjectsGoldenTest> {
                model("projects")
            }
            testClass<AbstractKdmTypesGoldenTest> {
                model("elementFields/types")
            }
            testClass<AbstractAnnotationsGoldenTest> {
                model("elementFields/annotations")
            }
            testClass<AbstractDeprecatedGoldenTest> {
                model("elementFields/deprecated")
            }
            testClass<AbstractSinceGoldenTest> {
                model("elementFields/since")
            }
            testClass<AbstractThrowsGoldenTest> {
                model("elementFields/throws")
            }
            testClass<AbstractOverridensGoldenTest> {
                model("elementFields/overridens")
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
