/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolved

import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.web.npm.ObsoleteNpmDependencyModel
import java.io.Serializable

@ObsoleteNpmDependencyModel
class KotlinProjectNpmResolution(
    val byCompilation: Map<String, KotlinCompilationNpmResolution>,
) : Serializable {
    val npmProjects: Collection<KotlinCompilationNpmResolution>
        get() = byCompilation.values

    operator fun get(compilationName: String): KotlinCompilationNpmResolution {
        return byCompilation.getValue(compilationName)
    }

    companion object {
        fun empty() = KotlinProjectNpmResolution(emptyMap())
    }
}
