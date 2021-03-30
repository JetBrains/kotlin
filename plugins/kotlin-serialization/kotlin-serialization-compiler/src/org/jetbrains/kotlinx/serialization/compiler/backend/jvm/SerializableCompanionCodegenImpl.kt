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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIALIZER_LAZY_DELEGATE_FIELD_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.getKSerializer
import org.jetbrains.kotlinx.serialization.compiler.resolve.getSerializableClassDescriptorByCompanion
import org.jetbrains.kotlinx.serialization.compiler.resolve.shouldHaveGeneratedMethodsInCompanion
import org.jetbrains.kotlinx.serialization.compiler.resolve.toSimpleType
import org.jetbrains.org.objectweb.asm.Opcodes

class SerializableCompanionCodegenImpl(private val classCodegen: ImplementationBodyCodegen) :
    SerializableCompanionCodegen(classCodegen.descriptor, classCodegen.bindingContext) {

    companion object {
        fun generateSerializableExtensions(codegen: ImplementationBodyCodegen) {
            val serializableClass = getSerializableClassDescriptorByCompanion(codegen.descriptor) ?: return
            if (serializableClass.shouldHaveGeneratedMethodsInCompanion)
                SerializableCompanionCodegenImpl(codegen).generate()
        }
    }

    override fun generateLazySerializerGetter(methodDescriptor: FunctionDescriptor) {
        // Create field for lazy delegate
        classCodegen.v.newField(
            OtherOrigin(classCodegen.myClass.psiOrParent),
            Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_STATIC,
            SERIALIZER_LAZY_DELEGATE_FIELD_NAME,
            kotlinLazyType.descriptor,
            "L${kotlinLazyType.internalName}<L${kSerializerType.internalName}<*>;>;",
            null
        )

        // create singleton lambda class
        val lambdaType =
            createSingletonLambda("serializer\$1", classCodegen, companionDescriptor.getKSerializer().defaultType) { lambdaCodegen ->
                val serializerDescriptor = requireNotNull(
                    findTypeSerializer(
                        serializableDescriptor.module,
                        serializableDescriptor.toSimpleType()
                    )
                )
                stackValueSerializerInstance(
                    lambdaCodegen,
                    serializableDescriptor.module,
                    serializableDescriptor.defaultType,
                    serializerDescriptor,
                    this,
                    null
                )
                areturn(kSerializerType)
            }

        // initialize lazy delegate
        val clInit = classCodegen.createOrGetClInitCodegen()
        with(clInit.v) {
            getstatic(threadSafeModeType.internalName, "PUBLICATION", threadSafeModeType.descriptor)
            getstatic(lambdaType.internalName, JvmAbi.INSTANCE_FIELD, lambdaType.descriptor)
            checkcast(function0Type)
            invokestatic(
                "kotlin/LazyKt",
                "lazy",
                "(${threadSafeModeType.descriptor}${function0Type.descriptor})${kotlinLazyType.descriptor}",
                false
            )
            putstatic(classCodegen.className, SERIALIZER_LAZY_DELEGATE_FIELD_NAME, kotlinLazyType.descriptor)
        }

        // create serializer getter
        classCodegen.generateMethod(methodDescriptor) { _, _ ->
            getstatic(classCodegen.className, SERIALIZER_LAZY_DELEGATE_FIELD_NAME, kotlinLazyType.descriptor)
            invokeinterface(kotlinLazyType.internalName, getLazyValueName, "()Ljava/lang/Object;")
            checkcast(kSerializerType)
            areturn(kSerializerType)
        }
    }

    override fun generateSerializerGetter(methodDescriptor: FunctionDescriptor) {
        val serial = requireNotNull(
            findTypeSerializer(
                serializableDescriptor.module,
                serializableDescriptor.toSimpleType()
            )
        )
        classCodegen.generateMethod(methodDescriptor) { _, _ ->
            stackValueSerializerInstance(
                classCodegen,
                serializableDescriptor.module,
                serializableDescriptor.defaultType,
                serial,
                this,
                null
            ) { it, _ ->
                load(it + 1, kSerializerType)
            }
            areturn(kSerializerType)
        }
    }
}
