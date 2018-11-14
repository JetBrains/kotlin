/*
 * Copyright in here? call @mirromutth on github
 */

package kotlin.jdk8.collections.test

import org.junit.Test
import kotlin.test.assertEquals

/**
 * [Enum] is regular when number of elements is less or equals than 64 on JDK
 */
class EnumSetTest {

    enum class MiniEnum {
        ONLY_ME // For test, only itself, we should not let it have any friends :-(
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
        E_61, E_62, E_63, E_64,
    }

    @Test fun allOf() {
        assertEquals(enumSetAllOf<MiniEnum>(), EnumSet.allOf(MiniEnum::class.java))
    }
}
