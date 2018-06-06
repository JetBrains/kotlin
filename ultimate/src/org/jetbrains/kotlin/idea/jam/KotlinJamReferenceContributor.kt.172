package org.jetbrains.kotlin.idea.jam

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.jam.JamService
import com.intellij.jam.JamStringAttributeElement
import com.intellij.jam.reflect.JamStringAttributeMeta
import com.intellij.psi.PsiElementRef
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.references.AbstractKotlinReferenceContributor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.parents

// Based on the JamReferenceContributor
class KotlinJamReferenceContributor : AbstractKotlinReferenceContributor() {
    @Suppress("UNCHECKED_CAST")
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerMultiProvider<KtStringTemplateExpression> { expression ->
            if (!expression.isPlain()) return@registerMultiProvider PsiReference.EMPTY_ARRAY
            val argument = expression
                                   .parents
                                   .filterIsInstance<KtValueArgument>()
                                   .firstOrNull() { it.parent?.parent is KtAnnotationEntry } ?: return@registerMultiProvider PsiReference.EMPTY_ARRAY
            val annotationEntry = argument.parent.parent as KtAnnotationEntry
            val lightAnnotation = (CompletionUtil.getOriginalOrSelf(annotationEntry)).toLightAnnotation()
                                  ?: return@registerMultiProvider PsiReference.EMPTY_ARRAY

            val service = JamService.getJamService(expression.project)
            val annotationMeta = service.getMeta(lightAnnotation)
                                 ?: return@registerMultiProvider PsiReference.EMPTY_ARRAY
            val attributeName = argument.getArgumentName()?.asName?.asString()
            val attribute = annotationMeta.findAttribute(attributeName) as? JamStringAttributeMeta<Any, Any>
                            ?: return@registerMultiProvider PsiReference.EMPTY_ARRAY
            val jam = attribute.getJam(PsiElementRef.real(lightAnnotation))
            val converter = attribute.converter
            when (jam) {
                is JamStringAttributeElement<*> -> converter.createReferences(jam as JamStringAttributeElement<Any>)
                is List<*> -> {
                    val list = SmartList<PsiReference>()
                    for (item in jam) {
                        val jamElement = item as? JamStringAttributeElement<Any> ?: continue
                        if (jamElement.psiElement?.unwrapped != expression) continue
                        list += converter.createReferences(jamElement)
                    }
                    list.toTypedArray()
                }
                else -> PsiReference.EMPTY_ARRAY
            }
        }
    }
}
