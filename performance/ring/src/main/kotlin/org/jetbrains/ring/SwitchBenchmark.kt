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

package org.jetbrains.ring

import org.jetbrains.benchmarksLauncher.Blackhole

val SPARSE_SWITCH_CASES = intArrayOf(11, 29, 47, 71, 103,
                                     149, 175, 227, 263, 307,
                                     361, 487, 563, 617, 677,
                                     751, 823, 883, 967, 1031)

const val V1 = 1
const val V2 = 2
const val V3 = 3
const val V4 = 4
const val V5 = 5
const val V6 = 6
const val V7 = 7
const val V8 = 8
const val V9 = 9
const val V10 = 10
const val V11 = 11
const val V12 = 12
const val V13 = 13
const val V14 = 14
const val V15 = 15
const val V16 = 16
const val V17 = 17
const val V18 = 18
const val V19 = 19
const val V20 = 20


object Numbers {
    const val V1 = 1
    const val V2 = 2
    const val V3 = 3
    const val V4 = 4
    const val V5 = 5
    const val V6 = 6
    const val V7 = 7
    const val V8 = 8
    const val V9 = 9
    const val V10 = 10
    const val V11 = 11
    const val V12 = 12
    const val V13 = 13
    const val V14 = 14
    const val V15 = 15
    const val V16 = 16
    const val V17 = 17
    const val V18 = 18
    const val V19 = 19
    const val V20 = 20
}

var VV1 = 1
var VV2 = 2
var VV3 = 3
var VV4 = 4
var VV5 = 5
var VV6 = 6
var VV7 = 7
var VV8 = 8
var VV9 = 9
var VV10 = 10
var VV11 = 11
var VV12 = 12
var VV13 = 13
var VV14 = 14
var VV15 = 15
var VV16 = 16
var VV17 = 17
var VV18 = 18
var VV19 = 19
var VV20 = 20

open class SwitchBenchmark {
    fun sparseIntSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            11 -> {
                t = 1
            }
            29 -> {
                t = 2
            }
            47 -> {
                t = 3
            }
            71 -> {
                t = 4
            }
            103 -> {
                t = 5
            }
            149 -> {
                t = 6
            }
            175 -> {
                t = 7
            }
            227 -> {
                t = 1
            }
            263 -> {
                t = 9
            }
            307 -> {
                t = 1
            }
            361 -> {
                t = 2
            }
            487 -> {
                t = 3
            }
            563 -> {
                t = 4
            }
            617 -> {
                t = 4
            }
            677 -> {
                t = 4
            }
            751 -> {
                t = 435
            }
            823 -> {
                t = 31
            }
            883 -> {
                t = 1
            }
            967 -> {
                t = 1
            }
            1031 -> {
                t = 1
            }
            20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun denseIntSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            1 -> {
                t = 1
            }
            -1 -> {
                t = 2
            }
            2 -> {
                t = 3
            }
            3 -> {
                t = 4
            }
            4 -> {
                t = 5
            }
            5 -> {
                t = 6
            }
            6 -> {
                t = 7
            }
            7 -> {
                t = 1
            }
            8 -> {
                t = 9
            }
            9 -> {
                t = 1
            }
            10 -> {
                t = 2
            }
            11 -> {
                t = 3
            }
            12 -> {
                t = 4
            }
            13 -> {
                t = 4
            }
            14 -> {
                t = 4
            }
            15 -> {
                t = 435
            }
            16 -> {
                t = 31
            }
            17 -> {
                t = 1
            }
            18 -> {
                t = 1
            }
            19 -> {
                t = 1
            }
            20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun constSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            V1 -> {
                t = 1
            }
            V2 -> {
                t = 3
            }
            V3 -> {
                t = 4
            }
            V4 -> {
                t = 5
            }
            V5 -> {
                t = 6
            }
            V6 -> {
                t = 7
            }
            V7 -> {
                t = 1
            }
            V8 -> {
                t = 9
            }
            V9 -> {
                t = 1
            }
            V10 -> {
                t = 2
            }
            V11 -> {
                t = 3
            }
            V12 -> {
                t = 4
            }
            V13 -> {
                t = 4
            }
            V14 -> {
                t = 4
            }
            V15 -> {
                t = 435
            }
            V16 -> {
                t = 31
            }
            V17 -> {
                t = 1
            }
            V18 -> {
                t = 1
            }
            V19 -> {
                t = 1
            }
            V20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun objConstSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            Numbers.V1 -> {
                t = 1
            }
            Numbers.V2 -> {
                t = 3
            }
            Numbers.V3 -> {
                t = 4
            }
            Numbers.V4 -> {
                t = 5
            }
            Numbers.V5 -> {
                t = 6
            }
            Numbers.V6 -> {
                t = 7
            }
            Numbers.V7 -> {
                t = 1
            }
            Numbers.V8 -> {
                t = 9
            }
            Numbers.V9 -> {
                t = 1
            }
            Numbers.V10 -> {
                t = 2
            }
            Numbers.V11 -> {
                t = 3
            }
            Numbers.V12 -> {
                t = 4
            }
            Numbers.V13 -> {
                t = 4
            }
            Numbers.V14 -> {
                t = 4
            }
            Numbers.V15 -> {
                t = 435
            }
            Numbers.V16 -> {
                t = 31
            }
            Numbers.V17 -> {
                t = 1
            }
            Numbers.V18 -> {
                t = 1
            }
            Numbers.V19 -> {
                t = 1
            }
            Numbers.V20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    fun varSwitch(u : Int) : Int {
        var t : Int
        when (u) {
            VV1 -> {
                t = 1
            }
            VV2 -> {
                t = 3
            }
            VV3 -> {
                t = 4
            }
            VV4 -> {
                t = 5
            }
            VV5 -> {
                t = 6
            }
            VV6 -> {
                t = 7
            }
            VV7 -> {
                t = 1
            }
            VV8 -> {
                t = 9
            }
            VV9 -> {
                t = 1
            }
            VV10 -> {
                t = 2
            }
            VV11 -> {
                t = 3
            }
            VV12 -> {
                t = 4
            }
            VV13 -> {
                t = 4
            }
            VV14 -> {
                t = 4
            }
            VV15 -> {
                t = 435
            }
            VV16 -> {
                t = 31
            }
            VV17 -> {
                t = 1
            }
            VV18 -> {
                t = 1
            }
            VV19 -> {
                t = 1
            }
            VV20 -> {
                t = 1
            }
            else -> {
                t = 5
            }
        }
        return t
    }

    private fun stringSwitch(s: String) : Int {
        when(s) {
            "ABCDEFG1" -> return 1
            "ABCDEFG2" -> return 2
            "ABCDEFG2" -> return 3
            "ABCDEFG3" -> return 4
            "ABCDEFG4" -> return 5
            "ABCDEFG5" -> return 6
            "ABCDEFG6" -> return 7
            "ABCDEFG7" -> return 8
            "ABCDEFG8" -> return 9
            "ABCDEFG9" -> return 10
            "ABCDEFG10" -> return 11
            "ABCDEFG11" -> return 12
            "ABCDEFG12" -> return 1
            "ABCDEFG13" -> return 2
            "ABCDEFG14" -> return 3
            "ABCDEFG15" -> return 4
            "ABCDEFG16" -> return 5
            "ABCDEFG17" -> return 6
            "ABCDEFG18" -> return 7
            "ABCDEFG19" -> return 8
            "ABCDEFG20" -> return 9
            else -> return -1
        }
    }

    lateinit var denseIntData: IntArray
    lateinit var sparseIntData: IntArray



    //Benchmark 
    fun testSparseIntSwitch() {
        for (i in sparseIntData) {
            Blackhole.consume(sparseIntSwitch(i))
        }
    }

    //Benchmark 
    fun testDenseIntSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(denseIntSwitch(i))
        }
    }

    //Benchmark 
    fun testConstSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(constSwitch(i))
        }
    }

    //Benchmark 
    fun testObjConstSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(objConstSwitch(i))
        }
    }

    //Benchmark 
    fun testVarSwitch() {
        for (i in denseIntData) {
            Blackhole.consume(varSwitch(i))
        }
    }

    var data : Array<String> = arrayOf()



    //Benchmark 
    fun testStringsSwitch() {
        val n = data.size
        for (s in data) {
            Blackhole.consume(stringSwitch(s))
        }
    }

    enum class MyEnum {
        ITEM1, ITEM2, ITEM3, ITEM4, ITEM5, ITEM6, ITEM7, ITEM8, ITEM9, ITEM10, ITEM11, ITEM12, ITEM13, ITEM14, ITEM15, ITEM16, ITEM17, ITEM18, ITEM19, ITEM20, ITEM21, ITEM22, ITEM23, ITEM24, ITEM25, ITEM26, ITEM27, ITEM28, ITEM29, ITEM30, ITEM31, ITEM32, ITEM33, ITEM34, ITEM35, ITEM36, ITEM37, ITEM38, ITEM39, ITEM40, ITEM41, ITEM42, ITEM43, ITEM44, ITEM45, ITEM46, ITEM47, ITEM48, ITEM49, ITEM50, ITEM51, ITEM52, ITEM53, ITEM54, ITEM55, ITEM56, ITEM57, ITEM58, ITEM59, ITEM60, ITEM61, ITEM62, ITEM63, ITEM64, ITEM65, ITEM66, ITEM67, ITEM68, ITEM69, ITEM70, ITEM71, ITEM72, ITEM73, ITEM74, ITEM75, ITEM76, ITEM77, ITEM78, ITEM79, ITEM80, ITEM81, ITEM82, ITEM83, ITEM84, ITEM85, ITEM86, ITEM87, ITEM88, ITEM89, ITEM90, ITEM91, ITEM92, ITEM93, ITEM94, ITEM95, ITEM96, ITEM97, ITEM98, ITEM99, ITEM100
    }

    private fun enumSwitch(x: MyEnum) : Int {
        when (x) {
            MyEnum.ITEM5 -> return 1
            MyEnum.ITEM10 -> return 2
            MyEnum.ITEM15 -> return 3
            MyEnum.ITEM20 -> return 4
            MyEnum.ITEM25 -> return 5
            MyEnum.ITEM30 -> return 6
            MyEnum.ITEM35 -> return 7
            MyEnum.ITEM40 -> return 8
            MyEnum.ITEM45 -> return 9
            MyEnum.ITEM50 -> return 10
            MyEnum.ITEM55 -> return 11
            MyEnum.ITEM60 -> return 12
            MyEnum.ITEM65 -> return 13
            MyEnum.ITEM70 -> return 14
            MyEnum.ITEM75 -> return 15
            MyEnum.ITEM80 -> return 16
            MyEnum.ITEM85 -> return 17
            MyEnum.ITEM90 -> return 18
            MyEnum.ITEM95 -> return 19
            MyEnum.ITEM100 -> return 20
            else -> return -1
        }
    }

    private fun denseEnumSwitch(x: MyEnum) : Int {
        when (x) {
            MyEnum.ITEM1 -> return 1
            MyEnum.ITEM2 -> return 2
            MyEnum.ITEM3 -> return 3
            MyEnum.ITEM4 -> return 4
            MyEnum.ITEM5 -> return 5
            MyEnum.ITEM6 -> return 6
            MyEnum.ITEM7 -> return 7
            MyEnum.ITEM8 -> return 8
            MyEnum.ITEM9 -> return 9
            MyEnum.ITEM10 -> return 10
            MyEnum.ITEM11 -> return 11
            MyEnum.ITEM12 -> return 12
            MyEnum.ITEM13 -> return 13
            MyEnum.ITEM14 -> return 14
            MyEnum.ITEM15 -> return 15
            MyEnum.ITEM16 -> return 16
            MyEnum.ITEM17 -> return 17
            MyEnum.ITEM18 -> return 18
            MyEnum.ITEM19 -> return 19
            MyEnum.ITEM20 -> return 20
            else -> return -1
        }
    }

    lateinit var enumData : Array<MyEnum>
    lateinit var denseEnumData : Array<MyEnum>



    //Benchmark 
    fun testEnumsSwitch() {
        val n = enumData.size -1
        val data = enumData
        for (i in 0..n) {
            Blackhole.consume(enumSwitch(data[i]))
        }
    }

    //Benchmark 
    fun testDenseEnumsSwitch() {
        val n = denseEnumData.size -1
        val data = denseEnumData
        for (i in 0..n) {
            Blackhole.consume(denseEnumSwitch(data[i]))
        }
    }

    sealed class MySealedClass {
        class MySealedClass1: MySealedClass()
        class MySealedClass2: MySealedClass()
        class MySealedClass3: MySealedClass()
        class MySealedClass4: MySealedClass()
        class MySealedClass5: MySealedClass()
        class MySealedClass6: MySealedClass()
        class MySealedClass7: MySealedClass()
        class MySealedClass8: MySealedClass()
        class MySealedClass9: MySealedClass()
        class MySealedClass10: MySealedClass()
    }

    lateinit var sealedClassData: Array<MySealedClass>

    init {
        data = Array(BENCHMARK_SIZE) {
            "ABCDEFG" + Random.nextInt(22)
        }
        enumData = Array(BENCHMARK_SIZE) {
            MyEnum.values()[it % MyEnum.values().size]
        }
        denseEnumData = Array(BENCHMARK_SIZE) {
            MyEnum.values()[it % 20]
        }
        denseIntData = IntArray(BENCHMARK_SIZE) { Random.nextInt(25) - 1 }
        sparseIntData = IntArray(BENCHMARK_SIZE) { SPARSE_SWITCH_CASES[Random.nextInt(20)] }
        sealedClassData = Array(BENCHMARK_SIZE) {
            when(Random.nextInt(10)) {
                0 -> MySealedClass.MySealedClass1()
                1 -> MySealedClass.MySealedClass2()
                2 -> MySealedClass.MySealedClass3()
                3 -> MySealedClass.MySealedClass4()
                4 -> MySealedClass.MySealedClass5()
                5 -> MySealedClass.MySealedClass6()
                6 -> MySealedClass.MySealedClass7()
                7 -> MySealedClass.MySealedClass8()
                8 -> MySealedClass.MySealedClass9()
                9 -> MySealedClass.MySealedClass10()
                else -> throw IllegalStateException()
            }
        }
    }

    private fun sealedWhenSwitch(x: MySealedClass) : Int =
        when (x) {
            is MySealedClass.MySealedClass1 -> 1
            is MySealedClass.MySealedClass2 -> 2
            is MySealedClass.MySealedClass3 -> 3
            is MySealedClass.MySealedClass4 -> 4
            is MySealedClass.MySealedClass5 -> 5
            is MySealedClass.MySealedClass6 -> 6
            is MySealedClass.MySealedClass7 -> 7
            is MySealedClass.MySealedClass8 -> 8
            is MySealedClass.MySealedClass9 -> 9
            is MySealedClass.MySealedClass10 -> 10
        }


    //Benchmark 
    fun testSealedWhenSwitch() {
        val n = sealedClassData.size -1
        for (i in 0..n) {
            Blackhole.consume(sealedWhenSwitch(sealedClassData[i]))
        }
    }
}
