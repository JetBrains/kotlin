// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.large_live_roots

import kotlin.native.runtime.GC

class RootItem(val id: Int)

fun box(): String {
    val r0 = RootItem(0); val r1 = RootItem(1); val r2 = RootItem(2); val r3 = RootItem(3); val r4 = RootItem(4)
    val r5 = RootItem(5); val r6 = RootItem(6); val r7 = RootItem(7); val r8 = RootItem(8); val r9 = RootItem(9)
    val r10 = RootItem(10); val r11 = RootItem(11); val r12 = RootItem(12); val r13 = RootItem(13); val r14 = RootItem(14)
    val r15 = RootItem(15); val r16 = RootItem(16); val r17 = RootItem(17); val r18 = RootItem(18); val r19 = RootItem(19)
    val r20 = RootItem(20); val r21 = RootItem(21); val r22 = RootItem(22); val r23 = RootItem(23); val r24 = RootItem(24)
    val r25 = RootItem(25); val r26 = RootItem(26); val r27 = RootItem(27); val r28 = RootItem(28); val r29 = RootItem(29)
    val r30 = RootItem(30); val r31 = RootItem(31); val r32 = RootItem(32); val r33 = RootItem(33); val r34 = RootItem(34)
    val r35 = RootItem(35); val r36 = RootItem(36); val r37 = RootItem(37); val r38 = RootItem(38); val r39 = RootItem(39)
    val r40 = RootItem(40); val r41 = RootItem(41); val r42 = RootItem(42); val r43 = RootItem(43); val r44 = RootItem(44)
    val r45 = RootItem(45); val r46 = RootItem(46); val r47 = RootItem(47); val r48 = RootItem(48); val r49 = RootItem(49)

    GC.collect()

    val sum = r0.id + r1.id + r2.id + r3.id + r4.id + r5.id + r6.id + r7.id + r8.id + r9.id +
              r10.id + r11.id + r12.id + r13.id + r14.id + r15.id + r16.id + r17.id + r18.id + r19.id +
              r20.id + r21.id + r22.id + r23.id + r24.id + r25.id + r26.id + r27.id + r28.id + r29.id +
              r30.id + r31.id + r32.id + r33.id + r34.id + r35.id + r36.id + r37.id + r38.id + r39.id +
              r40.id + r41.id + r42.id + r43.id + r44.id + r45.id + r46.id + r47.id + r48.id + r49.id

    if (sum != 1225) return "FAIL sum 50 roots: $sum"
    return "OK"
}
