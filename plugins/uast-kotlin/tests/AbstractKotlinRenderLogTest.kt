package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.test.common.RenderLogTestBase
import java.io.File

abstract class AbstractKotlinRenderLogTest : AbstractKotlinUastTest(), RenderLogTestBase {
    override fun getTestFile(testName: String, ext: String) =
            File(File(TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)
}