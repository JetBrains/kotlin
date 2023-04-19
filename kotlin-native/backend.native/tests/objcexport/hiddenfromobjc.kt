/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hiddenfromobjc

import kotlin.experimental.ExperimentalObjCRefinement

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
data class ClassNotAvailableInSwift(val param: String)

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

interface ChildOfUnavailableInterface : InterfaceNotAvailableInSwift {
    fun g(): String
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

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
interface ChildOfChildOfUnavailableInterface : ChildOfUnavailableInterface {
    fun h(): String
}

fun createChildOfChildOfUnavailableInterface(): ChildOfChildOfUnavailableInterface {
    return object : ChildOfChildOfUnavailableInterface {
        override fun f(): String = "f"

        override fun g(): String = "g"

        override fun h(): String = "h"
    }
}

fun useOfChildOfChildOfUnavailableInterface(param: ChildOfChildOfUnavailableInterface): String {
    return param.h()
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

class ImplementsHiddenInterface : InterfaceNotAvailableInSwift {
    override fun f(): String =
            "ImplementsHiddenInterface::f"
}

// Partially hidden hierarchy of interfaces (Phhi)

interface PhhiA {
    fun a(): String
}

fun callA(param: PhhiA): String =
    param.a()

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
interface PhhiB {
    fun b(): String
}

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
interface PhhiC : PhhiA, PhhiB {
    fun c(): String
}

interface PhhiD : PhhiA {
    fun d(): String
}

fun callD(param: PhhiD): String =
    param.d()

interface PhhiE : PhhiB, PhhiA {
    fun e(): String
}

fun callE(param: PhhiE): String =
    param.e()

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
interface PhhiF : PhhiD, PhhiC, PhhiE {
    fun f(): String
}

class PhhiClass : PhhiF {
    override fun a(): String = "class::a"

    override fun b(): String = "class::b"

    override fun c(): String = "class::c"

    override fun d(): String = "class::d"

    override fun e(): String = "class::e"

    override fun f(): String = "class::f"
}

// TODO:
// 1. Use unavailable class as a parameter/return type.
// 2. Use unavailable interface as a parameter/return type.
// 3. Enum, sealed class, object.
// 4. GENERICS 😱.
// 5. Inheritance from class.
// 6. Inheritance from interface.
// 7. Mixed chain of available-unavailable.
// 8. Pass object of unavailable class consume(produce()).
// 9. Pass incorrect object to erased parameter.