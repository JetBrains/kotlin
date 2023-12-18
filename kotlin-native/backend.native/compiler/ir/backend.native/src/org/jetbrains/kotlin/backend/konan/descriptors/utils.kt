/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.library.metadata.isFromInteropLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module

fun DeclarationDescriptor.deepPrint() {
    this.accept(DeepPrintVisitor(PrintVisitor()), 0)
}

internal val String.synthesizedName get() = Name.identifier(this.synthesizedString)

internal val String.synthesizedString get() = "\$$this"


internal val CallableMemberDescriptor.propertyIfAccessor
    get() = if (this is PropertyAccessorDescriptor)
                this.correspondingProperty
                else this

private fun IrDeclaration.findTopLevelDeclaration(): IrDeclaration = when (val parent = this.parent) {
    is IrDeclaration -> parent.findTopLevelDeclaration()
    else -> this
}

private fun IrDeclaration.propertyIfAccessor(): IrDeclaration =
        (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner ?: this

val ModuleDescriptor.isForwardDeclarationModule: Boolean
    get() = name == FORWARD_DECLARATIONS_MODULE_NAME

fun DeclarationDescriptor.isFromInteropLibrary(): Boolean =
        this.isFromFirDeserializedInteropLibrary() || this.module.isFromInteropLibrary()

private fun DeclarationDescriptor.isFromFirDeserializedInteropLibrary(): Boolean {
    val declaration = (this as? IrBasedDeclarationDescriptor<*>)?.owner ?: return false

    // We need to find top-level non-accessor declaration, because
    //  - fir2ir lazy IR creates non-AbstractFir2IrLazyDeclaration declarations sometimes, e.g. for enum entries;
    //  - K2 metadata deserializer doesn't set containerSource for property accessors.
    val topLevelDeclaration = declaration.findTopLevelDeclaration().propertyIfAccessor()

    val firDeclaration = (topLevelDeclaration as? AbstractFir2IrLazyDeclaration<*>)?.fir as? FirMemberDeclaration ?: return false
    val containerSource = when (firDeclaration) {
        is FirCallableDeclaration -> firDeclaration.containerSource
        is FirClassLikeDeclaration -> firDeclaration.sourceElement
    }

    return containerSource is KlibDeserializedContainerSource && containerSource.isFromNativeInteropLibrary
}
