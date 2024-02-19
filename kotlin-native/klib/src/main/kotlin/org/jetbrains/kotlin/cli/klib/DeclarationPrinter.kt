package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PUBLIC
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.PROTECTED
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.utils.Printer

// TODO: This class is used in dumping metadata by descriptors, "contents" command. Drop it after 2.0. KT-65380
internal class DeclarationPrinter(
        out: Appendable,
        private val signatureRenderer: KlibSignatureRenderer
) {
    private val printer = Printer(out, 1, "    ")

    private val DeclarationDescriptorWithVisibility.isNonPrivate: Boolean
        get() = visibility == PUBLIC || visibility == PROTECTED || visibility == INTERNAL

    private val CallableMemberDescriptor.isFakeOverride: Boolean
        get() = kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

    private val DeclarationDescriptor.shouldBePrinted: Boolean
        get() = this is ClassifierDescriptorWithTypeParameters && isNonPrivate
                || this is CallableMemberDescriptor && isNonPrivate && !isFakeOverride

    fun print(module: ModuleDescriptor) {
        module.accept(PrinterVisitor(), Unit)
    }

    private fun Printer.printWithBody(header: String, signature: String? = null, body: () -> Unit) {
        println()
        printPlain(header, signature, suffix = " {")
        pushIndent()
        body()
        popIndent()
        println("}")
        println()
    }

    private fun Printer.printPlain(header: String, signature: String? = null, suffix: String? = null) {
        if (signature != null) println(signature)
        println(if (suffix != null) header + suffix else header)
    }

    private inner class PrinterVisitor : DeclarationDescriptorVisitorEmptyBodies<Unit, Unit>() {
        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Unit) {
            descriptor.getPackageFragments().forEach { it.accept(this, data) }
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Unit) {
            val children = descriptor.getMemberScope().getContributedDescriptors()
                .filter { it.shouldBePrinted }
                .sortedBy { it.name }
            if (children.isNotEmpty()) {
                printer.printWithBody(header = DefaultDeclarationHeaderRenderer.render(descriptor)) {
                    children.forEach { it.accept(this, data) }
                }
            }
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Unit) {
            val header = DefaultDeclarationHeaderRenderer.render(descriptor)
            val signature = signatureRenderer.render(descriptor)

            val children = descriptor.unsubstitutedMemberScope.getContributedDescriptors().filter { it.shouldBePrinted }
            val constructors = descriptor.constructors.filter { !it.isPrimary && it.shouldBePrinted }
            if (children.isNotEmpty() || constructors.isNotEmpty()) {
                printer.printWithBody(header, signature) {
                    constructors.forEach { it.accept(this, data) }
                    children.forEach { it.accept(this, data) }
                }
            } else {
                printer.printPlain(header, signature)
            }
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit) {
            printer.printPlain(header = DefaultDeclarationHeaderRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit) {
            printer.printPlain(header = DefaultDeclarationHeaderRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
            descriptor.getter?.takeUnless { canSkipAccessor(it, descriptor) }?.let { getter ->
                printer.pushIndent()
                printer.printPlain(header = DefaultDeclarationHeaderRenderer.render(getter), signature = signatureRenderer.render(getter))
                printer.popIndent()
            }
            descriptor.setter?.takeUnless { canSkipAccessor(it, descriptor) }?.let { setter ->
                printer.pushIndent()
                printer.printPlain(header = DefaultDeclarationHeaderRenderer.render(setter), signature = signatureRenderer.render(setter))
                printer.popIndent()
            }
        }

        private fun canSkipAccessor(accessor: PropertyAccessorDescriptor, property: PropertyDescriptor) : Boolean {
            return accessor.annotations.isEmpty() && accessor.visibility == property.visibility && accessor.modality == property.modality
        }

        override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit) {
            printer.printPlain(header = DefaultDeclarationHeaderRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Unit) {
            printer.printPlain(header = DefaultDeclarationHeaderRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
        }
    }
}
