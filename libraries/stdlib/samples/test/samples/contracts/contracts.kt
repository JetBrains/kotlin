/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package samples.contracts

import samples.*
import kotlin.test.*
import kotlin.contracts.*

@kotlin.contracts.ExperimentalContracts
fun returnsContract(condition: Boolean) {
    contract {
        returns() implies (condition)
    }
    if (!condition) throw IllegalArgumentException()
}

@kotlin.contracts.ExperimentalContracts
fun returnsTrueContract(condition: Boolean): Boolean {
    contract {
        returns(true) implies (condition)
    }
    return condition
}

@kotlin.contracts.ExperimentalContracts
fun returnsFalseContract(condition: Boolean): Boolean {
    contract {
        returns(false) implies (condition)
    }
    return !condition
}

@kotlin.contracts.ExperimentalContracts
fun returnsNullContract(condition: Boolean): Boolean? {
    contract {
        returns(null) implies (condition)
    }
    return if (condition) null else false
}

@kotlin.contracts.ExperimentalContracts
fun returnsNotNullContract(condition: Boolean): Boolean? {
    contract {
        returnsNotNull() implies (condition)
    }
    return if (condition) true else null
}

@kotlin.contracts.ExperimentalContracts
inline fun callsInPlaceAtMostOnceContract(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
}

@kotlin.contracts.ExperimentalContracts
inline fun callsInPlaceAtLeastOnceContract(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
    block()
}

@kotlin.contracts.ExperimentalContracts
inline fun <T> callsInPlaceExactlyOnceContract(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

@kotlin.contracts.ExperimentalContracts
inline fun callsInPlaceUnknownContract(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    block()
    block()
    block()
}

@RunWith(Enclosed::class)
class Contracts {

    class Initialization {

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun valPossibleInitialization() {
            val x: Int

            callsInPlaceAtMostOnceContract {
                x = 10
                assertEquals(10, x)
            }
        }

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun varInitializationOrReassignment() {
            var x: Int

            callsInPlaceAtLeastOnceContract {
                x = 10
            }

            assertEquals(10, x)
        }

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun valInitialization() {
            val x: Int

            callsInPlaceExactlyOnceContract {
                x = 10
            }

            assertEquals(10, x)
        }

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun varPossibleInitialization() {
            var x: Int

            callsInPlaceUnknownContract {
                x = 10
                assertEquals(10, x)
            }
        }
    }

    class Smartcasts {
        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun continueWithNotNull() {
            fun run(x: Any?) {
                returnsContract(x != null)
                assertNotNull(x)
            }

            try {
                run("Hello, Kotlin!")
                run(10)
                run(null)
            } catch (e: IllegalArgumentException) {

            }
        }

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun continueWithInt() {
            fun run(x: Number) {
                if (returnsTrueContract(x is Int)) {
                    @Suppress("USELESS_IS_CHECK")
                    assertTrue(x is Int)
                }
            }

            run(10)
            run(-.9)
            run(0L)
        }

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun continueWithNotNullString() {
            fun run(x: Any?) {
                if (!returnsFalseContract(x is String)) {
                    assertNotNull(x)
                    @Suppress("USELESS_IS_CHECK")
                    assertTrue(x is String)
                }
            }

            run("Hello, Kotlin!")
            run(10)
            run(null)
        }

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun continueWithNotNullInt() {
            fun run(x: Int?) {
                if (returnsNullContract(x != null) == null) {
                    assertNotNull(x)
                }
            }

            run(10)
            run(null)
        }

        @Sample
        @kotlin.contracts.ExperimentalContracts
        fun continueWithNull() {
            fun run(x: Any?) {
                if (returnsNotNullContract(x == null) != null) {
                    assertNull(x)
                }
            }

            run(10)
            run(null)
        }
    }
}
