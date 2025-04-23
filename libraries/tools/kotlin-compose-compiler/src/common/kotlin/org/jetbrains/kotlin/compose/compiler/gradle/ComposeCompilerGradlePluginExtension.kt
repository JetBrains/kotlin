/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.compose.compiler.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import javax.inject.Inject

/**
 * Provides DSL to configure Compose compiler plugin options.
 *
 * It is available in the build scripts as the `composeCompiler {}` block:
 * ```
 * composeCompiler {
 *    ...
 * }
 * ```
 */
abstract class ComposeCompilerGradlePluginExtension @Inject internal constructor(objectFactory: ObjectFactory) {
    /**
     * Generate function key metaclasses with annotations indicating the functions and their group keys.
     *
     * Generally used for tooling.
     */
    @Deprecated("The user facing property is deprecated. Intended for tooling use only. Will be removed in future releases.")
    val generateFunctionKeyMetaClasses: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    /**
     * Include source information in generated code.
     *
     * Records source information that can be used for tooling to determine the source location of the corresponding composable function.
     * By default, this function is declared as having no side effects. It is safe for code shrinking tools (such as R8 or ProGuard) to
     * remove it. This option does NOT impact the presence of symbols or line information normally added by the Kotlin compiler; this option
     * controls only additional source information added by the Compose Compiler.
     */
    val includeSourceInformation: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * Save Compose build metrics to this folder.
     *
     * When specified, the Compose compiler will dump metrics about the current module, which can be useful when manually optimizing your
     * application's runtime performance. The module.json file will include the statistics about processed composables and classes, including
     * number of stable classes/parameters, skippable functions, and so on.
     *
     * To distinguish metrics files for different [KotlinTargets][KotlinTarget], the plugin uses a special subdirectory structure based on
     * [KotlinTarget.disambiguationClassifier]/[KotlinCompilation.compilationName] values. For example, the following
     * Kotlin multiplatform project configuration registers the JVM target with a custom compilation:
     * ```
     * kotlin {
     *    jvm {
     *       compilations.register("custom")
     *    }
     * }
     * ```
     * The Compose plugin uses this value to create the following subdirectory structure:
     * - `<metricsDestination-value>/jvm/main`
     * - `<metricsDestination-value>/jvm/custom`
     *
     * For more information, see these links:
     *  - [AndroidX compiler metrics](https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/compiler-metrics.md)
     *  - [Composable metrics blog post](https://chrisbanes.me/posts/composable-metrics/)
     */
    abstract val metricsDestination: DirectoryProperty

    /**
     * Save Compose build reports to this folder.
     *
     * When specified, the Compose compiler will dump reports about the compilation, which can be useful when manually optimizing
     * your application's runtime performance. These reports include information on which of your composable functions are skippable,
     * which are restartable, which are readonly, etc.
     *
     * For more information, see these links:
     *  - [AndroidX compiler metrics](https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/compiler-metrics.md)
     *  - [Composable metrics blog post](https://chrisbanes.me/posts/composable-metrics/)
     */
    abstract val reportsDestination: DirectoryProperty

    /**
     * Enable intrinsic remember performance optimization.
     *
     * Intrinsic Remember is an optimization mode which improves the runtime performance of your application by inlining `remember`
     * invocations and replacing `.equals` comparison (for keys) with comparisons of the `$changed` meta parameter when possible. This
     * results in fewer slots being used and fewer comparisons being done at runtime.
     *
     * It is enabled by default.
     *
     * To change the default value, use the following code:
     * ```
     * composeCompiler {
     *     enableIntrinsicRemember.set(false)
     * }
     * ```
     */
    @Deprecated("Use the featureFlags option instead")
    val enableIntrinsicRemember: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * Remove groups around non-skipping composable functions.
     *
     * Removing groups around non-skipping composables is an experimental mode which improves the runtime performance of your application
     * by skipping unnecessary groups around composables which do not skip (and thus do not require a group). This optimization will remove
     * the groups around functions that are not skippable such as explicitly marked as `@NonSkippableComposable` and functions that are
     * implicitly not skippable such inline functions and functions that return a non-Unit value such as remember.
     *
     * This feature is still considered experimental and is thus disabled by default.
     */
    @Deprecated("Use the featureFlags option instead")
    val enableNonSkippingGroupOptimization: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    /**
     * Enable strong skipping mode.
     *
     * Strong Skipping is a mode that improves the runtime performance of your application by skipping unnecessary
     * invocations of composable functions for which the parameters have not changed. In particular, when enabled, Composables with
     * unstable parameters become skippable and lambdas with unstable captures will be memoized.
     *
     * For more information, see this link:
     *  - [AndroidX strong skipping](https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/strong-skipping.md)
     */
    @Deprecated("Use the featureFlags option instead")
    val enableStrongSkippingMode: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * Path to the stability configuration file.
     *
     * For more information, see this link:
     *  - [AndroidX stability configuration file](https://developer.android.com/develop/ui/compose/performance/stability/fix#configuration-file)
     */
    @Deprecated("Use the stabilityConfigurationFiles option instead")
    abstract val stabilityConfigurationFile: RegularFileProperty

    /**
     * List of paths to stability configuration files.
     *
     * For more information, see this link:
     *  - [AndroidX stability configuration file](https://developer.android.com/develop/ui/compose/performance/stability/fix#configuration-file)
     *
     * To configure multiple stability configuration files, use the following code:
     * ```
     * composeCompiler {
     *     stabilityConfigurationFiles.addAll(
     *        project.layout.projectDirectory.file("configuration-file1.conf"),
     *        project.layout.projectDirectory.file("configuration-file2.conf"),
     *     )
     * }
     * ```
     */
    abstract val stabilityConfigurationFiles: ListProperty<RegularFile>

    /**
     * Include composition trace markers in the generated code.
     *
     * When `true`, this flag tells the Compose compiler to inject additional tracing information into the bytecode, which allows showing
     * composable functions in the Android Studio system trace profiler.
     *
     * For more information, see this link:
     *  - [composition tracing blog post](https://medium.com/androiddevelopers/jetpack-compose-composition-tracing-9ec2b3aea535)
     */
    val includeTraceMarkers: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * A set of Kotlin platforms to which the Compose compiler plugin will be applied.
     *
     * By default, all Kotlin platforms are enabled.
     *
     * To enable only one specific Kotlin platform:
     * ```
     * composeCompiler {
     *     targetKotlinPlatforms.set(setOf(KotlinPlatformType.jvm))
     * }
     * ```
     *
     * To disable the Compose plugin for one or more Kotlin platforms:
     * ```
     * composeCompiler {
     *     targetKotlinPlatforms.set(
     *         KotlinPlatformType.values()
     *             .filterNot {
     *                it == KotlinPlatformType.native ||
     *                it == KotlinPlatformType.js
     *             }.asIterable()
     *     )
     * }
     * ```
     */
    val targetKotlinPlatforms: SetProperty<KotlinPlatformType> = objectFactory
        .setProperty(KotlinPlatformType::class.java)
        .convention(KotlinPlatformType.values().asIterable())

    /**
     * A set of feature flags to enable. A feature requires a feature flag when it is in the process of becoming the default
     * behavior of the Compose compiler. Features in this set will eventually be removed and integrated as baseline behavior;
     * after that, disabling them will no longer be supported. See [ComposeFeatureFlag] for the list of features currently recognized by the plugin.
     *
     * @see ComposeFeatureFlag
     */
    @Suppress("DEPRECATION")
    val featureFlags: SetProperty<ComposeFeatureFlag> = objectFactory
        .setProperty(ComposeFeatureFlag::class.java)
        .convention(
            // Add features that used to be added by deprecated options. No other features should be added this way.
            enableIntrinsicRemember.zip(enableStrongSkippingMode) { intrinsicRemember, strongSkippingMode ->
                setOfNotNull(
                    if (!intrinsicRemember) ComposeFeatureFlag.IntrinsicRemember.disabled() else null,
                    if (!strongSkippingMode) ComposeFeatureFlag.StrongSkipping.disabled() else null
                )
            }.zip(enableNonSkippingGroupOptimization) { features, nonSkippingGroupsOptimization ->
                if (nonSkippingGroupsOptimization) features + ComposeFeatureFlag.OptimizeNonSkippingGroups else features
            }
        )
}
