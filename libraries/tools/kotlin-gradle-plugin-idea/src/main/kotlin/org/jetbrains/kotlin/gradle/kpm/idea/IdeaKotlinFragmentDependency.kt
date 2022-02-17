/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

interface IdeaKotlinFragmentDependency : Serializable

fun IdeaKotlinFragmentDependency.deepCopy(interner: Interner = Interner.default()): IdeaKotlinFragmentDependency {
    // TODO
    return interner.intern(this)
}
