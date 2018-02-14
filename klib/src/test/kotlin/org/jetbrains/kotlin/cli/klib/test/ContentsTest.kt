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

import kotlin.test.*
import org.jetbrains.kotlin.cli.klib.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Paths

class ContentsTest {

    private fun testLibrary(name: String) = LIBRARY_DIRECTORY.resolve("$name.klib").toFile().absolutePath

    private fun klibContents(library: String, expected: () -> String) {
        val output = StringBuilder()
        val lib = Library(library, null, "host")
        lib.contents(output)
        assertEquals(expected(), output.trim().toString(), "klib contents test failed for library: $library")
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
            final annotation class A constructor() : Annotation
            final annotation class B constructor() : Annotation
            final class Foo constructor()
        }
        """.trimIndent()
    }

    @Test
    fun constructors() = klibContents(testLibrary("Constructors")) {
        """
        package <root> {
            final annotation class A constructor() : Annotation
            final class Bar @A constructor(x: Int)
            final class Baz private constructor(x: Int)

            final class Foo constructor(x: Int) {
                constructor()
                constructor(x: Double)
                constructor(x: Double, y: Int)
                protected constructor(x: String)
                @A constructor(x: Foo)
            }

            final class Qux protected constructor(x: Int)

            final class Typed<T> constructor(x: Int) {
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
                final fun a()
            }

            final class B constructor() {

                object C {
                    final fun c()
                }

                companion object {
                    final fun b()
                }

            }

            final class D constructor() {

                companion object E {
                    final fun e()
                }

            }

        }
        """.trimIndent()
    }

    @Test
    fun classes() = klibContents(testLibrary("Classes")) {
        """
        package <root> {

            final class A constructor() {
                final val aVal: Int = 0
                final var aVar: String
                final fun aFun()

                final inner class B constructor() {
                    final val bVal: Int = 0
                    final var bVar: String
                    final fun bFun()

                    final inner class C constructor() {
                        final val cVal: Int = 0
                        final var cVar: String
                        final fun cFun()
                    }

                }

                final class E constructor() {
                    final val eVal: Int = 0
                    final var eVar: String
                    final fun eFun()
                }

            }

            final data class F constructor(fVal: Int, fVar: String) {
                final val fVal: Int
                final var fVar: String
                final operator fun component1(): Int
                final operator fun component2(): String
                final fun copy(fVal: Int = ..., fVar: String = ...): F
                override fun equals(other: Any?): Boolean
                final fun fFun()
                override fun hashCode(): Int
                override fun toString(): String
            }

            final class FinalImpl constructor() : OpenImpl {
                override val iVal: Int = 0
                override var iVar: String
                override fun iFun()
            }

            interface Interface {
                abstract val iVal: Int
                abstract var iVar: String
                abstract fun iFun()
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
    @Ignore // TODO: Do we need to print the overridden method in the C entry?
    fun enum() = klibContents(testLibrary("Enum")) {
        """
        package <root> {

            final enum class E private constructor(x: Int = ...) : Enum<E> {
                enum entry A
                enum entry B

                enum entry C {
                    override fun enumFun(): Int
                }

                final val enumVal: Int = 0
                final var enumVar: String
                final val x: Int
                open fun enumFun(): Int
            }

        }
        """
    }

    companion object {
        val LIBRARY_DIRECTORY = Paths.get("build/konan/libs").resolve( HostManager.host.visibleName)
    }
}