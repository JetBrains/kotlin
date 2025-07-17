// FILE: O.kt
package test

annotation class Anno(val value: E)

@Anno(E.ENTRY)
object O

// FILE: test/E.java
package test;

import kotlin.Metadata;

@Metadata()
public enum E {
    ENTRY;
}
