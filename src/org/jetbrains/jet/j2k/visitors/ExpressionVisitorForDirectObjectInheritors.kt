package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.DummyMethodCallExpression
import org.jetbrains.jet.j2k.ast.DummyStringExpression
import org.jetbrains.jet.j2k.ast.Identifier
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT

public open class ExpressionVisitorForDirectObjectInheritors(converter: Converter): ExpressionVisitor(converter) {
    public override fun visitMethodCallExpression(expression: PsiMethodCallExpression?): Unit {
        val methodExpression = expression?.getMethodExpression()!!
        if (superMethodInvocation(methodExpression, "hashCode")) {
            myResult = DummyMethodCallExpression(Identifier("System"), "identityHashCode", Identifier("this"))
        }
        else if (superMethodInvocation(methodExpression, "equals")) {
            myResult = DummyMethodCallExpression(Identifier("this"), "identityEquals", getConverter().elementToElement(expression?.getArgumentList()))
        }
        else if (superMethodInvocation(methodExpression, "toString")) {
            myResult = DummyStringExpression(String.format("getJavaClass<%s>.getName() + '@' + Integer.toHexString(hashCode())",
                    ExpressionVisitor.getClassName(methodExpression))!!)
        }
        else {
            convertMethodCallExpression(expression!!)
        }
    }

    class object {
        private fun superMethodInvocation(expression: PsiReferenceExpression, methodName: String?): Boolean {
            val referenceName: String? = expression.getReferenceName()
            val qualifierExpression: PsiExpression? = expression.getQualifierExpression()
            if (referenceName == methodName && qualifierExpression is PsiSuperExpression) {
                val `type`: PsiType? = qualifierExpression.getType()
                if (`type` != null && `type`.getCanonicalText() == JAVA_LANG_OBJECT) {
                    return true
                }
            }
            return false
        }
    }
}
