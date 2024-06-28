/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.native.interop.indexer.CompilationWithPCH

/**
 * Describes the native library and the options for adjusting the Kotlin API to be generated for this library.
 */
class InteropConfiguration(
        val library: CompilationWithPCH,
        val pkgName: String,
        val excludedFunctions: Set<String>,
        val excludedMacros: Set<String>,
        val strictEnums: Set<String>,
        val nonStrictEnums: Set<String>,
        val noStringConversion: Set<String>,
        val exportForwardDeclarations: List<String>,
        val allowedOverloadsForCFunctions: Set<String>,
        val disableDesignatedInitializerChecks: Boolean,
        val disableExperimentalAnnotation: Boolean,
        val target: KonanTarget
)

enum class KotlinPlatform {
    JVM,
    NATIVE
}