/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE")

package kotlin.reflect

import kotlin.coroutines.*

// (0..22).joinToString("\n") { i -> "interface KSuspendFunction$i<${(1..i).joinToString("") { j -> "in P$j, " }}out R> : SuspendFunction$i<${(1..i).joinToString("") { j -> "P$j, " }}R>, KFunction<R>\n" }

interface KSuspendFunction0<out R> : SuspendFunction0<R>, KFunction<R>

interface KSuspendFunction1<in P1, out R> : SuspendFunction1<P1, R>, KFunction<R>

interface KSuspendFunction2<in P1, in P2, out R> : SuspendFunction2<P1, P2, R>, KFunction<R>

interface KSuspendFunction3<in P1, in P2, in P3, out R> : SuspendFunction3<P1, P2, P3, R>, KFunction<R>

interface KSuspendFunction4<in P1, in P2, in P3, in P4, out R> : SuspendFunction4<P1, P2, P3, P4, R>, KFunction<R>

interface KSuspendFunction5<in P1, in P2, in P3, in P4, in P5, out R> : SuspendFunction5<P1, P2, P3, P4, P5, R>, KFunction<R>

interface KSuspendFunction6<in P1, in P2, in P3, in P4, in P5, in P6, out R> : SuspendFunction6<P1, P2, P3, P4, P5, P6, R>, KFunction<R>

interface KSuspendFunction7<in P1, in P2, in P3, in P4, in P5, in P6, in P7, out R> : SuspendFunction7<P1, P2, P3, P4, P5, P6, P7, R>, KFunction<R>

interface KSuspendFunction8<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, out R> : SuspendFunction8<P1, P2, P3, P4, P5, P6, P7, P8, R>, KFunction<R>

interface KSuspendFunction9<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, out R> : SuspendFunction9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>, KFunction<R>

interface KSuspendFunction10<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, out R> : SuspendFunction10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R>, KFunction<R>

interface KSuspendFunction11<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, out R> : SuspendFunction11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R>, KFunction<R>

interface KSuspendFunction12<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, out R> : SuspendFunction12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R>, KFunction<R>

interface KSuspendFunction13<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, out R> : SuspendFunction13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R>, KFunction<R>

interface KSuspendFunction14<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, out R> : SuspendFunction14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R>, KFunction<R>

interface KSuspendFunction15<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, out R> : SuspendFunction15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R>, KFunction<R>

interface KSuspendFunction16<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, out R> : SuspendFunction16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R>, KFunction<R>

interface KSuspendFunction17<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, out R> : SuspendFunction17<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R>, KFunction<R>

interface KSuspendFunction18<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, out R> : SuspendFunction18<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R>, KFunction<R>

interface KSuspendFunction19<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, out R> : SuspendFunction19<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R>, KFunction<R>

interface KSuspendFunction20<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, out R> : SuspendFunction20<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R>, KFunction<R>

interface KSuspendFunction21<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, out R> : SuspendFunction21<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R>, KFunction<R>

// Comment for now. The reason: https://github.com/JetBrains/kotlin/blob/ad1b795a69925e6ee8c79cbbd05cce8cd27a6704/compiler/serialization/src/org/jetbrains/kotlin/serialization/DescriptorSerializer.kt#L555
//interface KSuspendFunction22<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, in P22, out R> : SuspendFunction22<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R>, KFunction<R>
