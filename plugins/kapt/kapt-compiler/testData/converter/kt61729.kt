// FULL_JDK

// FILE: a/LinkifyMask.kt
package a

import b.RestrictTo

object SphinxLinkify {
    private fun addLinks(
        @LinkifyMask mask: Int
    ) {}
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
annotation class LinkifyMask

// FILE: b/RestrictTo.java

package b;

public @interface RestrictTo {
    Scope[] value();

    enum Scope {
        LIBRARY_GROUP_PREFIX,
    }
}