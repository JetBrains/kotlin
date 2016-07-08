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
import org.kotlin.native.translator.llvm.LLVMBuilder
import org.kotlin.native.translator.llvm.LLVMVariable
import org.kotlinnative.translator.llvm.LLVMDescriptorGenearte
import org.kotlinnative.translator.llvm.LLVMMapStandardType


class FunctionCodegen(val state: TranslationState, val function: KtNamedFunction, val codeBuilder: LLVMBuilder) {

    private var variableCount = 0

    fun generate() {
        generateDeclaration(function)
        codeBuilder.addLlvmCode("{")
        expressionWalker(function.bodyExpression)
        codeBuilder.addLlvmCode("}")
    }

    private fun generateDeclaration(function: KtNamedFunction) {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)
        var args = descriptor?.valueParameters?.map {
            Pair(it.name.toString(), LLVMMapStandardType(it.type.toString()))
        }
        val returnType = LLVMMapStandardType(descriptor?.returnType.toString())


        codeBuilder.addLlvmCode(LLVMDescriptorGenearte(function.fqName.toString(), args, returnType))
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
        var element = expr
        expressionWalker(element.firstChild)

        expressionWalker(element.getNextSiblingIgnoringWhitespaceAndComments())
    }

    private fun evaluatePsiExpression(expr: PsiElement) {
        var element = expr
        evaluateExpression(element.firstChild)

        evaluatePsiExpression(element.getNextSiblingIgnoringWhitespaceAndComments() ?: return)
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
        val left = evaluateExpression(expr.firstChild)
        val right = evaluateExpression(expr.lastChild)
        val operator = expr.operationToken


        val llvmOperator = when (operator) {
            KtTokens.PLUS -> "add nsw i32"
            KtTokens.MINUS -> "sub nsw i32"
            KtTokens.MUL -> "mul nsw i32"
            else -> throw UnsupportedOperationException("Unkbown binary operator")
        }

        variableCount++
        codeBuilder.addLlvmCode("%var$variableCount = $llvmOperator ${left?.label}, ${right?.label}")
        return LLVMVariable("%var$variableCount")
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

        codeBuilder.addLlvmCode("ret i32 ${retVar?.label}")
        return null
    }
}