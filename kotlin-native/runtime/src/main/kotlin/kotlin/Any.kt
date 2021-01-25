/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.identityHashCode
import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.GCCritical

/**
 * The root of the Kotlin class hierarchy. Every Kotlin class has [Any] as a superclass.
 */
@ExportTypeInfo("theAnyTypeInfo")
public open class Any {
    /**
     * Indicates whether some other object is "equal to" this one. Implementations must fulfil the following
     * requirements:
     *
     * * Reflexive: for any non-null value `x`, `x.equals(x)` should return true.
     * * Symmetric: for any non-null values `x` and `y`, `x.equals(y)` should return true if and only if `y.equals(x)` returns true.
     * * Transitive:  for any non-null values `x`, `y`, and `z`, if `x.equals(y)` returns true and `y.equals(z)` returns true, then `x.equals(z)` should return true.
     * * Consistent:  for any non-null values `x` and `y`, multiple invocations of `x.equals(y)` consistently return true or consistently return false, provided no information used in `equals` comparisons on the objects is modified.
     * * Never equal to null: for any non-null value `x`, `x.equals(null)` should return false.
     *
     * Read more about [equality](https://kotlinlang.org/docs/reference/equality.html) in Kotlin.
     */
    @SymbolName("Kotlin_Any_equals")
    @GCCritical
    external public open operator fun equals(other: Any?): Boolean

    /**
     * Returns a hash code value for the object.  The general contract of `hashCode` is:
     *
     * * Whenever it is invoked on the same object more than once, the `hashCode` method must consistently return the same integer, provided no information used in `equals` comparisons on the object is modified.
     * * If two objects are equal according to the `equals()` method, then calling the `hashCode` method on each of the two objects must produce the same integer result.
     */
    public open fun hashCode(): Int = this.identityHashCode()

    /**
     * Returns a string representation of the object.
     */
    public open fun toString(): String {
        val kClass = this::class
        val className = kClass.qualifiedName ?: kClass.simpleName ?: "<object>"
        // TODO: consider using [identityHashCode].
        val unsignedHashCode = this.hashCode().toLong() and 0xffffffffL
        val hashCodeStr = unsignedHashCode.toString(16)
        return "$className@$hashCodeStr"
    }
}
