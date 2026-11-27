/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaTypeParameter
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.AbstractTypeMapper.writeFormalTypeParameter
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

internal class AptName(private val value: String) : Name {
    override fun contentEquals(cs: CharSequence) = value == cs.toString()
    override val length: Int get() = value.length
    override fun get(index: Int) = value[index]
    override fun subSequence(startIndex: Int, endIndex: Int) = value.subSequence(startIndex, endIndex)
    override fun toString() = value
    override fun equals(other: Any?) = other is AptName && other.value == value
    override fun hashCode() = value.hashCode()
}

internal abstract class AptElement(val env: AptEnv) : Element {
    protected abstract val mirrors: List<AptAnnotationMirror>

    override fun getAnnotationMirrors(): List<AnnotationMirror> = mirrors

    override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? {
        val mirror = mirrors.firstOrNull { it.annotationElement.qualifiedName.contentEquals(annotationType.canonicalName) } ?: return null
        return createAnnotationProxy(env, mirror, annotationType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<A> {
        val found = listOfNotNull(getAnnotation(annotationType))
        val array = java.lang.reflect.Array.newInstance(annotationType, found.size) as Array<A>
        found.forEachIndexed { i, a -> array[i] = a }
        return array
    }
}

internal class AptPackageElement(env: AptEnv, private val name: String) : AptElement(env), PackageElement {
    override val mirrors: List<AptAnnotationMirror> get() = emptyList()

    override fun asType(): TypeMirror = AptNoType(TypeKind.PACKAGE)
    override fun getKind() = ElementKind.PACKAGE
    override fun getModifiers(): Set<Modifier> = emptySet()
    override fun getQualifiedName(): Name = AptName(name)
    override fun getSimpleName(): Name = AptName(name.substringAfterLast('.'))
    override fun getEnclosedElements(): List<Element> = env.sourceRoots.filter { it.packageName == name }
    override fun isUnnamed() = name.isEmpty()
    override fun getEnclosingElement(): Element? = null
    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R = v.visitPackage(this, p)
    override fun toString() = name
}

/** A class: of Kotlin sources or libraries, of Java libraries or the JDK, a file facade, or a class that is not found. */
internal abstract class AptTypeElement(env: AptEnv) : AptElement(env), TypeElement {
    abstract val internalName: String
    abstract val isSource: Boolean
    open val isError: Boolean get() = false
    abstract val outerElement: AptTypeElement?

    /** Non-static member classes; their instances need an outer instance. */
    abstract val isInner: Boolean

    protected abstract val simpleNameString: String
    protected abstract val modifierSet: Set<Modifier>
    protected abstract val supertypes: Pair<TypeMirror, List<TypeMirror>>
    abstract val fields: List<AptVariableElement>
    abstract val methods: List<AptExecutableElement>
    abstract val memberClasses: List<AptTypeElement>
    abstract override fun getTypeParameters(): List<AptTypeParameterElement>

    val packageName: String get() = internalName.substringBeforeLast('/', "").replace('/', '.')

    private val qualifiedNameString: String by lazy {
        val outer = outerElement
        when {
            outer != null -> outer.qualifiedNameString + "." + simpleNameString
            packageName.isEmpty() -> simpleNameString
            else -> "$packageName.$simpleNameString"
        }
    }

    val errorType: AptErrorType by lazy { AptErrorType(this) }

    val defaultEnclosingType: TypeMirror get() = if (isInner) outerElement?.asType() ?: env.noneType else env.noneType

    /** The raw type of a generic class, or its only type for a non-generic one. */
    val rawType: AptDeclaredType by lazy {
        AptDeclaredType(this, emptyList(), if (isInner) outerElement?.rawType ?: env.noneType else env.noneType)
    }

    private val declaredType: TypeMirror by lazy {
        if (isError) errorType else AptDeclaredType(this, typeParameters.map { it.asType() }, defaultEnclosingType)
    }

    fun resolveTypeVariable(name: String): TypeMirror {
        typeParameters.firstOrNull { it.name == name }?.let { return it.asType() }
        outerElement?.let { return it.resolveTypeVariable(name) }
        return env.typeElementOrError(name).errorType
    }

    override fun asType(): TypeMirror = declaredType
    override fun getModifiers(): Set<Modifier> = modifierSet
    override fun getNestingKind(): NestingKind = if (outerElement == null) NestingKind.TOP_LEVEL else NestingKind.MEMBER
    override fun getQualifiedName(): Name = AptName(qualifiedNameString)
    override fun getSimpleName(): Name = AptName(simpleNameString)
    override fun getSuperclass(): TypeMirror = supertypes.first
    override fun getInterfaces(): List<TypeMirror> = supertypes.second
    override fun getEnclosingElement(): Element = outerElement ?: env.packageElement(packageName) ?: AptPackageElement(env, packageName)

    private val enclosed: List<Element> by lazy { fields + methods + memberClasses }

    override fun getEnclosedElements(): List<Element> = enclosed
    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R = v.visitType(this, p)
    override fun toString() = qualifiedNameString
}

/** A Kotlin class of the sources or of a library, with the members the JVM backend generates for it. */
@OptIn(DirectDeclarationsAccess::class)
internal class AptKotlinClassElement(env: AptEnv, val klass: FirRegularClass, override val isSource: Boolean) : AptTypeElement(env) {
    val classId: ClassId = klass.symbol.classId

    override val internalName: String = JvmClassName.internalNameByClassId(classId)
    override val simpleNameString: String = classId.shortClassName.asString()
    override val outerElement: AptTypeElement? by lazy { classId.outerClassId?.let(env::typeElement) }
    override val isInner: Boolean get() = klass.isInner

    override fun getKind(): ElementKind = klass.classKind.toElementKind()

    override val modifierSet: Set<Modifier> by lazy {
        val result = mutableSetOf<Modifier>()
        if (classId.outerClassId == null) {
            if (klass.visibility != Visibilities.Private) result += Modifier.PUBLIC
        } else {
            result.addVisibility(klass.visibility)
            if (!klass.isInner) result += Modifier.STATIC
        }
        when (klass.classKind) {
            ClassKind.ANNOTATION_CLASS, ClassKind.INTERFACE -> result += Modifier.ABSTRACT
            ClassKind.ENUM_CLASS -> when {
                klass.modality == Modality.ABSTRACT || klass.modality == Modality.SEALED -> result += Modifier.ABSTRACT
                klass.declarations.none { it is FirEnumEntry && it.requiresSubclass() } -> result += Modifier.FINAL
            }
            else -> when (klass.modality ?: Modality.FINAL) {
                Modality.FINAL -> result += Modifier.FINAL
                Modality.ABSTRACT, Modality.SEALED -> result += Modifier.ABSTRACT
                Modality.OPEN -> {}
            }
        }
        result
    }

    private val typeParameterElements: List<AptTypeParameterElement> by lazy {
        kotlinTypeParameters(env, this, klass.typeParameters.filterIsInstance<FirTypeParameter>(), ::resolveTypeVariable)
    }

    override fun getTypeParameters(): List<AptTypeParameterElement> = typeParameterElements

    override val supertypes: Pair<TypeMirror, List<TypeMirror>> by lazy {
        val superTypes = klass.superConeTypes
        fun map(type: ConeKotlinType) = env.typeOf(type, TypeMappingMode.SUPER_TYPE, ::resolveTypeVariable)
        val superClassType = superTypes.firstOrNull { type -> type.toRegularClassSymbol(env.session) != null && !type.isInterface(env) }
        val superclass = when (klass.classKind) {
            ClassKind.INTERFACE, ClassKind.ANNOTATION_CLASS -> env.noneType
            ClassKind.ENUM_CLASS -> {
                val enum = env.typeElementOrError("java/lang/Enum")
                if (enum.isError) enum.errorType else AptDeclaredType(enum, listOf(asType()), env.noneType)
            }
            else -> superClassType?.let(::map) ?: env.objectType
        }
        superclass to superTypes.filter { it !== superClassType && it.isInterface(env) }.map(::map)
    }

    override val mirrors: List<AptAnnotationMirror> by lazy { declarationAnnotations(env, klass, klass.annotations, isSource) }

    private val members: AptKotlinMembers.Members by lazy { AptKotlinMembers(env).classMembers(this) }

    override val fields: List<AptVariableElement> get() = members.fields
    override val methods: List<AptExecutableElement> get() = members.methods

    override val memberClasses: List<AptTypeElement> by lazy {
        klass.declarations.filterIsInstance<FirRegularClass>().mapNotNull { env.typeElement(it.symbol.classId) }
    }
}

/** A Java class of a library or of the JDK, as FIR declares it. */
@OptIn(DirectDeclarationsAccess::class)
internal class AptJavaClassElement(env: AptEnv, val klass: FirJavaClass) : AptTypeElement(env) {
    val classId: ClassId = klass.symbol.classId

    override val isSource: Boolean get() = false
    override val internalName: String = JvmClassName.internalNameByClassId(classId)
    override val simpleNameString: String = classId.shortClassName.asString()
    override val outerElement: AptTypeElement? by lazy { classId.outerClassId?.let(env::typeElement) }
    override val isInner: Boolean get() = klass.isInner

    override fun getKind(): ElementKind = klass.classKind.toElementKind()

    override val modifierSet: Set<Modifier> by lazy {
        val result = mutableSetOf<Modifier>()
        result.addVisibility(klass.visibility)
        if (classId.outerClassId != null && !klass.isInner) result += Modifier.STATIC
        when (klass.classKind) {
            ClassKind.ANNOTATION_CLASS, ClassKind.INTERFACE -> result += Modifier.ABSTRACT
            else -> when (klass.modality) {
                Modality.FINAL -> result += Modifier.FINAL
                Modality.ABSTRACT, Modality.SEALED -> result += Modifier.ABSTRACT
                else -> {}
            }
        }
        result
    }

    private val typeParameterElements: List<AptTypeParameterElement> by lazy {
        klass.nonEnhancedTypeParameters.filterIsInstance<FirJavaTypeParameter>().map { parameter ->
            AptTypeParameterElement(env, this, parameter.name.asString()) {
                parameter.javaTypeParameter.upperBounds.map { env.typeOf(it, ::resolveTypeVariable) }
            }
        }
    }

    override fun getTypeParameters(): List<AptTypeParameterElement> = typeParameterElements

    override val supertypes: Pair<TypeMirror, List<TypeMirror>> by lazy {
        val superTypes = klass.superTypeRefs.map { it.coneType }
        fun map(type: ConeKotlinType) = env.typeOf(type, TypeMappingMode.SUPER_TYPE, ::resolveTypeVariable)
        val superClassType = superTypes.firstOrNull { !it.isInterface(env) }
        val superclass = when {
            klass.classKind == ClassKind.INTERFACE || klass.classKind == ClassKind.ANNOTATION_CLASS -> env.noneType
            internalName == "java/lang/Object" -> env.noneType
            else -> superClassType?.let(::map) ?: env.objectType
        }
        superclass to superTypes.filter { it !== superClassType && it.isInterface(env) }.map(::map)
    }

    override val mirrors: List<AptAnnotationMirror> by lazy { klass.annotations.mapNotNull { AptAnnotationMirror.of(env, it) } }

    private val members: Pair<List<AptVariableElement>, List<AptExecutableElement>> by lazy { AptJavaMembers(env).members(this) }

    override val fields: List<AptVariableElement> get() = members.first
    override val methods: List<AptExecutableElement> get() = members.second

    override val memberClasses: List<AptTypeElement> by lazy {
        klass.existingNestedClassifierNames.mapNotNull { env.typeElement(classId.createNestedClassId(it)) }
    }
}

/** The class of the top-level functions and properties of a Kotlin file. */
@OptIn(DirectDeclarationsAccess::class)
internal class AptFacadeElement private constructor(
    env: AptEnv,
    val file: FirFile,
    override val internalName: String,
) : AptTypeElement(env) {
    override val isSource: Boolean get() = true
    override val simpleNameString: String = internalName.substringAfterLast('/')
    override val outerElement: AptTypeElement? get() = null
    override val isInner: Boolean get() = false
    override fun getKind(): ElementKind = ElementKind.CLASS
    override val modifierSet: Set<Modifier> = setOf(Modifier.PUBLIC, Modifier.FINAL)
    override fun getTypeParameters(): List<AptTypeParameterElement> = emptyList()
    override val supertypes: Pair<TypeMirror, List<TypeMirror>> by lazy { env.objectType to emptyList() }
    override val mirrors: List<AptAnnotationMirror> by lazy { declarationAnnotations(env, null, file.annotations, isSource = true) }

    private val members: AptKotlinMembers.Members by lazy { AptKotlinMembers(env).facadeMembers(this) }

    override val fields: List<AptVariableElement> get() = members.fields
    override val methods: List<AptExecutableElement> get() = members.methods
    override val memberClasses: List<AptTypeElement> get() = emptyList()

    companion object {
        private val JVM_MULTIFILE_CLASS = ClassId.fromString("kotlin/jvm/JvmMultifileClass")

        /** The facade of [file], or `null` if the file has no top-level callables or is a part of a multifile class. */
        fun of(env: AptEnv, file: FirFile): AptFacadeElement? {
            val hasCallables = file.declarations.any { it is FirCallableDeclaration && !it.isExpect }
            if (!hasCallables || file.annotations.any { it.classIdOrNull(env) == JVM_MULTIFILE_CLASS }) return null
            val jvmName = file.annotations.firstOrNull { it.classIdOrNull(env) == JvmStandardClassIds.Annotations.JvmName }
                ?.let { (it.argumentMapping.mapping.values.firstOrNull() as? FirLiteralExpression)?.value as? String }
            val simpleName = jvmName ?: PackagePartClassUtils.getFilePartShortName(file.name)
            val packagePrefix = file.packageDirective.packageFqName.asString().replace('.', '/')
            return AptFacadeElement(env, file, if (packagePrefix.isEmpty()) simpleName else "$packagePrefix/$simpleName")
        }
    }
}

/** A class that is not found. It has no members. */
internal class AptErrorTypeElement(env: AptEnv, override val internalName: String) : AptTypeElement(env) {
    override val isSource: Boolean get() = false
    override val isError: Boolean get() = true
    override val simpleNameString: String = internalName.substringAfterLast('/')
    override val outerElement: AptTypeElement? get() = null
    override val isInner: Boolean get() = false
    override fun getKind(): ElementKind = ElementKind.CLASS
    override val modifierSet: Set<Modifier> = setOf(Modifier.PUBLIC)
    override fun getTypeParameters(): List<AptTypeParameterElement> = emptyList()
    override val supertypes: Pair<TypeMirror, List<TypeMirror>> by lazy { env.objectType to emptyList() }
    override val mirrors: List<AptAnnotationMirror> get() = emptyList()
    override val fields: List<AptVariableElement> get() = emptyList()
    override val methods: List<AptExecutableElement> get() = emptyList()
    override val memberClasses: List<AptTypeElement> get() = emptyList()
}

internal class AptTypeParameterElement(
    env: AptEnv,
    private val generic: Element,
    val name: String,
    bounds: () -> List<TypeMirror>,
) : AptElement(env), TypeParameterElement {
    override val mirrors: List<AptAnnotationMirror> get() = emptyList()

    private val typeVariable = AptTypeVariable(this)

    val boundTypes: List<TypeMirror> by lazy(bounds)

    override fun asType(): TypeMirror = typeVariable
    override fun getKind() = ElementKind.TYPE_PARAMETER
    override fun getModifiers(): Set<Modifier> = emptySet()
    override fun getSimpleName(): Name = AptName(name)
    override fun getGenericElement(): Element = generic
    override fun getBounds(): List<TypeMirror> = boundTypes.ifEmpty { listOf(env.objectType) }
    override fun getEnclosingElement(): Element = generic
    override fun getEnclosedElements(): List<Element> = emptyList()
    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R = v.visitTypeParameter(this, p)
    override fun toString() = name
}

/** A field or an enum constant. */
internal class AptVariableElement(
    env: AptEnv,
    private val owner: AptTypeElement,
    private val name: String,
    private val kind: ElementKind,
    private val modifierSet: Set<Modifier>,
    type: () -> TypeMirror,
    annotations: () -> List<AptAnnotationMirror>,
    private val constantValue: Any? = null,
) : AptElement(env), VariableElement {
    private val resolvedType by lazy(type)
    override val mirrors: List<AptAnnotationMirror> by lazy(annotations)

    override fun asType(): TypeMirror = resolvedType
    override fun getKind() = kind
    override fun getModifiers(): Set<Modifier> = modifierSet
    override fun getSimpleName(): Name = AptName(name)
    override fun getConstantValue(): Any? = constantValue
    override fun getEnclosingElement(): Element = owner
    override fun getEnclosedElements(): List<Element> = emptyList()
    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R = v.visitVariable(this, p)
    override fun toString() = name
}

internal class AptParameterElement(
    env: AptEnv,
    private val method: AptExecutableElement,
    private val name: String,
    type: () -> TypeMirror,
    annotations: () -> List<AptAnnotationMirror>,
) : AptElement(env), VariableElement {
    private val resolvedType by lazy(type)
    override val mirrors: List<AptAnnotationMirror> by lazy(annotations)

    override fun asType(): TypeMirror = resolvedType
    override fun getKind() = ElementKind.PARAMETER
    override fun getModifiers(): Set<Modifier> = emptySet()
    override fun getSimpleName(): Name = AptName(name)
    override fun getConstantValue(): Any? = null
    override fun getEnclosingElement(): Element = method
    override fun getEnclosedElements(): List<Element> = emptyList()
    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R = v.visitVariable(this, p)
    override fun toString() = name
}

/** A method or a constructor. [AptKotlinMembers] and [AptJavaMembers] fill in the parameters and the types. */
internal class AptExecutableElement(
    env: AptEnv,
    val owner: AptTypeElement,
    private val name: String,
    private val kind: ElementKind,
    private val modifierSet: Set<Modifier>,
    private val isVarArgs: Boolean,
    annotations: () -> List<AptAnnotationMirror>,
) : AptElement(env), ExecutableElement {
    override val mirrors: List<AptAnnotationMirror> by lazy(annotations)

    var typeParameterElements: List<AptTypeParameterElement> = emptyList()
    var parameterElements: List<AptParameterElement> = emptyList()
    var returnTypeProvider: () -> TypeMirror = { env.voidType }
    var thrownTypesProvider: () -> List<TypeMirror> = { emptyList() }
    var defaultValueProvider: () -> AnnotationValue? = { null }

    private val resolvedReturnType: TypeMirror by lazy { returnTypeProvider() }
    private val resolvedThrownTypes: List<TypeMirror> by lazy { thrownTypesProvider() }
    private val resolvedDefaultValue: AnnotationValue? by lazy { defaultValueProvider() }

    fun resolveTypeVariable(name: String): TypeMirror =
        typeParameterElements.firstOrNull { it.name == name }?.asType() ?: owner.resolveTypeVariable(name)

    private val executableType: AptExecutableType by lazy {
        AptExecutableType(
            typeParameterElements.map { it.asType() as AptTypeVariable }, returnType, parameterElements.map { it.asType() }, thrownTypes,
        )
    }

    override fun asType(): TypeMirror = executableType
    override fun getKind() = kind
    override fun getModifiers(): Set<Modifier> = modifierSet
    override fun getSimpleName(): Name = AptName(name)
    override fun getTypeParameters(): List<AptTypeParameterElement> = typeParameterElements
    override fun getReturnType(): TypeMirror = if (kind == ElementKind.CONSTRUCTOR) env.voidType else resolvedReturnType
    override fun getParameters(): List<VariableElement> = parameterElements
    override fun getReceiverType(): TypeMirror = env.noneType
    override fun isVarArgs() = isVarArgs
    override fun isDefault() = Modifier.DEFAULT in modifierSet
    override fun getThrownTypes(): List<TypeMirror> = resolvedThrownTypes
    override fun getDefaultValue(): AnnotationValue? = resolvedDefaultValue
    override fun getEnclosingElement(): Element = owner
    override fun getEnclosedElements(): List<Element> = emptyList()
    override fun <R, P> accept(v: ElementVisitor<R, P>, p: P): R = v.visitExecutable(this, p)

    override fun toString() = buildString {
        append(if (kind == ElementKind.CONSTRUCTOR) owner.simpleName else name)
        parameterElements.joinTo(this, ",", "(", ")") { it.asType().toString() }
    }
}

internal fun ClassKind.toElementKind(): ElementKind = when (this) {
    ClassKind.ANNOTATION_CLASS -> ElementKind.ANNOTATION_TYPE
    ClassKind.INTERFACE -> ElementKind.INTERFACE
    ClassKind.ENUM_CLASS -> ElementKind.ENUM
    else -> ElementKind.CLASS
}

private fun ConeKotlinType.isInterface(env: AptEnv): Boolean {
    val kind = toRegularClassSymbol(env.session)?.classKind
    return kind == ClassKind.INTERFACE || kind == ClassKind.ANNOTATION_CLASS
}

internal fun MutableSet<Modifier>.addVisibility(visibility: Visibility) {
    when (visibility) {
        Visibilities.Public, Visibilities.Internal -> add(Modifier.PUBLIC)
        Visibilities.Protected, JavaVisibilities.ProtectedAndPackage, JavaVisibilities.ProtectedStaticVisibility -> add(Modifier.PROTECTED)
        Visibilities.Private, Visibilities.PrivateToThis -> add(Modifier.PRIVATE)
        JavaVisibilities.PackageVisibility -> {}
        else -> add(Modifier.PUBLIC)
    }
}

/** As in Fir2Ir: arguments alone also make an anonymous object, but only declared members make a class. */
@OptIn(DirectDeclarationsAccess::class)
internal fun FirEnumEntry.requiresSubclass(): Boolean {
    val initializer = initializer
    return initializer is FirAnonymousObjectExpression && initializer.anonymousObject.declarations.any { it !is FirConstructor }
}

/** Type parameters of a Kotlin declaration, with the bounds the JVM backend writes for them. */
internal fun kotlinTypeParameters(
    env: AptEnv, generic: Element, typeParameters: List<FirTypeParameter>, resolve: (String) -> TypeMirror,
): List<AptTypeParameterElement> {
    if (typeParameters.isEmpty()) return emptyList()
    val bounds: Map<String, List<TypeMirror>> by lazy {
        val writer = TypeMirrorWriter(env, resolve)
        for (typeParameter in typeParameters) {
            env.typeMapper.typeContext.writeFormalTypeParameter(typeParameter.symbol.toLookupTag(), writer) { type, mode ->
                env.typeMapper.mapStubType(type as ConeKotlinType, env.session, mode, writer)
            }
        }
        writer.finishFormalTypeParameters()
        writer.formalTypeParameters.toMap()
    }
    return typeParameters.map { typeParameter ->
        val name = typeParameter.name.asString()
        AptTypeParameterElement(env, generic, name) { bounds[name].orEmpty() }
    }
}
