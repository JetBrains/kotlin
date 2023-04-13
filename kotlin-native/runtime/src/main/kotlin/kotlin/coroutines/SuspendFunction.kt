/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.coroutines

/**
 * Represents a value of a functional type, such as a lambda, an anonymous function or a function reference.
 *
 * @param R return type of the function.
 */
@Deprecated("This interface will be removed in a future release")
@DeprecatedSinceKotlin(warningSince = "1.9")
public interface SuspendFunction<out R>
