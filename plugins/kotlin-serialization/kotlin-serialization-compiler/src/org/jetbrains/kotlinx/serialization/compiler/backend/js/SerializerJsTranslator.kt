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

package org.jetbrains.kotlinx.serialization.compiler.backend.js

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.declaration.DefaultPropertyTranslator
import org.jetbrains.kotlin.js.translate.expression.ExpressionVisitor
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF.KOTLIN_EQUALS
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.getSerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.typeArgPrefix

class SerializerJsTranslator(descriptor: ClassDescriptor,
                             val translator: DeclarationBodyVisitor,
                             val context: TranslationContext) : SerializerCodegen(descriptor, context.bindingContext()) {

    private fun generateFunction(descriptor: FunctionDescriptor, bodyGen: JsBlockBuilder.(JsFunction, TranslationContext) -> Unit) {
        val f = context.buildFunction(descriptor, bodyGen)
        translator.addFunction(descriptor, f, null)
    }


    override fun generateSerialDesc() {
        val desc = generatedSerialDescPropertyDescriptor ?: return
        val serialDescImplClass = serializerDescriptor
                .getClassFromInternalSerializationPackage(SERIAL_DESCRIPTOR_CLASS_IMPL)
        val serialDescImplConstructor = serialDescImplClass
                .unsubstitutedPrimaryConstructor!!

        // this.serialDesc = new SerialDescImpl(...)
        val correctThis = context.getDispatchReceiver(JsDescriptorUtils.getReceiverParameterForDeclaration(desc.containingDeclaration))
        val value = JsNew(
            context.getInnerReference(serialDescImplConstructor),
            listOf(JsStringLiteral(serialName), if (isGeneratedSerializer) correctThis else JsNullLiteral())
        )
        val assgmnt = TranslationUtils.assignmentToBackingField(context, desc, value)
        translator.addInitializerStatement(assgmnt.makeStmt())

        // adding elements via serialDesc.addElement(...)
        val addFunc = serialDescImplClass.getFuncDesc(CallingConventions.addElement).single()
        val pushFunc = serialDescImplClass.getFuncDesc(CallingConventions.addAnnotation).single()
        val pushClassFunc = serialDescImplClass.getFuncDesc(CallingConventions.addClassAnnotation).single()
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(generatedSerialDescPropertyDescriptor), JsThisRef())

        for (prop in serializableProperties) {
            if (prop.transient) continue
            val call = JsInvocation(
                JsNameRef(context.getNameForDescriptor(addFunc), serialClassDescRef),
                JsStringLiteral(prop.name),
                JsBooleanLiteral(prop.optional)
            )
            translator.addInitializerStatement(call.makeStmt())
            // serialDesc.pushAnnotation(...)
            pushAnnotationsInto(prop.descriptor, pushFunc, serialClassDescRef)
        }

        // push class annotations
        pushAnnotationsInto(serializableDescriptor, pushClassFunc, serialClassDescRef)
    }

    private fun pushAnnotationsInto(annotated: Annotated, pushFunction: DeclarationDescriptor, intoRef: JsNameRef) {
        for ((annotationClass , args, _) in annotated.annotationsWithArguments()) {
            val argExprs = args.map { arg ->
                Translation.translateAsExpression(arg.getArgumentExpression()!!, context)
            }
            val classRef = context.translateQualifiedReference(annotationClass)
            val invok = JsInvocation(JsNameRef(context.getNameForDescriptor(pushFunction), intoRef), JsNew(classRef, argExprs))
            translator.addInitializerStatement(invok.makeStmt())
        }
    }

    override fun generateChildSerializersGetter(function: FunctionDescriptor) = generateFunction(function) { _, _ ->
        val allSerializers = serializableProperties.map { requireNotNull(serializerTower(it)) { "Property ${it.name} must have a serializer" } }
        +JsReturn(JsArrayLiteral(allSerializers))
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        val propDesc = generatedSerialDescPropertyDescriptor ?: return
        val propTranslator = DefaultPropertyTranslator(propDesc, context,
                                                       translator.getBackingFieldReference(propDesc))
        val getterDesc = propDesc.getter!!
        val getterExpr = context.getFunctionObject(getterDesc)
                .apply { propTranslator.generateDefaultGetterFunction(getterDesc, this) }
        translator.addProperty(propDesc, getterExpr, null)
    }

    override fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ClassConstructorDescriptor) {
        val f = context.buildFunction(typedConstructorDescriptor) { jsFun, context ->
            val thiz = jsFun.scope.declareName(Namer.ANOTHER_THIS_PARAMETER_NAME).makeRef()

            +JsVars(JsVars.JsVar(thiz.name, JsNew(context.getInnerNameForDescriptor(serializerDescriptor).makeRef())))
            jsFun.parameters.forEachIndexed { i, parameter ->
                val thisFRef = JsNameRef(context.scope().declareName("$typeArgPrefix$i"), thiz)
                +JsAstUtils.assignment(thisFRef, JsNameRef(parameter.name)).makeStmt()
            }
            +JsReturn(thiz)
        }

        f.name = context.getInnerNameForDescriptor(typedConstructorDescriptor);
        context.addDeclarationStatement(f.makeStmt())
    }

    private fun TranslationContext.referenceMethod(clazz: ClassDescriptor, name: String) =
        getNameForDescriptor(clazz.getFuncDesc(name).single())

    override fun generateSave(function: FunctionDescriptor) = generateFunction(function) { jsFun, ctx ->
        val encoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.ENCODER_CLASS)
        val kOutputClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.STRUCTURE_ENCODER_CLASS)
        val wBeginFunc = ctx.getNameForDescriptor(
            encoderClass.getFuncDesc(CallingConventions.begin).single { it.valueParameters.size == 2 })
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(anySerialDescProperty!!), JsThisRef())

        val serializableSource = ((serializableDescriptor.findPsi() as? KtPureClassOrObject)
            ?: throw AssertionError("Serializable descriptor $serializableDescriptor must have source file to build initializers map"))
        val initializersMap: Map<PropertyDescriptor, KtExpression?> =
            context.buildInitializersRemapping(serializableSource, serializableDescriptor.getSuperClassNotAny())

        // output.writeBegin(desc, [])
        val typeParams = serializableDescriptor.declaredTypeParameters.mapIndexed { idx, _ ->
            JsNameRef(context.scope().declareName("$typeArgPrefix$idx"), JsThisRef())
        }
        val call = JsInvocation(
            JsNameRef(wBeginFunc, JsNameRef(jsFun.parameters[0].name)),
            serialClassDescRef,
            JsArrayLiteral(typeParams)
        )
        val objRef = JsNameRef(jsFun.parameters[1].name)
        // output = output.writeBegin...
        val localOutputName = jsFun.scope.declareFreshName("output")
        val localOutputRef = JsNameRef(localOutputName)
        +JsVars(JsVars.JsVar(localOutputName, call))

        fun SerializableProperty.jsNameRef() = JsNameRef(ctx.getNameForDescriptor(descriptor), objRef)

        // todo: internal serialization via virtual calls
        val labeledProperties = serializableProperties.filter { !it.transient }
        for (index in labeledProperties.indices) {
            val property = labeledProperties[index]
            if (property.transient) continue
            // output.writeXxxElementValue(classDesc, index, value)
            val sti = getSerialTypeInfo(property)
            val innerSerial = serializerInstance(context, sti.serializer, property.module, property.type, property.genericIndex)
            val invocation = if (innerSerial == null) {
                val writeFunc =
                        kOutputClass.getFuncDesc("${CallingConventions.encode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}").single()
                                .let { ctx.getNameForDescriptor(it) }
                JsInvocation(
                    JsNameRef(writeFunc, localOutputRef),
                    serialClassDescRef,
                    JsIntLiteral(index),
                    property.jsNameRef()
                ).makeStmt()
            }
            else {
                val writeFunc =
                        kOutputClass.getFuncDesc("${CallingConventions.encode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}").single()
                                .let { ctx.getNameForDescriptor(it) }
                JsInvocation(
                    JsNameRef(writeFunc, localOutputRef),
                    serialClassDescRef,
                    JsIntLiteral(index),
                    innerSerial,
                    property.jsNameRef()
                ).makeStmt()
            }

            if (!property.optional) {
                +invocation
            } else {
                val shouldEncodeFunc = ctx.referenceMethod(kOutputClass, CallingConventions.shouldEncodeDefault)
                val defaultValue =
                    initializersMap.getValue(property.descriptor)?.let { Translation.translateAsExpression(it, ctx) }
                        ?: throw IllegalStateException("Optional property does not have an initializer?")
                val partA = JsAstUtils.not(KOTLIN_EQUALS.apply(property.jsNameRef(), listOf(defaultValue), ctx))
                val partB =
                    JsInvocation(JsNameRef(shouldEncodeFunc, localOutputRef), serialClassDescRef, JsIntLiteral(index))
                val cond = JsBinaryOperation(JsBinaryOperator.OR, partA, partB)
                +JsIf(cond, invocation)
            }
        }

        // output.writeEnd(serialClassDesc)
        val wEndFunc = kOutputClass.getFuncDesc(CallingConventions.end).single()
                .let { ctx.getNameForDescriptor(it) }
        +JsInvocation(JsNameRef(wEndFunc, localOutputRef), serialClassDescRef).makeStmt()
    }


    override fun generateLoad(function: FunctionDescriptor) = generateFunction(function) { jsFun, context ->
        val inputClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.STRUCTURE_DECODER_CLASS)
        val decoderClass = serializerDescriptor.getClassFromSerializationPackage(SerialEntityNames.DECODER_CLASS)
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(anySerialDescProperty!!), JsThisRef())

        // var index = -1, readAll = false
        val indexVar = JsNameRef(jsFun.scope.declareFreshName("index"))
        val readAllVar = JsNameRef(jsFun.scope.declareFreshName("readAll"))
        +JsVars(JsVars.JsVar(indexVar.name), JsVars.JsVar(readAllVar.name, JsBooleanLiteral(false)))

        // calculating bit mask vars
        val blocksCnt = serializableProperties.bitMaskSlotCount()
        fun bitMaskOff(i: Int) = bitMaskSlotAt(i)

        // var bitMask0 = 0, bitMask1 = 0...
        val bitMasks = (0 until blocksCnt).map { JsNameRef(jsFun.scope.declareFreshName("bitMask$it")) }
        +JsVars(bitMasks.map { JsVars.JsVar(it.name, JsIntLiteral(0)) }, false)

        // var localProp0, localProp1, ...
        val localProps = serializableProperties.mapIndexed { i, _ -> JsNameRef(jsFun.scope.declareFreshName("local$i")) }
        +JsVars(localProps.map { JsVars.JsVar(it.name) }, true)

        //input = input.readBegin(...)
        val typeParams = serializableDescriptor.declaredTypeParameters.mapIndexed { idx, _ ->
            JsNameRef(context.scope().declareName("$typeArgPrefix$idx"), JsThisRef())
        }
        val inputVar = JsNameRef(jsFun.scope.declareFreshName("input"))
        val readBeginF = decoderClass.getFuncDesc(CallingConventions.begin).single()
        val readBeginCall = JsInvocation(
            JsNameRef(context.getNameForDescriptor(readBeginF), JsNameRef(jsFun.parameters[0].name)),
            serialClassDescRef, JsArrayLiteral(typeParams)
        )
        +JsVars(JsVars.JsVar(inputVar.name, readBeginCall))

        // while(true) {
        val loop = JsLabel(jsFun.scope.declareFreshName("loopLabel"))
        val loopRef = JsNameRef(loop.name)
        jsWhile(JsBooleanLiteral(true), {
            // index = input.readElement(classDesc)
            val readElementF = context.getNameForDescriptor(inputClass.getFuncDesc(CallingConventions.decodeElementIndex).single())
            +JsAstUtils.assignment(
                indexVar,
                JsInvocation(JsNameRef(readElementF, inputVar), serialClassDescRef)
            ).makeStmt()
            // switch(index)
            jsSwitch(indexVar) {
                // -2: readAll = true
                case(JsIntLiteral(-2)) {
                    +JsAstUtils.assignment(
                        readAllVar,
                        JsBooleanLiteral(true)
                    ).makeStmt()
                }
                // all properties
                for ((i, property) in serializableProperties.withIndex()) {
                    case(JsIntLiteral(i)) {
                        // input.readXxxElementValue
                        val sti = getSerialTypeInfo(property)
                        val innerSerial = serializerInstance(context, sti.serializer, property.module, property.type, property.genericIndex)
                        val call: JsExpression = if (innerSerial == null) {
                            val unknownSer = (sti.elementMethodPrefix.isEmpty())
                            val readFunc =
                                inputClass.getFuncDesc("${CallingConventions.decode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}")
                                    // if readElementValue, must have 3 parameters, if readXXXElementValue - 2
                                    .single { !unknownSer || (it.valueParameters.size == 3) }
                                    .let { context.getNameForDescriptor(it) }
                            val readArgs = mutableListOf(serialClassDescRef, JsIntLiteral(i))
                            if (unknownSer) readArgs.add(
                                ExpressionVisitor.getObjectKClass(
                                    this@SerializerJsTranslator.context,
                                    property.type.toClassDescriptor!!
                                )
                            )
                            JsInvocation(JsNameRef(readFunc, inputVar), readArgs)
                        } else {
                            val notSeenTest = propNotSeenTest(bitMasks[bitMaskOff(i)], i)
                            val readFunc =
                                inputClass.getFuncDesc("${CallingConventions.decode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                                    .single()
                                    .let { context.getNameForDescriptor(it) }
                            val updateFunc =
                                inputClass.getFuncDesc("${CallingConventions.update}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                                    .single()
                                    .let { context.getNameForDescriptor(it) }
                            JsConditional(
                                notSeenTest,
                                JsInvocation(
                                    JsNameRef(readFunc, inputVar),
                                    serialClassDescRef,
                                    JsIntLiteral(i),
                                    innerSerial
                                ),
                                JsInvocation(
                                    JsNameRef(updateFunc, inputVar),
                                    serialClassDescRef,
                                    JsIntLiteral(i),
                                    innerSerial,
                                    localProps[i]
                                )
                            )
                        }
                        // localPropI = ...
                        +JsAstUtils.assignment(
                            localProps[i],
                            call
                        ).makeStmt()
                        // need explicit unit instance
                        if (sti.unit) {
                            +JsAstUtils.assignment(
                                localProps[i],
                                context.getQualifiedReference(property.type.builtIns.unit)
                            ).makeStmt()
                        }
                        // char unboxing crutch
                        if (KotlinBuiltIns.isCharOrNullableChar(property.type)) {
                            val coerceTo = TranslationUtils.getReturnTypeForCoercion(property.descriptor)
                            +JsAstUtils.assignment(
                                localProps[i],
                                TranslationUtils.coerce(context, localProps[i], coerceTo)
                            ).makeStmt()
                        }

                        // bitMask[i] |= 1 << x
                        val bitPos = 1 shl (i % 32)
                        +JsBinaryOperation(
                            JsBinaryOperator.ASG_BIT_OR,
                            bitMasks[bitMaskOff(i)],
                            JsIntLiteral(bitPos)
                        ).makeStmt()
                        // if (!readAll) break
                        +JsIf(JsAstUtils.not(readAllVar), JsBreak())
                    }
                }
                // case -1: break loop
                case(JsIntLiteral(-1)) {
                    +JsBreak(loopRef)
                }
                // default: throw
                default {
                    val excClassRef = serializableDescriptor.getClassFromSerializationPackage(SerialEntityNames.UNKNOWN_FIELD_EXC)
                        .let { context.translateQualifiedReference(it) }
                    +JsThrow(JsNew(excClassRef, listOf(indexVar)))
                }
            }
        }, loop)

        // input.readEnd(desc)
        val readEndF = inputClass.getFuncDesc(CallingConventions.end).single()
            .let { context.getNameForDescriptor(it) }
        +JsInvocation(
            JsNameRef(readEndF, inputVar),
            serialClassDescRef
        ).makeStmt()

        // deserialization constructor call
        // todo: external deserialization with primary constructor and setters calls after resolution of KT-11586
        val constrDesc = KSerializerDescriptorResolver.createLoadConstructorDescriptor(serializableDescriptor, context.bindingContext())
        val constrRef = context.getInnerNameForDescriptor(constrDesc).makeRef()
        val args: MutableList<JsExpression> = bitMasks.toMutableList()
        args += localProps
        args += JsNullLiteral()
        +JsReturn(JsInvocation(constrRef, args))
    }

    companion object {
        fun translate(declaration: KtPureClassOrObject, descriptor: ClassDescriptor, translator: DeclarationBodyVisitor, context: TranslationContext) {
            if (getSerializableClassDescriptorBySerializer(descriptor) != null)
                SerializerJsTranslator(descriptor, translator, context).generate()
        }
    }
}