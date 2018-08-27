/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.reflect

// (0..22).joinToString("\n") { i -> "interface KFunction$i<${(1..i).joinToString("") { j -> "in P$j, " }}out R> : Function$i<${(1..i).joinToString("") { j -> "P$j, " }}R>, KFunction<R>\n" }

interface KFunction0<out R> : Function0<R>, KFunction<R>

interface KFunction1<in P1, out R> : Function1<P1, R>, KFunction<R>

interface KFunction2<in P1, in P2, out R> : Function2<P1, P2, R>, KFunction<R>

interface KFunction3<in P1, in P2, in P3, out R> : Function3<P1, P2, P3, R>, KFunction<R>

interface KFunction4<in P1, in P2, in P3, in P4, out R> : Function4<P1, P2, P3, P4, R>, KFunction<R>

interface KFunction5<in P1, in P2, in P3, in P4, in P5, out R> : Function5<P1, P2, P3, P4, P5, R>, KFunction<R>

interface KFunction6<in P1, in P2, in P3, in P4, in P5, in P6, out R> : Function6<P1, P2, P3, P4, P5, P6, R>, KFunction<R>

interface KFunction7<in P1, in P2, in P3, in P4, in P5, in P6, in P7, out R> : Function7<P1, P2, P3, P4, P5, P6, P7, R>, KFunction<R>

interface KFunction8<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, out R> : Function8<P1, P2, P3, P4, P5, P6, P7, P8, R>, KFunction<R>

interface KFunction9<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, out R> : Function9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>, KFunction<R>

interface KFunction10<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, out R> : Function10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R>, KFunction<R>

interface KFunction11<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, out R> : Function11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R>, KFunction<R>

interface KFunction12<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, out R> : Function12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R>, KFunction<R>

interface KFunction13<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, out R> : Function13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R>, KFunction<R>

interface KFunction14<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, out R> : Function14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R>, KFunction<R>

interface KFunction15<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, out R> : Function15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R>, KFunction<R>

interface KFunction16<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, out R> : Function16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R>, KFunction<R>

interface KFunction17<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, out R> : Function17<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R>, KFunction<R>

interface KFunction18<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, out R> : Function18<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R>, KFunction<R>

interface KFunction19<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, out R> : Function19<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R>, KFunction<R>

interface KFunction20<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, out R> : Function20<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R>, KFunction<R>

interface KFunction21<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, out R> : Function21<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R>, KFunction<R>

interface KFunction22<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, in P9, in P10, in P11, in P12, in P13, in P14, in P15, in P16, in P17, in P18, in P19, in P20, in P21, in P22, out R> : Function22<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R>, KFunction<R>

