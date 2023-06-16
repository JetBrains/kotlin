/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.containerSource
import org.jetbrains.kotlin.fir.lazy.AbstractFir2IrLazyDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.irProviderName
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
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
    get() {
        // TODO: use KlibResolvedModuleDescriptorsFactoryImpl.FORWARD_DECLARATIONS_MODULE_NAME instead of
        //  manually created Name instance
        return name == Name.special("<forward declarations>")
    }

fun BaseKotlinLibrary.isInteropLibrary() = irProviderName == KLIB_INTEROP_IR_PROVIDER_IDENTIFIER

fun DeclarationDescriptor.isFromInteropLibrary(): Boolean =
        this.isFromFirDeserializedInteropLibrary() || this.module.isFromInteropLibrary()

private fun DeclarationDescriptor.isFromFirDeserializedInteropLibrary(): Boolean {
    val declaration = (this as? IrBasedDeclarationDescriptor<*>)?.owner ?: return false

    // We need to find top-level non-accessor declaration, because
    //  - fir2ir lazy IR creates non-AbstractFir2IrLazyDeclaration declarations sometimes, e.g. for enum entries;
    //  - K2 metadata deserializer doesn't set containerSource for property accessors.
    val topLevelDeclaration = declaration.findTopLevelDeclaration().propertyIfAccessor()

    val firDeclaration = (topLevelDeclaration as? AbstractFir2IrLazyDeclaration<*>)?.fir ?: return false
    val containerSource = (firDeclaration as? FirMemberDeclaration)?.containerSource

    return containerSource is KlibDeserializedContainerSource && containerSource.isFromNativeInteropLibrary
}

fun ModuleDescriptor.isFromInteropLibrary() =
        when (this) {
            is ModuleDescriptorImpl ->
                if (klibModuleOrigin !is DeserializedKlibModuleOrigin) false
                else kotlinLibrary.isInteropLibrary()
            else -> false // cinterop libraries are deserialized by Fir2Ir as ModuleDescriptorImpl, not FirModuleDescriptor
        }