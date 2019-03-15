package org.jetbrains.kotlin.idea.spring.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.jam.NonPhysicalReferenceWrapper
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiPackageReference
import com.intellij.spring.model.jam.stereotype.SpringComponentScan
import com.intellij.spring.model.jam.utils.SpringJamUtils
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotationForSourceEntry
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtVisitorVoid

class KotlinSpringComponentScanInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object: KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                val lightClass = classOrObject.toLightClass() ?: return
                val beansPackagesScan = SpringJamUtils.getInstance().getBeansPackagesScan(lightClass)
                        .filterIsInstance<SpringComponentScan>()
                val kotlinAnnotations = beansPackagesScan
                        .mapNotNull { (it.annotation as? KtLightAnnotationForSourceEntry)?.kotlinOrigin }
                if (kotlinAnnotations.isEmpty()) return
                val literalVisitor = PsiPackageReferenceChecker(holder)
                for (kotlinAnnotation in kotlinAnnotations) {
                    kotlinAnnotation.accept(literalVisitor)
                }
            }
        }
    }

    private class PsiPackageReferenceChecker(private val holder: ProblemsHolder) : KtTreeVisitorVoid() {
        override fun visitStringTemplateEntry(entry: KtStringTemplateEntry) {
            for (reference in entry.references) {
                val packageReference = psiPackageReference(reference) ?: continue
                if (packageReference.multiResolve(false).isEmpty()) {
                    holder.registerProblem(reference, //registering on physical reference
                                           ProblemsHolder.unresolvedReferenceMessage(packageReference), //but getting message from logical reference
                                           ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                }
            }
        }

        private fun psiPackageReference(reference: PsiReference): PsiPackageReference? = when (reference) {
            is PsiPackageReference -> reference
            is NonPhysicalReferenceWrapper -> psiPackageReference(reference.wrappedReference)
            else -> null
        }
    }

}