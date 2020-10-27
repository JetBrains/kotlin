/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils


open class DeepVisitor<D>(val worker: DeclarationDescriptorVisitor<Boolean, D>) : DeclarationDescriptorVisitor<Boolean, D> {

    open fun visitChildren(descriptors: Collection<DeclarationDescriptor>, data: D): Boolean {
        for (descriptor in descriptors) {
            if (!descriptor.accept(this, data)) return false
        }
        return true
    }

    open fun visitChildren(descriptor: DeclarationDescriptor?, data: D): Boolean {
        if (descriptor == null) return true

        return descriptor.accept(this, data)
    }

    fun applyWorker(descriptor: DeclarationDescriptor, data: D): Boolean {
        return descriptor.accept(worker, data)
    }

    fun processCallable(descriptor: CallableDescriptor, data: D): Boolean {
        return applyWorker(descriptor, data)
               && visitChildren(descriptor.getTypeParameters(), data)
               && visitChildren(descriptor.getExtensionReceiverParameter(), data)
               && visitChildren(descriptor.getValueParameters(), data)
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: D): Boolean? {
        return applyWorker(descriptor, data) && visitChildren(DescriptorUtils.getAllDescriptors(descriptor.getMemberScope()), data)
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: D): Boolean? {
        return applyWorker(descriptor, data) && visitChildren(DescriptorUtils.getAllDescriptors(descriptor.memberScope), data)
    }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: D): Boolean? {
        return processCallable(descriptor, data)
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: D): Boolean? {
        return processCallable(descriptor, data)
               && visitChildren(descriptor.getter, data)
               && visitChildren(descriptor.setter, data)
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: D): Boolean? {
        return processCallable(descriptor, data)
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: D): Boolean? {
        return applyWorker(descriptor, data)
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: D): Boolean? {
        return applyWorker(descriptor, data)
               && visitChildren(descriptor.getThisAsReceiverParameter(), data)
               && visitChildren(descriptor.getConstructors(), data)
               && visitChildren(descriptor.getTypeConstructor().getParameters(), data)
               && visitChildren(DescriptorUtils.getAllDescriptors(descriptor.getDefaultType().memberScope), data)
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: D): Boolean? {
        return applyWorker(descriptor, data) && visitChildren(descriptor.getDeclaredTypeParameters(), data)
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: D): Boolean? {
        return applyWorker(descriptor, data) && visitChildren(descriptor.getPackage(FqName.ROOT), data)
    }

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: D): Boolean? {
        return visitFunctionDescriptor(constructorDescriptor, data)
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: D): Boolean? {
        return visitClassDescriptor(scriptDescriptor, data)
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: D): Boolean? {
        return visitVariableDescriptor(descriptor, data)
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: D): Boolean? {
        return visitFunctionDescriptor(descriptor, data)
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: D): Boolean? {
        return visitFunctionDescriptor(descriptor, data)
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: D): Boolean? {
        return applyWorker(descriptor, data)
    }
}

open public class EmptyDescriptorVisitorVoid: DeclarationDescriptorVisitor<Boolean, Unit> {
    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Unit) = true
    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Unit) = true
    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Unit) = true
    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit) = true
    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Unit) = true
    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Unit) = true
    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Unit) = true
    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Unit) = true
    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit) = true
    override fun visitScriptDescriptor(descriptor: ScriptDescriptor, data: Unit) = true
    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit) = true
    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Unit) = true
    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Unit) = true
    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Unit) = true
    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Unit) = true
}

