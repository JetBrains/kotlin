/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals


/**
 * ## Test Scope
 * This test is intended to verify mapping and translation of types.
 * It therefore tests only a subset of the [ObjCExportHeaderGeneratorTest], but is more specialized and should be convenient
 * for identifying and debugging issues in type mapping.
 *
 * The test is running with K1 as well as the Analysis-Api based implementation (using the injected [HeaderGenerator])
 */
class ObjCExportTypeTranslationTest(
    private val headerGenerator: HeaderGenerator,
) {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test - function return type`() {
        val header = header(
            """
            class A
            fun foo(): A = error("stub")
        """.trimIndent()
        )

        assertEquals(" -> A *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - property return type`() {
        val header = header(
            """
            class A
            val foo: A get() = error("stub")
        """.trimIndent()
        )

        assertEquals("A *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - function with simple parameters`() {
        val header = header(
            """
            class A
            class B
            class C
            
            fun foo(a: A, b: B?): C? = error("stub")
        """.trimIndent()
        )

        assertEquals("A *, B * _Nullable -> C * _Nullable", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - Unit as return type`() {
        val header = header(
            """
            fun foo() = Unit
            """.trimIndent()
        )

        assertEquals(" -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - Unit as parameter type`() {
        val header = header(
            """
            fun foo(unit: Unit) = Unit
            """.trimIndent()
        )

        assertEquals("Unit * -> void", header.renderTypesOfSymbol("foo"))
    }


    @Test
    fun `test - hiddenTypes - Any - are exposed as id`() {
        val header = header("""fun foo(): Any""")
        assertEquals(" -> id", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - hiddenTypes - Function - are exposed as id`() {
        val header = header("""fun foo(): Function<*>""")
        assertEquals(" -> id", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - hiddenTypes - Iterable - are exposed as id`() {
        val header = header("""fun foo(): Iterable<*>""")
        assertEquals(" -> id", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - hiddenTypes - Number - are exposed as id`() {
        val header = header("""fun foo(): Number""")
        assertEquals(" -> id", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - Byte`() {
        val header = header("""val foo: Byte get() = error("stub")""")
        assertEquals("int8_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - UByte`() {
        val header = header("""val foo: UByte get() = error("stub")""")
        assertEquals("uint8_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - Short`() {
        val header = header("""val foo: Short get() = error("stub")""")
        assertEquals("int16_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - UShort`() {
        val header = header("""val foo: UShort get() = error("stub")""")
        assertEquals("uint16_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - Int`() {
        val header = header("""val foo: Int get() = error("stub")""")
        assertEquals("int32_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - UInt`() {
        val header = header("""val foo: UInt get() = error("stub")""")
        assertEquals("uint32_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - Long`() {
        val header = header("""val foo: Long get() = error("stub")""")
        assertEquals("int64_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - ULong`() {
        val header = header("""val foo: ULong get() = error("stub")""")
        assertEquals("uint64_t", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - Float`() {
        val header = header("""val foo: Float get() = error("stub")""")
        assertEquals("float", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - number - Double`() {
        val header = header("""val foo: Double get() = error("stub")""")
        assertEquals("double", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - char`() {
        val header = header("""val foo: Char get() = error("stub")""")
        assertEquals("unichar", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - string`() {
        val header = header("""val foo: String get() = error("stub")""")
        assertEquals("NSString *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - List`() {
        val header = header(
            """
            class A
            val foo: List<A> get() = error("stub")""".trimIndent()
        )
        assertEquals("NSArray<A *> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - List - nullable element`() {
        val header = header(
            """
            class A
            val foo: List<A?> get() = error("stub")""".trimIndent()
        )
        assertEquals("NSArray<id> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - MutableList`() {
        val header = header(
            """
            class A
            val foo: MutableList<A> get() = error("stub")
        """.trimIndent()
        )

        assertEquals("NSMutableArray<A *> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - MutableList - nullable element`() {
        val header = header(
            """
            class A
            val foo: MutableList<A?> get() = error("stub")
        """.trimIndent()
        )

        assertEquals("NSMutableArray<id> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - Set`() {
        val header = header(
            """
            class A
            val foo: Set<A> get() = error("stub")
            """.trimIndent()
        )

        assertEquals("NSSet<A *> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - MutableSet`() {
        val header = header(
            """
            class A
            val foo: MutableSet<A> get() = error("stub")
        """.trimIndent(),
            configuration = HeaderGenerator.Configuration(frameworkName = "Shared")
        )

        assertEquals("SharedMutableSet<SharedA *> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - Map`() {
        val header = header(
            """
            class A
            class B
            val foo: Map<A, B> get() = error("stub")
            """.trimIndent()
        )

        assertEquals("NSDictionary<A *, B *> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - MutableMap`() {
        val header = header(
            """
            class A
            class B
            val foo: MutableMap<A, B> get() = error("stub")
            """.trimIndent(),
            configuration = HeaderGenerator.Configuration(frameworkName = "Shared")
        )

        assertEquals("SharedMutableDictionary<SharedA *, SharedB *> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - function type - 0`() {
        val header = header(
            """
            fun foo(action: () -> Unit) = Unit
        """.trimIndent()
        )

        assertEquals("void (^)(void) -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - function type - 1`() {
        val header = header(
            """
            class A
            fun foo(action: () -> A) = Unit
        """.trimIndent()
        )

        assertEquals("A *(^)(void) -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - function type - 2`() {
        val header = header(
            """
            class A
            class B
            fun foo(action: (a: A) -> B) = Unit
        """.trimIndent()
        )

        assertEquals("B *(^)(A *) -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - function type - 3`() {
        val header = header(
            """
            class A
            class B
            class C
            fun foo(action: (a: A, b: B) -> C) = Unit
        """.trimIndent()
        )

        assertEquals("C *(^)(A *, B *) -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - function type - receiver`() {
        val header = header(
            """
            class A
            class B
            class C
            fun foo(action: A.(b: B) -> C) = Unit
        """.trimIndent()
        )

        assertEquals("C *(^)(A *, B *) -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - value inline class`() {
        val header = header(
            """
            class A
            value class Inlined(val a: A)
            
            val foo: Inlined get() = error("stub")
        """.trimIndent()
        )

        assertEquals("id", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - NativePtr`() {
        val header = header(
            """
            val foo: kotlin.native.internal.NativePtr get() = error("stub")
            """.trimIndent()
        )

        assertEquals("void * _Nullable", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - NonNullNativePtr`() {
        val header = header(
            """
            val foo: kotlin.native.internal.NonNullNativePtr get() = error("stub")
            """.trimIndent()
        )

        assertEquals("void *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - CPointer`() {
        val header = header(
            """
            import kotlinx.cinterop.CPointer
            import kotlinx.cinterop.CPointed
            val foo: CPointer<CPointed> get() = error("stub")
            """.trimIndent()
        )

        assertEquals("void *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - generics - class`() {
        val header = header(
            """
            class A<T>
            class B
            val foo: A<B> get() = error("stub")
            """.trimIndent()
        )
        assertEquals("A<B *> *", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - generic function`() {
        val header = header(
            """
            fun <T> foo(value: T) = Unit
            """.trimIndent()
        )
        assertEquals("id _Nullable -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - generic class with function`() {
        val header = header(
            """
            class A<T> {
                fun foo(value: T) = Unit
            }
            """.trimIndent()
        )
        assertEquals("T _Nullable -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - class with generic function`() {
        val header = header(
            """
            class A {
               fun <T: Any> foo(value: T) = Unit
            }
            """.trimIndent()
        )

        assertEquals("id -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - generic class with bounds with function`() {
        val header = header(
            """
            interface I
            class A<T: I> {
                fun foo(value: T) = Unit
            }
            """.trimIndent()
        )
        assertEquals("T -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - nested classes with same type parameter`() {
        val header = header(
            """
            class A<T: Any> {
                class B<T: Any> {
                    fun foo(value: T) = Unit
                }
            }
            """.trimIndent()
        )

        assertEquals("T -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - classes with same type parameter as function`() {
        val header = header(
            """
            class A<T: Any> {
                fun <T: Any> foo(value: T) = Unit
            }
            """.trimIndent()
        )

        assertEquals("id -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - ObjCObject types`() {
        val header = header(
            """
                class A: kotlinx.cinterop.ObjCObject
                val foo : A get() = A
                val bar: kotlinx.cinterop.ObjCObject
            """.trimIndent()
        )

        assertEquals("id", header.renderTypesOfSymbol("foo"))
        assertEquals("id", header.renderTypesOfSymbol("bar"))
    }

    @Test
    fun `test - unresolved error type`() {
        val header = header(
            """
            val property : Unresolved get() = error("stub")
            fun function(a:  Unresolved): Unresolved = error("stub")
            """.trimIndent()
        )
        assertEquals("ERROR *", header.renderTypesOfSymbol("property"))
        assertEquals("ERROR * -> ERROR *", header.renderTypesOfSymbol("function"))
    }

    @Test
    fun `test - char - property`() {
        val header = header(
            """
                val property : Char get() = error("stub")
            """.trimIndent()
        )

        assertEquals("unichar", header.renderTypesOfSymbol("property"))
    }

    @Test
    fun `test - char - function parameter`() {
        val header = header(
            """
                fun foo(x: Char) = Unit
            """.trimIndent()
        )

        assertEquals("unichar -> void", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - char - as return type`() {
        val header = header(
            """
                fun foo(): Char = error("stub")
            """.trimIndent()
        )

        assertEquals(" -> unichar", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - function type returning char`() {
        val header = header(
            """
            val foo: () -> Char
        """.trimIndent()
        )

        assertEquals("id (^)(void)", header.renderTypesOfSymbol("foo"))
    }

    @Test
    fun `test - custom List implementation`() {
        val header = header(
            """
                interface MyList<T>: List<T>
                val foo: MyList<String> get() = error("stub")
            """.trimIndent()
        )

        assertEquals("NSArray<NSString *> *", header.renderTypesOfSymbol("foo"))
    }

    private fun header(
        @Language("kotlin") vararg sourceCode: String,
        configuration: HeaderGenerator.Configuration = HeaderGenerator.Configuration(),
    ): ObjCHeader {
        sourceCode.forEachIndexed { index, code ->
            val sourceKt = tempDir.resolve("source$index.kt")
            sourceKt.writeText(code)
        }
        return headerGenerator.generateHeaders(tempDir.toFile(), configuration)
    }

    private fun ObjCHeader.renderTypesOfSymbol(name: String): String {
        val stub = stubs.closureSequence().find { it.origin?.name?.asString() == name }
        return when (stub) {
            is ObjCMethod -> stub.parameters.joinToString(", ") { it.type.render() } + " -> ${stub.returnType.render()}"
            is ObjCProperty -> stub.type.render()
            is ObjCParameter -> stub.type.render()
            is ObjCInterface -> "${stub.name}: ${stub.superClass.orEmpty()}" + "${
                stub.superClassGenerics.joinToString(", ", "<", ">")
            }, ${stub.superProtocols.joinToString(", ")}"
            is ObjCProtocol -> "${stub.name}: ${stub.superProtocols.joinToString(", ")}"
            null -> error("Missing symbol '$name' in \n${render().joinToString("\n")}")
            else -> error("No rendering defined for $stub")
        }
    }
}