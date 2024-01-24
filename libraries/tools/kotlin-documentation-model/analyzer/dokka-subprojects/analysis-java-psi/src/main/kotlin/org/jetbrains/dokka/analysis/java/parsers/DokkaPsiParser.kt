/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.analysis.java.SyntheticElementDocumentationProvider
import org.jetbrains.dokka.analysis.java.getVisibility
import org.jetbrains.dokka.analysis.java.util.*
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.nextTarget
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.links.withEnumEntryExtra
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.parallelForEach
import org.jetbrains.dokka.utilities.parallelMap
import org.jetbrains.dokka.utilities.parallelMapNotNull

internal class DokkaPsiParser(
    private val sourceSetData: DokkaConfiguration.DokkaSourceSet,
    private val project: Project,
    private val logger: DokkaLogger,
    private val javadocParser: JavadocParser,
    private val javaPsiDocCommentParser: JavaPsiDocCommentParser,
    private val lightMethodChecker: BreakingAbstractionKotlinLightMethodChecker,
) {
    private val syntheticDocProvider = SyntheticElementDocumentationProvider(javaPsiDocCommentParser, project)

    private val cachedBounds = hashMapOf<String, Bound>()

    private val PsiMethod.hash: Int
        get() = "$returnType $name$parameterList".hashCode()

    private val PsiField.hash: Int
        get() = "$type $name".hashCode()

    private val PsiClassType.shouldBeIgnored: Boolean
        get() = isClass("java.lang.Enum") || isClass("java.lang.Object")

    private fun PsiClassType.isClass(qName: String): Boolean {
        val shortName = qName.substringAfterLast('.')
        if (className == shortName) {
            val psiClass = resolve()
            return psiClass?.qualifiedName == qName
        }
        return false
    }

    private fun <T> T.toSourceSetDependent() = mapOf(sourceSetData to this)

    suspend fun parsePackage(packageName: String, psiFiles: List<PsiJavaFile>): DPackage = coroutineScope {
        val dri = DRI(packageName = packageName)
        val packageInfo = psiFiles.singleOrNull { it.name == "package-info.java" }
        val documentation = packageInfo?.let {
            javadocParser.parseDocumentation(it).toSourceSetDependent()
        }.orEmpty()
        val annotations = packageInfo?.packageStatement?.annotationList?.annotations

        DPackage(
            dri = dri,
            functions = emptyList(),
            properties = emptyList(),
            classlikes = psiFiles.parallelMap { psiFile ->
                coroutineScope {
                    psiFile.classes.asIterable().parallelMap { parseClasslike(it, dri) }
                }
            }.flatten(),
            typealiases = emptyList(),
            documentation = documentation,
            expectPresentInSet = null,
            sourceSets = setOf(sourceSetData),
            extra = PropertyContainer.withAll(
                annotations?.toList().orEmpty().toListOfAnnotations().toSourceSetDependent().toAnnotations()
            )
        )
    }

    private suspend fun parseClasslike(psi: PsiClass, parent: DRI): DClasslike = coroutineScope {
        with(psi) {
            val dri = parent.withClass(name.toString())
            val superMethodsKeys = hashSetOf<Int>()
            val superMethods = mutableListOf<Pair<PsiMethod, DRI>>()
            val superFieldsKeys = hashSetOf<Int>()
            val superFields = mutableListOf<Pair<PsiField, DRI>>()
            methods.asIterable().parallelForEach { superMethodsKeys.add(it.hash) }

            /**
             * Caution! This method mutates
             * - superMethodsKeys
             * - superMethods
             * - superFieldsKeys
             * - superKeys
             */
            /**
             * Caution! This method mutates
             * - superMethodsKeys
             * - superMethods
             * - superFieldsKeys
             * - superKeys
             */
            fun Array<PsiClassType>.getSuperTypesPsiClasses(): List<Pair<PsiClass, JavaClassKindTypes>> {
                forEach { type ->
                    (type as? PsiClassType)?.resolve()?.let {
                        val definedAt = DRI.from(it)
                        it.methods.forEach { method ->
                            val hash = method.hash
                            if (!method.isConstructor && !superMethodsKeys.contains(hash) &&
                                method.getVisibility() != JavaVisibility.Private
                            ) {
                                superMethodsKeys.add(hash)
                                superMethods.add(Pair(method, definedAt))
                            }
                        }
                        it.fields.forEach { field ->
                            val hash = field.hash
                            if (!superFieldsKeys.contains(hash)) {
                                superFieldsKeys.add(hash)
                                superFields.add(Pair(field, definedAt))
                            }
                        }
                    }
                }
                return filter { !it.shouldBeIgnored }.mapNotNull { supertypePsi ->
                    supertypePsi.resolve()?.let { supertypePsiClass ->
                        val javaClassKind = when {
                            supertypePsiClass.isInterface -> JavaClassKindTypes.INTERFACE
                            else -> JavaClassKindTypes.CLASS
                        }
                        supertypePsiClass to javaClassKind
                    }
                }
            }

            fun traversePsiClassForAncestorsAndInheritedMembers(psiClass: PsiClass): AncestryNode {
                val (classes, interfaces) = psiClass.superTypes.getSuperTypesPsiClasses()
                    .partition { it.second == JavaClassKindTypes.CLASS }

                return AncestryNode(
                    typeConstructor = GenericTypeConstructor(
                        DRI.from(psiClass),
                        psiClass.typeParameters.map { typeParameter ->
                            TypeParameter(
                                dri = DRI.from(typeParameter),
                                name = typeParameter.name.orEmpty(),
                                extra = typeParameter.annotations()
                            )
                        }
                    ),
                    superclass = classes.singleOrNull()?.first?.let(::traversePsiClassForAncestorsAndInheritedMembers),
                    interfaces = interfaces.map { traversePsiClassForAncestorsAndInheritedMembers(it.first) }
                )
            }

            val ancestry: AncestryNode = traversePsiClassForAncestorsAndInheritedMembers(this)

            val (regularFunctions, accessors) = splitFunctionsAndAccessors(psi.fields, psi.methods)
            val (regularSuperFunctions, superAccessors) = splitFunctionsAndAccessors(
                fields = superFields.map { it.first }.toTypedArray(),
                methods = superMethods.map { it.first }.toTypedArray()
            )

            val regularSuperFunctionsKeys = regularSuperFunctions.map { it.hash }.toSet()
            val regularSuperFunctionsWithDRI = superMethods.filter { it.first.hash in regularSuperFunctionsKeys }

            val superAccessorsWithDRI = superAccessors.mapValues { (field, methods) ->
                val containsJvmField = field.annotations.mapNotNull { it.toAnnotation() }.any { it.isJvmField() }
                if (containsJvmField) {
                    emptyList()
                } else {
                    methods.mapNotNull { method -> superMethods.find { it.first.hash == method.hash } }
                }
            }

            val overridden = regularFunctions.flatMap { it.findSuperMethods().toList() }
            val documentation = javadocParser.parseDocumentation(this).toSourceSetDependent()
            val allFunctions = async {
                val parsedRegularFunctions = regularFunctions.parallelMapNotNull {
                    if (!it.isConstructor) parseFunction(
                        it,
                        parentDRI = dri
                    ) else null
                }
                val parsedSuperFunctions = regularSuperFunctionsWithDRI
                    .filter { it.first !in overridden }
                    .parallelMap { parseFunction(it.first, inheritedFrom = it.second) }

                parsedRegularFunctions + parsedSuperFunctions
            }
            val allFields = async {
                val parsedFields = fields.toList().parallelMapNotNull {
                    parseField(it, accessors[it].orEmpty())
                }
                val parsedSuperFields = superFields.parallelMapNotNull { (field, dri) ->
                    parseFieldWithInheritingAccessors(
                        field,
                        superAccessorsWithDRI[field].orEmpty(),
                        inheritedFrom = dri
                    )
                }
                parsedFields + parsedSuperFields
            }
            val source = parseSources()
            val classlikes = async { innerClasses.asIterable().parallelMap { parseClasslike(it, dri) } }
            val visibility = getVisibility().toSourceSetDependent()
            val ancestors = (listOfNotNull(ancestry.superclass?.let {
                it.typeConstructor.let { typeConstructor ->
                    TypeConstructorWithKind(
                        typeConstructor,
                        JavaClassKindTypes.CLASS
                    )
                }
            }) + ancestry.interfaces.map {
                TypeConstructorWithKind(
                    it.typeConstructor,
                    JavaClassKindTypes.INTERFACE
                )
            }).toSourceSetDependent()
            val implementedInterfacesExtra =
                ImplementedInterfaces(ancestry.allImplementedInterfaces().toSourceSetDependent())

            // used only for class and enum
            val innerModifierExtra = when {
                // top level java classes - no `inner`
                psi.containingClass == null -> null
                // java `static class` = kotlin `class`
                psi.hasModifier(JvmModifier.STATIC) -> null
                // java `class` = kotlin `inner class`
                else -> setOf(
                    ExtraModifiers.KotlinOnlyModifiers.Inner
                ).toSourceSetDependent().toAdditionalModifiers()
            }

            when {
                isAnnotationType ->
                    DAnnotation(
                        name = name.orEmpty(),
                        dri = dri,
                        documentation = documentation,
                        expectPresentInSet = null,
                        sources = source,
                        functions = allFunctions.await(),
                        properties = allFields.await(),
                        classlikes = classlikes.await(),
                        visibility = visibility,
                        companion = null,
                        constructors = parseConstructors(dri),
                        generics = mapTypeParameters(dri),
                        sourceSets = setOf(sourceSetData),
                        isExpectActual = false,
                        extra = PropertyContainer.withAll(
                            implementedInterfacesExtra,
                            annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                .toAnnotations()
                        )
                    )

                isEnum -> DEnum(
                    dri = dri,
                    name = name.orEmpty(),
                    entries = fields.filterIsInstance<PsiEnumConstant>().map { entry ->
                        DEnumEntry(
                            dri = dri.withClass(entry.name).withEnumEntryExtra(),
                            name = entry.name,
                            documentation = javadocParser.parseDocumentation(entry).toSourceSetDependent(),
                            expectPresentInSet = null,
                            functions = emptyList(),
                            properties = emptyList(),
                            classlikes = emptyList(),
                            sourceSets = setOf(sourceSetData),
                            extra = PropertyContainer.withAll(
                                implementedInterfacesExtra,
                                annotations.toList().toListOfAnnotations().toSourceSetDependent()
                                    .toAnnotations()
                            )
                        )
                    },
                    documentation = documentation,
                    expectPresentInSet = null,
                    sources = source,
                    functions = allFunctions.await(),
                    properties = fields.filter { it !is PsiEnumConstant }
                        .map { parseField(it, accessors[it].orEmpty()) },
                    classlikes = classlikes.await(),
                    visibility = visibility,
                    companion = null,
                    constructors = parseConstructors(dri),
                    supertypes = ancestors,
                    sourceSets = setOf(sourceSetData),
                    isExpectActual = false,
                    extra = PropertyContainer.withAll(
                        implementedInterfacesExtra,
                        innerModifierExtra,
                        annotations.toList().toListOfAnnotations().toSourceSetDependent()
                            .toAnnotations()
                    )
                )

                isInterface -> DInterface(
                    dri = dri,
                    name = name.orEmpty(),
                    documentation = documentation,
                    expectPresentInSet = null,
                    sources = source,
                    functions = allFunctions.await(),
                    properties = allFields.await(),
                    classlikes = classlikes.await(),
                    visibility = visibility,
                    companion = null,
                    generics = mapTypeParameters(dri),
                    supertypes = ancestors,
                    modifier = getModifier().toSourceSetDependent(),
                    sourceSets = setOf(sourceSetData),
                    isExpectActual = false,
                    extra = PropertyContainer.withAll(
                        implementedInterfacesExtra,
                        annotations.toList().toListOfAnnotations().toSourceSetDependent()
                            .toAnnotations()
                    )
                )

                else -> DClass(
                    dri = dri,
                    name = name.orEmpty(),
                    constructors = parseConstructors(dri),
                    functions = allFunctions.await(),
                    properties = allFields.await(),
                    classlikes = classlikes.await(),
                    sources = source,
                    visibility = visibility,
                    companion = null,
                    generics = mapTypeParameters(dri),
                    supertypes = ancestors,
                    documentation = documentation,
                    expectPresentInSet = null,
                    modifier = getModifier().toSourceSetDependent(),
                    sourceSets = setOf(sourceSetData),
                    isExpectActual = false,
                    extra = PropertyContainer.withAll(
                        implementedInterfacesExtra,
                        innerModifierExtra,
                        annotations.toList().toListOfAnnotations().toSourceSetDependent()
                            .toAnnotations(),
                        ancestry.exceptionInSupertypesOrNull()
                    )
                )
            }
        }
    }

    /*
     * Parameter `parentDRI` required for substitute package name:
     * in the case of synthetic constructor, it will return empty from [DRI.Companion.from].
     */
    private fun PsiClass.parseConstructors(parentDRI: DRI): List<DFunction> {
        val constructors = when {
            isAnnotationType || isInterface -> emptyArray()
            isEnum -> this.constructors
            else -> this.constructors.takeIf { it.isNotEmpty() } ?: arrayOf(createDefaultConstructor())
        }
        return constructors.map { parseFunction(psi = it, isConstructor = true, parentDRI = parentDRI) }
    }

    /**
     * PSI doesn't return a default constructor if class doesn't contain an explicit one.
     * This method create synthetic constructor
     * Visibility modifier is preserved from the class.
     */
    private fun PsiClass.createDefaultConstructor(): PsiMethod {
        val psiElementFactory = JavaPsiFacade.getElementFactory(project)
        val signature = when (val classVisibility = getVisibility()) {
            JavaVisibility.Default -> name.orEmpty()
            else -> "${classVisibility.name} $name"
        }
        return psiElementFactory.createConstructor(signature, this)
    }

    private fun AncestryNode.exceptionInSupertypesOrNull(): ExceptionInSupertypes? =
        typeConstructorsBeingExceptions().takeIf { it.isNotEmpty() }
            ?.let { ExceptionInSupertypes(it.toSourceSetDependent()) }

    private fun parseFunction(
        psi: PsiMethod,
        isConstructor: Boolean = false,
        inheritedFrom: DRI? = null,
        parentDRI: DRI? = null,
    ): DFunction {
        val dri = parentDRI?.let { dri ->
            DRI.from(psi).copy(packageName = dri.packageName, classNames = dri.classNames)
        } ?: DRI.from(psi)
        val docs = psi.getDocumentation()
        return DFunction(
            dri = dri,
            name = psi.name,
            isConstructor = isConstructor,
            parameters = psi.parameterList.parameters.map { psiParameter ->
                DParameter(
                    dri = dri.copy(target = dri.target.nextTarget()),
                    name = psiParameter.name,
                    documentation = DocumentationNode(
                        listOfNotNull(docs.firstChildOfTypeOrNull<Param> {
                            it.name == psiParameter.name
                        })
                    ).toSourceSetDependent(),
                    expectPresentInSet = null,
                    type = getBound(psiParameter.type),
                    sourceSets = setOf(sourceSetData),
                    extra = PropertyContainer.withAll(
                        psiParameter.annotations.toList().toListOfAnnotations().toSourceSetDependent()
                            .toAnnotations()
                    )
                )
            },
            documentation = docs.toSourceSetDependent(),
            expectPresentInSet = null,
            sources = psi.parseSources(),
            visibility = psi.getVisibility().toSourceSetDependent(),
            type = psi.returnType?.let { getBound(type = it) } ?: Void,
            generics = psi.mapTypeParameters(dri),
            receiver = null,
            modifier = psi.getModifier().toSourceSetDependent(),
            sourceSets = setOf(sourceSetData),
            isExpectActual = false,
            extra = psi.additionalExtras().let {
                PropertyContainer.withAll(
                    inheritedFrom?.let { InheritedMember(it.toSourceSetDependent()) },
                    it.toSourceSetDependent().toAdditionalModifiers(),
                    (psi.annotations.toList()
                        .toListOfAnnotations() + it.toListOfAnnotations()).toSourceSetDependent()
                        .toAnnotations(),
                    ObviousMember.takeIf { psi.isObvious(inheritedFrom) },
                    psi.throwsList.toDriList().takeIf { it.isNotEmpty() }
                        ?.let { CheckedExceptions(it.toSourceSetDependent()) }
                )
            }
        )
    }

    private fun PsiNamedElement.parseSources(): SourceSetDependent<DocumentableSource> {
        return when {
            // `isPhysical` detects the virtual declarations without real sources.
            // Otherwise, `PsiDocumentableSource` initialization will fail: non-physical declarations doesn't have `virtualFile`.
            // This check protects from accidentally requesting sources for synthetic / virtual declarations.
            isPhysical -> PsiDocumentableSource(this).toSourceSetDependent()
            else -> emptyMap()
        }
    }

    private fun PsiMethod.getDocumentation(): DocumentationNode =
        this.takeIf { it is SyntheticElement }?.let { syntheticDocProvider.getDocumentation(it) }
            ?: javadocParser.parseDocumentation(this)

    private fun PsiMethod.isObvious(inheritedFrom: DRI? = null): Boolean {
        return (this is SyntheticElement && !syntheticDocProvider.isDocumented(this))
                || inheritedFrom?.isObvious() == true
    }

    private fun DRI.isObvious(): Boolean {
        return packageName == "java.lang" && (classNames == "Object" || classNames == "Enum")
    }

    private fun PsiReferenceList.toDriList() = referenceElements.mapNotNull { it?.resolve()?.let { DRI.from(it) } }

    private fun PsiModifierListOwner.additionalExtras() = listOfNotNull(
        ExtraModifiers.JavaOnlyModifiers.Static.takeIf { hasModifier(JvmModifier.STATIC) },
        ExtraModifiers.JavaOnlyModifiers.Native.takeIf { hasModifier(JvmModifier.NATIVE) },
        ExtraModifiers.JavaOnlyModifiers.Synchronized.takeIf { hasModifier(JvmModifier.SYNCHRONIZED) },
        ExtraModifiers.JavaOnlyModifiers.StrictFP.takeIf { hasModifier(JvmModifier.STRICTFP) },
        ExtraModifiers.JavaOnlyModifiers.Transient.takeIf { hasModifier(JvmModifier.TRANSIENT) },
        ExtraModifiers.JavaOnlyModifiers.Volatile.takeIf { hasModifier(JvmModifier.VOLATILE) },
        ExtraModifiers.JavaOnlyModifiers.Transitive.takeIf { hasModifier(JvmModifier.TRANSITIVE) }
    ).toSet()

    private fun Set<ExtraModifiers>.toListOfAnnotations() = map {
        if (it !is ExtraModifiers.JavaOnlyModifiers.Static)
            Annotations.Annotation(DRI("kotlin.jvm", it.name.toLowerCase().capitalize()), emptyMap())
        else
            Annotations.Annotation(DRI("kotlin.jvm", "JvmStatic"), emptyMap())
    }

    /**
     * Workaround for getting JvmField Kotlin annotation in PSIs
     */
    private fun Collection<PsiAnnotation>.findJvmFieldAnnotation(): Annotations.Annotation? {
        val anyJvmFieldAnnotation = this.any {
            it.qualifiedName == "$JVM_FIELD_PACKAGE_NAME.$JVM_FIELD_CLASS_NAMES"
        }
        return if (anyJvmFieldAnnotation) {
            Annotations.Annotation(DRI(JVM_FIELD_PACKAGE_NAME, JVM_FIELD_CLASS_NAMES), emptyMap())
        } else {
            null
        }
    }

    private fun <T : AnnotationTarget> PsiTypeParameter.annotations(): PropertyContainer<T> = this.annotations.toList().toListOfAnnotations().annotations()
    private fun <T : AnnotationTarget> PsiType.annotations(): PropertyContainer<T> = this.annotations.toList().toListOfAnnotations().annotations()

    private fun <T : AnnotationTarget> List<Annotations.Annotation>.annotations(): PropertyContainer<T> =
        this.takeIf { it.isNotEmpty() }?.let { annotations ->
            PropertyContainer.withAll(annotations.toSourceSetDependent().toAnnotations())
        } ?: PropertyContainer.empty()

    private fun getBound(type: PsiType): Bound {
        //We would like to cache most of the bounds since it is not common to annotate them,
        //but if this is the case, we treat them as 'one of'
        fun PsiType.cacheBoundIfHasNoAnnotation(f: (List<Annotations.Annotation>) -> Bound): Bound {
            val annotations = this.annotations.toList().toListOfAnnotations()
            return if (annotations.isNotEmpty()) f(annotations)
            else cachedBounds.getOrPut(canonicalText) {
                f(annotations)
            }
        }

        return when (type) {
            is PsiClassType ->
                type.resolve()?.let { resolved ->
                    when {
                        resolved.qualifiedName == "java.lang.Object" -> type.cacheBoundIfHasNoAnnotation { annotations -> JavaObject(annotations.annotations()) }
                        resolved is PsiTypeParameter -> {
                            TypeParameter(
                                dri = DRI.from(resolved),
                                name = resolved.name.orEmpty(),
                                extra = type.annotations()
                            )
                        }

                        Regex("kotlin\\.jvm\\.functions\\.Function.*").matches(resolved.qualifiedName ?: "") ||
                                Regex("java\\.util\\.function\\.Function.*").matches(
                                    resolved.qualifiedName ?: ""
                                ) -> FunctionalTypeConstructor(
                            DRI.from(resolved),
                            type.parameters.map { getProjection(it) },
                            extra = type.annotations()
                        )

                        else -> {
                            // cache types that have no annotation and no type parameter
                            // since we cache only by name and type parameters depend on context
                            val typeParameters = type.parameters.map { getProjection(it) }
                            if (typeParameters.isEmpty())
                                type.cacheBoundIfHasNoAnnotation { annotations ->
                                    GenericTypeConstructor(
                                        DRI.from(resolved),
                                        typeParameters,
                                        extra = annotations.annotations()
                                    )
                                }
                            else
                                GenericTypeConstructor(
                                    DRI.from(resolved),
                                    typeParameters,
                                    extra = type.annotations()
                                )
                        }
                    }
                } ?: UnresolvedBound(type.presentableText, type.annotations())

            is PsiArrayType -> GenericTypeConstructor(
                DRI("kotlin", "Array"),
                listOf(getProjection(type.componentType)),
                extra = type.annotations()
            )

            is PsiPrimitiveType -> if (type.name == "void") Void
            else type.cacheBoundIfHasNoAnnotation { annotations -> PrimitiveJavaType(type.name, annotations.annotations()) }
            else -> throw IllegalStateException("${type.presentableText} is not supported by PSI parser")
        }
    }


    private fun getVariance(type: PsiWildcardType): Projection = when {
        type.isExtends -> Covariance(getBound(type.extendsBound))
        type.isSuper -> Contravariance(getBound(type.superBound))
        // If the type isn't explicitly bounded, it still has an implicit `extends Object` bound
        type.extendsBound != PsiType.NULL -> Covariance(getBound(type.extendsBound))
        else -> throw IllegalStateException("${type.presentableText} has incorrect bounds")
    }

    private fun getProjection(type: PsiType): Projection = when (type) {
        is PsiEllipsisType -> Star
        is PsiWildcardType -> getVariance(type)
        else -> getBound(type)
    }

    private fun PsiModifierListOwner.getModifier(): JavaModifier {
        val isInterface = this is PsiClass && this.isInterface

        return if (isInterface) {
            // Java interface can't have modality modifiers except for "sealed", which is not supported yet in Dokka
            JavaModifier.Empty
        } else when {
            hasModifier(JvmModifier.ABSTRACT) -> JavaModifier.Abstract
            hasModifier(JvmModifier.FINAL) -> JavaModifier.Final
            else -> JavaModifier.Empty
        }
    }

    private fun PsiTypeParameterListOwner.mapTypeParameters(dri: DRI): List<DTypeParameter> {
        fun mapBounds(bounds: Array<JvmReferenceType>): List<Bound> =
            if (bounds.isEmpty()) emptyList() else bounds.mapNotNull {
                (it as? PsiClassType)?.let { classType -> Nullable(getBound(classType)) }
            }
        return typeParameters.map { type ->
            DTypeParameter(
                dri = dri.copy(target = dri.target.nextTarget()),
                name = type.name.orEmpty(),
                presentableName = null,
                documentation = javadocParser.parseDocumentation(type).toSourceSetDependent(),
                expectPresentInSet = null,
                bounds = mapBounds(type.bounds),
                sourceSets = setOf(sourceSetData),
                extra = PropertyContainer.withAll(
                    type.annotations.toList().toListOfAnnotations().toSourceSetDependent()
                        .toAnnotations()
                )
            )
        }
    }

    private fun parseFieldWithInheritingAccessors(
        psi: PsiField,
        accessors: List<Pair<PsiMethod, DRI>>,
        inheritedFrom: DRI
    ): DProperty {
        val getter = accessors
            .firstOrNull { (method, _) -> method.isGetterFor(psi) }
            ?.let { (method, dri) -> parseFunction(method, inheritedFrom = dri) }

        val setter = accessors
            .firstOrNull { (method, _) -> method.isSetterFor(psi) }
            ?.let { (method, dri) -> parseFunction(method, inheritedFrom = dri) }

        return parseField(
            psi = psi,
            getter = getter,
            setter = setter,
            inheritedFrom = inheritedFrom
        )
    }

    private fun parseField(psi: PsiField, accessors: List<PsiMethod>, inheritedFrom: DRI? = null): DProperty {
        val getter = accessors.firstOrNull { it.isGetterFor(psi) }?.let { parseFunction(it) }
        val setter = accessors.firstOrNull { it.isSetterFor(psi) }?.let { parseFunction(it) }
        return parseField(
            psi = psi,
            getter = getter,
            setter = setter,
            inheritedFrom = inheritedFrom
        )
    }

    private fun parseField(psi: PsiField, getter: DFunction?, setter: DFunction?, inheritedFrom: DRI? = null): DProperty {
        val dri = DRI.from(psi)

        // non-final java field without accessors should be a var
        // setter should be not null when inheriting kotlin vars
        val isMutable = !psi.hasModifierProperty("final")
        val isVar = (isMutable && getter == null && setter == null) || (getter != null && setter != null)

        return DProperty(
            dri = dri,
            name = psi.name,
            documentation = javadocParser.parseDocumentation(psi).toSourceSetDependent(),
            expectPresentInSet = null,
            sources = psi.parseSources(),
            visibility = psi.getVisibility(getter).toSourceSetDependent(),
            type = getBound(psi.type),
            receiver = null,
            setter = setter,
            getter = getter,
            modifier = psi.getModifier().toSourceSetDependent(),
            sourceSets = setOf(sourceSetData),
            generics = emptyList(),
            isExpectActual = false,
            extra = psi.additionalExtras().let {
                val psiAnnotations = psi.annotations.toList()
                val parsedAnnotations = psiAnnotations.toListOfAnnotations()
                val extraModifierAnnotations = it.toListOfAnnotations()
                val jvmFieldAnnotation = psiAnnotations.findJvmFieldAnnotation()
                val annotations = parsedAnnotations + extraModifierAnnotations + listOfNotNull(jvmFieldAnnotation)

                PropertyContainer.withAll(
                    inheritedFrom?.let { inheritedFrom -> InheritedMember(inheritedFrom.toSourceSetDependent()) },
                    it.toSourceSetDependent().toAdditionalModifiers(),
                    annotations.toSourceSetDependent().toAnnotations(),
                    psi.getConstantExpression()?.let { DefaultValue(it.toSourceSetDependent()) },
                    takeIf { isVar }?.let { IsVar }
                )
            }
        )
    }

    private fun PsiField.getVisibility(getter: DFunction?): Visibility {
        return getter?.visibility?.get(sourceSetData) ?: this.getVisibility()
    }

    private fun Collection<PsiAnnotation>.toListOfAnnotations() =
        filter { !lightMethodChecker.isLightAnnotation(it) }.mapNotNull { it.toAnnotation() }

    private fun PsiField.getConstantExpression(): Expression? {
        val constantValue = this.computeConstantValue() ?: return null
        return when (constantValue) {
            is Byte -> IntegerConstant(constantValue.toLong())
            is Short -> IntegerConstant(constantValue.toLong())
            is Int -> IntegerConstant(constantValue.toLong())
            is Long -> IntegerConstant(constantValue)
            is Char -> StringConstant(constantValue.toString())
            is String -> StringConstant(constantValue)
            is Double -> DoubleConstant(constantValue)
            is Float -> FloatConstant(constantValue)
            is Boolean -> BooleanConstant(constantValue)
            else -> ComplexExpression(constantValue.toString())
        }
    }

    private fun JvmAnnotationAttribute.toValue(): AnnotationParameterValue = when (this) {
        is PsiNameValuePair -> value?.toValue() ?: attributeValue?.toValue() ?: StringValue("")
        else -> StringValue(this.attributeName)
    }.let { annotationValue ->
        if (annotationValue is StringValue) annotationValue.copy(annotationValue.value.removeSurrounding("\""))
        else annotationValue
    }

    /**
     * This is a workaround for static imports from JDK like RetentionPolicy
     * For some reason they are not represented in the same way than using normal import
     */
    private fun JvmAnnotationAttributeValue.toValue(): AnnotationParameterValue? {
        return when (this) {
            is JvmAnnotationEnumFieldValue -> (field as? PsiElement)?.let { EnumValue(fieldName ?: "", DRI.from(it)) }
            // static import of a constant is resolved to constant value instead of a field/link
            is JvmAnnotationConstantValue -> this.constantValue?.toAnnotationLiteralValue()
            else -> null
        }
    }

    private fun Any.toAnnotationLiteralValue() = when (this) {
        is Byte -> IntValue(this.toInt())
        is Short -> IntValue(this.toInt())
        is Char -> StringValue(this.toString())
        is Int -> IntValue(this)
        is Long -> LongValue(this)
        is Boolean -> BooleanValue(this)
        is Float -> FloatValue(this)
        is Double -> DoubleValue(this)
        else -> StringValue(this.toString())
    }

    private fun PsiAnnotationMemberValue.toValue(): AnnotationParameterValue? = when (this) {
        is PsiAnnotation -> toAnnotation()?.let { AnnotationValue(it) }
        is PsiArrayInitializerMemberValue -> ArrayValue(initializers.mapNotNull { it.toValue() })
        is PsiReferenceExpression -> psiReference?.let { EnumValue(text ?: "", DRI.from(it)) }
        is PsiClassObjectAccessExpression -> {
            val parameterType = (type as? PsiClassType)?.parameters?.firstOrNull()
            val classType = when (parameterType) {
                is PsiClassType -> parameterType.resolve()
                // Notice: Array<String>::class will be passed down as String::class
                // should probably be Array::class instead but this reflects behaviour for Kotlin sources
                is PsiArrayType -> (parameterType.componentType as? PsiClassType)?.resolve()
                else -> null
            }
            classType?.let { ClassValue(it.name ?: "", DRI.from(it)) }
        }
        is PsiLiteralExpression -> toValue()
        else -> StringValue(text ?: "")
    }

    private fun PsiLiteralExpression.toValue(): AnnotationParameterValue? = when (type) {
        PsiType.INT -> (value as? Int)?.let { IntValue(it) }
        PsiType.LONG -> (value as? Long)?.let { LongValue(it) }
        PsiType.FLOAT -> (value as? Float)?.let { FloatValue(it) }
        PsiType.DOUBLE -> (value as? Double)?.let { DoubleValue(it) }
        PsiType.BOOLEAN -> (value as? Boolean)?.let { BooleanValue(it) }
        PsiType.NULL -> NullValue
        else -> StringValue(text ?: "")
    }

    private fun PsiAnnotation.toAnnotation(): Annotations.Annotation? {
        // TODO Mitigating workaround for issue https://github.com/Kotlin/dokka/issues/1341
        //  Tracking https://youtrack.jetbrains.com/issue/KT-41234
        //  Needs to be removed once this issue is fixed in light classes
        fun PsiElement.getAnnotationsOrNull(): Array<PsiAnnotation>? {
            this as PsiClass
            return try {
                this.annotations
            } catch (e: Exception) {
                logger.warn("Failed to get annotations from ${this.qualifiedName}")
                null
            }
        }

        return psiReference?.let { psiElement ->
            Annotations.Annotation(
                dri = DRI.from(psiElement),
                params = attributes
                    .filter { !lightMethodChecker.isLightAnnotationAttribute(it) }
                    .mapNotNull { it.attributeName to it.toValue() }
                    .toMap(),
                mustBeDocumented = psiElement.getAnnotationsOrNull().orEmpty().any { annotation ->
                    annotation.hasQualifiedName("java.lang.annotation.Documented")
                }
            )
        }
    }

    private val PsiElement.psiReference
        get() = getChildOfType<PsiJavaCodeReferenceElement>()?.resolve()
}
