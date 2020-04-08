package copytest

import dataclass.Settings

fun foo(s: Settings) {
    s.copy(1, 32)
    s.copy(1)
}

// KT-5975 data class copy method invocation gives errors in IDE when some optional parameters specified
/* Resolution of copy was called before parameters resolve */