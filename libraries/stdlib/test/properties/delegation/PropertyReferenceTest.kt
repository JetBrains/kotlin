/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.properties.delegation.references

import kotlin.reflect.*
import kotlin.test.*
import kotlin.internal.*
import kotlin.native.concurrent.ThreadLocal
import kotlin.random.*

open class Data(val stringVal: String, var intVar: Int, var builderVar: StringBuilder = StringBuilder("default"))
val Data.formattedVal: String get() = "Hello $stringVal"
var Data.displacedVar: Int
    get() = intVar + 2
    set(value) { intVar = value - 2 }

@ThreadLocal
val data = Data("bound string", 42)

val topVal: Double get() = 3.14
@ThreadLocal
var topVar: ULong = 0xFFFFUL

// top-level properties

// val to bound val
@ThreadLocal
val tlValBoundVal by data::stringVal
// val to bound var
@ThreadLocal
val tlValBoundVar by data::intVar
// var to bound var
@ThreadLocal
var tlVarBoundVar by data::intVar

// val to top-level val
@ThreadLocal
val tlValTopLevelVal by ::topVal
// val to top-level var
@ThreadLocal
val tlValTopLevelVar by ::topVar
// var to top-level var
@ThreadLocal
var tlVarTopLevelVar by ::topVar

// member properties
class DataExt : Data("member string", -1) {

    // val to top-level val
    val valTopLevelVal by ::topVal

    // val to top-level var
    val valTopLevelVar by ::topVar

    // var to top-level var
    var varTopLevelVar by ::topVar


    // val to bound val
    val valBoundVal by this::stringVal

    // val to bound var
    val valBoundVar by data::intVar

    // var to bound var
    var varBoundVar by ::intVar


    // val to extension val
    val valExtVal by Data::formattedVal

    // val to extension var
    val valExtVar by Data::displacedVar

    // var to extension var
    var varExtVar by Data::displacedVar
}


// extension properties
// val to bound val
@ThreadLocal
val Data.extValBoundVal by data::stringVal
// val to bound var
@ThreadLocal
val Data.extValBoundVar by data::intVar
// var to bound var
@ThreadLocal
var Data.extVarBoundVar by data::intVar

// val to top-level val
@ThreadLocal
val Data.extValTopLevelVal by ::topVal
// val to top-level var
@ThreadLocal
val Data.extValTopLevelVar by ::topVar
// var to top-level var
@ThreadLocal
var Data.extVarTopLevelVar by ::topVar

// val to member val
@ThreadLocal
val Data.extValMemberVal by Data::stringVal
// val to member var
@ThreadLocal
val Data.extValMemberVar by Data::intVar
// var to member var
@ThreadLocal
var Data.extVarMemberVar by Data::intVar

// val to extension val
@ThreadLocal
val Data.extValExtVal by Data::formattedVal
// val to extension var
@ThreadLocal
val Data.extValExtVar by Data::displacedVar
// var to extension var
@ThreadLocal
var Data.extVarExtVar by Data::displacedVar


// covariance
@ThreadLocal
val covariantVal: Number by ::topVal
@ThreadLocal
val Data.extCovariantVal: CharSequence by Data::builderVar


@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
class PropertyReferenceTest {

    private fun <V> checkDelegate0(delegated: KProperty0<@NoInfer V>, source: KProperty0<V>) {
        assertEquals(delegated.get(), source.get())
    }

    private fun <V> checkDelegate1(delegated: KProperty0<@NoInfer V>, source: KMutableProperty0<V>, newValue: @NoInfer V) {
        assertEquals(delegated.get(), source.get())
        source.set(newValue)
        assertEquals(newValue, source.get())
        assertEquals(newValue, delegated.get())
    }

    private fun <V> checkDelegate2(delegated: KMutableProperty0<@NoInfer V>, source: KMutableProperty0<V>, newValue1: @NoInfer V, newValue2: @NoInfer V) {
        assertEquals(delegated.get(), source.get())
        source.set(newValue1)
        assertEquals(newValue1, source.get())
        assertEquals(newValue1, delegated.get())

        delegated.set(newValue2)
        assertEquals(newValue2, source.get())
        assertEquals(newValue2, delegated.get())
    }

    private fun int(): Int = Random.nextInt()
    private fun ulong(): ULong = Random.nextULong()


    @Test
    fun topLevelProperties() {
        checkDelegate0(::tlValBoundVal, data::stringVal)
        checkDelegate1(::tlValBoundVar, data::intVar, int())
        checkDelegate2(::tlVarBoundVar, data::intVar, int(), int())

        checkDelegate0(::tlValTopLevelVal, ::topVal)
        checkDelegate1(::tlValTopLevelVar, ::topVar, ulong())
        checkDelegate2(::tlVarTopLevelVar, ::topVar, ulong(), ulong())
    }

    @Test
    fun memberProperties() {
        val local = DataExt()
        checkDelegate0(local::valBoundVal, local::stringVal)
        checkDelegate1(local::valBoundVar, data::intVar, int())
        checkDelegate2(local::varBoundVar, local::intVar, int(), int())

        checkDelegate0(local::valTopLevelVal, ::topVal)
        checkDelegate1(local::valTopLevelVar, ::topVar, ulong())
        checkDelegate2(local::varTopLevelVar, ::topVar, ulong(), ulong())

        checkDelegate0(local::valExtVal, local::formattedVal)
        checkDelegate1(local::valExtVar, local::displacedVar, int())
        checkDelegate2(local::varExtVar, local::displacedVar, int(), int())
    }

    @Test
    fun extensionProperties() {
        val local = Data("ext", Int.MAX_VALUE)

        checkDelegate0(local::extValBoundVal, data::stringVal)
        checkDelegate1(local::extValBoundVar, data::intVar, int())
        checkDelegate2(local::extVarBoundVar, data::intVar, int(), int())

        checkDelegate0(local::extValMemberVal, local::stringVal)
        checkDelegate1(local::extValMemberVar, local::intVar, int())
        checkDelegate2(local::extVarMemberVar, local::intVar, int(), int())

        checkDelegate0(local::extValTopLevelVal, ::topVal)
        checkDelegate1(local::extValTopLevelVar, ::topVar, ulong())
        checkDelegate2(local::extVarTopLevelVar, ::topVar, ulong(), ulong())

        checkDelegate0(local::extValExtVal, local::formattedVal)
        checkDelegate1(local::extValExtVar, local::displacedVar, int())
        checkDelegate2(local::extVarExtVar, local::displacedVar, int(), int())
    }

    @Test
    fun covariantProperties() {
        checkDelegate0<Number>(::covariantVal, ::topVal)
        checkDelegate0<CharSequence>(data::extCovariantVal, data::builderVar)
    }

}