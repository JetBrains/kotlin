/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

public actual open class NoWhenBranchMatchedException : RuntimeException {
    public actual constructor()

    public actual constructor(message: String?) : super(message)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)

    public actual constructor(cause: Throwable?) : super(cause)
}
