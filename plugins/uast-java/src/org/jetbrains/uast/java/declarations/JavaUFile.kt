package org.jetbrains.uast.java

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.uast.UComment
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastLanguagePlugin
import java.util.*

class JavaUFile(override val psi: PsiJavaFile, override val languagePlugin: UastLanguagePlugin) : UFile {
    override val packageName: String
        get() = psi.packageName
    
    override val imports by lz {
        psi.importList?.allImportStatements?.map { JavaUImportStatement(it, this) } ?: listOf() 
    }

    override val classes by lz { psi.classes.map { JavaUClass.create(it, this) } }

    override val allCommentsInFile by lz {
        val comments = ArrayList<UComment>(0)
        psi.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitComment(comment: PsiComment) {
                comments += UComment(comment, this@JavaUFile)
            }
        })
        comments
    }

    override fun equals(other: Any?) = (other as? JavaUFile)?.psi == psi
}