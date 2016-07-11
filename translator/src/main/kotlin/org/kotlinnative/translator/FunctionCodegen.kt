package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.resolve.BindingContext
import org.kotlinnative.translator.debug.printFunction
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMIntType
import org.kotlinnative.translator.llvm.types.parseLLVMType
import org.kotlinnative.translator.utils.FunctionArgument
import java.util.*


class FunctionCodegen(val state: TranslationState, val function: KtNamedFunction, val codeBuilder: LLVMBuilder) {

    var name = function.fqName.toString()
    var returnType: String
    var args: List<FunctionArgument>?

    init {
        val descriptor = state.bindingContext.get(BindingContext.FUNCTION, function)
        args = descriptor?.valueParameters?.map {
            FunctionArgument(LLVMMapStandardType(it.type.toString()), it.name.toString())
        }

        returnType = LLVMMapStandardType(descriptor?.returnType.toString())
    }

    fun generate() {
        generateDeclaration(function)
        codeBuilder.addStartExpression()
        expressionWalker(function.bodyExpression)
        codeBuilder.addEndExpression()
    }

    private fun generateDeclaration(function: KtNamedFunction) {
        codeBuilder.addLLVMCode(LLVMDescriptorGenerate(function.fqName.toString(), args, returnType))
    }

    private fun expressionWalker(expr: PsiElement?) {
        when (expr) {
            is KtBlockExpression -> expressionWalker(expr.firstChild)
            is KtProperty -> evaluateLeafPsiElement(expr.firstChild as LeafPsiElement)
            is PsiElement -> evaluateExpression(expr.firstChild)
            null -> return
            else -> UnsupportedOperationException()
        }

        expressionWalker(expr?.getNextSiblingIgnoringWhitespaceAndComments())
    }

    private fun evaluateExpression(expr: PsiElement?): LLVMNode? {
        return when (expr) {
            is KtBinaryExpression -> evaluateBinaryExpression(expr)
            is KtConstantExpression -> evaluateConstantExpression(expr)
            is KtCallExpression -> evaluateCallExpression(expr)
            is PsiWhiteSpace -> null
            is PsiElement -> evaluatePsiElement(expr)
            null -> null
            else -> throw UnsupportedOperationException()
        }
    }

    private fun evaluateCallExpression(expr: KtCallExpression): LLVMNode? {
        val function = expr.firstChild.firstChild
        val descriptor = state.functions[function.text] ?: return null
        val names = parseArgList(expr
                .firstChild
                .getNextSiblingIgnoringWhitespaceAndComments()
                ?.firstChild)

        return LLVMCall(descriptor.returnType, function.text, descriptor.argTypes.mapIndexed { i: Int, type: String -> LLVMVariable(names[i], parseLLVMType(type)) })
    }

    private fun parseArgList(argumentList: PsiElement?): List<String> {
        val args = ArrayList<String>()

        var currentArg = argumentList?.getNextSiblingIgnoringWhitespaceAndComments()

        while (currentArg?.text != ")" && currentArg != null) {
            args.add(currentArg?.text)

            currentArg = currentArg
                    ?.getNextSiblingIgnoringWhitespaceAndComments()
                    ?.getNextSiblingIgnoringWhitespaceAndComments()
        }
        return args
    }

    private fun evaluateBinaryExpression(expr: KtBinaryExpression): LLVMNode {
        val left = evaluateExpression(expr.firstChild) as LLVMVariable? ?: throw UnsupportedOperationException("Wrong binary exception")
        val right = evaluateExpression(expr.lastChild) as LLVMVariable? ?: throw UnsupportedOperationException("Wrong binary exception")
        val operator = expr.operationToken

        return codeBuilder.addPrimitiveBinaryOperation(operator, left, right)
    }

    private fun evaluateConstantExpression(expr: KtConstantExpression): LLVMVariable {
        val node = expr.node
        return LLVMVariable(node.firstChildNode.text, ::LLVMIntType.invoke())
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
            KtTokens.VAL_KEYWORD -> evaluateValExpression(element)
            KtTokens.VAR_KEYWORD -> evaluateValExpression(element)
            else -> null
        }
    }

    private fun evaluateValExpression(element: LeafPsiElement): LLVMVariable? {
        val identifier = element.getNextSiblingIgnoringWhitespaceAndComments()
        val eq = identifier?.getNextSiblingIgnoringWhitespaceAndComments() ?: return null

        val assignExpression = evaluateExpression(eq?.getNextSiblingIgnoringWhitespaceAndComments()) ?: return null
        codeBuilder.addAssignment(LLVMVariable(identifier!!.text), assignExpression)
        return null
    }

    private fun evaluateReturnInstruction(element: LeafPsiElement): LLVMVariable? {
        var next = element.getNextSiblingIgnoringWhitespaceAndComments()
        val retVar = evaluateExpression(next) as LLVMVariable

        codeBuilder.addLLVMCode("ret i32 ${retVar?.label}")
        return null
    }
}