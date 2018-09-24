/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * A discriminated union that encapsulates successful outcome with a value of type [T]
 * or a failure with an arbitrary [Throwable] exception.
 * @suppress **Deprecated**: Renamed to [Result].
 */
@Deprecated(
    message = "Renamed to Result",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Result")
)
public typealias SuccessOrFailure<T> = Result<T>
