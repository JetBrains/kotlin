/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin

// (0..22).joinToString("\n") { i -> "interface SuspendFunction$i<${(1..i).joinToString("") { j -> "in P$j, " }}out R> : SuspendFunction<R> {\n    operator suspend fun invoke(${(1..i).joinToString { j -> "p$j: P$j" }}): R\n}\n" }

interface SuspendFunction0<out R> : SuspendFunction<R> {
    operator suspend fun invoke(): R
}

interface SuspendFunction1<in P1, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1): R
}

interface SuspendFunction2<in P1, in P2, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2): R
}

interface SuspendFunction3<in P1, in P2, in P3, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3): R
}

interface SuspendFunction4<in P1, in P2, in P3, in P4, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R
}

interface SuspendFunction5<in P1, in P2, in P3, in P4, in P5, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): R
}

interface SuspendFunction6<in P1, in P2, in P3, in P4, in P5, in P6, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): R
}

interface SuspendFunction7<in P1, in P2, in P3, in P4, in P5, in P6, in P7, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7): R
}

interface SuspendFunction8<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8): R
}

interface SuspendFunction9<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9): R
}

interface SuspendFunction10<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10): R
}

interface SuspendFunction11<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11): R
}

interface SuspendFunction12<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12): R
}

interface SuspendFunction13<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13): R
}

interface SuspendFunction14<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14): R
}

interface SuspendFunction15<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15): R
}

interface SuspendFunction16<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16): R
}

interface SuspendFunction17<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17): R
}

interface SuspendFunction18<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18): R
}

interface SuspendFunction19<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19): R
}

interface SuspendFunction20<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20): R
}

interface SuspendFunction21<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21): R
}

interface SuspendFunction22<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, in P22, out R> : SuspendFunction<R> {
    operator suspend fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21, p22: P22): R
}
