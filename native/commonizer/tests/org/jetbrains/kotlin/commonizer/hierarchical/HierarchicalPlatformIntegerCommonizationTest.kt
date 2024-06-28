/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.OptimisticNumberCommonizationEnabledKey
import org.jetbrains.kotlin.commonizer.PlatformIntegerCommonizationEnabledKey
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.konan.target.KonanTarget.*

class HierarchicalPlatformIntegerCommonizationTest : AbstractInlineSourcesCommonizationTest() {
    fun `test signed ints without optimistic commonization`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            setting(OptimisticNumberCommonizationEnabledKey, false)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                typealias X = Int
                typealias Y = Long
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = Long
                typealias Y = Int
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            expect class X : Number
            typealias Y = PlatformInt
        """.trimIndent()
        )
    }

    fun `test signed ints with optimistic commonization backup`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            setting(OptimisticNumberCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                typealias X = Int
                typealias Y = Long
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = Long
                typealias Y = Int
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            @UnsafeNumber(["${LINUX_ARM32_HFP.name}: kotlin.Long", "${LINUX_ARM64.name}: kotlin.Int"])
            typealias X = Int
            typealias Y = PlatformInt
        """.trimIndent()
        )
    }

    fun `test unsigned ints`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                typealias X = UInt
                typealias Y = ULong
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = ULong
                typealias Y = UInt
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            @UnsafeNumber(["${LINUX_ARM32_HFP.name}: kotlin.ULong", "${LINUX_ARM64.name}: kotlin.UInt"])
            typealias X = UInt
            typealias Y = PlatformUInt
        """.trimIndent()
        )
    }

    fun `test signed vars`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                import kotlinx.cinterop.*
                
                typealias AX = Int
                typealias AY = Long
                typealias X = IntVarOf<AX>
                typealias Y = LongVarOf<AY>
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                import kotlinx.cinterop.*

                typealias AX = Long
                typealias AY = Int
                typealias X = LongVarOf<AX>
                typealias Y = IntVarOf<AY>
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlinx.cinterop.*                
            
            @UnsafeNumber(["${LINUX_ARM32_HFP.name}: kotlin.Long", "${LINUX_ARM64.name}: kotlin.Int"])
            typealias AX = Int
            typealias AY = PlatformInt
            @UnsafeNumber(["${LINUX_ARM32_HFP.name}: kotlinx.cinterop.LongVarOf<kotlin.Long>", "${LINUX_ARM64.name}: kotlinx.cinterop.IntVarOf<kotlin.Int>"])
            typealias X = IntVarOf<AX>
            typealias Y = PlatformIntVarOf<AY>
        """.trimIndent()
        )
    }

    fun `test unsigned vars`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                import kotlinx.cinterop.*
                
                typealias AX = UInt
                typealias AY = ULong
                typealias X = UIntVarOf<AX>
                typealias Y = ULongVarOf<AY>
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                import kotlinx.cinterop.*

                typealias AX = ULong
                typealias AY = UInt
                typealias X = ULongVarOf<AX>
                typealias Y = UIntVarOf<AY>
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlinx.cinterop.*                
            
            @UnsafeNumber(["${LINUX_ARM32_HFP.name}: kotlin.ULong", "${LINUX_ARM64.name}: kotlin.UInt"])
            typealias AX = UInt
            typealias AY = PlatformUInt
            @UnsafeNumber(["${LINUX_ARM32_HFP.name}: kotlinx.cinterop.ULongVarOf<kotlin.ULong>", "${LINUX_ARM64.name}: kotlinx.cinterop.UIntVarOf<kotlin.UInt>"])
            typealias X = UIntVarOf<AX>
            typealias Y = PlatformUIntVarOf<AY>
        """.trimIndent()
        )
    }

    fun `test signed arrays`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                typealias X = IntArray
                typealias Y = LongArray
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = LongArray
                typealias Y = IntArray
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlinx.cinterop.*                
            
            expect class X
            typealias Y = PlatformIntArray
        """.trimIndent()
        )
    }

    fun `test unsigned arrays`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                typealias X = UIntArray
                typealias Y = ULongArray
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = ULongArray
                typealias Y = UIntArray
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlinx.cinterop.*                
            
            expect class X
            typealias Y = PlatformUIntArray
        """.trimIndent()
        )
    }

    fun `test signed ranges`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                import kotlin.ranges.*
                
                typealias X = IntRange
                typealias Y = LongRange
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = LongRange
                typealias Y = IntRange
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlin.ranges.*                
            
            expect class X
            typealias Y = PlatformIntRange
        """.trimIndent()
        )
    }

    fun `test unsigned ranges`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                import kotlin.ranges.*
                
                typealias X = UIntRange
                typealias Y = ULongRange
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = ULongRange
                typealias Y = UIntRange
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlin.ranges.*                
            
            expect class X
            typealias Y = PlatformUIntRange
        """.trimIndent()
        )
    }

    fun `test signed progressions`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                import kotlin.ranges.*
                
                typealias X = IntProgression
                typealias Y = LongProgression
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = LongProgression
                typealias Y = IntProgression
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlin.ranges.*                
            
            expect class X
            typealias Y = PlatformIntProgression
        """.trimIndent()
        )
    }

    fun `test unsigned progressions`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
                import kotlin.ranges.*
                
                typealias X = UIntProgression
                typealias Y = ULongProgression
            """.trimIndent()

            LINUX_ARM32_HFP.name withSource """
                typealias X = ULongProgression
                typealias Y = UIntProgression
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlin.ranges.*                
            
            expect class X
            typealias Y = PlatformUIntProgression
        """.trimIndent()
        )
    }

    fun `test platform types in return positions`() {
        val result = commonize {
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM32_HFP.name withSource """
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

            LINUX_ARM64.name withSource """
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
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
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
            outputTarget("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})")

            LINUX_ARM64.name withSource """
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

            LINUX_ARM32_HFP.name withSource """
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
            "(${LINUX_ARM64.name}, ${LINUX_ARM32_HFP.name})", """
            import kotlin.ranges.*
            import kotlinx.cinterop.*
            
            expect class C()
        """.trimIndent()
        )
    }

    fun `test platform integers in multi-target commonization`() {
        val intTarget1 = LINUX_ARM32_HFP.name
        val intTarget2 = WATCHOS_ARM64.name
        val longTarget1 = LINUX_X64.name
        val longTarget2 = LINUX_ARM64.name

        val outputCommonizerTargets = arrayOf(
            "($intTarget1, $intTarget2)", "($longTarget1, $longTarget2)", "($intTarget1, $longTarget1)",
            "($intTarget1, $intTarget2, $longTarget1)",
            "($longTarget1, $longTarget2, $intTarget1)",
            "($intTarget1, $intTarget2, $longTarget1, $longTarget2)",
        )

        val result = commonize {
            setting(PlatformIntegerCommonizationEnabledKey, true)
            outputTarget(*outputCommonizerTargets)
            registerFakeStdlibIntegersDependency(*outputCommonizerTargets)

            intTarget1 withSource """
                import kotlinx.cinterop.*
                
                typealias X = Int
                typealias XV = IntVarOf<X>
            """.trimIndent()

            intTarget2 withSource """
                import kotlinx.cinterop.*
                
                typealias X = Int
                typealias XV = IntVarOf<X>
            """.trimIndent()

            longTarget1 withSource """
                import kotlinx.cinterop.*
                
                typealias X = Long
                typealias XV = LongVarOf<X>
            """.trimIndent()

            longTarget2 withSource """
                import kotlinx.cinterop.*
                
                typealias X = Long
                typealias XV = LongVarOf<X>
            """.trimIndent()

        }

        result.assertCommonized(
            "($intTarget1, $intTarget2)", """
            import kotlinx.cinterop.*
            
            typealias X = Int
            typealias XV = IntVarOf<X>
        """.trimIndent()
        )

        result.assertCommonized(
            "($longTarget1, $longTarget2)", """
            import kotlinx.cinterop.*
            
            typealias X = Long
            typealias XV = LongVarOf<X>
        """.trimIndent()
        )

        result.assertCommonized(
            "($intTarget1, $longTarget1)", """
            import kotlinx.cinterop.*
            
            typealias X = PlatformInt
            typealias XV = PlatformIntVarOf<X>
        """.trimIndent()
        )

        result.assertCommonized(
            "($intTarget1, $intTarget2, $longTarget1)", """
            import kotlinx.cinterop.*
            
            typealias X = PlatformInt
            typealias XV = PlatformIntVarOf<X>
        """.trimIndent()
        )

        result.assertCommonized(
            "($longTarget1, $longTarget2, $intTarget1)", """
            import kotlinx.cinterop.*
            
            typealias X = PlatformInt
            typealias XV = PlatformIntVarOf<X>
        """.trimIndent()
        )

        result.assertCommonized(
            "($intTarget1, $intTarget2, $longTarget1, $longTarget2)", """
            import kotlinx.cinterop.*
            
            typealias X = PlatformInt
            typealias XV = PlatformIntVarOf<X>
        """.trimIndent()
        )
    }

    fun `test platform types from known leaf targets are commonized`() {
        val result = commonize {
            outputTarget("(${LINUX_X64.name}, ${WATCHOS_ARM64.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${LINUX_X64.name}, ${WATCHOS_ARM64.name})")

            LINUX_X64.name withSource """
                val platformPropertyInOneLeafTarget: PlatformInt 
                    get() = null!!
                val platformPropertyInBothLeafTargets: PlatformInt 
                    get() = null!!
            """.trimIndent()

            WATCHOS_ARM64.name withSource """
                val platformPropertyInOneLeafTarget: Int 
                    get() = 42
                val platformPropertyInBothLeafTargets: PlatformInt 
                    get() = null!!
            """.trimIndent()
        }

        result.assertCommonized(
            "(${LINUX_X64.name}, ${WATCHOS_ARM64.name})", """
            expect val platformPropertyInOneLeafTarget: PlatformInt
            expect val platformPropertyInBothLeafTargets: PlatformInt
        """.trimIndent()
        )
    }

    fun `test platform types from unknown targets are not commonized`() {
        val result = commonize {
            outputTarget("(unknown, ${WATCHOS_ARM64.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(unknown, ${WATCHOS_ARM64.name})")

            "unknown" withSource """
                val platformPropertyInOneLeafTarget: PlatformInt 
                    get() = null!!
                val platformPropertyInOtherLeafTarget: Int 
                    get() = null!!
            """.trimIndent()

            WATCHOS_ARM64.name withSource """
                val platformPropertyInOneLeafTarget: Int 
                    get() = 42
                val platformPropertyInOtherLeafTarget: PlatformInt 
                    get() = null!!
            """.trimIndent()
        }

        result.assertCommonized(
            "(unknown, ${WATCHOS_ARM64.name})", "".trimIndent()
        )
    }

    // Issue: KT-51528
    fun `test multiple argument function with over the edge type alias available`() {
        val result = commonize {
            outputTarget("(${WATCHOS_ARM64.name}, ${IOS_ARM64.name})")
            setting(PlatformIntegerCommonizationEnabledKey, true)
            registerFakeStdlibIntegersDependency("(${WATCHOS_ARM64.name}, ${IOS_ARM64.name})")

            WATCHOS_ARM64.name withSource """
                typealias Arm32Alias = UInt
                typealias OtherAlias = UInt
                
                class Box<T>
                
                fun fn(
                    arg1: Box<kotlinx.cinterop.UIntVarOf<Arm32Alias>>, 
                    arg2: Box<kotlinx.cinterop.UIntVarOf<Arm32Alias>>
                ) {}
            """.trimIndent()

            IOS_ARM64.name withSource """
                typealias Arm64Alias = ULong
                typealias OtherAlias = ULong
                
                class Box<T>
                
                fun fn(
                    arg1: Box<kotlinx.cinterop.ULongVarOf<Arm64Alias>>, 
                    arg2: Box<kotlinx.cinterop.ULongVarOf<Arm64Alias>>
                ) {}
            """.trimIndent()
        }

        result.assertCommonized(
            "(${WATCHOS_ARM64.name}, ${IOS_ARM64.name})", """
                typealias OtherAlias = PlatformUInt
                
                expect class Box<T>()

                expect fun fn(
                    arg1: Box<kotlinx.cinterop.PlatformUIntVarOf<OtherAlias>>, 
                    arg2: Box<kotlinx.cinterop.PlatformUIntVarOf<OtherAlias>>
                )
            """.trimIndent()
        )
    }
}
