/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import junit.framework.Assert;
import org.jetbrains.k2js.facade.exceptions.TranslationInternalException;
import org.jetbrains.k2js.test.SingleFileTranslationTest;

public final class NumberTest extends SingleFileTranslationTest {
    public NumberTest() {
        super("number/");
    }

    public void testIntConversions() throws Exception {
        fooBoxTest();
    }

    public void testDoubleConversions() throws Exception {
        fooBoxTest();
    }

    public void testNumberConversions() throws Exception {
        fooBoxTest();
    }

    public void testByteAndShortConversions() throws Exception {
        fooBoxTest();
    }

    public void testDivision() throws Exception {
        fooBoxIsValue("SUCCESS");
    }

    public void testHexademicalConstant() throws Exception {
        try {
            fooBoxTest();
        } catch (TranslationInternalException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof IllegalStateException);
            Assert.assertTrue(cause.getMessage().startsWith("Unsupported long constant "));
        }
    }
}
