/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Type
import javax.lang.model.SourceVersion
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Everything the model shares: the FIR session that all elements come from, element caches and the well-known types.
 *
 * Classes are found with the session's symbol provider: Kotlin sources and Kotlin libraries as FIR classes, Java libraries and the
 * JDK as FIR Java classes. Kotlin types become Java types through the compiler's JVM type mapper, Java types are the ones FIR keeps
 * for its Java declarations.
 */
@OptIn(DirectDeclarationsAccess::class, org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
internal class AptEnv(
    val session: FirSession,
    val scopeSession: ScopeSession,
    val firFiles: List<FirFile>,
    configuration: CompilerConfiguration,
    val stripMetadata: Boolean,
) {
    val typeMapper = FirJvmTypeMapper(session)
    val moduleName: String = configuration.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME

    private val byClassId = HashMap<ClassId, AptTypeElement?>()
    private val byInternalName = HashMap<String, AptTypeElement?>()
    private val errorElements = HashMap<String, AptErrorTypeElement>()
    private val packages = HashMap<String, AptPackageElement?>()
    private val primitives = TypeKind.entries.filter { it.isPrimitive }.associateWith { AptPrimitiveType(it) }

    val voidType = AptNoType(TypeKind.VOID)
    val noneType = AptNoType(TypeKind.NONE)

    val objectElement: AptTypeElement by lazy { typeElementOrError("java/lang/Object") }
    val objectType: TypeMirror by lazy { objectElement.asType() }

    fun primitive(kind: TypeKind): AptPrimitiveType = primitives.getValue(kind)

    private val sourceClassSymbols: Set<FirRegularClassSymbol> by lazy {
        val result = HashSet<FirRegularClassSymbol>()
        fun add(klass: FirRegularClass) {
            result += klass.symbol
            klass.declarations.filterIsInstance<FirRegularClass>().forEach(::add)
        }
        firFiles.forEach { file -> file.declarations.filterIsInstance<FirRegularClass>().forEach(::add) }
        result
    }

    private val facades: Map<String, AptFacadeElement> by lazy {
        val result = LinkedHashMap<String, AptFacadeElement>()
        for (file in firFiles) {
            val facade = AptFacadeElement.of(this, file) ?: continue
            result.putIfAbsent(facade.internalName, facade)
        }
        result
    }

    /** The top-level classes of the sources, as javac would see them in stubs. */
    val sourceRoots: List<AptTypeElement> by lazy {
        val result = mutableListOf<AptTypeElement>()
        for (file in firFiles) {
            for (declaration in file.declarations) {
                if (declaration !is FirRegularClass || declaration.isExpect) continue
                typeElement(declaration.symbol.classId)?.let(result::add)
            }
            facades.values.firstOrNull { it.file === file }?.let(result::add)
        }
        result.filter { SourceVersion.isName(it.qualifiedName.toString()) }
    }

    fun typeElement(classId: ClassId): AptTypeElement? = byClassId.getOrPut(classId) {
        when (val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)) {
            is FirRegularClassSymbol -> when (val klass = symbol.fir) {
                is FirJavaClass -> AptJavaClassElement(this, klass)
                else -> AptKotlinClassElement(this, klass, isSource = symbol in sourceClassSymbols)
            }
            else -> null
        }
    }

    /** Finds a class by its JVM internal name, where `$` separates nested classes but may also be a part of a name. */
    fun typeElement(internalName: String): AptTypeElement? = byInternalName.getOrPut(internalName) {
        facades[internalName]?.let { return@getOrPut it }
        val packageName = internalName.substringBeforeLast('/', "").replace('/', '.')
        val relativeName = internalName.substringAfterLast('/')
        val parts = relativeName.split('$')
        // Every way to join the parts into class names: `A$B$C` is `A.B.C`, `A$B.C`, `A.B$C` or `A$B$C`.
        fun candidates(index: Int, prefix: List<String>): Sequence<List<String>> = sequence {
            if (index == parts.size) {
                yield(prefix)
                return@sequence
            }
            if (prefix.isNotEmpty()) yieldAll(candidates(index + 1, prefix.dropLast(1) + (prefix.last() + "$" + parts[index])))
            yieldAll(candidates(index + 1, prefix + parts[index]))
        }
        val names = if (parts.size > 6 || parts.any { it.isEmpty() }) sequenceOf(listOf(relativeName)) else candidates(0, emptyList())
        for (names in names) {
            val classId = ClassId(FqName(packageName), FqName.fromSegments(names), isLocal = false)
            typeElement(classId)?.let { return@getOrPut it }
        }
        null
    }

    fun typeElementOrError(internalName: String): AptTypeElement =
        typeElement(internalName) ?: errorElements.getOrPut(internalName) { AptErrorTypeElement(this, internalName) }

    fun packageElement(name: String): AptPackageElement? = packages.getOrPut(name) {
        if (name.isEmpty() || session.symbolProvider.hasPackage(FqName(name))) AptPackageElement(this, name) else null
    }

    /** The Java type of a Kotlin type, as the JVM backend maps it in [mode]. Type variables are resolved by name with [resolve]. */
    fun typeOf(type: ConeKotlinType, mode: TypeMappingMode, resolve: (String) -> TypeMirror): TypeMirror {
        val writer = TypeMirrorWriter(this, resolve)
        typeMapper.mapStubType(type, session, mode, writer)
        return writer.result
    }

    /** The Java type of a JVM type without generic arguments. */
    fun typeOf(type: Type): TypeMirror = when (type.sort) {
        Type.VOID -> voidType
        Type.BOOLEAN -> primitive(TypeKind.BOOLEAN)
        Type.CHAR -> primitive(TypeKind.CHAR)
        Type.BYTE -> primitive(TypeKind.BYTE)
        Type.SHORT -> primitive(TypeKind.SHORT)
        Type.INT -> primitive(TypeKind.INT)
        Type.FLOAT -> primitive(TypeKind.FLOAT)
        Type.LONG -> primitive(TypeKind.LONG)
        Type.DOUBLE -> primitive(TypeKind.DOUBLE)
        Type.ARRAY -> {
            var result = typeOf(type.elementType)
            repeat(type.dimensions) { result = AptArrayType(result) }
            result
        }
        else -> typeElementOrError(type.internalName).let { if (it.isError) it.errorType else it.rawType }
    }

    /** The type of a Java declaration, as FIR keeps it for the declaration. */
    fun typeOf(type: JavaType?, resolve: (String) -> TypeMirror): TypeMirror = when (type) {
        null -> objectType
        is JavaPrimitiveType -> when (type.type) {
            null -> voidType
            PrimitiveType.BOOLEAN -> primitive(TypeKind.BOOLEAN)
            PrimitiveType.CHAR -> primitive(TypeKind.CHAR)
            PrimitiveType.BYTE -> primitive(TypeKind.BYTE)
            PrimitiveType.SHORT -> primitive(TypeKind.SHORT)
            PrimitiveType.INT -> primitive(TypeKind.INT)
            PrimitiveType.FLOAT -> primitive(TypeKind.FLOAT)
            PrimitiveType.LONG -> primitive(TypeKind.LONG)
            PrimitiveType.DOUBLE -> primitive(TypeKind.DOUBLE)
        }
        is JavaArrayType -> AptArrayType(typeOf(type.componentType, resolve))
        is JavaWildcardType -> {
            val bound = type.bound?.let { typeOf(it, resolve) }
            if (type.isExtends) AptWildcardType(bound, null) else AptWildcardType(null, bound)
        }
        is JavaClassifierType -> when (val classifier = type.classifier) {
            is JavaTypeParameter -> resolve(classifier.name.asString())
            is JavaClass -> {
                val element = classifier.classId()?.let(::typeElement)
                when {
                    element == null -> typeElementOrError(type.classifierQualifiedName.replace('.', '/')).errorType
                    type.isRaw || type.typeArguments.isEmpty() -> element.rawType
                    else -> {
                        val ownCount = element.typeParameters.size
                        val arguments = type.typeArguments.takeLast(ownCount).map { typeOf(it, resolve) }
                        if (arguments.size == ownCount) AptDeclaredType(element, arguments, element.defaultEnclosingType) else element.rawType
                    }
                }
            }
            else -> typeElementOrError(type.classifierQualifiedName.replace('.', '/')).errorType
        }
        else -> objectType
    }

    private fun JavaClass.classId(): ClassId? {
        val outer = outerClass ?: return fqName?.let(ClassId::topLevel)
        return outer.classId()?.createNestedClassId(name)
    }
}

/**
 * Builds a [TypeMirror] from the calls of the JVM type mapper, which writes a type as it would write a generic signature. Type variables
 * are resolved by name with [resolve]; formal type parameters are collected with their bounds.
 */
internal class TypeMirrorWriter(private val env: AptEnv, private val resolve: (String) -> TypeMirror) : JvmSignatureWriter() {
    private abstract class Frame {
        abstract fun accept(type: TypeMirror)
    }

    private inner class ResultFrame : Frame() {
        var type: TypeMirror? = null
        override fun accept(type: TypeMirror) {
            this.type = type
        }
    }

    private class ClassFrame(val internalName: String, val enclosing: TypeMirror?) : Frame() {
        val arguments = mutableListOf<TypeMirror>()
        override fun accept(type: TypeMirror) {
            arguments += type
        }
    }

    private class ArgumentFrame(val variance: Variance) : Frame() {
        var type: TypeMirror? = null
        override fun accept(type: TypeMirror) {
            this.type = type
        }
    }

    private class ArrayFrame : Frame() {
        var component: TypeMirror? = null
        override fun accept(type: TypeMirror) {
            component = type
        }
    }

    private class BoundsFrame(val name: String) : Frame() {
        val bounds = mutableListOf<TypeMirror>()
        override fun accept(type: TypeMirror) {
            bounds += type
        }
    }

    private val root = ResultFrame()
    private val stack = ArrayDeque<Frame>().apply { addLast(root) }

    /** Formal type parameters with their bounds, in the order the mapper wrote them. */
    val formalTypeParameters = mutableListOf<Pair<String, List<TypeMirror>>>()
    private var currentFormal: BoundsFrame? = null

    val result: TypeMirror get() = root.type ?: env.objectType

    private fun emit(type: TypeMirror) = stack.last().accept(type)

    override fun skipGenericSignature(): Boolean = false

    override fun writeAsmType(asmType: Type) {
        when (asmType.sort) {
            Type.ARRAY -> {
                writeArrayType()
                writeAsmType(Type.getType(asmType.descriptor.substring(1)))
                writeArrayEnd()
            }
            else -> emit(env.typeOf(asmType))
        }
    }

    override fun writeClass(objectType: Type) = writeAsmType(objectType)

    override fun writeClassBegin(asmType: Type) {
        stack.addLast(ClassFrame(asmType.internalName, null))
    }

    override fun writeOuterClassBegin(resultingAsmType: Type, outerInternalName: String) {
        stack.addLast(ClassFrame(outerInternalName, null))
    }

    override fun writeInnerClass(name: String) {
        val outer = stack.removeLast() as ClassFrame
        stack.addLast(ClassFrame(outer.internalName + "$" + name, declared(outer)))
    }

    override fun writeClassEnd() {
        val frame = stack.removeLast() as ClassFrame
        emit(declared(frame))
    }

    private fun declared(frame: ClassFrame): TypeMirror {
        val element = env.typeElementOrError(frame.internalName)
        if (element.isError) return element.errorType
        if (frame.arguments.isEmpty() && frame.enclosing == null) return element.rawType
        return AptDeclaredType(element, frame.arguments.toList(), frame.enclosing ?: element.defaultEnclosingType)
    }

    override fun writeTypeArgument(projectionKind: Variance) {
        stack.addLast(ArgumentFrame(projectionKind))
    }

    override fun writeTypeArgumentEnd() {
        val frame = stack.removeLast() as ArgumentFrame
        val type = frame.type ?: env.objectType
        emit(
            when (frame.variance) {
                Variance.OUT_VARIANCE -> AptWildcardType(type, null)
                Variance.IN_VARIANCE -> AptWildcardType(null, type)
                Variance.INVARIANT -> type
            }
        )
    }

    override fun writeUnboundedWildcard() = emit(AptWildcardType(null, null))

    override fun writeArrayType() {
        stack.addLast(ArrayFrame())
    }

    override fun writeArrayEnd() {
        val frame = stack.removeLast() as ArrayFrame
        emit(AptArrayType(frame.component ?: env.objectType))
    }

    override fun writeTypeVariable(name: Name, type: Type) = emit(resolve(name.asString()))

    override fun writeFormalTypeParameter(name: String) {
        currentFormal?.let { finishFormal(it) }
        currentFormal = BoundsFrame(name).also { stack.addLast(it) }
    }

    private fun finishFormal(frame: BoundsFrame) {
        check(stack.removeLast() === frame)
        formalTypeParameters += frame.name to frame.bounds.toList()
        currentFormal = null
    }

    /** Finishes the last formal type parameter. */
    fun finishFormalTypeParameters() {
        currentFormal?.let { finishFormal(it) }
    }

    override fun writeClassBound() {}
    override fun writeClassBoundEnd() {}
    override fun writeInterfaceBound() {}
    override fun writeInterfaceBoundEnd() {}
}
