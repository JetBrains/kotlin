package org.jetbrains.kotlin.doc.highlighter

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.psi.*

class HtmlKotlinVisitor: JetTreeVisitor<StringBuilder>() {

    public override fun visitFile(file: PsiFile?) {
        if (file is JetFile) {
            val data = StringBuilder()
            visitJetFile(file, data)
        }
    }
    public override fun visitJetFile(file: JetFile?, data: StringBuilder?): Void? {
        if (file != null) {
            println("============ Jet File ${file.getName()}")
            acceptChildren(file, data)
        }
        return null
    }


    public override fun visitClassObject(classObject: JetClassObject?, data: StringBuilder?): Void? {
        println("============ class $classObject data $data")
        return super.visitClassObject(classObject, data)
    }

    public override fun visitClass(klass: JetClass?, data: StringBuilder?): Void? {
        println("============ class $klass")
        if (klass != null) {
            acceptChildren(klass, data)
            return null
        } else {
            return super.visitClass(klass, data)
        }
    }


    public override fun visitClassBody(classBody: JetClassBody?, data: StringBuilder?): Void? {
        println("============ class body $classBody data $data")
        return super.visitClassBody(classBody, data)
    }


    public override fun visitFunctionType(fnType: JetFunctionType?, data: StringBuilder?): Void? {
        println("======================= function Type $fnType")
        return super.visitFunctionType(fnType, data)
    }

    protected fun accept(child: PsiElement?, data: StringBuilder?): Unit {
            if (child is JetElement) {
                child.accept(this, data)
            } else {
                if (child is PsiComment || child is PsiWhiteSpace) {
                    // ignore
                } else {
                    println("------- Child $child of type ${child.javaClass}")
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