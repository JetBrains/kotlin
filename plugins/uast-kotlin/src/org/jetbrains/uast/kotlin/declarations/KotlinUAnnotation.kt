package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod

class KotlinUAnnotation(
        override val psi: KtAnnotationEntry,
        givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UAnnotation {

    override val javaPsi = psi.toLightAnnotation()

    override val sourcePsi = psi

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
            findDeclaredAttributeValue(name) ?: findAttributeDefaultValue(name ?: "value")

    fun findAttributeValueExpression(arg: ValueArgument): UExpression? {
        val mapping = resolvedCall?.getArgumentMapping(arg)
        return (mapping as? ArgumentMatch)?.let { match ->
            val namedExpression = attributeValues.find { it.name == match.valueParameter.name.asString() }
            namedExpression?.expression as? KotlinUVarargExpression ?: namedExpression
        }
    }

    override fun findDeclaredAttributeValue(name: String?): UExpression? {
        return attributeValues.find {
            it.name == name ||
            (name == null && it.name == "value") ||
            (name == "value" && it.name == null)
        }?.expression
    }

    private fun findAttributeDefaultValue(name: String): UExpression? {
        val parameter = resolvedAnnotation
                                ?.annotationClass
                                ?.unsubstitutedPrimaryConstructor
                                ?.valueParameters
                                ?.find { it.name.asString() == name } ?: return null

        val defaultValue = (parameter.source.getPsi() as? KtParameter)?.defaultValue ?: return null
        return getLanguagePlugin().convertWithParent(defaultValue)
    }

    override fun convertParent(): UElement? {
        val superParent = super.convertParent() ?: return null
        if (psi.useSiteTarget?.getAnnotationUseSiteTarget() == AnnotationUseSiteTarget.RECEIVER) {
            (superParent.uastParent as? KotlinUMethod)?.uastParameters?.firstIsInstance<KotlinReceiverUParameter>()?.let {
                return it
            }
        }
        return superParent
    }
}

