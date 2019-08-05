/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

// N.B. TargetPlatform/SimplePlatform are non exhaustive enough to address both target platforms such as
// JVM, JS and concrete Kotlin/Native targets, e.g. macos_x64, ios_x64, linux_x64.
sealed class TargetId

data class ConcreteTargetId(val name: String) : TargetId()

data class CommonTargetId(val targets: Set<TargetId>) : TargetId() {
    init {
        require(targets.isNotEmpty())
    }
}
