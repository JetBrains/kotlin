/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.nio.file.Path

/**
 * Context for the `copyAction` function passed to [Path.copyToRecursively].
 */
@ExperimentalPathApi
@SinceKotlin("1.8")
public interface CopyActionContext {
    /**
     * Copies the entry located by this path to the specified [target] path,
     * except if both this and [target] entries are directories,
     * in which case the method completes without copying the entry.
     *
     * The entry is copied using `this.copyTo(target, *followLinksOption)`. See [kotlin.io.path.copyTo].
     *
     * @param target the path to copy this entry to.
     * @param followLinks `false` to copy the entry itself even if it's a symbolic link.
     *   `true` to copy its target if this entry is a symbolic link.
     *   If this entry is not a symbolic link, the value of this parameter doesn't make any difference.
     * @return [CopyActionResult.CONTINUE]
     */
    public fun Path.copyToIgnoringExistingDirectory(target: Path, followLinks: Boolean): CopyActionResult
}