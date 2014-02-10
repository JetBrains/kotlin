// Attention Kotlin contributors: this file is used to build a minimal runtime suitable for tests. It's best to keep it as small as possible

package kotlin

public fun Any?.equals(other: Any?): Boolean =
    if (this == null) other == null else this.equals(other)

public fun Any?.toString(): String =
    if (this == null) "null" else this.toString()
