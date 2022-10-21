/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.languageSetting

import org.jetbrains.kotlin.config.LanguageVersion
import java.util.Comparator

interface ConsistencyRelation<in T> {
    fun isConsistent(left: T?, right: T?): Boolean

    object DontCare : ConsistencyRelation<Any?> {
        override fun isConsistent(left: Any?, right: Any?): Boolean = true
    }

    class MonotonicAsc<T>(
        private val comparator: Comparator<T>,
        /** By default, null is infimum i.e. for every x from Language Settings Set : null <= x */
        private val nullIsSupremum: Boolean = false
    ) : ConsistencyRelation<T> {
        override fun isConsistent(left: T?, right: T?): Boolean = when {
            left == null && right == null -> true
            left == null && right != null -> !nullIsSupremum
            left != null && right == null -> nullIsSupremum
            left != null && right != null -> comparator.compare(left, right) <= 0 // -> left <= right
            else -> error("Invalid state")
        }
    }

    class MonotonicDesc<T>(
        private val comparator: Comparator<T>,
        /** By default, null is infimum i.e. for every x from Language Settings Set : null <= x */
        private val nullIsSupremum: Boolean = false
    ) : ConsistencyRelation<T> {
        override fun isConsistent(left: T?, right: T?): Boolean = when {
            left == null && right == null -> true
            left == null && right != null -> nullIsSupremum
            left != null && right == null -> !nullIsSupremum
            left != null && right != null -> comparator.compare(left, right) >= 0 // -> left >= right
            else -> error("Invalid state")
        }
    }

    object Constant : ConsistencyRelation<Any?> {
        override fun isConsistent(left: Any?, right: Any?): Boolean = left === right
    }
}

val languageVersionConsistencyRule = ConsistencyRelation.MonotonicDesc<String>(
    comparator = compareBy { LanguageVersion.fromVersionString(it) ?: error("Unknown version $it") }
)