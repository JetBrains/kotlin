/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalObjCName::class)

package objCNameB

import kotlin.experimental.ExperimentalObjCName

// https://youtrack.jetbrains.com/issue/KT-50767
@ObjCName("ObjCNameC1B")
class ObjCNameC1 {
    fun foo(): String = "b"
}

@ObjCName("MyObjCArray", "MySwiftArray")
class MyKotlinArray {
    // https://developer.apple.com/documentation/foundation/nsarray/1409982-count
    @ObjCName("count")
    val size: Int = 0
    // https://developer.apple.com/documentation/foundation/nsarray/1417076-index
    @ObjCName(swiftName = "index")
    fun indexOf(@ObjCName("object", "of") element: Int): Int = element
}

interface ObjCNameI1 {
    @ObjCName("someOtherValue")
    val someValue: Int
    @ObjCName("someOtherFunction")
    fun @receiver:ObjCName("receiver") Int.someFunction(@ObjCName("otherParam") param: Int): Int
}

fun @receiver:ObjCName("of") ObjCNameI1.getSomeValue(): Int = someValue

@ObjCName(swiftName = "SwiftNameC2")
class ObjCNameC2: ObjCNameI1 {
    @ObjCName("ObjCNestedClass", "SwiftNestedClass")
    class NestedClass {
        var nestedValue: Int = 1
    }

    @ObjCName("ObjCExactNestedClass", "SwiftExactNestedClass", true)
    class ExactNestedClass {
        var nestedValue: Int = 1
    }

    override var someValue: Int = 0
    override fun Int.someFunction(param: Int): Int = this * param
}

@ObjCName("ObjCNameC3", "SwiftNameC3", true)
class ObjCNameC3 {
    @ObjCName("ObjCNestedClass", "SwiftNestedClass")
    class NestedClass {
        var nestedValue: Int = 2
    }
}

private interface ObjCNameI2 {
    fun @receiver:ObjCName("objCReceiver") Int.foo(@ObjCName("objCParam") param: Int): Int
}

class ObjCNameC4: ObjCNameI2 {
    override fun Int.foo(param: Int): Int = this * param
}

@ObjCName("ObjCNameObjCObject", "ObjCNameSwiftObject")
object ObjCNameKotlinObject

@ObjCName("ObjCNameObjCEnum", "ObjCNameSwiftEnum")
enum class ObjCNameKotlinEnum {
    @ObjCName("objcOne", "swiftOne")
    KOTLIN_ONE,
    @ObjCName("objcTwo", "companion")
    kotlinTwo,
    @ObjCName("objcThree", "swiftThree")
    KotlinThree;

    companion object {
        fun foo(): Int = 0
    }
}

class ObjCAvoidPreprocessorName(@property:ObjCName("time") @ObjCName("time") val __TIME__: Int)
