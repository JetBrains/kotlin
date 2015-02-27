/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.semantics;

import org.jetbrains.kotlin.js.test.SingleFileTranslationWithDirectivesTest;

public class LabelTest extends SingleFileTranslationWithDirectivesTest {
    public LabelTest() {
        super("labels/");
    }

    public void testSimpleLabel() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSiblingLabels() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNestedLabels() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSimpleLabelInlined() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSiblingLabelsInlined() throws Exception {
        checkFooBoxIsOk();
    }

    public void testSiblingLabelsInlinedClashing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNestedLabelsInlinedClashing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLabelWithVariableClashing() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNestedLabelsInlinedClashingAtFunctionsWithClosure() throws Exception {
        checkFooBoxIsOk();
    }
}
