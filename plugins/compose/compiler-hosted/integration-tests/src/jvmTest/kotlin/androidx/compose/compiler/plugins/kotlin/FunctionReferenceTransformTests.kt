/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class FunctionReferenceTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {

    @Before
    fun setUp() {
        assumeTrue(useFir)
    }

    @Test
    fun reference() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(int: Int) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (Int) -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(int: Int = 0) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (Int) -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(int: Int = 0) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable () -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_dispatch() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {
                @Composable
                fun Fn(int: Int) {}
            }
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(cls: Cls, content: @Composable (Int) -> Unit) {
                Ref(cls, cls::Fn)
            }
        """,
    )

    @Test
    fun reference_dispatch_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {
                @Composable
                fun Fn(int: Int = 0) {}
            }
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(cls: Cls, content: @Composable (Int) -> Unit) {
                Ref(cls, cls::Fn)
            }
        """,
    )

    @Test
    fun reference_dispatch_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {
                @Composable
                fun Fn(int: Int = 0) {}
            }
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(cls: Cls, content: @Composable () -> Unit) {
                Ref(cls, cls::Fn)
            }
        """,
    )

    @Test
    fun reference_extension() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {}

            @Composable 
            fun Cls.Fn(int: Int) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(cls: Cls, content: @Composable (Int) -> Unit) {
                Ref(cls, cls::Fn)
            }
        """,
    )

    @Test
    fun reference_extension_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {}

            @Composable 
            fun Cls.Fn(int: Int = 0) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(cls: Cls, content: @Composable (Int) -> Unit) {
                Ref(cls, cls::Fn)
            }
        """,
    )

    @Test
    fun reference_extension_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {}

            @Composable 
            fun Cls.Fn(int: Int = 0) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(cls: Cls, content: @Composable () -> Unit) {
                Ref(cls, cls::Fn)
            }
        """,
    )

    @Test
    fun reference_w_extension() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {}

            @Composable 
            fun Cls.Fn(int: Int) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable Cls.(Int) -> Unit) {
                Ref(Cls::Fn)
            }
        """,
    )

    @Test
    fun reference_w_extension_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {}

            @Composable 
            fun Cls.Fn(int: Int = 0) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable Cls.(Int) -> Unit) {
                Ref(Cls::Fn)
            }
        """,
    )

    @Test
    fun reference_w_extension_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            class Cls {}

            @Composable 
            fun Cls.Fn(int: Int = 0) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable Cls.() -> Unit) {
                Ref(Cls::Fn)
            }
        """,
    )

    @Test
    fun reference_w_extension_dispatch() = verifyGoldenComposeIrTransform(
    extra = """
            import androidx.compose.runtime.*
            
            class Cls {
                @Composable 
                fun Fn(string: String, int: Int) {}
            }
        """,
    source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(cls: Cls, content: @Composable String.(Int) -> Unit) {
                Ref(cls, cls::Fn)
            }
        """,
    )

    @Test
    fun reference_vararg() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(vararg int: Int) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (Int) -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_vararg_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(vararg int: Int = intArrayOf(0)) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (Int) -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_vararg_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(vararg int: Int = intArrayOf(0)) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable () -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_10_params() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(
                int1: Int,
                int2: Int,
                int3: Int,
                int4: Int,
                int5: Int,
                int6: Int,
                int7: Int,
                int8: Int,
                int9: Int,
                int10: Int
            ) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int
            ) -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_10_params_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(
                int1: Int,
                int2: Int = 0,
                int3: Int = 0,
                int4: Int = 0,
                int5: Int = 0,
                int6: Int = 0,
                int7: Int = 0,
                int8: Int = 0,
                int9: Int = 0,
                int10: Int = 0
            ) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (Int) -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_10_params_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(
                int1: Int,
                int2: Int = 0,
                int3: Int = 0,
                int4: Int = 0,
                int5: Int = 0,
                int6: Int = 0,
                int7: Int = 0,
                int8: Int = 0,
                int9: Int = 0,
                int10: Int = 0
            ) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int
            ) -> Unit) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_33_params_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(
                int1: Int,
                int2: Int,
                int3: Int,
                int4: Int,
                int5: Int,
                int6: Int,
                int7: Int,
                int8: Int,
                int9: Int,
                int10: Int,
                int11: Int,
                int12: Int,
                int13: Int,
                int14: Int,
                int15: Int,
                int16: Int,
                int17: Int,
                int18: Int,
                int19: Int,
                int20: Int,
                int21: Int,
                int22: Int,
                int23: Int,
                int24: Int,
                int25: Int,
                int26: Int,
                int27: Int,
                int28: Int,
                int29: Int,
                int30: Int,
                int31: Int,
                int32: Int,
                int33: Int = 0
            ) {}
        """,
        source = """
            import androidx.compose.runtime.*

            fun Ref(content: @Composable (
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int,
                Int
            ) -> Unit) {
                Ref(::Fn)
            }
        """
    )

    @Test
    fun reference_interface_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            interface Intf {
                @Composable
                fun Fn(int: Int = 0)
            }
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(intf: Intf, content: @Composable (Int) -> Unit) {
                Ref(intf, intf::Fn)
            }
        """,
    )

    @Test
    fun reference_interface_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            interface Intf {
                @Composable
                fun Fn(int: Int = 0)
            }
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(intf: Intf, content: @Composable () -> Unit) {
                Ref(intf, intf::Fn)
            }
        """,
    )

    @Test
    fun reference_int() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(int: Int): Int = 0
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (Int) -> Int) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_int_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(int: Int = 0): Int = 0
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (Int) -> Int) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_int_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun Fn(int: Int = 0): Int = 0
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable () -> Int) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_getter() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*

            val property: Int
                @Composable get() = 0
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable () -> Int) {
                Ref(::property::get)
            }
        """,
    )

    @Test
    fun reference_different_nullability() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Fn(str: String?): Int = 0
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Ref(content: @Composable (String) -> Int) {
                Ref(::Fn)
            }
        """,
    )

    @Test
    fun reference_into_val() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Fn(int: Int): Int = 0
        """,
        source = """
            import androidx.compose.runtime.*

            val property: @Composable (Int) -> Int = ::Fn

            @Composable
            fun Ref(content: @Composable (Int) -> Int) {
                Ref(property)
            }
        """
    )

    @Test
    fun generics() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun <T> Fn(t: T) {}
            fun IntFn(t: Int) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun <T> Ref(content: @Composable (T) -> Unit) {
                Ref<T>(::Fn)
                Ref(::IntFn)
            }
        """,
    )

    @Test
    fun generics_default() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun <T> Fn(t: T? = null) {}
            @Composable
            fun IntFn(t: Int = 0) {}
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun <T> Ref(content: @Composable (T) -> Unit) {
                Ref<T>(::Fn)
                Ref(::IntFn)
            }
        """,
        dumpTree = true
    )

    @Test
    fun generics_adapted() = verifyGoldenComposeIrTransform(
        extra = """
            import androidx.compose.runtime.*
            
            @Composable
            fun <T> Fn(t: T? = null): T = TODO()
            @Composable
            fun IntFn(t: Int = 0) = t
        """,
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun <T> Ref(content: @Composable () -> T) {
                Ref<T>(::Fn)
                Ref(::IntFn)
            }
        """,
    )
}