package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.declarations.KotlinUIdentifier
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod

abstract class KotlinUAnnotationBase(
    final override val psi: KtElement,
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UAnnotationEx, UAnchorOwner {

    abstract override val javaPsi: PsiAnnotation?

    final override val sourcePsi = psi

    protected abstract fun annotationUseSiteTarget(): AnnotationUseSiteTarget?

    private val resolvedCall: ResolvedCall<*>? by lz { psi.getResolvedCall(psi.analyze()) }

    override val qualifiedName: String?
        get() = annotationClassDescriptor.takeUnless(ErrorUtils::isError)
            ?.fqNameUnsafe
            ?.takeIf(FqNameUnsafe::isSafe)
            ?.toSafe()
            ?.toString()

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

    protected abstract val annotationClassDescriptor: ClassDescriptor?

    override fun resolve(): PsiClass? {
        val descriptor = annotationClassDescriptor ?: return null
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
        val parameter = annotationClassDescriptor
            ?.unsubstitutedPrimaryConstructor
            ?.valueParameters
            ?.find { it.name.asString() == name } ?: return null

        val defaultValue = (parameter.source.getPsi() as? KtParameter)?.defaultValue ?: return null
        return getLanguagePlugin().convertWithParent(defaultValue)
    }

    override fun convertParent(): UElement? {
        val superParent = super.convertParent() ?: return null
        if (annotationUseSiteTarget() == AnnotationUseSiteTarget.RECEIVER) {
            (superParent.uastParent as? KotlinUMethod)?.uastParameters?.firstIsInstance<KotlinReceiverUParameter>()?.let {
                return it
            }
        }
        return superParent
    }
}

class KotlinUAnnotation(
    val annotationEntry: KtAnnotationEntry,
    givenParent: UElement?
) : KotlinUAnnotationBase(annotationEntry, givenParent), UAnnotation {

    override val javaPsi = annotationEntry.toLightAnnotation()

    private val resolvedAnnotation: AnnotationDescriptor? by lz { annotationEntry.analyze()[BindingContext.ANNOTATION, annotationEntry] }

    override val annotationClassDescriptor: ClassDescriptor?
        get() = resolvedAnnotation?.annotationClass

    override fun annotationUseSiteTarget() = annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()

    override val uastAnchor by lazy {
        KotlinUIdentifier(
            javaPsi?.nameReferenceElement,
            annotationEntry.typeReference?.typeElement?.let {
                (it as? KtUserType)?.referenceExpression?.getReferencedNameElement() ?: it.navigationElement
            },
            this
        )
    }

}

class KotlinUNestedAnnotation(
    private val original: KtCallExpression,
    givenParent: UElement?,
    private val classDescriptor: ClassDescriptor?
) : KotlinUAnnotationBase(original, givenParent) {
    override val javaPsi: PsiAnnotation? by lazy { original.toLightAnnotation() }
    override val annotationClassDescriptor: ClassDescriptor?
        get() = classDescriptor

    override fun annotationUseSiteTarget(): AnnotationUseSiteTarget? = null

    override val uastAnchor by lazy {
        KotlinUIdentifier(
            javaPsi?.nameReferenceElement?.referenceNameElement,
            (original.calleeExpression as? KtNameReferenceExpression)?.getReferencedNameElement(),
            this
        )
    }

}


