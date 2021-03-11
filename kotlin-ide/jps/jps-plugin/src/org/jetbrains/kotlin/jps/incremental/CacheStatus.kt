/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

/**
 * Status that is used by system to perform required actions (i.e. rebuild something, clearing caches, etc...).
 */
enum class CacheStatus {
    /**
     * Cache is valid and ready to use.
     */
    VALID,

    /**
     * Cache is not exists or have outdated versions and/or other attributes.
     */
    INVALID,

    /**
     * Cache is exists, but not required anymore.
     */
    SHOULD_BE_CLEARED,

    /**
     * Cache is not exists and not required.
     */
    CLEARED
}