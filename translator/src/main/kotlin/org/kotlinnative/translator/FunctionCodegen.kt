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
        expressionWalker(function.bodyExpression)

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
            val loadVariable = LLVMVariable("%${it.label}", it.type, it.label, true)
            codeBuilder.loadVariable(loadVariable)
            variableManager.addVariable(it.label, loadVariable, 2)
        }
    }

    private fun expressionWalker(expr: PsiElement?, scopeDepth: Int = 0) {
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
        return null
    }

    private fun evaluteFunctionCallExpression(expr: KtCallExpression): LLVMNode? {
        val function = expr.firstChild.firstChild

        val descriptor = state.functions[function.text] ?: return null
        val names = parseArgList(expr
                .firstChild
                .getNextSiblingIgnoringWhitespaceAndComments()
                ?.firstChild)

        return LLVMCall(descriptor.returnType, "@${function.text}", descriptor.args?.mapIndexed {
            i: Int, variable: LLVMVariable -> LLVMVariable(names[i], variable.type)
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

    private fun evaluateBinaryExpression(expr: KtBinaryExpression, scopeDepth: Int): LLVMNode {
        val left = evaluateExpression(expr.firstChild, scopeDepth) as LLVMVariable? ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild, scopeDepth) as LLVMVariable? ?: throw UnsupportedOperationException("Wrong binary exception")
        val operator = expr.operationToken

        return codeBuilder.addPrimitiveBinaryOperation(operator, left, right)
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMVariable {
        val node = expr.node
        return codeBuilder.addConstant(LLVMVariable(node.firstChildNode.text, LLVMIntType(), pointer = false))
    }

    private fun evaluatePsiElement(element: PsiElement, scopeDepth: Int): LLVMVariable? {
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
            else -> null
        }
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
                return null
            }
        }
        codeBuilder.addAssignment(LLVMVariable("%${identifier!!.text}", null, identifier.text), assignExpression)
        return null
    }

    private fun evaluateReturnInstruction(element: LeafPsiElement, scopeDepth: Int): LLVMVariable? {
        val next = element.getNextSiblingIgnoringWhitespaceAndComments()
        val retVar = evaluateExpression(next, scopeDepth) as LLVMVariable

        codeBuilder.addReturnOperator(retVar)
        return null
    }
}