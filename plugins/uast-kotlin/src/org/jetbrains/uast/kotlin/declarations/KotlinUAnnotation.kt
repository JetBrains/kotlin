package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.uast.*

class KotlinUAnnotation(
        override val psi: KtAnnotationEntry,
        override val uastParent: UElement?
) : UAnnotation {
    private val resolvedAnnotation: AnnotationDescriptor? by lz { psi.analyze()[BindingContext.ANNOTATION, psi] }

    private val resolvedCall: ResolvedCall<*>? by lz { psi.getResolvedCall(psi.analyze()) }

    override val qualifiedName: String?
        get() = resolvedAnnotation?.fqName?.asString()

    override val attributeValues: List<UNamedExpression> by lz {
        resolvedCall?.valueArguments?.entries?.mapNotNull {
            val arguments = it.value.arguments
            val name = it.key.name.asString()
            when {
                arguments.size == 1 ->
                    KotlinUNamedExpression.create(name, arguments.first(), this)
                arguments.size > 1 ->
                    KotlinUNamedExpression.create(name, arguments, this)
                else -> null
            }
        } ?: emptyList()
    }

    override fun resolve(): PsiClass? {
        val descriptor = resolvedAnnotation?.annotationClass ?: return null
        return descriptor.toSource()?.getMaybeLightElement(this) as? PsiClass
    }

    override fun findAttributeValue(name: String?): UExpression? =
            findDeclaredAttributeValue(name)

    override fun findDeclaredAttributeValue(name: String?): UExpression? {
        return attributeValues.find {
            it.name == name ||
            (name == null && it.name == "value") ||
            (name == "value" && it.name == null)
        }?.expression
    }

}

