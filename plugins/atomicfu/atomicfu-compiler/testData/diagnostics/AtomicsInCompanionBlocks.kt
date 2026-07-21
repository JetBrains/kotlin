// LANGUAGE: +CompanionBlocks

import kotlinx.atomicfu.*

public class PublicComanionHolder {
    companion {
        <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>public<!> val pa = atomic(0)
        <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> val pi = atomic(0)
        @PublishedApi <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> val pip = atomic(0)
        private val pp = atomic(0)

        private <!ATOMIC_PROPERTIES_SHOULD_BE_VAL!>var<!> volatileAtomic = atomic(0)
    }
}

internal class InternalComanionHolder {
    companion {
        <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>public<!> val pa = atomic(0)
        <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> val pi = atomic(0)
        @PublishedApi <!NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN!>internal<!> val pip = atomic(0)
        private val pp = atomic(0)
    }
}

private class PrivateComanionHolder {
    companion {
        public val pa = atomic(0)
        internal val pi = atomic(0)
        @PublishedApi internal val pip = atomic(0)
        private val pp = atomic(0)
    }
}

fun box(): String {
    return "OK"
}
