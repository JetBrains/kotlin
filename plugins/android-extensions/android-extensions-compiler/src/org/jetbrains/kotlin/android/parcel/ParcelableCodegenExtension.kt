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

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.android.parcel.ParcelableSyntheticComponent.ComponentKind.*
import org.jetbrains.kotlin.android.parcel.ParcelableResolveExtension.Companion.createMethod
import org.jetbrains.kotlin.android.parcel.serializers.ParcelSerializer
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy.CodegenBased
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.writeSyntheticClassMetadata
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import java.io.FileDescriptor

class ParcelableCodegenExtension : ExpressionCodegenExtension {
    private companion object {
        private val FILE_DESCRIPTOR_FQNAME = FqName(FileDescriptor::class.java.canonicalName)
    }

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val parcelableClass = codegen.descriptor
        if (!parcelableClass.isMagicParcelable) return
        assert(parcelableClass.kind == ClassKind.CLASS || parcelableClass.kind == ClassKind.OBJECT)

        val propertiesToSerialize = getPropertiesToSerialize(codegen, parcelableClass)

        val parcelClassType = ParcelableResolveExtension.resolveParcelClassType(parcelableClass.module)
        val parcelAsmType = codegen.typeMapper.mapType(parcelClassType)

        with (parcelableClass) {
            writeDescribeContentsFunction(codegen, propertiesToSerialize)
            writeWriteToParcel(codegen, propertiesToSerialize, parcelAsmType)
        }

        writeCreatorAccessField(codegen, parcelableClass)
        writeCreatorClass(codegen, parcelableClass, parcelClassType, parcelAsmType, propertiesToSerialize)
    }

    private fun ClassDescriptor.writeWriteToParcel(
            codegen: ImplementationBodyCodegen,
            properties: List<Pair<String, KotlinType>>,
            parcelAsmType: Type
    ): Unit? {
        val containerAsmType = codegen.typeMapper.mapType(this.defaultType)

        return findFunction(WRITE_TO_PARCEL)?.write(codegen) {
            for ((fieldName, type) in properties) {
                val asmType = codegen.typeMapper.mapType(type)

                v.load(1, parcelAsmType)
                v.load(0, containerAsmType)
                v.getfield(containerAsmType.internalName, fieldName, asmType.descriptor)

                val serializer = ParcelSerializer.get(type, asmType, codegen.typeMapper)
                serializer.writeValue(v)
            }

            v.areturn(Type.VOID_TYPE)
        }
    }

    private fun ClassDescriptor.writeDescribeContentsFunction(
            codegen: ImplementationBodyCodegen,
            propertiesToSerialize: List<Pair<String, KotlinType>>
    ): Unit? {
        val hasFileDescriptorAnywhere = propertiesToSerialize.any { it.second.containsFileDescriptor() }

        return findFunction(DESCRIBE_CONTENTS)?.write(codegen) {
            v.aconst(if (hasFileDescriptorAnywhere) 1 /* CONTENTS_FILE_DESCRIPTOR */ else 0)
            v.areturn(Type.INT_TYPE)
        }
    }

    private fun KotlinType.containsFileDescriptor(): Boolean {
        val declarationDescriptor = this.constructor.declarationDescriptor
        if (declarationDescriptor != null) {
            if (declarationDescriptor.fqNameSafe == FILE_DESCRIPTOR_FQNAME) {
                return true
            }
        }

        return this.arguments.any { it.type.containsFileDescriptor() }
    }

    private fun getPropertiesToSerialize(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor
    ): List<Pair<String, KotlinType>> {
        val constructor = parcelableClass.constructors.first { it.isPrimary }

        val propertiesToSerialize = constructor.valueParameters.map { param ->
            codegen.bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, param]
            ?: error("Value parameter should have 'val' or 'var' keyword")
        }

        return propertiesToSerialize.map { it.name.asString() /* TODO */ to it.type }
    }

    private fun writeCreateFromParcel(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor,
            creatorClass: ClassDescriptorImpl,
            parcelClassType: KotlinType,
            parcelAsmType: Type,
            properties: List<Pair<String, KotlinType>>
    ) {
        val containerAsmType = codegen.typeMapper.mapType(parcelableClass)

        createMethod(creatorClass, CREATE_FROM_PARCEL, parcelableClass.defaultType, "in" to parcelClassType).write(codegen) {
            v.anew(containerAsmType)
            v.dup()

            val asmConstructorParameters = StringBuilder()

            for ((_, type) in properties) {
                val asmType = codegen.typeMapper.mapType(type)
                asmConstructorParameters.append(asmType.descriptor)

                val serializer = ParcelSerializer.get(type, asmType, codegen.typeMapper)
                v.load(1, parcelAsmType)
                serializer.readValue(v)
            }

            v.invokespecial(containerAsmType.internalName, "<init>", "($asmConstructorParameters)V", false)
            v.areturn(containerAsmType)
        }
    }

    private fun writeCreatorAccessField(codegen: ImplementationBodyCodegen, parcelableClass: ClassDescriptor) {
        val parcelableAsmType = codegen.typeMapper.mapType(parcelableClass.defaultType)
        val creatorAsmType = Type.getObjectType(parcelableAsmType.internalName + "\$CREATOR")
        codegen.v.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_STATIC or ACC_PUBLIC or ACC_FINAL, "CREATOR",
                           creatorAsmType.descriptor, null, null)
    }

    private fun writeCreatorClass(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor,
            parcelClassType: KotlinType,
            parcelAsmType: Type,
            properties: List<Pair<String, KotlinType>>
    ) {
        val containerAsmType = codegen.typeMapper.mapType(parcelableClass.defaultType)
        val creatorAsmType = Type.getObjectType(containerAsmType.internalName + "\$CREATOR")

        val creatorClass = ClassDescriptorImpl(
                parcelableClass.containingDeclaration, Name.identifier("Creator"), Modality.FINAL, ClassKind.CLASS, emptyList(),
                parcelableClass.source, false)

        creatorClass.initialize(
                MemberScope.Empty, emptySet(),
                DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source))

        val classBuilderForCreator = codegen.state.factory.newVisitor(
                JvmDeclarationOrigin.NO_ORIGIN,
                Type.getObjectType(creatorAsmType.internalName),
                codegen.myClass.containingKtFile)

        val classContextForCreator = ClassContext(
                codegen.typeMapper, creatorClass, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
        val codegenForCreator = ImplementationBodyCodegen(
                codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

        classBuilderForCreator.defineClass(null, V1_6, ACC_PUBLIC or ACC_STATIC,
                              creatorAsmType.internalName, null, "java/lang/Object",
                              arrayOf("android/os/Parcelable\$Creator"))

        writeSyntheticClassMetadata(classBuilderForCreator, codegen.state)

        writeCreatorConstructor(codegenForCreator, creatorClass, creatorAsmType)
        writeNewArrayMethod(codegenForCreator, parcelableClass, creatorClass)
        writeCreateFromParcel(codegenForCreator, parcelableClass, creatorClass, parcelClassType, parcelAsmType, properties)

        classBuilderForCreator.done()
    }

    private fun writeCreatorConstructor(codegen: ImplementationBodyCodegen, creatorClass: ClassDescriptor, creatorAsmType: Type) {
        DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source)
                .apply {
                    returnType = creatorClass.defaultType
                }.write(codegen) {
                    v.load(0, creatorAsmType)
                    v.invokespecial("java/lang/Object", "<init>", "()V", false)
                    v.areturn(Type.VOID_TYPE)
                }
    }

    private fun writeNewArrayMethod(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor,
            creatorClass: ClassDescriptorImpl
    ) {
        val builtIns = parcelableClass.builtIns
        val parcelableAsmType = codegen.typeMapper.mapType(parcelableClass)

        createMethod(creatorClass, NEW_ARRAY,
                builtIns.getArrayType(Variance.INVARIANT, parcelableClass.defaultType),
                "size" to builtIns.intType
        ).write(codegen) {
            v.load(1, Type.INT_TYPE)
            v.newarray(parcelableAsmType)
            v.areturn(Type.getType("[L$parcelableAsmType;"))
        }
    }

    private fun FunctionDescriptor.write(codegen: ImplementationBodyCodegen, code: ExpressionCodegen.() -> Unit) {
        codegen.functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, this, object : CodegenBased(codegen.state) {
            override fun doGenerateBody(e: ExpressionCodegen, signature: JvmMethodSignature) {
                e.code()
            }
        })
    }

    private fun ClassDescriptor.findFunction(componentKind: ParcelableSyntheticComponent.ComponentKind): SimpleFunctionDescriptor? {
        return unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier(componentKind.methodName), WHEN_GET_ALL_DESCRIPTORS)
                .firstOrNull { (it as? ParcelableSyntheticComponent)?.componentKind == componentKind }
    }
}