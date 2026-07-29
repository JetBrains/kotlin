/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

import org.jetbrains.kotlin.test.services.ReflectionPackageNameAnnotation

/**
 * `kotlin.native.internal.ReflectionPackageName`, which the Native backend takes into account when generating TypeInfo.
 *
 * Test runners that group several tests into one compilation are expected either to register it in `TestServices`,
 * so that `BatchingPackageInserter` compensates the package renaming in the reflective information,
 * or to pass it to `BatchingPackageInserter.PackageNamePatcher` directly.
 */
val nativeReflectionPackageNameAnnotation: ReflectionPackageNameAnnotation =
    ReflectionPackageNameAnnotation(
        fqName = "kotlin.native.internal.ReflectionPackageName",
        requiredOptInMarkers = listOf("kotlin.native.internal.InternalForKotlinNativeTests"),
    )
