// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90507
// LANGUAGE: +CompanionBlocks
// RENDER_DIAGNOSTICS_FULL_TEXT
// FILE: Mint.kt
import kotlinx.atomicfu.atomic

class CompanionObjectMint {
    companion object {
        private val coinsAtomic = atomic(0)

        fun mint() {
            <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> local = atomic(0)

            class First {
                val <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>outerSeen<!> by coinsAtomic
                val localSeen by local
                val internal = atomic(0)
            }

            class Second {
                val <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>firstSeen<!> by First().internal
            }

            coinsAtomic.incrementAndGet()
        }
    }

    class Inspector {
        companion object {
            var <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by CompanionObjectMint.coinsAtomic
        }
    }
}

class ClassMint {
    internal val coinsAtomic = atomic(0)
    internal val <!INSTANCE_PROPERTIES_DELEGATED_TO_ATOMICS_CAN_ONLY_ACCESS_SAME_INSTANCE!>delegateInline<!> by ClassMint().coinsAtomic
}

class ClassInspector {
    var <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by ClassMint().coinsAtomic
}

object ObjectMint {
    internal val coinsAtomic = atomic(0)
}

class ObjectInspector {
    var <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by ObjectMint.coinsAtomic
}

class CompanionBlockMint {
    companion {
        private val coinsAtomic = atomic(0)
        private val <!COMPANION_PROPERTIES_CAN_ONLY_DELEGATE_TO_ATOMICS_IN_COMPANION_BLOCK!>outerSeen<!> by CompanionBlockMint().coinsAtomicInstance
    }

    class Inspector {
        companion {
            var <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by CompanionBlockMint.coinsAtomic
        }
    }

    private val coinsAtomicInstance = atomic(0)
    //TODO(KT-91662) This should stop being an error.
    var <!INSTANCE_PROPERTIES_DELEGATED_TO_ATOMICS_CAN_ONLY_ACCESS_SAME_INSTANCE!>companionSeen<!> by coinsAtomic
}

enum class EnumMint {
    FOO,
    BAR {
        private val barAtomic = atomic(0)
        override val seen by barAtomic
    };

    internal val coinsAtomic = atomic(0)
    open val seen by coinsAtomic
}

class EnumInspector {
    var <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by EnumMint.FOO.coinsAtomic
}

fun mint() {
   val coins = ClassMint()
   <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> localCoins = atomic(0)
   val lazyDelegate = lazy { 10 }
   <!LOCAL_PROPERTIES_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN!>val<!> seen by coins.coinsAtomic
   <!LOCAL_PROPERTIES_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN!>val<!> seenInline by ClassMint().coinsAtomic
   <!LOCAL_PROPERTIES_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN!>val<!> seenLocal by localCoins
   <!LOCAL_PROPERTIES_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN!>val<!> seenFunction by atomic(0)
   val lazyCoins by lazyDelegate
}

class SameContainer {
    private val atomicProperty = atomic(0)
    var delegated: Int by atomicProperty
    var delegatedWithThis by this.<!UNRESOLVED_REFERENCE!>coinsAtomic<!>
    var delegatedWithExplicitThis by this<!UNRESOLVED_LABEL!>@ClassMint<!>.coinsAtomic
}

class SameContainerCompanionObject {
    companion object {
        private val atomicProperty = atomic(0)
        var delegated: Int by atomicProperty
        val delegatedWithThis by this.atomicProperty
        val delegatedExplicit by SameContainerCompanionObject.atomicProperty
    }
}

object SameContainerObject {
    private val atomicProperty = atomic(0)
    var delegated: Int by atomicProperty
    var delegatedExplicit by SameContainerObject.atomicProperty
}

class SameContainerCompanionBlock {
    companion {
        private val atomicProperty = atomic(0)
        var delegated: Int by atomicProperty
        var delegatedExplicit by SameContainerCompanionBlock.atomicProperty
    }

    companion {
        var secondDelegated: Int by atomicProperty
        var secondDelegatedExplicit by SameContainerCompanionBlock.atomicProperty
    }
}

// FILE: TopLevel.kt
import kotlinx.atomicfu.atomic

internal val topLevelAtomic = atomic(0)
var topLevelDelegated: Int by topLevelAtomic

class SameFileClassInspector {
    var <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>seen<!>: Int by topLevelAtomic
}

// FILE: OtherFile.kt
var <!PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER!>otherFileDelegated<!>: Int by topLevelAtomic

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
integerLiteral, lambdaLiteral, localClass, localProperty, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, propertyDelegate, setter, thisExpression */
