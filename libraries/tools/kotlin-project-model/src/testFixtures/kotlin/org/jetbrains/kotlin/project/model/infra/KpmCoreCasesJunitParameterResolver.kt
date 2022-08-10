/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.jetbrains.kotlin.project.model.coreCases.KpmTestCaseDescriptor
import org.jetbrains.kotlin.project.model.coreCases.instantiateCase
import org.junit.jupiter.api.extension.*
import java.lang.reflect.Method

class KpmCoreCasesJunitParameterResolver : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type == KpmTestCaseDescriptor::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        require(parameterContext.parameter.type == KpmTestCaseDescriptor::class.java)
        val kpmCaseName = extensionContext.requiredTestMethod.kpmCaseName
        val caseDescriptor = KpmTestCaseDescriptor.allCaseDescriptorsByNames[kpmCaseName]
        requireNotNull(caseDescriptor) {
            "Can't find KpmCoreCase for name $kpmCaseName while " +
                    "\n injecting parameter ${parameterContext.parameter} into \n" +
                    "${extensionContext.requiredTestMethod}"
        }
        return caseDescriptor.instantiateCase()
    }
}

private val Method.kpmCaseName: String
    get() {
        val testCaseName = this.name.substringAfter("test")
        require(testCaseName in KpmTestCaseDescriptor.allCasesNames) {
            "Can't find matching core case for name ${testCaseName}.\n" +
                    "Please check that the test method follow pattern 'test\$caseName', \n" +
                    "where '\$caseName' is a name as declared in 'o.j.k.project.model.coreCases'-package\n" +
                    "\n" +
                    "Known cases:\n" +
                    "${KpmTestCaseDescriptor.allCasesNames}"
        }

        return testCaseName
    }
