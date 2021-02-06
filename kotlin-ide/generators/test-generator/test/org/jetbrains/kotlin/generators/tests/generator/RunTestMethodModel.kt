/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer

class RunTestMethodModel(
    private val targetBackend: TargetBackend,
    private val testMethodName: String,
    private val testRunnerMethodName: String,
    private val additionalRunnerArguments: List<String> = emptyList()
) : MethodModel {
    override val name = METHOD_NAME
    override val dataString: String? = null

    override fun generateSignature(p: Printer) {
        p.print("private void $name(String testDataFilePath) throws Exception")
    }

    override fun generateBody(p: Printer) {
        if (!isWithTargetBackend()) {
            p.println("KotlinTestUtils.$testRunnerMethodName(this::$testMethodName, this, testDataFilePath);")
        } else {
            val className = TargetBackend::class.java.simpleName
            val additionalArguments = if (additionalRunnerArguments.isNotEmpty())
                additionalRunnerArguments.joinToString(separator = ", ", prefix = ", ")
            else ""
            p.println("KotlinTestUtils.$testRunnerMethodName(this::$testMethodName, $className.$targetBackend, testDataFilePath$additionalArguments);")
        }
    }

    override fun imports(): Collection<Class<*>> {
        return super.imports() + if (isWithTargetBackend()) setOf(TargetBackend::class.java) else emptySet()
    }

    private fun isWithTargetBackend(): Boolean {
        return !(targetBackend == TargetBackend.ANY && additionalRunnerArguments.isEmpty() && testRunnerMethodName == METHOD_NAME)
    }

    companion object {
        const val METHOD_NAME = "runTest"
    }
}