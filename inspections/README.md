# Inspections

Custom [FlexInspect](https://www.jetbrains.com/help/qodana/flexinspect.html) inspections for the Kotlin repository.

IntelliJ IDEA picks up `*.inspection.kts` files from the `inspections/` directory of the project root and highlights
their problems in the editor. Qodana runs those enabled in [`qodana.yaml`](../qodana.yaml) in the project root.

## Adding an inspection

Put each inspection into its own file named after its id: `<id>.inspection.kts`, e.g. `no-map-get-not-null-assertion`.
The file returns a list of `InspectionKts` with that `id`, a `name`, an `htmlDescription` and a `level`.

```kotlin
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

    for (postfix in PsiTreeUtil.collectElementsOfType(ktFile, KtPostfixExpression::class.java)) {
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
```

Then enable the inspection for Qodana by adding its id to `include` in [`qodana.yaml`](../qodana.yaml):

```yaml
include:
  - name: no-map-get-not-null-assertion
```

and list it in [Inspections](#inspections) below.

## Inspections

There are no inspections yet.

| Id | Reports |
|----|---------|
