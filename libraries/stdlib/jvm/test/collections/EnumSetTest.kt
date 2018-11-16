/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

class EnumSetTest {

    enum class MiniEnum {
        ONLY_ME // For test, only itself, we should not let it have any friends :-(
    }

    enum class AnotherEnum {
        ONLY_ME // HIM!
    }

    enum class SmallEnum {
        ZEROTH, FIRST, SECOND, THIRD, FOURTH,
        FIFTH, SIXTH, SEVENTH, EIGHTH, NINTH, // Nine is the maximum value of the number in "Tao"
    }

    enum class RegularEnum {
        E_1, E_2, E_3, E_4, E_5, E_6, E_7, E_8, E_9, E_10,
        E_11, E_12, E_13, E_14, E_15, E_16, E_17, E_18, E_19, E_20,
        E_21, E_22, E_23, E_24, E_25, E_26, E_27, E_28, E_29, E_30,
        E_31, E_32, E_33, E_34, E_35, E_36, E_37, E_38, E_39, E_40,
        E_41, E_42, E_43, E_44, E_45, E_46, E_47, E_48, E_49, E_50,
        E_51, E_52, E_53, E_54, E_55, E_56, E_57, E_58, E_59, E_60,
        E_61, E_62, E_63, E_64, // `Enum` is regular when number of elements is less or equals than 64 on JDK
    }

    enum class JumboEnum {
        E_1, E_2, E_3, E_4, E_5, E_6, E_7, E_8, E_9, E_10,
        E_11, E_12, E_13, E_14, E_15, E_16, E_17, E_18, E_19, E_20,
        E_21, E_22, E_23, E_24, E_25, E_26, E_27, E_28, E_29, E_30,
        E_31, E_32, E_33, E_34, E_35, E_36, E_37, E_38, E_39, E_40,
        E_41, E_42, E_43, E_44, E_45, E_46, E_47, E_48, E_49, E_50,
        E_51, E_52, E_53, E_54, E_55, E_56, E_57, E_58, E_59, E_60,
        E_61, E_62, E_63, E_64, E_65,
    }

    @Test fun filledEnumSet() {
        assertEquals(enumSetAllOf<MiniEnum>(), EnumSet.allOf(MiniEnum::class.java))
        assertEquals(enumSetAllOf<SmallEnum>(), EnumSet.allOf(SmallEnum::class.java))
        assertEquals(enumSetAllOf<RegularEnum>(), EnumSet.allOf(RegularEnum::class.java))
        assertEquals(enumSetAllOf<JumboEnum>(), EnumSet.allOf(JumboEnum::class.java))
        assertNotEquals<Any>(enumSetAllOf<MiniEnum>(), EnumSet.allOf(AnotherEnum::class.java))
        assertNotEquals<Any>(enumSetAllOf<MiniEnum>(), EnumSet.allOf(SmallEnum::class.java))
        assertNotEquals<Any>(enumSetAllOf<MiniEnum>(), EnumSet.allOf(RegularEnum::class.java))
        assertNotEquals<Any>(enumSetAllOf<MiniEnum>(), EnumSet.allOf(JumboEnum::class.java))
    }

    @Test fun emptyEnumSet() {
        // Empty set always equals than another empty set
        assertEquals(enumSetOf<MiniEnum>(), EnumSet.noneOf(MiniEnum::class.java))
        assertEquals(enumSetOf<SmallEnum>(), EnumSet.noneOf(SmallEnum::class.java))
        assertEquals(enumSetOf<RegularEnum>(), EnumSet.noneOf(RegularEnum::class.java))
        assertEquals(enumSetOf<JumboEnum>(), EnumSet.noneOf(JumboEnum::class.java))
        assertEquals<Any>(enumSetOf<MiniEnum>(), EnumSet.noneOf(AnotherEnum::class.java))
        assertEquals<Any>(enumSetOf<MiniEnum>(), EnumSet.noneOf(SmallEnum::class.java))
        assertEquals<Any>(enumSetOf<MiniEnum>(), EnumSet.noneOf(RegularEnum::class.java))
        assertEquals<Any>(enumSetOf<MiniEnum>(), EnumSet.noneOf(JumboEnum::class.java))
    }

    @Test fun singleEnumSet() {
        assertEquals(enumSetOf(MiniEnum.ONLY_ME), EnumSet.of(MiniEnum.ONLY_ME))
        assertEquals(enumSetOf(SmallEnum.SEVENTH), EnumSet.of(SmallEnum.SEVENTH))
        assertEquals(enumSetOf(RegularEnum.E_16), EnumSet.of(RegularEnum.E_16))
        assertEquals(enumSetOf(JumboEnum.E_65), EnumSet.of(JumboEnum.E_65))
        assertNotEquals<Any>(enumSetOf(MiniEnum.ONLY_ME), EnumSet.of(AnotherEnum.ONLY_ME))
        assertNotEquals<Any>(enumSetOf(MiniEnum.ONLY_ME), EnumSet.of(SmallEnum.FIRST))
        assertNotEquals<Any>(enumSetOf(MiniEnum.ONLY_ME), EnumSet.of(JumboEnum.E_1))
    }

    @Test fun doubleEnumSet() {
        assertNotEquals(enumSetOf(SmallEnum.EIGHTH, SmallEnum.FIFTH), enumSetOf(SmallEnum.EIGHTH, SmallEnum.FOURTH))
        assertEquals(enumSetOf(SmallEnum.EIGHTH, SmallEnum.FIFTH), enumSetOf(SmallEnum.FIFTH, SmallEnum.EIGHTH))
        assertNotEquals(enumSetOf(RegularEnum.E_10, RegularEnum.E_1), enumSetOf(RegularEnum.E_10, RegularEnum.E_5))
        assertEquals(enumSetOf(RegularEnum.E_10, RegularEnum.E_1), enumSetOf(RegularEnum.E_1, RegularEnum.E_10))
        assertNotEquals(enumSetOf(JumboEnum.E_10, JumboEnum.E_1), enumSetOf(JumboEnum.E_10, JumboEnum.E_5))
        assertEquals(enumSetOf(JumboEnum.E_10, JumboEnum.E_1), enumSetOf(JumboEnum.E_1, JumboEnum.E_10))
        assertTrue(enumSetOf(RegularEnum.E_10, RegularEnum.E_1).containsAll(enumSetOf(RegularEnum.E_1)))
        assertFalse(enumSetOf(RegularEnum.E_10, RegularEnum.E_1).containsAll(enumSetOf(RegularEnum.E_5)))
        assertTrue(enumSetOf(JumboEnum.E_10, JumboEnum.E_1).containsAll(enumSetOf(JumboEnum.E_1)))
        assertFalse(enumSetOf(JumboEnum.E_10, JumboEnum.E_1).containsAll(enumSetOf(JumboEnum.E_5)))
    }
}
