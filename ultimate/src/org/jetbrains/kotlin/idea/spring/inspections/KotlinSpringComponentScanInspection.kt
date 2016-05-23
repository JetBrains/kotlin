package org.jetbrains.kotlin.idea.spring.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.spring.model.highlighting.jam.SpringComponentScanInspection
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.registerWithElementsUnwrapped
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitorVoid

class KotlinSpringComponentScanInspection : AbstractKotlinInspection() {
    private val javaInspection by lazy {
        object : SpringComponentScanInspection() {
            // make base method visible
            override public fun checkClass(aClass: PsiClass, holder: ProblemsHolder, module: Module) = super.checkClass(aClass, holder, module)
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object: KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                val module = ModuleUtilCore.findModuleForPsiElement(classOrObject) ?: return
                val lightClass = classOrObject.toLightClass() ?: return
                val tmpHolder = ProblemsHolder(holder.manager, classOrObject.containingFile, isOnTheFly)
                javaInspection.checkClass(lightClass, tmpHolder, module)
                tmpHolder.resultsArray.registerWithElementsUnwrapped(holder, isOnTheFly)
            }
        }
    }
}