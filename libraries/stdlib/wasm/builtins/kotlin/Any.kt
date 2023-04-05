/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

import kotlin.random.*

/**
 * The root of the Kotlin class hierarchy. Every Kotlin class has [Any] as a superclass.
 */
public open class Any @WasmPrimitiveConstructor constructor() {
    // Pointer to runtime type info
    // Initialized by a compiler
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    internal var typeInfo: Int

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
    public open operator fun equals(other: Any?): Boolean =
        wasm_ref_eq(this, other)

    /**
     * Returns a hash code value for the object.  The general contract of `hashCode` is:
     *
     * * Whenever it is invoked on the same object more than once, the `hashCode` method must consistently return the same integer, provided no information used in `equals` comparisons on the object is modified.
     * * If two objects are equal according to the `equals()` method, then calling the `hashCode` method on each of the two objects must produce the same integer result.
     */
    internal var _hashCode: Int = 0
    public open fun hashCode(): Int {
        return identityHashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    public open fun toString(): String {
        val typeInfoPtr = this.typeInfo
        val packageName = getPackageName(typeInfoPtr)
        val simpleName = getSimpleName(typeInfoPtr)
        val qualifiedName = if (packageName.isEmpty()) simpleName else "$packageName.$simpleName"
        return "$qualifiedName@${identityHashCode()}"
    }
}

// Don't use outside, otherwise it could break classes reusing `_hashCode` field, like String.
// Don't inline it into usages, specifically to `hashCode`. 
// It was extracted to remove `toString`'s dependency on `hashCode`, which improves output size when DCE is involved.
private fun Any.identityHashCode(): Int {
    if (_hashCode == 0)
        _hashCode = Random.nextInt(1, Int.MAX_VALUE)
    return _hashCode
}
