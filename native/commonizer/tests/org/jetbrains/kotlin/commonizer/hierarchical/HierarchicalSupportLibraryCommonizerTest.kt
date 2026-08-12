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
        sourceSet("(((ios_arm64, ios_x64), tvos, (watchos_arm64, watchos_device_arm64)), linux_arm64)") {
            name = "native-main-support-module" // Workaround for `java.nio`'s "File name too long"
            source(
                """
                    package support
                    expect class NativeSSizeT
                    expect class NativeIntFast32T
                """.trimIndent(),
            )
        }.apply {
            refinedBySourceSet("((ios_arm64, ios_x64), tvos, (watchos_arm64, watchos_device_arm64))") {
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
                refinedBySourceSet("(ios_arm64, ios_x64)") {
                    source(
                        """
                            package support
                            actual typealias AppleSSizeT = Long
                        """.trimIndent()
                    )
                }.apply {
                    refinedBySourceSet("ios_arm64") {}
                    refinedBySourceSet("ios_x64") {}
                }

                refinedBySourceSet("tvos") {
                    source(
                        """
                            package support
                            actual typealias AppleSSizeT = Long
                        """.trimIndent()
                    )
                }

                refinedBySourceSet("(watchos_arm64, watchos_device_arm64)") {
                    source(
                        """
                            package support
                            expect class WatchosSSizeT
                            actual typealias AppleSSizeT = WatchosSSizeT
                        """.trimIndent()
                    )
                }.apply {
                    refinedBySourceSet("watchos_arm64") {
                        source(
                            """
                                package support
                                actual typealias WatchosSSizeT = Int
                            """.trimIndent()
                        )
                    }

                    refinedBySourceSet("watchos_device_arm64") {
                        source(
                            """
                                package support
                                actual typealias WatchosSSizeT = Long
                            """.trimIndent()
                        )
                    }
                }
            }

            refinedBySourceSet("linux_arm64") {
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
            outputTarget("(ios_arm64, watchos_arm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(ios_arm64, watchos_arm64)")

            "ios_arm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            "watchos_arm64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            registerSupportLibrary(buildMockSupportLibrary())
        }

        result.assertCommonized(
            "(ios_arm64, watchos_arm64)",
            """
                expect fun foo(arg: support.AppleSSizeT)
            """.trimIndent()
        )
    }

    fun testTypealiasPreservation() {
        val result = commonize {
            outputTarget("(ios_arm64, watchos_arm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(ios_arm64, watchos_arm64)")

            "ios_arm64" withSource """
                typealias TA = Long
                fun foo(arg: TA) {}
            """.trimIndent()

            "watchos_arm64" withSource """
                typealias TA = Int
                fun foo(arg: TA) {}
            """.trimIndent()

            registerSupportLibrary(buildMockSupportLibrary())
        }

        result.assertCommonized(
            "(ios_arm64, watchos_arm64)",
            """
                typealias TA = support.AppleSSizeT
                expect fun foo(arg: TA)
            """.trimIndent()
        )
    }

    private fun testIntermediateSupportClasses(configureSupportLibrary: ParametersBuilder.() -> Unit) {
        val result = commonize {
            outputTarget("(watchos_arm64, watchos_device_arm64)", "(ios_arm64, (watchos_arm64, watchos_device_arm64))")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(ios_arm64, (watchos_arm64, watchos_device_arm64))")

            "ios_arm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            "watchos_arm64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "watchos_device_arm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            configureSupportLibrary()
        }

        result.assertCommonized(
            "(watchos_arm64, watchos_device_arm64)",
            """
                expect fun foo(arg: support.WatchosSSizeT)
            """.trimIndent()
        )

        result.assertCommonized(
            "(ios_arm64, (watchos_arm64, watchos_device_arm64))",
            """
                expect fun foo(arg: support.AppleSSizeT)
            """.trimIndent()
        )
    }

    fun testIntermediateSupportClassesWithMockSupportLibrary() = testIntermediateSupportClasses {
        registerSupportLibrary(buildMockSupportLibrary())
    }

    fun testIntermediateSupportClassesWithRealSupportLibrary() = testIntermediateSupportClasses {
        registerRealSupportLibrary()
    }

    // See the contents
    fun testIncompleteHierarchyAmbiguity() {
        val result = commonize {
            outputTarget("(ios_arm64, ios_x64)", "((ios_arm64, ios_x64), watchos_arm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("((ios_arm64, ios_x64), watchos_arm64)")

            "ios_arm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            "ios_x64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "watchos_arm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            val mockSupportLibrary = createModuleHierarchy(nameSourceSetsAs = { target -> "$target-support-module" }) {
                sourceSet("((ios_arm64, ios_x64), watchos_arm64)") {
                    source(
                        """
                            package support
                            expect class NativeSomethingA
                            expect class NativeSomethingB
                        """.trimIndent(),
                    )
                }.apply {
                    refinedBySourceSet("ios_arm64") {
                        source(
                            """
                                package support
                                actual typealias NativeSomethingA = Long
                                actual typealias NativeSomethingB = Long
                            """.trimIndent()
                        )
                    }

                    refinedBySourceSet("ios_x64") {
                        source(
                            """
                                package support
                                actual typealias NativeSomethingA = Int
                                actual typealias NativeSomethingB = Int
                            """.trimIndent()
                        )
                    }

                    refinedBySourceSet("watchos_arm64") {
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
            "(ios_arm64, ios_x64)",
            """
                expect fun foo(arg: support.NativeSomethingA)
            """.trimIndent()
        )

        result.assertCommonized(
            "((ios_arm64, ios_x64), watchos_arm64)",
            """
                expect fun foo(arg: support.NativeSomethingB)
            """.trimIndent()
        )
    }

    fun testCustomUserSourceSets() {
        val result = commonize {
            outputTarget("(ios_arm64, ios_x64, linux_arm64)")
            setting(OptimisticNumberCommonizationEnabledKey, true)

            registerFakeStdlibIntegersDependency("(ios_arm64, ios_x64, linux_arm64)")

            "ios_arm64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "ios_x64" withSource """
                fun foo(arg: Int) {}
            """.trimIndent()

            "linux_arm64" withSource """
                fun foo(arg: Long) {}
            """.trimIndent()

            registerSupportLibrary(buildMockSupportLibrary())
        }

        result.assertCommonized(
            "(ios_arm64, ios_x64, linux_arm64)",
            """
                expect fun foo(arg: support.NativeIntFast32T)
            """.trimIndent()
        )
    }
}
