/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.utils.property
import java.io.File
import javax.inject.Inject

open class SeparateDukatTask
@Inject
constructor(
    compilation: KotlinJsCompilation
) : DukatTask(compilation) {
    override val considerGeneratingFlag: Boolean = false

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> by lazy {
        emptySet()
    }

    @get:OutputDirectory
    override var destinationDir: File by property {
        project.projectDir.resolve("externals")
    }
}