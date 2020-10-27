/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.util.nTabs
import org.jetbrains.kotlin.descriptors.*

class DeepPrintVisitor(worker: DeclarationDescriptorVisitor<Boolean, Int>): DeepVisitor<Int>(worker) {

    override fun visitChildren(descriptor: DeclarationDescriptor?, data: Int): Boolean {
        return super.visitChildren(descriptor, data+1)
    }

    override fun visitChildren(descriptors: Collection<DeclarationDescriptor>, data: Int): Boolean {
        return super.visitChildren(descriptors, data+1)
    }
}

class PrintVisitor: DeclarationDescriptorVisitor<Boolean, Int> {

    fun printDescriptor(descriptor: DeclarationDescriptor, amount: Int): Boolean {
        println("${nTabs(amount)} ${descriptor.toString()}")
        return true;
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitScriptDescriptor(descriptor: ScriptDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Int): Boolean
        = printDescriptor(descriptor, data)
}


