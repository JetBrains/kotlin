/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
// TODO: generate automatically during build.
// The code below is generated with

fun main() {
    println("""
        package kotlinx.cinterop

        @OptIn(ExperimentalStdlibApi::class)
        @PublishedApi
        internal inline fun <reified T> t() = kotlin.reflect.typeOf<T>()

    """.trimIndent())

    repeat(23) { count ->
        val typeParameterNames = (1 .. count).map { "P$it" }
        val typeParameters = (typeParameterNames + "R").joinToString { "reified $it" }

        val functionType = buildString {
            append('(')
            typeParameterNames.joinTo(this)
            append(") -> R")
        }

        println("""
            inline fun <$typeParameters> staticCFunction(noinline function: $functionType): CPointer<CFunction<$functionType>> =
                    staticCFunctionImpl(
                            function,
                            ${(listOf("R") + typeParameterNames).joinToString { "t<$it>()" }}
                    )

        """.trimIndent())
    }
}
*/
package kotlinx.cinterop

@OptIn(ExperimentalStdlibApi::class)
@PublishedApi
internal inline fun <reified T> t() = kotlin.reflect.typeOf<T>()

inline fun <reified R> staticCFunction(noinline function: () -> R): CPointer<CFunction<() -> R>> =
        staticCFunctionImpl(
                function,
                t<R>()
        )

inline fun <reified P1, reified R> staticCFunction(noinline function: (P1) -> R): CPointer<CFunction<(P1) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>()
        )

inline fun <reified P1, reified P2, reified R> staticCFunction(noinline function: (P1, P2) -> R): CPointer<CFunction<(P1, P2) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>()
        )

inline fun <reified P1, reified P2, reified P3, reified R> staticCFunction(noinline function: (P1, P2, P3) -> R): CPointer<CFunction<(P1, P2, P3) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified R> staticCFunction(noinline function: (P1, P2, P3, P4) -> R): CPointer<CFunction<(P1, P2, P3, P4) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified P16, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>(), t<P16>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified P16, reified P17, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>(), t<P16>(), t<P17>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified P16, reified P17, reified P18, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>(), t<P16>(), t<P17>(), t<P18>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified P16, reified P17, reified P18, reified P19, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>(), t<P16>(), t<P17>(), t<P18>(), t<P19>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified P16, reified P17, reified P18, reified P19, reified P20, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>(), t<P16>(), t<P17>(), t<P18>(), t<P19>(), t<P20>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified P16, reified P17, reified P18, reified P19, reified P20, reified P21, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>(), t<P16>(), t<P17>(), t<P18>(), t<P19>(), t<P20>(), t<P21>()
        )

inline fun <reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8, reified P9, reified P10, reified P11, reified P12, reified P13, reified P14, reified P15, reified P16, reified P17, reified P18, reified P19, reified P20, reified P21, reified P22, reified R> staticCFunction(noinline function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R>> =
        staticCFunctionImpl(
                function,
                t<R>(), t<P1>(), t<P2>(), t<P3>(), t<P4>(), t<P5>(), t<P6>(), t<P7>(), t<P8>(), t<P9>(), t<P10>(), t<P11>(), t<P12>(), t<P13>(), t<P14>(), t<P15>(), t<P16>(), t<P17>(), t<P18>(), t<P19>(), t<P20>(), t<P21>(), t<P22>()
        )
