/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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