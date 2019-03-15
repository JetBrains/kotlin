package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.common.kotlin.ResolveTestBase
import org.junit.Test

class KotlinUastResolveTest : AbstractKotlinUastTest(), ResolveTestBase {
    override fun check(testName: String, file: UFile) {
        super.check(testName, file)
    }

    @Test fun testMethodReference() = doTest("MethodReference")
}
