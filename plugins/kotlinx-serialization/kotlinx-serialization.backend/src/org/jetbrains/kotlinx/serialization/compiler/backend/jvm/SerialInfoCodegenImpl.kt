/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlinx.serialization.compiler.resolve.KSerializerDescriptorResolver
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

class SerialInfoCodegenImpl(val codegen: ImplementationBodyCodegen, val thisClass: ClassDescriptor, val bindingContext: BindingContext) {
    val thisAsmType = codegen.typeMapper.mapClass(thisClass)

    fun generate() {
        val props = thisClass.unsubstitutedMemberScope.getDescriptorsFiltered().filterIsInstance<PropertyDescriptor>()
        if (props.isEmpty()) return

        generateFieldsAndSetters(props)
        generateConstructor(props)
    }

    private fun generateFieldsAndSetters(props: List<PropertyDescriptor>) {
        props.forEach { prop ->
            val propType = codegen.typeMapper.mapType(prop.type)
            val propFieldName = "_" + prop.name.identifier
            codegen.v.newField(OtherOrigin(codegen.myClass.psiOrParent), Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC,
                               propFieldName, propType.descriptor, null, null)
            val f = SimpleFunctionDescriptorImpl.create(thisClass, Annotations.EMPTY, prop.name, CallableMemberDescriptor.Kind.SYNTHESIZED, thisClass.source)
            f.initialize(null, thisClass.thisAsReceiverParameter, emptyList(), emptyList(), emptyList(), prop.type, Modality.FINAL, DescriptorVisibilities.PUBLIC)
            codegen.generateMethod(f, { _, _ ->
                load(0, thisAsmType)
                getfield(thisAsmType.internalName, propFieldName, propType.descriptor)
                areturn(propType)
            })
        }
    }

    private fun generateConstructor(props: List<PropertyDescriptor>) {
        val constr = ClassConstructorDescriptorImpl.createSynthesized(
                thisClass,
                Annotations.EMPTY,
                false,
                thisClass.source
        )
        val args = mutableListOf<ValueParameterDescriptor>()
        var i = 0
        props.forEach { prop ->
            args.add(ValueParameterDescriptorImpl(constr, null, i++, Annotations.EMPTY, prop.name, prop.type, false, false, false, null, constr.source))
        }
        constr.initialize(
                args,
                DescriptorVisibilities.PUBLIC
        )

        constr.returnType = thisClass.defaultType

        codegen.generateMethod(constr, { _, _ ->
            load(0, thisAsmType)
            invokespecial("java/lang/Object", "<init>", "()V", false)
            var varOffset = 1
            props.forEach { prop ->
                val propType = codegen.typeMapper.mapType(prop.type)
                val propFieldName = "_" + prop.name.identifier
                load(0, thisAsmType)
                load(varOffset, propType)
                putfield(thisAsmType.internalName, propFieldName, propType.descriptor)
                varOffset += propType.size
            }
            areturn(Type.VOID_TYPE)
        })
    }

    companion object {
        fun generateSerialInfoImplBody(codegen: ImplementationBodyCodegen) {
            val thisClass = codegen.descriptor
            if (KSerializerDescriptorResolver.isSerialInfoImpl(thisClass))
                SerialInfoCodegenImpl(codegen, thisClass, codegen.bindingContext).generate()
        }
    }
}

