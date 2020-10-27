/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.indexer

enum class Language(val sourceFileExtension: String) {
    C("c"),
    OBJECTIVE_C("m")
}

interface HeaderInclusionPolicy {
    /**
     * Whether unused declarations from given header should be excluded.
     *
     * @param headerName header path relative to the appropriate include path element (e.g. `time.h` or `curl/curl.h`),
     * or `null` for builtin declarations.
     */
    fun excludeUnused(headerName: String?): Boolean
}

interface HeaderExclusionPolicy {
    /**
     * Whether all declarations from this header should be excluded.
     *
     * Note: the declarations from such headers can be actually present in the internal representation,
     * but not included into the root collections.
     */
    fun excludeAll(headerId: HeaderId): Boolean
}

sealed class NativeLibraryHeaderFilter {
    class NameBased(
            val policy: HeaderInclusionPolicy,
            val excludeDepdendentModules: Boolean
    ) : NativeLibraryHeaderFilter()

    class Predefined(val headers: Set<String>) : NativeLibraryHeaderFilter()
}

interface Compilation {
    val includes: List<String>
    val additionalPreambleLines: List<String>
    val compilerArgs: List<String>
    val language: Language
}

data class CompilationWithPCH(
        override val compilerArgs: List<String>,
        override val language: Language
) : Compilation {

    constructor(compilerArgs: List<String>, precompiledHeader: String, language: Language)
            : this(compilerArgs + listOf("-include-pch", precompiledHeader), language)

    override val includes: List<String>
        get() = emptyList()

    override val additionalPreambleLines: List<String>
        get() = emptyList()
}

// TODO: Compilation hierarchy seems to require some refactoring.

data class NativeLibrary(override val includes: List<String>,
                         override val additionalPreambleLines: List<String>,
                         override val compilerArgs: List<String>,
                         val headerToIdMapper: HeaderToIdMapper,
                         override val language: Language,
                         val excludeSystemLibs: Boolean, // TODO: drop?
                         val headerExclusionPolicy: HeaderExclusionPolicy,
                         val headerFilter: NativeLibraryHeaderFilter) : Compilation

data class IndexerResult(val index: NativeIndex, val compilation: CompilationWithPCH)

/**
 * Retrieves the definitions from given C header file using given compiler arguments (e.g. defines).
 */
fun buildNativeIndex(library: NativeLibrary, verbose: Boolean): IndexerResult = buildNativeIndexImpl(library, verbose)

/**
 * This class describes the IR of definitions from C header file(s).
 */
abstract class NativeIndex {
    abstract val structs: Collection<StructDecl>
    abstract val enums: Collection<EnumDef>
    abstract val objCClasses: Collection<ObjCClass>
    abstract val objCProtocols: Collection<ObjCProtocol>
    abstract val objCCategories: Collection<ObjCCategory>
    abstract val typedefs: Collection<TypedefDef>
    abstract val functions: Collection<FunctionDecl>
    abstract val macroConstants: Collection<ConstantDef>
    abstract val wrappedMacros: Collection<WrappedMacroDef>
    abstract val globals: Collection<GlobalDecl>
    abstract val includedHeaders: Collection<HeaderId>
}

/**
 * The (contents-based) header id.
 * Its [value] remains valid across different runs of the indexer and the process,
 * and thus can be used to 'serialize' the id.
 */
data class HeaderId(val value: String)

data class Location(val headerId: HeaderId)

interface TypeDeclaration {
    val location: Location
}

sealed class StructMember(val name: String, val type: Type) {
    abstract val offset: Long?
}

/**
 * C struct field.
 */
class Field(name: String, type: Type, override val offset: Long, val typeSize: Long, val typeAlign: Long)
    : StructMember(name, type)

val Field.isAligned: Boolean
    get() = offset % (typeAlign * 8) == 0L

class BitField(name: String, type: Type, override val offset: Long, val size: Int) : StructMember(name, type)

class IncompleteField(name: String, type: Type) : StructMember(name, type) {
    override val offset: Long? get() = null
}

/**
 * C struct declaration.
 */
abstract class StructDecl(val spelling: String) : TypeDeclaration {

    abstract val def: StructDef?
}

/**
 * C struct definition.
 *
 * @param hasNaturalLayout must be `false` if the struct has unnatural layout, e.g. it is `packed`.
 * May be `false` even if the struct has natural layout.
 */
abstract class StructDef(val size: Long, val align: Int, val decl: StructDecl) {

    enum class Kind {
        STRUCT, UNION
    }

    abstract val members: List<StructMember>
    abstract val kind: Kind

    val fields: List<Field> get() = members.filterIsInstance<Field>()
    val bitFields: List<BitField> get() = members.filterIsInstance<BitField>()
}

/**
 * C enum value.
 */
class EnumConstant(val name: String, val value: Long, val isExplicitlyDefined: Boolean)

/**
 * C enum definition.
 */
abstract class EnumDef(val spelling: String, val baseType: Type) : TypeDeclaration {

    abstract val constants: List<EnumConstant>
}

sealed class ObjCContainer {
    abstract val protocols: List<ObjCProtocol>
    abstract val methods: List<ObjCMethod>
    abstract val properties: List<ObjCProperty>
}

sealed class ObjCClassOrProtocol(val name: String) : ObjCContainer(), TypeDeclaration {
    abstract val isForwardDeclaration: Boolean
}

data class ObjCMethod(
        val selector: String, val encoding: String, val parameters: List<Parameter>, private val returnType: Type,
        val isVariadic: Boolean, val isClass: Boolean, val nsConsumesSelf: Boolean, val nsReturnsRetained: Boolean,
        val isOptional: Boolean, val isInit: Boolean, val isExplicitlyDesignatedInitializer: Boolean
) {

    fun returnsInstancetype(): Boolean = returnType is ObjCInstanceType

    fun getReturnType(container: ObjCClassOrProtocol): Type = if (returnType is ObjCInstanceType) {
        when (container) {
            is ObjCClass -> ObjCObjectPointer(container, returnType.nullability, protocols = emptyList())
            is ObjCProtocol -> ObjCIdType(returnType.nullability, protocols = listOf(container))
        }
    } else {
        returnType
    }
}

data class ObjCProperty(val name: String, val getter: ObjCMethod, val setter: ObjCMethod?) {
    fun getType(container: ObjCClassOrProtocol): Type = getter.getReturnType(container)
}

abstract class ObjCClass(name: String) : ObjCClassOrProtocol(name) {
    abstract val binaryName: String?
    abstract val baseClass: ObjCClass?
}
abstract class ObjCProtocol(name: String) : ObjCClassOrProtocol(name)

abstract class ObjCCategory(val name: String, val clazz: ObjCClass) : ObjCContainer()

/**
 * C function parameter.
 */
data class Parameter(val name: String?, val type: Type, val nsConsumed: Boolean)

/**
 * C function declaration.
 */
class FunctionDecl(val name: String, val parameters: List<Parameter>, val returnType: Type, val binaryName: String,
                   val isDefined: Boolean, val isVararg: Boolean)

/**
 * C typedef definition.
 *
 * ```
 * typedef $aliased $name;
 * ```
 */
class TypedefDef(val aliased: Type, val name: String, override val location: Location) : TypeDeclaration

abstract class MacroDef(val name: String)

abstract class ConstantDef(name: String, val type: Type): MacroDef(name)
class IntegerConstantDef(name: String, type: Type, val value: Long) : ConstantDef(name, type)
class FloatingConstantDef(name: String, type: Type, val value: Double) : ConstantDef(name, type)
class StringConstantDef(name: String, type: Type, val value: String) : ConstantDef(name, type)

class WrappedMacroDef(name: String, val type: Type) : MacroDef(name)

class GlobalDecl(val name: String, val type: Type, val isConst: Boolean)

/**
 * C type.
 */
interface Type

interface PrimitiveType : Type

object CharType : PrimitiveType

open class BoolType: PrimitiveType

object CBoolType : BoolType()

object ObjCBoolType : BoolType()
// We omit `const` qualifier for IntegerType and FloatingType to make `CBridgeGen` simpler.
// See KT-28102.
data class IntegerType(val size: Int, val isSigned: Boolean, val spelling: String) : PrimitiveType

// TODO: floating type is not actually defined entirely by its size.
data class FloatingType(val size: Int, val spelling: String) : PrimitiveType

data class VectorType(val elementType: Type, val elementCount: Int, val spelling: String) : PrimitiveType

object VoidType : Type

data class RecordType(val decl: StructDecl) : Type

data class EnumType(val def: EnumDef) : Type

data class PointerType(val pointeeType: Type, val pointeeIsConst: Boolean = false) : Type
// TODO: refactor type representation and support type modifiers more generally.

data class FunctionType(val parameterTypes: List<Type>, val returnType: Type) : Type

interface ArrayType : Type {
    val elemType: Type
}

data class ConstArrayType(override val elemType: Type, val length: Long) : ArrayType
data class IncompleteArrayType(override val elemType: Type) : ArrayType
data class VariableArrayType(override val elemType: Type) : ArrayType

data class Typedef(val def: TypedefDef) : Type

sealed class ObjCPointer : Type {
    enum class Nullability {
        Nullable, NonNull, Unspecified
    }

    abstract val nullability: Nullability
}

sealed class ObjCQualifiedPointer : ObjCPointer() {
    abstract val protocols: List<ObjCProtocol>
}

data class ObjCObjectPointer(
        val def: ObjCClass,
        override val nullability: Nullability,
        override val protocols: List<ObjCProtocol>
) : ObjCQualifiedPointer()

data class ObjCClassPointer(
        override val nullability: Nullability,
        override val protocols: List<ObjCProtocol>
) : ObjCQualifiedPointer()

data class ObjCIdType(
        override val nullability: Nullability,
        override val protocols: List<ObjCProtocol>
) : ObjCQualifiedPointer()

data class ObjCInstanceType(override val nullability: Nullability) : ObjCPointer()
data class ObjCBlockPointer(
        override val nullability: Nullability,
        val parameterTypes: List<Type>, val returnType: Type
) : ObjCPointer()

object UnsupportedType : Type
