package org.jetbrains.uast.test.kotlin

import org.junit.Test


class KotlinUastCommentOwnersTest : AbstractKotlinCommentsTest()  {

    @Test
    fun testCommentOwners() = doTest("CommentOwners")

    @Test
    fun testComments() = doTest("Comments")
}