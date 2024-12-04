/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.nio.file.Path

/**
 * The result of the `onError` function passed to [Path.copyToRecursively] that specifies further actions when an exception occurs.
 */
@ExperimentalPathApi
@SinceKotlin("1.8")
public enum class OnErrorResult {
    /**
     * If the entry that caused the error is a directory, skip the directory and its content, and
     * continue with the next entry outside this directory in the traversal order.
     * Otherwise, skip this entry and continue with the next entry in the traversal order.
     */
    SKIP_SUBTREE,

    /**
     * Stop the recursive copy function. The function will return without throwing exception.
     * To terminate the function with an exception rethrow instead.
     */
    TERMINATE
}