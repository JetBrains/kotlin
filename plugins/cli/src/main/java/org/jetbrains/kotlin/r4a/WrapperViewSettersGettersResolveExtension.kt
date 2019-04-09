package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class WrapperViewSettersGettersResolveExtension : SyntheticResolveExtension {
    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        if (!ComponentMetadata.isWrapperView(thisDescriptor)) return

        result.addAll((thisDescriptor as GeneratedViewClassDescriptor).attributeSetters)
    }
}
