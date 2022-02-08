/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlinx.serialization.compiler.backend.common.*
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.VersionReader
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.serializableAnnotationIsUseless
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ARRAY_MASK_FIELD_MISSING_FUNC_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.CACHED_DESCRIPTOR_FIELD
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SINGLE_MASK_FIELD_MISSING_FUNC_NAME
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class SerializableCodegenImpl(
    private val classCodegen: ImplementationBodyCodegen
) : SerializableCodegen(classCodegen.descriptor, classCodegen.bindingContext) {

    private val thisAsmType = classCodegen.typeMapper.mapClass(serializableDescriptor)
    private val fieldMissingOptimizationVersion = ApiVersion.parse("1.1")!!
    private val useFieldMissingOptimization = canUseFieldMissingOptimization()

    companion object {
        fun generateSerializableExtensions(codegen: ImplementationBodyCodegen) {
            val serializableClass = codegen.descriptor
            if (serializableClass.isInternalSerializable)
                SerializableCodegenImpl(codegen).generate()
            else if (serializableClass.serializableAnnotationIsUseless) {
                throw CompilationException(
                    "@Serializable annotation on $serializableClass would be ignored because it is impossible to serialize it automatically. " +
                            "Provide serializer manually via e.g. companion object", null, serializableClass.findPsi()
                )
            }
        }
    }

    private val descToProps = classCodegen.myClass.bodyPropertiesDescriptorsMap(classCodegen.bindingContext)

    private val paramsToProps: Map<PropertyDescriptor, KtParameter> =
        classCodegen.myClass.primaryConstructorPropertiesDescriptorsMap(classCodegen.bindingContext)

    private fun getProp(prop: SerializableProperty) = descToProps[prop.descriptor]
    private fun getParam(prop: SerializableProperty) = paramsToProps[prop.descriptor]

    private fun initializersMapper(prop: SerializableProperty): Pair<KtExpression, Type> {
        val maybeInit =
            getProp(prop)?.let { it.delegateExpressionOrInitializer ?: throw AssertionError("${it.name} property must have initializer") }

        val initializer = maybeInit ?: getParam(prop)?.let {
            it.defaultValue ?: throw AssertionError("${it.name} property must have initializer")
        }

        return if (initializer == null) throw AssertionError("Can't find initializer for property ${prop.descriptor}")
        else initializer to classCodegen.typeMapper.mapType(prop.type)
    }

    private val SerializableProperty.asmType get() = classCodegen.typeMapper.mapType(this.type)

    override fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor) {
        classCodegen.generateMethod(constructorDescriptor) { _, expr -> doGenerateConstructorImpl(expr) }
    }

    override fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {
        classCodegen.generateMethod(methodDescriptor) { _, expr -> doGenerateWriteSelf(expr) }
    }

    private fun InstructionAdapter.doGenerateWriteSelf(exprCodegen: ExpressionCodegen) {
        val thisI = 0
        val outputI = 1
        val serialDescI = 2
        val offsetI = 3

        val superClass = serializableDescriptor.getSuperClassOrAny()
        val myPropsStart: Int
        if (superClass.isInternalSerializable) {
            myPropsStart = bindingContext.serializablePropertiesFor(superClass).serializableProperties.size
            val superTypeArguments =
                serializableDescriptor.typeConstructor.supertypes.single { it.toClassDescriptor?.isInternalSerializable == true }.arguments
            //super.writeSelf(output, serialDesc)
            load(thisI, thisAsmType)
            load(outputI, kOutputType)
            load(serialDescI, descType)
            superTypeArguments.forEach {
                val genericIdx = serializableDescriptor.defaultType.arguments.indexOf(it).let { if (it == -1) null else it }
                val serial = findTypeSerializerOrContext(serializableDescriptor.module, it.type)
                stackValueSerializerInstance(classCodegen, serializableDescriptor.module, it.type, serial, this, genericIdx) { i, _ ->
                    load(offsetI + i, kSerializerType)
                }
            }
            val superSignature =
                classCodegen.typeMapper.mapSignatureSkipGeneric(KSerializerDescriptorResolver.createWriteSelfFunctionDescriptor(superClass))
            invokestatic(
                classCodegen.typeMapper.mapType(superClass).internalName,
                superSignature.asmMethod.name,
                superSignature.asmMethod.descriptor,
                false
            )
        } else {
            myPropsStart = 0
        }

        fun emitEncoderCall(property: SerializableProperty, index: Int) {
            // output.writeXxxElementValue (desc, index, value)
            load(outputI, kOutputType)
            load(serialDescI, descType)
            iconst(index)
            genKOutputMethodCall(
                property,
                classCodegen,
                exprCodegen,
                thisAsmType,
                thisI,
                offsetI,
                generator = this@SerializableCodegenImpl
            )
        }

        for (i in myPropsStart until properties.serializableProperties.size) {
            val property = properties[i]
            if (!property.optional) {
                emitEncoderCall(property, i)
            } else {
                val writeLabel = Label()
                val nonWriteLabel = Label()
                // obj.prop != DEFAULT_VAL
                val propAsmType = classCodegen.typeMapper.mapType(property.type)
                val actualType: JvmKotlinType = ImplementationBodyCodegen.genPropertyOnStack(
                    this,
                    exprCodegen.context,
                    property.descriptor,
                    thisAsmType,
                    thisI,
                    classCodegen.state
                )
                StackValue.coerce(actualType.type, propAsmType, this)
                val lhs = StackValue.onStack(propAsmType)
                val (expr, _) = initializersMapper(property)
                exprCodegen.gen(expr, propAsmType)
                val rhs = StackValue.onStack(propAsmType)
                // INVOKESTATIC kotlin/jvm/internal/Intrinsics.areEqual (Ljava/lang/Object;Ljava/lang/Object;)Z
                DescriptorAsmUtil.genEqualsForExpressionsOnStack(KtTokens.EXCLEQ, lhs, rhs).put(Type.BOOLEAN_TYPE, null, this)
                ifne(writeLabel)

                // output.shouldEncodeElementDefault(descriptor, i)
                load(outputI, kOutputType)
                load(serialDescI, descType)
                iconst(i)
                invokeinterface(kOutputType.internalName, CallingConventions.shouldEncodeDefault, "(${descType.descriptor}I)Z")
                ifeq(nonWriteLabel)

                visitLabel(writeLabel)
                emitEncoderCall(property, i)
                visitLabel(nonWriteLabel)
            }
        }

        areturn(Type.VOID_TYPE)
    }

    private fun InstructionAdapter.doGenerateConstructorImpl(exprCodegen: ExpressionCodegen) {
        val seenMaskVar = 1
        val bitMaskOff = fun(it: Int): Int { return seenMaskVar + bitMaskSlotAt(it) }
        val bitMaskEnd = seenMaskVar + properties.serializableProperties.bitMaskSlotCount()

        if (useFieldMissingOptimization) {
            generateOptimizedGoldenMaskCheck(seenMaskVar)
        }

        var (propIndex, propOffset) = generateSuperSerializableCall(seenMaskVar, bitMaskEnd)
        for (i in propIndex until properties.serializableProperties.size) {
            val prop = properties[i]
            val propType = prop.asmType
            if (!prop.optional) {
                if (!useFieldMissingOptimization) {
                    // primary were validated before constructor call
                    genValidateProperty(i, bitMaskOff(i))
                    val nonThrowLabel = Label()
                    ificmpne(nonThrowLabel)
                    genMissingFieldExceptionThrow(prop.name)
                    visitLabel(nonThrowLabel)
                }

                // setting field
                load(0, thisAsmType)
                load(propOffset, propType)
                putfield(thisAsmType.internalName, prop.descriptor.name.asString(), propType.descriptor)
            } else {
                genValidateProperty(i, bitMaskOff(i))
                val setLbl = Label()
                val nextLabel = Label()
                ificmpeq(setLbl)
                // setting field
                load(0, thisAsmType)
                load(propOffset, propType)
                putfield(thisAsmType.internalName, prop.descriptor.name.asString(), propType.descriptor)
                goTo(nextLabel)
                visitLabel(setLbl)
                // setting defaultValue
                if (classCodegen.bindingContext[BindingContext.BACKING_FIELD_REQUIRED, prop.descriptor] != true)
                    throw CompilationException(
                        "Optional properties without backing fields doesn't have much sense, maybe you want transient?",
                        null,
                        getProp(prop)
                    )
                exprCodegen.genInitProperty(prop)
                visitLabel(nextLabel)
            }
            propOffset += prop.asmType.size
        }

        // these properties required to be manually invoked, because they are not in serializableProperties
        val serializedProps = properties.serializableProperties.map { it.descriptor }

        (descToProps - serializedProps)
            .filter { classCodegen.shouldInitializeProperty(it.value) }
            .forEach { (_, prop) -> classCodegen.initializeProperty(exprCodegen, prop) }
        (paramsToProps - serializedProps)
            .forEach { (t, u) -> exprCodegen.genInitParam(t, u) }

        // init blocks
        // todo: proper order with other initializers?
        classCodegen.myClass.anonymousInitializers()
            .forEach { exprCodegen.gen(it, Type.VOID_TYPE) }
        areturn(Type.VOID_TYPE)
    }

    private fun InstructionAdapter.generateSuperSerializableCall(maskVar: Int, propStartVar: Int): Pair<Int, Int> {
        val superClass = serializableDescriptor.getSuperClassOrAny()
        val superType = classCodegen.typeMapper.mapType(superClass).internalName

        load(0, thisAsmType)

        if (!superClass.isInternalSerializable) {
            require(superClass.constructors.firstOrNull { it.valueParameters.isEmpty() } != null) {
                "Non-serializable parent of serializable $serializableDescriptor must have no arg constructor"
            }

            // call
            // Sealed classes have private <init> so they cannot be inherited from Java src
            // public <init> is synthetic and contains additional parameter
            val desc = if (DescriptorUtils.isSealedClass(superClass)) {
                aconst(null)
                "(Lkotlin/jvm/internal/DefaultConstructorMarker;)V"
            } else {
                "()V"
            }
            invokespecial(superType, "<init>", desc, false)
            return 0 to propStartVar
        } else {
            val superProps = bindingContext.serializablePropertiesFor(superClass).serializableProperties
            val creator = buildInternalConstructorDesc(propStartVar, maskVar, classCodegen, superProps)
            invokespecial(superType, "<init>", creator, false)
            return superProps.size to propStartVar + superProps.sumOf { it.asmType.size }
        }
    }

    private fun InstructionAdapter.generateOptimizedGoldenMaskCheck(maskVar: Int) {
        if (serializableDescriptor.isAbstractOrSealedSerializableClass()) {
            // for abstract classes fields MUST BE checked in child classes
            return
        }

        val allPresentsLabel = Label()
        val maskSlotCount = properties.serializableProperties.bitMaskSlotCount()
        if (maskSlotCount == 1) {
            val goldenMask = properties.goldenMask

            iconst(goldenMask)
            dup()
            load(maskVar, OPT_MASK_TYPE)
            and(OPT_MASK_TYPE)
            ificmpeq(allPresentsLabel)

            load(maskVar, OPT_MASK_TYPE)
            iconst(goldenMask)

            stackSerialDescriptor()
            invokestatic(
                pluginUtilsType.internalName,
                SINGLE_MASK_FIELD_MISSING_FUNC_NAME.asString(),
                "(II${descType.descriptor})V",
                false
            )
        } else {
            val fieldsMissingLabel = Label()

            val goldenMaskList = properties.goldenMaskList
            goldenMaskList.forEachIndexed { i, goldenMask ->
                val maskIndex = maskVar + i
                // if( (goldenMask & seen) != goldenMask )
                iconst(goldenMask)
                dup()
                load(maskIndex, OPT_MASK_TYPE)
                and(OPT_MASK_TYPE)
                ificmpne(fieldsMissingLabel)
            }
            goTo(allPresentsLabel)

            visitLabel(fieldsMissingLabel)
            // prepare seen array
            fillArray(OPT_MASK_TYPE, goldenMaskList) { i, _ ->
                load(maskVar + i, OPT_MASK_TYPE)
            }
            // prepare golden mask array
            fillArray(OPT_MASK_TYPE, goldenMaskList) { _, goldenMask ->
                iconst(goldenMask)
            }
            stackSerialDescriptor()
            invokestatic(
                pluginUtilsType.internalName,
                ARRAY_MASK_FIELD_MISSING_FUNC_NAME.asString(),
                "([I[I${descType.descriptor})V",
                false
            )
        }
        visitLabel(allPresentsLabel)
    }

    private fun InstructionAdapter.stackSerialDescriptor() {
        if (serializableDescriptor.isStaticSerializable) {
            val serializer = serializableDescriptor.classSerializer!!
            StackValue.singleton(serializer, classCodegen.typeMapper).put(kSerializerType, this)
            invokeinterface(kSerializerType.internalName, descriptorGetterName, "()${descType.descriptor}")
        } else {
            generateStaticDescriptorField()

            getstatic(thisAsmType.internalName, CACHED_DESCRIPTOR_FIELD, descType.descriptor)
        }
    }

    private fun generateStaticDescriptorField() {
        val flags = Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_STATIC
        classCodegen.v.newField(
            OtherOrigin(classCodegen.myClass.psiOrParent), flags,
            CACHED_DESCRIPTOR_FIELD, descType.descriptor, null, null
        )

        val clInit = classCodegen.createOrGetClInitCodegen()
        with(clInit.v) {
            anew(descImplType)
            dup()
            aconst(serializableDescriptor.serialName())
            aconst(null)
            aconst(properties.serializableProperties.size)
            invokespecial(descImplType.internalName, "<init>", "(Ljava/lang/String;${generatedSerializerType.descriptor}I)V", false)
            for (property in properties.serializableProperties) {
                dup()
                aconst(property.name)
                iconst(if (property.optional) 1 else 0)
                invokevirtual(descImplType.internalName, CallingConventions.addElement, "(Ljava/lang/String;Z)V", false)
            }

            putstatic(thisAsmType.internalName, CACHED_DESCRIPTOR_FIELD, descType.descriptor)
        }
    }

    private fun ExpressionCodegen.genInitProperty(prop: SerializableProperty) = getProp(prop)?.let {
        classCodegen.initializeProperty(this, it)
    }
        ?: getParam(prop)?.let {
            this.v.load(0, thisAsmType)
            if (!it.hasDefaultValue()) throw CompilationException(
                "Optional field ${it.name} in primary constructor of serializable " +
                        "$serializableDescriptor must have default value", null, it
            )
            this.gen(it.defaultValue, prop.asmType)
            this.v.putfield(thisAsmType.internalName, prop.descriptor.name.asString(), prop.asmType.descriptor)
        }
        ?: throw IllegalStateException()

    private fun ExpressionCodegen.genInitParam(prop: PropertyDescriptor, param: KtParameter) {
        this.v.load(0, thisAsmType)
        val mapType = classCodegen.typeMapper.mapType(prop.type)
        if (!param.hasDefaultValue()) throw CompilationException(
            "Transient field ${param.name} in primary constructor of serializable " +
                    "$serializableDescriptor must have default value", null, param
        )
        this.gen(param.defaultValue, mapType)
        this.v.putfield(thisAsmType.internalName, prop.name.asString(), mapType.descriptor)
    }

    private fun canUseFieldMissingOptimization(): Boolean {
        val implementationVersion = VersionReader.getVersionsForCurrentModuleFromContext(
            currentDeclaration.module,
            bindingContext
        )?.implementationVersion
        return if (implementationVersion != null) implementationVersion >= fieldMissingOptimizationVersion else false
    }
}
