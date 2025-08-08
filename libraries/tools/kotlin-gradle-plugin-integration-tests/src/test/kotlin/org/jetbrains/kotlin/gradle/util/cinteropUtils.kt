/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal fun KotlinMultiplatformExtension.setupCInteropForTarget(
    name: String,
    konanTarget: KonanTarget,
    defFile: File = project.file("src/nativeInterop/cinterop/$name"),
    includeDir: File = project.file("include"),
) {
    targets.withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget == konanTarget }
        .configureEach { target ->
            target.compilations
                .getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                .cinterops
                .create(name) { interop ->
                    interop.defFile(defFile)
                    interop.includeDirs(includeDir)
                }
        }
}