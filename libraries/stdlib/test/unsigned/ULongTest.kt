/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.unsigned

import kotlin.math.*
import kotlin.random.*
import kotlin.test.*

class ULongTest {

    private fun identity(u: ULong): ULong =
        (u.toLong() + 0).toULong()

    val zero = 0uL
    val one = 1uL
    val max = ULong.MAX_VALUE

    @Test
    fun equality() {

        fun testEqual(uv1: ULong, uv2: ULong) {
            assertEquals(uv1, uv2, "Boxed values should be equal")
            assertTrue(uv1.equals(uv2), "Boxed values should be equal: $uv1, $uv2")
            assertTrue(uv1 == uv2, "Values should be equal: $uv1, $uv2")
            assertEquals(uv1.hashCode(), uv2.hashCode())
            assertEquals((uv1 as Any).hashCode(), (uv2 as Any).hashCode())
            assertEquals(uv1.toString(), uv2.toString())
            assertEquals((uv1 as Any).toString(), (uv2 as Any).toString())
        }

        testEqual(one, identity(one))
        testEqual(max, identity(max))

        fun testNotEqual(uv1: ULong, uv2: ULong) {
            assertNotEquals(uv1, uv2, "Boxed values should be equal")
            assertTrue(uv1 != uv2, "Values should be not equal: $uv1, $uv2")
            assertNotEquals(uv1.toString(), uv2.toString())
            assertNotEquals((uv1 as Any).toString(), (uv2 as Any).toString())
        }

        testNotEqual(one, zero)
        testNotEqual(max, zero)
    }

    @Test
    fun convertToString() {
        fun testToString(expected: String, u: ULong) {
            assertEquals(expected, u.toString())
            assertEquals(expected, (u as Any).toString(), "Boxed toString")
            assertEquals(expected, "$u", "String template")
        }

        repeat(100) {
            val v = Random.nextLong() ushr 1
            testToString(v.toString(), v.toULong())
        }

        repeat(100) {
            val v = Random.nextLong(8446744073709551615L + 1)
            testToString("1${v.toString().padStart(19, '0')}", (5000000000000000000.toULong() * 2.toULong() + v.toULong()))
        }

        testToString("18446744073709551615", ULong.MAX_VALUE)
    }

    @Test
    fun operations() {
        assertEquals(9223372036854775808u, Long.MAX_VALUE.toULong() + identity(1u))
        assertEquals(11u, ULong.MAX_VALUE + identity(12u))
        assertEquals(ULong.MAX_VALUE - 99u, 45u - identity(145u))

        assertEquals(ULong.MAX_VALUE - 1u, Long.MAX_VALUE.toULong() * identity(2u))
        assertEquals(9223372036854775805u, Long.MAX_VALUE.toULong() * identity(3u))

        testMulDivRem(125u, 3u, 41u, 2u)
        testMulDivRem(210u, 5u, 42u, 0u)
        testMulDivRem(ULong.MAX_VALUE, 65536uL * 65536u, 4294967295u, 4294967295u)
        testMulDivRem(ULong.MAX_VALUE - 1u, ULong.MAX_VALUE, 0u, ULong.MAX_VALUE - 1u)
        testMulDivRem(ULong.MAX_VALUE, ULong.MAX_VALUE - 1u, 1u, 1u)
        testMulDivRem(ULong.MAX_VALUE, Long.MAX_VALUE.toULong(), 2u, 1u)
    }


    private fun testMulDivRem(number: ULong, divisor: ULong, div: ULong, rem: ULong) {
        assertEquals(div, number / divisor)
        assertEquals(rem, number % divisor)
        assertEquals(div, number.floorDiv(divisor))
        assertEquals(rem, number.mod(divisor))

        assertEquals(number, div * divisor + rem)
        assertTrue(rem < divisor)
        assertTrue(div < number)
    }

    @Test
    fun divRem() = repeat(1000) {
        val number = Random.nextULong()
        val divisor = Random.nextULong(until = ULong.MAX_VALUE) + 1u
        testMulDivRem(number, divisor, number / divisor, number % divisor)
    }

    @Test
    fun comparisons() {
        fun <T> compare(op1: Comparable<T>, op2: T) = op1.compareTo(op2)

        fun testComparison(uv1: ULong, uv2: ULong, expected: Int) {
            val desc = "${uv1.toString()}, ${uv2.toString()}"
            assertEquals(expected, uv1.compareTo(uv2).sign, "compareTo: $desc")
            assertEquals(expected, (uv1 as Comparable<ULong>).compareTo(uv2).sign, "Comparable.compareTo: $desc")
            assertEquals(expected, compare(uv1, uv2).sign, "Generic compareTo: $desc")

            assertEquals(expected < 0, uv1 < uv2)
            assertEquals(expected <= 0, uv1 <= uv2)
            assertEquals(expected > 0, uv1 > uv2)
            assertEquals(expected >= 0, uv1 >= uv2)
        }

        fun testEquals(uv1: ULong, uv2: ULong) = testComparison(uv1, uv2, 0)
        fun testCompare(uv1: ULong, uv2: ULong, expected12: Int) {
            testComparison(uv1, uv2, expected12)
            testComparison(uv2, uv1, -expected12)
        }

        testEquals(one, identity(one))
        testEquals(max, identity(max))

        testCompare(zero, one, -1)
        testCompare(Long.MAX_VALUE.toULong(), zero, 1)

        testCompare(zero, ULong.MAX_VALUE, -1)
        testCompare((Long.MAX_VALUE).toULong() + one, ULong.MAX_VALUE, -1)
    }


    @Test
    fun convertToFloat() {
        fun testEquals(v1: Float, v2: ULong) = assertEquals(v1, v2.toFloat())

        testEquals(0.0f, zero)
        testEquals(1.0f, one)

        testEquals(2.0f.pow(ULong.SIZE_BITS) - 1, max)
        testEquals(2.0f * Long.MAX_VALUE + 1, max)

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            testEquals(long.toFloat(), long.toULong())
        }

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            val float = Long.MAX_VALUE.toFloat() + long.toFloat()    // We lose accuracy here, hence `eps` is used.
            val ulong = Long.MAX_VALUE.toULong() + long.toULong()

            // TODO: replace with ulp comparison when available on Float
            val eps = 1e+13
            assertTrue(abs(float - ulong.toFloat()) < eps)
        }
    }

    @Test
    fun convertToDouble() {
        fun testEquals(v1: Double, v2: ULong) = assertEquals(v1, v2.toDouble())

        testEquals(0.0, zero)
        testEquals(1.0, one)

        testEquals(2.0.pow(ULong.SIZE_BITS) - 1, max)
        testEquals(2.0 * Long.MAX_VALUE + 1, max)

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            testEquals(long.toDouble(), long.toULong())
        }

        repeat(100) {
            val long = Random.nextLong(from = 0, until = Long.MAX_VALUE)
            val value = Long.MAX_VALUE.toULong() + long.toULong()
            val expected = Long.MAX_VALUE.toDouble() + long.toDouble()    // Should be accurate to one ulp
            val actual = value.toDouble()
            val diff = abs(expected - value.toDouble())

            assertTrue(diff <= actual.ulp, "$actual should be within one ulp (${actual.ulp}) from the expected $expected")
        }

        fun testRounding(from: ULong, count: ULong) {
            for (x in from..(from + count)) {
                val double = x.toDouble()
                val v = double.toULong()
                val down = double.nextDown().toULong()
                val up = double.nextUp().toULong()

                assertTrue(down <= x && down <= v)
                assertTrue(up >= x && up >= v)

                if (v > x) {
                    assertTrue(v - x <= x - down, "Expected $x being closer to $v than to $down")
                } else {
                    assertTrue(x - v <= up - x, "Expected $x being closer to $v than to $up")
                }
            }
        }

        testRounding(0u, 100u)
        testRounding(Long.MAX_VALUE.toULong() - 520u, 100u)
        testRounding(ULong.MAX_VALUE - 10000u, 10000u)
    }

    @Test
    fun convertDoubleToULong() {
        fun testEquals(v1: Double, v2: ULong) = assertEquals(v1.toULong(), v2)

        testEquals(0.0, zero)
        testEquals(-1.0, zero)

        testEquals(-2_000_000_000_000.0, zero)
        testEquals(-(2.0.pow(ULong.SIZE_BITS + 5)), zero)
        testEquals(Double.MIN_VALUE, zero)
        testEquals(Double.NEGATIVE_INFINITY, zero)
        testEquals(Double.NaN, zero)

        testEquals(1.0, one)

        testEquals(2_000_000_000_000_000_000_000.0, max)
        testEquals(2.0.pow(ULong.SIZE_BITS), max)
        testEquals(2.0.pow(ULong.SIZE_BITS + 5), max)
        testEquals(Double.MAX_VALUE, max)
        testEquals(Double.POSITIVE_INFINITY, max)

        repeat(100) {
            val v = -Random.nextDouble(until = 2.0.pow(ULong.SIZE_BITS + 8))
            testEquals(v, zero)
        }

        repeat(100) {
            val v = Random.nextDouble(from = max.toDouble(), until = 2.0.pow(ULong.SIZE_BITS + 8))
            testEquals(v, max)
        }

        repeat(100) {
            val v = Random.nextDouble() * Long.MAX_VALUE
            testEquals(v, v.toLong().toULong())
        }

        repeat(100) {
            val d = 2.0.pow(63) * (1 + Random.nextDouble())
            val expected = specialDoubleToULong(d)
            val actual = d.toULong()

            assertEquals(expected, actual, "Expected bit pattern: ${expected.toString(2)}, actual bit pattern: ${actual.toString(2)}")
        }

        fun testTrailingBits(v: Double, count: Int) {
            val mask = (1uL shl count) - 1uL
            assertEquals(0uL, v.toULong() and mask)
        }

        var withTrailingZeros = 2.0.pow(64)
        repeat(10) {
            withTrailingZeros = withTrailingZeros.nextDown()
            testTrailingBits(withTrailingZeros, 11)
        }

        withTrailingZeros = 2.0.pow(63)
        repeat(10) {
            testTrailingBits(withTrailingZeros, 11)
            withTrailingZeros = withTrailingZeros.nextUp()
        }

        repeat(100) {
            val msb = Random.nextInt(53, 64)
            val v = 2.0.pow(msb) * (1.0 + Random.nextDouble())
            testTrailingBits(v, msb - 52)
        }
    }

    /** Creates an ULong value directly from mantissa bits of Double that is in range [2^63, 2^64). */
    private fun specialDoubleToULong(v: Double): ULong {
        require(v >= 2.0.pow(63))
        require(v < 2.0.pow(64))
        val bits = v.toBits().toULong()
        return (1uL shl 63) + ((bits and (1uL shl 52) - 1u) shl 11)
    }

    /* Tests ported from Scala.js:
     * https://github.com/scala-js/scala-js/blob/v1.22.0/test-suite/shared/src/test/scala/org/scalajs/testsuite/javalib/lang/LongTest.scala#L706-L1071
     */

    @Test
    fun divideMore() {
        fun test(x: Long, y: Long, result: Long) {
            assertEquals(result, (x.toULong() / y.toULong()).toLong())
        }

        test(-9223372034182170740L, 53886L, 171164533265177L)
        test(-9223372036854775807L, 1L, -9223372036854775807L)
        test(1L, 1L, 1L)
        test(-9223372028033273801L, 1093832863L, 8432158474L)
        test(3L, 1L, 3L)
        test(-9152576797767832099L, 1831882805942L, 5073559L)
        test(143L, 1L, 143L)
        test(-9223372036785876966L, 67L, 137662269207816039L)
        test(409670828687897L, 41239724459L, 9933L)
        test(-9223372036854571433L, 405L, 22773758115691309L)
        test(-9223372036786133323L, 684L, 13484462042285699L)
        test(1011212285034L, 1L, 1011212285034L)
        test(-9223372036854052446L, 14386L, 641135272963679L)
        test(-9223372036854775794L, 1L, -9223372036854775794L)
        test(-9223372036854769295L, 29L, 318047311615682149L)
        test(-9223372026307494597L, 58L, 159023655989690638L)
        test(329537042584197319L, 3386229L, 97316821332L)
        test(-9223372036853925905L, 2789311L, 3306684710616L)
        test(184910967393761L, 612461458L, 301914L)
        test(-9220748194206666929L, 30344662L, 304040159666L)
        test(2710297051L, 488660775L, 5L)
        test(-9223372036854444961L, 106367L, 86712721397191L)
        test(-9223372036644696595L, 125978L, 73214148796336L)
        test(0L, 16L, 0L)
        test(34727385263708L, 1726L, 20120153686L)
        test(24538L, 214L, 114L)
        test(3441692738180855L, 2922016232L, 1177848L)
        test(-9223372036854573063L, 2065L, 4466523988791757L)
        test(-9223372036854738811L, 4532L, 2035165939288352L)
        test(25392410921644L, 27738L, 915437699L)
        test(-9223371993563946637L, 371818L, 24806147309021L)
        test(226905L, 12L, 18908L)
        test(-9223187071501586227L, 5436611L, 1696563723652L)
        test(62324594094L, 62L, 1005235388L)
        test(-9147191206118290885L, 1934532429910965L, 4807L)
        test(-9172283274772171204L, 556443731116414L, 16667L)
        test(-9223372036854775477L, 106L, 87012943743912982L)
        test(60485531945L, 12L, 5040460995L)
        test(-9223372036854598147L, 2L, 4611686018427476734L)
        test(31834147648L, 14719L, 2162792L)
        test(58014L, 1L, 58014L)
        test(-9223372036854775733L, 6L, 1537228672809129313L)
        test(3L, 1L, 3L)
        test(-7905579639447511885L, 747885734L, 14094618943L)
        test(14346885725L, 29005921L, 494L)
        test(13672312178L, 6L, 2278718696L)
        test(-9223371657435410050L, 5901282L, 1562943851229L)
        test(-9223372036389851141L, 1055121L, 8741530153716L)
        test(1149586100416530720L, 564L, 2038273227688884L)
        test(1L, 1L, 1L)
        test(-9223372036854385180L, 8663L, 1064685678962849L)
        test(89731974104L, 1173247030L, 76L)
        test(385847542338318L, 7846L, 49177611819L)
        test(-9223372026066135207L, 480301980L, 19203277170L)
    }

    @Test
    fun remainderMore() {
        fun test(x: Long, y: Long, result: Long) {
            assertEquals(result, (x.toULong() % y.toULong()).toLong())
        }

        test(97062081516L, 772L, 668L)
        test(-9223372036854775472L, 49L, 43L)
        test(-9223372036854775756L, 17L, 10L)
        test(270261062411L, 19L, 13L)
        test(654151050L, 1293L, 369L)
        test(252077906700L, 5147561526L, 4994953452L)
        test(131302394690918280L, 45672263L, 32871850L)
        test(6861002361535L, 5306169939241L, 1554832422294L)
        test(-9221700602589689139L, 1L, 0L)
        test(-9223355302048330857L, 616921560624L, 570573045175L)
        test(7L, 387L, 7L)
        test(57025455556036L, 5340L, 2296L)
        test(118837327813611L, 30L, 21L)
        test(87L, 909L, 87L)
        test(272013095293278842L, 29839452324246875L, 3458024375056967L)
        test(1434L, 3L, 0L)
        test(22980297L, 475870L, 138537L)
        test(10410504L, 56605L, 51789L)
        test(-9223365332599000086L, 37509837810312L, 9702257313226L)
        test(-9223372036851205117L, 120L, 99L)
        test(-9223372036854772689L, 62L, 27L)
        test(-9223369370621240354L, 8761020421L, 6409028421L)
        test(-9223371153085904549L, 942378L, 421655L)
        test(1357532983495L, 28618L, 5701L)
        test(2578981576162L, 2884L, 334L)
        test(-9216746775620579770L, 57399743689L, 39826797750L)
        test(-9223372036854775593L, 2L, 1L)
        test(35146338041774819L, 479358464104950L, 153170162113469L)
        test(28855L, 6436L, 3111L)
        test(1115645622608748416L, 2L, 0L)
        test(39928234567786375L, 225464372977360L, 21040550793655L)
        test(198628052L, 954693145L, 198628052L)
        test(3022077378019577L, 7545L, 6647L)
        test(Long.MIN_VALUE, 1L, 0L)
        test(16L, 93L, 16L)
        test(-9223372036854775804L, 12L, 0L)
        test(-9223372036854775718L, 1L, 0L)
        test(433297497717789L, 793815867L, 251058642L)
        test(55412933435948L, 13494776387L, 3381590926L)
        test(1652351527406382L, 20225334L, 8793636L)
        test(7L, 364L, 7L)
        test(1125379509822519L, 41077089453701L, 16298094572592L)
        test(7677989811350377624L, 513072419162588473L, 494975943074139002L)
        test(525899929L, 16536035226L, 525899929L)
        test(2353013018L, 53739171823L, 2353013018L)
        test(3L, 18L, 3L)
        test(-9223372036853809229L, 11838317L, 6492461L)
        test(-9223371335502904413L, 247005757438L, 158332129281L)
        test(-9209220612812195231L, 2L, 1L)
        test(-9223372036684983395L, 1017L, 662L)
        test(-9223372036625677948L, 1L, 0L)
        test(39608677L, 5L, 2L)
        test(-9223372022281433992L, 16359L, 15357L)
        test(54651001988172L, 4463729541L, 1561217709L)
        test(-9223372036846154797L, 11L, 0L)
        test(-9212519596031121696L, 1L, 0L)
        test(0L, 2L, 0L)
    }

    @Test
    fun toStringMore() {
        fun test(x: Long, s: String, radix: Int) {
            assertEquals(s, x.toULong().toString(radix))
            if (radix == 10) {
                assertEquals(s, x.toULong().toString())
            }
        }

        for (radix in 2..36) {
            test(0L, "0", radix)
            test(1L, "1", radix)
            test((radix * radix + 1).toLong(), "101", radix)
        }

        test(-111L, "1111111111111111111111111111111111111111111111111111111110010001", 2)
        test(-841L, "1111111111111111111111111111111111111111111111111111110010110111", 2)
        test(-48L, "1111111111111111111111111111111111111111111111111111111111010000", 2)
        test(11568553533L, "1010110001100010100001111000111101", 2)
        test(1448703278415412L, "101001001011001011010000100010010110111111000110100", 2)
        test(-78600467092795L, "1111111111111111101110001000001101100111000101000110101011000101", 2)
        test(97785243187L, "1011011000100011101000110011000110011", 2)
        test(-16595422816873L, "1111111111111111111100001110100000010011101101111101010110010111", 2)
        test(-8L, "1111111111111111111111111111111111111111111111111111111111111000", 2)
        test(1423166986L, "1010100110100111100111000001010", 2)
        test(528027512103548939L, "11101010011111011100011100110000010100100001011100000001011", 2)
        test(581852302953L, "1000011101111001000110011000111001101001", 2)
        test(-1L, "1111111111111111111111111111111111111111111111111111111111111111", 2)
        test(-1117567308L, "1111111111111111111111111111111110111101011000110100011010110100", 2)
        test(-10067395877878416L, "1111111111011100001110111100000110111100111000101100010101110000", 2)
        test(-3070582626L, "1111111111111111111111111111111101001000111110101010000010011110", 2)
        test(220037L, "110101101110000101", 2)
        test(-952L, "1111111111111111111111111111111111111111111111111111110001001000", 2)
        test(9266140931L, "1000101000010011100001011100000011", 2)
        test(-1356784282352L, "1111111111111111111111101100010000011001010110101101010100010000", 2)

        test(-301667321L, "11112220022122120101210110120011112002012", 3)
        test(-488003L, "11112220022122120101211020112220000102202", 3)
        test(56833883717507265L, "101020001001002012222002210022020210", 3)
        test(-59926140798833700L, "11112111112121000112201020201201222210221", 3)
        test(5L, "12", 3)
        test(50979L, "2120221010", 3)
        test(-1L, "11112220022122120101211020120210210211220", 3)
        test(-776392750L, "11112220022122120101202020110212222012220", 3)
        test(-4286476522150321L, "11112212112211110020200002200122122012210", 3)
        test(-1020566053765181881L, "11022002201211022212220222122112012002120", 3)
        test(9987889L, "200210102210211", 3)
        test(-3596895307268158L, "11112212200011212211222202012112212011110", 3)
        test(-2487431L, "11112220022122120101211020102010102201212", 3)
        test(3976408776971345954L, "222111022011101001112122222011212212012", 3)
        test(-4568571L, "11112220022122120101211020021012200221121", 3)
        test(545827038361L, "1221011212121212110122121", 3)
        test(364L, "111111", 3)
        test(886519L, "1200001002001", 3)
        test(-15052851071645379L, "11112210121112120200022111212222012020101", 3)
        test(-458371549386951L, "11112220020102110102221011012021112202101", 3)

        test(-1027601908345L, "1777777761027611636607", 8)
        test(28293264763580L, "633561177565274", 8)
        test(3743895037392734L, "152320657561733536", 8)
        test(-2129L, "1777777777777777773657", 8)
        test(1474403050396020748L, "121661034147246670014", 8)
        test(-40350517067559L, "1777776664645160700331", 8)
        test(-24378485730495L, "1777777235176101111501", 8)
        test(4051870L, "17351636", 8)
        test(-29624021L, "1777777777777616774453", 8)
        test(-175144656567263367L, "1766216055634100017571", 8)
        test(-474156057855165229L, "1745533531173334276323", 8)
        test(-37642598850426L, "1777776734164621060206", 8)
        test(79825011L, "460404163", 8)
        test(1026636894728412L, "35133420733070334", 8)
        test(323L, "503", 8)
        test(-5733135571000769476L, "1301576371133125742074", 8)
        test(169058178437096559L, "11304730220007422157", 8)
        test(16160605658002878L, "713237606746332676", 8)
        test(236007877229443L, "6552273553735603", 8)
        test(29373675467L, "332663533713", 8)

        test(-10630694120372L, "18446733443015431244", 10)
        test(59996L, "59996", 10)
        test(40422871616L, "40422871616", 10)
        test(-1L, "18446744073709551615", 10)
        test(-494906997247357617L, "17951837076462193999", 10)
        test(2094704541963722604L, "2094704541963722604", 10)
        test(-9143375L, "18446744073700408241", 10)
        test(-369000878580850L, "18446375072830970766", 10)
        test(-190028L, "18446744073709361588", 10)
        test(10710L, "10710", 10)
        test(1309381L, "1309381", 10)
        test(-251430906237896L, "18446492642803313720", 10)
        test(-6956615040474786610L, "11490129033234765006", 10)
        test(1053197837512127521L, "1053197837512127521", 10)
        test(32634423842079867L, "32634423842079867", 10)
        test(15845997546210L, "15845997546210", 10)
        test(366L, "366", 10)
        test(78019L, "78019", 10)
        test(67583790597992934L, "67583790597992934", 10)
        test(-2142564L, "18446744073707409052", 10)

        test(-3851L, "335500516a4290693a4", 11)
        test(-317L, "335500516a429071017", 11)
        test(2696544171243224829L, "53759308a9512a651a", 11)
        test(19187171L, "a915673", 11)
        test(-1435523339405L, "3355004766648615354", 11)
        test(-1047026437385872L, "3354831595a72093aa7", 11)
        test(3172942606031698L, "839aa7447730925", 11)
        test(11919217347L, "50670a6454", 11)
        test(413563597L, "1a249a4a7", 11)
        test(10669052023994L, "34437a07a9241", 11)
        test(2207571452769701770L, "44052532a563493294", 11)
        test(-5448833177050732L, "3353776a89239446519", 11)
        test(-7L, "335500516a429071279", 11)
        test(-137L, "335500516a429071170", 11)
        test(1067L, "890", 11)
        test(32077898L, "1711a666", 11)
        test(-305293149315285551L, "3298a12384947623923", 11)
        test(-4174600L, "335500516a42677a8a4", 11)
        test(-59105L, "335500516a429030933", 11)
        test(17379L, "1206a", 11)

        test(-290L, "fffffffffffffede", 16)
        test(31782L, "7c26", 16)
        test(150502870782171L, "88e1ae2690db", 16)
        test(400034567014840L, "16bd44e43c9b8", 16)
        test(65L, "41", 16)
        test(-79L, "ffffffffffffffb1", 16)
        test(-525928399581224L, "fffe21abc090cfd8", 16)
        test(-761491114358094656L, "f56ea3ddb511c4c0", 16)
        test(14207017882L, "34ecde39a", 16)
        test(1548087L, "179f37", 16)
        test(-2L, "fffffffffffffffe", 16)
        test(-4476778436479056L, "fff01864cb8e37b0", 16)
        test(1806L, "70e", 16)
        test(-97927L, "fffffffffffe8179", 16)
        test(-70947362492231344L, "ff03f1bfece65150", 16)
        test(1524862009L, "5ae38c39", 16)
        test(-86488940923772L, "ffffb156b9663484", 16)
        test(-655893195L, "ffffffffd8e7dd35", 16)
        test(21L, "15", 16)
        test(-2161087123997L, "fffffe08d52b6de3", 16)

        test(-5713122L, "1ddh88h27758f4f", 23)
        test(-7874718946L, "1ddh88h003eihkf", 23)
        test(-1347L, "1ddh88h2782i2bg", 23)
        test(2070942075280570L, "23mi28bh29f9", 23)
        test(-6832029644011364L, "1dda4adif5c7m69", 23)
        test(-4913545743348979L, "1ddc4hh1lfib20e", 23)
        test(-91416800L, "1ddh88h26gl2gai", 23)
        test(491457862L, "3784fk3", 23)
        test(4035521741L, "145mhec0", 23)
        test(-106795716L, "1ddh88h26ec3gi6", 23)
        test(24363083L, "3i18mb", 23)
        test(13910775L, "23g787", 23)
        test(-2L, "1ddh88h2782i514", 23)
        test(43224536139L, "cfmg1645", 23)
        test(-76910L, "1ddh88h2782bkf8", 23)
        test(-2395654L, "1ddh88h277h5799", 23)
        test(0L, "0", 23)
        test(16714141436088029L, "hcafkl33emlh", 23)
        test(-55339524590L, "1ddh88g91c37h8h", 23)
        test(1961423L, "704i6", 23)

        test(1L, "1", 36)
        test(-1L, "3w5e11264sgsf", 36)
        test(323385309041262901L, "2gg6iw29x3jp", 36)
        test(7L, "7", 36)
        test(33613L, "pxp", 36)
        test(11L, "b", 36)
        test(-173365910560L, "3w5e0yuiz7c2o", 36)
        test(16690491997L, "7o1361p", 36)
        test(7674L, "5x6", 36)
        test(5100325896106887L, "1e7wxdc12mf", 36)
        test(35651078165696286L, "9r1937kam8u", 36)
        test(-5896L, "3w5e11264sc8o", 36)
        test(-7L, "3w5e11264sgs9", 36)
        test(-765241254L, "3w5e111th6ouy", 36)
        test(174025356202421L, "1poq57vq9h", 36)
        test(-9655682L, "3w5e1125z1if2", 36)
        test(-8418216826200L, "3w5dy1mw7ue94", 36)
        test(325L, "91", 36)
        test(-5110L, "3w5e11264scui", 36)
        test(156326L, "3cme", 36)
    }
}
