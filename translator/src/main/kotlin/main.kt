package hello

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.getModuleName
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.resolve.    AnalyzingUtils
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.kotlin.native.translator.debug.debugPrintNode
import org.jetbrains.kotlin.lexer.KtToken
import  java.lang.StringBuilder
import java.io.File
import java.util.*
import java.util.logging.Logger


var llvmCode = llvmBuilder()

fun expressionWalker(expr : Any?) {
    when(expr){
        is KtBlockExpression -> evaluateBlockExpression(expr)
        is PsiElement -> evaluatePsiExpression(expr)
        null -> Unit
        else -> UnsupportedOperationException()
    }
}


fun evaluateBlockExpression(expr : KtBlockExpression){
    var element = expr
    expressionWalker(element.firstChild)

    expressionWalker(element.getNextSiblingIgnoringWhitespaceAndComments())
}

var variableCount = 0
fun evaluatePsiExpression(expr : PsiElement){
    var element = expr
    evaluateExpression(element.firstChild)

    evaluatePsiExpression(element.getNextSiblingIgnoringWhitespaceAndComments() ?: return)
}

fun evaluateExpression(expr: Any?) : llvmVariable?{
    return when(expr){
        is KtBinaryExpression -> evaluateBinaryExpression(expr)
        is PsiWhiteSpace -> null
        is PsiElement -> evaluatePsiElement(expr)
        is KtConstantExpression -> evaluateConstantExpression(expr)
        null -> null
        else ->  throw UnsupportedOperationException()
    }
}

fun evaluateBinaryExpression(expr : KtBinaryExpression) : llvmVariable{
    val left = evaluateExpression(expr.firstChild)
    val right = evaluateExpression(expr.lastChild)
    val operator = expr.operationToken



    val llvmOperator = when (operator) {
        KtTokens.PLUS -> "add nsw i32"
        KtTokens.MINUS -> "sub nsw i32"
        KtTokens.MUL   -> "mul nsw i32"
        else -> throw UnsupportedOperationException("Unkbown binary operator")
    }

    variableCount++
    llvmCode.addLlvmCode("%var$variableCount = $llvmOperator ${left?.label}, ${right?.label}")
    return llvmVariable("%var$variableCount")
}

fun evaluateConstantExpression(expr : KtConstantExpression) : llvmVariable {
    val node = expr.node
    return llvmVariable(node.firstChildNode.text)
}

fun evaluatePsiElement(element : PsiElement) : llvmVariable? {
    return when(element){
        is LeafPsiElement -> evaluateLeafPsiElement(element)
        is KtConstantExpression -> evaluateConstantExpression(element)
        KtTokens.INTEGER_LITERAL -> null
        else -> null
    }
}

fun evaluateLeafPsiElement(element : LeafPsiElement) : llvmVariable? {
    return when(element.elementType){
        KtTokens.RETURN_KEYWORD -> evaluateReturnInstruction(element)
        else -> llvmVariable("")
    }

}

fun evaluateReturnInstruction(element : LeafPsiElement) : llvmVariable?{
    var next  = element.getNextSiblingIgnoringWhitespaceAndComments();
    val retVar = evaluateExpression(next)

    llvmCode.addLlvmCode("ret i32 ${retVar?.label}")
    return null
}

fun main(args: Array<String>) {
    val scriptFile = "/home/user/Kotlin/manual/sum.kt"

    val parser = KotlinScriptParser()

    val analyzeContext = parser.parse(scriptFile)

    val function = analyzeContext.functions.keys.first()
    var body = function.bodyExpression as KtBlockExpression

    llvmCode.addLlvmCode("define i32 @${function.fqName}() #0{")

    expressionWalker(body);

    llvmCode.addLlvmCode("}")
    println(llvmCode)


}