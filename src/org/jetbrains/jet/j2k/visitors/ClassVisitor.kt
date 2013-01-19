package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import java.util.HashSet

public open class ClassVisitor(): JavaRecursiveElementVisitor() {
    private val myClassIdentifiers = HashSet<String>()

    public open fun getClassIdentifiers(): Set<String> {
        return HashSet<String>(myClassIdentifiers)
    }

    public override fun visitClass(aClass: PsiClass?): Unit {
        val qName = aClass?.getQualifiedName()
        if (qName != null) {
            myClassIdentifiers.add(qName)
        }
        super.visitClass(aClass)
    }
}
