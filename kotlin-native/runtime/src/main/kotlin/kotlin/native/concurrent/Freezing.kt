/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

/**
 * Exception thrown whenever freezing is not possible.
 *
 * @param toFreeze an object intended to be frozen.
 * @param blocker an object preventing freezing, usually one marked with [ensureNeverFrozen] earlier.
 */
@Deprecated("Support for the legacy memory manager has been completely removed. Usages of this exception can be safely dropped.")
@DeprecatedSinceKotlin(errorSince = "2.1")
public class FreezingException(toFreeze: Any, blocker: Any) :
        RuntimeException("freezing of $toFreeze has failed, first blocker is $blocker")

/**
 * Exception thrown whenever we attempt to mutate frozen objects.
 *
 * @param where a frozen object that was attempted to mutate
 */
@Deprecated("Support for the legacy memory manager has been completely removed. Usages of this exception can be safely dropped.")
@DeprecatedSinceKotlin(errorSince = "2.1")
public class InvalidMutabilityException(message: String) : RuntimeException(message)

/**
 * Freezes object subgraph reachable from this object. Frozen objects can be freely
 * shared between threads/workers.
 *
 * @throws FreezingException if freezing is not possible
 * @return the object itself
 * @see ensureNeverFrozen
 */
@Deprecated("Support for the legacy memory manager has been completely removed. Usages of this function can be safely dropped.", ReplaceWith("this"))
@DeprecatedSinceKotlin(errorSince = "2.1")
public fun <T> T.freeze(): T = this

/**
 * Checks if given object is null or frozen or permanent (i.e. instantiated at compile-time).
 *
 * @return true if given object is null or frozen or permanent
 */
@Deprecated("Support for the legacy memory manager has been completely removed. Consequently, this property is always `false`.", ReplaceWith("false"))
@DeprecatedSinceKotlin(errorSince = "2.1")
public val Any?.isFrozen: Boolean
    get() = false

/**
 * This function ensures that if we see such an object during freezing attempt - freeze fails and
 * [FreezingException] is thrown.
 *
 * @throws FreezingException thrown immediately if this object is already frozen
 * @see freeze
 */
@Deprecated("Support for the legacy memory manager has been completely removed. Usages of this function can be safely dropped.")
@DeprecatedSinceKotlin(errorSince = "2.1")
public fun Any.ensureNeverFrozen() {}