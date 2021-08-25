/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatform

@Target(AnnotationTarget.CLASS)
annotation class AssociatedWithPlatform(val platform: Platform)

@Target(AnnotationTarget.CLASS)
annotation class AttributeKey(val key: String)

/**
 * FIXME: Might be inconvenient to use such interface
 */
@Serializable
sealed class Attribute {

    sealed interface Key {
        /**
         * In order to be able to say
         * whether an Attribute is compatible with null and vice-versa.
         * There should be some 'static' method associated with certain Attribute Class that resolves
         * compatibility among null valued attributes.
         *
         * Since Key is always associated with certain Attribute I thought it is logical to have such logic in it.
         */
        fun isCompatible(left: Attribute?, right: Attribute?): Boolean
    }

    abstract class MonotonicKey(
        /**
         * `true` means monotonously non-increase in refinement closure
         * * null -> null : ok
         * * null -> XXX  : not ok
         * * XXX  -> null : ok
         *
         * `false` means monotonously non-decrease in refinement closure
         * * null -> null : ok
         * * null -> XXX  : ok
         * * XXX  -> null : not ok
         */
        val isNotIncreasing: Boolean
    ) : Key {
        final override fun isCompatible(left: Attribute?, right: Attribute?): Boolean = when {
            left == null && right == null -> true
            left == null && right != null -> !isNotIncreasing
            left != null && right == null -> isNotIncreasing
            else -> left!!.isCompatible(right!!)
        }
    }

    abstract fun isCompatible(another: Attribute): Boolean
}

/**
 * Platforms should monotonously non-decrease.
 * Examples, symbol `->` means `isCompatible`
 * **Correct**: null -> (JS) -> (JS) -> (JS, JVM) -> (JS, JVM, Native)
 * **Incorrect**: (JS) -> (JVM); (JS, JVM) -> (JVM);
 */
@Serializable
@AttributeKey("platforms")
data class Platforms(
    val platforms: Set<Platform>
): Attribute() {
    constructor(vararg platforms: Platform): this(setOf(*platforms))

    companion object : MonotonicKey(isNotIncreasing = false) {
        override fun toString(): String = "KPM Attribute 'Platforms'"
    }

    override fun isCompatible(another: Attribute): Boolean {
        if (another !is Platforms) return false
        return another.platforms.containsAll(platforms)
    }
}

@Serializable
@AssociatedWithPlatform(Platform.JVM)
@AttributeKey("jvm.target")
data class JvmTargetAttribute(
    val value: JvmTarget
) : Attribute() {
    companion object : MonotonicKey(isNotIncreasing = false)

    override fun isCompatible(another: Attribute): Boolean {
        if (another !is JvmTargetAttribute) return false
        return this.value >= another.value
    }
}