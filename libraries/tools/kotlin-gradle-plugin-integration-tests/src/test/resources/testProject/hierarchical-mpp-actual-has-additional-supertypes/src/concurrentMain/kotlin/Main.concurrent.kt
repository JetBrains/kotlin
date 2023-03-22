/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

actual class CompletionHandlerBase: CompletionHandler {
    override fun foo() {}
}

fun cancelFutureOnCompletionAlt(handlerBase: CompletionHandlerBase) {
    invokeOnCompletion(handlerBase)
    handlerBase.foo()
}
