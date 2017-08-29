/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.spring.generate

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInspection.SmartHashMap
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.spring.CommonSpringModel
import com.intellij.spring.model.CommonSpringBean
import com.intellij.spring.model.SpringBeanPointer
import com.intellij.spring.model.SpringModelSearchParameters
import com.intellij.spring.model.actions.generate.GenerateSpringBeanDependenciesUtil
import com.intellij.spring.model.actions.generate.GenerateSpringBeanDependenciesUtil.*
import com.intellij.spring.model.actions.generate.SpringBeanClassMember
import com.intellij.spring.model.highlighting.xml.SpringConstructorArgResolveUtil

import com.intellij.spring.model.utils.SpringBeanCoreUtils
import com.intellij.spring.model.utils.SpringBeanUtils
import com.intellij.spring.model.utils.SpringModelSearchers
import com.intellij.spring.model.utils.SpringModelUtils
import com.intellij.spring.model.xml.DomSpringBean
import com.intellij.spring.model.xml.beans.Beans
import com.intellij.spring.model.xml.beans.SpringBean
import com.intellij.util.IncorrectOperationException
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.completion.BasicLookupElementFactory
import org.jetbrains.kotlin.idea.completion.InsertHandlerProvider
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.idea.editor.BatchTemplateRunner
import org.jetbrains.kotlin.idea.spring.beanClass
import org.jetbrains.kotlin.idea.spring.effectiveBeanClasses
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.load.java.propertyNameBySetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.TypeReconstructionUtil
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

// TODO: GenerateSpringBeanDependenciesUtil.ensureFileWritable() is not accessible here
private fun DomElement.ensureFileWritable() = ensureFileWritable(DomUtil.getFile(this).virtualFile, manager.project)

// TODO: GenerateSpringBeanDependenciesUtil.getReferenceName() is not accessible here
private fun getReferencedName(currentBean: SpringBean, bean: SpringBeanPointer<CommonSpringBean>): String? {
    val model = SpringModelUtils.getInstance().getSpringModel(currentBean)
    return SpringBeanCoreUtils.getReferencedName(bean, model.allCommonBeans)
}

// TODO: GenerateSpringBeanDependenciesUtil.getExistedSetter() is not accessible here
private fun getExistedSetter(currentBeanClass: PsiClass, setterPsiClass: PsiClass): PsiMethod? {
    val psiClassType = JavaPsiFacade.getInstance(setterPsiClass.project).elementFactory.createType(setterPsiClass)
    return currentBeanClass.allMethods.firstOrNull {
        PropertyUtil.isSimplePropertySetter(it) && it.parameterList.parameters.first().type.isAssignableFrom(psiClassType)
    }
}

// TODO: GenerateSpringBeanDependenciesUtil.findConstructor() is not accessible here
private fun findConstructor(constructors: Array<PsiMethod>, psiParameterTypes: List<PsiType>): PsiMethod? {
    return constructors.firstOrNull {
        val parameters = it.parameterList.parameters
        if (it.parameterList.parametersCount != psiParameterTypes.size) return@firstOrNull false
        (psiParameterTypes zip parameters).all { it.first.isAssignableFrom(it.second.type) }
    }
}

// TODO: GenerateSpringBeanDependenciesUtil.findExistedConstructor() is not accessible here
private fun findExistedConstructor(
        currentBean: SpringBean,
        currentBeanClass: PsiClass,
        candidateParameterClasses: Array<out PsiClass>
): PsiMethod? {
    val constructors = SpringConstructorArgResolveUtil.findMatchingMethods(currentBean)
    for (candidateBeanClass in candidateParameterClasses) {
        for (constructor in constructors) {
            val psiParameterTypes = SmartList<PsiType>().apply {
                constructor.parameterList.parameters.mapTo(this) { it.type }
                add(PsiTypesUtil.getClassType(candidateBeanClass))
            }
            findConstructor(currentBeanClass.constructors, psiParameterTypes)?.let { return it }
        }
    }

    return null
}

// TODO: GenerateSpringBeanDependenciesUtil.createSpringBean() is not accessible here
private fun createSpringBean(parentBeans: Beans, psiClass: PsiClass): SpringBean? {
    if (!((parentBeans as DomElement)).ensureFileWritable()) return null

    return parentBeans.addBean().apply {
        clazz.stringValue = psiClass.qualifiedName
        id.stringValue = SpringBeanCoreUtils.suggestBeanNames(this).firstOrNull() ?: ""
    }
}

private fun SpringBean.getFactoryFunctionName(factoryBeanClass: KtClass): String {
    factoryMethod.stringValue?.let { if (!it.isBlank()) return it }

    val existingNames = factoryBeanClass.declarations.mapNotNull { if (it is KtFunction || it is KtClassOrObject) it.name else null }
    return KotlinNameSuggester.suggestNameByName("create${beanClass()!!.name}") { it !in existingNames }
}

internal val PsiClass.defaultTypeText: String
    get() {
        val qName = qualifiedName ?: return "Any"
        val typeParameters = typeParameters
        if (typeParameters.isEmpty()) return qName
        return TypeReconstructionUtil.getTypeNameAndStarProjectionsString(qName, typeParameters.size)
    }

private fun createSettable(
        candidateBean: SpringBeanPointer<CommonSpringBean>,
        currentBeanClass: KtLightClass,
        candidateBeanClasses: Array<out PsiClass>,
        injectionKind: SpringDependencyInjectionKind
): KtNamedDeclaration {
    val project = currentBeanClass.project
    val psiFactory = KtPsiFactory(project)
    val beanName = candidateBean.name
    try {
        val candidateClass = candidateBeanClasses.first()
        val propertyName = if (beanName != null && KotlinNameSuggester.isIdentifier(beanName)) beanName else candidateClass.name!!

        val prototype: KtNamedDeclaration = when (injectionKind) {
            SpringDependencyInjectionKind.SETTER -> {
                psiFactory.createFunction("fun set${propertyName.capitalize()}(${propertyName.decapitalize()}: ${candidateClass.defaultTypeText}) { }")
            }

            SpringDependencyInjectionKind.LATEINIT_PROPERTY -> {
                psiFactory.createProperty("lateinit var ${propertyName.decapitalize()}: ${candidateClass.defaultTypeText}")
            }

            else -> error("Unexpected injection kind: $injectionKind")
        }
        return currentBeanClass.kotlinOrigin!!.addDeclaration(prototype).apply { ShortenReferences.DEFAULT.process(this) }
    }
    catch (e: IncorrectOperationException) {
        throw RuntimeException(e)
    }
}

internal fun getSuggestedNames(
        beanPointer: SpringBeanPointer<CommonSpringBean>,
        declaration: KtCallableDeclaration,
        existingNames: Collection<String> = emptyList(),
        getType: CallableDescriptor.() -> KotlinType?
): Collection<String> {
    val names = LinkedHashSet<String>()

    val newDeclarationNameValidator =
            NewDeclarationNameValidator(declaration.parent, null, NewDeclarationNameValidator.Target.VARIABLES, listOf(declaration))
    fun validate(name: String) = name !in existingNames && newDeclarationNameValidator(name)

    SpringBeanUtils.getInstance()
            .findBeanNames(beanPointer.springBean)
            .asSequence()
            .filter { KotlinNameSuggester.isIdentifier(it) }
            .mapTo(names) { KotlinNameSuggester.suggestNameByName(it, ::validate) }

    (declaration.resolveToDescriptor() as CallableDescriptor).getType()?.let {
        names += KotlinNameSuggester.suggestNamesByType(it, ::validate)
    }

    return names
}

internal fun TemplateBuilderImpl.appendVariableTemplate(
        variable: KtCallableDeclaration,
        candidateBeanClasses: Array<out PsiClass>,
        computeSuggestions: (() -> Collection<String>)?
) {
    if (computeSuggestions != null) {
        val suggestedNames = computeSuggestions()
        val defaultName = variable.name ?: ""
        val nameExpression = object : Expression() {
            override fun calculateResult(context: ExpressionContext) = TextResult(defaultName)

            override fun calculateQuickResult(context: ExpressionContext) = calculateResult(context)

            override fun calculateLookupItems(context: ExpressionContext): Array<LookupElement>? {
                PsiDocumentManager.getInstance(context.project).commitAllDocuments()
                return suggestedNames.map { LookupElementBuilder.create(it) }.toTypedArray()

            }
        }
        replaceElement(variable.nameIdentifier, "names", nameExpression, true)
    }

    val superTypeVariants = getSuperTypeVariants(candidateBeanClasses)
    if (superTypeVariants.size > 1) {
        val lookupFactory = BasicLookupElementFactory(variable.project, InsertHandlerProvider(CallType.TYPE) { emptyList() })
        val typeReferenceExpression = object : Expression() {
            override fun calculateResult(context: ExpressionContext) = TextResult(candidateBeanClasses.first().qualifiedName ?: "")

            override fun calculateQuickResult(context: ExpressionContext) = calculateResult(context)

            override fun calculateLookupItems(context: ExpressionContext): Array<LookupElement>? {
                return superTypeVariants
                        .mapTo(LinkedHashSet()) { lookupFactory.createLookupElementForJavaClass(it, qualifyNestedClasses = true) }
                        .toTypedArray()
            }
        }
        replaceElement(variable.typeReference, "type", typeReferenceExpression, true)
    }
}

private fun BatchTemplateRunner.addCreateFunctionTemplate(
        function: KtFunction,
        candidateBeanClasses: Map<Int, Array<out PsiClass>>,
        dependency: SpringBeanPointer<CommonSpringBean>
) {
    val parameterList = function.valueParameterList!!
    val builder = TemplateBuilderImpl(parameterList)
    addTemplateFactory(parameterList) {
        for ((paramIndex, candidateBeanClassesForParam) in candidateBeanClasses) {
            builder.appendVariableTemplate(function.valueParameters[paramIndex], candidateBeanClassesForParam) {
                getSuggestedNames(dependency, function) { valueParameters[paramIndex].type }
            }
        }

        builder.buildInlineTemplate()
    }
}

private fun BatchTemplateRunner.addCreatePropertyTemplate(property: KtProperty, candidateBeanClasses: Array<out PsiClass>) {
    val builder = TemplateBuilderImpl(property)
    addTemplateFactory(property) {
        builder.appendVariableTemplate(property, candidateBeanClasses, null)
        builder.buildInlineTemplate()
    }
}

private fun BatchTemplateRunner.addCreateFunctionTemplate(
        function: KtFunction,
        paramIndex: Int,
        candidateBeanClasses: Array<out PsiClass>,
        dependency: SpringBeanPointer<CommonSpringBean>
) {
    addCreateFunctionTemplate(function, mapOf(paramIndex to candidateBeanClasses), dependency)
}

private fun getOrCreateSetter(
        candidateBean: SpringBeanPointer<CommonSpringBean>,
        currentBeanClass: KtLightClass,
        candidateBeanClasses: Array<out PsiClass>,
        templatesHolder: BatchTemplateRunner,
        injectionKind: SpringDependencyInjectionKind
): PsiNamedElement? {
    for (candidateBeanClass in candidateBeanClasses) {
        getExistedSetter(currentBeanClass, candidateBeanClass)?.let { return it }
    }

    if (!ensureFileWritable(currentBeanClass)) return null
    val settable = createSettable(candidateBean, currentBeanClass, candidateBeanClasses, injectionKind)
    when (settable) {
        is KtNamedFunction -> templatesHolder.addCreateFunctionTemplate(settable, 0, candidateBeanClasses, candidateBean)
        is KtProperty -> templatesHolder.addCreatePropertyTemplate(settable, candidateBeanClasses)
    }

    return settable
}

private fun createSetterDependency(
        currentBean: SpringBean,
        dependency: SpringBeanPointer<CommonSpringBean>,
        injectionKind: SpringDependencyInjectionKind
): BatchTemplateRunner? {
    val templatesHolder = BatchTemplateRunner(currentBean.manager.project)
    val currentBeanClass = currentBean.beanClass() as? KtLightClass ?: return null
    val candidateBeanClasses = dependency.effectiveBeanClasses().ifEmpty { return null }
    val setter = getOrCreateSetter(dependency, currentBeanClass, candidateBeanClasses, templatesHolder, injectionKind) ?: return null
    currentBean.addProperty().apply {
        name.ensureXmlElementExists()
        name.stringValue = when (setter) {
            is PsiMethod, is KtFunction -> propertyNameBySetMethodName(Name.identifier(setter.name!!), false)!!.asString()
            else -> setter.name
        }
        refAttr.setStringValue(getReferencedName(currentBean, dependency))
    }
    return templatesHolder
}

private fun addConstructorParameter(
        currentBeanClass: KtLightClass,
        candidateBeanClass: PsiClass,
        constructor: KtFunction
) {
    val psiFactory = KtPsiFactory(currentBeanClass)

    val validator = NewDeclarationNameValidator(constructor, null, NewDeclarationNameValidator.Target.VARIABLES)
    val resolutionFacade = currentBeanClass.kotlinOrigin!!.getResolutionFacade()
    val defaultType = (candidateBeanClass.getJavaOrKotlinMemberDescriptor(resolutionFacade) as ClassDescriptor).defaultType
    val name = KotlinNameSuggester.suggestNamesByType(defaultType, validator).first()

    val parameter = psiFactory.createParameter("$name: ${candidateBeanClass.defaultTypeText}")
    constructor.getValueParameterList()!!.addParameter(parameter).apply { ShortenReferences.DEFAULT.process(this) }
}

private fun findProperConstructorAndAddParameter(
        currentBean: SpringBean,
        dependency: SpringBeanPointer<CommonSpringBean>,
        currentBeanClass: KtLightClass,
        candidateBeanClass: PsiClass,
        holder: BatchTemplateRunner
): PsiMethod? {
    val lightConstructor = currentBean.resolvedConstructorArgs.resolvedMethod as? KtLightMethod ?: return null
    val constructorOrigin = lightConstructor.kotlinOrigin
    val properConstructor = when (constructorOrigin) {
        is KtFunction -> constructorOrigin
        is KtClass -> constructorOrigin.createPrimaryConstructorIfAbsent()
        else -> return null
    }
    addConstructorParameter(currentBeanClass, candidateBeanClass, properConstructor)
    holder.addCreateFunctionTemplate(properConstructor, properConstructor.getValueParameters().lastIndex, arrayOf(candidateBeanClass), dependency)
    return lightConstructor
}

private fun createConstructorWithTemplate(
        currentBean: SpringBean,
        dependency: SpringBeanPointer<CommonSpringBean>,
        templatesHolder: BatchTemplateRunner
) {
    val beanClass = currentBean.beanClass() as? KtLightClass ?: return
    val ktBeanClass = beanClass.kotlinOrigin as? KtClass ?: return

    try {
        val psiFactory = KtPsiFactory(beanClass)
        val factoryFunction = when {
            currentBean.factoryBean.exists() -> {
                // TODO: Support non-Kotlin factory beans
                val factoryClass = currentBean.factoryBean.value?.beanClass as? KtLightClass ?: return
                val ktFactoryClass = factoryClass.kotlinOrigin as? KtClass ?: return
                val funName = currentBean.getFactoryFunctionName(ktFactoryClass)
                val funText = "fun $funName(): ${beanClass.defaultTypeText} {\n return null \n}"
                ktFactoryClass.addDeclaration(psiFactory.createFunction(funText))
            }
            currentBean.factoryMethod.exists() -> {
                val funName = currentBean.getFactoryFunctionName(ktBeanClass)
                val funText = "@JvmStatic fun $funName(): ${beanClass.defaultTypeText} {\n return null \n}"
                ktBeanClass.getOrCreateCompanionObject().addDeclaration(psiFactory.createFunction(funText))
            }
            else -> {
                if (ktBeanClass.hasPrimaryConstructor() || ktBeanClass.getSecondaryConstructors().isNotEmpty()) {
                    ktBeanClass.addDeclaration(psiFactory.createSecondaryConstructor("constructor()"))
                }
                else {
                    ktBeanClass.createPrimaryConstructorIfAbsent()
                }
            }
        } as KtFunction

        ShortenReferences.DEFAULT.process(factoryFunction)

        val dummyMethod = JavaPsiFacade.getElementFactory(beanClass.project).createMethod("foo", PsiType.VOID)
        dummyMethod.containingFile.moduleInfo = ktBeanClass.getModuleInfo()
        SpringConstructorArgResolveUtil.suggestParamsForConstructorArgs(currentBean).forEach { dummyMethod.parameterList.add(it) }

        val parameterList = factoryFunction.valueParameterList!!
        val indexOffset = parameterList.parameters.size
        val candidateClassMap = SmartHashMap<Int, Array<PsiClass>>()
        val resolutionFacade = ktBeanClass.getResolutionFacade()
        for ((i, psiParam) in dummyMethod.parameterList.parameters.withIndex()) {
            val descriptor = psiParam.getParameterDescriptor(resolutionFacade)!!
            val ktParameter = psiFactory.createParameter(
                    "${psiParam.name}: ${IdeDescriptorRenderers.SOURCE_CODE_NOT_NULL_TYPE_APPROXIMATION.renderType(descriptor.type)}"
            )
            parameterList.addParameter(ktParameter)
            (psiParam.type as? PsiClassType)?.resolve()?.let { candidateClassMap[i + indexOffset] = arrayOf(it) }
        }

        templatesHolder.addCreateFunctionTemplate(factoryFunction, candidateClassMap, dependency)
    }
    catch (e: IncorrectOperationException) {
        throw RuntimeException(e)
    }
}

private fun createConstructorDependency(
        currentBean: SpringBean,
        dependency: SpringBeanPointer<CommonSpringBean>
): BatchTemplateRunner? {
    val templatesHolder = BatchTemplateRunner(currentBean.manager.project)
    val currentBeanClass = currentBean.beanClass() as? KtLightClass ?: return null
    val candidateBeanClasses = dependency.effectiveBeanClasses().ifEmpty { return null }
    var existedConstructor = findExistedConstructor(currentBean, currentBeanClass, candidateBeanClasses)
    if (existedConstructor == null) {
        if (!ensureFileWritable(currentBeanClass)) return null
        existedConstructor = findProperConstructorAndAddParameter(currentBean, dependency, currentBeanClass, candidateBeanClasses.first(), templatesHolder)
    }

    val newConstructorArg = currentBean.addConstructorArg()
    newConstructorArg.refAttr.stringValue = getReferencedName(currentBean, dependency)
    if (existedConstructor == null && SpringConstructorArgResolveUtil.findMatchingMethods(currentBean).isEmpty()) {
        createConstructorWithTemplate(currentBean, dependency, templatesHolder)
    }

    return templatesHolder
}

private fun generateDependency(
        springBean: SpringBean,
        dependency: SpringBeanPointer<CommonSpringBean>,
        injectionKind: SpringDependencyInjectionKind
): BatchTemplateRunner? {
    return runWriteAction {
        if (injectionKind.isSetter) {
            createSetterDependency(springBean, dependency, injectionKind)
        }
        else {
            createConstructorDependency(springBean, dependency)
        }
    }
}

enum class SpringDependencyInjectionKind(val isSetter: Boolean) {
    CONSTRUCTOR(false),
    SETTER(true),
    LATEINIT_PROPERTY(true)
}

@set:TestOnly
var Project.beanFilter: (SpringBeanPointer<CommonSpringBean>) -> Boolean
        by NotNullableUserDataProperty(Key.create("BEAN_CHOOSER")) { true }

@NotNull
private fun chooseDependentBeans(
        candidates: Set<SpringBeanClassMember>,
        project: Project,
        isSetterDependency: Boolean
): List<SpringBeanPointer<CommonSpringBean>> {
    return if (ApplicationManager.getApplication().isUnitTestMode) {
        candidates.map { it.springBean }.filter(project.beanFilter)
    }
    else {
        GenerateSpringBeanDependenciesUtil.chooseDependentBeans(candidates, project, isSetterDependency)
    }
}

fun generateDependenciesFor(
        springModel: CommonSpringModel,
        klass: KtLightClass,
        injectionKind: SpringDependencyInjectionKind
): List<BatchTemplateRunner> {
    val project = klass.project
    val isSetter = injectionKind.isSetter

    SpringModelSearchers
            .findBeans(springModel, SpringModelSearchParameters.byClass(klass))
            .asSequence()
            .map { it.springBean }
            .filterIsInstance<SpringBean>()
            .firstOrNull { acceptBean(it, false) }
            ?.let { springBean ->
                if (!springBean.ensureFileWritable()) return emptyList()
                if (springBean.beanClass() == null) return emptyList()
                val dependencies = chooseDependentBeans(getCandidates(springBean, isSetter), project, isSetter)
                return dependencies.mapNotNull { generateDependency(springBean, it, injectionKind) }
            }

    val dependencies = chooseDependentBeans(getCandidates(springModel, klass, isSetter), project, isSetter)
    val beanXml = dependencies.singleOrNull()?.springBean as? DomSpringBean ?: return emptyList()
    val beansXml = beanXml.getParentOfType(Beans::class.java, false) ?: return emptyList()
    val newBean = createSpringBean(beansXml, klass) ?: return emptyList()

    return dependencies.mapNotNull { generateDependency(newBean, it, injectionKind) }
}
