/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformer
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.jvm.checkers.SuperCallWithDefaultArgumentsChecker
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesSettings

object KonanPlatformConfigurator : PlatformConfiguratorBase(
    DynamicTypesSettings(),
    additionalDeclarationCheckers = listOf(ExpectedActualDeclarationChecker()),
    additionalCallCheckers = listOf(SuperCallWithDefaultArgumentsChecker()),
    additionalTypeCheckers = listOf(),
    additionalClassifierUsageCheckers = listOf(),
    additionalAnnotationCheckers = listOf(),
    additionalClashResolvers = listOf(),
    identifierChecker = IdentifierChecker.Default,
    overloadFilter = OverloadFilter.Default,
    platformToKotlinClassMap = PlatformToKotlinClassMap.EMPTY,
    delegationFilter = DelegationFilter.Default,
    overridesBackwardCompatibilityHelper = OverridesBackwardCompatibilityHelper.Default,
    declarationReturnTypeSanitizer = DeclarationReturnTypeSanitizer.Default
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(SyntheticScopes.Empty)
        container.useInstance(TypeSpecificityComparator.NONE)
        container.useInstance(SamConversionTransformer.Empty)
    }
}
