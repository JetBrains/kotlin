/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.test

import kotlin.metadata.*
import org.junit.Test
import kotlin.reflect.KMutableProperty0
import kotlin.test.*

@Suppress("DEPRECATION", "DEPRECATION_ERROR") // flags will become internal eventually
class FlagDelegatesTest {
    private class Private

    public class Public {
        public fun f(): String = ""
    }

    @Test
    fun testVisibilityFlags() {
        val p1 = Private::class.java.readMetadataAsKmClass()
        val p2 = Public::class.java.readMetadataAsKmClass()

        assertEquals(Visibility.PRIVATE, p1.visibility)
        assertEquals(Visibility.PUBLIC, p2.visibility)

        assertTrue(Flag.IS_PRIVATE(p1.flags))
        assertTrue(Flag.IS_PUBLIC(p2.flags))

        p1.visibility = Visibility.PUBLIC
        p2.visibility = Visibility.INTERNAL

        assertFalse(Flag.IS_PRIVATE(p1.flags))
        assertFalse(Flag.IS_PUBLIC(p2.flags))

        assertTrue(Flag.IS_PUBLIC(p1.flags))
        assertTrue(Flag.IS_INTERNAL(p2.flags))

        val f = assertNotNull(p2.functions.find { it.name == "f" })
        assertEquals(Visibility.PUBLIC, f.visibility)
        f.visibility = Visibility.PRIVATE
        assertFalse(Flag.IS_PUBLIC(f.flags))
        assertTrue(Flag.IS_PRIVATE(f.flags))
    }

    @Test
    fun testBooleanFlags() {
        val klass = Public::class.java.readMetadataAsKmClass()
        fun doTest(prop: KMutableProperty0<Boolean>, flags: () -> Int, rawFlag: Flag) {
            assertFalse(prop.get())
            assertFalse(rawFlag(flags()))

            prop.set(true)

            assertTrue(prop.get())
            assertTrue(rawFlag(flags()))
        }


        doTest(klass::isData, klass::flags, Flag.Class.IS_DATA)
        val f = klass.functions.single { it.name == "f" }
        doTest(f::isOperator, f::flags, Flag.Function.IS_OPERATOR)
        val rt = f.returnType
        doTest(rt::isNullable, rt::flags, Flag.Type.IS_NULLABLE)
    }

    @Suppress("UNUSED_PARAMETER")
    class PropertiesContainer {
        val defaultVal: String = ""

        var defaultVar: String = ""

        val getterVal: String get() = "foo"

        var getterSetterFieldVar: String = ""
            get() = field + "a"
            set(param) {
                field = param + "b"
            }

        var getterSetterNoFieldVar: String
            get() = ""
            set(param) {
                // no
            }

        var getterSetterNoFieldNoParamVar: String
            get() = ""
            set(_) {
                // no
            }

        var defaultSetterVar: String = ""
            get() = "foo"

        var defaultGetterVar: String = ""
            set(param) {
                field = param.takeLast(0)
            }

        inline var noinlineModifierVar: () -> String
            get() = { "" }
            set(noinline param) {
                param()
            }

    }

    @Test
    fun testPropertyAccessors() {
        val klass = PropertiesContainer::class.java.readMetadataAsKmClass()
        val propMap = klass.properties.associateBy { it.name }

        fun assertProperty(
            name: String,
            isVarProp: Boolean,
            getterNotDefault: Boolean,
            setterNotDefault: Boolean?,
            setterParamCheck: (KmValueParameter?) -> Unit = { assertNull(it, "Should not be a setter parameter for $name") },
        ) {
            with(propMap.getValue(name)) {
                assertEquals(listOf(isVarProp, true, isVarProp), listOf(isVar, hasGetter, hasSetter), "for $name")
                assertEquals(visibility, getter.visibility, "for $name")
                assertEquals(getterNotDefault, getter.isNotDefault, "for $name")

                assertEquals(isVarProp, setter != null, "for $name")
                assertEquals(setterNotDefault, setter?.isNotDefault, "for $name")
                assertEquals(if (isVarProp) visibility else null, setter?.visibility, "for $name")

                setterParamCheck(setterParameter)
            }
        }

        assertProperty("defaultVal", false, false, null)
        assertProperty("defaultVar", true, false, false)
        assertProperty("getterVal", false, true, null)

        assertProperty("getterSetterFieldVar", true, true, true) {
            assertEquals("param", it?.name)
        }

        // Same as with field
        assertProperty("getterSetterNoFieldVar", true, true, true) {
            assertEquals("param", it?.name)
        }

        assertProperty("getterSetterNoFieldNoParamVar", true, true, true) {
            assertEquals(true, it?.name == "_") // KT-62582 (K2 should have _ here despite a special name is used in K1)
            // K1 version
            // assertEquals(true, it?.name?.contains("anonymous parameter"))
        }

        assertProperty("defaultSetterVar", true, true, false)
        assertProperty("defaultGetterVar", true, false, true) {
            assertEquals("param", it?.name)
        }

        assertProperty("noinlineModifierVar", true, true, true) {
            assertEquals("param", it?.name)
            assertEquals(true, it?.isNoinline)
        }

    }

    class X {

        val x2 = 2
        fun x22() = 2
        val x3 = x22()

        companion object Y {
            val y2 = 2
            const val y3 = 3
        }
    }

    @Test
    fun testHasConstantExample() {
        class X {
            val a = 1
            val b = a

            fun x() = 2
            val c = x()
        }

        val props = X::class.java.readMetadataAsKmClass().properties.associateBy { it.name }
        props.values.forEach { assertFalse(it.isConst, it.name) }
        assertTrue(props.getValue("a").hasConstant)
        assertFalse(props.getValue("b").hasConstant)
        assertFalse(props.getValue("c").hasConstant)
    }

    interface I {
        val x: Int
    }

    class Foo(i: I) : I by i {
        val props: Map<String, Int> = mapOf()

        val y: Int by props
    }

    @Test
    fun testDelegation() {
        val foo = Foo::class.java.readMetadataAsKmClass()
        val props = foo.properties.associateBy { it.name }
        with(props["x"]!!) {
            //assertEquals(MemberKind.DELEGATION, kind) // TODO: KT-62581 (uncomment after bootstrapping, remove TODO)
            assertFalse(isDelegated)
            assertFalse(getter.isNotDefault)
        }
        with(props["y"]!!) {
            assertEquals(MemberKind.DECLARATION, kind)
            assertTrue(isDelegated)
            assertTrue(getter.isNotDefault)
        }
    }
}
