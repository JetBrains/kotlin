/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test.k2repl

import junit.framework.TestCase
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.jvmhost.repl.k2.K2ReplCompiler

/**
 * This class is intended to test the K2 Repl implementation.
 *
 * In particular, this class contains smoke tests for setting and reading properties
 * within a single cell.
 *
 * These tests are written in a way where we manually define the expected output
 * of the compilers lowering phase when running on REPL scripts. This is because
 * the compiler doesn't yet support this.
 *
 * This also means that we are using a special version of [K2ReplCompiler] that just
 * compile normal Kotlin files rather than `kts` files.
 *
 * This setup allows us to iterate on how we want the lowering phase to behave as well
 * as iterate on the [kotlin.script.experimental.jvmhost.repl.k2.ReplState] API without
 * having compiler support.
 *
 * Once the compiler part is done, we can just remove the manual lowering code and all tests
 * should still pass.
 */
class ReplPropertySmokeTest : TestCase() {

    // Scripts with no output should return "Unit"
    fun testNoOutput() {
        // Only used for documentation
        val snippets = listOf(
            """
                println("Hello, world!")
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {
                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    println("Hello, world!")
                    `${'$'}replState`.setUnitOutput(this) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assert(value.result is ResultValue.Unit)
    }

    // Readonly property with backing field and default accessor
    fun testReadOnlyPropertyWithBackingFieldAndDefaultAccessor() {
        // Only used for documentation
        val snippets = listOf(
            """
                val field1 = "Hello"
                field1            
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadOnlyProperty<String>(`field1${'$'}replProperty`, "Hello")
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals("Hello", (value.result as ResultValue.Value).value)
    }

    // Readonly property with a getter and no backing field
    fun testReadOnlyPropertyWithGetterAndNoBackingField() {
        // Only used for documentation
        val snippets = listOf(
            """
                val field1: String
                    get() { return "Hello" }
                field1
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        // "return" in getters should be removed
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadOnlyProperty<String>(`field1${'$'}replProperty`, { state: ReplState, name: String -> "Hello" })
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals("Hello", (value.result as ResultValue.Value).value)
    }

    // Readonly property with getter and backing field
    fun testReadOnlyPropertyWithGetterAndBackingField() {
        // Only used for documentation
        val snippets = listOf(
            """
                val field4: String = "Backing"
                    get() {
                        return "Getter: ${'$'}field"
                    }
                field4
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        // "return" in getter is removed
        // "field" in getter is replaced with `state.getBackingFieldValue<Type>("propertyName")`
        // String interpolation require that we use `${}`
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadOnlyProperty<String>(
                        `field1${'$'}replProperty`, 
                        "Backing",
                        { state: ReplState, name: String -> 
                            "Getter: ${'$'}{state.getBackingFieldValue<String>("field1")}" 
                        }
                    )
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val result = evaluateInRepl(loweredSnippet).last().valueOrThrow().result
        assertTrue(result is ResultValue.Value)
        assertEquals("Getter: Backing", (result as ResultValue.Value).value)
    }

    // Readonly property with getter using a `this` reference
    fun testReadOnlyPropertyWithThisReference() {
        // Only used for documentation
        val snippets = listOf(
            """
                val field1: String
                    get() { return this.toString() }
                field1
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        // "return" in getters should be removed
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadOnlyProperty<String>(`field1${'$'}replProperty`, { state: ReplState, name: String -> this.toString() })
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val result = evaluateInRepl(loweredSnippet).last().valueOrThrow().result
        assertTrue(result is ResultValue.Value)
        assertTrue("Was: $result", (result as ResultValue.Value).value.toString().startsWith("repl.snippet0.Snippet0@"))
    }

    // Readonly property with a nullable backing field and default accessor
    fun testReadOnlyPropertyNullableType() {
        // Only used for documentation
        val snippets = listOf(
            """
                val field1: String? = null
                field1            
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String?>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadOnlyProperty<String?>(`field1${'$'}replProperty`, null)
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String?>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals(null, (value.result as ResultValue.Value).value)
    }

    // Readonly property which throws in the initializer
    // Line numbering is off compared to the users code. How can this be adjusted?
    fun testReadOnlyPropertyWithExceptionInInitializer() {
        // Only used for documentation
        val snippets = listOf(
            """
                val field1: String = TODO()
                field1            
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String?>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadOnlyProperty<String?>(`field1${'$'}replProperty`, TODO())
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Error)
        val error = (value.result as ResultValue.Error).error
        assertTrue(error is NotImplementedError)
        assertEquals("An operation is not implemented.", error.message)
        assertEquals("repl.snippet0.Snippet0.evaluate(Snippet_0.kt:11)", error.stackTrace[0].toString())
    }

    // Readonly property which throws in the getter
    // Line numbering is off compared to the users code. How can this be adjusted?
    fun testReadOnlyPropertyWithExceptionInGetter() {
        // Only used for documentation
        val snippets = listOf(
            """
                val field1: String
                    get() { TODO() }
                field1            
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String?>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadOnlyProperty<String?>(
                        `field1${'$'}replProperty`,
                        { state: ReplState, name: String -> 
                            TODO()
                        }
                    )
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Error)
        val error = (value.result as ResultValue.Error).error
        assertTrue(error is NotImplementedError)
        assertEquals("An operation is not implemented.", error.message)
        assertEquals("repl.snippet0.Snippet0.evaluate\$lambda\$0(Snippet_0.kt:14)", error.stackTrace[0].toString())
    }

    // ReadWrite property with a backing field and default accessors
    fun testReadWritePropertyWithBackingFieldAndDefaultAccessors() {
        // Only used for documentation
        val snippets = listOf(
            """
                var field1 = "Backing"
                field1 = "Set: ${'$'}field1"
                field1
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadWriteProperty<String>(`field1${'$'}replProperty`, "Backing")
                    `${'$'}replState`.setPropertyValue<String>("field1", "Set: ${'$'}{ `${'$'}replState`.getBackingFieldValue<String>("field1") }")
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals("Set: Backing", (value.result as ResultValue.Value).value)
    }

    // ReadWrite property with custom getters/setters and no backing field
    fun testReadWritePropertyWithCustomAccessorsAndNoBackingField() {
        // Only used for documentation
        val snippets = listOf(
            """
            var field1: String
                get() { return "Foo" }
                set(value) { println(value) }
            field1 = "Wave"
            field1
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadWriteProperty<String>(
                        `field1${'$'}replProperty`,
                        { state: ReplState, name: String -> "Foo" },
                        { state: ReplState, name: String, value: String -> println(value) }
                    )
                    `${'$'}replState`.setPropertyValue<String>("field1", "Wave")
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals("Foo", (value.result as ResultValue.Value).value)
    }

    // ReadWrite property with custom getters/setters and no backing field
    fun testReadWritePropertyWithCustomAccessorsAndBackingField() {
        // Only used for documentation
        val snippets = listOf(
            """
            var field10: String = "Backing"
                get() { return "Getter" }
                set(value) { field = value }
            field1 = "SetValue"
            field1
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createReadWriteProperty<String>(
                        `field1${'$'}replProperty`,
                        "Backing",
                        { state: ReplState, name: String -> "Getter" },
                        { state: ReplState, name: String, value: String ->
                            state.setBackingFieldValue(name, value)
                        }
                    )
                    `${'$'}replState`.setPropertyValue<String>("field1", "SetValue")
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals("Getter", (value.result as ResultValue.Value).value)
    }

    fun testLazyProperty() {
        // Only used for documentation
        val snippets = listOf(
            """
            private val field1 by lazy { 42 }
            field1
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        // The lazy { .. } constructor is just moved into a function parameter
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            
            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<Int>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createLazyProperty<Int>(
                        `field1${'$'}replProperty`,
                        lazy { 42 }
                    )
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<Int>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals(42, (value.result as ResultValue.Value).value)
    }

    fun testDelegateProperty() {
        // Only used for documentation
        val snippets = listOf(
            """
            import kotlin.properties.Delegates

            // Delegated property
            var field1 by Delegates.observable("") { prop, old, new ->
                println("Property ${'$'}{prop.name} changed from '${'$'}old' to '${'$'}new'")
            }
            field1 = "Hello"
            field1
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase
        // The Delegate is just moved into a function parameter
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet
            import kotlin.properties.Delegates

            class Snippet0: ExecutableReplSnippet  {

                val `field1${'$'}replProperty` = createMockProperty<String>("field1") 

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    `${'$'}replState`.createDelegateProperty<String>(
                        `field1${'$'}replProperty`,
                        Delegates.observable("") { prop, old, new ->
                            println("Property ${'$'}{prop.name} changed from '${'$'}old' to '${'$'}new'")
                        }
                    )
                    `${'$'}replState`.setPropertyValue<String>("field1", "Hello")
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("field1")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals("Hello", (value.result as ResultValue.Value).value)
    }

    fun testPropertyDestructuring() {
        // Only used for documentation
        val snippets = listOf(
            """
            val (name, age) = Pair("John", 42)
            first + " " + last
            """.trimIndent()
        )

        // Expected output of the compilers lowering phase.
        // Move each individual part into its own property.
        // We ned a temporary variable so we do not risk triggering side-effects twice.
        // Unclear how to handle stack traces with this pattern
        val loweredSnippet = listOf(
            """
            package repl.snippet0
            import kotlin.script.experimental.jvmhost.repl.k2.createMockProperty
            import kotlin.script.experimental.jvmhost.repl.k2.ReplState
            import kotlin.script.experimental.jvmhost.repl.k2.ExecutableReplSnippet

            class Snippet0: ExecutableReplSnippet  {

                private val `name${'$'}replProperty` = createMockProperty<String>("name")
                private val `age${'$'}replProperty` = createMockProperty<Int>("age")

                override suspend fun evaluate(`${'$'}replState`: ReplState) {
                    val `${'$'}decl${'$'}name${'$'}age` = Pair("John", 42)
                    `${'$'}replState`.createReadOnlyProperty<String>(`name${'$'}replProperty`, `${'$'}decl${'$'}name${'$'}age`.component1())
                    `${'$'}replState`.createReadOnlyProperty<Int>(`age${'$'}replProperty`, `${'$'}decl${'$'}name${'$'}age`.component2())
                    `${'$'}replState`.setValueOutput(this, `${'$'}replState`.getPropertyValue<String>("name") + " " + `${'$'}replState`.getPropertyValue<Int>("age")) 
                }
            }
            """.trimIndent()
        )
        val value = evaluateInRepl(loweredSnippet).last().valueOrThrow()
        assertTrue(value.result is ResultValue.Value)
        assertEquals("John 42", (value.result as ResultValue.Value).value)
    }
}
