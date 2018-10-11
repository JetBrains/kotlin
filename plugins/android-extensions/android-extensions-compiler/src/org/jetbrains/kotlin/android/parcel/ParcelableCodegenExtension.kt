
package org.jetbrains.kotlin.android.parcel
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

import kotlinx.android.parcel.TypeParceler
import org.jetbrains.kotlin.android.parcel.ParcelableResolveExtension.Companion.createMethod
import org.jetbrains.kotlin.android.parcel.ParcelableSyntheticComponent.*
import org.jetbrains.kotlin.android.parcel.serializers.*
import org.jetbrains.kotlin.android.parcel.ParcelableSyntheticComponent.ComponentKind.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy.CodegenBased
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.writeSyntheticClassMetadata
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import java.io.FileDescriptor

open class ParcelableCodegenExtension : ExpressionCodegenExtension {
    private companion object {
        private val FILE_DESCRIPTOR_FQNAME = FqName(FileDescriptor::class.java.canonicalName)
        private val CREATOR_NAME = Name.identifier("CREATOR")

        private val ALLOWED_CLASS_KINDS = listOf(ClassKind.CLASS, ClassKind.OBJECT, ClassKind.ENUM_CLASS)
    }

    protected open fun isExperimental(element: KtElement) = true

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = true

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        val parcelableClass = codegen.descriptor
        if (!parcelableClass.isParcelize) return

        val sourceElement = (codegen.myClass as? KtClassOrObject) ?: return
        if (!isExperimental(sourceElement)) return

        if (parcelableClass.kind !in ALLOWED_CLASS_KINDS) return

        val propertiesToSerialize = getPropertiesToSerialize(codegen, parcelableClass)

        val parcelerObject = parcelableClass.companionObjectDescriptor?.takeIf {
            TypeUtils.getAllSupertypes(it.defaultType).any { it.isParceler }
        }

        with (parcelableClass) {
            if (hasSyntheticDescribeContents()) {
                writeDescribeContentsFunction(codegen, propertiesToSerialize)
            }

            if (hasSyntheticWriteToParcel()) {
                writeWriteToParcel(codegen, propertiesToSerialize, PARCEL_TYPE, parcelerObject)
            }

            if (!hasCreatorField()) {
                writeCreatorAccessField(codegen)
            }
        }

        if (codegen.state.classBuilderMode != ClassBuilderMode.LIGHT_CLASSES) {
            val parcelClassType = ParcelableResolveExtension.resolveParcelClassType(parcelableClass.module)
                                  ?: error("Can't resolve 'android.os.Parcel' class")

            writeCreatorClass(codegen, parcelableClass, parcelClassType, PARCEL_TYPE, parcelerObject, propertiesToSerialize)
        }
    }

    private fun ClassDescriptor.hasCreatorField(): Boolean {
        val companionObject = companionObjectDescriptor ?: return false

        if (companionObject.name == CREATOR_NAME) {
            return true
        }

        return companionObject.unsubstitutedMemberScope
                .getContributedVariables(CREATOR_NAME, NoLookupLocation.FROM_BACKEND)
                .isNotEmpty()
    }

    private fun ClassDescriptor.hasSyntheticDescribeContents() = hasParcelizeSyntheticMethod(ComponentKind.DESCRIBE_CONTENTS)

    private fun ClassDescriptor.hasSyntheticWriteToParcel() = hasParcelizeSyntheticMethod(ComponentKind.WRITE_TO_PARCEL)

    private fun ClassDescriptor.hasParcelizeSyntheticMethod(componentKind: ParcelableSyntheticComponent.ComponentKind): Boolean {
        val methodName = Name.identifier(componentKind.methodName)

        val writeToParcelMethods = unsubstitutedMemberScope
                .getContributedFunctions(methodName, NoLookupLocation.FROM_BACKEND)
                .filter { it is ParcelableSyntheticComponent && it.componentKind == componentKind }

        return writeToParcelMethods.size == 1
    }

    private fun getCompanionClassType(containerAsmType: Type, parcelerObject: ClassDescriptor): Pair<Type, String> {
        val shortName = parcelerObject.name
        return Pair(Type.getObjectType(containerAsmType.internalName + "\$$shortName"), shortName.asString())
    }

    private fun ClassDescriptor.writeWriteToParcel(
            codegen: ImplementationBodyCodegen,
            properties: List<PropertyToSerialize>,
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
                val frameMap = FrameMap().apply {
                    enterTemp(containerAsmType)
                    enterTemp(PARCEL_TYPE)
                    enterTemp(Type.INT_TYPE)
                }

                val globalContext = ParcelSerializer.ParcelSerializerContext(codegen.typeMapper, containerAsmType, emptyList(), frameMap)

                if (properties.isEmpty()) {
                    val entityType = this@writeWriteToParcel.defaultType
                    val asmType = codegen.state.typeMapper.mapType(entityType)
                    val serializer = if (this@writeWriteToParcel.kind == ClassKind.CLASS) {
                        NullAwareParcelSerializerWrapper(ZeroParameterClassSerializer(asmType, entityType))
                    } else {
                        ParcelSerializer.get(entityType, asmType, globalContext, strict = true)
                    }

                    v.load(1, parcelAsmType)
                    v.load(0, containerAsmType)
                    serializer.writeValue(v)
                } else {
                    for ((fieldName, type, parcelers) in properties) {
                        val asmType = codegen.typeMapper.mapType(type)

                        v.load(1, parcelAsmType)
                        v.load(0, containerAsmType)
                        v.getfield(containerAsmType.internalName, fieldName, asmType.descriptor)

                        val serializer = ParcelSerializer.get(type, asmType, globalContext.copy(typeParcelers = parcelers))
                        serializer.writeValue(v)
                    }

                }
            }

            v.areturn(Type.VOID_TYPE)
        }
    }

    private fun ClassDescriptor.writeDescribeContentsFunction(
            codegen: ImplementationBodyCodegen,
            propertiesToSerialize: List<PropertyToSerialize>
    ): Unit? {
        val hasFileDescriptorAnywhere = propertiesToSerialize.any { it.type.containsFileDescriptor() }

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

    data class PropertyToSerialize(val name: String, val type: KotlinType, val parcelers: List<TypeParcelerMapping>)

    private fun getPropertiesToSerialize(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor
    ): List<PropertyToSerialize> {
        if (parcelableClass.kind != ClassKind.CLASS) {
            return emptyList()
        }

        val constructor = parcelableClass.constructors.firstOrNull { it.isPrimary } ?: return emptyList()

        val propertiesToSerialize = constructor.valueParameters.mapNotNull { param ->
            codegen.bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, param]
        }

        val classParcelers = getTypeParcelers(parcelableClass.annotations)

        return propertiesToSerialize.map {
            PropertyToSerialize(it.name.asString(), it.type, classParcelers + getTypeParcelers(it.annotations))
        }
    }

    private fun writeCreateFromParcel(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor,
            creatorClass: ClassDescriptorImpl,
            parcelClassType: KotlinType,
            parcelAsmType: Type,
            parcelerObject: ClassDescriptor?,
            properties: List<PropertyToSerialize>
    ) {
        val containerAsmType = codegen.typeMapper.mapType(parcelableClass)
        val creatorAsmType = codegen.typeMapper.mapType(creatorClass)

        createMethod(
            creatorClass, CREATE_FROM_PARCEL, Modality.FINAL,
            parcelableClass.builtIns.anyType, "in" to parcelClassType
        ).write(codegen) {
            if (parcelerObject != null) {
                val (companionAsmType, companionFieldName) = getCompanionClassType(containerAsmType, parcelerObject)

                v.getstatic(containerAsmType.internalName, companionFieldName, companionAsmType.descriptor)
                v.load(1, PARCEL_TYPE)
                v.invokevirtual(companionAsmType.internalName, "create", "(${PARCEL_TYPE.descriptor})Ljava/lang/Object;", false)
            }
            else {
                v.anew(containerAsmType)
                v.dup()

                val asmConstructorParameters = StringBuilder()
                val frameMap = FrameMap().apply {
                    enterTemp(creatorAsmType)
                    enterTemp(PARCEL_TYPE)
                }

                val globalContext = ParcelSerializer.ParcelSerializerContext(codegen.typeMapper, containerAsmType, emptyList(), frameMap)

                if (properties.isEmpty()) {
                    val entityType = parcelableClass.defaultType
                    val asmType = codegen.state.typeMapper.mapType(entityType)
                    val serializer = if (parcelableClass.kind == ClassKind.CLASS) {
                        NullAwareParcelSerializerWrapper(ZeroParameterClassSerializer(asmType, entityType))
                    } else {
                        ParcelSerializer.get(entityType, asmType, globalContext, strict = true)
                    }
                    v.load(1, parcelAsmType)
                    serializer.readValue(v)
                } else {
                    for ((_, type, parcelers) in properties) {
                        val asmType = codegen.typeMapper.mapType(type)
                        asmConstructorParameters.append(asmType.descriptor)

                        val serializer = ParcelSerializer.get(type, asmType, globalContext.copy(typeParcelers = parcelers))
                        v.load(1, parcelAsmType)
                        serializer.readValue(v)
                    }

                    v.invokespecial(containerAsmType.internalName, "<init>", "($asmConstructorParameters)V", false)
                }
            }

            v.areturn(containerAsmType)
        }
    }

    private fun writeCreatorAccessField(codegen: ImplementationBodyCodegen) {
        val creatorType = Type.getObjectType("android/os/Parcelable\$Creator")

        codegen.v.newField(JvmDeclarationOrigin.NO_ORIGIN, ACC_STATIC or ACC_PUBLIC or ACC_FINAL, "CREATOR",
                           creatorType.descriptor, null, null)
    }

    private fun writeCreatorClass(
            codegen: ImplementationBodyCodegen,
            parcelableClass: ClassDescriptor,
            parcelClassType: KotlinType,
            parcelAsmType: Type,
            parcelerObject: ClassDescriptor?,
            properties: List<PropertyToSerialize>
    ) {
        val containerAsmType = codegen.typeMapper.mapType(parcelableClass.defaultType)
        val creatorAsmType = Type.getObjectType(containerAsmType.internalName + "\$Creator")

        val creatorClass = ClassDescriptorImpl(
                parcelableClass, Name.identifier("Creator"), Modality.FINAL, ClassKind.CLASS, emptyList(),
                parcelableClass.source, false, LockBasedStorageManager.NO_LOCKS)

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

        createMethod(creatorClass, NEW_ARRAY, Modality.FINAL,
                builtIns.getArrayType(Variance.INVARIANT, builtIns.anyType),
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
                    v.invokevirtual(companionAsmType.internalName, "newArray", "(I)[Ljava/lang/Object;", false)
                    v.areturn(Type.getType("[Ljava/lang/Object;"))

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

internal fun getTypeParcelers(annotations: Annotations): List<TypeParcelerMapping> {
    val typeParcelerFqName = FqName(TypeParceler::class.java.name)
    val serializers = mutableListOf<TypeParcelerMapping>()

    for (anno in annotations.filter { it.fqName == typeParcelerFqName }) {
        val (mappedType, parcelerType) = anno.type.arguments.takeIf { it.size == 2 } ?: continue
        serializers += TypeParcelerMapping(mappedType.type, parcelerType.type)
    }

    return serializers
}