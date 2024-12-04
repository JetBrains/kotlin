/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.junit.Test


class ParameterizedTypesCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test simple parameterized class`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    class X<T, R>
                    fun<T, R> x1(x: X<T, R>) {}
                    fun <M> x2(x: X<M, String>) {}
                    fun x3(x: X<Int, String>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class X<T, R>
                    fun<T, R> x1(x: X<T, R>) {}
                    fun <M> x2(x: X<M, String>) {}
                    fun x3(x: X<Int, String>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X<T, R>()
                expect fun <T, R> x1(x: X<T, R>)
                expect fun <M> x2(x: X<M, String>)
                expect fun x3(x: X<Int, String>)
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 0`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>

                    fun x1(x: TA3) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>
                    
                    fun x1(x: TA3) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias TA1<T, R> = Map<T, R>
                typealias TA2<R> = TA1<String, R>
                typealias TA3 = TA2<Int>
                
                expect fun x1(x: TA3)
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 1`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>

                    fun x1(x: TA3) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias TA1<T, R> = Map<T, R>
                    typealias TA2<R> = TA1<String, R>
                    typealias TA3 = TA2<Int>
                    
                    fun x1(x: TA1<String, Int>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias TA1<T, R> = Map<T, R>
                typealias TA2<R> = TA1<String, R>
                typealias TA3 = TA2<Int>
                
                expect fun x1(x: TA1<String, Int>) 
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 2`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Triple<T, R, M>
                    typealias A<T, M> = Triple<T, String, M>
                    typealias B<T, M> = Triple<T, String, M>

                    fun x1(x: B<Int, Long>) {}
                    fun x2(x: A<Int, Long>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Triple<T, R, M>
                    typealias A<T, M> = Triple<T, String, M>
                    typealias B<T, M> = Triple<T, String, M>

                    fun x1(x: A<Int, Long>) {} // NOTE: A & B flipped
                    fun x2(x: B<Int, Long>) {} // NOTE: A & B flipped
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                 expect class Triple<T, R, M>()
                 typealias A<T, M> = Triple<T, String, M>
                 typealias B<T, M> = Triple<T, String, M>
                 
                 expect fun x1(x: Triple<Int, String, Long>) 
                 expect fun x2(x: Triple<Int, String, Long>)
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 3`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Triple<T, R, M>
                    typealias T1<T, R, M> = Triple<T, R, M>
                    typealias T2<T, R, M> = Triple<T, R, M>
                    typealias T3<T, M> = Triple<T, String, M>
                    typealias T4<T, M> = Triple<T, String, M>

                    fun x1(x: T4<Int, Long>) {}
                    fun x2(x: T3<Int, Long>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Triple<T, R, M>
                    typealias T1<T, R, M> = Triple<T, R, M>
                    typealias T2<T, R, M> = Triple<T, R, M>
                    typealias T3<T, M> = Triple<T, String, M>
                    typealias T4<T, M> = T3<T, M>

                    fun x1(x: T3<Int, Long>) {} // NOTE: T3 & T4 flipped
                    fun x2(x: T4<Int, Long>) {} // NOTE: T3 & T4 flipped
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                 expect class Triple<T, R, M>()
                 typealias T1<T, R, M> = Triple<T, R, M>
                 typealias T2<T, R, M> = Triple<T, R, M>
                 typealias T3<T, M> = Triple<T, String, M>
                 typealias T4<T, M> = Triple<T, String, M>
                 
                 expect fun x1(x: Triple<Int, String, Long>) 
                 expect fun x2(x: T3<Int, Long>)
            """.trimIndent()
        )
    }

    fun `test parameterized type alias - 4`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Triple<T, R, M>
                    typealias A1 = String
                    typealias A2 = Long
                    typealias A3<T, R> = Triple<T, String, R>
                    
                    fun x(x: A3<A1, A2>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Triple<T, R, M>
                    typealias B1 = String
                    typealias B2 = Long
                    
                    fun x(x: Triple<B1, String, B2>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                 expect class Triple<T, R, M>()
                 expect fun x(x: Triple<String, String, Long>)
            """.trimIndent()
        )
    }


    fun `test type alias from dependencies parameterized with library source type`() {
        val result = commonize {
            outputTarget("(a, b)")

            registerDependency("(a, b)") {
                source(
                    """
                    class Box<T>
                    typealias TA<T> = Box<T>
                    """.trimIndent()
                )
            }

            simpleSingleSourceTarget(
                "a", """
                    class MyClass
                    typealias X = TA<MyClass>
                    
                    fun x(x: X) {}
                    fun x2(x: TA<MyClass>) {}
                    fun x3(x: TA<X>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class MyClass
                    typealias X = TA<MyClass>
                    
                    fun x(x: X) {}
                    fun x2(x: TA<MyClass>) {}
                    fun x3(x: TA<X>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class MyClass()
                typealias X = TA<MyClass>
                
                expect fun x(x: X)
                expect fun x2(x: TA<MyClass>)
                expect fun x3(x: TA<X>)
            """.trimIndent()
        )
    }

    fun `test parameterized function - 0`() {
        val result = commonize {
            outputTarget("(a, b)")
            simpleSingleSourceTarget(
                "a", """
                    typealias X<T> = Map<Int, T>
                    fun <T: Any> x(x: X<T>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    fun <T: Any> x(x: Map<Int, T>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect fun <T: Any> x(x: Map<Int, T>)
            """.trimIndent()
        )
    }

    fun `test parameterized function - 1`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias X<T> = Map<Int, T>
                    fun <T: X<T>> x(x: X<T>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    fun <T: Map<Int, T>> x(x: Map<Int, T>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect fun <T: Map<Int, T>> x(x: Map<Int, T>)
            """.trimIndent()
        )
    }

    fun `test parameterized function - 2`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    typealias X<T> = Map<Int, T>
                    fun <T: X<T>> x(x: X<T>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    typealias X<T> = Map<Int, T>
                    typealias Y<T> = X<T>
                    fun <T: Y<T>> x(x: Y<T>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                typealias X<T> = Map<Int, T>
                expect fun <T: X<T>> x(x: X<T>)
            """.trimIndent()
        )
    }

    fun `test parameterized function - 3`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Foo<A>
                    typealias Bar<A> = Foo<A>
                    fun <T0, T1> fn(arg: Bar<T1>) {}
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Foo<A>
                    typealias Bar<A> = Foo<A>
                    fun <T0, T1> fn(arg: Foo<T1>) {}
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Foo<A>()
                typealias Bar<A> = Foo<A>
                expect fun <T0, T1> fn(arg: Foo<T1>)
            """.trimIndent()
        )
    }

    fun `test member function - 0`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Foo<T1, T2, T3> {
                        fun t(x: Baz<T3>) = Unit
                    }
                    typealias Bar<T1, T2> = Foo<Int, T1, T2>                   
                    typealias Baz<T1> = Bar<String, T1>
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Foo<T1, T2, T3> {
                        fun t(x: Boo<T3>) {}
                    }
                    typealias Boo<T1> = Foo<Int, String, T1>
                    
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Foo<T1, T2, T3>() {
                    fun t(x: Foo<Int, String, T3>)
                }
            """.trimIndent()
        )
    }

    fun `test member function - 1`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Foo<T1, T2, T3> {
                        fun t(x: Z<T3>) = Unit
                    }

                    typealias X<T1, T2, T3> = Foo<T1, T2, T3>
                    typealias Y<T1, T3> = X<T1, String, T3>
                    typealias Z<T1> = Y<T1, Int>
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                   class Foo<T1, T2, T3> {
                        fun t(x: Y<T3, Int>) = Unit
                    }

                   typealias X<T1, T2, T3> = Foo<T1, T2, T3>
                   typealias Y<T1, T3> = X<T1, String, T3>
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Foo<T1, T2, T3>() {
                    fun t(x: Y<T3, Int>)
                }

                typealias X<T1, T2, T3> = Foo<T1, T2, T3>
                typealias Y<T1, T3> = X<T1, String, T3>
            """.trimIndent()
        )
    }

    @Suppress("unused") // https://youtrack.jetbrains.com/issue/KT-48850
    fun `KT-48850 - test non-commonizable type alias arguments - 0`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Foo<T1, T2, T3>
                    typealias X<T1, T2, T3> = Foo<T3, T2, T1>
                    fun<T1, T2, T3> x(x: X<T3, T2, T1>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Foo<T1, T2, T3>
                    typealias X<T1, T2, T3> = Foo<T1, T2, T3>
                    fun<T1, T2, T3> x(x: X<T1, T2, T3>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Foo<T1, T2, T3>() 
                expect fun<T1, T2, T3> x(x: Foo<T1, T2, T3>)
            """.trimIndent()
        )
    }

    @Suppress("unused") // https://youtrack.jetbrains.com/issue/KT-48850
    fun `KT-48850 - test non-commonizable type alias arguments - 1`() {
        val result = commonize {
            outputTarget("(a, b)")

            simpleSingleSourceTarget(
                "a", """
                    class Foo<T1, T2, T3>
                    typealias X<T1, T2, T3> = Foo<T3, T2, T1>
                    typealias Y<T1, T2, T3> = X<T1, T2, T3>
                    fun<T1, T2, T3> x(x: Y<T3, T2, T1>)
                """.trimIndent()
            )

            simpleSingleSourceTarget(
                "b", """
                    class Foo<T1, T2, T3>
                    typealias X<T1, T2, T3> = Foo<T1, T2, T3>
                    typealias Y<T1, T2, T3> = X<T1, T2, T3>
                    fun<T1, T2, T3> x(x: Y<T1, T2, T3>)
                """.trimIndent()
            )
        }

        result.assertCommonized(
            "(a, b)", """
                expect class Foo<T1, T2, T3>() 
                expect fun<T1, T2, T3> x(x: Foo<T1, T2, T3>)
            """.trimIndent()
        )
    }

    fun `test nested arguments`() {
        val result = commonize {
            outputTarget("(a, b)")
            "a" withSource """
                class X<T>
                class Y<T>
                fun<T> x(x: X<Y<T>>) = Unit
            """.trimIndent()

            "b" withSource """
                class X<T>
                class Y<T>
                fun<T> x(x: X<Y<T>>) = Unit
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X<T>()
                expect class Y<T>()
                expect fun<T> x(x: X<Y<T>>)
            """.trimIndent()
        )
    }

    fun `test nested arguments - with typealias`() {
        val result = commonize {
            outputTarget("(a, b)")
            "a" withSource """
                class X<T>
                class Y<T>
                typealias TA<T> = Y<T>
                fun<T> x(x: X<TA<T>>) = Unit
                fun y(x: X<TA<Unit>>) = Unit
            """.trimIndent()

            "b" withSource """
                class X<T>
                class Y<T>
                typealias TA<T> = Y<T>
                fun<T> x(x: X<TA<T>>) = Unit
                fun y(x: X<TA<Unit>>) = Unit
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
                expect class X<T>()
                expect class Y<T>()
                typealias TA<T> = Y<T>
                expect fun<T> x(x: X<TA<T>>)
                expect fun y(x: X<TA<Unit>>)
            """.trimIndent()
        )
    }

    fun `test KT-51686 - type argument is parameterized class`() {
        val result = commonize {
            outputTarget("(a, b)")
            "a" withSource """
                class CPointer<T>
                class CPointerVarOf<T: CPointer<*>>
                class ByteVarOf<T: Byte>
                typealias ByteVar = ByteVarOf<Byte>
                typealias CPointerVar<T> = CPointerVarOf<CPointer<T>> 
                
                fun x(x: CPointer<CPointerVar<ByteVar>>) = Unit
            """.trimIndent()

            "b" withSource """
                class CPointer<T>
                class CPointerVarOf<T: CPointer<*>>
                class ByteVarOf<T: Byte>
                typealias ByteVar = ByteVarOf<Byte>
                typealias CPointerVar<T> = CPointerVarOf<CPointer<T>> 
                
                fun x(x: CPointer<CPointerVar<ByteVar>>) = Unit
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
                expect class CPointer<T>()
                expect class CPointerVarOf<T: CPointer<*>>()
                expect class ByteVarOf<T: Byte>()
                typealias ByteVar = ByteVarOf<Byte>
                typealias CPointerVar<T> = CPointerVarOf<CPointer<T>> 
                
                expect fun x(x: CPointer<CPointerVar<ByteVar>>)
            """.trimIndent()
        )
    }

    fun `test KT-51686 - type argument is parameterized class - nullability - 0`() {
        val result = commonize {
            outputTarget("(a, b)")
            "a" withSource """
                class A
                class Y<T>
                class X<T>
                typealias TA<T> = X<Y<T>>
                fun f(): TA<A>? = null!!
            """.trimIndent()

            "b" withSource """
                class A
                class Y<T>
                class X<T>
                typealias TA<T> = X<Y<T>>
                fun f(): TA<A>? = null!!
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
                expect class A()
                expect class Y<T>()
                expect class X<T>()
                typealias TA<T> = X<Y<T>>
                expect fun f(): TA<A>?
            """.trimIndent()
        )
    }

    fun `test KT-51686 - type argument is parameterized class - nullability - 1`() {
        val result = commonize {
            outputTarget("(a, b)")
            "a" withSource """
                class A<T>
                class X<T>
                typealias TA1<T> = X<T>
                typealias TA2<T> = X<T>?
                                
                val p1: A<TA1<Unit>> get() = null!!
                val p2: A<TA1<Unit?> get() = null!!
                val p3: A<TA1<Unit>?> get() = null!!
                val p4: A<TA2<Unit>> get() = null!!
                val p5: A<TA2<Unit?>> get() = null!!
                val p6: A<TA2<Unit>?> get() = null!!
                val p7: A<TA1<Unit>>? get() = null!!
                val p8: TA2<A<Unit>> get() = null!!
                val p9: TA2<A<Unit>?> get() = null!!
                val p10: TA2<Unit> get() = null!!
                fun<T> f1(): A<TA1<TA2<T>>> = null!! 
            """.trimIndent()

            "b" withSource """
                class A<T>
                class X<T>
                typealias TA1<T> = X<T>
                typealias TA2<T> = X<T>?
                                
                val p1: A<TA1<Unit>> get() = null!!
                val p2: A<TA1<Unit?> get() = null!!
                val p3: A<TA1<Unit>?> get() = null!!
                val p4: A<TA2<Unit>> get() = null!!
                val p5: A<TA2<Unit?>> get() = null!!
                val p6: A<TA2<Unit>?> get() = null!!
                val p7: A<TA1<Unit>>? get() = null!!
                val p8: TA2<A<Unit>> get() = null!!
                val p9: TA2<A<Unit>?> get() = null!!
                val p10: TA2<Unit> get() = null!!
                fun<T> f1(): A<TA1<TA2<T>>> = null!! 
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
                expect class A<T>()
                expect class X<T>()
                typealias TA1<T> = X<T>
                typealias TA2<T> = X<T>?
                
                expect val p1: A<TA1<Unit>> 
                expect val p2: A<TA1<Unit?> 
                expect val p3: A<TA1<Unit>?>
                expect val p4: A<TA2<Unit>?>
                expect val p5: A<TA2<Unit?>?>
                expect val p6: A<TA2<Unit>?>
                expect val p7: A<TA1<Unit>>?
                expect val p8: TA2<A<Unit>>?
                expect val p9: TA2<A<Unit>?>?
                expect val p10: TA2<Unit>?
                expect fun<T> f1(): A<TA1<TA2<T>?>>
            """.trimIndent()
        )
    }
}
