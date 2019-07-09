package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.test.common.kotlin.ValuesTestBase
import java.io.File

abstract class AbstractKotlinValuesTest : AbstractKotlinUastTest(), ValuesTestBase {

    private fun getTestFile(testName: String, ext: String) =
            File(File(AbstractKotlinUastTest.TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    override fun getValuesFile(testName: String) = getTestFile(testName, "values.txt")
}