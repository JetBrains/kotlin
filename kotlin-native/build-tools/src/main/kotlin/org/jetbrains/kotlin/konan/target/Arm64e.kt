/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

/**
 * The triple of the arm64e variant of the target, if konan.properties describe one with `targetTriple.arm64e.<target>`.
 * `-Xbinary=arm64e=true` selects the variant, and the compiler reads the same properties.
 */
val Configurables.arm64eTargetTriple: TargetTriple?
    get() = targetString("targetTriple.arm64e")?.let(TargetTriple::fromString)

// Copy of Distribution.hasArm64eVariant: the build tools compile against the bootstrap kotlin-native-utils, which doesn't have it yet.
/**
 * Whether [target] has an arm64e variant, see [arm64eTargetTriple].
 */
fun PlatformManager.hasArm64eVariant(target: KonanTarget): Boolean = platform(target).configurables.arm64eTargetTriple != null
