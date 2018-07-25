/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.ranges

import java.lang.Integer.MAX_VALUE as MaxI
import java.lang.Integer.MIN_VALUE as MinI
import java.lang.Byte.MAX_VALUE as MaxB
import java.lang.Byte.MIN_VALUE as MinB
import java.lang.Short.MAX_VALUE as MaxS
import java.lang.Short.MIN_VALUE as MinS
import java.lang.Long.MAX_VALUE as MaxL
import java.lang.Long.MIN_VALUE as MinL
import java.lang.Character.MAX_VALUE as MaxC
import java.lang.Character.MIN_VALUE as MinC
import kotlin.test.*

// Test data for codegen is generated from this class. If you change it, rerun GenerateTests
public class RangeIterationJVMTest : RangeIterationTestBase() {


    @Test fun maxValueToMaxValue() {
        doTest(MaxI..MaxI, MaxI, MaxI, 1, listOf(MaxI))
        doTest(MaxB..MaxB, MaxB.toInt(), MaxB.toInt(), 1, listOf(MaxB.toInt()))
        doTest(MaxS..MaxS, MaxS.toInt(), MaxS.toInt(), 1, listOf(MaxS.toInt()))
        doTest(MaxL..MaxL, MaxL, MaxL, 1.toLong(), listOf(MaxL))

        doTest(MaxC..MaxC, MaxC, MaxC, 1, listOf(MaxC))
    }

    @Test fun maxValueMinusTwoToMaxValue() {
        doTest((MaxI - 2)..MaxI, MaxI - 2, MaxI, 1, listOf(MaxI - 2, MaxI - 1, MaxI))
        doTest((MaxB - 2).toByte()..MaxB, (MaxB - 2).toInt(), MaxB.toInt(), 1, listOf((MaxB - 2).toInt(), (MaxB - 1).toInt(), MaxB.toInt()))
        doTest((MaxS - 2).toShort()..MaxS, (MaxS - 2).toInt(), MaxS.toInt(), 1, listOf((MaxS - 2).toInt(), (MaxS - 1).toInt(), MaxS.toInt()))
        doTest((MaxL - 2).toLong()..MaxL, (MaxL - 2).toLong(), MaxL, 1.toLong(), listOf((MaxL - 2).toLong(), (MaxL - 1).toLong(), MaxL))

        doTest((MaxC - 2)..MaxC, (MaxC - 2), MaxC, 1, listOf((MaxC - 2), (MaxC - 1), MaxC))
    }

    @Test fun maxValueToMinValue() {
        doTest(MaxI..MinI, MaxI, MinI, 1, listOf())
        doTest(MaxB..MinB, MaxB.toInt(), MinB.toInt(), 1, listOf())
        doTest(MaxS..MinS, MaxS.toInt(), MinS.toInt(), 1, listOf())
        doTest(MaxL..MinL, MaxL, MinL, 1.toLong(), listOf())

        doTest(MaxC..MinC, MaxC, MinC, 1, listOf())
    }

    @Test fun progressionMaxValueToMaxValue() {
        doTest(MaxI..MaxI step 1, MaxI, MaxI, 1, listOf(MaxI))
        doTest(MaxB..MaxB step 1, MaxB.toInt(), MaxB.toInt(), 1, listOf(MaxB.toInt()))
        doTest(MaxS..MaxS step 1, MaxS.toInt(), MaxS.toInt(), 1, listOf(MaxS.toInt()))
        doTest(MaxL..MaxL step 1, MaxL, MaxL, 1.toLong(), listOf(MaxL))

        doTest(MaxC..MaxC step 1, MaxC, MaxC, 1, listOf(MaxC))
    }

    @Test fun progressionMaxValueMinusTwoToMaxValue() {
        doTest((MaxI - 2)..MaxI step 2, MaxI - 2, MaxI, 2, listOf(MaxI - 2, MaxI))
        doTest((MaxB - 2).toByte()..MaxB step 2, (MaxB - 2).toInt(), MaxB.toInt(), 2, listOf((MaxB - 2).toInt(), MaxB.toInt()))
        doTest((MaxS - 2).toShort()..MaxS step 2, (MaxS - 2).toInt(), MaxS.toInt(), 2, listOf((MaxS - 2).toInt(), MaxS.toInt()))
        doTest((MaxL - 2).toLong()..MaxL step 2, (MaxL - 2).toLong(), MaxL, 2.toLong(), listOf((MaxL - 2).toLong(), MaxL))

        doTest((MaxC - 2)..MaxC step 2, (MaxC - 2), MaxC, 2, listOf((MaxC - 2), MaxC))
    }

    @Test fun progressionMaxValueToMinValue() {
        doTest(MaxI..MinI step 1, MaxI, MinI, 1, listOf())
        doTest(MaxB..MinB step 1, MaxB.toInt(), MinB.toInt(), 1, listOf())
        doTest(MaxS..MinS step 1, MaxS.toInt(), MinS.toInt(), 1, listOf())
        doTest(MaxL..MinL step 1, MaxL, MinL, 1.toLong(), listOf())

        doTest(MaxC..MinC step 1, MaxC, MinC, 1, listOf())
    }

    @Test fun progressionMinValueToMinValue() {
        doTest(MinI..MinI step 1, MinI, MinI, 1, listOf(MinI))
        doTest(MinB..MinB step 1, MinB.toInt(), MinB.toInt(), 1, listOf(MinB.toInt()))
        doTest(MinS..MinS step 1, MinS.toInt(), MinS.toInt(), 1, listOf(MinS.toInt()))
        doTest(MinL..MinL step 1, MinL, MinL, 1.toLong(), listOf(MinL))

        doTest(MinC..MinC step 1, MinC, MinC, 1, listOf(MinC))
    }

    @Test fun inexactToMaxValue() {
        doTest((MaxI - 5)..MaxI step 3, MaxI - 5, MaxI - 2, 3, listOf(MaxI - 5, MaxI - 2))
        doTest((MaxB - 5).toByte()..MaxB step 3, (MaxB - 5).toInt(), (MaxB - 2).toInt(), 3, listOf((MaxB - 5).toInt(), (MaxB - 2).toInt()))
        doTest((MaxS - 5).toShort()..MaxS step 3, (MaxS - 5).toInt(), (MaxS - 2).toInt(), 3, listOf((MaxS - 5).toInt(), (MaxS - 2).toInt()))
        doTest((MaxL - 5).toLong()..MaxL step 3, (MaxL - 5).toLong(), (MaxL - 2).toLong(), 3.toLong(), listOf((MaxL - 5).toLong(), (MaxL - 2).toLong()))

        doTest((MaxC - 5)..MaxC step 3, (MaxC - 5), (MaxC - 2), 3, listOf((MaxC - 5), (MaxC - 2)))
    }

    @Test fun overflowZeroToMinValue() {
        doTest(0..MinI step 3, 0, MinI, 3, listOf())
        doTest(0L..MinL step 3, 0, MinL, 3.toLong(), listOf())
    }

    @Test fun progressionDownToMinValue() {
        doTest((MinI + 2) downTo MinI step 1, MinI + 2, MinI, -1, listOf(MinI + 2, MinI + 1, MinI))
        doTest((MinB + 2).toByte() downTo MinB step 1, (MinB + 2).toInt(), MinB.toInt(), -1, listOf((MinB + 2).toInt(), (MinB + 1).toInt(), MinB.toInt()))
        doTest((MinS + 2).toShort() downTo MinS step 1, (MinS + 2).toInt(), MinS.toInt(), -1, listOf((MinS + 2).toInt(), (MinS + 1).toInt(), MinS.toInt()))
        doTest((MinL + 2).toLong() downTo MinL step 1, (MinL + 2).toLong(), MinL, -1.toLong(), listOf((MinL + 2).toLong(), (MinL + 1).toLong(), MinL))

        doTest((MinC + 2) downTo MinC step 1, (MinC + 2), MinC, -1, listOf((MinC + 2), (MinC + 1), MinC))
    }

    @Test fun inexactDownToMinValue() {
        doTest((MinI + 5) downTo MinI step 3, MinI + 5, MinI + 2, -3, listOf(MinI + 5, MinI + 2))
        doTest((MinB + 5).toByte() downTo MinB step 3, (MinB + 5).toInt(), (MinB + 2).toInt(), -3, listOf((MinB + 5).toInt(), (MinB + 2).toInt()))
        doTest((MinS + 5).toShort() downTo MinS step 3, (MinS + 5).toInt(), (MinS + 2).toInt(), -3, listOf((MinS + 5).toInt(), (MinS + 2).toInt()))
        doTest((MinL + 5).toLong() downTo MinL step 3, (MinL + 5).toLong(), (MinL + 2).toLong(), -3.toLong(), listOf((MinL + 5).toLong(), (MinL + 2).toLong()))

        doTest((MinC + 5) downTo MinC step 3, (MinC + 5), (MinC + 2), -3, listOf((MinC + 5), (MinC + 2)))
    }

    @Test fun overflowZeroDownToMaxValue() {
        doTest(0 downTo MaxI step 3, 0, MaxI, -3, listOf())
        doTest(0 downTo MaxL step 3, 0, MaxL, -3.toLong(), listOf())
    }
}