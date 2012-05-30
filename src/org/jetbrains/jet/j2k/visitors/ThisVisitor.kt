package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.annotations.Nullable
import java.util.Set
import java.util.LinkedHashSet

public open class ThisVisitor(): JavaRecursiveElementVisitor() {
    private val myResolvedConstructors: Set<PsiMethod> = LinkedHashSet<PsiMethod>()

    public override fun visitReferenceExpression(expression: PsiReferenceExpression?): Unit {
        for (r : PsiReference? in expression?.getReferences()) {
            if (r?.getCanonicalText() == "this") {
                val res: PsiElement? = r?.resolve()
                if (res is PsiMethod && res.isConstructor()) {
                    myResolvedConstructors.add(res)
                }
            }
        }
    }

    public open fun getPrimaryConstructor(): PsiMethod? {
        if (myResolvedConstructors.size() > 0) {
            val first: PsiMethod = myResolvedConstructors.iterator().next()
            for (m in myResolvedConstructors)
                if (m.hashCode() != first.hashCode()) {
                    return null
                }

            return first
        }
        return null
    }
}
