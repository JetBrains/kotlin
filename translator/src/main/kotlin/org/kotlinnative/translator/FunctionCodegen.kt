package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.llvm.LLVMBuilder
import org.kotlinnative.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMDescriptorGenerate
import org.kotlinnative.translator.llvm.LLVMMapStandardType


class FunctionCodegen(val state: TranslationState, val function: KtNamedFunction, val codeBuilder: LLVMBuilder) {

    fun generate() {
        generateDeclaration(function)
        codeBuilder.addStartExpression()
        expressionWalker(function.bodyExpression)
        codeBuilder.addEndExpression()
    }

    private fun generateDeclaration(function: KtNamedFunction) {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)
        var args = descriptor?.valueParameters?.map {
            Pair(it.name.toString(), LLVMMapStandardType(it.type.toString()))
        }
        val returnType = LLVMMapStandardType(descriptor?.returnType.toString())

        codeBuilder.addLLVMCode(LLVMDescriptorGenerate(function.fqName.toString(), args, returnType))
    }

    private fun expressionWalker(expr: Any?) {
        when (expr) {
            is KtBlockExpression -> evaluateBlockExpression(expr)
            is PsiElement -> evaluatePsiExpression(expr)
            null -> Unit
            else -> UnsupportedOperationException()
        }
    }

    private fun evaluateBlockExpression(expr: KtBlockExpression) {
        expressionWalker(expr.firstChild)
        expressionWalker(expr.getNextSiblingIgnoringWhitespaceAndComments())
    }

    private fun evaluatePsiExpression(expr: PsiElement) {
        evaluateExpression(expr.firstChild)
        evaluatePsiExpression(expr.getNextSiblingIgnoringWhitespaceAndComments() ?: return)
    }

    private fun evaluateExpression(expr: Any?): LLVMVariable? {
        return when (expr) {
            is KtBinaryExpression -> evaluateBinaryExpression(expr)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateBinaryExpression(expr: KtBinaryExpression): LLVMVariable {
        val left = evaluateExpression(expr.firstChild) ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild) ?: throw UnsupportedOperationException("Wrong binary exception")
        val operator = expr.operationToken

        return codeBuilder.addPrimitiveBinaryOperation(operator, left, right)
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMVariable {
        val node = expr.node
        return LLVMVariable(node.firstChildNode.text)
    }

    private fun evaluatePsiElement(element: PsiElement): LLVMVariable? {
        return when (element) {
            is LeafPsiElement -> evaluateLeafPsiElement(element)
            is KtConstantExpression -> evaluateConstantExpression(element)
            KtTokens.INTEGER_LITERAL -> null
            else -> null
        }
    }

    private fun evaluateLeafPsiElement(element: LeafPsiElement): LLVMVariable? {
        return when (element.elementType) {
            KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element)
            else -> LLVMVariable("")
        }
    }

    private fun evaluateReturnInstruction(element: LeafPsiElement): LLVMVariable? {
        var next = element.getNextSiblingIgnoringWhitespaceAndComments();
        val retVar = evaluateExpression(next)

        codeBuilder.addLLVMCode("ret i32 ${retVar?.label}")
        return null
    }
}