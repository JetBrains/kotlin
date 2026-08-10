/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

// Targets already removed from the in-tree compiler but still present in the bootstrap.
// Matched by name so this keeps compiling after the bootstrap drops the KonanTarget constants.
// todo: KT-78078
private val removedTargetNames = setOf("watchos_arm32")

fun enabledTargets(platformManager: PlatformManager) = platformManager.enabled.filterNot {
    it.name in removedTargetNames
            || (it in KonanTarget.deprecatedTargets && it !in KonanTarget.toleratedDeprecatedTargets)
}
