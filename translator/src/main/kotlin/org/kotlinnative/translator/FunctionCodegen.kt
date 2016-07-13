package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*


class FunctionCodegen(val state: TranslationState, val function: KtNamedFunction, val codeBuilder: LLVMBuilder) {

    var name = function.fqName.toString()
    var returnType: LLVMType
    var args: List<LLVMVariable>?
    val variableManager = state.variableManager

    init {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)
        args = descriptor?.valueParameters?.map {
            LLVMVariable(it.name.toString(), LLVMMapStandardType(it.type.toString()))
        }

        returnType = LLVMMapStandardType(descriptor?.returnType.toString())
    }

    fun generate() {
        if (generateDeclaration()) {
            return
        }

        codeBuilder.addStartExpression()
        generateLoadArguments()
        evaluateCodeBlock(function.bodyExpression, startLabel = null, finishLabel = null, scopeDepth = 0)


        if (returnType is LLVMVoidType) {
            codeBuilder.addVoidReturn()
        }

        codeBuilder.addEndExpression()
    }

    private fun generateDeclaration(): Boolean {
        var external = false

        var keyword = function.firstChild
        while (keyword != null) {
            if (keyword.text == "external") {
                external = true
                break
            }

            keyword = keyword.getNextSiblingIgnoringWhitespaceAndComments()
        }

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(function.fqName.toString(), args, returnType, external))
        return external
    }

    private fun generateLoadArguments() {
        args?.forEach {
            val loadVariable = LLVMVariable("%${it.label}", it.type, it.label, false)
            codeBuilder.loadArgument(loadVariable)
            variableManager.addVariable(it.label, loadVariable, 2)
        }
    }

    private fun evaluateCodeBlock(expr: PsiElement?, startLabel: LLVMLabel?, finishLabel: LLVMLabel?, scopeDepth: Int) {
        codeBuilder.markWithLabel(startLabel)
        expressionWalker(expr, scopeDepth = scopeDepth)
        codeBuilder.addUnconditionJump(finishLabel ?: return)
    }

    private fun expressionWalker(expr: PsiElement?, scopeDepth: Int) {
        when (expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild, scopeDepth + 1)
            is KtProperty -> evaluateLeafPsiElement(expr.firstChild as LeafPsiElement, scopeDepth)
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is PsiElement -> evaluateExpression(expr.firstChild, scopeDepth + 1)
            null -> {
                variableManager.pullUpwardsLevel(scopeDepth)
                return
            }
            else -> UnsupportedOperationException()
        }

        expressionWalker(expr.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth)
    }

    private fun evaluateExpression(expr: PsiElement?, scopeDepth: Int): LLVMNode? {
        return when (expr) {
            is KtBinaryExpression -> evaluateBinaryExpression(expr, scopeDepth)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            is KtCallExpression -> evaluateCallExpression(expr)
            is KtReferenceExpression -> evaluateReferenceExpression(expr)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr, scopeDepth)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateReferenceExpression(expr: KtReferenceExpression): LLVMNode? {
        val variableName = expr.firstChild.text
        return variableManager.getLLVMvalue(variableName)
    }

    private fun evaluateCallExpression(expr: KtCallExpression): LLVMNode? {
        val function = expr.firstChild.firstChild.text

        if (state.functions.containsKey(function)) {
            return evaluteFunctionCallExpression(expr)
        }

        if (state.classes.containsKey(function)) {
            return evaluteConstructorCallExpression(expr)
        }

        return null
    }

    private fun evaluteConstructorCallExpression(expr: KtCallExpression): LLVMNode? {
        val function = expr.firstChild.firstChild
        val descriptor = state.classes[function.text] ?: return null
        val names = parseArgList(expr
                .firstChild
                .getNextSiblingIgnoringWhitespaceAndComments()
                ?.firstChild).mapIndexed { i: Int, s: String ->
            LLVMVariable(s, descriptor.fields[i].type, pointer = descriptor.fields[i].pointer)
        }.toList()


        return LLVMConstructorCall(
                descriptor.type, fun(thisVar): LLVMCall {
            val args = ArrayList<LLVMVariable>()
            args.add(thisVar)
            args.addAll(names)
            return LLVMCall(LLVMVoidType(), descriptor.constructorName, args)
        })
    }

    private fun evaluteFunctionCallExpression(expr: KtCallExpression): LLVMNode? {
        val function = expr.firstChild.firstChild

        val descriptor = state.functions[function.text] ?: return null
        val names = parseArgList(expr
                .firstChild
                .getNextSiblingIgnoringWhitespaceAndComments()
                ?.firstChild)

        return LLVMCall(descriptor.returnType, "@${descriptor.name}", descriptor.args?.mapIndexed {
            i: Int, variable: LLVMVariable ->
            LLVMVariable(names[i], variable.type)
        } ?: listOf())
    }

    private fun parseArgList(argumentList: PsiElement?): List<String> {
        val args = ArrayList<String>()

        var currentArg = argumentList?.getNextSiblingIgnoringWhitespaceAndComments()

        while (currentArg?.text != ")" && currentArg != null) {
            args.add(currentArg.text)

            currentArg = currentArg
                    .getNextSiblingIgnoringWhitespaceAndComments()
                    ?.getNextSiblingIgnoringWhitespaceAndComments()
        }
        return args
    }

    private fun evaluateBinaryExpression(expr: KtBinaryExpression, scopeDepth: Int): LLVMVariable {
        val left = evaluateExpression(expr.firstChild, scopeDepth) as LLVMSingleValue? ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild, scopeDepth) as LLVMSingleValue? ?: throw UnsupportedOperationException("Wrong binary exception")
        val operator = expr.operationToken
        return codeBuilder.addPrimitiveBinaryOperation(operator, left, right)
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMConstant {
        val node = expr.node
        return LLVMConstant(node.firstChildNode.text, LLVMIntType(), pointer = false)
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
            KtTokens.VAL_KEYWORD -> evaluateValExpression(element, scopeDepth)
            KtTokens.VAR_KEYWORD -> evaluateValExpression(element, scopeDepth)
            KtTokens.IF_KEYWORD -> evaluateIfExpression(element, scopeDepth)
            else -> null
        }
    }

    private fun evaluateIfExpression(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        var getBrackets = element.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val condition = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        getBrackets = condition.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val thenExpression = getBrackets.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val elseKeyword = thenExpression.getNextSiblingIgnoringWhitespaceAndComments() ?: return null
        val elseExpression = elseKeyword.getNextSiblingIgnoringWhitespaceAndComments() ?: return null


        return executeCondition(condition.firstChild as KtBinaryExpression, thenExpression.firstChild, elseExpression.firstChild, scopeDepth + 1)
    }

    private fun executeCondition(condition: KtBinaryExpression, thenExpression: PsiElement, elseExpression: PsiElement?, scopeDepth: Int): LLVMVariable? {
        val conditionResult: LLVMVariable = evaluateBinaryExpression(condition, scopeDepth + 1)
        val thenLabel = codeBuilder.getNewLabel()
        val elseLabel = codeBuilder.getNewLabel()
        val endLabel = codeBuilder.getNewLabel()

        codeBuilder.addCondition(conditionResult, thenLabel, elseLabel)

        evaluateCodeBlock(thenExpression, thenLabel, endLabel, scopeDepth + 1)
        evaluateCodeBlock(elseExpression, elseLabel, endLabel, scopeDepth + 1)

        codeBuilder.markWithLabel(endLabel)

        return null
    }

    private fun evaluateValExpression(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        val identifier = element.getNextSiblingIgnoringWhitespaceAndComments()
        val eq = identifier?.getNextSiblingIgnoringWhitespaceAndComments() ?: return null

        val assignExpression = evaluateExpression(eq.getNextSiblingIgnoringWhitespaceAndComments(), scopeDepth) ?: return null

        when (assignExpression) {
            is LLVMVariable -> {
                assignExpression.kotlinName = identifier!!.text
                assignExpression.pointer = false
                variableManager.addVariable(identifier.text, assignExpression, scopeDepth)
            }
            is LLVMConstant -> {
                val newVar = LLVMVariable("%${identifier!!.text}.addr", type = LLVMIntType(), kotlinName = identifier.text, pointer = true)
                codeBuilder.addConstant(newVar, assignExpression)
                variableManager.addVariable(identifier.text, newVar, scopeDepth)
            }
            is LLVMConstructorCall -> {
                val result = LLVMVariable("%${identifier!!.text}", assignExpression.type)
                codeBuilder.allocVar(result)
                result.pointer = true
                codeBuilder.addLLVMCode(assignExpression.call(result).toString())
            }
            else -> {
                codeBuilder.addAssignment(LLVMVariable("%${identifier!!.text}", null, identifier.text), assignExpression)
            }
        }
        return null
    }

    private fun evaluateReturnInstruction(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        val retVar = evaluateExpression(next, scopeDepth) as LLVMSingleValue
        val retNativeValue = codeBuilder.receiveNativeValue(retVar)

        codeBuilder.addReturnOperator(retNativeValue)
        return null
    }
}