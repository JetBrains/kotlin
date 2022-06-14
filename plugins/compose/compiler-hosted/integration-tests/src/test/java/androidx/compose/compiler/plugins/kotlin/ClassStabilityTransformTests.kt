/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.statements
import org.junit.Test

class ClassStabilityTransformTests : ComposeIrTransformTest() {

    @Test
    fun testEmptyClassIsStable() = assertStability(
        "class Foo",
        "Stable"
    )

    @Test
    fun testSingleValPrimitivePropIsStable() = assertStability(
        "class Foo(val value: Int)",
        "Stable"
    )

    @Test
    fun testSingleVarPrimitivePropIsUnstable() = assertStability(
        "class Foo(var value: Int)",
        "Unstable"
    )

    @Test
    fun testSingleValTypeParamIsStableIfParamIs() = assertStability(
        "class Foo<T>(val value: T)",
        "Parameter(T)"
    )

    @Test
    fun testSingleVarTypeParamIsUnstable() = assertStability(
        "class Foo<T>(var value: T)",
        "Unstable"
    )

    @Test
    fun testNonCtorVarIsUnstable() = assertStability(
        "class Foo(value: Int) { var value: Int = value }",
        "Unstable"
    )

    @Test
    fun testNonCtorValIsStable() = assertStability(
        "class Foo(value: Int) { val value: Int = value }",
        "Stable"
    )

    @Test
    fun testMutableStateDelegateVarIsStable() = assertStability(
        "class Foo(value: Int) { var value by mutableStateOf(value) }",
        "Stable"
    )

    @Test
    fun testLazyValIsUnstable() = assertStability(
        "class Foo(value: Int) { val square by lazy { value * value } }",
        "Unstable"
    )

    @Test
    fun testNonFieldCtorParamDoesNotImpactStability() = assertStability(
        "class Foo<T>(val a: Int, b: T) { val c: Int = b.hashCode() }",
        "Stable"
    )

    @Test
    fun testNonBackingFieldUnstableVarIsStable() = assertStability(
        """
            class Foo {
                var p1: Unstable
                    get() { TODO() }
                    set(value) { }
            }
        """,
        "Stable"
    )

    @Test
    fun testNonBackingFieldUnstableValIsStable() = assertStability(
        """
            class Foo {
                val p1: Unstable
                    get() { return Unstable() }
            }
        """,
        "Stable"
    )

    @Test
    fun testTypeParameterWithNonExactBackingFieldType() = assertStability(
        "class Foo<T>(val a: List<T>)",
        "Unstable"
    )

    @Test
    fun testTypeParamNonExactCtorParamExactBackingFields() = assertStability(
        "class Foo<T>(a: List<T>) { val b = a.first(); val c = a.last() }",
        "Parameter(T),Parameter(T)"
    )

    @Test
    fun testInterfacesAreUncertain() = assertStability(
        "interface Foo",
        "Uncertain(Foo)"
    )

    @Test
    fun testInterfaceWithStableValAreUncertain() = assertStability(
        "interface Foo { val value: Int }",
        "Uncertain(Foo)"
    )

    @Test
    fun testCrossModuleTypesResultInRuntimeStability() = assertStability(
        """
            class A
            class B
            class C
        """,
        "class D(val a: A, val v: B, val C: C)",
        "Runtime(A),Runtime(B),Runtime(C)"
    )

    @Test
    fun testStable17() = assertStability(
        """
            class Counter {
                private var count: Int = 0
                fun getCount(): Int = count
                fun increment() { count++ }
            }
        """,
        "Unstable"
    )

    @Test
    fun testProtobufLiteTypesAreStable() = assertStability(
        """
            class Foo(val x: androidx.compose.compiler.plugins.StabilityTestProtos.SampleProto)
        """,
        "Stable"
    )

    @Test
    fun testPairIsStableIfItsTypesAre() = assertStability(
        """
            class Foo<T, V>(val x: Pair<T, V>)
        """,
        "Parameter(T),Parameter(V)"
    )

    @Test
    fun testPairOfCrossModuleTypesAreRuntimeStable() = assertStability(
        """
            class A
            class B
        """,
        "class Foo(val x: Pair<A, B>)",
        "Runtime(A),Runtime(B)"
    )

    @Test
    fun testGuavaImmutableListIsStableIfItsTypesAre() = assertStability(
        """
            class Foo<T>(val x: com.google.common.collect.ImmutableList<T>)
        """,
        "Parameter(T)"
    )

    @Test
    fun testGuavaImmutableListCrossModuleTypesAreRuntimeStable() = assertStability(
        """
            class A
        """,
        """
            class Foo(val x: com.google.common.collect.ImmutableList<A>)
        """,
        "Runtime(A)"
    )

    @Test
    fun testGuavaImmutableSetIsStableIfItsTypesAre() = assertStability(
        """
            class Foo<T>(val x: com.google.common.collect.ImmutableSet<T>)
        """,
        "Parameter(T)"
    )

    @Test
    fun testGuavaImmutableSetCrossModuleTypesAreRuntimeStable() = assertStability(
        """
            class A
        """,
        """
            class Foo(val x: com.google.common.collect.ImmutableSet<A>)
        """,
        "Runtime(A)"
    )

    @Test
    fun testGuavaImmutableMapIsStableIfItsTypesAre() = assertStability(
        """
            class Foo<K, V>(val x: com.google.common.collect.ImmutableMap<K, V>)
        """,
        "Parameter(K),Parameter(V)"
    )

    @Test
    fun testGuavaImmutableMapCrossModuleTypesAreRuntimeStable() = assertStability(
        """
            class A
            class B
        """,
        """
            class Foo(val x: com.google.common.collect.ImmutableMap<A, B>)
        """,
        "Runtime(A),Runtime(B)"
    )

    @Test
    fun testKotlinxImmutableListIsStableIfItsTypesAre() = assertStability(
        """
            class Foo<T>(val x: kotlinx.collections.immutable.ImmutableList<T>)
        """,
        "Parameter(T)"
    )

    @Test
    fun testKotlinxImmutableListCrossModuleTypesAreRuntimeStable() = assertStability(
        """
            class A
        """,
        """
            class Foo(val x: kotlinx.collections.immutable.ImmutableList<A>)
        """,
        "Runtime(A)"
    )

    @Test
    fun testKotlinxImmutableSetIsStableIfItsTypesAre() = assertStability(
        """
            class Foo<T>(val x: kotlinx.collections.immutable.ImmutableSet<T>)
        """,
        "Parameter(T)"
    )

    @Test
    fun testKotlinxImmutableSetCrossModuleTypesAreRuntimeStable() = assertStability(
        """
            class A
        """,
        """
            class Foo(val x: kotlinx.collections.immutable.ImmutableSet<A>)
        """,
        "Runtime(A)"
    )

    @Test
    fun testKotlinxImmutableMapIsStableIfItsTypesAre() = assertStability(
        """
            class Foo<K, V>(val x: kotlinx.collections.immutable.ImmutableMap<K, V>)
        """,
        "Parameter(K),Parameter(V)"
    )

    @Test
    fun testKotlinxImmutableMapCrossModuleTypesAreRuntimeStable() = assertStability(
        """
            class A
            class B
        """,
        """
            class Foo(val x: kotlinx.collections.immutable.ImmutableMap<A, B>)
        """,
        "Runtime(A),Runtime(B)"
    )

    @Test
    fun testVarPropDelegateWithCrossModuleStableDelegateTypeIsStable() = assertStability(
        """
            @Stable
            class StableDelegate {
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }
        """,
        "class Foo { var p1 by StableDelegate() }",
        "Stable"
    )

    @Test
    fun testVarPropDelegateWithStableDelegateTypeIsStable() = assertStability(
        """
        @Stable
        class StableDelegate {
            operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
            }
            operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                return 10
            }
        }
        class Foo { var p1 by StableDelegate() }
        """,
        "Stable"
    )

    @Test
    fun testVarPropDelegateOfInferredStableDelegate() = assertStability(
        """
        class StableDelegate {
            operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
            }
            operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                return 10
            }
        }
        class Foo { var p1 by StableDelegate() }
        """,
        "Stable"
    )

    @Test
    fun testVarPropDelegateOfCrossModuleInferredStableDelegate() = assertStability(
        """
            class StableDelegate {
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }
        """,
        "class Foo { var p1 by StableDelegate() }",
        "Runtime(StableDelegate)"
    )

    @Test
    fun testStableDelegateWithTypeParamButNoBackingFieldDoesntDependOnReturnType() =
        assertStability(
            """
                class StableDelegate<T> {
                    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: T) {
                    }
                    operator fun getValue(thisObj: Any?, property: KProperty<*>): T {
                        error("")
                    }
                }
                class Bar
            """,
            "class Foo { var p1 by StableDelegate<Bar>() }",
            "Runtime(StableDelegate)"
        )

    @Test
    fun testStable26() = assertStability(
        """
            class A
            class B
            class C
        """,
        """
            class Foo(a: A, b: B, c: C) {
                var a by mutableStateOf(a)
                var b by mutableStateOf(b)
                var c by mutableStateOf(c)
            }
        """,
        "Stable"
    )

    @Test
    fun testExplicitlyMarkedStableTypesAreStable() = assertStability(
        """
            @Stable
            class A
        """,
        """
            class Foo(val a: A)
        """,
        "Stable"
    )

    @Test
    fun testListOfCallWithPrimitiveTypeIsStable() = assertStability(
        "",
        "",
        "listOf(1)",
        "Stable"
    )

    @Test
    fun testListOfCallWithLocalInferredStableTypeIsStable() = assertStability(
        "",
        "class Foo",
        "listOf(Foo())",
        "Stable"
    )

    @Test
    fun testListOfCallWithExternalInferredStableTypeIsRuntimeStable() = assertStability(
        "class Foo",
        "",
        "listOf(Foo())",
        "Runtime(Foo)"
    )

    @Test
    fun testMapOfCallWithPrimitiveTypesIsStable() = assertStability(
        "",
        "",
        "mapOf(1 to 1)",
        "Stable,Stable"
    )

    @Test
    fun testMapOfCallWithStableTypeIsStable() = assertStability(
        "",
        """
            class A
            class B
        """,
        "mapOf(A() to B())",
        "Stable,Stable"
    )

    @Test
    fun testMapOfCallWithExternalInferredStableTypeIsRuntimeStable() = assertStability(
        """
            class A
            class B
        """,
        "",
        "mapOf(A() to B())",
        "Runtime(A),Runtime(B)"
    )

    @Test
    fun testSetOfCallWithPrimitiveTypesIsStable() = assertStability(
        "",
        "",
        "setOf(1)",
        "Stable"
    )

    @Test
    fun testSetOfCallWithStableTypeIsStable() = assertStability(
        "",
        """
            class A
        """,
        "setOf(A())",
        "Stable"
    )

    @Test
    fun testSetOfCallWithExternalInferredStableTypeIsRuntimeStable() = assertStability(
        """
            class A
        """,
        "",
        "setOf(A())",
        "Runtime(A)"
    )

    @Test
    fun testEmptyClass() = assertTransform(
        """
            class Foo
        """,
        """
            @StabilityInferred(parameters = 0)
            class Foo {
              static val %stable: Int = 0
            }
        """
    )

    @Test
    fun testStabilityTransformOfVariousTypes() = assertTransform(
        """
            import androidx.compose.runtime.Stable
            import kotlin.reflect.KProperty

            @Stable
            class StableDelegate {
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }

            class UnstableDelegate {
                var value: Int = 0
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                    this.value = value
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }
            class Unstable { var value: Int = 0 }
            class EmptyClass
            class SingleStableVal(val p1: Int)
            class SingleParamProp<T>(val p1: T)
            class SingleParamNonProp<T>(p1: T) { val p2 = p1.hashCode() }
            class DoubleParamSingleProp<T, V>(val p1: T, p2: V) { val p3 = p2.hashCode() }
            class X<T>(val p1: List<T>)
            class NonBackingFieldUnstableProp {
                val p1: Unstable get() { TODO() }
            }
            class NonBackingFieldUnstableVarProp {
                var p1: Unstable
                    get() { TODO() }
                    set(value) { }
            }
            class StableDelegateProp {
                var p1 by StableDelegate()
            }
            class UnstableDelegateProp {
                var p1 by UnstableDelegate()
            }
        """,
        """
            @Stable
            class StableDelegate {
              fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) { }
              fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                return 10
              }
            }
            @StabilityInferred(parameters = 0)
            class UnstableDelegate {
              var value: Int = 0
              fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                value = value
              }
              fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                return 10
              }
              static val %stable: Int = 8
            }
            @StabilityInferred(parameters = 0)
            class Unstable {
              var value: Int = 0
              static val %stable: Int = 8
            }
            @StabilityInferred(parameters = 0)
            class EmptyClass {
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class SingleStableVal(val p1: Int) {
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 1)
            class SingleParamProp<T> (val p1: T) {
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class SingleParamNonProp<T> (p1: T) {
              val p2: Int = p1.hashCode()
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 1)
            class DoubleParamSingleProp<T, V> (val p1: T, p2: V) {
              val p3: Int = p2.hashCode()
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class X<T> (val p1: List<T>) {
              static val %stable: Int = 8
            }
            @StabilityInferred(parameters = 0)
            class NonBackingFieldUnstableProp {
              val p1: Unstable
                get() {
                  TODO()
                }
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class NonBackingFieldUnstableVarProp {
              var p1: Unstable
                get() {
                  TODO()
                }
                set(value) {
                }
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class StableDelegateProp {
              var p1: StableDelegate = StableDelegate()
                get() {
                  return <this>.p1%delegate.getValue()
                }
                set(value) {
                  return <this>.p1%delegate.setValue()
                }
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class UnstableDelegateProp {
              var p1: UnstableDelegate = UnstableDelegate()
                get() {
                  return <this>.p1%delegate.getValue()
                }
                set(value) {
                  return <this>.p1%delegate.setValue()
                }
              static val %stable: Int = 8
            }
        """
    )

    @Test
    fun testStabilityPropagationOfVariousTypes() = verifyCrossModuleComposeIrTransform(
        """
            package a
            import androidx.compose.runtime.Stable
            import kotlin.reflect.KProperty

            @Stable
            class StableDelegate {
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }

            class UnstableDelegate {
                var value: Int = 0
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                    this.value = value
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }
            class UnstableClass {
                var value: Int = 0
            }
            class StableClass

            class EmptyClass
            class SingleStableValInt(val p1: Int)
            class SingleStableVal(val p1: StableClass)
            class SingleParamProp<T>(val p1: T)
            class SingleParamNonProp<T>(p1: T) { val p2 = p1.hashCode() }
            class DoubleParamSingleProp<T, V>(val p1: T, p2: V) { val p3 = p2.hashCode() }
            class X<T>(val p1: List<T>)
            class NonBackingFieldUnstableVal {
                val p1: UnstableClass get() { TODO() }
            }
            class NonBackingFieldUnstableVar {
                var p1: UnstableClass
                    get() { TODO() }
                    set(value) { }
            }
            class StableDelegateProp {
                var p1 by StableDelegate()
            }
            class UnstableDelegateProp {
                var p1 by UnstableDelegate()
            }
            fun used(x: Any?) {}
        """,
        """
            import a.*
            import androidx.compose.runtime.Composable

            @Composable fun A(y: Any? = null) {
                used(y)
                A()
                A(EmptyClass())
                A(SingleStableValInt(123))
                A(SingleStableVal(StableClass()))
                A(SingleParamProp(StableClass()))
                A(SingleParamProp(UnstableClass()))
                A(SingleParamNonProp(StableClass()))
                A(SingleParamNonProp(UnstableClass()))
                A(DoubleParamSingleProp(StableClass(), StableClass()))
                A(DoubleParamSingleProp(UnstableClass(), StableClass()))
                A(DoubleParamSingleProp(StableClass(), UnstableClass()))
                A(DoubleParamSingleProp(UnstableClass(), UnstableClass()))
                A(X(listOf(StableClass())))
                A(X(listOf(StableClass())))
                A(NonBackingFieldUnstableVal())
                A(NonBackingFieldUnstableVar())
                A(StableDelegateProp())
                A(UnstableDelegateProp())
            }
        """,
        """
            @Composable
            fun A(y: Any?, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<A()>,<A(Empt...>,<A(Sing...>,<A(Sing...>,<A(Sing...>,<A(Sing...>,<A(Sing...>,<A(Sing...>,<A(Doub...>,<A(Doub...>,<A(Doub...>,<A(Doub...>,<A(X(li...>,<A(X(li...>,<A(NonB...>,<A(NonB...>,<A(Stab...>,<A(Unst...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0010
              }
              if (%default and 0b0001 !== 0b0001 || %dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  y = null
                }
                used(y)
                A(null, %composer, 0, 0b0001)
                A(EmptyClass(), %composer, EmptyClass.%stable, 0)
                A(SingleStableValInt(123), %composer, SingleStableValInt.%stable, 0)
                A(SingleStableVal(StableClass()), %composer, SingleStableVal.%stable, 0)
                A(SingleParamProp(StableClass()), %composer, SingleParamProp.%stable or StableClass.%stable, 0)
                A(SingleParamProp(UnstableClass()), %composer, SingleParamProp.%stable or UnstableClass.%stable, 0)
                A(SingleParamNonProp(StableClass()), %composer, SingleParamNonProp.%stable, 0)
                A(SingleParamNonProp(UnstableClass()), %composer, SingleParamNonProp.%stable, 0)
                A(DoubleParamSingleProp(StableClass(), StableClass()), %composer, DoubleParamSingleProp.%stable or StableClass.%stable, 0)
                A(DoubleParamSingleProp(UnstableClass(), StableClass()), %composer, DoubleParamSingleProp.%stable or UnstableClass.%stable, 0)
                A(DoubleParamSingleProp(StableClass(), UnstableClass()), %composer, DoubleParamSingleProp.%stable or StableClass.%stable, 0)
                A(DoubleParamSingleProp(UnstableClass(), UnstableClass()), %composer, DoubleParamSingleProp.%stable or UnstableClass.%stable, 0)
                A(X(listOf(StableClass())), %composer, X.%stable, 0)
                A(X(listOf(StableClass())), %composer, X.%stable, 0)
                A(NonBackingFieldUnstableVal(), %composer, NonBackingFieldUnstableVal.%stable, 0)
                A(NonBackingFieldUnstableVar(), %composer, NonBackingFieldUnstableVar.%stable, 0)
                A(StableDelegateProp(), %composer, StableDelegateProp.%stable, 0)
                A(UnstableDelegateProp(), %composer, UnstableDelegateProp.%stable, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(y, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testStabilityPropagationOfVariousTypesInSameModule() = verifyCrossModuleComposeIrTransform(
        """
            package a
            import androidx.compose.runtime.Stable
            import kotlin.reflect.KProperty

            @Stable
            class StableDelegate {
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }

            class UnstableDelegate {
                var value: Int = 0
                operator fun setValue(thisObj: Any?, property: KProperty<*>, value: Int) {
                    this.value = value
                }
                operator fun getValue(thisObj: Any?, property: KProperty<*>): Int {
                    return 10
                }
            }
            class UnstableClass {
                var value: Int = 0
            }
            class StableClass

            class EmptyClass
            class SingleStableValInt(val p1: Int)
            class SingleStableVal(val p1: StableClass)
            class SingleParamProp<T>(val p1: T)
            class SingleParamNonProp<T>(p1: T) { val p2 = p1.hashCode() }
            class DoubleParamSingleProp<T, V>(val p1: T, p2: V) { val p3 = p2.hashCode() }
            class NonBackingFieldUnstableVal {
                val p1: UnstableClass get() { TODO() }
            }
            class NonBackingFieldUnstableVar {
                var p1: UnstableClass
                    get() { TODO() }
                    set(value) { }
            }
            fun used(x: Any?) {}
        """,
        """
            import a.*
            import androidx.compose.runtime.Composable

            class X<T>(val p1: List<T>)
            class StableDelegateProp {
                var p1 by StableDelegate()
            }
            class UnstableDelegateProp {
                var p1 by UnstableDelegate()
            }
            @Composable fun A(y: Any) {
                used(y)
                A(X(listOf(StableClass())))
                A(StableDelegateProp())
                A(UnstableDelegateProp())
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class X<T> (val p1: List<T>) {
              static val %stable: Int = 8
            }
            @StabilityInferred(parameters = 0)
            class StableDelegateProp {
              var p1: StableDelegate = StableDelegate()
                get() {
                  return <this>.p1%delegate.getValue()
                }
                set(value) {
                  return <this>.p1%delegate.setValue()
                }
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class UnstableDelegateProp {
              var p1: UnstableDelegate = UnstableDelegate()
                get() {
                  return <this>.p1%delegate.getValue()
                }
                set(value) {
                  return <this>.p1%delegate.setValue()
                }
              static val %stable: Int = UnstableDelegate.%stable
            }
            @Composable
            fun A(y: Any, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<A(X(li...>,<A(Stab...>,<A(Unst...>:Test.kt")
              used(y)
              A(X(listOf(StableClass())), %composer, 0b1000)
              A(StableDelegateProp(), %composer, 0)
              A(UnstableDelegateProp(), %composer, UnstableDelegate.%stable)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(y, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testEmptyClassAcrossModules() = verifyCrossModuleComposeIrTransform(
        """
            package a
            class Wrapper<T>(value: T) {
              fun make(): T = error("")
            }
            class Foo
            fun used(x: Any?) {}
        """,
        """
            import a.*
            import androidx.compose.runtime.Composable

            @Composable fun A(y: Any) {
                used(y)
                A(Wrapper(Foo()))
            }
        """,
        """
            @Composable
            fun A(y: Any, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)<A(Wrap...>:Test.kt")
              used(y)
              A(Wrapper(Foo()), %composer, Wrapper.%stable)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(y, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testLocalParameterBasedTypeParameterSubstitution() = verifyCrossModuleComposeIrTransform(
        """
            package a
            import androidx.compose.runtime.Composable
            class Wrapper<T>(val value: T)
            @Composable fun A(y: Any) {}
        """,
        """
            import a.*
            import androidx.compose.runtime.Composable

            @Composable fun <V> B(value: V) {
                A(Wrapper(value))
            }
            @Composable fun <T> X(items: List<T>, itemContent: @Composable (T) -> Unit) {
                for (item in items) itemContent(item)
            }
            @Composable fun C(items: List<String>) {
                X(items) { item ->
                    A(item)
                    A(Wrapper(item))
                }
            }
        """,
        """
            @Composable
            fun <V> B(value: V, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B)<A(Wrap...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(value)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                A(Wrapper(value), %composer, Wrapper.%stable or 0b1000 and %dirty)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(value, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun <T> X(items: List<T>, itemContent: Function3<T, Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(X)P(1)*<itemCo...>:Test.kt")
              val %dirty = %changed
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val item = tmp0_iterator.next()
                itemContent(item, %composer, 0b01110000 and %dirty)
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                X(items, itemContent, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun C(items: List<String>, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(C)<X(item...>:Test.kt")
              X(items, ComposableSingletons%TestKt.lambda-1, %composer, 0b00111000)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                C(items, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function3<String, Composer, Int, Unit> = composableLambdaInstance(<>, false) { item: String, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<A(item...>,<A(Wrap...>:Test.kt")
                val %dirty = %changed
                if (%changed and 0b1110 === 0) {
                  %dirty = %dirty or if (%composer.changed(item)) 0b0100 else 0b0010
                }
                if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                  A(item, %composer, 0b1110 and %dirty)
                  A(Wrapper(item), %composer, Wrapper.%stable or 0)
                } else {
                  %composer.skipToGroupEnd()
                }
              }
            }
        """
    )

    @Test
    fun testSingleVarVersusValProperty() = assertTransform(
        """
            class Stable(val bar: Int)
            class Unstable(var bar: Int)
        """,
        """
            @StabilityInferred(parameters = 0)
            class Stable(val bar: Int) {
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class Unstable(var bar: Int) {
              static val %stable: Int = 8
            }
        """
    )

    @Test
    fun testComposableCall() = assertTransform(
        """
            import androidx.compose.runtime.Composable

            class Foo
            @Composable fun A(y: Int, x: Any) {
                used(y)
                B(x)
            }
            @Composable fun B(x: Any) {
                used(x)
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class Foo {
              static val %stable: Int = 0
            }
            @Composable
            fun A(y: Int, x: Any, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)P(1)<B(x)>:Test.kt")
              used(y)
              B(x, %composer, 0b1000)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(y, x, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun B(x: Any, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              used(x)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(x, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testComposableCallWithUnstableFinalClassInSameModule() = assertTransform(
        """
            import androidx.compose.runtime.Composable

            class Foo(var bar: Int = 0)
            @Composable fun A(y: Int, x: Foo) {
                used(y)
                B(x)
            }
            @Composable fun B(x: Any) {
                used(x)
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class Foo(var bar: Int = 0) {
              static val %stable: Int = 8
            }
            @Composable
            fun A(y: Int, x: Foo, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A)P(1)<B(x)>:Test.kt")
              used(y)
              B(x, %composer, 0b1000)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(y, x, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun B(x: Any, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              used(x)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(x, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    private fun assertStability(
        @Language("kotlin")
        classDefSrc: String,
        stability: String
    ) {
        val source = """
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.setValue
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Stable
            import androidx.compose.runtime.State
            import kotlin.reflect.KProperty

            $classDefSrc

            class Unstable { var value: Int = 0 }
        """.trimIndent()

        val files = listOf(
            sourceFile("Test.kt", source.replace('%', '$'))
        )
        val irModule = JvmCompilation().compile(files)
        val irClass = irModule.files.last().declarations.first() as IrClass
        val classStability = StabilityInferencer(pluginContext!!)
            .stabilityOf(irClass.defaultType as IrType)

        assertEquals(
            stability,
            classStability.toString()
        )
    }

    private fun assertStability(
        @Language("kotlin")
        externalSrc: String,
        @Language("kotlin")
        classDefSrc: String,
        stability: String,
        dumpClasses: Boolean = false
    ) {
        val irModule = buildModule(externalSrc, classDefSrc, dumpClasses)
        val irClass = irModule.files.last().declarations.first() as IrClass
        val classStability = StabilityInferencer(pluginContext!!)
            .stabilityOf(irClass.defaultType as IrType)

        assertEquals(
            stability,
            classStability.toString()
        )
    }

    private fun assertStability(
        externalSrc: String,
        localSrc: String,
        expression: String,
        stability: String,
        dumpClasses: Boolean = false
    ) {
        val irModule = buildModule(
            externalSrc,
            """
                $localSrc

                fun TestFunction() = $expression
            """.trimIndent(),
            dumpClasses
        )
        val irTestFn = irModule
            .files
            .last()
            .declarations
            .filterIsInstance<IrSimpleFunction>()
            .first { it.name.asString() == "TestFunction" }

        val lastStatement = irTestFn.body!!.statements.last()
        val irExpr = when (lastStatement) {
            is IrReturn -> lastStatement.value
            is IrExpression -> lastStatement
            else -> error("unexpected statement: $lastStatement")
        }
        val exprStability = StabilityInferencer(pluginContext!!).stabilityOf(irExpr)

        assertEquals(
            stability,
            exprStability.toString()
        )
    }

    private fun buildModule(
        @Language("kotlin")
        externalSrc: String,
        @Language("kotlin")
        localSrc: String,
        dumpClasses: Boolean = false
    ): IrModuleFragment {
        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        val dependencyFileName = "Test_REPLACEME_${uniqueNumber++}"
        val dependencySrc = """
            package dependency
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.setValue
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Stable
            import androidx.compose.runtime.State
            import kotlin.reflect.KProperty

            class UnusedClassToEnsurePackageGetsGenerated

            $externalSrc
        """.trimIndent()

        classLoader(dependencySrc, dependencyFileName, dumpClasses)
            .allGeneratedFiles
            .also {
                // Write the files to the class directory so they can be used by the next module
                // and the application
                it.writeToDir(classesDirectory)
            }

        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        val source = """
            import dependency.*
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.setValue
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Stable
            import androidx.compose.runtime.State
            import kotlin.reflect.KProperty

            $localSrc
        """.trimIndent()

        val files = listOf(
            sourceFile("Test.kt", source.replace('%', '$'))
        )
        return JvmCompilation().compile(files)
    }

    private fun assertTransform(
        @Language("kotlin")
        checked: String,
        expectedTransformed: String,
        unchecked: String = "",
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        checked,
        expectedTransformed,
        """
            $unchecked
            fun used(x: Any?) {}
        """,
        dumpTree = dumpTree
    )
}
