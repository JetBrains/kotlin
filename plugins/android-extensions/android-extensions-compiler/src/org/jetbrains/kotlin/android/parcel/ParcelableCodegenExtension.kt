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
import org.jetbrains.kotlin.android.parcel.serializers.PARCEL_TYPE
import org.jetbrains.kotlin.android.parcel.serializers.ParcelSerializer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
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
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import java.io.FileDescriptor

open class ParcelableCodegenExtension : ExpressionCodegenExtension {
    private companion object {
        private val FILE_DESCRIPTOR_FQNAME = FqName(FileDescriptor::class.java.canonicalName)
    }

    protected open fun isExperimental(element: KtElement) = true

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val parcelableClass = codegen.descriptor
        if (!parcelableClass.isParcelize) return

        val sourceElement = (codegen.myClass as? KtClassOrObject) ?: return
        if (!isExperimental(sourceElement)) return

        assert(parcelableClass.kind == ClassKind.CLASS || parcelableClass.kind == ClassKind.OBJECT)

        val propertiesToSerialize = getPropertiesToSerialize(codegen, parcelableClass)

        val parcelClassType = ParcelableResolveExtension.resolveParcelClassType(parcelableClass.module)
        val parcelAsmType = codegen.typeMapper.mapType(parcelClassType)

        val parcelerObject = parcelableClass.companionObjectDescriptor?.takeIf {
            TypeUtils.getAllSupertypes(it.defaultType).any { it.isParceler }
        }

        with (parcelableClass) {
            writeDescribeContentsFunction(codegen, propertiesToSerialize)
            writeWriteToParcel(codegen, propertiesToSerialize, parcelAsmType, parcelerObject)
        }

        writeCreatorAccessField(codegen, parcelableClass)
        writeCreatorClass(codegen, parcelableClass, parcelClassType, parcelAsmType, parcelerObject, propertiesToSerialize)
    }

    private fun getCompanionClassType(containerAsmType: Type, parcelerObject: ClassDescriptor): Pair<Type, String> {
        val shortName = parcelerObject.name
        return Pair(Type.getObjectType(containerAsmType.internalName + "\$$shortName"), shortName.asString())
    }

    private fun ClassDescriptor.writeWriteToParcel(
            codegen: ImplementationBodyCodegen,
            properties: List<Pair<String, KotlinType>>,
            parcelAsmType: Type,
            parcelerObject: ClassDescriptor?
    ): Unit? {
        val containerAsmType = codegen.typeMapper.mapType(this.defaultType)

        return findFunction(WRITE_TO_PARCEL)?.write(codegen) {
            if (parcelerObject != null) {
                val (companionAsmType, companionFieldName) = getCompanionClassType(containerAsmType, parcelerObject)

                v.getstatic(containerAsmType.internalName, companionFieldName, companionAsmType.descriptor)
                v.load(0, containerAsmType)
                v.load(1, PARCEL_TYPE)
                v.load(2, Type.INT_TYPE)
                v.invokevirtual(companionAsmType.internalName, "write",
                                "(${containerAsmType.descriptor}${PARCEL_TYPE.descriptor}I)V", false)
            }
            else {
                val context = ParcelSerializer.ParcelSerializerContext(codegen.typeMapper, containerAsmType)

                for ((fieldName, type) in properties) {
                    val asmType = codegen.typeMapper.mapType(type)

                    v.load(1, parcelAsmType)
                    v.load(0, containerAsmType)
                    v.getfield(containerAsmType.internalName, fieldName, asmType.descriptor)

                    val serializer = ParcelSerializer.get(type, asmType, context)
                    serializer.writeValue(v)
                }
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
            parcelerObject: ClassDescriptor?,
            properties: List<Pair<String, KotlinType>>
    ) {
        val containerAsmType = codegen.typeMapper.mapType(parcelableClass)

        createMethod(creatorClass, CREATE_FROM_PARCEL, parcelableClass.builtIns.anyType, "in" to parcelClassType).write(codegen) {
            if (parcelerObject != null) {
                val (companionAsmType, companionFieldName) = getCompanionClassType(containerAsmType, parcelerObject)

                v.getstatic(containerAsmType.internalName, companionFieldName, companionAsmType.descriptor)
                v.load(1, PARCEL_TYPE)
                v.invokevirtual(companionAsmType.internalName, "create", "(${PARCEL_TYPE.descriptor})Landroid/os/Parcelable;", false)
            }
            else {
                v.anew(containerAsmType)
                v.dup()

                val asmConstructorParameters = StringBuilder()
                val context = ParcelSerializer.ParcelSerializerContext(codegen.typeMapper, containerAsmType)

                for ((_, type) in properties) {
                    val asmType = codegen.typeMapper.mapType(type)
                    asmConstructorParameters.append(asmType.descriptor)

                    val serializer = ParcelSerializer.get(type, asmType, context)
                    v.load(1, parcelAsmType)
                    serializer.readValue(v)
                }

                v.invokespecial(containerAsmType.internalName, "<init>", "($asmConstructorParameters)V", false)
            }

            v.areturn(containerAsmType)
        }
    }

    private fun writeCreatorAccessField(codegen: ImplementationBodyCodegen, parcelableClass: ClassDescriptor) {
        val parcelableAsmType = codegen.typeMapper.mapType(parcelableClass.defaultType)
        val creatorAsmType = Type.getObjectType(
                codegen.typeMapper.typeMappingConfiguration.innerClassNameFactory(parcelableAsmType.internalName, "Creator"))

        codegen.v.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_STATIC or ACC_PUBLIC or ACC_FINAL, "CREATOR",
                           creatorAsmType.descriptor, null, null)
    }

    private fun writeCreatorClass(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor,
            parcelClassType: KotlinType,
            parcelAsmType: Type,
            parcelerObject: ClassDescriptor?,
            properties: List<Pair<String, KotlinType>>
    ) {
        val containerAsmType = codegen.typeMapper.mapType(parcelableClass.defaultType)
        val creatorAsmType = Type.getObjectType(
                codegen.typeMapper.typeMappingConfiguration.innerClassNameFactory(containerAsmType.internalName, "Creator"))

        val creatorClass = ClassDescriptorImpl(
                parcelableClass, Name.identifier("Creator"), Modality.FINAL, ClassKind.CLASS, emptyList(),
                parcelableClass.source, false)

        creatorClass.initialize(
                MemberScope.Empty, emptySet(),
                DescriptorFactory.createPrimaryConstructorForObject(creatorClass, creatorClass.source))

        val classBuilderForCreator = codegen.state.factory.newVisitor(
                JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, creatorClass),
                Type.getObjectType(creatorAsmType.internalName),
                codegen.myClass.containingKtFile)

        val classContextForCreator = ClassContext(
                codegen.typeMapper, creatorClass, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
        val codegenForCreator = ImplementationBodyCodegen(
                codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

        classBuilderForCreator.defineClass(null, V1_6, ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
                              creatorAsmType.internalName, null, "java/lang/Object",
                              arrayOf("android/os/Parcelable\$Creator"))

        codegen.v.visitInnerClass(creatorAsmType.internalName, containerAsmType.internalName, "Creator", ACC_PUBLIC or ACC_STATIC)
        codegenForCreator.v.visitInnerClass(creatorAsmType.internalName, containerAsmType.internalName, "Creator", ACC_PUBLIC or ACC_STATIC)

        writeSyntheticClassMetadata(classBuilderForCreator, codegen.state)

        writeCreatorConstructor(codegenForCreator, creatorClass, creatorAsmType)
        writeNewArrayMethod(codegenForCreator, parcelableClass, creatorClass, parcelerObject)
        writeCreateFromParcel(codegenForCreator, parcelableClass, creatorClass, parcelClassType, parcelAsmType, parcelerObject, properties)

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
            creatorClass: ClassDescriptorImpl,
            parcelerObject: ClassDescriptor?
    ) {
        val builtIns = parcelableClass.builtIns
        val parcelableAsmType = codegen.typeMapper.mapType(parcelableClass)

        createMethod(creatorClass, NEW_ARRAY,
                builtIns.getArrayType(Variance.INVARIANT, parcelableClass.defaultType),
                "size" to builtIns.intType
        ).write(codegen) {
            if (parcelerObject != null) {
                val newArrayMethod = parcelerObject.unsubstitutedMemberScope
                        .getContributedFunctions(Name.identifier("newArray"), NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
                        .firstOrNull {
                            it.typeParameters.isEmpty()
                                && it.kind == CallableMemberDescriptor.Kind.DECLARATION
                                && (it.valueParameters.size == 1 && KotlinBuiltIns.isInt(it.valueParameters[0].type))
                                && !((it.containingDeclaration as? ClassDescriptor)?.defaultType?.isParceler ?: true)
                        }

                if (newArrayMethod != null) {
                    val containerAsmType = codegen.typeMapper.mapType(parcelableClass.defaultType)
                    val (companionAsmType, companionFieldName) = getCompanionClassType(containerAsmType, parcelerObject)

                    v.getstatic(containerAsmType.internalName, companionFieldName, companionAsmType.descriptor)
                    v.load(1, Type.INT_TYPE)
                    v.invokevirtual(companionAsmType.internalName, "newArray", "(I)[${containerAsmType.descriptor}", false)
                    v.areturn(Type.getType("[L$parcelableAsmType;"))

                    return@write
                }
            }

            v.load(1, Type.INT_TYPE)
            v.newarray(parcelableAsmType)
            v.areturn(Type.getType("[L$parcelableAsmType;"))
        }
    }

    private fun FunctionDescriptor.write(codegen: ImplementationBodyCodegen, code: ExpressionCodegen.() -> Unit) {
        val declarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, this)
        codegen.functionCodegen.generateMethod(declarationOrigin, this, object : CodegenBased(codegen.state) {
            override fun doGenerateBody(e: ExpressionCodegen, signature: JvmMethodSignature) = with(e) {
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