/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.native.interop.isExternalObjCClassProperty
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.util.SymbolTable

internal object NativeFir2IrExtensions : Fir2IrExtensions {
    override val irNeedsDeserialization = false
    override val parametersAreAssignable: Boolean get() = false
    override val externalOverridabilityConditions: List<IrExternalOverridabilityCondition> = listOf(IrObjCOverridabilityCondition)
    override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean = false
    override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? = null
    override fun hasBackingField(property: FirProperty, session: FirSession): Boolean {
        return if (property.isExternalObjCClassProperty(session)) {
            property.isLateInit
        } else {
            Fir2IrExtensions.Default.hasBackingField(property, session)
        }
    }

    override fun isTrueStatic(declaration: FirCallableDeclaration, session: FirSession): Boolean = false
    override fun initializeIrBuiltInsAndSymbolTable(irBuiltIns: IrBuiltIns, symbolTable: SymbolTable) {}
    override fun shouldGenerateDelegatedMember(delegateMemberFromBaseType: IrOverridableDeclaration<*>): Boolean = true
}
