package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy

object DefaultDeclarationHeaderRenderer : DeclarationHeaderRenderer {
    override fun render(descriptor: DeclarationDescriptor): String = when (descriptor) {
        is PackageFragmentDescriptor -> render(descriptor)
        is ClassifierDescriptorWithTypeParameters -> render(descriptor)
        is PropertyAccessorDescriptor -> render(descriptor)
        is CallableMemberDescriptor -> render(descriptor)
        else -> throw AssertionError("Unknown declaration descriptor type: $descriptor")
    }

    private fun render(descriptor: PackageFragmentDescriptor): String {
        val packageName = descriptor.fqName.let { if (it.isRoot) "<root>" else it.asString() }
        return "package $packageName"
    }

    private fun render(descriptor: ClassifierDescriptorWithTypeParameters): String {
        val renderer = when (descriptor.modality) {
            // Don't render 'final' modality
            Modality.FINAL -> Renderers.WITHOUT_MODALITY
            else -> Renderers.DEFAULT
        }
        return renderer.render(descriptor)
    }

    private fun render(descriptor: CallableMemberDescriptor): String {
        val containingDeclaration = descriptor.containingDeclaration
        val renderer = when {
            // Don't render modality for non-override final methods and interface methods.
            containingDeclaration is ClassDescriptor && containingDeclaration.kind == ClassKind.INTERFACE ||
                    descriptor.modality == Modality.FINAL && descriptor.overriddenDescriptors.isEmpty() -> Renderers.WITHOUT_MODALITY
            else -> Renderers.DEFAULT
        }
        return renderer.render(descriptor)
    }

    private fun render(descriptor: PropertyAccessorDescriptor) = buildString {
        descriptor.annotations.forEach {
            append(Renderers.DEFAULT.renderAnnotation(it)).append(" ")
        }
        if (descriptor.visibility != DescriptorVisibilities.DEFAULT_VISIBILITY) {
            append(descriptor.visibility.internalDisplayName).append(" ")
        }
        when (descriptor) {
            is PropertyGetterDescriptor -> append("get")
            is PropertySetterDescriptor -> append("set")
            else -> throw AssertionError("Unknown accessor descriptor type: $descriptor")
        }
    }

    private object Renderers {
        val DEFAULT = DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
            modifiers = DescriptorRendererModifier.ALL
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
            excludedAnnotationClasses += StandardNames.FqNames.suppress

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
