/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.assertCommonized
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HierarchicalModuleCommonizationTest : AbstractInlineSourcesCommonizationTest() {

    fun `test common modules hierarchically`() {
        val result = commonize {
            outputTarget("(a, b)", "(a, b, c)")

            target("a") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }

                module {
                    name = "bar"
                    source("val bar: Int = 1")
                }
            }

            target("b") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }

                module {
                    name = "bar"
                    source("val bar: Int = 1")
                }
            }

            target("c") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }

                module {
                    name = "not-bar"
                    source("val bar: Int = 1")
                }
            }
        }

        result.assertCommonized("(a, b)") {
            name = "foo"
            source("expect val foo: Int")
        }

        result.assertCommonized("(a, b)") {
            name = "bar"
            source("expect val bar: Int")
        }


        result.assertCommonized("(a, b, c)") {
            name = "foo"
            source("expect val foo: Int")
        }

        assertEquals(
            1, result.results[parseCommonizerTarget("(a, b, c)")].orEmpty().size,
            "Expected only a single module"
        )
    }

    fun `test module commonization with empty root not sharing any module`() {
        val result = commonize {
            outputTarget("(a, b)", "(a, b, c)")

            target("a") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }
            }

            target("b") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }
            }

            target("c") {
                module {
                    name = "bar"
                    source("val bar: Int = 1")
                }
            }
        }

        result.assertCommonized("(a, b)") {
            name = "foo"
            source("expect val foo: Int")
        }

        assertTrue(
            result.results[parseCommonizerTarget("(a, b, c)")].orEmpty().isEmpty(),
            "Expected empty result for (a, b, c)"
        )
    }

    fun `test no common modules`() {
        val result = commonize(Status.NOTHING_TO_DO) {
            outputTarget("(a, b)", "(a, b, c)")

            target("a") {
                module {
                    name = "a"
                    source("val foo: Int = 1")
                }
            }

            target("b") {
                module {
                    name = "b"
                    source("val foo: Int = 1")
                }
            }

            target("c") {
                module {
                    name = "c"
                    source("val foo: Int = 1")
                }
            }
        }

        assertTrue(result.results.isEmpty(), "Expected no results")
    }

    fun `test propagation`() {
        val result = commonize {
            outputTarget("(a, b)", "(a, b, c)", "(a, b, c, d)")

            target("a") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }
            }

            target("b") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }
            }

            target("d") {
                module {
                    name = "foo"
                    source("val foo: Int = 1")
                }
            }
        }

        result.assertCommonized("(a, b, c)") {
            name = "foo"
            source("expect val foo: Int")
        }

        result.assertCommonized("(a, b, c, d)") {
            name = "foo"
            source("expect val foo: Int")
        }
    }

    fun `test missing modules on two targets`() {
        val result = commonize {
            outputTarget("(a, b)")

            target("a") {
                module {
                    name = "shared"
                    source("class Shared")
                }

                module {
                    name = "onlyInA"
                    source("class A")
                }
            }

            target("b") {
                module {
                    name = "shared"
                    source("class Shared")
                }

                module {
                    name = "onlyInB"
                    source("class B")
                }
            }
        }

        assertEquals(1, result.results[parseCommonizerTarget("(a, b)")]?.size, "Expected only one commonized module")

        result.assertCommonized("(a, b)") {
            name = "shared"
            source("expect class Shared()")
        }
    }
}
