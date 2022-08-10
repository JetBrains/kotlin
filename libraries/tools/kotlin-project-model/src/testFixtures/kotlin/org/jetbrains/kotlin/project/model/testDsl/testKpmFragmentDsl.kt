/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.project.model.testDsl

import org.jetbrains.kotlin.project.model.KpmModuleDependency
import org.jetbrains.kotlin.project.model.infra.TestKpmFragment
import org.jetbrains.kotlin.project.model.infra.TestKpmModule

fun TestKpmFragment.depends(module: TestKpmModule): TestKpmFragment {
    declaredModuleDependencies += KpmModuleDependency(module.moduleIdentifier)
    return this
}

fun TestKpmFragment.refines(fragment: TestKpmFragment): TestKpmFragment {
    declaredRefinesDependencies += fragment
    return this
}

fun TestKpmFragment.fragment(name: String, applyDefaults: Boolean = true, configure: TestKpmFragment.() -> Unit = { }): TestKpmFragment {
    return containingModule.fragment(name, applyDefaults, configure).also { subFragment -> subFragment.refines(this) }
}
