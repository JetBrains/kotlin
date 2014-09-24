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

package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.facade.exceptions.TranslationInternalException;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.junit.Assert;

public final class CharTest extends SingleFileTranslationTest {
    public CharTest() {
        super("char/");
    }

    public void testCharIsCheck() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCharConversions() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCharCompareToIntrinsic() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCharBinaryOperations() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCharUnaryOperations() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCharRanges() throws Exception {
        checkFooBoxIsOk();
    }

    public void testCharEquals() throws Exception {
        checkFooBoxIsOk();
    }
}
