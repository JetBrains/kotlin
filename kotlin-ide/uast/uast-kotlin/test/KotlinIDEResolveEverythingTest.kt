package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.UFile
import org.junit.Test


/**
 * [testComments] requires `KotlinReferenceContributor`
 * which is not set up in [AbstractKotlinUastTest]
 */
class KotlinIDEResolveEverythingTest : AbstractKotlinUastLightCodeInsightFixtureTest(), ResolveEverythingTestBase {

    override fun check(testName: String, file: UFile) {
        super.check(testName, file)
    }

    @Test
    fun testComments() = doTest("Comments")
}