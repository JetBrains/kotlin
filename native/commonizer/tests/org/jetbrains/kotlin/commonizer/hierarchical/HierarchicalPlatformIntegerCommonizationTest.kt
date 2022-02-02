/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.PlatformIntegerCommonizationEnabledKey
import org.jetbrains.kotlin.commonizer.assertCommonized

class HierarchicalPlatformIntegerCommonizationTest : AbstractInlineSourcesCommonizationTest() {
    fun `test signed ints`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                typealias X = Int
                typealias Y = Long
            """.trimIndent()

            "b" withSource """
                typealias X = Long
                typealias Y = Int
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            @UnsafeNumber(["a: kotlin.Int", "b: kotlin.Long"])
            typealias X = PlatformInt
            @UnsafeNumber(["a: kotlin.Long", "b: kotlin.Int"])
            typealias Y = PlatformInt
        """.trimIndent()
        )
    }

    fun `test unsigned ints`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                typealias X = UInt
                typealias Y = ULong
            """.trimIndent()

            "b" withSource """
                typealias X = ULong
                typealias Y = UInt
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            @UnsafeNumber(["a: kotlin.UInt", "b: kotlin.ULong"])
            typealias X = PlatformUInt
            @UnsafeNumber(["a: kotlin.ULong", "b: kotlin.UInt"])
            typealias Y = PlatformUInt
        """.trimIndent()
        )
    }

    fun `test signed vars`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlinx.cinterop.*
                
                typealias AX = Int
                typealias AY = Long
                typealias X = IntVarOf<AX>
                typealias Y = LongVarOf<AY>
            """.trimIndent()

            "b" withSource """
                import kotlinx.cinterop.*

                typealias AX = Long
                typealias AY = Int
                typealias X = LongVarOf<AX>
                typealias Y = IntVarOf<AY>
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlinx.cinterop.*                
            
            @UnsafeNumber(["a: kotlin.Int", "b: kotlin.Long"])
            typealias AX = PlatformInt
            @UnsafeNumber(["a: kotlin.Long", "b: kotlin.Int"])
            typealias AY = PlatformInt
            @UnsafeNumber(["a: kotlinx.cinterop.IntVarOf", "b: kotlinx.cinterop.LongVarOf"])
            typealias X = PlatformIntVarOf<AX>
            @UnsafeNumber(["a: kotlinx.cinterop.LongVarOf", "b: kotlinx.cinterop.IntVarOf"])
            typealias Y = PlatformIntVarOf<AY>
        """.trimIndent()
        )
    }

    fun `test unsigned vars`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlinx.cinterop.*
                
                typealias AX = UInt
                typealias AY = ULong
                typealias X = UIntVarOf<AX>
                typealias Y = ULongVarOf<AY>
            """.trimIndent()

            "b" withSource """
                import kotlinx.cinterop.*

                typealias AX = ULong
                typealias AY = UInt
                typealias X = ULongVarOf<AX>
                typealias Y = UIntVarOf<AY>
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlinx.cinterop.*                
            
            @UnsafeNumber(["a: kotlin.UInt", "b: kotlin.ULong"])
            typealias AX = PlatformUInt
            @UnsafeNumber(["a: kotlin.ULong", "b: kotlin.UInt"])
            typealias AY = PlatformUInt
            @UnsafeNumber(["a: kotlinx.cinterop.UIntVarOf", "b: kotlinx.cinterop.ULongVarOf"])
            typealias X = PlatformUIntVarOf<AX>
            @UnsafeNumber(["a: kotlinx.cinterop.ULongVarOf", "b: kotlinx.cinterop.UIntVarOf"])
            typealias Y = PlatformUIntVarOf<AY>
        """.trimIndent()
        )
    }

    fun `test signed arrays`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                typealias X = IntArray
                typealias Y = LongArray
            """.trimIndent()

            "b" withSource """
                typealias X = LongArray
                typealias Y = IntArray
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlinx.cinterop.*                
            
            typealias X = PlatformIntArray
            typealias Y = PlatformIntArray
        """.trimIndent()
        )
    }

    fun `test unsigned arrays`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                typealias X = UIntArray
                typealias Y = ULongArray
            """.trimIndent()

            "b" withSource """
                typealias X = ULongArray
                typealias Y = UIntArray
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlinx.cinterop.*                
            
            typealias X = PlatformUIntArray
            typealias Y = PlatformUIntArray
        """.trimIndent()
        )
    }

    fun `test signed ranges`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlin.ranges.*
                
                typealias X = IntRange
                typealias Y = LongRange
            """.trimIndent()

            "b" withSource """
                typealias X = LongRange
                typealias Y = IntRange
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlin.ranges.*                
            
            typealias X = PlatformIntRange
            typealias Y = PlatformIntRange
        """.trimIndent()
        )
    }

    fun `test unsigned ranges`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlin.ranges.*
                
                typealias X = UIntRange
                typealias Y = ULongRange
            """.trimIndent()

            "b" withSource """
                typealias X = ULongRange
                typealias Y = UIntRange
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlin.ranges.*                
            
            typealias X = PlatformUIntRange
            typealias Y = PlatformUIntRange
        """.trimIndent()
        )
    }

    fun `test signed progressions`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlin.ranges.*
                
                typealias X = IntProgression
                typealias Y = LongProgression
            """.trimIndent()

            "b" withSource """
                typealias X = LongProgression
                typealias Y = IntProgression
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlin.ranges.*                
            
            typealias X = PlatformIntProgression
            typealias Y = PlatformIntProgression
        """.trimIndent()
        )
    }

    fun `test unsigned progressions`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlin.ranges.*
                
                typealias X = UIntProgression
                typealias Y = ULongProgression
            """.trimIndent()

            "b" withSource """
                typealias X = ULongProgression
                typealias Y = UIntProgression
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlin.ranges.*                
            
            typealias X = PlatformUIntProgression
            typealias Y = PlatformUIntProgression
        """.trimIndent()
        )
    }

    fun `test platform types in return positions`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlin.ranges.*
                import kotlinx.cinterop.*
                
                class C {
                    val i: Int = null!!
                    fun v(): IntVarOf<Int> = null!!
                    fun r(): IntRange = null!!
                }
                
                val a: IntArray = null!!
                fun p(): IntProgression = null!!
            """.trimIndent()

            "b" withSource """
                import kotlin.ranges.*
                import kotlinx.cinterop.*
                
                class C {
                    val i: Long = null!!
                    fun v(): LongVarOf<Long> = null!!
                    fun r(): LongRange = null!!
                }
                
                val a: LongArray = null!!
                fun p(): LongProgression = null!!
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlin.ranges.*
            import kotlinx.cinterop.*
            
            expect class C() {
                val i: PlatformInt
                fun v(): PlatformIntVarOf<PlatformInt>
                fun r(): PlatformIntRange
            }

            expect val a: PlatformIntArray
            expect fun p(): PlatformIntProgression
        """.trimIndent()
        )
    }

    fun `test platform types in signatures`() {
        val result = commonize {
            outputTarget("(a, b)")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(a, b)")

            "a" withSource """
                import kotlin.ranges.*
                import kotlinx.cinterop.*
                
                class C {
                    fun i(arg: Int) {}
                    fun v(arg: IntVarOf<Int>) {}
                    fun r(arg: IntRange) {}
                }
                
                fun a(arg: IntArray) {}
                fun p(arg: IntProgression) {}
            """.trimIndent()

            "b" withSource """
                import kotlin.ranges.*
                import kotlinx.cinterop.*
                
                class C {
                    fun i(arg: Long) {}
                    fun v(arg: LongVarOf<Long>) {}
                    fun r(arg: LongRange) {}
                }
                
                fun a(arg: LongArray) {}
                fun p(arg: LongProgression) {}
            """.trimIndent()
        }

        result.assertCommonized(
            "(a, b)", """
            import kotlin.ranges.*
            import kotlinx.cinterop.*
            
            expect class C() {
            }
        """.trimIndent()
        )
    }
}
