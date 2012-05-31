package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import java.util.HashSet
import java.util.Set

public open class ClassVisitor(): JavaRecursiveElementVisitor() {
    private val myClassIdentifiers: Set<String> = HashSet<String>()
    public open fun getClassIdentifiers(): Set<String> {
        return HashSet<String>(myClassIdentifiers)
    }

    public override fun visitClass(aClass: PsiClass?): Unit {
        myClassIdentifiers.add(aClass?.getQualifiedName()!!)
        super.visitClass(aClass)
    }
}
