package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.*
import java.util.*


abstract class BlockCodegen(open val state: TranslationState, open val variableManager: VariableManager, open val codeBuilder: LLVMBuilder) {

    val topLevel = 2
    var returnType: LLVMVariable? = null
    var wasReturnOnTopLevel = false


    protected fun evaluateCodeBlock(expr: PsiElement?, startLabel: LLVMLabel? = null, finishLabel: LLVMLabel? = null, scopeDepth: Int = 0) {
        codeBuilder.markWithLabel(startLabel)
        expressionWalker(expr, scopeDepth)
        codeBuilder.addUnconditionalJump(finishLabel ?: return)
    }

    private fun expressionWalker(expr: PsiElement?, scopeDepth: Int) {
        when (expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild, scopeDepth + 1)
            is KtProperty -> evaluateValExpression(expr, scopeDepth)
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtCallExpression -> evaluateCallExpression(expr, scopeDepth)
            is KtDoWhileExpression -> evaluateDoWhileExpression(expr.firstChild, scopeDepth + 1)
            is KtDotQualifiedExpression -> evaluateDotExpression(expr, scopeDepth)
            is PsiElement -> evaluateExpression(expr.firstChild, scopeDepth + 1)
            null -> {
                variableManager.pullUpwardsLevel(scopeDepth)
                return
            }
            else -> UnsupportedOperationException()
        }

        expressionWalker(expr.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth)
    }

    private fun evaluateDoWhileExpression(element: PsiElement, scopeDepth: Int) {
        val bodyExpression = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return
        val condition = bodyExpression.siblings(withItself = false).filter { it is KtContainerNode }.firstOrNull() ?: return

        executeWhileBlock(condition.firstChild as KtBinaryExpression, bodyExpression.firstChild, scopeDepth, checkConditionBeforeExecute = false)
    }

    private fun evaluateExpression(expr: PsiElement?, scopeDepth: Int): LLVMSingleValue? {
        return when (expr) {
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            is KtCallExpression -> evaluateCallExpression(expr, scopeDepth)
            is KtWhenExpression -> evaluateWhenExpression(expr, scopeDepth)
            is KtCallableReferenceExpression -> evaluateCallableReferenceExpression(expr)
            is KtDotQualifiedExpression -> evaluateDotExpression(expr, scopeDepth)
            is KtReferenceExpression -> evaluateReferenceExpression(expr, scopeDepth)
            is KtIfExpression -> evaluateIfOperator(expr.firstChild as LeafPsiElement, scopeDepth + 1, expr)
            is KtStringTemplateExpression -> evaluateStringTemplateExpression(expr)
            is KtReturnExpression -> evaluateReturnInstruction(expr.firstChild, scopeDepth)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr, scopeDepth)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    fun evaluateStringTemplateExpression(expr: KtStringTemplateExpression): LLVMSingleValue? {
        val receiveValue = state.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expr)
        val type = (receiveValue as TypedCompileTimeConstant).type
        val value = receiveValue.getValue(type) ?: return null
        val variable = variableManager.receiveVariable(".str", LLVMStringType(value.toString().length), LLVMVariableScope(), pointer = 0)

        codeBuilder.addStringConstant(variable, value.toString())
        return variable
    }

    private fun evaluateCallableReferenceExpression(expr: KtCallableReferenceExpression): LLVMSingleValue? {
        val kotlinType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        return LLVMMapStandardType(expr.text.substring(2), kotlinType, LLVMVariableScope())
    }

    private fun evaluateDotExpression(expr: KtDotQualifiedExpression, scopeDepth: Int): LLVMSingleValue? {
        val receiverName = expr.receiverExpression.text
        val selectorExpr = expr.selectorExpression!!

        var receiver = variableManager.getLLVMvalue(receiverName)
        if (receiver != null) {
            if (receiver.pointer == 2) {
                receiver = codeBuilder.loadAndGetVariable(receiver)
            }
            return evaluateMemberMethodOrField(receiver, selectorExpr.text, scopeDepth, expr.lastChild)
        }

        val clazz = state.classes[receiverName] ?: return null
        return evaluateClassScopedDotExpression(clazz, selectorExpr, scopeDepth)
    }

    private fun evaluateClassScopedDotExpression(clazz: ClassCodegen, selector: KtExpression, scopeDepth: Int): LLVMSingleValue? = when (selector) {

        is KtCallExpression -> evaluateCallExpression(selector, scopeDepth, clazz)
        is KtReferenceExpression -> evaluateReferenceExpression(selector, scopeDepth, clazz)
        else -> throw UnsupportedOperationException()
    }

    private fun evaluateNameReferenceExpression(expr: KtNameReferenceExpression, scopeDepth: Int, classScope: ClassCodegen? = null): LLVMSingleValue? {
        val fieldName = state.bindingContext.get(BindingContext.REFERENCE_TARGET, expr)!!.name.toString()
        val field = classScope!!.companionFieldsIndex[fieldName]

        val companionObject = classScope.companionFieldsSource[fieldName]

        val receiver = variableManager.getLLVMvalue(companionObject!!.fullName)!!

        val result = codeBuilder.getNewVariable(field!!.type, pointer = 1)
        codeBuilder.loadClassField(result, receiver, field.offset)
        return result
    }

    private fun evaluateMemberMethodOrField(receiver: LLVMVariable, selectorName: String, scopeDepth: Int, call: PsiElement): LLVMSingleValue? {
        val type = receiver.type as LLVMReferenceType
        val clazz = resolveClassOrObjectLocation(type)
        val field = clazz.fieldsIndex[selectorName]

        if (field != null) {
            val result = codeBuilder.getNewVariable(field.type, pointer = 1)
            codeBuilder.loadClassField(result, receiver, field.offset)
            return result
        }

        val typePath = type.location.joinToString(".")
        val methodName = "${if (typePath.length > 0) "$typePath." else ""}${clazz.structName}.${selectorName.substringBefore('(')}"

        val method = clazz.methods[methodName]!!
        val returnType = clazz.methods[methodName]!!.returnType!!.type

        val names = parseArgList(call as KtCallExpression, scopeDepth)
        val loadedArgs = loadArgsIfRequired(names, method.args)
        val callArgs = mutableListOf<LLVMSingleValue>(receiver)
        callArgs.addAll(loadedArgs)

        return evaluateFunctionCallExpression(LLVMVariable(methodName, returnType, scope = LLVMVariableScope()), callArgs)
    }

    private fun resolveClassOrObjectLocation(type: LLVMReferenceType): StructCodegen {
        if (type.location.size == 0) {
            return state.classes[type.type] ?: state.objects[type.type]!!
        }

        var codegen = state.classes[type.location[0]]!!
        var i = 1
        while (i < type.location.size) {
            codegen = codegen.nestedClasses[type.location[i]]!!
            i++
        }

        return codegen.nestedClasses[type.type]!!
    }

    fun evaluateArrayAccessExpression(expr: KtArrayAccessExpression, scope: Int): LLVMSingleValue? {
        val arrayNameVariable = evaluateReferenceExpression(expr.arrayExpression as KtReferenceExpression, scope) as LLVMVariable
        val arrayIndex = evaluateConstantExpression(expr.indexExpressions.first() as KtConstantExpression)
        val arrayReceivedVariable = codeBuilder.loadAndGetVariable(arrayNameVariable)
        val arrayElementType = (arrayNameVariable.type as LLVMArray).basicType()
        val indexVariable = codeBuilder.getNewVariable(arrayElementType, pointer = 1)
        codeBuilder.loadVariableOffset(indexVariable, arrayReceivedVariable, arrayIndex)
        return indexVariable
    }

    private fun evaluateReferenceExpression(expr: KtReferenceExpression, scopeDepth: Int, classScope: ClassCodegen? = null): LLVMSingleValue? = when (expr) {
        is KtArrayAccessExpression -> evaluateArrayAccessExpression(expr, scopeDepth + 1)
        else -> if ((expr is KtNameReferenceExpression) && (classScope != null)) evaluateNameReferenceExpression(expr, scopeDepth + 1, classScope)
                else variableManager.getLLVMvalue(expr.firstChild.text)
    }

    private fun evaluateCallExpression(expr: KtCallExpression, scopeDepth: Int, classScope: ClassCodegen? = null): LLVMSingleValue? {
        val function = expr.firstChild.firstChild.text
        val names = parseArgList(expr, scopeDepth)

        if (state.functions.containsKey(function)) {
            val descriptor = state.functions[function] ?: return null
            val args = loadArgsIfRequired(names, descriptor.args)
            return evaluateFunctionCallExpression(LLVMVariable(function, descriptor.returnType!!.type, scope = LLVMVariableScope()), args)
        }

        if (state.classes.containsKey(function)) {
            val descriptor = state.classes[function] ?: return null
            val args = loadArgsIfRequired(names, descriptor.constructorFields)
            return evaluateConstructorCallExpression(LLVMVariable(function, descriptor.type, scope = LLVMVariableScope()), args)
        }

        val localFunction = variableManager.getLLVMvalue(function)
        if (localFunction != null) {
            val type = localFunction.type as LLVMFunctionType
            val args = loadArgsIfRequired(names, type.arguments)
            return evaluateFunctionCallExpression(LLVMVariable(function, type.returnType.type, scope = LLVMRegisterScope()), args)
        }

        if (state.classes.containsKey(classScope?.structName)) {
            val classDescriptor = state.classes[classScope?.structName] ?: return null
            val methodShortName = classScope?.structName + "." + function
            if (classDescriptor.companionMethods.containsKey(methodShortName)) {
                val descriptor = classDescriptor.companionMethods[methodShortName] ?: return null
                val parentDescriptor = descriptor.parentCodegen!!
                val receiver = variableManager.getLLVMvalue(parentDescriptor.fullName)!!
                val methodFullName = descriptor.name

                val returnType = descriptor.returnType!!.type

                val loadedArgs = loadArgsIfRequired(names, descriptor.args)
                val callArgs = mutableListOf<LLVMSingleValue>(receiver)
                callArgs.addAll(loadedArgs)

                return evaluateFunctionCallExpression(LLVMVariable(methodFullName, returnType, scope = LLVMVariableScope()), callArgs)
            }
        }

        val nestedConstructor = classScope?.nestedClasses?.get(expr.calleeExpression!!.text)
        if (nestedConstructor != null) {
            val args = loadArgsIfRequired(names, nestedConstructor.constructorFields)
            return evaluateConstructorCallExpression(LLVMVariable(nestedConstructor.fullName, nestedConstructor.type, scope = LLVMVariableScope()), args)
        }

        return null
    }

    private fun evaluateFunctionCallExpression(function: LLVMVariable, names: List<LLVMSingleValue>): LLVMSingleValue? {
        val returnType = function.type
        when (returnType) {
            is LLVMVoidType -> {
                codeBuilder.addLLVMCode(LLVMCall(LLVMVoidType(), function.toString(), names).toString())
            }
            is LLVMReferenceType -> {
                val returnVar = codeBuilder.getNewVariable(returnType, pointer = 1)
                codeBuilder.allocStaticVar(returnVar)
                returnVar.pointer++

                val args = ArrayList<LLVMSingleValue>()
                args.add(returnVar)
                args.addAll(names)

                codeBuilder.addLLVMCode(LLVMCall(LLVMVoidType(), function.toString(), args).toString())
                return codeBuilder.loadAndGetVariable(returnVar)
            }
            else -> {
                val result = codeBuilder.getNewVariable(returnType)
                codeBuilder.addAssignment(result, LLVMCall(returnType, function.toString(), names))

                val resultPtr = codeBuilder.getNewVariable(returnType)
                codeBuilder.allocStackVar(resultPtr)
                resultPtr.pointer = 1
                codeBuilder.storeVariable(resultPtr, result)
                return resultPtr
            }
        }

        return null
    }

    private fun loadArgsIfRequired(names: List<LLVMSingleValue>, args: List<LLVMVariable>) = names.mapIndexed(fun(i: Int, value: LLVMSingleValue): LLVMSingleValue {
        var result = value

        if (result.pointer > 0 && args[i].pointer == 0) {
            result = codeBuilder.getNewVariable(args[i].type)
            codeBuilder.loadVariable(result, value as LLVMVariable)
        }

        return result
    }).toList()

    private fun evaluateConstructorCallExpression(function: LLVMVariable, names: List<LLVMSingleValue>): LLVMSingleValue? {
        val store = codeBuilder.getNewVariable(function.type)
        codeBuilder.allocStaticVar(store)
        store.pointer++

        val result = codeBuilder.getNewVariable(function.type)
        result.pointer++
        codeBuilder.allocStaticVar(result)
        result.pointer++

        codeBuilder.storeVariable(result, store)

        val args = ArrayList<LLVMSingleValue>()
        args.add(store)
        args.addAll(names)

        codeBuilder.addLLVMCode(LLVMCall(
                LLVMVoidType(),
                function.toString(),
                args
        ).toString())

        return result
    }

    private fun parseArgList(expr: KtCallExpression, scopeDepth: Int): ArrayList<LLVMSingleValue> {
        val args = expr.getValueArgumentsInParentheses()
        val result = ArrayList<LLVMSingleValue>()

        for (arg in args) {
            val currentExpression = evaluateExpression(arg.getArgumentExpression(), scopeDepth) as LLVMSingleValue
            result.add(currentExpression)
        }

        return result
    }

    private fun evaluateBinaryExpression(expr: KtBinaryExpression, scopeDepth: Int): LLVMVariable {
        val operator = expr.operationToken
        if (operator == KtTokens.ELVIS) {
            return evaluateElvisOperator(expr, scopeDepth)
        }

        val left = evaluateExpression(expr.firstChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")

        return executeBinaryExpression(operator, expr.operationReference, left, right, scopeDepth)
    }

    private fun executeBinaryExpression(operator: IElementType, referenceName: KtSimpleNameExpression?, left: LLVMSingleValue, right: LLVMSingleValue, scopeDepth: Int)
            = addPrimitiveBinaryOperation(operator, referenceName, left, right)

    private fun evaluateElvisOperator(expr: KtBinaryExpression, scopeDepth: Int): LLVMVariable {
        val left = evaluateExpression(expr.firstChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
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

    private fun addPrimitiveBinaryOperation(operator: IElementType, referenceName: KtSimpleNameExpression?, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMVariable {
        val firstNativeOp = codeBuilder.receiveNativeValue(firstOp)
        val secondNativeOp = codeBuilder.receiveNativeValue(secondOp)
        val llvmExpression = when (operator) {
            KtTokens.PLUS -> firstOp.type!!.operatorPlus(firstNativeOp, secondNativeOp)
            KtTokens.MINUS -> firstOp.type!!.operatorMinus(firstNativeOp, secondNativeOp)
            KtTokens.MUL -> firstOp.type!!.operatorTimes(firstNativeOp, secondNativeOp)
            KtTokens.LT -> firstOp.type!!.operatorLt(firstNativeOp, secondNativeOp)
            KtTokens.GT -> firstOp.type!!.operatorGt(firstNativeOp, secondNativeOp)
            KtTokens.LTEQ -> firstOp.type!!.operatorLeq(firstNativeOp, secondNativeOp)
            KtTokens.GTEQ -> firstOp.type!!.operatorGeq(firstNativeOp, secondNativeOp)
            KtTokens.EQEQ ->
                if (firstOp.type is LLVMReferenceType)
                    firstOp.type!!.operatorEq(firstNativeOp, secondOp)
                else
                    firstOp.type!!.operatorEq(firstNativeOp, secondNativeOp)
            KtTokens.EXCLEQ -> firstOp.type!!.operatorNeq(firstNativeOp, secondNativeOp)
            KtTokens.EQ -> {
                if (secondOp.type is LLVMNullType) {
                    val result = codeBuilder.getNewVariable(firstOp.type!!, firstOp.pointer)
                    codeBuilder.allocStackVar(result)
                    result.pointer++

                    codeBuilder.storeNull(result)
                    return result
                }

                val result = firstOp as LLVMVariable
                codeBuilder.storeVariable(result, secondNativeOp)
                return result
            }
            else -> codeBuilder.addPrimitiveReferenceOperation(referenceName!!, firstNativeOp, secondNativeOp)
        }
        val resultOp = codeBuilder.getNewVariable(llvmExpression.variableType)
        codeBuilder.addAssignment(resultOp, llvmExpression)
        return resultOp
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMConstant {
        val node = expr.node

        val type = when (node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> LLVMBooleanType()
            KtNodeTypes.INTEGER_CONSTANT -> LLVMIntType()
            KtNodeTypes.FLOAT_CONSTANT -> LLVMDoubleType()
            KtNodeTypes.CHARACTER_CONSTANT -> LLVMCharType()
            KtNodeTypes.NULL -> LLVMNullType()
            else -> throw IllegalArgumentException("Unknown type")
        }
        return LLVMConstant(node.firstChildNode.text, type, pointer = 0)
    }

    private fun evaluatePsiElement(element: PsiElement, scopeDepth: Int): LLVMSingleValue? {
        return when (element) {
            is LeafPsiElement -> evaluateLeafPsiElement(element, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(element)
            KtTokens.INTEGER_LITERAL -> null
            else -> null
        }
    }

    private fun evaluateLeafPsiElement(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        return when (element.elementType) {
            KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element, scopeDepth)
            KtTokens.IF_KEYWORD -> evaluateIfOperator(element, scopeDepth, ifExpression = null)
            KtTokens.WHILE_KEYWORD -> evaluateWhileOperator(element, scopeDepth)
            else -> null
        }
    }

    private fun evaluateWhenItem(item: KtWhenEntry, target: LLVMSingleValue, resultVariable: LLVMVariable, elseLabel: LLVMLabel, endLabel: LLVMLabel, isElse: Boolean, scopeDepth: Int) {
        val successConditionsLabel = codeBuilder.getNewLabel(prefix = "when_condition_success")
        var nextLabel = codeBuilder.getNewLabel(prefix = "when_condition_condition")
        codeBuilder.addUnconditionalJump(nextLabel)
        for (condition in item.conditions) {
            codeBuilder.markWithLabel(nextLabel)
            nextLabel = codeBuilder.getNewLabel(prefix = "when_condition_condition")

            val currentConditionExpression = evaluateExpression(condition.firstChild, scopeDepth + 1)!!
            val conditionResult = executeBinaryExpression(KtTokens.EQEQ, null, target, currentConditionExpression, scopeDepth)

            codeBuilder.addCondition(conditionResult, successConditionsLabel, nextLabel)
        }
        codeBuilder.markWithLabel(nextLabel)
        codeBuilder.addComment("last condition item")
        codeBuilder.addUnconditionalJump(if (isElse) successConditionsLabel else elseLabel)
        codeBuilder.markWithLabel(successConditionsLabel)
        val successExpression = evaluateExpression(item.expression, scopeDepth + 1)
        codeBuilder.storeVariable(resultVariable, successExpression ?: return)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.addComment("end last condition item")
    }

    private fun evaluateWhenExpression(expr: KtWhenExpression, scopeDepth: Int): LLVMVariable? {
        codeBuilder.addComment("start when expression")
        val whenExpression = expr.subjectExpression
        val kotlinType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, expr)!!.type!!
        val expressionType = LLVMMapStandardType("type", kotlinType, LLVMVariableScope()).type

        val targetExpression = evaluateExpression(whenExpression, scopeDepth + 1)!!

        val resultVariable = codeBuilder.getNewVariable(expressionType, pointer = 1)
        codeBuilder.allocStackPointedVarAsValue(resultVariable)

        var nextLabel = codeBuilder.getNewLabel(prefix = "when_start")
        val endLabel = codeBuilder.getNewLabel(prefix = "when_end")
        codeBuilder.addUnconditionalJump(nextLabel)
        for (item in expr.entries) {
            codeBuilder.addComment("start new when item")
            codeBuilder.markWithLabel(nextLabel)
            nextLabel = codeBuilder.getNewLabel(prefix = "when_item")
            evaluateWhenItem(item, targetExpression, resultVariable, nextLabel, endLabel, item.isElse, scopeDepth + 1)
            codeBuilder.addComment("end new when item")
        }
        codeBuilder.addComment("else branch of when expression")
        codeBuilder.markWithLabel(nextLabel)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        codeBuilder.addComment("end when expression")
        return resultVariable
    }

    private fun evaluateWhileOperator(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        var getBrackets = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val condition = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        getBrackets = condition.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val bodyExpression = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null

        return executeWhileBlock(condition.firstChild as KtBinaryExpression, bodyExpression.firstChild, scopeDepth, checkConditionBeforeExecute = true)
    }

    private fun executeWhileBlock(condition: KtBinaryExpression, bodyExpression: PsiElement, scopeDepth: Int, checkConditionBeforeExecute: Boolean): LLVMVariable? {
        val conditionLabel = codeBuilder.getNewLabel(prefix = "while")
        val bodyLabel = codeBuilder.getNewLabel(prefix = "while")
        val exitLabel = codeBuilder.getNewLabel(prefix = "while")

        codeBuilder.addUnconditionalJump(if (checkConditionBeforeExecute) conditionLabel else bodyLabel)
        codeBuilder.markWithLabel(conditionLabel)
        val conditionResult = evaluateBinaryExpression(condition, scopeDepth + 1)

        codeBuilder.addCondition(conditionResult, bodyLabel, exitLabel)
        evaluateCodeBlock(bodyExpression, bodyLabel, conditionLabel, scopeDepth + 1)
        codeBuilder.markWithLabel(exitLabel)

        return null
    }

    private fun evaluateIfOperator(element: LeafPsiElement, scopeDepth: Int, ifExpression: KtIfExpression?): LLVMVariable? {
        var getBrackets = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val condition = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        getBrackets = condition.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val thenExpression = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val elseKeyword = thenExpression.getNextSiblingIgnoringWhitespaceAndComments()
        val elseExpression = elseKeyword?.getNextSiblingIgnoringWhitespaceAndComments()

        return when (ifExpression) {
            null -> executeIfBlock(condition.firstChild as KtBinaryExpression, thenExpression.firstChild, elseExpression?.firstChild, scopeDepth + 1)
            else -> executeIfExpression(condition.firstChild as KtBinaryExpression, thenExpression.firstChild, elseExpression!!.firstChild, ifExpression, scopeDepth + 1)
        }
    }

    private fun executeIfExpression(condition: KtBinaryExpression, thenExpression: PsiElement, elseExpression: PsiElement?, ifExpression: KtIfExpression, scopeDepth: Int): LLVMVariable? {
        val conditionResult: LLVMVariable = evaluateBinaryExpression(condition, scopeDepth + 1)
        val kotlinType = state.bindingContext.get(BindingContext.EXPRESSION_TYPE_INFO, ifExpression)!!.type!!
        val expressionType = LLVMMapStandardType("type", kotlinType, LLVMVariableScope()).type
        val resultVariable = codeBuilder.getNewVariable(expressionType, pointer = 1)
        codeBuilder.allocStackPointedVarAsValue(resultVariable)
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)
        codeBuilder.markWithLabel(thenLabel)
        val thenResultExpression = evaluateExpression(thenExpression, scopeDepth + 1)
        codeBuilder.storeVariable(resultVariable, thenResultExpression ?: return null)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(elseLabel)
        val elseResultExpression = evaluateExpression(elseExpression, scopeDepth + 1)
        codeBuilder.storeVariable(resultVariable, elseResultExpression ?: return null)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        return resultVariable
    }

    private fun executeIfBlock(condition: KtBinaryExpression, thenExpression: PsiElement, elseExpression: PsiElement?, scopeDepth: Int): LLVMVariable? {
        val conditionResult = evaluateBinaryExpression(condition, scopeDepth + 1)
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, if (elseExpression != null) elseLabel else endLabel)

        evaluateCodeBlock(thenExpression, thenLabel, endLabel, scopeDepth + 1)
        if (elseExpression != null) {
            evaluateCodeBlock(elseExpression, elseLabel, endLabel, scopeDepth + 1)
        }

        codeBuilder.markWithLabel(endLabel)

        return null
    }


    private fun evaluateValExpression(element: KtProperty, scopeDepth: Int): LLVMVariable? {
        val variable = state.bindingContext.get(BindingContext.VARIABLE, element)!!
        val identifier = variable.name.toString()

        val assignExpression = evaluateExpression(element.delegateExpressionOrInitializer, scopeDepth) ?: return null

        when (assignExpression) {
            is LLVMVariable -> {
                if (assignExpression.pointer == 0) {
                    val allocVar = variableManager.receiveVariable(identifier, assignExpression.type, LLVMRegisterScope(), pointer = 0)
                    codeBuilder.allocStackVar(allocVar)
                    allocVar.pointer++
                    allocVar.kotlinName = identifier

                    variableManager.addVariable(identifier, allocVar, scopeDepth)
                    codeBuilder.copyVariable(assignExpression, allocVar)
                } else {
                    assignExpression.kotlinName = identifier
                    variableManager.addVariable(identifier, assignExpression, scopeDepth)
                }
            }
            is LLVMConstant -> {
                if (assignExpression.type is LLVMNullType) {
                    val reference = LLVMMapStandardType(identifier, variable.type)
                    if (state.classes.containsKey(variable.type.toString().dropLast(1))) {
                        (reference.type as LLVMReferenceType).prefix = "class"
                    }

                    codeBuilder.allocStackVar(reference)
                    reference.pointer += 1

                    codeBuilder.storeNull(reference)

                    variableManager.addVariable(identifier, reference, scopeDepth)
                    return null
                }

                val newVar = variableManager.receiveVariable(identifier, assignExpression.type!!, LLVMRegisterScope(), pointer = 1)
                codeBuilder.addConstant(newVar, assignExpression)
                variableManager.addVariable(identifier, newVar, scopeDepth)
            }
            else -> {
                throw UnsupportedOperationException()
            }
        }
        return null
    }

    private fun evaluateReturnInstruction(element: PsiElement, scopeDepth: Int): LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        var retVar = evaluateExpression(next, scopeDepth)
        val type = retVar?.type ?: LLVMVoidType()

        when (type) {
            is LLVMReferenceType -> {
                if (retVar!!.pointer == 2) {
                    retVar = codeBuilder.loadAndGetVariable(retVar as LLVMVariable)
                }

                codeBuilder.storeVariable(returnType!!, retVar)
                codeBuilder.addAnyReturn(LLVMVoidType())
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
}