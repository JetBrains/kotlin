package org.jetbrains.kotlin.doc.highlighter

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.psi.*

class HtmlKotlinVisitor: JetTreeVisitor<StringBuilder>() {

    override fun visitFile(file: PsiFile?) {
        if (file is JetFile) {
            visitJetFile(file, StringBuilder())
        }
    }

    override fun visitJetFile(file: JetFile, data: StringBuilder?): Void? {
        println("============ Jet File ${file.getName()}")
        acceptChildren(file, data)
        return null
    }

    override fun visitClassObject(classObject: JetClassObject, data: StringBuilder?): Void? {
        println("============ class $classObject data $data")
        return super.visitClassObject(classObject, data)
    }

    override fun visitClass(klass: JetClass, data: StringBuilder?): Void? {
        println("============ class $klass")
        acceptChildren(klass, data)
        return null
    }

    override fun visitClassBody(classBody: JetClassBody, data: StringBuilder?): Void? {
        println("============ class body $classBody data $data")
        return super.visitClassBody(classBody, data)
    }

    override fun visitFunctionType(`type`: JetFunctionType, data: StringBuilder?): Void? {
        println("======================= function Type $`type`")
        return super.visitFunctionType(`type`, data)
    }

    protected fun accept(child: PsiElement?, data: StringBuilder?): Unit {
            if (child is JetElement) {
                child.accept(this, data)
            } else {
                if (child is PsiComment || child is PsiWhiteSpace) {
                    // ignore
                } else {
                    println("------- Child $child of type ${child?.javaClass}")
                }
                child?.accept(this)
            }
    }

    protected fun acceptChildren(element: PsiElement, data: StringBuilder?): Unit {
        for (child in element.getChildren()) {
            accept(child, data)
        }
    }
}