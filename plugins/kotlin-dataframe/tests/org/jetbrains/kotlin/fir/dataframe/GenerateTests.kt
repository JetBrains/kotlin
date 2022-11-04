package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
//        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
//            testClass<AbstractDiagnosticTest> {
//                model("diagnostics")
//            }
//
//            testClass<AbstractBoxTest> {
//                model("box")
//            }
//        }

        testGroup("tests-gen", "testData") {
            testClass<AbstractDataFrameDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractDataFrameBlackBoxCodegenTest> {
                model("box")
            }
//            testClass<AbstractDataFrameInterpretationTests> {
//                model("interpretation")
//            }
//
//            testClass<AbstractResearchTest> {
//                model("research")
//            }
        }
    }
}
