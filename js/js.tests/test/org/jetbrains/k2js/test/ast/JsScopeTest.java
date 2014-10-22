/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.ast;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsScope;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

public final class JsScopeTest extends TestCase {
    private JsScope scope;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        scope = new JsScope("Test scope") {};
    }

    public void testDeclareFreshName() throws Exception {
        declareFreshNameAndAssertEquals("a", "a");
        declareFreshNameAndAssertEquals("a", "a_0");
        declareFreshNameAndAssertEquals("a", "a_1");

        declareFreshNameAndAssertEquals("a_1", "a_2");
        declareFreshNameAndAssertEquals("a_3", "a_3");

        declareFreshNameAndAssertEquals("a_1_1", "a_1_1");
        declareFreshNameAndAssertEquals("a_1_1", "a_1_2");

        declareFreshNameAndAssertEquals("tmp$0", "tmp$0");
        declareFreshNameAndAssertEquals("tmp$0", "tmp$1");

        declareFreshNameAndAssertEquals("a0", "a0");
        declareFreshNameAndAssertEquals("a0", "a0_0");
        declareFreshNameAndAssertEquals("a0_0", "a0_1");
    }

    private void declareFreshNameAndAssertEquals(@NotNull String suggested, @NotNull String expected) {
        JsName actual = scope.declareFreshName(suggested);
        assertEquals(expected, actual.getIdent());
    }
}
