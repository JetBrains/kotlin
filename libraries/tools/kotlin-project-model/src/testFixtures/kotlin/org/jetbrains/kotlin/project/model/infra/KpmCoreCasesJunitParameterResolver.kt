/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.jetbrains.kotlin.project.model.coreCases.KpmTestCaseWrapper
import org.junit.jupiter.api.extension.*
import java.lang.reflect.Method

class KpmCoreCasesJunitParameterResolver : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type == KpmTestCase::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        require(parameterContext.parameter.type == KpmTestCase::class.java)
        val kpmCaseName = extensionContext.requiredTestMethod.kpmCaseName
        val case = KpmTestCaseWrapper.allCasesByNames[kpmCaseName]
        requireNotNull(case) {
            "Can't find KpmCoreCase for name $kpmCaseName while " +
                    "\n injecting parameter ${parameterContext.parameter} into \n" +
                    "${extensionContext.requiredTestMethod}"
        }
        return case.case
    }
}

private val Method.kpmCaseName: String
    get() {
        val testCaseName = this.name.substringAfter("test")
        require(testCaseName in KpmTestCaseWrapper.allCasesNames) {
            "Can't find matching core case for name ${testCaseName}.\n" +
                    "Please check that the test method follow pattern 'test\$caseName', \n" +
                    "where '\$caseName' is a name as declared in 'o.j.k.project.model.coreCases'-package\n" +
                    "\n" +
                    "Known cases:\n" +
                    "${KpmTestCaseWrapper.allCasesNames}"
        }

        return testCaseName
    }
