/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.utils

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.coroutinesIntrinsicsPackageFqName
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.intrinsic.functions.basic.FunctionIntrinsicWithReceiverComputed
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils.simpleReturnFunction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOrInheritsParametersWithDefaultValue
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

fun generateDelegateCall(
    classDescriptor: ClassDescriptor,
    fromDescriptor: FunctionDescriptor,
    toDescriptor: FunctionDescriptor,
    thisObject: JsExpression,
    context: TranslationContext,
    detectDefaultParameters: Boolean,
    source: PsiElement?
): JsStatement {
    fun FunctionDescriptor.getNameForFunctionWithPossibleDefaultParam() =
        if (detectDefaultParameters && hasOrInheritsParametersWithDefaultValue()) {
            context.scope().declareName(context.getNameForDescriptor(this).ident + Namer.DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX)
        } else {
            context.getNameForDescriptor(this)
        }

    val overriddenMemberFunctionName = toDescriptor.getNameForFunctionWithPossibleDefaultParam()
    val overriddenMemberFunctionRef = JsNameRef(overriddenMemberFunctionName, thisObject)

    val parameters = SmartList<JsParameter>()
    val args = SmartList<JsExpression>()

    if (DescriptorUtils.isExtension(fromDescriptor)) {
        val extensionFunctionReceiverName = JsScope.declareTemporaryName(Namer.getReceiverParameterName())
        parameters.add(JsParameter(extensionFunctionReceiverName))
        args.add(JsNameRef(extensionFunctionReceiverName))
    }

    for (param in fromDescriptor.valueParameters) {
        val paramName = param.name.asString()
        val jsParamName = JsScope.declareTemporaryName(paramName)
        parameters.add(JsParameter(jsParamName))
        args.add(JsNameRef(jsParamName))
    }

    val intrinsic = context.intrinsics().getFunctionIntrinsic(toDescriptor, context)
    val invocation = if (intrinsic is FunctionIntrinsicWithReceiverComputed) {
        intrinsic.apply(thisObject, args, context)
    } else {
        JsInvocation(overriddenMemberFunctionRef, args)
    }

    invocation.source = source

    val functionObject = simpleReturnFunction(context.scope(), invocation)
    functionObject.source = source?.finalElement
    functionObject.parameters.addAll(parameters)

    val fromFunctionName = fromDescriptor.getNameForFunctionWithPossibleDefaultParam()

    val prototypeRef = JsAstUtils.prototypeOf(context.getInnerReference(classDescriptor))
    val functionRef = JsNameRef(fromFunctionName, prototypeRef)
    return JsAstUtils.assignment(functionRef, functionObject).makeStmt()
}

fun <T, S> List<T>.splitToRanges(classifier: (T) -> S): List<Pair<List<T>, S>> {
    if (isEmpty()) return emptyList()

    var lastIndex = 0
    var lastClass: S = classifier(this[0])
    val result = mutableListOf<Pair<List<T>, S>>()

    for ((index, e) in asSequence().withIndex().drop(1)) {
        val cls = classifier(e)
        if (cls != lastClass) {
            result += Pair(subList(lastIndex, index), lastClass)
            lastClass = cls
            lastIndex = index
        }
    }

    result += Pair(subList(lastIndex, size), lastClass)
    return result
}

fun getReferenceToJsClass(type: KotlinType, context: TranslationContext): JsExpression {
    val classifierDescriptor = type.constructor.declarationDescriptor

    return when (classifierDescriptor) {
        is ClassDescriptor -> {
            ReferenceTranslator.translateAsTypeReference(classifierDescriptor, context)
        }
        is TypeParameterDescriptor -> {
            assert(classifierDescriptor.isReified)

            context.usageTracker()?.used(classifierDescriptor)

            context.captureTypeIfNeedAndGetCapturedName(classifierDescriptor)
                    ?: context.getNameForDescriptor(classifierDescriptor).makeRef()
        }
        else -> {
            throw IllegalStateException("Can't get reference for $type")
        }
    }
}

fun TranslationContext.addFunctionToPrototype(
    classDescriptor: ClassDescriptor,
    descriptor: FunctionDescriptor,
    function: JsExpression
): JsStatement {
    val prototypeRef = JsAstUtils.prototypeOf(getInnerReference(classDescriptor))
    val functionRef = JsNameRef(getNameForDescriptor(descriptor), prototypeRef)
    return JsAstUtils.assignment(functionRef, function).makeStmt()
}

fun TranslationContext.addAccessorsToPrototype(
    containingClass: ClassDescriptor,
    propertyDescriptor: PropertyDescriptor,
    literal: JsObjectLiteral
) {
    val prototypeRef = JsAstUtils.prototypeOf(getInnerReference(containingClass))
    val propertyName = getNameForDescriptor(propertyDescriptor)
    val defineProperty = JsAstUtils.defineProperty(prototypeRef, propertyName.ident, literal)
    addDeclarationStatement(defineProperty.makeStmt())
}

fun JsFunction.fillCoroutineMetadata(
    context: TranslationContext,
    descriptor: FunctionDescriptor,
    hasController: Boolean
) {
    val suspendPropertyDescriptor = context.currentModule.getPackage(context.languageVersionSettings.coroutinesIntrinsicsPackageFqName())
        .memberScope
        .getContributedVariables(COROUTINE_SUSPENDED_NAME, NoLookupLocation.FROM_BACKEND).first()

    val coroutineBaseClassRef = ReferenceTranslator.translateAsTypeReference(TranslationUtils.getCoroutineBaseClass(context), context)

    fun getCoroutinePropertyName(id: String) =
        context.getNameForDescriptor(TranslationUtils.getCoroutineProperty(context, id))

    coroutineMetadata = CoroutineMetadata(
        doResumeName = context.getNameForDescriptor(TranslationUtils.getCoroutineDoResumeFunction(context)),
        suspendObjectRef = ReferenceTranslator.translateAsValueReference(suspendPropertyDescriptor, context),
        baseClassRef = coroutineBaseClassRef,
        stateName = getCoroutinePropertyName("state"),
        exceptionStateName = getCoroutinePropertyName("exceptionState"),
        finallyPathName = getCoroutinePropertyName("finallyPath"),
        resultName = getCoroutinePropertyName("result"),
        exceptionName = getCoroutinePropertyName("exception"),
        hasController = hasController,
        hasReceiver = descriptor.dispatchReceiverParameter != null,
        psiElement = descriptor.source.getPsi()
    )
}

fun definePackageAlias(name: String, varName: JsName, tag: String, parentRef: JsExpression): JsStatement {
    val selfRef = JsNameRef(name, parentRef)
    val rhs = JsAstUtils.or(selfRef, JsAstUtils.assignment(selfRef.deepCopy(), JsObjectLiteral(false)))

    return JsAstUtils.newVar(varName, rhs).apply { exportedPackage = tag }
}

val PsiElement.finalElement: PsiElement
    get() = when (this) {
        is KtFunctionLiteral -> rBrace ?: this
        is KtDeclarationWithBody -> (bodyExpression as? KtBlockExpression)?.rBrace ?: bodyExpression ?: this
        is KtLambdaExpression -> bodyExpression?.rBrace ?: this
        else -> this
    }

fun TranslationContext.addFunctionButNotExport(descriptor: FunctionDescriptor, expression: JsExpression): JsName =
    addFunctionButNotExport(getInnerNameForDescriptor(descriptor), expression)

fun TranslationContext.addFunctionButNotExport(name: JsName, expression: JsExpression): JsName {
    when (expression) {
        is JsFunction -> {
            expression.name = name
            addDeclarationStatement(expression.makeStmt())
        }
        else -> {
            addDeclarationStatement(JsAstUtils.newVar(name, expression))
        }
    }
    return name
}

fun createPrototypeStatements(superName: JsName, name: JsName): List<JsStatement> {
    val superclassRef = superName.makeRef()
    val superPrototype = JsAstUtils.prototypeOf(superclassRef)
    val superPrototypeInstance = JsInvocation(JsNameRef("create", "Object"), superPrototype)

    val classRef = name.makeRef()
    val prototype = JsAstUtils.prototypeOf(classRef)
    val prototypeStatement = JsAstUtils.assignment(prototype, superPrototypeInstance).makeStmt()

    val constructorRef = JsNameRef("constructor", prototype.deepCopy())
    val constructorStatement = JsAstUtils.assignment(constructorRef, classRef.deepCopy()).makeStmt()

    return listOf(prototypeStatement, constructorStatement)
}

fun TranslationContext.createCoroutineResult(resolvedCall: ResolvedCall<*>): JsExpression {
    val callElement = resolvedCall.call.callElement
    val coroutineRef = TranslationUtils.translateContinuationArgument(this).source(callElement)
    return JsNameRef("\$\$coroutineResult\$\$", coroutineRef).apply {
        sideEffects = SideEffectKind.DEPENDS_ON_STATE
        source = callElement
        coroutineResult = true
        synthetic = true
    }
}

/**
 * Tries to get precise statically known primitive type. Takes generic supertypes into account. Doesn't handle smart-casts.
 * This is needed to be compatible with JVM NaN behaviour:
 *
 * // Generics with Double super-type
 * fun <T: Double> foo(v: T) = println(v == v)
 * foo(Double.NaN) // false
 *
 * Also see org/jetbrains/kotlin/codegen/codegenUtil.kt#calcTypeForIEEE754ArithmeticIfNeeded
 */
fun TranslationContext.getPrecisePrimitiveType(expression: KtExpression): KotlinType? {
    val bindingContext = bindingContext()
    val ktType = bindingContext.getType(expression) ?: return null

    return TypeUtils.getAllSupertypes(ktType).find(KotlinBuiltIns::isPrimitiveTypeOrNullablePrimitiveType) ?: ktType
}

fun TranslationContext.getPrecisePrimitiveTypeNotNull(expression: KtExpression): KotlinType {
    return getPrecisePrimitiveType(expression) ?: throw IllegalStateException("Type must be not null for " + expression)
}

fun TranslationContext.getPrimitiveNumericComparisonInfo(expression: KtExpression) =
    config.configuration.languageVersionSettings.let {
        if (it.supportsFeature(LanguageFeature.ProperIeee754Comparisons)) {
            bindingContext().get(BindingContext.PRIMITIVE_NUMERIC_COMPARISON_INFO, expression)
        } else {
            null
        }
}