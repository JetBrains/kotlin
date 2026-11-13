/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

/**
 * The triple of the arm64e variant of the target, from `targetTriple.arm64e.<target>` in konan.properties, or null if
 * the target has no arm64e variant. `-Xbinary=arm64e=true` selects the variant.
 */
val Configurables.arm64eTargetTriple: TargetTriple?
    get() = targetString("targetTriple.arm64e")?.let(TargetTriple::fromString)

/**
 * Whether [target] has an arm64e variant, see [arm64eTargetTriple].
 */
fun PlatformManager.hasArm64eVariant(target: KonanTarget): Boolean = platform(target).configurables.arm64eTargetTriple != null
