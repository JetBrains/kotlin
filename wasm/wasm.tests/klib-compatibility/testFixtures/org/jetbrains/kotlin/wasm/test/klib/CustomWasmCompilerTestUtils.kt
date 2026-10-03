/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilderBase
import org.jetbrains.kotlin.test.services.ReflectionPackageNameAnnotation

/** The stdlib version that introduced `kotlin.internal.ReflectionPackageName`. */
internal val REFLECTION_PACKAGE_NAME_SINCE = LanguageVersion.KOTLIN_2_5

/**
 * Registers [ReflectionPackageNameAnnotation] only when the stdlib the first stage compiles against has the annotation.
 *
 * A KLIB-compatibility test compiles its first stage against the stdlib of the custom compiler version, and the
 * annotation the batching package inserter adds exists in the stdlib only since Kotlin 2.5. Registering it against an
 * older stdlib makes every batched test fail its first stage with an unresolved reference, which the suppressor then
 * mutes into a silent skip.
 *
 * @param firstStageStdlibVersion the language version of the compiler whose stdlib is used on the first stage
 */
fun TestConfigurationBuilderBase<*, *>.useReflectionPackageNameAnnotationIfSupported(firstStageStdlibVersion: LanguageVersion) {
    if (firstStageStdlibVersion >= REFLECTION_PACKAGE_NAME_SINCE) {
        useAdditionalService { ReflectionPackageNameAnnotation }
    }
}
