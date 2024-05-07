/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle

import org.gradle.api.Named
import java.io.Serializable

/**
 * A feature flag is used to roll out features that will eventually become the default behavior of the Compose compiler plugin.
 *
 * A features flag value that disables the feature can be created by  calling [disable] on the feature flag.
 */
sealed interface ComposeFeatureFlag : Named, Serializable {
    /**
     * Return a feature flag that will disable this feature.
     */
    fun disable(): ComposeFeatureFlag

    /**
     * Enable strong skipping.
     *
     * Strong Skipping is a mode that improves the runtime performance of your application by skipping unnecessary
     * invocations of composable functions for which the parameters have not changed. In particular, when enabled, Composables with
     * unstable parameters become skippable and lambdas with unstable captures will be memoized.
     *
     * For more information, see this link:
     *  - [AndroidX strong skipping](https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/strong-skipping.md)
     *
     * This feature is still experimental and is disabled by default. To enable, include this feature flag,
     * ```
     * composeOptions {
     *     featureFlags = setOf(ComposeFeatureFlag.StrongSkipping)
     * }
     * ```
     */
    object StrongSkipping : ComposeFeatureFlag {
        override fun disable(): ComposeFeatureFlag = DisableStrongSkipping
        override fun getName() = "Strong skipping"
        override fun toString(): String = "StrongSkipping"
        private fun readResolve(): Any = StrongSkipping
    }

    private object DisableStrongSkipping : ComposeFeatureFlag {
        override fun disable(): ComposeFeatureFlag = this
        override fun getName(): String = "Disable Strong Skipping"
        override fun toString(): String = "-StrongSkipping"
        private fun readResolve(): Any = DisableStrongSkipping
    }

    /**
     * Enable the intrinsic remember performance optimization.
     *
     * Intrinsic Remember is an optimization mode which improves the runtime performance of your application by inlining `remember`
     * invocations and replacing `.equals` comparison (for keys) with comparisons of the `$changed` meta parameter when possible. This
     * results in fewer slots being used and fewer comparisons being done at runtime.
     *
     * This feature is on by default. It can be disabled by adding a disable flag by calling [disable] on this flag. To disable,
     * ```
     * composeOptions {
     *     featureFlags = setOf(ComposeFeatureFlag.IntrinsicRemember.disable())
     * }
     * ```
     */
    object IntrinsicRemember : ComposeFeatureFlag {
        override fun disable(): ComposeFeatureFlag = DisableIntrinsicRemember
        override fun getName() = "Intrinsic Remember"
        override fun toString(): String = "IntrinsicRemember"
        private fun readResolve(): Any = IntrinsicRemember
    }

    private object DisableIntrinsicRemember: ComposeFeatureFlag {
        override fun disable(): ComposeFeatureFlag = this
        override fun getName() = "Disable Intrinsic Remember"
        override fun toString(): String = "-IntrinsicRemember"
        private fun readResolve(): Any = DisableIntrinsicRemember
    }

    /**
     * Remove groups around non-skipping composable functions.
     *
     * Removing groups around non-skipping composables is an experimental mode which improves the runtime performance of your application
     * by skipping unnecessary groups around composables which do not skip (and thus do not require a group). This optimization will remove
     * the groups around functions that are not skippable such as explicitly marked as `@NonSkippableComposable` and functions that are
     * implicitly not skippable such inline functions and functions that return a non-Unit value such as remember.
     *
     * This feature is still considered experimental and is thus disabled by default. To enable,
     * ```
     * composeOptions {
     *     featureFlags = setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups)
     * }
     * ```
     */
    object OptimizeNonSkippingGroups : ComposeFeatureFlag {
        override fun disable(): ComposeFeatureFlag = DisableOptimizeNonSkippingGroups
        override fun getName() = "Optimize Non-Skipping Groups"
        override fun toString(): String = "OptimizeNonSkippingGroups"
        private fun readResolve(): Any = OptimizeNonSkippingGroups
    }

    private object DisableOptimizeNonSkippingGroups: ComposeFeatureFlag {
        override fun disable(): ComposeFeatureFlag = this
        override fun getName() = "Disable Optimize Non-Skipping Groups"
        override fun toString(): String = "-OptimizeNonSkippingGroups"
        private fun readResolve(): Any = DisableOptimizeNonSkippingGroups
    }
}
