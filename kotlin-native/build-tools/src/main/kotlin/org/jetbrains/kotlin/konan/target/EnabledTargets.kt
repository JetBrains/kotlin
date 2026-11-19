/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

// Targets the in-tree compiler can no longer compile for (removed from KonanTarget, or kept there only to report
// a deprecation error), but the bootstrap still knows. Not even klibs can be compiled for them, so they are excluded
// from the stdlib's native targets manifest, from platform libraries and from the def files dependency update.
// Matched by name so this keeps compiling after the bootstrap drops the KonanTarget constants.
// todo: KT-78078, KT-91457
val removedTargetNames = setOf("watchos_arm32", "macos_x64", "tvos_x64", "watchos_x64")

fun enabledTargets(platformManager: PlatformManager) = platformManager.enabled.filterNot {
    it.name in removedTargetNames
            || (it in KonanTarget.deprecatedTargets && it !in KonanTarget.toleratedDeprecatedTargets)
}
