/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.scopes.platformSpecificOverridabilityRules

/**
 * That class is expected to work just the same as FirStandardOverrideChecker for regular members,
 * but when it comes to the platform originated member, it's more correct to use platform specific rules, too
 * Behaviorally it's similar to FirIntersectionScopeOverrideChecker, though that must not be used here outside of intersection scope.
 */
class FirNativeOverrideChecker(session: FirSession) : FirOverrideChecker {
    private val standardOverrideChecker = FirStandardOverrideChecker(session)
    private val platformSpecificOverridabilityRules = session.platformSpecificOverridabilityRules

    override fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        platformSpecificOverridabilityRules?.isOverriddenFunction(overrideCandidate, baseDeclaration)?.let { return it }
        return standardOverrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration)
    }

    override fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean {
        platformSpecificOverridabilityRules?.isOverriddenProperty(overrideCandidate, baseDeclaration)?.let { return it }
        return standardOverrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration)
    }
}
