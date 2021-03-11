package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.test.kotlin.org.jetbrains.uast.test.common.kotlin.CommentsTestBase
import java.io.File


abstract class AbstractKotlinCommentsTest : AbstractKotlinUastTest(), CommentsTestBase {

    private fun getTestFile(testName: String, ext: String) =
        File(File(TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    override fun getCommentsFile(testName: String) = getTestFile(testName, "comments.txt")
}