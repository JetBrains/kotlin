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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ComposeCompilerGradlePluginExtension @Inject constructor(objectFactory: ObjectFactory) {
    /**
     * Generate function key meta classes with annotations indicating the functions and their group keys.
     *
     * Generally used for tooling.
     */
    val generateFunctionKeyMetaClasses: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    /**
     * Include source information in generated code.
     *
     * Records source information that can be used for tooling to determine the source location of the corresponding composable function.
     * By default, this function is declared as having no side-effects. It is safe for code shrinking tools (such as R8 or ProGuard) to
     * remove it. This option does NOT impact the presence of symbols or line information normally added by the Kotlin compiler; this option
     * controls additional source information added by the Compose Compiler.
     */
    val includeSourceInformation: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    /**
     * Save compose build metrics to this folder.
     * When specified, the Compose Compiler will dump metrics about the compilation which can be useful when manually optimizing your application's runtime performance.  These metrics include information about which of your composable functions are skippable, which are restartable, which are readonly, etc.
     *
     * For more information, see these links:
     *  - [AndroidX compiler metrics](https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md)
     *  - [Composable metrics blog post](https://chrisbanes.me/posts/composable-metrics/)
     */
    abstract val metricsDestination: DirectoryProperty

    /**
     * Save compose build reports to this folder.
     *
     * When specified, the Compose Compiler will dump metrics about the compilation which can be useful when manually optimizing
     * your application's runtime performance. These metrics include information about which of your composable functions are skippable,
     * which are restartable, which are readonly, etc.
     *
     * For more information, see these links:
     *  - [AndroidX compiler metrics](https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md)
     *  - [Composable metrics blog post](https://chrisbanes.me/posts/composable-metrics/)
     */
    abstract val reportsDestination: DirectoryProperty

    /**
     * Enable intrinsic remember performance optimization.
     *
     * Intrinsic Remember is an optimization mode which improves the runtime performance of your application by inlining `remember`
     * invocations and replacing `.equals` comparison (for keys) with comparisons of the `$changed` meta parameter when possible. This
     * results in fewer slots being used and fewer comparisons being done at runtime.
     */
    val enableIntrinsicRemember: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

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
    val enableNonSkippingGroupOptimization: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    /**
     * Suppress Kotlin version compatibility check.
     *
     * By default, this compatibility check verifies that the specified version of Compose Compiler is compatible with the specified
     * version of Kotlin.  In most cases, users should only be using versions that are known to be compatible, and so setting this flag
     * should be unnecessary.  However, in rare cases (such as custom compiler builds, or to take a fix in one compiler without upgrading
     * the other compiler), users may wish to suppress the compatibility check in order to use a combination of Compose Compiler and Kotlin
     * Compiler that is not officially supported.
     *
     * As a value, you should provide a Kotlin version which is used with the compose compiler.
     */
    abstract val suppressKotlinVersionCompatibilityCheck: Property<String>

    /**
     * Enable experimental strong skipping mode.
     *
     * Strong Skipping is an experimental mode that improves the runtime performance of your application by skipping unnecessary
     * invocations of composable functions for which the parameters have not changed.  In particular, when enabled, Composables with
     * unstable parameters become skippable and lambdas with unstable captures will be memoized.
     *
     * String skipping is still considered experimental and is thus disabled by default.
     *
     * For more information, see this link:
     *  - [AndroidX strong skipping](https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/strong-skipping.md)
     */
    val enableExperimentalStrongSkippingMode: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

    /**
     * Path to the stability configuration file.
     *
     * For more information, see this link:
     *  - [AndroidX stability configuration file](https://developer.android.com/develop/ui/compose/performance/stability/fix#configuration-file)
     */
    abstract val stabilityConfigurationFile: RegularFileProperty

    /**
     * Include composition trace markers in the generated code.
     *
     * When `true`, this flag tells the compose compiler to inject additional tracing information into the bytecode, which allows showing
     * composable functions in the Android Studio system trace profiler.
     *
     * For more information, see this link:
     *  - [composition tracing blog post](https://medium.com/androiddevelopers/jetpack-compose-composition-tracing-9ec2b3aea535)
     */
    val includeTraceMarkers: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)
}
