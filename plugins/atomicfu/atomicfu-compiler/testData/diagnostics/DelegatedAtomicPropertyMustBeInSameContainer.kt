// ISSUE: KT-90507
// LANGUAGE: +CompanionBlocks
// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.atomic

class CompanionObjectMint {
    companion object {
        private val coinsAtomic = atomic(0)

        fun mint() {
            <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> local = atomic(0)

            class First {
                val <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>outerSeen<!> by coinsAtomic
                val <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>localSeen<!> by local
                val internal = atomic(0)
            }

            class Second {
                val <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>firstSeen<!> by First().internal
            }

            coinsAtomic.incrementAndGet()
        }
    }

    class Inspector {
        companion object {
            var <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by CompanionObjectMint.coinsAtomic
        }
    }
}

class ClassMint {
    internal val coinsAtomic = atomic(0)
}

class ClassInspector {
    var <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by ClassMint().coinsAtomic
}

object ObjectMint {
    internal val coinsAtomic = atomic(0)
}

class ObjectInspector {
    var <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by ObjectMint.coinsAtomic
}

class CompanionBlockMint {
    companion {
        private val coinsAtomic = atomic(0)
    }

    class Inspector {
        companion {
            var <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by CompanionBlockMint.coinsAtomic
        }
    }
}

enum class EnumMint {
    FOO, BAR;

    internal val coinsAtomic = atomic(0)
}

class EnumInspector {
    var <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by EnumMint.FOO.coinsAtomic
}

class SameContainer {
    private val atomicProperty = atomic(0)
    var delegated: Int by atomicProperty
}

class SameContainerCompanionObject {
    companion object {
        private val atomicProperty = atomic(0)
        var delegated: Int by atomicProperty
    }
}

class SameContainerCompanionBlock {
    companion {
        private val atomicProperty = atomic(0)
        var delegated: Int by atomicProperty
    }
}

fun mint() {
   val coins = ClassMint()
   <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> localCoins = atomic(0)
   val lazyDelegate = lazy { 10 }
   <!LOCALS_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN!>val<!> seen by coins.coinsAtomic
   <!LOCALS_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN!>val<!> seenLocal by localCoins
   val lazyCoins by lazyDelegate
}

fun ClassMint.doStuff() = coinsAtomic.incrementAndGet()

// FILE: TopLevel.kt
internal val topLevelAtomic = atomic(0)
var topLevelDelegated: Int by topLevelAtomic

class SameFileClassInspector {
    var <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by topLevelAtomic
}

// FILE: OtherFile.kt
var <!ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER!>otherFileDelegated<!>: Int by topLevelAtomic
