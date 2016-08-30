package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.cfg.pseudocode.getSubtypesPredicate
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.*
import java.rmi.UnexpectedException
import kotlin.comparisons.compareBy


abstract class BlockCodegen(val state: TranslationState, val variableManager: VariableManager, val codeBuilder: LLVMBuilder) {

    val topLevel = 2
    var returnType: LLVMVariable? = null
    var wasReturnOnTopLevel = false

    fun evaluateCodeBlock(expr: PsiElement?, startLabel: LLVMLabel? = null, nextIterationLabel: LLVMLabel? = null, breakLabel: LLVMLabel? = null, scopeDepth: Int = 0, isBlock: Boolean = true) {
        codeBuilder.markWithLabel(startLabel)
        if (isBlock) {
            expressionWalker(expr, breakLabel, scopeDepth)
        } else {
            var result = evaluateExpression(expr, scopeDepth) ?: throw UnexpectedException(expr!!.text)
            when (result) {
                is LLVMVariable -> {
                    if (result.pointer == 1 && result.type !is LLVMReferenceType) {
                        result = codeBuilder.loadAndGetVariable(result)
                    }
                    if (result.type is LLVMReferenceType) {
                        generateReferenceReturn(result)
                    } else {
                        codeBuilder.addReturnOperator(result)
                    }
                }
                else -> codeBuilder.addAnyReturn(result.type!!, result.toString())
            }

            wasReturnOnTopLevel = true
        }
        codeBuilder.addUnconditionalJump(nextIterationLabel ?: return)
    }

    private fun expressionWalker(expr: PsiElement?, breakLabel: LLVMLabel?, scopeDepth: Int) {
        when (expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild, breakLabel, scopeDepth + 1)
            is KtProperty -> evaluateValExpression(expr, scopeDepth)
            is KtPostfixExpression -> evaluatePostfixExpression(expr, scopeDepth)
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtCallExpression -> evaluateCallExpression(expr, scopeDepth)
            is KtDoWhileExpression -> evaluateDoWhileExpression(expr.firstChild, scopeDepth + 1)
            is KtBreakExpression -> evaluateBreakExpression(breakLabel!!)
            is KtDotQualifiedExpression -> evaluateDotExpression(expr, scopeDepth)
            is KtWhenExpression -> evaluateWhenExpression(expr, scopeDepth)
            is PsiElement -> evaluateExpression(expr.firstChild, scopeDepth + 1)
            null -> {
                variableManager.pullUpwardsLevel(scopeDepth)
                return
            }
            else -> UnsupportedOperationException()
        }

        expressionWalker(expr.getNextSiblingIgnoringWhitespaceAndComments(), breakLabel, scopeDepth)
    }

    private fun evaluateBreakExpression(breakLabel: LLVMLabel) {
        codeBuilder.addUnconditionalJump(breakLabel)
    }

    private fun evaluateDoWhileExpression(element: PsiElement, scopeDepth: Int) {
        val expr = element.context as KtDoWhileExpression
        executeWhileBlock(expr.condition!!, expr.body!!, scopeDepth, checkConditionBeforeExecute = false)
    }

    fun evaluateExpression(expr: PsiElement?, scopeDepth: Int): LLVMSingleValue? {
        return when (expr) {
            is KtBlockExpression -> {
                expressionWalker(expr.firstChild, breakLabel = null, scopeDepth = scopeDepth + 1)
                return null
            }
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtPostfixExpression -> evaluatePostfixExpression(expr, scopeDepth)
            is KtPrefixExpression -> evaluatePrefixExpression(expr, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            is KtCallExpression -> evaluateCallExpression(expr, scopeDepth)
            is KtWhenExpression -> evaluateWhenExpression(expr, scopeDepth)
            is KtCallableReferenceExpression -> evaluateCallableReferenceExpression(expr)
            is KtDotQualifiedExpression -> evaluateDotExpression(expr, scopeDepth)
            is KtReferenceExpression -> evaluateReferenceExpression(expr, scopeDepth)
            is KtIfExpression -> evaluateIfOperator(expr, scopeDepth + 1)
            is KtStringTemplateExpression -> evaluateStringTemplateExpression(expr)
            is KtReturnExpression -> evaluateReturnInstruction(expr.firstChild, scopeDepth)
            is KtThisExpression -> evaluateThisExpression()
            is KtSafeQualifiedExpression -> evaluateSafeAccessExpression(expr, scopeDepth)
            is KtParenthesizedExpression -> evaluateExpression(expr.expression, scopeDepth)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr, scopeDepth)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    fun evaluateConstructorDelegationReferenceExpression(expr: KtConstructorDelegationReferenceExpression, structCodegen: StructCodegen, constructorArguments: List<LLVMVariable>, scopeDepth: Int): LLVMSingleValue? {
        val targetCall = state.bindingContext.get(BindingContext.CALL, expr)
        val names = parseValueArguments(targetCall!!.valueArguments, scopeDepth)
        val args = codeBuilder.loadArgsIfRequired(names, constructorArguments)
        return evaluateConstructorCallExpression(LLVMVariable(structCodegen.fullName + LLVMType.mangleFunctionArguments(names), structCodegen.type, scope = LLVMVariableScope()), args)
    }

    private fun evaluateThisExpression(): LLVMSingleValue? =
            variableManager["this"]

    fun evaluateStringTemplateExpression(expr: KtStringTemplateExpression): LLVMSingleValue? {
        val receiveValue = state.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expr)
        val type = (receiveValue as TypedCompileTimeConstant).type
        val value = receiveValue.getValue(type) ?: return null
        val variable = variableManager.receiveVariable(".str" + LLVMBuilder.UniqueGenerator.generateUniqueString(), LLVMStringType(value.toString().length, isLoaded = false), LLVMVariableScope(), pointer = 0)

        codeBuilder.addStringConstant(variable, value.toString())
        return variable
    }

    private fun evaluateCallableReferenceExpression(expr: KtCallableReferenceExpression): LLVMSingleValue? {
        val kotlinType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        val result = LLVMInstanceOfStandardType(expr.text.substring(2), kotlinType, LLVMVariableScope(), state)
        return LLVMVariable("${result.label}${(result.type as LLVMFunctionType).mangleArgs()}", result.type, result.kotlinName, result.scope, result.pointer)
    }

    private fun evaluateSafeAccessExpression(expr: KtSafeQualifiedExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiver = expr.receiverExpression
        val selector = expr.selectorExpression

        val left = evaluateExpression(receiver, scopeDepth)!!
        val loadedLeft = codeBuilder.receiveNativeValue(left)
        val expectedType = LLVMMapStandardType(state.bindingContext.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expr)!!, state) as LLVMReferenceType

        if (state.classes.containsKey(expectedType.type)) {
            expectedType.prefix = "class"
        }

        val result = codeBuilder.getNewVariable(expectedType, pointer = 2)
        codeBuilder.allocStaticVar(result, pointer = true)

        val condition = left.type!!.operatorEq(loadedLeft, LLVMVariable("", LLVMNullType()))
        val thenLabel = codeBuilder.getNewLabel(prefix = "safe.access")
        val elseLabel = codeBuilder.getNewLabel(prefix = "safe.access")
        val endLabel = codeBuilder.getNewLabel(prefix = "safe.access")

        val conditionResult = codeBuilder.getNewVariable(condition.variableType)
        codeBuilder.addAssignment(conditionResult, condition)
        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)

        codeBuilder.markWithLabel(thenLabel)
        codeBuilder.storeNull(result)
        codeBuilder.addUnconditionalJump(endLabel)

        codeBuilder.markWithLabel(elseLabel)
        val right = evaluateDotBody(receiver, selector!!, scopeDepth)
        val rightLoaded = codeBuilder.loadAndGetVariable(right as LLVMVariable)
        codeBuilder.storeVariable(result, rightLoaded)
        codeBuilder.addUnconditionalJump(endLabel)

        codeBuilder.markWithLabel(endLabel)

        return result
    }

    private fun evaluateDotExpression(expr: KtDotQualifiedExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiverExpr = expr.receiverExpression
        val selectorExpr = expr.selectorExpression!!

        return evaluateDotBody(receiverExpr, selectorExpr, scopeDepth)
    }

    private fun evaluateDotBody(receiverExpr: KtExpression, selectorExpr: KtExpression, scopeDepth: Int): LLVMSingleValue? {
        var receiverName = receiverExpr.text
        var receiver = when (receiverExpr) {
            is KtCallExpression,
            is KtPrefixExpression,
            is KtPostfixExpression,
            is KtBinaryExpression -> evaluateExpression(receiverExpr, scopeDepth) as LLVMVariable
            is KtDotQualifiedExpression -> {
                val codegen = resolveCodegenByName(receiverName, receiverName.split("."))
                if (codegen != null) null else evaluateExpression(receiverExpr, scopeDepth) as LLVMVariable
            }
            is KtNameReferenceExpression -> {
                val referenceContext = state.bindingContext.get(BindingContext.REFERENCE_TARGET, receiverExpr)
                receiverName = referenceContext!!.fqNameSafe.asString()
                when (referenceContext) {
                    is PropertyDescriptorImpl -> {
                        val receiverThis = variableManager["this"]!!
                        evaluateMemberMethodOrField(receiverThis, receiverName, topLevel, call = null)!! as LLVMVariable
                    }
                    else -> variableManager[receiverName]
                }
            }
            else -> variableManager[receiverName]
        }

        val isExtension = selectorExpr is KtCallExpression && selectorExpr.getFunctionResolvedCallWithAssert(state.bindingContext).extensionReceiver != null

        if (isExtension) {
            return evaluateExtensionExpression(receiverExpr, receiver, selectorExpr as KtCallExpression, scopeDepth)
        }

        val selectorName = when (selectorExpr) {
            is KtNameReferenceExpression -> state.bindingContext.get(BindingContext.REFERENCE_TARGET, selectorExpr)?.fqNameSafe?.asString() ?: selectorExpr.text
            else -> selectorExpr.text
        }

        if (receiver != null) {
            if (receiver.pointer == 2) {
                receiver = codeBuilder.loadAndGetVariable(receiver)
            }
            when (receiver.type) {
                is LLVMReferenceType -> return evaluateMemberMethodOrField(receiver, selectorName, scopeDepth, selectorExpr)
                else -> return evaluateExtensionExpression(receiverExpr, receiver, selectorExpr as KtCallExpression, scopeDepth)
            }
        }

        val clazz = resolveCodegen(receiverExpr)
                ?: return evaluateExtensionExpression(receiverExpr, receiver, selectorExpr as? KtCallExpression
                ?: throw UnexpectedException(selectorExpr.text), scopeDepth)
        return evaluateClassScopedDotExpression(clazz, selectorExpr, scopeDepth, receiver)
    }

    private fun evaluateExtensionExpression(receiver: KtExpression, receiverExpressionArgument: LLVMVariable?, selector: KtCallExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiverType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, receiver)
        val standardType = LLVMMapStandardType(receiverType!!.type!!, state)

        val targetFunction = state.bindingContext.get(BindingContext.CALL, selector.calleeExpression)
        val candidateDescriptor = state.bindingContext.get(BindingContext.RESOLVED_CALL, targetFunction)!!.candidateDescriptor
        val targetFunctionName = candidateDescriptor.fqNameSafe.convertToNativeName()
        val nameWithoutMangling = candidateDescriptor.name.asString().replace(Regex("""(.?)<init>"""), "")
        val packageNameFirst = targetFunction?.calleeExpression?.getContainingKtFile()?.packageFqName?.convertToNativeName() ?: ""
        val packageNameSecond = candidateDescriptor.containingDeclaration.fqNameSafe.convertToNativeName()

        val names = parseArgList(selector, scopeDepth)
        val type = LLVMType.mangleFunctionArguments(names)

        val constructedFunctionName = standardType.mangle() + nameWithoutMangling.addBeforeIfNotEmpty(".") + type
        val targetExtension = state.extensionFunctions[standardType.toString()]

        val extensionCodegen = targetExtension?.get(packageNameFirst.addAfterIfNotEmpty(".") + constructedFunctionName) ?:
                targetExtension?.get(packageNameSecond.addAfterIfNotEmpty(".") + constructedFunctionName) ?:
                targetExtension?.get(targetFunctionName)
                ?: throw UnexpectedException(constructedFunctionName)
        val receiverExpression = receiverExpressionArgument ?: evaluateExpression(receiver, scopeDepth + 1)!!

        val typeThisArgument = LLVMVariable("type", standardType, pointer = if (standardType is LLVMReferenceType) 1 else 0)

        val args = mutableListOf(codeBuilder.loadOneArgumentIfRequired(receiverExpression, typeThisArgument))
        args.addAll(codeBuilder.loadArgsIfRequired(names, extensionCodegen.args))
        return evaluateFunctionCallExpression(LLVMVariable(extensionCodegen.fullName, extensionCodegen.returnType!!.type, scope = LLVMVariableScope()), args)
    }

    private fun evaluateClassScopedDotExpression(clazz: StructCodegen, selector: KtExpression, scopeDepth: Int, receiver: LLVMVariable? = null): LLVMSingleValue? =
            when (selector) {
                is KtCallExpression -> evaluateCallExpression(selector, scopeDepth, clazz, caller = receiver)
                is KtReferenceExpression -> evaluateReferenceExpression(selector, scopeDepth, clazz)
                else -> throw UnsupportedOperationException()
            }

    fun evaluateMemberMethodOrField(receiver: LLVMVariable, selectorName: String, scopeDepth: Int, call: PsiElement? = null): LLVMSingleValue? {
        val type = receiver.type as LLVMReferenceType
        val clazz = resolveClassOrObjectLocation(type) ?: throw UnexpectedException(type.toString() + receiver.toString())
        val field = clazz.fieldsIndex[selectorName]

        if (field != null) {
            val result = codeBuilder.getNewVariable(field.type, pointer = field.pointer + 1)
            codeBuilder.loadClassField(result, receiver, field.offset)
            return result
        }

        (call as? KtCallExpression) ?: throw UnexpectedException("$receiver:$selectorName")
        val resolvedCall = (call as KtCallExpression).getCall(state.bindingContext)!!.getResolvedCallWithAssert(state.bindingContext)
        val functionDescriptor = resolvedCall.candidateDescriptor
        val functionArguments = functionDescriptor.valueParameters.map { it -> it.type }.map { LLVMMapStandardType(it, state) }
        val methodName = functionDescriptor.fqNameSafe.asString() + LLVMType.mangleFunctionTypes(functionArguments)

        val method = clazz.methods[methodName] ?: throw UnexpectedException(methodName)
        val returnType = method.returnType!!.type

        val arguments = resolvedCall.valueArguments.toSortedMap(compareBy { it.index }).values
        val names = parseNamedValueArguments(arguments, method.defaultValues, scopeDepth)
        val loadedArgs = codeBuilder.loadArgsIfRequired(names, method.args)

        val callArgs = mutableListOf<LLVMSingleValue>(receiver)
        callArgs.addAll(loadedArgs)

        return evaluateFunctionCallExpression(LLVMVariable(methodName, returnType, scope = LLVMVariableScope()), callArgs)
    }

    private fun resolveClassOrObjectLocation(type: LLVMReferenceType): StructCodegen? {
        if (type.location.size == 0) {
            return state.classes[type.type] ?: state.objects[type.type]
        }

        var codegen = state.classes[type.location[0]]
        var i = 1
        while (i < type.location.size) {
            codegen = codegen?.nestedClasses?.get(type.location[i])
            i++
        }

        if (codegen?.companionObjectCodegen != null && type.type == codegen?.companionObjectCodegen?.structName) {
            return codegen?.companionObjectCodegen!!
        }

        return codegen?.nestedClasses?.get(type.type)
    }

    fun evaluateArrayAccessExpression(expr: KtArrayAccessExpression, scope: Int): LLVMSingleValue? {
        val arrayNameVariable = evaluateExpression(expr.arrayExpression, scope) as LLVMVariable
        return when (arrayNameVariable.type) {
            is LLVMReferenceType -> {
                val callMaker = state.bindingContext.get(BindingContext.CALL, expr)
                when (callMaker!!.callType) {
                    Call.CallType.ARRAY_SET_METHOD,
                    Call.CallType.ARRAY_GET_METHOD -> {
                        val arrayActionType = if (callMaker.callType == Call.CallType.ARRAY_SET_METHOD) "set" else "get"
                        val explicitReceiver = callMaker.explicitReceiver as ExpressionReceiver
                        val receiver = evaluateExpression(explicitReceiver.expression, scope)!! as LLVMVariable
                        val pureReceiver = codeBuilder.downLoadArgument(receiver, 1)

                        val targetClassName = (receiver.type as LLVMReferenceType).type

                        val names = parseValueArguments(callMaker.valueArguments, scope)
                        val methodName = "$targetClassName.$arrayActionType${LLVMType.mangleFunctionArguments(names)}"
                        val type = receiver.type as LLVMReferenceType
                        val clazz = resolveClassOrObjectLocation(type) ?: throw UnexpectedException(type.toString())

                        val method = clazz.methods[methodName] ?: throw UnexpectedException(expr.text)
                        val returnType = clazz.methods[methodName]!!.returnType!!.type

                        val loadedArgs = codeBuilder.loadArgsIfRequired(names, method.args)
                        val callArgs = mutableListOf(pureReceiver)
                        callArgs.addAll(loadedArgs)

                        return evaluateFunctionCallExpression(LLVMVariable(methodName, returnType, scope = LLVMVariableScope()), callArgs)
                    }
                    else -> throw IllegalStateException("Unknown array access method")
                }
            }
            else -> {
                val arrayIndex = evaluateConstantExpression(expr.indexExpressions.first() as KtConstantExpression)
                val arrayReceivedVariable = codeBuilder.loadAndGetVariable(arrayNameVariable)
                val arrayElementType = (arrayNameVariable.type as LLVMArray).basicType()
                val indexVariable = codeBuilder.getNewVariable(arrayElementType, pointer = 1)
                codeBuilder.loadVariableOffset(indexVariable, arrayReceivedVariable, arrayIndex)
                indexVariable
            }
        }
    }

    private fun evaluateReferenceExpression(expr: KtReferenceExpression, scopeDepth: Int, classScope: StructCodegen? = null): LLVMSingleValue? {
        val targetName = state.bindingContext.get(BindingContext.REFERENCE_TARGET, expr)?.fqNameSafe?.convertToNativeName()
        return when {
            expr is KtArrayAccessExpression -> evaluateArrayAccessExpression(expr, scopeDepth + 1)
            isEnumClassField(expr, classScope) -> resolveEnumClassField(expr, classScope)
            (targetName != null) && (variableManager[targetName] != null) -> variableManager[targetName]
            ((expr is KtNameReferenceExpression) && (classScope != null)) -> evaluateNameReferenceExpression(expr, classScope.parentCodegen!!)
            else -> {
                val clazz = classScope ?: resolveCodegen(expr)
                val receiver = if (clazz != null) variableManager[clazz.fullName] ?: variableManager["this"] else variableManager["this"]
                targetName ?: throw RuntimeException(expr.firstChild.text)
                evaluateMemberMethodOrField(receiver ?: throw UnexpectedException(targetName), targetName, topLevel)
            }
        }
    }


    private fun evaluateNameReferenceExpression(expr: KtNameReferenceExpression, classScope: StructCodegen): LLVMSingleValue? {
        val fieldName = state.bindingContext.get(BindingContext.REFERENCE_TARGET, expr)!!.name.toString()
        val companionObject = (classScope as ClassCodegen).companionObjectCodegen ?: throw UnexpectedException(expr.text)
        val field = companionObject.fieldsIndex[fieldName] ?: return null
        val receiver = variableManager[companionObject.fullName]!!
        val result = codeBuilder.getNewVariable(field.type, pointer = 1)

        codeBuilder.loadClassField(result, receiver, field.offset)
        return result
    }

    private fun resolveEnumClassField(expr: KtReferenceExpression, classScope: StructCodegen?): LLVMSingleValue =
            (classScope ?: resolveCodegen(expr))!!.enumFields[expr.text]!!

    private fun isEnumClassField(expr: KtReferenceExpression, classScope: StructCodegen?): Boolean =
            (classScope ?: resolveCodegen(expr))?.enumFields?.containsKey(expr.text) ?: false

    private fun resolveCodegen(expr: KtExpression): StructCodegen? {
        val type = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, expr)?.type
                ?: expr.getType(state.bindingContext)
                ?: expr.getQualifiedExpressionForReceiver()?.getType(state.bindingContext)

        val location = type?.getSubtypesPredicate()?.toString()?.split(".")
                ?: state.bindingContext.get(BindingContext.REFERENCE_TARGET, expr as KtReferenceExpression)?.fqNameSafe?.toString()?.split(".")
                ?: return null

        val name = type?.constructor?.declarationDescriptor?.fqNameSafe?.asString() ?: throw UnexpectedException(expr.text)

        return resolveCodegenByName(name, location)

    }

    private fun resolveCodegenByName(name: String, location: List<String>): StructCodegen? {
        val classType = LLVMReferenceType(name, prefix = "class")
        var locationPrefix = ""
        for (currentLocation in location.dropLast(1)) {
            locationPrefix += (if (locationPrefix.length > 0) "." else "") + currentLocation
            classType.location.add(locationPrefix)
        }

        return resolveClassOrObjectLocation(classType)
    }

    private fun evaluateCallExpression(expr: KtCallExpression, scopeDepth: Int, classScope: StructCodegen? = null, caller: LLVMVariable? = null): LLVMSingleValue? {
        var names = parseArgList(expr, scopeDepth)
        val targetFunction = state.bindingContext.get(BindingContext.CALL, expr.calleeExpression)
        var resolvedCall = state.bindingContext.get(BindingContext.RESOLVED_CALL, targetFunction)
        if (resolvedCall is VariableAsFunctionResolvedCallImpl) {
            resolvedCall = resolvedCall.variableCall
        }

        val targetFunctionName = resolvedCall!!.candidateDescriptor.fqNameSafe.convertToNativeName()
        val functionDescriptor = expr.getFunctionResolvedCallWithAssert(state.bindingContext).candidateDescriptor
        val arguments = resolvedCall.valueArguments.toSortedMap(compareBy { it.index }).values

        val external = state.externalFunctions.containsKey(targetFunctionName)
        val functionArguments = functionDescriptor.valueParameters.map { it -> it.type }.map { LLVMMapStandardType(it, state) }
        val function = "$targetFunctionName${if (!external) LLVMType.mangleFunctionTypes(functionArguments) else ""}"


        if (state.functions.containsKey(function) || state.externalFunctions.containsKey(function)) {
            val descriptor = state.functions[function] ?: state.externalFunctions[function] ?: return null
            names = parseNamedValueArguments(arguments, descriptor.defaultValues, scopeDepth)
            val args = codeBuilder.loadArgsIfRequired(names, descriptor.args)
            return evaluateFunctionCallExpression(LLVMVariable(function, descriptor.returnType!!.type, scope = LLVMVariableScope()), args)
        }

        if (state.classes.containsKey(targetFunctionName) || classScope?.structName == targetFunctionName) {
            val descriptor = state.classes[targetFunctionName] ?: classScope ?: return null
            val detectedConstructor = LLVMType.mangleFunctionTypes(functionArguments)
            val args = codeBuilder.loadArgsIfRequired(names, descriptor.constructorFields[detectedConstructor]!!)
            return evaluateConstructorCallExpression(LLVMVariable(descriptor.fullName + detectedConstructor, descriptor.type, scope = LLVMVariableScope()), args)
        }

        val localFunction = variableManager[targetFunctionName]
        if (localFunction != null) {
            val type = localFunction.type as LLVMFunctionType
            val args = codeBuilder.loadArgsIfRequired(names, type.arguments)
            return evaluateFunctionCallExpression(LLVMVariable(targetFunctionName, type.returnType.type, scope = LLVMRegisterScope()), args)
        }

        if (classScope != null) {
            val methodShortName = function
            if (classScope.methods.containsKey(methodShortName)) {
                val descriptor = classScope.methods[methodShortName]!!
                val parentDescriptor = descriptor.parentCodegen!!
                val receiver = variableManager[parentDescriptor.fullName] ?: throw UnexpectedException(parentDescriptor.fullName)
                val methodFullName = descriptor.name
                val returnType = descriptor.returnType!!.type
                val loadedArgs = codeBuilder.loadArgsIfRequired(names, descriptor.args)
                val callArgs = mutableListOf<LLVMSingleValue>(receiver)
                callArgs.addAll(loadedArgs)

                return evaluateFunctionCallExpression(LLVMVariable(methodFullName, returnType, scope = LLVMVariableScope()), callArgs)
            }
        }

        val nestedConstructor = classScope?.nestedClasses?.get(expr.calleeExpression!!.text)
        if (nestedConstructor != null) {
            val args = codeBuilder.loadArgsIfRequired(names, nestedConstructor.constructorFields[nestedConstructor.primaryConstructorIndex]!!)
            return evaluateConstructorCallExpression(LLVMVariable(nestedConstructor.fullName, nestedConstructor.type, scope = LLVMVariableScope()), args)
        }

        val containingClass = resolveContainingClass(expr) ?: return null
        val method = containingClass.methods[function] ?: throw RuntimeException("Cannot find function $targetFunctionName")
        val args = mutableListOf<LLVMSingleValue>()

        if (caller != null) {
            args.add(caller)
        } else if (variableManager[containingClass.fullName] != null) {
            args.add(variableManager[containingClass.fullName]!!)
        } else {
            args.add(variableManager["this"]!!)
        }

        args.addAll(codeBuilder.loadArgsIfRequired(names, method.args))

        return evaluateFunctionCallExpression(LLVMVariable(method.fullName, method.returnType?.type ?: LLVMVoidType(), scope = LLVMVariableScope()), args)
    }

    private fun resolveContainingClass(expr: KtElement): StructCodegen? {
        val name = expr.getResolvedCallWithAssert(state.bindingContext).dispatchReceiver?.type?.constructor?.declarationDescriptor?.fqNameSafe?.asString() ?: return null

        var currentResolvedClassName = ""
        var codegen: StructCodegen? = null
        for (classPart in name.split(".")) {
            currentResolvedClassName += (if (currentResolvedClassName.length > 0) "." else "") + classPart
            codegen = (codegen?.nestedClasses ?: state.classes)[currentResolvedClassName]
                    ?: (if ((codegen as? ClassCodegen)?.companionObjectCodegen?.structName == currentResolvedClassName) (codegen as? ClassCodegen)?.companionObjectCodegen else null)
                    ?: state.objects[currentResolvedClassName]
                    ?: codegen
        }

        return codegen
    }

    private fun evaluateFunctionCallExpression(function: LLVMVariable, names: List<LLVMSingleValue>): LLVMSingleValue? {
        val returnType = function.type
        when (returnType) {
            is LLVMVoidType -> {
                codeBuilder.addLLVMCodeToLocalPlace(LLVMCall(LLVMVoidType(), function.toString(), names).toString())
            }
            is LLVMReferenceType -> {
                val returnVar = codeBuilder.getNewVariable(returnType, pointer = 2)
                codeBuilder.allocStaticVar(returnVar, pointer = true)

                val args = mutableListOf<LLVMSingleValue>(returnVar)
                args.addAll(names)

                codeBuilder.addLLVMCodeToLocalPlace(LLVMCall(LLVMVoidType(), function.toString(), args).toString())
                if (returnVar.pointer == 2) {
                    return returnVar
                } else {
                    return codeBuilder.loadAndGetVariable(returnVar)
                }
            }
            else -> {
                val result = codeBuilder.getNewVariable(returnType)
                codeBuilder.addAssignment(result, LLVMCall(returnType, function.toString(), names))

                val resultPtr = codeBuilder.getNewVariable(returnType, pointer = 1)
                codeBuilder.allocStackVar(resultPtr, pointer = true)
                codeBuilder.storeVariable(resultPtr, result)
                return resultPtr
            }
        }

        return null
    }

    private fun evaluateConstructorCallExpression(function: LLVMVariable, names: List<LLVMSingleValue>): LLVMSingleValue? {
        val store = codeBuilder.getNewVariable(function.type, pointer = 1)
        codeBuilder.allocStaticVar(store, pointer = true)

        val result = codeBuilder.getNewVariable(function.type, pointer = 2)
        codeBuilder.allocStackVar(result, pointer = true)
        codeBuilder.storeVariable(result, store)

        val args = mutableListOf<LLVMSingleValue>(store)
        args.addAll(names)

        codeBuilder.addLLVMCodeToLocalPlace(LLVMCall(
                LLVMVoidType(),
                function.toString(),
                args
        ).toString())

        return result
    }

    private fun parseArgList(expr: KtCallExpression, scopeDepth: Int): List<LLVMSingleValue> =
            parseValueArguments(expr.getValueArgumentsInParentheses(), scopeDepth)

    private fun parseValueArguments(args: List<ValueArgument>, scopeDepth: Int): List<LLVMSingleValue> =
            args.map { parseOneValueArgument(it, scopeDepth) }

    fun parseOneValueArgument(arg: ValueArgument, scopeDepth: Int): LLVMSingleValue =
            evaluateExpression(arg.getArgumentExpression(), scopeDepth) as LLVMSingleValue

    private fun parseNamedValueArguments(args: MutableCollection<ResolvedValueArgument>, defaultValues: List<KtExpression?>, scopeDepth: Int): List<LLVMSingleValue> =
            args.mapIndexed(fun(i: Int, value: ResolvedValueArgument): LLVMSingleValue {
                return when (value) {
                    is DefaultValueArgument -> evaluateExpression(defaultValues[i], scopeDepth)!!
                    is ExpressionValueArgument -> evaluateExpression((value.valueArgument as KtValueArgument).getArgumentExpression()!!, scopeDepth)!!
                    else -> throw UnexpectedException("Wrong parser argument")
                }
            }).toList()


    private fun evaluateBinaryExpression(expr: KtBinaryExpression, scopeDepth: Int): LLVMVariable? {
        val operator = expr.operationToken

        if (operator == KtTokens.ELVIS) {
            return evaluateElvisOperator(expr, scopeDepth)
        }

        val left = evaluateExpression(expr.left, scopeDepth)
        if (expr.left is KtArrayAccessExpression) {
            val callMaker = state.bindingContext.get(BindingContext.CALL, expr.left)
            if (callMaker!!.callType == Call.CallType.ARRAY_SET_METHOD) {
                return left as LLVMVariable?
            }
        }

        left ?: throw UnsupportedOperationException("Wrong binary expression: ${expr.text}")
        val right = evaluateExpression(expr.right, scopeDepth)
                ?: throw UnsupportedOperationException("Wrong binary expression: ${expr.text}")

        return executeBinaryExpression(operator, expr.operationReference, left, right)
    }

    private fun evaluatePostfixExpression(expr: KtPostfixExpression, scopeDepth: Int): LLVMSingleValue? {
        val operator = expr.operationToken
        val left = evaluateExpression(expr.baseExpression, scopeDepth)
                ?: throw UnsupportedOperationException("Wrong binary expression: ${expr.text}")
        return executePostfixExpression(operator, left as LLVMVariable)
    }

    private fun evaluatePrefixExpression(expr: KtPrefixExpression, scopeDepth: Int): LLVMSingleValue? {
        val operator = expr.operationToken
        val left = evaluateExpression(expr.baseExpression, scopeDepth)
                ?: throw UnsupportedOperationException("Wrong binary expression")
        return executePrefixExpression(operator, expr.operationReference, left)
    }

    private fun executePostfixExpression(operator: IElementType?, left: LLVMVariable): LLVMSingleValue?
            = addPrimitivePostfixOperation(operator, left)

    private fun executePrefixExpression(operator: IElementType?, operationReference: KtSimpleNameExpression, left: LLVMSingleValue): LLVMSingleValue?
            = addPrimitivePrefixOperation(operator, operationReference, left)

    private fun addPrimitivePostfixOperation(operator: IElementType?, firstOp: LLVMVariable): LLVMSingleValue? {
        val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
        when (operator) {
            KtTokens.PLUSPLUS, KtTokens.MINUSMINUS -> {
                val oldValue = codeBuilder.getNewVariable(firstOp.type, firstOp.pointer)
                codeBuilder.allocStackVar(oldValue, asValue = true)
                codeBuilder.copyVariable(firstOp, oldValue)

                val llvmExpression = when (operator) {
                    KtTokens.PLUSPLUS -> firstOp.type.operatorInc(firstNativeOp)
                    KtTokens.MINUSMINUS -> firstOp.type.operatorDec(firstNativeOp)
                    else -> throw IllegalAccessError()
                }

                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)
                codeBuilder.storeVariable(firstOp, resultOp)

                return oldValue
            }
            KtTokens.EXCLEXCL -> {
                var result = firstOp
                val nullLabel = codeBuilder.getNewLabel(prefix = "nullCheck")
                val notNullLabel = codeBuilder.getNewLabel(prefix = "nullCheck")
                val nullCheck = codeBuilder.nullCheck(firstOp)
                codeBuilder.addCondition(nullCheck, nullLabel, notNullLabel)
                codeBuilder.markWithLabel(nullLabel)
                codeBuilder.addExceptionCall("KotlinNullPointerException")
                codeBuilder.addUnconditionalJump(notNullLabel)
                codeBuilder.markWithLabel(notNullLabel)
                if (firstOp.type.isPrimitive()) {
                    result = codeBuilder.downLoadArgument(firstOp, 0) as LLVMVariable
                }
                return result
            }
            else -> throw UnsupportedOperationException()
        }
    }

    private fun addPrimitivePrefixOperation(operator: IElementType?, operationReference: KtSimpleNameExpression, firstOp: LLVMSingleValue): LLVMSingleValue? {
        when (operator) {
            KtTokens.MINUS,
            KtTokens.PLUS -> {
                return addPrimitiveBinaryOperation(operator!!, operationReference, LLVMConstant("0", firstOp.type), firstOp)
            }
            KtTokens.EXCL -> {
                val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
                val llvmExpression = addPrimitiveReferenceOperationByName("xor", LLVMConstant("true", LLVMBooleanType()), firstNativeOp)
                val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
                codeBuilder.addAssignment(resultOp, llvmExpression)
                return resultOp
            }
            else -> throw UnsupportedOperationException()
        }
    }

    fun executeBinaryExpression(operator: IElementType, referenceName: KtSimpleNameExpression?, left: LLVMSingleValue, right: LLVMSingleValue)
            = addPrimitiveBinaryOperation(operator, referenceName, left, right)

    private fun evaluateElvisOperator(expr: KtBinaryExpression, scopeDepth: Int): LLVMVariable {
        val left = evaluateExpression(expr.firstChild, scopeDepth)
                ?: throw UnsupportedOperationException("Wrong binary expression")
        val lptr = codeBuilder.loadAndGetVariable(left as LLVMVariable)

        val condition = lptr.type.operatorEq(lptr, LLVMVariable("", LLVMNullType()))

        val conditionResult = codeBuilder.getNewVariable(condition.variableType)
        codeBuilder.addAssignment(conditionResult, condition)

        val thenLabel = codeBuilder.getNewLabel(prefix = "elvis")
        val elseLabel = codeBuilder.getNewLabel(prefix = "elvis")
        val endLabel = codeBuilder.getNewLabel(prefix = "elvis")

        codeBuilder.addCondition(conditionResult, elseLabel, thenLabel)
        codeBuilder.markWithLabel(thenLabel)
        codeBuilder.addUnconditionalJump(endLabel)

        codeBuilder.markWithLabel(elseLabel)
        var right = evaluateExpression(expr.lastChild, scopeDepth + 1)
        if (right != null) {
            right = codeBuilder.loadAndGetVariable(right as LLVMVariable)
            codeBuilder.storeVariable(left, right)
        }

        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)

        return left
    }

    fun addPrimitiveReferenceOperation(referenceName: KtSimpleNameExpression, firstOp: LLVMSingleValue, secondNativeOp: LLVMSingleValue): LLVMExpression
            = addPrimitiveReferenceOperationByName(referenceName.getReferencedName(), firstOp, secondNativeOp)

    fun addPrimitiveReferenceOperationByName(operator: String, firstOp: LLVMSingleValue, secondNativeOp: LLVMSingleValue): LLVMExpression {
        val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)

        return when (operator) {
            "||",
            "or" -> firstNativeOp.type!!.operatorOr(firstNativeOp, secondNativeOp)
            "xor" -> firstNativeOp.type!!.operatorXor(firstNativeOp, secondNativeOp)
            "&&",
            "and" -> firstNativeOp.type!!.operatorAnd(firstNativeOp, secondNativeOp)
            "%" -> firstNativeOp.type!!.operatorMod(firstNativeOp, secondNativeOp)
            "shl" -> firstNativeOp.type!!.operatorShl(firstNativeOp, codeBuilder.convertVariableToType(secondNativeOp, firstNativeOp.type!!))
            "shr" -> firstNativeOp.type!!.operatorShr(firstNativeOp, codeBuilder.convertVariableToType(secondNativeOp, firstNativeOp.type!!))
            "ushr" -> firstNativeOp.type!!.operatorUshr(firstNativeOp, codeBuilder.convertVariableToType(secondNativeOp, firstNativeOp.type!!))
            "+=" -> {
                val resultOp = codeBuilder.storeExpression(firstOp, firstNativeOp.type!!.operatorPlus(firstNativeOp, secondNativeOp))
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            "-=" -> {
                val resultOp = codeBuilder.storeExpression(firstOp, firstNativeOp.type!!.operatorMinus(firstNativeOp, secondNativeOp))
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            "*=" -> {
                val resultOp = codeBuilder.storeExpression(firstOp, firstNativeOp.type!!.operatorTimes(firstNativeOp, secondNativeOp))
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            "%=" -> {
                val resultOp = codeBuilder.storeExpression(firstOp, firstNativeOp.type!!.operatorMod(firstNativeOp, secondNativeOp))
                return LLVMExpression(resultOp.type, "load ${firstOp.getType()} $firstOp, align ${firstOp.type!!.align}")
            }
            ".." -> {
                val descriptor = state.classes["kotlin.ranges.IntRange"]
                val arguments = listOf(firstOp, secondNativeOp)
                val detectedConstructor = LLVMType.mangleFunctionTypes(arguments.map { it.type!! })
                val result = evaluateConstructorCallExpression(LLVMVariable(descriptor!!.fullName + detectedConstructor, descriptor.type, scope = LLVMVariableScope()), arguments)
                return LLVMExpression(result!!.type!!, "load ${descriptor.type}** $result, align ${descriptor.type.align}", pointer = 1)
            }
            else -> throw UnsupportedOperationException("Unknown binary operator: $operator(${firstNativeOp.type}, ${secondNativeOp.type})")
        }
    }

    private fun addPrimitiveBinaryOperation(operator: IElementType, referenceName: KtSimpleNameExpression?, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMVariable {
        val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
        val secondNativeOp = codeBuilder.receiveNativeValue(secondOp)

        val llvmExpression = when (operator) {
            KtTokens.PLUS -> firstOp.type!!.operatorPlus(firstNativeOp, secondNativeOp)
            KtTokens.MINUS -> firstOp.type!!.operatorMinus(firstNativeOp, secondNativeOp)
            KtTokens.MUL -> firstOp.type!!.operatorTimes(firstNativeOp, secondNativeOp)
            KtTokens.DIV -> firstOp.type!!.operatorDiv(firstNativeOp, secondNativeOp)
            KtTokens.LT -> firstOp.type!!.operatorLt(firstNativeOp, secondNativeOp)
            KtTokens.GT -> firstOp.type!!.operatorGt(firstNativeOp, secondNativeOp)
            KtTokens.LTEQ -> firstOp.type!!.operatorLeq(firstNativeOp, secondNativeOp)
            KtTokens.GTEQ -> firstOp.type!!.operatorGeq(firstNativeOp, secondNativeOp)
            KtTokens.EQEQ ->
                if (LLVMType.isReferredType(firstOp.type) && LLVMType.isReferredType(secondOp.type)) {
                    val firstPointedArgument = codeBuilder.receivePointedArgument(firstOp, 1)
                    val secondPointedArgument = codeBuilder.receivePointedArgument(secondOp, 1)
                    firstOp.type!!.operatorEq(firstPointedArgument, secondPointedArgument)
                } else
                    firstOp.type!!.operatorEq(firstNativeOp, secondNativeOp)
            KtTokens.EQEQEQ -> {
                val firstPointedArgument = codeBuilder.receivePointedArgument(firstOp, 1)
                val secondPointedArgument = codeBuilder.receivePointedArgument(secondOp, 1)
                firstOp.type!!.operatorEq(firstPointedArgument, secondPointedArgument)
            }
            KtTokens.EXCLEQ -> {
                if (LLVMType.isReferredType(firstOp.type) && LLVMType.isReferredType(secondOp.type)) {
                    val firstPointedArgument = codeBuilder.receivePointedArgument(firstOp, 1)
                    val secondPointedArgument = codeBuilder.receivePointedArgument(secondOp, 1)
                    firstOp.type!!.operatorNeq(firstPointedArgument, secondPointedArgument)
                } else
                    firstOp.type!!.operatorNeq(firstNativeOp, secondNativeOp)
            }
            KtTokens.EXCLEQEQEQ -> {
                val firstPointedArgument = codeBuilder.receivePointedArgument(firstOp, 1)
                val secondPointedArgument = codeBuilder.receivePointedArgument(secondOp, 1)
                firstOp.type!!.operatorNeq(firstPointedArgument, secondPointedArgument)
            }
            KtTokens.EQ -> {
                if (secondOp.type is LLVMNullType) {
                    codeBuilder.storeNull(firstOp as LLVMVariable)
                    return firstOp
                }

                val result = firstOp as LLVMVariable
                val sourceArgument: LLVMSingleValue
                if ((firstOp.pointer == 2) && secondOp.type!!.isPrimitive() && (secondOp.pointer == 0)) {
                    sourceArgument = codeBuilder.getNewVariable(secondOp.type!!, 1)
                    codeBuilder.allocStaticVar(sourceArgument, asValue = true)
                    codeBuilder.storeVariable(sourceArgument, secondOp)
                } else {
                    sourceArgument = if (result.pointer == secondOp.pointer + 1) secondOp else secondNativeOp
                }

                codeBuilder.storeVariable(result, sourceArgument)
                return result
            }
            else -> addPrimitiveReferenceOperation(referenceName!!, firstOp, secondNativeOp)
        }
        val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType, pointer = llvmExpression.pointer)
        codeBuilder.addAssignment(resultOp, llvmExpression)
        return resultOp
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMConstant {
        val expressionKotlinType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        val expressionValue = state.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expr)?.getValue(expressionKotlinType)
        val type = LLVMMapStandardType(expressionKotlinType, state)
        return LLVMConstant(expressionValue?.toString() ?: "", type, pointer = 0)
    }

    private fun evaluatePsiElement(element: PsiElement, scopeDepth: Int): LLVMSingleValue? {
        return when (element) {
            is LeafPsiElement -> evaluateLeafPsiElement(element, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(element)
            else -> null
        }
    }

    private fun evaluateLeafPsiElement(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        return when (element.elementType) {
            KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element, scopeDepth)
            KtTokens.IF_KEYWORD -> evaluateIfOperator(element.context as KtIfExpression, scopeDepth, isExpression = false)
            KtTokens.WHILE_KEYWORD -> evaluateWhileOperator(element.context as KtWhileExpression, scopeDepth)
            KtTokens.FOR_KEYWORD -> evaluateForOperator(element.context as KtForExpression, scopeDepth)
            else -> null
        }
    }

    private fun evaluateForOperator(expr: KtForExpression, scopeDepth: Int): LLVMVariable? {
        val range = evaluateExpression(expr.loopRange, scopeDepth + 1)!!
        val conditionLabel = codeBuilder.getNewLabel(prefix = "for_condition")
        val bodyLabel = codeBuilder.getNewLabel(prefix = "for_body")
        val exitLabel = codeBuilder.getNewLabel(prefix = "for_exit")
        val rangeTypeName = (range.type as LLVMReferenceType).type

        val descriptor = state.classes[rangeTypeName] ?: throw UnexpectedException("Error occurred in evaluating range expression")
        val method = descriptor.methods["$rangeTypeName.iterator"] ?: throw UnexpectedException("Cant receive iterator $rangeTypeName.iterator")
        val returnType = method.returnType!!.type
        val returnTypeName = (returnType as LLVMReferenceType).type
        val iteratorDescriptor = state.classes[returnTypeName]
        val nextDescriptor = iteratorDescriptor!!.methods["$returnTypeName.next"] ?: throw UnexpectedException("$returnTypeName.nextInt")

        val conditionIterator = evaluateFunctionCallExpression(LLVMVariable("$rangeTypeName.iterator", returnType, scope = LLVMVariableScope()), listOf(range))!!
        val iteratorThisArgument = codeBuilder.loadOneArgumentIfRequired(conditionIterator, LLVMVariable("type", descriptor.type, pointer = 1))
        codeBuilder.addUnconditionalJump(conditionLabel)
        codeBuilder.markWithLabel(conditionLabel)
        var conditionResult = evaluateFunctionCallExpression(LLVMVariable("$returnTypeName.hasNext", LLVMBooleanType(), scope = LLVMVariableScope()), listOf(iteratorThisArgument))!!
        conditionResult = codeBuilder.downLoadArgument(conditionResult, 0)
        codeBuilder.addCondition(conditionResult, bodyLabel, exitLabel)

        codeBuilder.addUnconditionalJump(bodyLabel)
        codeBuilder.markWithLabel(bodyLabel)
        val loopParameter = evaluateFunctionCallExpression(LLVMVariable("$returnTypeName.next", LLVMIntType(), scope = LLVMVariableScope()), listOf(iteratorThisArgument))!!
        val loopParameterDescriptor = state.bindingContext.get(BindingContext.VALUE_PARAMETER, expr.loopParameter!!)?.fqNameSafe?.asString() ?: expr.loopParameter!!.name!!

        val allocVar = variableManager.receiveVariable(loopParameterDescriptor, nextDescriptor.returnType!!.type, LLVMRegisterScope(), pointer =
        nextDescriptor.returnType!!.pointer + 1)
        variableManager.addVariable(loopParameterDescriptor, allocVar, scopeDepth + 1)
        codeBuilder.allocStackVar(allocVar, pointer = true)

        addPrimitiveBinaryOperation(KtTokens.EQ, null, allocVar, loopParameter)

        evaluateCodeBlock(expr.body, null, conditionLabel, exitLabel, scopeDepth + 1)
        codeBuilder.markWithLabel(exitLabel)

        return null
    }

    private fun evaluateWhenItem(item: KtWhenEntry, target: LLVMSingleValue, resultVariable: LLVMVariable, elseLabel: LLVMLabel, endLabel: LLVMLabel, isElse: Boolean, scopeDepth: Int) {
        val successConditionsLabel = codeBuilder.getNewLabel(prefix = "when_condition_success")
        var nextLabel = codeBuilder.getNewLabel(prefix = "when_condition_condition")

        codeBuilder.addUnconditionalJump(nextLabel)

        for (condition in item.conditions) {
            codeBuilder.markWithLabel(nextLabel)
            nextLabel = codeBuilder.getNewLabel(prefix = "when_condition_condition")

            val currentConditionExpression = evaluateExpression(condition.firstChild, scopeDepth + 1)!!
            val conditionResult = executeBinaryExpression(KtTokens.EQEQ, null, target, currentConditionExpression)

            codeBuilder.addCondition(conditionResult, successConditionsLabel, nextLabel)
        }

        codeBuilder.markWithLabel(nextLabel)
        codeBuilder.addUnconditionalJump(if (isElse) successConditionsLabel else elseLabel)
        codeBuilder.markWithLabel(successConditionsLabel)

        var successExpression = evaluateExpression(item.expression, scopeDepth + 1)
        while (successExpression is LLVMVariable && successExpression.pointer > 0) {
            successExpression = codeBuilder.loadAndGetVariable(successExpression)
        }

        if (successExpression != null && !LLVMType.nullOrVoidType(resultVariable.type)) {
            codeBuilder.storeVariable(resultVariable, successExpression)
        }

        codeBuilder.addUnconditionalJump(endLabel)
    }

    private fun evaluateWhenExpression(expr: KtWhenExpression, scopeDepth: Int): LLVMVariable? {
        codeBuilder.addComment("start when expression")
        val whenExpression = expr.subjectExpression
        val kotlinType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        val expressionType = LLVMMapStandardType(kotlinType, state)

        val targetExpression = evaluateExpression(whenExpression, scopeDepth + 1) ?: LLVMConstant("true", LLVMBooleanType())
        val resultVariable = codeBuilder.getNewVariable(expressionType, pointer = 1)

        when (expressionType) {
            is LLVMVoidType,
            is LLVMNullType -> {
            }
            is LLVMReferenceType -> codeBuilder.allocStaticVar(resultVariable, asValue = true)
            else -> codeBuilder.allocStackVar(resultVariable, asValue = true)
        }

        var nextLabel = codeBuilder.getNewLabel(prefix = "when_start")
        val endLabel = codeBuilder.getNewLabel(prefix = "when_end")
        codeBuilder.addUnconditionalJump(nextLabel)
        for (item in expr.entries) {
            codeBuilder.markWithLabel(nextLabel)
            nextLabel = codeBuilder.getNewLabel(prefix = "when_item")
            evaluateWhenItem(item, targetExpression, resultVariable, nextLabel, endLabel, item.isElse, scopeDepth + 1)
        }

        codeBuilder.markWithLabel(nextLabel)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        return resultVariable
    }

    private fun evaluateWhileOperator(expr: KtWhileExpression, scopeDepth: Int): LLVMVariable? =
            executeWhileBlock(expr.condition!!, expr.body!!, scopeDepth, checkConditionBeforeExecute = true)

    private fun executeWhileBlock(condition: KtExpression, bodyExpression: PsiElement, scopeDepth: Int, checkConditionBeforeExecute: Boolean): LLVMVariable? {
        val conditionLabel = codeBuilder.getNewLabel(prefix = "while")
        val bodyLabel = codeBuilder.getNewLabel(prefix = "while")
        val exitLabel = codeBuilder.getNewLabel(prefix = "while")

        codeBuilder.addUnconditionalJump(if (checkConditionBeforeExecute) conditionLabel else bodyLabel)
        codeBuilder.markWithLabel(conditionLabel)
        var conditionResult = evaluateExpression(condition, scopeDepth + 1)!!
        while (conditionResult.pointer > 0) {
            conditionResult = codeBuilder.loadAndGetVariable(conditionResult as LLVMVariable)
        }

        codeBuilder.addCondition(conditionResult, bodyLabel, exitLabel)
        evaluateCodeBlock(bodyExpression, bodyLabel, conditionLabel, exitLabel, scopeDepth + 1)
        codeBuilder.markWithLabel(exitLabel)

        return null
    }

    private fun evaluateIfOperator(element: KtIfExpression, scopeDepth: Int, isExpression: Boolean = true): LLVMVariable? {
        val conditionResult = evaluateExpression(element.condition, scopeDepth)!!
        val conditionNativeResult = codeBuilder.downLoadArgument(conditionResult, 0)

        return if (isExpression)
            executeIfExpression(conditionNativeResult, element.then!!, element.`else`, element, scopeDepth + 1)
        else
            executeIfBlock(conditionNativeResult, element.then!!, element.`else`, scopeDepth + 1)
    }

    private fun executeIfExpression(conditionResult: LLVMSingleValue, thenExpression: KtExpression, elseExpression: PsiElement?, ifExpression: KtIfExpression, scopeDepth: Int): LLVMVariable? {
        val kotlinType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, ifExpression)!!.type!!
        val expressionType = LLVMInstanceOfStandardType("type", kotlinType, LLVMVariableScope(), state).type
        val resultVariable = codeBuilder.getNewVariable(expressionType, pointer = 1)

        when (resultVariable.type) {
            is LLVMReferenceType -> codeBuilder.allocStaticVar(resultVariable, asValue = true)
            else -> codeBuilder.allocStackVar(resultVariable, asValue = true)
        }
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)
        codeBuilder.markWithLabel(thenLabel)
        val thenResultExpression = evaluateExpression(thenExpression, scopeDepth + 1) ?: throw UnexpectedException("Can't evaluate then in if expression")
        val thenResultNativeExpression = codeBuilder.receiveNativeValue(thenResultExpression)
        codeBuilder.storeVariable(resultVariable, thenResultNativeExpression)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(elseLabel)

        val elseResultExpression = evaluateExpression(elseExpression, scopeDepth + 1) ?: throw UnexpectedException("Can't evaluate else in if expression")
        val elseResultNativeExpression = codeBuilder.receiveNativeValue(elseResultExpression)
        codeBuilder.storeVariable(resultVariable, elseResultNativeExpression)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        return resultVariable
    }

    private fun executeIfBlock(conditionResult: LLVMSingleValue, thenExpression: PsiElement, elseExpression: PsiElement?, scopeDepth: Int): LLVMVariable? {
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, if (elseExpression != null) elseLabel else endLabel)

        evaluateCodeBlock(thenExpression, thenLabel, endLabel, endLabel, scopeDepth + 1)
        if (elseExpression != null) {
            evaluateCodeBlock(elseExpression, elseLabel, endLabel, endLabel, scopeDepth + 1)
        }

        codeBuilder.markWithLabel(endLabel)
        return null
    }

    private fun evaluateValExpression(element: KtProperty, scopeDepth: Int): LLVMVariable? {
        val variable = state.bindingContext.get(BindingContext.VARIABLE, element)!!
        val identifier = variable.fqNameSafe.convertToNativeName()

        val assignExpression = evaluateExpression(element.delegateExpressionOrInitializer, scopeDepth)
        val expectedExpressionType = LLVMInstanceOfStandardType("", variable.type, state = state)

        val primitivePointer = !LLVMType.isReferredType(LLVMMapStandardType(variable.type, state))

        val allocVar = variableManager.receiveVariable(identifier, expectedExpressionType.type, LLVMRegisterScope(), pointer = expectedExpressionType.pointer + 1)
        codeBuilder.allocStackVar(allocVar, pointer = true)

        variableManager.addVariable(identifier, allocVar, scopeDepth)
        if (assignExpression != null) {
            if ((primitivePointer) && (assignExpression.type is LLVMReferenceType)) {
                throw UnexpectedException(element.text)
            }
            addPrimitiveBinaryOperation(KtTokens.EQ, null, allocVar, assignExpression)
        }

        return null
    }

    private fun evaluateReturnInstruction(element: PsiElement, scopeDepth: Int): LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        var retVar = evaluateExpression(next, scopeDepth)
        val type = retVar?.type ?: LLVMVoidType()

        when (type) {
            is LLVMReferenceType -> generateReferenceReturn(retVar!!)
            is LLVMNullType -> {
                retVar = when (retVar) {
                    is LLVMConstant -> LLVMConstant(retVar.value, LLVMNullType(returnType!!.type), returnType!!.pointer - 1)
                    is LLVMVariable -> LLVMVariable(retVar.label, LLVMNullType(returnType!!.type), retVar.kotlinName, retVar.scope, returnType!!.pointer - 1)
                    else -> throw UnexpectedException("Unknown inheritor of LLVMSingleValue")
                }
                generateReferenceReturn(retVar)
            }
            is LLVMVoidType -> {
                codeBuilder.addAnyReturn(LLVMVoidType())
            }
            else -> {
                val retNativeValue = codeBuilder.receiveNativeValue(retVar!!)
                codeBuilder.addReturnOperator(retNativeValue)
            }
        }
        if (scopeDepth == topLevel + 2) {
            wasReturnOnTopLevel = true
        }
        return null
    }

    private fun generateReferenceReturn(retVar: LLVMSingleValue) {
        var result = retVar
        if (result.pointer == 2) {
            result = codeBuilder.loadAndGetVariable(retVar as LLVMVariable)
        }

        codeBuilder.storeVariable(returnType!!, result)
        codeBuilder.addAnyReturn(LLVMVoidType())
    }
}