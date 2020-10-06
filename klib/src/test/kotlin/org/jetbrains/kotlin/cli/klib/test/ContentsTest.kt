/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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
        lib.contents(output, false)
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
        val distributionPath = System.getProperty("konan.home")
        Library(Distribution(distributionPath).stdlib, null, "host").contents(output, false)
    }

    @Test
    fun topLevelFunctions() = klibContents(testLibrary("TopLevelFunctions")) {
        """
        package <root> {
            annotation class A constructor() : Annotation
            annotation class B constructor() : Annotation
            class Foo constructor()
        }

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
                    @A get
                    @A set
                val annotatedGetter: Int = 0
                    @A get
                var annotatedSetter: Int
                    @A set
                var privateSetter: Int
                    private set
                protected val protectedSimple: Int = 0
                val simple: Int = 0
            }

        }
        """.trimIndent()
    }

    @Test
    fun topLevelPropertiesCustomPackage() = klibContents(testLibrary("TopLevelPropertiesCustomPackage")) {
        """
        package custom.pkg {
            typealias MyTransformer = (String) -> Int
        }

        package custom.pkg {
            val v1: Int = 1
            val v2: String = "hello"
            val v3: (String) -> Int
            val v4: MyTransformer /* = (String) -> Int */
        }
        """.trimIndent()
    }

    @Test
    fun topLevelPropertiesRootPackage() = klibContents(testLibrary("TopLevelPropertiesRootPackage")) {
        """
        package <root> {
            typealias MyTransformer = (String) -> Int
        }

        package <root> {
            val v1: Int = 1
            val v2: String = "hello"
            val v3: (String) -> Int
            val v4: MyTransformer /* = (String) -> Int */
        }
        """.trimIndent()
    }

    @Test
    fun topLevelPropertiesWithClassesCustomPackage() = klibContents(testLibrary("TopLevelPropertiesWithClassesCustomPackage")) {
        """
        package custom.pkg {
            object Bar
            class Foo constructor()
            typealias MyTransformer = (String) -> Int
        }

        package custom.pkg {
            val v1: Int = 1
            val v2: String = "hello"
            val v3: (String) -> Int
            val v4: MyTransformer /* = (String) -> Int */
        }
        """.trimIndent()
    }

    @Test
    fun topLevelPropertiesWithClassesRootPackage() = klibContents(testLibrary("TopLevelPropertiesWithClassesRootPackage")) {
        """
        package <root> {
            object Bar
            class Foo constructor()
            typealias MyTransformer = (String) -> Int
        }

        package <root> {
            val v1: Int = 1
            val v2: String = "hello"
            val v3: (String) -> Int
            val v4: MyTransformer /* = (String) -> Int */
        }
        """.trimIndent()
    }

    companion object {
        val LIBRARY_DIRECTORY = Paths.get("build/konan/libs").resolve(HostManager.hostName)
    }
}