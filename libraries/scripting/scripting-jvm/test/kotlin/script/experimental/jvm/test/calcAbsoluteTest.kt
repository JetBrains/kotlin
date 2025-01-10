/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.test

import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvm.util.calcAbsolute
import kotlin.script.experimental.jvm.util.toSourceCodePosition

class CalcAbsoluteTest {

    @Test
    fun testMultiline() {
        val source = """
            abcdefg
            hij
            klmnopqrst
            covid19
            uv
        """.trimIndent().toSource()

        val pos = SourceCode.Position(4, 6)

        val absPos = pos.calcAbsolute(source)

        Assert.assertEquals('1', source.text[absPos])
        Assert.assertEquals(17, 17.toSourceCodePosition(source).calcAbsolute(source))
    }

    fun String.toSource() = SourceCodeTestImpl(this)

    class SourceCodeTestImpl(override val text: String) : SourceCode {
        override val name: String? = null
        override val locationId: String? = null
    }
}