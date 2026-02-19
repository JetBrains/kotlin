/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

class HashMapCompactTest {
    // KT-68298
    private fun MutableMap<IntWrapper, String?>.performOperations() {
        put(IntWrapper(0, -817396920), null)
        remove(IntWrapper(0, -817396920))
        put(IntWrapper(1, -916290166), null)
        put(IntWrapper(2, 118902548), null)
        put(IntWrapper(3, 1934947075), null)
        remove(IntWrapper(1, -916290166))
        remove(IntWrapper(2, 118902548))
        put(IntWrapper(4, 850555370), null)
        put(IntWrapper(5, -1166066571), null)
        remove(IntWrapper(3, 1934947075))
        put(IntWrapper(6, -2124713199), null)
        remove(IntWrapper(4, 850555370))
        put(IntWrapper(7, -89778600), null)
        remove(IntWrapper(5, -1166066571))
        put(IntWrapper(8, -1000638164), null)
        put(IntWrapper(9, 2034798242), null)
        put(IntWrapper(10, 229182579), null)
        put(IntWrapper(11, -1899382535), null)
        put(IntWrapper(12, 1289438843), null)
        remove(IntWrapper(6, -2124713199))
        put(IntWrapper(13, 1640889315), null)
        remove(IntWrapper(7, -89778600))
        put(IntWrapper(14, -1496052769), null)
        remove(IntWrapper(8, -1000638164))
        put(IntWrapper(15, -991527647), null)
        put(IntWrapper(16, -415676369), null)
        remove(IntWrapper(9, 2034798242))
        put(IntWrapper(17, 319443917), null)
        put(IntWrapper(18, -2051627686), null)
        put(IntWrapper(19, 1448073074), null)
        remove(IntWrapper(10, 229182579))
        put(IntWrapper(20, 1230791638), null)
        put(IntWrapper(21, 765663141), null)
        remove(IntWrapper(11, -1899382535))
        put(IntWrapper(22, 915046512), null)
        put(IntWrapper(23, -1622293756), null)
        put(IntWrapper(24, -1359721457), null)
        put(IntWrapper(25, 505867256), null)
        put(IntWrapper(26, 682358694), null)
        put(IntWrapper(27, 1100100510), null)
        put(IntWrapper(28, -1802524149), null)
        put(IntWrapper(29, 513102170), null)
        put(IntWrapper(30, 29546930), null)
        remove(IntWrapper(12, 1289438843))
        remove(IntWrapper(13, 1640889315))
        remove(IntWrapper(14, -1496052769))
        put(IntWrapper(31, -2033155453), null)
        put(IntWrapper(32, 1489147928), null)
        put(IntWrapper(33, -1535829019), null)
        remove(IntWrapper(15, -991527647))
        put(IntWrapper(34, -721039104), null)
        put(IntWrapper(35, -511394113), null)
        put(IntWrapper(36, 1853950035), null)
        put(IntWrapper(37, -1820540578), null)
        put(IntWrapper(38, -942256591), null)
        put(IntWrapper(39, 846308925), null)
        put(IntWrapper(40, 59966988), null)
        put(IntWrapper(41, 2141357595), null)
        remove(IntWrapper(16, -415676369))
        put(IntWrapper(42, 2133636554), null)
        remove(IntWrapper(17, 319443917))
        remove(IntWrapper(18, -2051627686))
        remove(IntWrapper(19, 1448073074))
        put(IntWrapper(43, 801662087), null)
        put(IntWrapper(44, 1332044799), null)
        put(IntWrapper(45, 1956322459), null)
        put(IntWrapper(46, -1576449428), null)
        put(IntWrapper(47, 1979534318), null)
        remove(IntWrapper(20, 1230791638))
        put(IntWrapper(48, 325807527), null)
        remove(IntWrapper(21, 765663141))
        remove(IntWrapper(22, 915046512))
        put(IntWrapper(49, -117048588), null)
        put(IntWrapper(50, -1034664289), null)
        put(IntWrapper(51, -331316176), null)
        put(IntWrapper(52, -1469321797), null)
    }

    @Test
    fun kt68298() {
        buildMap { performOperations() }
        HashMap<IntWrapper, String?>().performOperations()
        LinkedHashMap<IntWrapper, String?>().performOperations()
    }
}

private class IntWrapper(val value: Int, val hash: Int) {
    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean {
        if (other !is IntWrapper) {
            return false
        }
        check(value != other.value || hash == other.hash)  // if elements are equal hashCodes must be equal
        return value == other.value
    }
}
