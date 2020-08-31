/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.utils.Printer

class KlibPrinter(out: Appendable) {

    val printer = Printer(out, 1, "    ")

    val DeclarationDescriptorWithVisibility.isPublicOrProtected: Boolean
        get() = visibility == Visibilities.PUBLIC || visibility == Visibilities.PROTECTED

    val CallableMemberDescriptor.isFakeOverride: Boolean
        get() = kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

    val DeclarationDescriptor.shouldBePrinted: Boolean
        get() = this is ClassifierDescriptorWithTypeParameters && isPublicOrProtected
                || this is CallableMemberDescriptor && isPublicOrProtected && !isFakeOverride

    private fun Printer.printBody(header: CharSequence, block: () -> Unit) {
        println()
        println("$header {")
        pushIndent()
        block()
        popIndent()
        println("}")
        println()
    }

    private fun ClassifierDescriptorWithTypeParameters.render(): String {
        val renderer = when (modality) {
            // Don't render 'final' modality
            Modality.FINAL -> Renderers.WITHOUT_MODALITY
            else -> Renderers.DEFAULT
        }
        return renderer.render(this)
    }

    private fun CallableMemberDescriptor.render(): String {
        val containingDeclaration = containingDeclaration
        val renderer = when {
            // Don't render modality for non-override final methods and interface methods.
            containingDeclaration is ClassDescriptor && containingDeclaration.kind == ClassKind.INTERFACE ||
            modality == Modality.FINAL && overriddenDescriptors.isEmpty() ->
                Renderers.WITHOUT_MODALITY
            else -> Renderers.DEFAULT
        }
        return renderer.render(this)
    }

    private fun PropertyAccessorDescriptor.render() = buildString {
        annotations.forEach {
            append(Renderers.DEFAULT.renderAnnotation(it)).append(" ")
        }
        if (visibility != Visibilities.DEFAULT_VISIBILITY) {
            append(visibility.internalDisplayName).append(" ")
        }
        when (this@render) {
            is PropertyGetterDescriptor -> append("get")
            is PropertySetterDescriptor -> append("set")
            else -> throw AssertionError("Unknown accessor descriptor type: ${this@render}")
        }
    }

    fun print(module: ModuleDescriptor) {
        module.accept(PrinterVisitor(), Unit)
    }

    private inner class PrinterVisitor : DeclarationDescriptorVisitorEmptyBodies<Unit, Unit>() {
        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Unit) {
            descriptor.getPackageFragments().forEach {
                it.accept(this, data)
            }
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Unit) {
            val children = descriptor.getMemberScope().getContributedDescriptors().filter { it.shouldBePrinted }
            if (children.isNotEmpty()) {
                val packageName = descriptor.fqName.let { if (it.isRoot) "<root>" else it.asString() }
                val header = "package $packageName"
                printer.printBody(header) {
                    children.forEach { it.accept(this, data) }
                }
            }
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Unit) {
            val children = descriptor.unsubstitutedMemberScope.getContributedDescriptors().filter { it.shouldBePrinted }
            val constructors = descriptor.constructors.filter { !it.isPrimary && it.shouldBePrinted }
            val header = descriptor.render()
            if (children.isNotEmpty() || constructors.isNotEmpty()) {
                printer.printBody(header) {
                    constructors.forEach { it.accept(this, data) }
                    children.forEach { it.accept(this, data) }
                }
            } else {
                printer.println(header)
            }
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit) {
            printer.println(descriptor.render())
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit) {
            printer.println(descriptor.render())
            descriptor.getter?.let { getter ->
                if (!getter.annotations.isEmpty()) {
                    printer.pushIndent()
                    printer.println(getter.render())
                    printer.popIndent()
                }
            }
            descriptor.setter?.let { setter ->
                if (!setter.annotations.isEmpty() || setter.visibility != descriptor.visibility) {
                    printer.pushIndent()
                    printer.println(setter.render())
                    printer.popIndent()
                }
            }
        }

        override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit) {
            printer.println(descriptor.render())
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Unit) {
            printer.println(descriptor.render())
        }
    }

    object Renderers {
        val DEFAULT = DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
            modifiers = DescriptorRendererModifier.ALL
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
            excludedAnnotationClasses += setOf(StandardNames.FqNames.suppress)

            classWithPrimaryConstructor = true
            renderConstructorKeyword = true
            includePropertyConstant = true

            unitReturnType = false
            withDefinedIn = false
            renderDefaultVisibility = false
            secondaryConstructorsAsPrimary = false
        }
        val WITHOUT_MODALITY = DEFAULT.withOptions {
            modifiers -= DescriptorRendererModifier.MODALITY
        }
    }

}
