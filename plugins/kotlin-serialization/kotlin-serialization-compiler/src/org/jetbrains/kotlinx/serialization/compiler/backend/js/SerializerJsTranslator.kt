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
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.declaration.DeclarationBodyVisitor
import org.jetbrains.kotlin.js.translate.declaration.DefaultPropertyTranslator
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.shouldBoxReturnValue
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.annotationVarsAndDesc
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializer
import org.jetbrains.kotlinx.serialization.compiler.backend.common.getSerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

class SerializerJsTranslator(declaration: KtPureClassOrObject,
                             val translator: DeclarationBodyVisitor,
                             val context: TranslationContext) : SerializerCodegen(declaration, context.bindingContext()) {

    private fun generateFunction(descriptor: FunctionDescriptor, bodyGen: JsBlockBuilder.(JsFunction, TranslationContext) -> Unit) {
        val f = context.buildFunction(descriptor, bodyGen)
        translator.addFunction(descriptor, f, null)
    }


    override fun generateSerialDesc() {
        val desc = serialDescPropertyDescriptor ?: return
        val serialDescImplClass = serializerDescriptor
                .getClassFromInternalSerializationPackage("SerialClassDescImpl")
        val serialDescImplConstructor = serialDescImplClass
                .unsubstitutedPrimaryConstructor!!

        // this.serialDesc = new SerialDescImpl(...)
        val value = JsNew(context.getInnerReference(serialDescImplConstructor), listOf(JsStringLiteral(serialName)))
        val assgmnt = TranslationUtils.assignmentToBackingField(context, desc, value)
        translator.addInitializerStatement(assgmnt.makeStmt())

        // adding elements via serialDesc.addElement(...)
        val addFunc = serialDescImplClass.getFuncDesc("addElement").single()
        val pushFunc = serialDescImplClass.getFuncDesc("pushAnnotation").single()
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(serialDescPropertyDescriptor), JsThisRef())

        for (prop in orderedProperties) {
            if (prop.transient) continue
            val call = JsInvocation(JsNameRef(context.getNameForDescriptor(addFunc), serialClassDescRef), JsStringLiteral(prop.name))
            translator.addInitializerStatement(call.makeStmt())
            // serialDesc.pushAnnotation(...)
            for (annotationClass in prop.annotations) {
                val (args, _) = prop.annotationVarsAndDesc(annotationClass)
                val argExprs = args.map { arg ->
                    Translation.translateAsExpression(arg.getArgumentExpression()!!, context)
                }
                val classRef = context.getQualifiedReference(annotationClass)
                val invok = JsInvocation(JsNameRef(context.getNameForDescriptor(pushFunc), serialClassDescRef), JsNew(classRef, argExprs))
                translator.addInitializerStatement(invok.makeStmt())
            }
        }
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        val propDesc = serialDescPropertyDescriptor ?: return
        val propTranslator = DefaultPropertyTranslator(propDesc, context,
                                                       translator.getBackingFieldReference(propDesc))
        val getterDesc = propDesc.getter!!
        val getterExpr = context.getFunctionObject(getterDesc)
                .apply { propTranslator.generateDefaultGetterFunction(getterDesc, this) }
        translator.addProperty(propDesc, getterExpr, null)
    }

    private fun ClassDescriptor.getFuncDesc(funcName: String) =
            unsubstitutedMemberScope.getDescriptorsFiltered { it == Name.identifier(funcName) }

    override fun generateSave(function: FunctionDescriptor) = generateFunction(function) { jsFun, ctx ->
        val kOutputClass = serializerDescriptor.getClassFromSerializationPackage("KOutput")
        val wBeginFunc = ctx.getNameForDescriptor(
                kOutputClass.getFuncDesc("writeBegin").single { (it as FunctionDescriptor).valueParameters.size == 2 })
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(serialDescPropertyDescriptor!!), JsThisRef())

        // output.writeBegin(desc, [])
        val call = JsInvocation(JsNameRef(wBeginFunc, JsNameRef(jsFun.parameters[0].name)),
                                serialClassDescRef,
                                JsArrayLiteral())
        val objRef = JsNameRef(jsFun.parameters[1].name)
        // output = output.writeBegin...
        val localOutputName = jsFun.scope.declareFreshName("output")
        val localOutputRef = JsNameRef(localOutputName)
        +JsVars(JsVars.JsVar(localOutputName, call))

        // todo: internal serialization via virtual calls
        val labeledProperties = orderedProperties.filter { !it.transient }
        for (index in labeledProperties.indices) {
            val property = labeledProperties[index]
            if (property.transient) continue
            // output.writeXxxElementValue(classDesc, index, value)
            val sti = getSerialTypeInfo(property)
            val innerSerial = if (sti.serializer == null) null else serializerInstance(sti.serializer, property.module, property.type)
            if (innerSerial == null) {
                val writeFunc =
                        kOutputClass.getFuncDesc("write${sti.elementMethodPrefix}ElementValue").single()
                                .let { ctx.getNameForDescriptor(it) }
                +JsInvocation(JsNameRef(writeFunc, localOutputRef),
                              serialClassDescRef,
                              JsIntLiteral(index),
                              JsNameRef(ctx.getNameForDescriptor(property.descriptor), objRef)).makeStmt()
            }
            else {
                val writeFunc =
                        kOutputClass.getFuncDesc("write${sti.elementMethodPrefix}SerializableElementValue").single()
                                .let { ctx.getNameForDescriptor(it) }
                +JsInvocation(JsNameRef(writeFunc, localOutputRef),
                              serialClassDescRef,
                              JsIntLiteral(index),
                              innerSerial,
                              JsNameRef(ctx.getNameForDescriptor(property.descriptor), objRef)).makeStmt()
            }
        }

        // output.writeEnd(serialClassDesc)
        val wEndFunc = kOutputClass.getFuncDesc("writeEnd").single()
                .let { ctx.getNameForDescriptor(it) }
        +JsInvocation(JsNameRef(wEndFunc, localOutputRef), serialClassDescRef).makeStmt()
    }

    private fun getQualifiedClassReferenceName(classDescriptor: ClassDescriptor): JsExpression {
        return context.getQualifiedReference(classDescriptor)
    }

    private fun serializerInstance(serializerClass: ClassDescriptor, module: ModuleDescriptor, kType: KotlinType): JsExpression? {
        val nullableSerClass = getQualifiedClassReferenceName(
                serializerClass.getClassFromInternalSerializationPackage("NullableSerializer"))
        if (serializerClass.kind == ClassKind.OBJECT) {
            return getQualifiedClassReferenceName(serializerClass)
        }
        else {
            var args = if (serializerClass.classId == enumSerializerId)
                listOf(createGetKClassExpression(kType.toClassDescriptor!!))
            else kType.arguments.map {
                val argSer = findTypeSerializer(module, it.type) ?: return null
                val expr = serializerInstance(argSer, module, it.type) ?: return null
                if (it.type.isMarkedNullable) JsNew(nullableSerClass, listOf(expr)) else expr
            }
            if (serializerClass.classId == referenceArraySerializerId)
               args = listOf(createGetKClassExpression(kType.arguments[0].type.toClassDescriptor!!)) + args
            return JsNew(getQualifiedClassReferenceName(serializerClass), args)
        }
    }

    private fun createGetKClassExpression(classDescriptor: ClassDescriptor): JsExpression =
            JsInvocation(context.namer().kotlin("getKClass"),
                         getQualifiedClassReferenceName(classDescriptor))


    override fun generateLoad(function: FunctionDescriptor) = generateFunction(function) { jsFun, context ->
        val inputClass = serializerDescriptor.getClassFromSerializationPackage("KInput")
        val serialClassDescRef = JsNameRef(context.getNameForDescriptor(serialDescPropertyDescriptor!!), JsThisRef())

        // var index = -1, readAll = false
        val indexVar = JsNameRef(jsFun.scope.declareFreshName("index"))
        val readAllVar = JsNameRef(jsFun.scope.declareFreshName("readAll"))
        +JsVars(JsVars.JsVar(indexVar.name), JsVars.JsVar(readAllVar.name, JsBooleanLiteral(false)))

        // calculating bit mask vars
        val blocksCnt = orderedProperties.size / 32 + 1
        fun bitMaskOff(i: Int) = i / 32

        // var bitMask0 = 0, bitMask1 = 0...
        val bitMasks = (0 until blocksCnt).map { JsNameRef(jsFun.scope.declareFreshName("bitMask$it")) }
        +JsVars(bitMasks.map { JsVars.JsVar(it.name, JsIntLiteral(0)) }, false)

        // var localProp0, localProp1, ...
        val localProps = orderedProperties.map { JsNameRef(jsFun.scope.declareFreshName(it.name)) }
        +JsVars(localProps.map { JsVars.JsVar(it.name) }, true)

        //input = input.readBegin(...)
        val inputVar = JsNameRef(jsFun.scope.declareFreshName("input"))
        val readBeginF = inputClass.getFuncDesc("readBegin").single()
        val call = JsInvocation(JsNameRef(context.getNameForDescriptor(readBeginF), JsNameRef(jsFun.parameters[0].name)),
                                serialClassDescRef, JsArrayLiteral())
        +JsVars(JsVars.JsVar(inputVar.name, call))

        // while(true) {
        val loop = JsLabel(jsFun.scope.declareFreshName("loopLabel"))
        val loopRef = JsNameRef(loop.name)
        jsWhile(JsBooleanLiteral(true), {
            // index = input.readElement(classDesc)
            val readElementF = context.getNameForDescriptor(inputClass.getFuncDesc("readElement").single())
            +JsAstUtils.assignment(
                    indexVar,
                    JsInvocation(JsNameRef(readElementF, inputVar), serialClassDescRef)
            ).makeStmt()
            // switch(index)
            jsSwitch (indexVar) {
                // -2: readAll = true
                case(JsIntLiteral(-2)) {
                    +JsAstUtils.assignment(
                            readAllVar,
                            JsBooleanLiteral(true)
                    ).makeStmt()
                }
                // all properties
                for ((i, property) in orderedProperties.withIndex()) {
                    case(JsIntLiteral(i)) {
                        // input.readXxxElementValue
                        val sti = getSerialTypeInfo(property)
                        val innerSerial = if (sti.serializer == null) null else serializerInstance(sti.serializer, property.module, property.type)
                        val call = if (innerSerial == null) {
                            val readFunc =
                                    inputClass.getFuncDesc("read${sti.elementMethodPrefix}ElementValue").single()
                                            .let { context.getNameForDescriptor(it) }
                            JsInvocation(JsNameRef(readFunc, inputVar),
                                         serialClassDescRef,
                                         JsIntLiteral(i))
                        }
                        else {
                            val readFunc =
                                    inputClass.getFuncDesc("read${sti.elementMethodPrefix}SerializableElementValue").single()
                                            .let { context.getNameForDescriptor(it) }
                            JsInvocation(JsNameRef(readFunc, inputVar),
                                         serialClassDescRef,
                                         JsIntLiteral(i),
                                         innerSerial)
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
                        if (KotlinBuiltIns.isCharOrNullableChar(property.type) && !shouldBoxReturnValue(property.descriptor.getter)) {
                            +JsAstUtils.assignment(
                                    localProps[i],
                                    Translation.unboxIfNeeded(localProps[i], true)
                            ).makeStmt()
                        }

                        // bitMask[i] |= 1 << x
                        val bitPos = 1 shl (i % 32)
                        +JsBinaryOperation(JsBinaryOperator.ASG_BIT_OR,
                                           bitMasks[i / 32],
                                           JsIntLiteral(bitPos)
                        ).makeStmt()
                        // if (!readAll) break -- but only if this is last prop
                        if (i != orderedProperties.lastIndex)
                            +JsIf(JsAstUtils.not(readAllVar), JsBreak())
                        else {
                            // if (readAll) breakLoop else break
                            +JsIf(readAllVar, JsBreak(loopRef), JsBreak())
                        }
                    }
                }
                // case -1, default: break loop
                case(JsIntLiteral(-1)) {}
                default {
                    +JsBreak(loopRef)
                }
            }
        }, loop)

        // input.readEnd(desc)
        val readEndF = inputClass.getFuncDesc("readEnd").single()
                .let { context.getNameForDescriptor(it) }
        +JsInvocation(
                JsNameRef(readEndF, inputVar),
                serialClassDescRef
        ).makeStmt()

        // deserialization constructor call
        val constrDesc = KSerializerDescriptorResolver.createLoadConstructorDescriptor(serializableDescriptor, context.bindingContext())
        val constrRef = context.getInnerNameForDescriptor(constrDesc).makeRef()
        val args: MutableList<JsExpression> = mutableListOf(bitMasks[0])
        args += localProps
        args += JsNullLiteral()
        +JsReturn(JsInvocation(constrRef, args))
    }

    companion object {
        fun translate(declaration: KtPureClassOrObject, descriptor: ClassDescriptor, translator: DeclarationBodyVisitor, context: TranslationContext) {
            if (getSerializableClassDescriptorBySerializer(descriptor) != null)
                SerializerJsTranslator(declaration, translator, context).generate()
        }
    }
}