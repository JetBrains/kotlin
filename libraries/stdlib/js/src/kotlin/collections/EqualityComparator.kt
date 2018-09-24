/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

internal interface EqualityComparator {
    /**
     * Subclasses must override to return a value indicating
     * whether or not two keys or values are equal.
     */
    abstract fun equals(value1: Any?, value2: Any?): Boolean

    /**
     * Subclasses must override to return the hash code of a given key.
     */
    abstract fun getHashCode(value: Any?): Int


    object HashCode : EqualityComparator {
        override fun equals(value1: Any?, value2: Any?): Boolean = value1 == value2

        override fun getHashCode(value: Any?): Int = value?.hashCode() ?: 0
    }
}