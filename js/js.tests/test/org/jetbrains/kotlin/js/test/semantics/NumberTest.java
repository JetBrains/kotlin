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

import org.jetbrains.kotlin.js.test.SingleFileTranslationTest;

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
        checkFooBoxIsOk();
    }

    // KT-2342 Type mismatch on Int division (JavaScript back-end)
    public void testKt2342() throws Exception {
        checkFooBoxIsOk();
    }

    // KT-5345 Type mismatch on Int / Float division
    public void testIntDivFloat() throws Exception {
        checkFooBoxIsOk();
    }

    public void testHexadecimalConstant() throws Exception {
        fooBoxTest();
    }

    public void testNumberCompareTo() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNumberIsCheck() throws Exception {
        checkFooBoxIsOk();
    }

    public void testNumberIncDec() throws Exception {
        checkFooBoxIsOk();
    }

    public void testConversionsWithoutTruncation() throws Exception {
        checkFooBoxIsOk();
    }

    public void testConversionsWithTruncation() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLongEqualsIntrinsic() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLongArray() throws Exception {
       checkFooBoxIsOk();
    }

    public void testLongCompareToIntrinsic() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLongBinaryOperations() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLongUnaryOperations() throws Exception {
        checkFooBoxIsOk();
    }

    public void testLongBitOperations() throws Exception {
        checkFooBoxIsOk();
    }
}
