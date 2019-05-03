/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import androidx.compose.plugins.kotlin.analysis.ComponentMetadata
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider

class StaticWrapperCreatorFunctionResolveExtension() : SyntheticResolveExtension {

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
        return if (ComponentMetadata.isComposeComponent(thisDescriptor))
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

        if (ComponentMetadata.isComposeComponent(thisDescriptor)) {
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
