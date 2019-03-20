/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.shared.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class LazyScriptClassMemberScope(
    resolveSession: ResolveSession,
    declarationProvider: ClassMemberDeclarationProvider,
    private val scriptDescriptor: LazyScriptDescriptor,
    trace: BindingTrace
) : LazyClassMemberScope(resolveSession, declarationProvider, scriptDescriptor, trace) {

    private val scriptPrimaryConstructor: () -> ClassConstructorDescriptorImpl? = resolveSession.storageManager.createNullableLazyValue {
        val baseClass = scriptDescriptor.baseClassDescriptor()
        val baseConstructorDescriptor = baseClass?.unsubstitutedPrimaryConstructor
        if (baseConstructorDescriptor != null) {
            val implicitReceiversParamTypes =
                scriptDescriptor.implicitReceivers.mapIndexed { idx, receiver ->
                    val name =
                        if (receiver is ScriptDescriptor) "$IMPORTED_SCRIPT_PARAM_NAME_PREFIX${receiver.name}"
                        else "$IMPLICIT_RECEIVER_PARAM_NAME_PREFIX$idx"
                    name to receiver.defaultType
                }
            val providedPropertiesParamTypes =
                scriptDescriptor.scriptProvidedProperties.map {
                    it.name.identifier to it.type
                }
            val annotations = baseConstructorDescriptor.annotations
            val constructorDescriptor = ClassConstructorDescriptorImpl.create(
                scriptDescriptor, annotations, baseConstructorDescriptor.isPrimary, scriptDescriptor.source
            )
            var paramsIndexBase = baseConstructorDescriptor.valueParameters.lastIndex + 1
            val syntheticParameters =
                (implicitReceiversParamTypes + providedPropertiesParamTypes).map { param: Pair<String, KotlinType> ->
                    ValueParameterDescriptorImpl(
                        constructorDescriptor,
                        null,
                        paramsIndexBase++,
                        Annotations.EMPTY,
                        Name.identifier(param.first),
                        param.second,
                        false, false, false, null, SourceElement.NO_SOURCE
                    )
                }
            val parameters = baseConstructorDescriptor.valueParameters.map { it.copy(constructorDescriptor, it.name, it.index) } +
                    syntheticParameters
            constructorDescriptor.initialize(parameters, baseConstructorDescriptor.visibility)
            constructorDescriptor.returnType = scriptDescriptor.defaultType
            constructorDescriptor
        } else {
            null
        }
    }

    override fun resolvePrimaryConstructor(): ClassConstructorDescriptor? {
        val constructor = scriptPrimaryConstructor()
                ?: ClassConstructorDescriptorImpl.create(
                    scriptDescriptor,
                    Annotations.EMPTY,
                    true,
                    SourceElement.NO_SOURCE
                ).initialize(
                    emptyList(),
                    Visibilities.PUBLIC
                )
        setDeferredReturnType(constructor)
        return constructor
    }

    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        super.getNonDeclaredProperties(name, result)
        if (scriptDescriptor.resultFieldName() == name.asString()) {
            scriptDescriptor.resultValue?.let {
                result.add(it)
            }
        }
    }

    override fun createPropertiesFromPrimaryConstructorParameters(name: Name, result: MutableSet<PropertyDescriptor>) {
    }

    companion object {
        const val IMPLICIT_RECEIVER_PARAM_NAME_PREFIX = "\$\$implicitReceiver"
        const val IMPORTED_SCRIPT_PARAM_NAME_PREFIX = "\$\$importedScript"
    }
}

private fun ClassDescriptor.substitute(vararg types: KotlinType): KotlinType? =
    KotlinTypeFactory.simpleType(this.defaultType, arguments = types.map { it.asTypeProjection() })
