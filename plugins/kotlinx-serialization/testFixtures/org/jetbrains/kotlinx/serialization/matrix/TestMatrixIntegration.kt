/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.matrix

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlinx.serialization.matrix.impl.CombinationContextImpl
import org.jetbrains.kotlinx.serialization.configureForKotlinxSerialization
import java.io.File

internal fun TestGroup.testMatrix(casesBlock: TestCaseContext.() -> Unit) {
    val relativeRootPath = "matrix"
    val dir = File("$testDataRoot/$relativeRootPath")
    dir.mkdirs()
    dir.deleteDirectoryContents()

    val caseContext = TestCaseContext()
    caseContext.casesBlock()

    caseContext.testCases.forEach { (caseName, block) ->
        val combinationContext = CombinationContextImpl()
        combinationContext.block()
        combinationContext.generateInto(dir.resolve("$caseName.kt"))
    }

    testClass<AbstractTestMatrix> {
        model(relativeRootPath)
    }
    testClass<AbstractFirTestMatrix> {
        model(relativeRootPath)
    }
}

internal class TestCaseContext {
    val testCases: MutableMap<String, CombinationContext.() -> Unit> = mutableMapOf()

    fun add(name: String, block: CombinationContext.() -> Unit) {
        testCases[name] = block
    }
}

internal open class AbstractTestMatrix : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization()
    }
}

internal open class AbstractFirTestMatrix : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForKotlinxSerialization()
    }
}

private fun CombinationContext.generateInto(file: File) {
    file.writer().use {
        generate(it, "TestMatrixIntegration.kt")
    }
}
