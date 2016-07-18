package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
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


class FunctionCodegen(val state: TranslationState, val variableManager: VariableManager, val function: KtNamedFunction, val codeBuilder: LLVMBuilder) {

    var name = function.fqName.toString()
    var returnType: LLVMVariable
    var args = ArrayList<LLVMVariable>()
    val topLevel = 2
    var wasReturnOnTopLevel = false

    init {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)!!
        args.addAll(descriptor.valueParameters.map {
            LLVMMapStandardType(it.name.toString(), it.type)
        })

        returnType = LLVMMapStandardType("instance", descriptor.returnType!!)
        val retType = returnType.type
        when (retType) {
            is LLVMReferenceType -> {
                if (state.classes.containsKey(retType.type)) {
                    retType.prefix = "class"
                }

                retType.byRef = true
            }
        }
        if (retType is LLVMReferenceType && state.classes.containsKey(retType.type)) {
            retType.prefix = "class"
        }
    }

    fun generate(this_type: LLVMVariable? = null) {
        if (generateDeclaration(this_type)) {
            return
        }

        codeBuilder.addStartExpression()
        generateLoadArguments()
        evaluateCodeBlock(function.bodyExpression, scopeDepth = topLevel)

        if (!wasReturnOnTopLevel)
            codeBuilder.addAnyReturn(returnType.type)

        codeBuilder.addEndExpression()
    }

    private fun generateDeclaration(this_type: LLVMVariable? = null): Boolean {
        var external = false

        if (this_type != null) {
            args.add(this_type)
        }
        args.forEach {
            val type = it.type
            if (type is LLVMReferenceType && state.classes.containsKey(type.type)) {
                type.prefix = "class"
            }
        }

        var keyword = function.firstChild
        while (keyword != null) {
            if (keyword.text == "external") {
                external = true
                break
            }

            keyword = keyword.getNextSiblingIgnoringWhitespaceAndComments()
        }

        var actualReturnType: LLVMType = returnType.type
        val actualArgs = ArrayList<LLVMVariable>()

        if (returnType.pointer > 0) {
            actualReturnType = LLVMVoidType()
            actualArgs.add(returnType)
        }

        actualArgs.addAll(args)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(function.fqName.toString(), actualArgs, actualReturnType, external, state.arm))
        return external
    }

    private fun generateLoadArguments() {
        args.forEach(fun(it: LLVMVariable) {
            if (it.type is LLVMFunctionType || (it.type is LLVMReferenceType && (it.type as LLVMReferenceType).byRef)) {
                variableManager.addVariable(it.label, LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope(), pointer = 1), topLevel)
                return
            }

            if (it.type !is LLVMReferenceType || (it.type as LLVMReferenceType).byRef) {
                val loadVariable = LLVMVariable("${it.label}", it.type, it.label, LLVMRegisterScope(), pointer = it.pointer)
                val allocVar = codeBuilder.loadArgument(loadVariable)
                variableManager.addVariable(it.label, allocVar, topLevel)
            } else {
                variableManager.addVariable(it.label, LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope(), pointer = 0), topLevel)
            }
        })
    }

    private fun evaluateCodeBlock(expr: PsiElement?, startLabel: LLVMLabel? = null, finishLabel: LLVMLabel? = null, scopeDepth: Int = 0) {
        codeBuilder.markWithLabel(startLabel)
        expressionWalker(expr, scopeDepth)
        codeBuilder.addUnconditionalJump(finishLabel ?: return)
    }

    private fun expressionWalker(expr: PsiElement?, scopeDepth: Int) {
        when (expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild, scopeDepth + 1)
            is KtProperty -> evaluateLeafPsiElement(expr.firstChild as LeafPsiElement, scopeDepth)
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtCallExpression -> evaluateCallExpression(expr, scopeDepth)
            is KtDoWhileExpression -> evaluateDoWhileExpression(expr.firstChild, scopeDepth + 1)
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
            is KtCallableReferenceExpression -> evaluateCallableReferenceExpression(expr)
            is KtReferenceExpression -> evaluateReferenceExpression(expr, scopeDepth)
            is KtIfExpression -> evaluateIfOperator(expr.firstChild as LeafPsiElement, scopeDepth + 1, true)
            is KtDotQualifiedExpression -> evaluateDotExpression(expr, scopeDepth)
            is KtStringTemplateExpression -> evaluateStringTemplateExpression(expr)
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
        val selectorName = expr.selectorExpression!!.text

        val receiver = variableManager.getLLVMvalue(receiverName)!!

        val clazz = state.classes[(receiver.type as LLVMReferenceType).type]!!
        val field = clazz.fieldsIndex[selectorName]
        if (field != null) {
            val result = codeBuilder.getNewVariable(field.type, pointer = 1)
            codeBuilder.loadClassField(result, receiver, field.offset)
            return result
        } else {
            val methodName = clazz.clazz.name.toString() + '.' + selectorName.substringBefore('(')
            val method = clazz.methods[methodName]!!
            val returnType = clazz.methods[methodName]!!.returnType.type

            val names = parseArgList(expr.lastChild as KtCallExpression, scopeDepth)
            return evaluateFunctionCallExpression(LLVMVariable(methodName, returnType, scope = LLVMRegisterScope()), names, method.args)


        }
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

    private fun evaluateReferenceExpression(expr: KtReferenceExpression, scopeDepth: Int): LLVMSingleValue? = when (expr) {
        is KtArrayAccessExpression -> evaluateArrayAccessExpression(expr, scopeDepth + 1)
        else -> variableManager.getLLVMvalue(expr.firstChild.text)
    }

    private fun evaluateCallExpression(expr: KtCallExpression, scopeDepth: Int): LLVMSingleValue? {
        val function = expr.firstChild.firstChild.text
        val names = parseArgList(expr, scopeDepth)

        if (state.functions.containsKey(function)) {
            val descriptor = state.functions[function] ?: return null
            return evaluateFunctionCallExpression(LLVMVariable(function, descriptor.returnType.type, scope = LLVMVariableScope()), names, descriptor.args)
        }

        if (state.classes.containsKey(function)) {
            val descriptor = state.classes[function] ?: return null
            return evaluateConstructorCallExpression(LLVMVariable(function, descriptor.type, scope = LLVMVariableScope()), names)
        }

        val localFunction = variableManager.getLLVMvalue(function)
        if (localFunction != null) {
            val type = localFunction.type as LLVMFunctionType
            return evaluateFunctionCallExpression(LLVMVariable(function, type.returnType.type, scope = LLVMRegisterScope()), names, type.arguments)
        }

        return null
    }

    private fun evaluateFunctionCallExpression(function: LLVMVariable, names: List<LLVMSingleValue>, args: List<LLVMVariable>): LLVMSingleValue? {

        val names = names.mapIndexed(fun(i: Int, value: LLVMSingleValue): LLVMSingleValue {
            var result = value

            if (result.pointer > 0 && args[i].pointer == 0) {
                result = codeBuilder.getNewVariable(args[i].type)
                codeBuilder.loadVariable(result, value as LLVMVariable)
            }

            return result
        }).toList()

        val returnType = function.type
        when (returnType) {
            is LLVMVoidType -> {
                codeBuilder.addLLVMCode(LLVMCall(LLVMVoidType(), function.toString(), names).toString())
            }
            is LLVMReferenceType -> {
                val result = codeBuilder.getNewVariable(returnType, pointer = 1)
                codeBuilder.allocStaticVar(result)

                val args = ArrayList<LLVMSingleValue>()
                args.add(result)
                args.addAll(names)

                codeBuilder.addLLVMCode(LLVMCall(LLVMVoidType(), function.toString(), args).toString())

                return result
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

    private fun evaluateConstructorCallExpression(function: LLVMVariable, names: ArrayList<LLVMSingleValue>): LLVMSingleValue? {
        val result = codeBuilder.getNewVariable(function.type, pointer = 1)
        codeBuilder.allocStaticVar(result)

        val args = ArrayList<LLVMSingleValue>()
        args.add(result)
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
        val left = evaluateExpression(expr.firstChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild, scopeDepth) ?: throw UnsupportedOperationException("Wrong binary exception")
        val operator = expr.operationToken
        return codeBuilder.addPrimitiveBinaryOperation(operator, left, right)
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMConstant {
        val node = expr.node

        val type = when (node.elementType) {
            KtNodeTypes.BOOLEAN_CONSTANT -> LLVMBooleanType()
            KtNodeTypes.INTEGER_CONSTANT -> LLVMIntType()
            KtNodeTypes.FLOAT_CONSTANT -> LLVMDoubleType()
            KtNodeTypes.CHARACTER_CONSTANT -> LLVMCharType()
            else -> throw IllegalArgumentException("Unknown type")
        }
        return LLVMConstant(node.firstChildNode.text, type, pointer = 0)
    }

    private fun evaluatePsiElement(element: PsiElement, scopeDepth: Int): LLVMSingleValue? {
        return when (element) {
            is LeafPsiElement -> evaluateLeafPsiElement(element, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(element)
            is KtTypeReference -> evaluateTypeReference(element)
            KtTokens.INTEGER_LITERAL -> null
            else -> null
        }
    }

    private fun evaluateTypeReference(element: KtTypeReference): LLVMSingleValue? {
        val type = element.typeElement as KtUserType
        val operator = element.getNextSiblingIgnoringWhitespaceAndComments()
        val value = operator?.getNextSiblingIgnoringWhitespaceAndComments() as KtConstantExpression

        return when (operator) {
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateLeafPsiElement(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        return when (element.elementType) {
            KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element, scopeDepth)
            KtTokens.VAL_KEYWORD -> evaluateValExpression(element, scopeDepth)
            KtTokens.VAR_KEYWORD -> evaluateValExpression(element, scopeDepth)
            KtTokens.IF_KEYWORD -> evaluateIfOperator(element, scopeDepth, containReturn = false)
            KtTokens.WHILE_KEYWORD -> evaluateWhileOperator(element, scopeDepth)
            else -> null
        }
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

    private fun evaluateIfOperator(element: LeafPsiElement, scopeDepth: Int, containReturn: Boolean): LLVMVariable? {
        var getBrackets = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val condition = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        getBrackets = condition.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val thenExpression = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val elseKeyword = thenExpression.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val elseExpression = elseKeyword.getNextSiblingIgnoringWhitespaceAndComments() ?: return null


        return when (containReturn) {
            false -> executeIfBlock(condition.firstChild as KtBinaryExpression, thenExpression.firstChild, elseExpression.firstChild, scopeDepth + 1)
            true -> executeIfExpression(condition.firstChild as KtBinaryExpression, thenExpression.firstChild, elseExpression.firstChild, scopeDepth + 1)
        }
    }

    private fun executeIfExpression(condition: KtBinaryExpression, thenExpression: PsiElement, elseExpression: PsiElement?, scopeDepth: Int): LLVMVariable? {
        val conditionResult: LLVMVariable = evaluateBinaryExpression(condition, scopeDepth + 1)
        val variable = codeBuilder.getNewVariable(LLVMIntType(), pointer = 1)
        codeBuilder.allocStackVar(variable)
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)
        codeBuilder.markWithLabel(thenLabel)
        val thenResultExpression = evaluateExpression(thenExpression, scopeDepth + 1)
        codeBuilder.storeVariable(variable, thenResultExpression ?: return null)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(elseLabel)
        val elseResultExpression = evaluateExpression(elseExpression, scopeDepth + 1)
        codeBuilder.storeVariable(variable, elseResultExpression ?: return null)
        codeBuilder.addUnconditionalJump(endLabel)
        codeBuilder.markWithLabel(endLabel)
        return variable
    }

    private fun executeIfBlock(condition: KtBinaryExpression, thenExpression: PsiElement, elseExpression: PsiElement?, scopeDepth: Int): LLVMVariable? {
        val conditionResult = evaluateBinaryExpression(condition, scopeDepth + 1)
        val thenLabel = codeBuilder.getNewLabel(prefix = "if")
        val elseLabel = codeBuilder.getNewLabel(prefix = "if")
        val endLabel = codeBuilder.getNewLabel(prefix = "if")

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)

        evaluateCodeBlock(thenExpression, thenLabel, endLabel, scopeDepth + 1)
        evaluateCodeBlock(elseExpression, elseLabel, endLabel, scopeDepth + 1)

        codeBuilder.markWithLabel(endLabel)

        return null
    }

    private fun copyVariable(from: LLVMVariable, to: LLVMVariable) = when (from.type) {
        is LLVMStringType -> codeBuilder.storeString(to, from, 0)
        else -> codeBuilder.copyVariableValue(to, from)
    }

    private fun evaluateValExpression(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        val identifier = element.getNextSiblingIgnoringWhitespaceAndComments()
        val eq = identifier?.getNextSiblingIgnoringWhitespaceAndComments() ?: return null

        val assignExpression = evaluateExpression(eq.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth) ?: return null

        when (assignExpression) {
            is LLVMVariable -> {
                val allocVar = variableManager.receiveVariable(identifier!!.text, assignExpression.type, LLVMRegisterScope(), pointer = 1)
                codeBuilder.allocStackVar(allocVar)
                variableManager.addVariable(identifier.text, allocVar, scopeDepth)
                copyVariable(assignExpression, allocVar)
            }
            is LLVMConstant -> {
                val newVar = variableManager.receiveVariable(identifier!!.text, assignExpression.type!!, LLVMRegisterScope(), pointer = 1)

                codeBuilder.addConstant(newVar, assignExpression)
                variableManager.addVariable(identifier.text, newVar, scopeDepth)
            }
            else -> {
                throw UnsupportedOperationException()
            }
        }
        return null
    }

    private fun evaluateReturnInstruction(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        val retVar = evaluateExpression(next, scopeDepth) as LLVMSingleValue

        when (returnType.type) {
            is LLVMReferenceType -> {
                val src = codeBuilder.bitcast(retVar as LLVMVariable, LLVMVariable("", LLVMCharType(), pointer = 1))
                val dst = codeBuilder.bitcast(returnType, LLVMVariable("", LLVMCharType(), pointer = 1))
                val size = state.classes[(retVar.type as LLVMReferenceType).type]!!.size
                codeBuilder.memcpy(dst, src, size)
                codeBuilder.addAnyReturn(LLVMVoidType())
            }
            else -> {
                val retNativeValue = codeBuilder.receiveNativeValue(retVar)
                codeBuilder.addReturnOperator(retNativeValue)
            }
        }
        if (scopeDepth == topLevel + 2) {
            wasReturnOnTopLevel = true
        }
        return null
    }
}