package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.utils.Printer

class DeclarationPrinter(
        out: Appendable,
        private val headerRenderer: DeclarationHeaderRenderer,
        private val signatureRenderer: IdSignatureRenderer
) {
    private val printer = Printer(out, 1, "    ")

    private val DeclarationDescriptorWithVisibility.isPublicOrProtected: Boolean
        get() = visibility == DescriptorVisibilities.PUBLIC || visibility == DescriptorVisibilities.PROTECTED

    private val CallableMemberDescriptor.isFakeOverride: Boolean
        get() = kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

    private val DeclarationDescriptor.shouldBePrinted: Boolean
        get() = this is ClassifierDescriptorWithTypeParameters && isPublicOrProtected
                || this is CallableMemberDescriptor && isPublicOrProtected && !isFakeOverride

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
        println(if (suffix != null ) header + suffix else header)
    }

    private inner class PrinterVisitor : DeclarationDescriptorVisitorEmptyBodies<Unit, Unit>() {
        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Unit) {
            descriptor.getPackageFragments().forEach { it.accept(this, data) }
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Unit) {
            val children = descriptor.getMemberScope().getContributedDescriptors().filter { it.shouldBePrinted }
            if (children.isNotEmpty()) {
                printer.printWithBody(header = headerRenderer.render(descriptor)) {
                    children.forEach { it.accept(this, data) }
                }
            }
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Unit) {
            val header = headerRenderer.render(descriptor)
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
            printer.printPlain(header = headerRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit) {
            printer.printPlain(header = headerRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
            descriptor.getter?.takeIf { !it.annotations.isEmpty() }?.let { getter ->
                printer.pushIndent()
                printer.printPlain(header = headerRenderer.render(getter), signature = signatureRenderer.render(getter))
                printer.popIndent()
            }
            descriptor.setter?.takeIf { !it.annotations.isEmpty() || it.visibility != descriptor.visibility }?.let { setter ->
                printer.pushIndent()
                printer.printPlain(header = headerRenderer.render(setter), signature = signatureRenderer.render(setter))
                printer.popIndent()
            }
        }

        override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit) {
            printer.printPlain(header = headerRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Unit) {
            printer.printPlain(header = headerRenderer.render(descriptor), signature = signatureRenderer.render(descriptor))
        }
    }
}
