package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider

class StaticWrapperCreatorFunctionResolveExtension() : SyntheticResolveExtension {

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
        return if (ComponentMetadata.isR4AComponent(thisDescriptor))
            Name.identifier("R4HStaticRenderCompanion")
        else null
    }

    override fun generateSyntheticClasses(
        thisDescriptor: ClassDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        super.generateSyntheticClasses(thisDescriptor, name, ctx, declarationProvider, result)

        if (ComponentMetadata.isR4AComponent(thisDescriptor)) {
            val wrapperViewDescriptor =
                ComponentMetadata.fromDescriptor(thisDescriptor).wrapperViewDescriptor
            if (wrapperViewDescriptor.name == name) result.add(wrapperViewDescriptor)
        }
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        if (!ComponentMetadata.isComponentCompanion(thisDescriptor)) return
        if (name != Name.identifier("createInstance")) return

        val containingClass = thisDescriptor.containingDeclaration as? ClassDescriptor ?: return
        val wrapperView = ComponentMetadata.fromDescriptor(containingClass).wrapperViewDescriptor
        result.add(wrapperView.getInstanceCreatorFunction(thisDescriptor))
    }
}
