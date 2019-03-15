/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.testFramework.UsefulTestCase
import org.junit.Assert


class TemplateTokenSequenceTest:UsefulTestCase() {
    fun doTest(input: String, expected:String) {
        val output = createTemplateSequenceTokenString(input)
        Assert.assertEquals("Unexpected template sequence output for $input: " , expected, output)
    }

    fun `test multiple template tokens`(){
        doTest("literal \${a.length} literal \${b.length}", "LITERAL_CHUNK(literal )ENTRY_CHUNK(\${a.length})LITERAL_CHUNK( literal )ENTRY_CHUNK(\${b.length})")
    }

    fun `test broken entry`(){
        doTest("literal \${a.lengt \n literal", "LITERAL_CHUNK(literal )LITERAL_CHUNK(\${a.lengt )NEW_LINE()LITERAL_CHUNK( literal)")
    }

    fun `test multiple short entries`(){
        doTest("literal \$a literal \$a", "LITERAL_CHUNK(literal )ENTRY_CHUNK(\$a)LITERAL_CHUNK( literal )ENTRY_CHUNK(\$a)")
    }

    fun `test leading new lines`(){
        doTest("\n\nliteral", "NEW_LINE()NEW_LINE()LITERAL_CHUNK(literal)")
    }

    //last empty line is skipped
    fun `test trailing new lines`(){
        doTest("literal\n\n", "LITERAL_CHUNK(literal)NEW_LINE()NEW_LINE()")

    }

    fun `test multi new lines`(){
        doTest("literal\n\nliteral", "LITERAL_CHUNK(literal)NEW_LINE()NEW_LINE()LITERAL_CHUNK(literal)")

    }
}