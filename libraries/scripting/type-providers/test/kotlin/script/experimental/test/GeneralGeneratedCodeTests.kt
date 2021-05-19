/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import java.io.Serializable
import kotlin.math.sqrt
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.impl.*
import kotlin.script.experimental.typeProviders.generatedCode.impl.property
import kotlin.script.experimental.typeProviders.generatedCode.implement
import kotlin.script.experimental.typeProviders.generatedCode.withParameters

class GeneralGeneratedCodeTests : GeneratedCodeTestsBase() {

    fun testRunOnStart() {
        val out = runScriptOrFail {
            runOnStart {
                print("Hello World")
            }
        }

        assertEquals(out, "Hello World")
    }

    fun testConstantInt() {
        val out = runScriptOrFail {
            constant("someConstant", 42)

            +"""
                println(someConstant)
            """.trimIndent()
        }.trim()

        assertEquals(out, "42")
    }

    fun testConstantDataClass() {

        val out = runScriptOrFail {
            constant("someConstant", TestDataClass(42, "Meaning of life"))

            +"""
                println(someConstant)
            """.trimIndent()
        }.trim()

        assertEquals(out, "TestDataClass(value=42, description=Meaning of life)")
    }

    fun testDataClass() {
        val out = runScriptOrFail {
            dataClass("MyDataClass") {
                property("myProperty", default = 42)
            }

            +"""
                val instance = MyDataClass(1337)
                println(instance)
                
                val withDefault = MyDataClass()
                println(withDefault)
            """.trimIndent()
        }.lines().filter { it.isNotBlank() }

        assertEquals(out.count(), 2)
        assertEquals(out[0], "MyDataClass(myProperty=1337)")
        assertEquals(out[1], "MyDataClass(myProperty=42)")
    }

    fun testMethodOnDataClass() {
        val out = runScriptOrFail {
            dataClass("MyDataClass") {
                property("myProperty", default = 42)

                method("add") { value: Int ->
                    val myProperty by property<Int>()
                    myProperty + value
                }
            }

            +"""
                val instance = MyDataClass()
                println(instance.add(1337))
            """.trimIndent()
        }.trim()

        assertEquals(out, "1379")
    }

    fun testExtensionMethodOnType() {
        val out = runScriptOrFail {
            extensionMethod<String, String>("weirdCase") { ->
                val chars = mapIndexed { index, char -> if (index % 2 == 0) char.toLowerCase() else char.toUpperCase() }
                chars.joinToString("")
            }

            +"""
                val string = "One Two Three"
                println(string.weirdCase())
            """.trimIndent()
        }.trim()

        assertEquals(out, "oNe tWo tHrEe")
    }

    fun testExtensionMethodReturnsWithGenericParameters() {
        val out = runScriptOrFail {
            extensionMethod<String, List<String>>("nonBlankLines") { ->
                lines().filter { it.isNotBlank() }
            }

            +"""
                val string = "One\n\n\nTwo\n\nThree\n\n"
                println(string.nonBlankLines().joinToString("\n"))
            """.trimIndent()
        }.trim()

        assertEquals(out, "One\nTwo\nThree")
    }

    fun testExtensionMethodOnTypeWithGenerics() {
        val out = runScriptOrFail {
            extensionMethod<TestClassWithGenerics<Int>, Unit>("printValue") {
                print("Value: $value")
            }

            constant("testInstance", TestClassWithGenerics(42))

            +"""
                testInstance.printValue()
            """.trimIndent()
        }.trim()

        assertEquals(out, "Value: 42")
    }

    fun testExtensionMethodOnGeneratedType() {
        val out = runScriptOrFail {
            val myDataClass = dataClass("MyDataClass") {
                property<Int>("myProperty")
            }

            extensionMethod("printValue", myDataClass) { ->
                val myProperty by property<Int>()
                print("Value: $myProperty")
            }

            +"""
                MyDataClass(42).printValue()
            """.trimIndent()
        }.trim()

        assertEquals(out, "Value: 42")
    }

    fun testExtensionMethodOnTypeWithGenericParameterIsGenerated() {
        val out = runScriptOrFail {
            val myDataClass = dataClass("MyDataClass") {
                property<Int>("myProperty")
            }

            extensionMethod("printValue", List::class.withParameters(myDataClass)) { ->
                @Suppress("unchecked_cast")
                val iterable = instance as Iterable<Any>

                val sum = iterable.sumOf { value ->
                    @Suppress("unchecked_cast")
                    val property = value::class.memberProperties.first { it.name == "myProperty" } as KProperty1<Any, Int>
                    property.get(value)
                }

                print("Total: $sum")
            }

            +"""
                listOf(MyDataClass(42), MyDataClass(1337)).printValue()
            """.trimIndent()
        }.trim()

        assertEquals(out, "Total: 1379")
    }

    fun testSimpleFunction() {
        val out = runScriptOrFail {
            function("myFunction") { first: Int, second: Int ->
                first + second
            }

            +"""
                println(myFunction(42, 1337))
            """.trimIndent()
        }.trim()

        assertEquals(out, "1379")
    }

    fun testLazyProperty() {
        val out = runScriptOrFail {
            lazyProperty("myLazy") {
                42
            }

            +"""
                println(myLazy)
            """.trimIndent()
        }.trim()

        assertEquals(out, "42")
    }

    fun testLazyPropertyWithGeneratedType() {
        val out = runScriptOrFail {
            val myDataClass = dataClass("MyDataClass") {
                property<Int>("myProperty")
            }

            lazyProperty("myLazy", myDataClass) {
                it.jvmErasure.primaryConstructor!!.call(42)
            }

            +"""
                println(myLazy)
            """.trimIndent()
        }.trim()

        assertEquals(out, "MyDataClass(myProperty=42)")
    }

    fun testDataClassWithNestedDataClass() {
        val out = runScriptOrFail {
            dataClass("MyDataClass") {
                val nested = dataClass("Nested") {
                    property<String>("nestedProperty")
                }

                property<Int>("myProperty")
                property("nested", nested)
            }

            +"""
                val instance = MyDataClass(42, MyDataClass.Nested("Nested"))
                println(instance)
            """.trimIndent()
        }.trim()

        assertEquals(out, "MyDataClass(myProperty=42, nested=Nested(nestedProperty=Nested))")
    }

    fun testDataClassWithCompanionObject() {
        val out = runScriptOrFail {
            dataClass("MyDataClass") {
                property<Int>("myProperty")

                companionObject {
                    lazyProperty("singleton", IdentifiableMember("MyDataClass")) {
                        it.jvmErasure.primaryConstructor!!.call(42)
                    }
                }
            }

            +"""
                println(MyDataClass.singleton)
            """.trimIndent()
        }.trim()

        assertEquals(out, "MyDataClass(myProperty=42)")
    }

    fun testDataClassWithComputedProperty() {
        val out = runScriptOrFail {
            dataClass("MyDataClass") {
                property<Int>("myProperty")

                property<Int>("squared") {
                    val myProperty by property<Int>()
                    myProperty * myProperty
                }
            }

            +"""
                val instance = MyDataClass(42)
                println(instance.squared)
            """.trimIndent()
        }.trim()

        assertEquals(out, "1764")
    }

    fun testDataClassWithComputedPropertyWithSetter() {
        val out = runScriptOrFail {
            dataClass("MyDataClass") {
                property<Int>("myProperty", mutable = true)

                property(
                    "squared",
                    getter = {
                        val myProperty by property<Int>()
                        myProperty * myProperty
                    },
                    setter = { newValue ->
                        var myProperty by property<Int>()
                        myProperty = sqrt(newValue.toDouble()).toInt()
                    }
                )
            }

            +"""
                val instance = MyDataClass(42)
                instance.squared = 144
                println(instance)
            """.trimIndent()
        }.trim()

        assertEquals(out, "MyDataClass(myProperty=12)")
    }

    fun testNamedObject() {
        val out = runScriptOrFail {
            namedObject("MyObject") {
                constant("myConstant", 42)

                method("myMethod") { value: Int ->
                    val myConstant by property<Int>()
                    myConstant + value
                }
            }

            +"""
                println(MyObject.myMethod(1337))
            """.trimIndent()
        }.trim()

        assertEquals(out, "1379")
    }

    fun testNamedObjectWithNestedObject() {
        val out = runScriptOrFail {
            namedObject("MyObject") {
                namedObject("NestedObject") {
                    constant("myConstant", 42)

                    method("myMethod") { value: Int ->
                        val myConstant by property<Int>()
                        myConstant + value
                    }
                }
            }

            +"""
                println(MyObject.NestedObject.myMethod(1337))
            """.trimIndent()
        }.trim()

        assertEquals(out, "1379")
    }

    fun testExtensionProperty() {
        val out = runScriptOrFail {
            extensionProperty<String, Int>("nonBlankLineCount") {
                lineSequence().filter { it.isNotBlank() }.count()
            }

            +"""
                val string = "one\n\n\ntwo\n\nthree\n"
                println(string.nonBlankLineCount)
            """.trimIndent()
        }.trim()

        assertEquals(out, "3")
    }

    fun testExtensionPropertyWithSetter() {
        val out = runScriptOrFail {
            extensionProperty<MutableList<Int>, Int>(
                "firstElement",
                getter = {
                    this[0]
                },
                setter = { newValue ->
                    this[0] = newValue
                }
            )

            +"""
                val list = mutableListOf(42)
                println(list.firstElement)
                list.firstElement = 1337
                println(list.firstElement)
            """.trimIndent()
        }.trim().lines()

        assertEquals(out[0], "42")
        assertEquals(out[1], "1337")
    }

    fun testDataClassImplementsInterface() {
        val out = runScriptOrFail {
            import("kotlin.script.experimental.test.printName")

            dataClass("MyClass") {
                implement<Named>()

                override {
                    property<String>("name")
                }
            }

            +"""
                val instance: Named = MyClass(name = "me")
                instance.printName()
            """.trimIndent()
        }.trim()

        assertEquals(out, "name = me")
    }

    fun testObjectImplementsInterface() {
        val out = runScriptOrFail {
            import("kotlin.script.experimental.test.printName")

            namedObject("MyObject") {
                implement<Named>()

                override {
                    constant("name", "me")
                }
            }

            +"""
                MyObject.printName()
            """.trimIndent()
        }.trim()

        assertEquals(out, "name = me")
    }

    fun testDataClassImplementsGeneratedInterface() {
        val out = runScriptOrFail {
            val named = `interface`("Named") {
                property<String>("name")
            }

            extensionMethod("printName", named) { ->
                val name by property<String>()
                println("name = $name")
            }

            dataClass("MyClass") {
                implement(named) {
                    property<String>("name")
                }
            }

            +"""
                val instance: Named = MyClass(name = "me")
                instance.printName()
            """.trimIndent()
        }.trim()

        assertEquals(out, "name = me")
    }

    fun testObjectImplementsGeneratedInterface() {
        val out = runScriptOrFail {
            val named = `interface`("Named") {
                property<String>("name")
            }

            extensionMethod("printName", named) { ->
                val name by property<String>()
                println("name = $name")
            }

            namedObject("MyObject") {
                implement(named) {
                    lazyProperty("name") {
                        "me"
                    }
                }
            }

            +"""
                MyObject.printName()
            """.trimIndent()
        }.trim()

        assertEquals(out, "name = me")
    }

    fun testGeneratedCodeFromCustomGeneratedCodeImplementation() {
        class SomeCode(
            val typeName: String,
            val propertyName: String,
            val nested: SomeCode? = null
        ) : GeneratedCode {
            override fun GeneratedCode.Builder.body() {
                dataClass(typeName) {
                    property<String>(propertyName)

                    nested?.let { +it }
                }
            }
        }

        val out = runScriptOrFail {
            +SomeCode("A", "a", SomeCode("B", "b"))

            +"""
                println(A("a"))
                println(A.B("b"))
            """.trimIndent()
        }.trim().lines()

        assertEquals(out[0], "A(a=a)")
        assertEquals(out[1], "B(b=b)")
    }

    fun testEnum() {
        val out = runScriptOrFail {
            enum("MyEnum") {
                case("One")
                case("Two")
                case("Three")
            }

            +"""
                println(MyEnum.values().map { it.name })
            """.trimIndent()
        }.trim()

        assertEquals(out, "[One, Two, Three]")
    }

}

// Used Types

data class TestDataClass(
    val value: Int,
    val description: String
) : Serializable

class TestClassWithGenerics<T : Serializable>(
    internal val value: T
) : Serializable

interface Named {
    val name: String
}

fun Named.printName() {
    println("name = $name")
}