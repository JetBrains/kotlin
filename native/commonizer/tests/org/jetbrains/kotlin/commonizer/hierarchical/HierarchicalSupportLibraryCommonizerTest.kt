/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.utils.InlineSourceBuilder
import org.jetbrains.kotlin.commonizer.utils.createModuleHierarchy

private fun buildMockSupportLibrary(): Map<String, InlineSourceBuilder.Module> =
    createModuleHierarchy(nameSourceSetsAs = { target -> "$target-support-module" }) {
        sourceSet("(((iosArm64, iosX64), tvos, (watchosArm64, watchosDeviceArm64)), linuxArm64)") {
            name = "native-main-support-module" // Workaround for `java.nio`'s "File name too long"
            source(
                """
                    package support
                    expect class NativeSSizeT
                    expect class NativeIntFast32T
                """.trimIndent(),
            )
        }.apply {
            refinedBySourceSet("((iosArm64, iosX64), tvos, (watchosArm64, watchosDeviceArm64))") {
                name = "apple-main-support-module" // Workaround for `java.nio`'s "File name too long"
                source(
                    """
                        package support
                        expect class AppleSSizeT
                        actual typealias NativeSSizeT = AppleSSizeT
                        actual typealias NativeIntFast32T = Int
                    """.trimIndent(),
                )
            }.apply {
                refinedBySourceSet("(iosArm64, iosX64)") {
                    source(
                        """
                            package support
                            actual typealias AppleSSizeT = Long
                        """.trimIndent()
                    )
                }.apply {
                    refinedBySourceSet("iosArm64") {}
                    refinedBySourceSet("iosX64") {}
                }

                refinedBySourceSet("tvos") {
                    source(
                        """
                            package support
                            actual typealias AppleSSizeT = Long
                        """.trimIndent()
                    )
                }

                refinedBySourceSet("(watchosArm64, watchosDeviceArm64)") {
                    source(
                        """
                            package support
                            expect class WatchosSSizeT
                            actual typealias AppleSSizeT = WatchosSSizeT
                        """.trimIndent()
                    )
                }.apply {
                    refinedBySourceSet("watchosArm64") {
                        source(
                            """
                                package support
                                actual typealias WatchosSSizeT = Int
                            """.trimIndent()
                        )
                    }

                    refinedBySourceSet("watchosDeviceArm64") {
                        source(
                            """
                                package support
                                actual typealias WatchosSSizeT = Long
                            """.trimIndent()
                        )
                    }
                }
            }

            refinedBySourceSet("linuxArm64") {
                source(
                    """
                        package support
                        actual typealias NativeSSizeT = Long
                        actual typealias NativeIntFast32T = Long
                    """.trimIndent(),
                )
            }
        }
    }

class HierarchicalSupportLibraryCommonizerTest : AbstractInlineSourcesCommonizationTest() {
    fun testFarawayLeaves() {
        val result = commonize {
            outputTarget("(iosArm64, watchosArm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(iosArm64, watchosArm64)")

            "iosArm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            "watchosArm64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            registerSupportLibrary(buildMockSupportLibrary())
        }

        result.assertCommonized(
            "(iosArm64, watchosArm64)",
            """
                expect fun foo(arg: support.AppleSSizeT)
            """.trimIndent()
        )
    }

    fun testTypealiasPreservation() {
        val result = commonize {
            outputTarget("(iosArm64, watchosArm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(iosArm64, watchosArm64)")

            "iosArm64" withSource """
                typealias TA = Long
                fun foo(arg: TA) {}
            """.trimIndent()

            "watchosArm64" withSource """
                typealias TA = Int
                fun foo(arg: TA) {}
            """.trimIndent()

            registerSupportLibrary(buildMockSupportLibrary())
        }

        result.assertCommonized(
            "(iosArm64, watchosArm64)",
            """
                typealias TA = support.AppleSSizeT
                expect fun foo(arg: TA)
            """.trimIndent()
        )
    }

    fun testIntermediateSupportClasses() {
        val result = commonize {
            outputTarget("(watchosArm64, watchosDeviceArm64)", "(iosArm64, (watchosArm64, watchosDeviceArm64))")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(iosArm64, (watchosArm64, watchosDeviceArm64))")

            "iosArm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            "watchosArm64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "watchosDeviceArm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            registerSupportLibrary(buildMockSupportLibrary())
        }

        result.assertCommonized(
            "(watchosArm64, watchosDeviceArm64)",
            """
                expect fun foo(arg: support.WatchosSSizeT)
            """.trimIndent()
        )

        result.assertCommonized(
            "(iosArm64, (watchosArm64, watchosDeviceArm64))",
            """
                expect fun foo(arg: support.AppleSSizeT)
            """.trimIndent()
        )
    }

    // See the contents
    fun testIncompleteHierarchyAmbiguity() {
        val result = commonize {
            outputTarget("(iosArm64, iosX64)", "((iosArm64, iosX64), watchosArm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("((iosArm64, iosX64), watchosArm64)")

            "iosArm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            "iosX64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "watchosArm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            val mockSupportLibrary = createModuleHierarchy(nameSourceSetsAs = { target -> "$target-support-module" }) {
                sourceSet("((iosArm64, iosX64), watchosArm64)") {
                    source(
                        """
                            package support
                            expect class NativeSomethingA
                            expect class NativeSomethingB
                        """.trimIndent(),
                    )
                }.apply {
                    refinedBySourceSet("iosArm64") {
                        source(
                            """
                                package support
                                actual typealias NativeSomethingA = Long
                                actual typealias NativeSomethingB = Long
                            """.trimIndent()
                        )
                    }

                    refinedBySourceSet("iosX64") {
                        source(
                            """
                                package support
                                actual typealias NativeSomethingA = Int
                                actual typealias NativeSomethingB = Int
                            """.trimIndent()
                        )
                    }

                    refinedBySourceSet("watchosArm64") {
                        source(
                            """
                                package support
                                actual typealias NativeSomethingA = Int
                                actual typealias NativeSomethingB = Long
                            """.trimIndent(),
                        )
                    }
                }
            }

            registerSupportLibrary(mockSupportLibrary)
        }

        result.assertCommonized(
            "(iosArm64, iosX64)",
            """
                expect fun foo(arg: support.NativeSomethingA)
            """.trimIndent()
        )

        result.assertCommonized(
            "((iosArm64, iosX64), watchosArm64)",
            """
                expect fun foo(arg: support.NativeSomethingB)
            """.trimIndent()
        )
    }

    fun testCustomUserSourceSets() {
        val result = commonize {
            outputTarget("(iosArm64, iosX64, linuxArm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(iosArm64, iosX64, linuxArm64)")

            "iosArm64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "iosX64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "linuxArm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            registerSupportLibrary(buildMockSupportLibrary())
        }

        result.assertCommonized(
            "(iosArm64, iosX64, linuxArm64)",
            """
                expect fun foo(arg: support.NativeIntFast32T)
            """.trimIndent()
        )
    }
}
