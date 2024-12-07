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
 * A feature flag value that disables the feature can be created by calling [disabled] on the feature flag.
 */
sealed interface ComposeFeatureFlag : Named, Serializable {
    /**
     * Return a feature flag that will disable this feature.
     */
    fun disabled(): ComposeFeatureFlag

    /**
     * The enabled value of [feature]. These values are stored in a set they should have value identity.
     */
    private class Enabled(val feature: Feature) : ComposeFeatureFlag, Serializable {
        override fun disabled() = Disabled(feature)
        override fun getName(): String = feature.name
        override fun hashCode(): Int = feature.hashCode() * 17
        override fun equals(other: Any?): Boolean = other is Enabled && other.feature == feature
        override fun toString(): String = feature.flag
    }

    /**
     * The disabled value of [feature]. These values are stored in a set they should have value identity.
     */
    private class Disabled(val feature: Feature) : ComposeFeatureFlag, Serializable {
        override fun disabled(): ComposeFeatureFlag = this
        override fun getName(): String = "Disabled ${feature.name}"
        override fun hashCode(): Int = feature.hashCode() * 19
        override fun equals(other: Any?): Boolean = other is Disabled && other.feature == feature
        override fun toString(): String = "-${feature.flag}"
    }

    /**
     * The enum of all feature flags known by the Gradle plugin. This should be a superset of the features
     * known by the Compose Gradle plugin.
     *
     * When a feature flag is added to the compiler plugin,
     *   1) Add an enum value of the feature flag where the [flag] value is the token used in the `featureFlags` option of the
     *      compiler plugin.
     *   2) Add a @JvmField field in the companion object that will be accessible from Groovy adn used in build files to enable and
     *      disable the field.
     *   3) Add a doc comment to the field to explain what the feature does and if it is enabled or disabled by default.
     *
     * When a feature becomes enabled by default,
     *   1) Change the documentation for the feature flag to indicate that it is now on by default.
     *   - No other changes are required. The default enabled/disabled state of a feature is owned by the compiler plugin.
     *
     * When a feature flag is removed from the compiler plugin,
     *   1) Deprecate the @JvmField field in the companion object explaining that this flag is now ignored by the compiler plugin and should
     *      be removed from the configuration.
     *   2) Update the documentation to indicate that this flag is ignored.
     *   3) In a subsequent version remove the enum entry and the @JvmField field.
     */
    private enum class Feature(val flag: String) {
        StrongSkipping("StrongSkipping"),
        IntrinsicRemember("IntrinsicRemember"),
        OptimizeNonSkippingGroups("OptimizeNonSkippingGroups"),
        PausableComposition("PausableComposition"),
    }

    /**
     * Contains currently available [ComposeFeatureFlag]s.
     */
    companion object {
        /**
         * Enable strong skipping.
         *
         * Strong Skipping is a mode that improves the runtime performance of your application by skipping unnecessary
         * invocations of composable functions for which the parameters have not changed. In particular, when enabled, composable functions
         * with unstable parameters become skippable and lambdas with unstable captures will be memoized.
         *
         * For more information, see this link:
         *  - [Strong skipping](https://https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/strong-skipping.md)
         *
         * This feature is enabled by default. To disable, provide this feature flag in a [disabled] state:
         * ```
         * composeCompiler {
         *     featureFlags = setOf(ComposeFeatureFlag.StrongSkipping.disabled())
         * }
         * ```
         */
        @JvmField
        val StrongSkipping: ComposeFeatureFlag = Enabled(Feature.StrongSkipping)

        /**
         * Enable the intrinsic remember performance optimization.
         *
         * Intrinsic Remember is an optimization mode which improves the runtime performance of your application by inlining `remember`
         * invocations and replacing `.equals` comparison (for keys) with comparisons of the `$changed` meta parameter when possible. This
         * results in fewer slots being used and fewer comparisons being done at runtime.
         *
         * This feature is enabled by default. To disable, provide this feature flag in a [disabled] state:
         * ```
         * composeCompiler {
         *     featureFlags = setOf(ComposeFeatureFlag.IntrinsicRemember.disabled())
         * }
         * ```
         */
        @JvmField
        val IntrinsicRemember: ComposeFeatureFlag = Enabled(Feature.IntrinsicRemember)

        /**
         * Remove groups around non-skipping composable functions.
         *
         * Removing groups around non-skipping composable function is an experimental mode which improves the runtime performance of your
         * application by skipping unnecessary groups around composable functions which do not skip (and thus do not require a group). This
         * optimization will remove the groups around functions that are not skippable such as explicitly marked as
         * `@NonSkippableComposable` and functions that are implicitly not skippable, such as inline functions and functions that return a
         * non-`Unit` value such as `remember`.
         *
         * This feature is still considered experimental and is thus disabled by default. To enable, add this line
         * to the `composeCompiler {}` block:
         * ```
         * composeCompiler {
         *     featureFlags = setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups)
         * }
         * ```
         */
        @JvmField
        val OptimizeNonSkippingGroups: ComposeFeatureFlag = Enabled(Feature.OptimizeNonSkippingGroups)

        /**
         * Change the code generation of composable functions to enable pausing when a composable function is part of a pausable composition.
         *
         * Pausable composition is an experimental runtime feature. Experiments with this feature can be run by enabling this feature flag
         * and using a runtime version that supports pausable composition. If the runtime used does not support pausable composition, no
         * change is made to the code generation.
         *
         * This feature is still considered experimental and is thus disabled by default. To enable, add this line
         * to the `composeCompiler {}` block:
         *```
         * composeCompiler {
         *   featureFlag = setOf(ComposeFeatureFlag.PausableComposition)
         * }
         * ```
         */
        @JvmField
        val PausableComposition: ComposeFeatureFlag = Enabled(Feature.PausableComposition)
    }
}
