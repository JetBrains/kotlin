/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.Printer

class ScriptProvidedPropertiesDescriptor(script: LazyScriptDescriptor) :
    MutableClassDescriptor(
        script,
        ClassKind.CLASS, false, false,
        Name.special("<synthetic script provided properties for ${script.name}>"),
        SourceElement.NO_SOURCE,
        LockBasedStorageManager.NO_LOCKS
    ) {

    init {
        modality = Modality.FINAL
        visibility = Visibilities.PUBLIC
        setTypeParameterDescriptors(emptyList())
        createTypeConstructor()
    }

    private val memberScope: () -> ScriptProvidedPropertiesMemberScope = script.resolveSession.storageManager.createLazyValue {
        ScriptProvidedPropertiesMemberScope(
            script.name.identifier,
            properties()
        )
    }

    override fun getUnsubstitutedMemberScope(): MemberScope = memberScope()

    val properties: () -> List<ScriptProvidedPropertyDescriptor> = script.resolveSession.storageManager.createLazyValue {
        script.scriptDefinition().legacyDefinition.providedProperties.mapNotNull { (name, type) ->
            script.findTypeDescriptor(type, Errors.MISSING_SCRIPT_PROVIDED_PROPERTY_CLASS)?.let {
                name to it
            }
        }.map { (name, classDescriptor) ->
            ScriptProvidedPropertyDescriptor(
                Name.identifier(name),
                classDescriptor,
                thisAsReceiverParameter,
                true,
                script
            )
        }
    }

    private class ScriptProvidedPropertiesMemberScope(
        private val scriptId: String,
        private val providedProperties: List<PropertyDescriptor>
    ) : MemberScopeImpl() {
        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> =
            providedProperties

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> =
            providedProperties.filter { it.name == name }

        override fun printScopeStructure(p: Printer) {
            p.println("Scope of script provided properties: $scriptId")
        }
    }
}