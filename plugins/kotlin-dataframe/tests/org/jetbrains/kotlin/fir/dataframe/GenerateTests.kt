package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup("tests-gen", "testData") {
            testClass<AbstractDataFrameDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractDataFrameBlackBoxCodegenTest> {
                model("box")
            }
        }
    }
}
