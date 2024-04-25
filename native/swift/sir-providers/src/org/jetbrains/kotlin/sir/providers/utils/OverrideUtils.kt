/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.sir.*

/**
 * Should [SirInit] node be marked as `override`?
 *
 * For now, the implementation is rather trivial: it just checks that the superclass has an init method with the same list of parameters.
 */
public fun computeIsOverrideForDesignatedInit(parent: SirClass, parameters: List<SirParameter>): Boolean {
    val superClass = parent.superClass?.declaration ?: return false
    val overridesDesignatedInitFromSuper = superClass.declarations
        .filterIsInstance<SirInit>()
        .filter { it.initKind == SirInitializerKind.ORDINARY }
        .any { superInit -> superInit.parameters == parameters }
    return if (overridesDesignatedInitFromSuper) {
        true
    } else {
        computeIsOverrideForDesignatedInit(superClass, parameters)
    }
}