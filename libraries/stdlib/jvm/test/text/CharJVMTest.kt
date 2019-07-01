/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class CharJVMTest {

    @Test fun getCategory() {
        assertEquals(CharCategory.DECIMAL_DIGIT_NUMBER, '7'.category)
        assertEquals(CharCategory.CURRENCY_SYMBOL, '$'.category)
        assertEquals(CharCategory.LOWERCASE_LETTER, 'a'.category)
        assertEquals(CharCategory.UPPERCASE_LETTER, 'Õ'.category)

        assertTrue(',' in CharCategory.OTHER_PUNCTUATION)
    }

}