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

open class ComposeCompilerGradlePluginExtension {
    internal val myPresets = mutableListOf<String>()

    open fun preset(name: String) {
        myPresets.add(name)
    }


    /** Enable Live Literals code generation */
    var liveLiterals: Boolean = false

    /** Enable Live Literals code generation (with per-file enabled flags) */
    var liveLiteralsEnabled: Boolean = false

    /** Generate function key meta classes with annotations indicating the functions and their group keys. Generally used for tooling. */
    var generateFunctionKeyMetaClasses: Boolean = false

    /** Include source information in generated code */
    var sourceInformation: Boolean = false

    /** Save compose build metrics to this folder */
    var metricsDestination: String? = null

    /** Save compose build reports to this folder */
    var reportsDestination: String? = null

    /** Include source information in generated code */
    var intrinsicRemember: Boolean = false

    /** Remove groups around non-skipping composable functions */
    var nonSkippingGroupOptimization: Boolean = false

    /** Suppress Kotlin version compatibility check */
    var suppressKotlinVersionCompatibilityCheck: String? = null

    /** Generate decoy methods in IR transform */
    var generateDecoys: Boolean = false

    /** Enable experimental strong skipping mode */
    var experimentalStrongSkipping: Boolean = false

    /** Path to stability configuration file */
    var stabilityConfigurationPath: String = ""

    /** Include composition trace markers in generate code */
    var traceMarkersEnabled: Boolean = false
}
