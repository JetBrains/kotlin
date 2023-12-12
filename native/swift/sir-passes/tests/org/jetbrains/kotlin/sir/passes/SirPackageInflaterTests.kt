/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.passes

import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.builder.buildEnum
import org.jetbrains.kotlin.sir.builder.buildForeignFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.builder.buildStruct
import org.jetbrains.kotlin.sir.util.SirComparator
import org.jetbrains.kotlin.sir.util.SirPrinter
import org.jetbrains.sir.passes.SirInflatePackagesPass
import org.jetbrains.sir.passes.run
import kotlin.test.Test

class SirPackageInflaterTests {
    @Test
    fun `should pass on empty module`() {
        fun buildModule(): SirModule = buildModule {
            name = "Root"
        }

        val actual = buildModule()
        val expected = buildModule()

        val pass = SirInflatePackagesPass()
        pass.run(actual)

        assertEqual(expected, actual)
    }

    @Test
    fun `should collect entities into a single flat namespace`() {
        val original = buildModule {
            name = "Root"

            declarations += listOf(
                makeFunction("com.foo"),
                makeFunction("com.bar"),
                makeFunction("com.baz"),
            )
        }

        val expected = buildModule {
            name = "Root"

            declarations += buildEnum {
                name = "com"
                origin = SirOrigin.Namespace(path =  listOf("com"))
                declarations += listOf(
                    makeFunction("com.foo"),
                    makeFunction("com.bar"),
                    makeFunction("com.baz"),
                )
            }
        }

        val pass = SirInflatePackagesPass()
        val actual = pass.run(original)

        assertEqual(expected, actual)
    }

    @Test
    fun `should leave other declarations alone`() {
        val original = buildModule {
            name = "Root"

            declarations += listOf(
                buildStruct { name = "Orphan" },
                makeFunction("com.foo"),
                makeFunction("com.bar"),
            )
        }

        val expected = buildModule {
            name = "Root"

            declarations += listOf(
                buildStruct { name = "Orphan" },
                buildEnum {
                    name = "com"
                    origin = SirOrigin.Namespace(path =  listOf("com"))
                    declarations += listOf(
                        makeFunction("com.foo"),
                        makeFunction("com.bar"),
                    )
                },
            )
        }

        val pass = SirInflatePackagesPass()
        val actual = pass.run(original)

        assertEqual(expected, actual)
    }


    @Test
    fun `should collect entities into multiple namespaces`() {
        val original = buildModule {
            name = "Root"

            declarations += listOf(
                makeFunction("com.foo"),
                makeFunction("org.bar"),
                makeFunction("com.baz"),
            )
        }

        val expected = buildModule {
            name = "Root"

            declarations += buildEnum {
                name = "com"
                origin = SirOrigin.Namespace(path =  listOf("com"))
                declarations += listOf(
                    makeFunction("com.foo"),
                    makeFunction("com.baz"),
                )

            }

            declarations += buildEnum {
                name = "org"
                origin = SirOrigin.Namespace(path =  listOf("org"))
                declarations += listOf(
                    makeFunction("org.bar"),
                )
            }
        }

        val pass = SirInflatePackagesPass()
        val actual = pass.run(original)

        assertEqual(expected, actual)
    }

    @Test
    fun `should collect entities into multiple nested namespaces`() {
        val original = buildModule {
            name = "Root"

            declarations += listOf(
                makeFunction("orphan"),
                makeFunction("com.foo"),
                makeFunction("org.bar"),
                makeFunction("com.baz"),
                makeFunction("org.jetbrains.baz"),
                makeFunction("org.jetbrains.mascots.kotlin.kodee"),
            )
        }

        val expected = buildModule {
            name = "Root"

            declarations += listOf(
                makeFunction("orphan"),
                buildEnum {
                    name = "com"
                    origin = SirOrigin.Namespace(path =  listOf("com"))
                    declarations += listOf(
                        makeFunction("com.foo"),
                        makeFunction("com.baz"),
                    )
                },
                buildEnum {
                    name = "org"
                    origin = SirOrigin.Namespace(path =  listOf("org"))
                    declarations += listOf(
                        makeFunction("org.bar"),
                        buildEnum {
                            name = "jetbrains"
                            origin = SirOrigin.Namespace(path =  listOf("org", "jetbrains"))
                            declarations += listOf(
                                makeFunction("org.jetbrains.baz"),
                                buildEnum {
                                    name = "mascots"
                                    origin = SirOrigin.Namespace(path =  listOf("org", "jetbrains", "mascots"))
                                    declarations += buildEnum {
                                        name = "kotlin"
                                        origin = SirOrigin.Namespace(path =  listOf("org", "jetbrains", "mascots", "kotlin"))
                                        declarations += makeFunction("org.jetbrains.mascots.kotlin.kodee")
                                    }
                                }
                            )
                        }
                    )
                }
            )
        }

        val pass = SirInflatePackagesPass()
        val actual = pass.run(original)

        assertEqual(expected, actual)
    }
}

private fun makeFunction(fqName: String) = buildForeignFunction {
    val path = fqName.split(".")
    assert(path.isNotEmpty())
    origin = SirOrigin.Foreign.Unknown(path)
}

private fun assertEqual(expected: SirModule, actual: SirModule) {
    assert(SirComparator(options = setOf(SirComparator.Options.COMPARE_ORIGINS)).areEqual(expected, actual)) {
        "\nExpected:\n\n${SirPrinter.toString(expected)}\n\nActual:\n\n${SirPrinter.toString(actual)}"
    }
}