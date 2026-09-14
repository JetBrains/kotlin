// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.large_live_roots

import kotlin.native.runtime.GC

class ValueCell(val v: Int)

fun box(): String {
    val r0 = ValueCell(0); val r1 = ValueCell(1); val r2 = ValueCell(2); val r3 = ValueCell(3); val r4 = ValueCell(4)
    val r5 = ValueCell(5); val r6 = ValueCell(6); val r7 = ValueCell(7); val r8 = ValueCell(8); val r9 = ValueCell(9)
    val r10 = ValueCell(10); val r11 = ValueCell(11); val r12 = ValueCell(12); val r13 = ValueCell(13); val r14 = ValueCell(14)
    val r15 = ValueCell(15); val r16 = ValueCell(16); val r17 = ValueCell(17); val r18 = ValueCell(18); val r19 = ValueCell(19)
    val r20 = ValueCell(20); val r21 = ValueCell(21); val r22 = ValueCell(22); val r23 = ValueCell(23); val r24 = ValueCell(24)
    val r25 = ValueCell(25); val r26 = ValueCell(26); val r27 = ValueCell(27); val r28 = ValueCell(28); val r29 = ValueCell(29)
    val r30 = ValueCell(30); val r31 = ValueCell(31); val r32 = ValueCell(32); val r33 = ValueCell(33); val r34 = ValueCell(34)
    val r35 = ValueCell(35); val r36 = ValueCell(36); val r37 = ValueCell(37); val r38 = ValueCell(38); val r39 = ValueCell(39)
    val r40 = ValueCell(40); val r41 = ValueCell(41); val r42 = ValueCell(42); val r43 = ValueCell(43); val r44 = ValueCell(44)
    val r45 = ValueCell(45); val r46 = ValueCell(46); val r47 = ValueCell(47); val r48 = ValueCell(48); val r49 = ValueCell(49)
    val r50 = ValueCell(50); val r51 = ValueCell(51); val r52 = ValueCell(52); val r53 = ValueCell(53); val r54 = ValueCell(54)
    val r55 = ValueCell(55); val r56 = ValueCell(56); val r57 = ValueCell(57); val r58 = ValueCell(58); val r59 = ValueCell(59)
    val r60 = ValueCell(60); val r61 = ValueCell(61); val r62 = ValueCell(62); val r63 = ValueCell(63); val r64 = ValueCell(64)
    val r65 = ValueCell(65); val r66 = ValueCell(66); val r67 = ValueCell(67); val r68 = ValueCell(68); val r69 = ValueCell(69)
    val r70 = ValueCell(70); val r71 = ValueCell(71); val r72 = ValueCell(72); val r73 = ValueCell(73); val r74 = ValueCell(74)
    val r75 = ValueCell(75); val r76 = ValueCell(76); val r77 = ValueCell(77); val r78 = ValueCell(78); val r79 = ValueCell(79)
    val r80 = ValueCell(80); val r81 = ValueCell(81); val r82 = ValueCell(82); val r83 = ValueCell(83); val r84 = ValueCell(84)
    val r85 = ValueCell(85); val r86 = ValueCell(86); val r87 = ValueCell(87); val r88 = ValueCell(88); val r89 = ValueCell(89)
    val r90 = ValueCell(90); val r91 = ValueCell(91); val r92 = ValueCell(92); val r93 = ValueCell(93); val r94 = ValueCell(94)
    val r95 = ValueCell(95); val r96 = ValueCell(96); val r97 = ValueCell(97); val r98 = ValueCell(98); val r99 = ValueCell(99)

    GC.collect()

    val sum = r0.v + r1.v + r2.v + r3.v + r4.v + r5.v + r6.v + r7.v + r8.v + r9.v +
              r10.v + r11.v + r12.v + r13.v + r14.v + r15.v + r16.v + r17.v + r18.v + r19.v +
              r20.v + r21.v + r22.v + r23.v + r24.v + r25.v + r26.v + r27.v + r28.v + r29.v +
              r30.v + r31.v + r32.v + r33.v + r34.v + r35.v + r36.v + r37.v + r38.v + r39.v +
              r40.v + r41.v + r42.v + r43.v + r44.v + r45.v + r46.v + r47.v + r48.v + r49.v +
              r50.v + r51.v + r52.v + r53.v + r54.v + r55.v + r56.v + r57.v + r58.v + r59.v +
              r60.v + r61.v + r62.v + r63.v + r64.v + r65.v + r66.v + r67.v + r68.v + r69.v +
              r70.v + r71.v + r72.v + r73.v + r74.v + r75.v + r76.v + r77.v + r78.v + r79.v +
              r80.v + r81.v + r82.v + r83.v + r84.v + r85.v + r86.v + r87.v + r88.v + r89.v +
              r90.v + r91.v + r92.v + r93.v + r94.v + r95.v + r96.v + r97.v + r98.v + r99.v

    if (sum != 4950) return "FAIL 100 roots sum: $sum"
    return "OK"
}
