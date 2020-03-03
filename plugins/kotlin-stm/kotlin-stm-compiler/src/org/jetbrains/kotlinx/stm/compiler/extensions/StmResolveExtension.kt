/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm.compiler.extensions

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.Name.identifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.stm.compiler.*
import org.jetbrains.kotlinx.stm.compiler.SHARABLE_NAME_SUFFIX
import org.jetbrains.kotlinx.stm.compiler.STM_CONTEXT
import org.jetbrains.kotlinx.stm.compiler.STM_PACKAGE


open class StmResolveExtension : SyntheticResolveExtension {

    companion object {
        internal fun shareFqName(name: FqName): Name =
            identifier("$name${SHARABLE_NAME_SUFFIX}")

        internal fun sharedName(name: Name): Name =
            identifier("$name${SHARABLE_NAME_SUFFIX}")

        internal fun immutableName(name: Name): Name =
            identifier(name.asString().removeSuffix(SHARABLE_NAME_SUFFIX))

        internal fun nestedDelegateName(thisDescriptor: ClassDescriptor): Name =
            shareFqName(thisDescriptor.fqNameSafe)

        internal fun getterName(name: Name) =
            identifier("$GET_PREFIX$name$SHARABLE_NAME_SUFFIX")

        internal fun setterName(name: Name) =
            identifier("$SET_PREFIX$name$SHARABLE_NAME_SUFFIX")

        internal fun undoGetterName(name: Name) =
            identifier(name.asString().removePrefix(GET_PREFIX).removeSuffix(SHARABLE_NAME_SUFFIX))

        internal fun undoSetterName(name: Name) =
            identifier(name.asString().removePrefix(SET_PREFIX).removeSuffix(SHARABLE_NAME_SUFFIX))
    }

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        val res = thisDescriptor.unsubstitutedMemberScope.getVariableNames()
            .filter { thisDescriptor.unsubstitutedMemberScope.getContributedVariables(it, NoLookupLocation.FROM_BACKEND).isNotEmpty() }
            .map { listOf(getterName(it), setterName(it)) }
            .flatten()
        return res
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        if (!name.asString().startsWith(GET_PREFIX) && !name.asString().startsWith(SET_PREFIX))
            return
        val varName = undoSetterName(undoGetterName(name))

        val property = thisDescriptor.unsubstitutedMemberScope.getContributedVariables(
            varName,
            NoLookupLocation.FROM_BACKEND
        ).first()

        val contextClass = thisDescriptor.module.findClassAcrossModuleDependencies(
            ClassId(
                STM_PACKAGE,
                STM_CONTEXT
            )
        ) ?: throw StmResolveException("Couldn't find $STM_CONTEXT runtime class in dependencies of module ${thisDescriptor.module.name}")

        fun createAccessorDescriptor(accessorName: Name) = SimpleFunctionDescriptorImpl.create(
            thisDescriptor,
            Annotations.EMPTY,
            accessorName,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            thisDescriptor.source
        )

        fun createValueParam(containingAccessor: FunctionDescriptor, type: KotlinType, name: String, index: Int) =
            ValueParameterDescriptorImpl(
                containingAccessor,
                original = null,
                index = index,
                annotations = Annotations.EMPTY,
                name = identifier(name),
                outType = type,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                source = thisDescriptor.source
            )

        fun createContextValueParam(containingAccessor: FunctionDescriptor, index: Int) = createValueParam(
            containingAccessor,
            contextClass.defaultType,
            name = "ctx",
            index = index
        )

        if (name.asString().startsWith(GET_PREFIX)) {
            val newGetter = createAccessorDescriptor(getterName(varName))

            newGetter.initialize(
                null,
                thisDescriptor.thisAsReceiverParameter,
                listOf(),
                listOf(createContextValueParam(newGetter, index = 0)),
                property.type,
                Modality.FINAL,
                property.visibility
            )

            result += newGetter
        } else if (name.asString().startsWith(SET_PREFIX)) {
            val newSetter = createAccessorDescriptor(setterName(varName))

            newSetter.initialize(
                null,
                thisDescriptor.thisAsReceiverParameter,
                listOf(),
                listOf(
                    createContextValueParam(newSetter, index = 0),
                    createValueParam(newSetter, property.type, name = "newValue", index = 1)
                ),
                DefaultBuiltIns.Instance.unitType,
                Modality.FINAL,
                property.visibility
            )

            result += newSetter
        }


    }
}
//
//@Shared
//class С {
//    public val x = 10
//
//    private val x_delegate: Delegate<Int> = stm.wrap(10)
//
//    fun get_X_BLA(ctx) = x_delegate.get()
//    fun set_X_BLA(ctx, value) = x_delegate.set(value)
//}
// @S fun f() { print(C().get_X_BLA(ctx)) }

class StmResolveException(s: String) : Exception() {
    override val message = s
}