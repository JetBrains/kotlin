@file:kotlin.jvm.JvmVersion
package test.text

import kotlin.test.*
import org.junit.Test

class CharJVMTest {

    @Test fun getCategory() {
        assertEquals(CharCategory.DECIMAL_DIGIT_NUMBER, '7'.category)
        assertEquals(CharCategory.CURRENCY_SYMBOL, '$'.category)
        assertEquals(CharCategory.LOWERCASE_LETTER, 'a'.category)
        assertEquals(CharCategory.UPPERCASE_LETTER, 'Õ'.category)

        assertTrue(',' in CharCategory.OTHER_PUNCTUATION)
    }

}