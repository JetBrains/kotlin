/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.klib.test

import com.intellij.openapi.util.text.StringUtil
import kotlin.test.*
import org.jetbrains.kotlin.cli.klib.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Paths

class ContentsTest {

    private fun testLibrary(name: String) = LIBRARY_DIRECTORY.resolve("$name.klib").toFile().absolutePath

    private fun klibContents(library: String, printOutput: Boolean = false, expected: () -> String) {
        val output = StringBuilder()
        val lib = Library(library, null, "host")
        lib.contents(output)
        if (printOutput) {
            println(output.trim().toString())
        }
        assertEquals(
                StringUtil.convertLineSeparators(expected()),
                StringUtil.convertLineSeparators(output.trim().toString()),
                "klib contents test failed for library: $library"
        )
    }

    @Test
    fun `Stdlib content should be printed without exceptions`() {
        val output = StringBuilder()
        Library(Distribution().stdlib, null, "host").contents(output)
    }

    @Test
    fun topLevelFunctions() = klibContents(testLibrary("TopLevelFunctions")) {
        """
        package <root> {
            @A @B fun a()
            fun f1(x: Foo)
            fun f2(x: Foo, y: Foo): Int
            inline fun i1(block: () -> Foo)
            inline fun i2(noinline block: () -> Foo)
            inline fun i3(crossinline block: () -> Foo)
            fun i4(block: (Foo) -> Int)
            fun i5(block: (Foo, Foo) -> Int)
            fun i6(block: Foo.() -> Int)
            fun i7(block: Foo.(Foo) -> Int)
            fun <T> t1(x: Foo)
            fun <T> t2(x: T)
            fun <T, F> t3(x: T, y: F)
            inline fun <reified T> t4(x: T)
            fun <T : Number> t5(x: T)
            fun Foo.e()
            annotation class A constructor() : Annotation
            annotation class B constructor() : Annotation
            class Foo constructor()
        }
        """.trimIndent()
    }

    @Test
    fun constructors() = klibContents(testLibrary("Constructors")) {
        """
        package <root> {
            annotation class A constructor() : Annotation
            class Bar @A constructor(x: Int)
            class Baz private constructor(x: Int)

            class Foo constructor(x: Int) {
                constructor()
                constructor(x: Double)
                constructor(x: Double, y: Int)
                protected constructor(x: String)
                @A constructor(x: Foo)
            }

            class Qux protected constructor(x: Int)

            class Typed<T> constructor(x: Int) {
                constructor()
                constructor(x: Double)
                constructor(x: Double, y: Int)
                protected constructor(x: String)
                @A constructor(x: Foo)
            }

        }
        """.trimIndent()
    }

    @Test
    fun objects() = klibContents(testLibrary("Objects")) {
        """
        package <root> {

            object A {
                fun a()
            }

            class B constructor() {

                object C {
                    fun c()
                }

                companion object {
                    fun b()
                }

            }

            class D constructor() {

                companion object E {
                    fun e()
                }

            }

        }
        """.trimIndent()
    }

    @Test
    fun classes() = klibContents(testLibrary("Classes")) {
        """
        package <root> {

            class A constructor() {
                val aVal: Int = 0
                var aVar: String
                fun aFun()

                inner class B constructor() {
                    val bVal: Int = 0
                    var bVar: String
                    fun bFun()

                    inner class C constructor() {
                        val cVal: Int = 0
                        var cVar: String
                        fun cFun()
                    }

                }

                class E constructor() {
                    val eVal: Int = 0
                    var eVar: String
                    fun eFun()
                }

            }

            data class F constructor(fVal: Int, fVar: String) {
                val fVal: Int
                var fVar: String
                operator fun component1(): Int
                operator fun component2(): String
                fun copy(fVal: Int = ..., fVar: String = ...): F
                override fun equals(other: Any?): Boolean
                fun fFun()
                override fun hashCode(): Int
                override fun toString(): String
            }

            class FinalImpl constructor() : OpenImpl {
                override val iVal: Int = 0
                override var iVar: String
                override fun iFun()
            }

            interface Interface {
                val iVal: Int
                var iVar: String
                fun iFun()
            }

            open class OpenImpl constructor() : Interface {
                override val iVal: Int = 0
                override var iVar: String
                override fun iFun()
            }

        }
        """.trimIndent()
    }

    @Test
    fun methodModality() = klibContents(testLibrary("MethodModality")) {
        """
        package <root> {

            abstract class AbstractClass constructor() : Interface {
                abstract fun abstractFun()
                override fun interfaceFun()
            }

            class FinalClass constructor() : OpenClass {
                override fun openFun1()
                final override fun openFun2()
            }

            interface Interface {
                fun interfaceFun()
            }

            open class OpenClass constructor() : AbstractClass {
                override fun abstractFun()
                fun finalFun()
                open fun openFun1()
                open fun openFun2()
            }

        }
        """.trimIndent()
    }

    @Test
    fun functionModifiers() = klibContents(testLibrary("FunctionModifiers")) {
        """
        package <root> {

            class Foo constructor() {
                fun f1()
                infix fun f2(x: Int)
                suspend fun f3()
                tailrec fun f4()
                fun f5(vararg x: Int)
                operator fun plus(x: Int)
            }

        }
        """.trimIndent()
    }

    @Test
    // TODO: Support enum entry methods in serialization/deserialization.
    fun enum() = klibContents(testLibrary("Enum")) {
        """
        package <root> {

            enum class E private constructor(x: Int = ...) : Enum<E> {
                enum entry A
                enum entry B
                enum entry C
                val enumVal: Int = 0
                var enumVar: String
                val x: Int
                open fun enumFun(): Int
            }

        }
        """.trimIndent()
    }

    @Test
    // TODO: Support getter/setter annotations in serialization/deserialization.
    fun accessors() = klibContents(testLibrary("Accessors")) {
        """
        package custom.pkg {
            annotation class A constructor() : Annotation

            class Foo constructor() {
                @A val annotated: Int = 0
                var annotatedAccessors: Int
                    get
                    set
                val annotatedGetter: Int = 0
                    get
                var annotatedSetter: Int
                    set
                var privateSetter: Int
                    private set
                protected val protectedSimple: Int = 0
                val simple: Int = 0
            }

        }
        """.trimIndent()
    }

    companion object {
        val LIBRARY_DIRECTORY = Paths.get("build/konan/libs").resolve( HostManager.hostName)
    }
}