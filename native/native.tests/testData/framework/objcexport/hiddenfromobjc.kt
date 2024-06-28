/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hiddenfromobjc

import kotlin.experimental.ExperimentalObjCRefinement

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
data class ClassNotAvailableInSwift(val param: String)

// KT-58839
fun ClassNotAvailableInSwift.doSomethingMeaningless(another: ClassNotAvailableInSwift): ClassNotAvailableInSwift {
    return ClassNotAvailableInSwift(this.param + another.param)
}

fun String.doSomethingMeaningless(another: ClassNotAvailableInSwift): ClassNotAvailableInSwift {
    return ClassNotAvailableInSwift(this + another.param)
}

// Check that inner and nested classes are hidden if enclosing class is hidden
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
class ParentClass {
    class NestedClass {
        class DeeplyNestedClass
    }

    inner class InnerClass
}

fun useOfUnavailableClass(param: ClassNotAvailableInSwift): ClassNotAvailableInSwift {
    return ClassNotAvailableInSwift("hi")
}

fun useOfNullableUnavailableClass(param: ClassNotAvailableInSwift?): ClassNotAvailableInSwift? {
    return null
}

fun produceUnavailable(): ClassNotAvailableInSwift {
    return ClassNotAvailableInSwift("hi")
}

fun consumeUnavailable(param: ClassNotAvailableInSwift): String {
    return param.param
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
interface InterfaceNotAvailableInSwift {
    fun f(): String
}

fun createUnavailableInterface(): InterfaceNotAvailableInSwift {
    return object : InterfaceNotAvailableInSwift {
        override fun f(): String = "I'm actually unavailable, call me later."
    }
}

fun useOfNullableUnavailableInterface(param: InterfaceNotAvailableInSwift?): String? {
    return param?.f() ?: "null"
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
enum class UnavailableEnum {
    A, B, C;
}

fun createUnavailableEnum(): UnavailableEnum {
    return UnavailableEnum.A
}

fun useOfUnavailableEnum(param: UnavailableEnum): String {
    return param.toString()
}

fun useOfNullableUnavailableEnum(param: UnavailableEnum?): String {
    return param?.toString() ?: "null"
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
object UnavailableObject {
    val field: String = "objectField"
}

fun getUnavailableObject(): UnavailableObject {
    return UnavailableObject
}

fun useOfUnavailableObject(param:UnavailableObject):String {
    return param.field
}

fun useOfNullableUnavailableObject(param:UnavailableObject?):String? {
    return param?.field ?: "null"
}


sealed class SealedClass {

    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    class A : SealedClass()

    class B : SealedClass()

    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    object C : SealedClass()
}

fun createSealedClass(): SealedClass {
    return SealedClass.A()
}

fun useSealedClass(param: SealedClass): String {
    return when (param) {
        is SealedClass.A -> "A"
        is SealedClass.B -> "B"
        SealedClass.C -> "C"
    }
}

fun <T : InterfaceNotAvailableInSwift> useUnavailable(a : T): String {
    return a.f()
}

class WrapperOverUnavailable<T: InterfaceNotAvailableInSwift>(val arg: T)