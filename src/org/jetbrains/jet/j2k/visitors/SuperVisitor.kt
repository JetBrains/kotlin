package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import java.util.HashSet

public open class SuperVisitor(): JavaRecursiveElementVisitor() {
    public val resolvedSuperCallParameters: HashSet<PsiExpressionList> = hashSet()

    public override fun visitMethodCallExpression(expression: PsiMethodCallExpression?): Unit {
        if (expression != null && isSuper(expression.getMethodExpression())) {
            resolvedSuperCallParameters.add(expression.getArgumentList())
        }
    }
    class object {
        open fun isSuper(r: PsiReference): Boolean {
            if (r.getCanonicalText().equals("super")) {
                val baseConstructor: PsiElement? = r.resolve()
                if (baseConstructor != null && baseConstructor is PsiMethod && baseConstructor.isConstructor()) {
                    return true
                }
            }

            return false
        }
    }
}
