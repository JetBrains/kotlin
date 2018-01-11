package org.jetbrains.kotlin.idea.spring.inspections

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.template.*
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PropertyUtil
import com.intellij.spring.CommonSpringModel
import com.intellij.spring.SpringBundle
import com.intellij.spring.model.SpringBeanPointer
import com.intellij.spring.model.converters.SpringConverterUtil
import com.intellij.spring.model.highlighting.autowire.SpringJavaInjectionPointsAutowiringInspection
import com.intellij.spring.model.utils.SpringAutowireUtil
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.registerWithElementsUnwrapped
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class SpringKotlinAutowiringInspection : AbstractKotlinInspection() {
    // Based on SpringJavaInjectionPointsAutowiringInspection.AddSpringBeanQualifierFix
    class AddQualifierFix(
            modifierListOwner: KtModifierListOwner,
            private val beanPointers: Collection<SpringBeanPointer<*>>,
            private val annotationFqName: String
    ) : LocalQuickFix {
        private val elementPointer = modifierListOwner.createSmartPointer()

        override fun getName() = SpringBundle.message("SpringAutowiringInspection.add.qualifier.fix")

        override fun getFamilyName() = name

        private fun getQualifierNamesSuggestNamesExpression(expression: KtStringTemplateExpression): Expression {
            return object : Expression() {
                override fun calculateResult(context: ExpressionContext): Result? {
                    PsiDocumentManager.getInstance(context.project).commitAllDocuments()
                    return TextResult(expression.plainContent)
                }

                override fun calculateQuickResult(context: ExpressionContext) = this.calculateResult(context)

                override fun calculateLookupItems(context: ExpressionContext): Array<LookupElement>? {
                    PsiDocumentManager.getInstance(context.project).commitAllDocuments()
                    return beanPointers.sortedBy { it.name ?: "" }.mapNotNull { SpringConverterUtil.createCompletionVariant(it) }.toTypedArray()
                }
            }
        }

        private fun createQualifierNameTemplate(expression: KtStringTemplateExpression): Template {
            val builder = TemplateBuilderImpl(expression.containingFile)
            builder.replaceRange(
                    expression.getContentRange().shiftRight(expression.startOffset),
                    getQualifierNamesSuggestNamesExpression(expression)
            )
            return builder.buildInlineTemplate()
        }

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val modifierListOwner = elementPointer.element ?: return
            if (!FileModificationService.getInstance().preparePsiElementForWrite(modifierListOwner)) return
            if (beanPointers.isEmpty()) return

            val defaultBeanName = with(beanPointers.first().name) { if (isNullOrBlank()) "Unknown" else this }
            val entry = KtPsiFactory(project).createAnnotationEntry("@$annotationFqName(\"$defaultBeanName\")")
            val addedEntry = modifierListOwner.addAnnotationEntry(entry)

            ShortenReferences.DEFAULT.process(addedEntry)

            val stringTemplate = addedEntry.valueArguments.first().getArgumentExpression() as? KtStringTemplateExpression ?: return
            val virtualFile = modifierListOwner.containingFile.virtualFile!!
            val editor = FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, virtualFile, 0), false)!!
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            TemplateManager.getInstance(project).startTemplate(editor, createQualifierNameTemplate(stringTemplate))
        }
    }

    private val javaInspection by lazy { SpringJavaInjectionPointsAutowiringInspection() }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        // TODO: SpringJavaInjectionPointsAutowiringInspection.checkAutowiredMethod() is not accessible here
        private fun checkAutowiredMethod(psiMethod: PsiMethod, holder: ProblemsHolder, springModel: CommonSpringModel, required: Boolean) {
            val resourceAnnotation = SpringAutowireUtil.getResourceAnnotation(psiMethod)
            when {
                resourceAnnotation != null -> {
                    val propertyType = PropertyUtil.getPropertyType(psiMethod) ?: return
                    SpringJavaInjectionPointsAutowiringInspection.checkInjectionPoint(psiMethod, propertyType, holder, springModel, required)
                }
                psiMethod.parameterList.parametersCount == 0 &&
                SpringAutowireUtil.isAutowiredByAnnotation(psiMethod) -> {
                    val nameIdentifier = psiMethod.nameIdentifier ?: return
                    val message = SpringBundle.message("bean.autowiring.by.type.no.parameter.for.autowired.method",
                                                       if (psiMethod.isConstructor) "constructor" else "method")
                    holder.registerProblem(nameIdentifier, message)
                }
                else -> {
                    for (parameter in psiMethod.parameterList.parameters) {
                        if (AnnotationUtil.isAnnotated(parameter, "org.springframework.beans.factory.annotation.Value", true)) continue
                        SpringJavaInjectionPointsAutowiringInspection.checkInjectionPoint(parameter, parameter.type, holder, springModel, required)
                    }
                }
            }
        }

        private fun Array<ProblemDescriptor>.registerAdjustedProblems() {
            registerWithElementsUnwrapped(holder, isOnTheFly) { qf, element ->
                // Can't access AddSpringBeanQualifierFix class directly

                val klass = qf.javaClass
                if (!klass.name.endsWith("AddSpringBeanQualifierFix")) return@registerWithElementsUnwrapped qf

                try {
                    val fields = klass.declaredFields
                    @Suppress("UNCHECKED_CAST")
                    val beanPointers = fields[1].apply { isAccessible = true }.get(qf) as Collection<SpringBeanPointer<*>>
                    val annotationFqName = fields[2].apply { isAccessible = true }.get(qf) as String
                    val modifierListOwner = element.getNonStrictParentOfType<KtModifierListOwner>()
                                            ?: return@registerWithElementsUnwrapped qf
                    AddQualifierFix(modifierListOwner, beanPointers, annotationFqName)
                }
                catch (e: Exception) {
                    return@registerWithElementsUnwrapped null
                }
            }
        }

        private fun <T : PsiMember> T.processLightMember(
                action: T.(holder: ProblemsHolder, model: CommonSpringModel, required: Boolean) -> Unit
        ) {
            val model = SpringAutowireUtil.getProcessingSpringModel(containingClass) ?: return
            val required = SpringAutowireUtil.isRequired(this)
            val tmpHolder = ProblemsHolder(holder.manager, containingFile, isOnTheFly)
            action(this, tmpHolder, model, required)
            tmpHolder.resultsArray.registerAdjustedProblems()
        }

        private fun PsiMethod.processLightMethod() {
            if (!SpringAutowireUtil.isInjectionPoint(this)) return
            processLightMember { holder, model, required -> checkAutowiredMethod(this, holder, model, required) }
        }

        private fun PsiField.processLightField() {
            if (!SpringAutowireUtil.isAutowiredByAnnotation(this)) return
            processLightMember { holder, model, required ->
                SpringJavaInjectionPointsAutowiringInspection.checkInjectionPoint(this, type, holder, model, required)
            }
        }

        private fun PsiClass.processLightClass() {
            javaInspection.checkClass(this, holder.manager, isOnTheFly)?.registerAdjustedProblems()
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            function.toLightMethods().firstOrNull()?.processLightMethod()
        }

        override fun visitProperty(property: KtProperty) {
            if (property.name != null) // It is here because `lightElement.name` returns `<no name provided>` instead of `null` suddenly
                for (lightElement in property.toLightElements()) {
                    when (lightElement) {
                        is KtLightMethod -> lightElement.processLightMethod()
                        is KtLightField -> lightElement.processLightField()
                    }
                }
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            classOrObject.toLightClass()?.processLightClass()
        }
    }
}