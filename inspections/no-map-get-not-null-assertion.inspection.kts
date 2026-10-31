import org.intellij.lang.annotations.Language
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

@Language("HTML")
val htmlDescription = """
<html>
<body>
Reports <code>!!</code> applied directly to an indexed access such as <code>map[key]!!</code>.
Use <code>getValue(key)</code> or handle the missing key explicitly.
</body>
</html>
""".trimIndent()

val noMapGetNotNullAssertionInspection = localInspection { psiFile, inspection ->
    val ktFile = psiFile as? KtFile ?: return@localInspection

    val postfixExpressions = PsiTreeUtil.collectElementsOfType(ktFile, KtPostfixExpression::class.java)
    for (postfix in postfixExpressions) {
        if (postfix.operationToken != KtTokens.EXCLEXCL) continue
        val base = postfix.baseExpression?.let { KtPsiUtil.deparenthesize(it) } ?: continue
        if (base !is KtArrayAccessExpression) continue
        inspection.registerProblem(postfix, "Avoid '!!' on indexed access; use getValue() or handle the missing key.")
    }
}

listOf(
    InspectionKts(
        id = "no-map-get-not-null-assertion",
        name = "Not-null Assertion on Indexed Access",
        htmlDescription = htmlDescription,
        level = HighlightDisplayLevel.WARNING,
        localTool = noMapGetNotNullAssertionInspection,
    )
)
