/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.nio.file.Path

/**
 * The result of the `copyAction` function passed to [Path.copyToRecursively] that specifies further actions when copying an entry.
 */
@ExperimentalPathApi
@SinceKotlin("1.8")
public enum class CopyActionResult {
    /**
     * Continue with the next entry in the traversal order.
     */
    CONTINUE,

    /**
     * Skip the directory content, continue with the next entry outside the directory in the traversal order.
     * For files this option is equivalent to [CONTINUE].
     */
    SKIP_SUBTREE,

    /**
     * Stop the recursive copy function. The function will return without throwing exception.
     */
    TERMINATE
}