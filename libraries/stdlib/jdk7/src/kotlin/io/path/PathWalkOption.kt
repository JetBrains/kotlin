/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.io.path

import java.nio.file.Path

/**
 * An enumeration to provide walk options for [Path.walk] function.
 * The options can be combined to form the walk order and behavior needed.
 *
 * Note that this enumeration is not exhaustive and new cases might be added in the future.
 */
@ExperimentalPathApi
@SinceKotlin("1.7")
public enum class PathWalkOption {
    /** Visits directories as well. */
    INCLUDE_DIRECTORIES,

    /** Walks in breadth-first order. */
    BREADTH_FIRST,

    /** Follows symbolic links to the directories they point to. */
    FOLLOW_LINKS
}