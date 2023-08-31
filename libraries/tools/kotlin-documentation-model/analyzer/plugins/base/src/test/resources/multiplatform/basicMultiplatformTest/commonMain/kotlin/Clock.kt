/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package example

/**
 * Documentation for expected class Clock
 * in common module
 */
expect open class Clock() {
    fun getTime(): String
    /**
     * Time in minis
     */
    fun getTimesInMillis(): String
    fun getYear(): String
}

