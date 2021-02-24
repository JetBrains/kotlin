/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope

class LazyScriptClassMemberScope(
    resolveSession: ResolveSession,
    declarationProvider: ClassMemberDeclarationProvider,
    private val scriptDescriptor: LazyScriptDescriptor,
    trace: BindingTrace
) : LazyClassMemberScope(resolveSession, declarationProvider, scriptDescriptor, trace) {

    private val _variableNames: MutableSet<Name>
            by lazy(LazyThreadSafetyMode.PUBLICATION) {
                super.getVariableNames().apply {
                    scriptDescriptor.scriptProvidedProperties.forEach {
                        add(it.name)
                    }
                    scriptDescriptor.resultFieldName()?.let {
                        add(it)
                    }
                }
            }

    override fun resolvePrimaryConstructor(): ClassConstructorDescriptor {
        val constructor = scriptDescriptor.scriptPrimaryConstructorWithParams().constructor
        setDeferredReturnType(constructor)
        return constructor
    }

    override fun getVariableNames() = _variableNames

    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        super.getNonDeclaredProperties(name, result)
        if (scriptDescriptor.resultFieldName() == name) {
            scriptDescriptor.resultValue?.let {
                result.add(it)
            }
        }
        scriptDescriptor.scriptProvidedProperties.forEach { if (it.name == name) result.add(it) }
    }

    override fun createPropertiesFromPrimaryConstructorParameters(name: Name, result: MutableSet<PropertyDescriptor>) {
    }

    companion object {
        const val IMPLICIT_RECEIVER_PARAM_NAME_PREFIX = "\$\$implicitReceiver"
        const val IMPORTED_SCRIPT_PARAM_NAME_PREFIX = "\$\$importedScript"
    }
}

