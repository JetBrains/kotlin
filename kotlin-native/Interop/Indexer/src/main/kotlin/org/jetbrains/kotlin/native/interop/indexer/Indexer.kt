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

import clang.*
import clang.CXIdxEntityKind.*
import clang.CXTypeKind.*
import kotlinx.cinterop.*

private class StructDeclImpl(spelling: String, override val location: Location) : StructDecl(spelling) {
    override var def: StructDefImpl? = null
}

private class StructDefImpl(
        size: Long, align: Int, decl: StructDecl,
        override val kind: Kind
) : StructDef(
        size, align, decl
) {
    override val members = mutableListOf<StructMember>()
    override val methods = mutableListOf<FunctionDecl>()
    override val staticFields = mutableListOf<GlobalDecl>()
}

private class EnumDefImpl(spelling: String, type: Type, override val location: Location) : EnumDef(spelling, type) {
    override val constants = mutableListOf<EnumConstant>()
}

private interface ObjCContainerImpl {
    val protocols: MutableList<ObjCProtocol>
    val methods: MutableList<ObjCMethod>
    val properties: MutableList<ObjCProperty>
}

private class ObjCProtocolImpl(
        name: String,
        override val location: Location,
        override val isForwardDeclaration: Boolean
) : ObjCProtocol(name), ObjCContainerImpl {
    override val protocols = mutableListOf<ObjCProtocol>()
    override val methods = mutableListOf<ObjCMethod>()
    override val properties = mutableListOf<ObjCProperty>()
}

private class ObjCClassImpl(
        name: String,
        override val location: Location,
        override val isForwardDeclaration: Boolean,
        override val binaryName: String?
) : ObjCClass(name), ObjCContainerImpl {
    override val protocols = mutableListOf<ObjCProtocol>()
    override val methods = mutableListOf<ObjCMethod>()
    override val properties = mutableListOf<ObjCProperty>()
    override var baseClass: ObjCClass? = null
}

private class ObjCCategoryImpl(
        name: String, clazz: ObjCClass
) : ObjCCategory(name, clazz), ObjCContainerImpl {
    override val protocols = mutableListOf<ObjCProtocol>()
    override val methods = mutableListOf<ObjCMethod>()
    override val properties = mutableListOf<ObjCProperty>()
}


private fun getParentName(cursor: CValue<CXCursor>, pkg: List<String> = emptyList()) : String? { // }: List<String>? {
    // This doesn't work for anonymous C++ struct (such as typedef struct { void foo(); } TypeDefName)  as well as anon namespace
    // In contrast, clang_getTypeSpelling return fully qualified name for struct & class (incl. typedef anon struct),
    // but does not help for anything elde such as template member, namespace etc
    // So, TODO Use ultimately clang_getTypeSpelling for CXType_Record (no traversing needed) and traverse up the whole hierarchy for anythiong else
    // Unfortunately, this won't work too for variable decl with anon type like that: ''struct { void foo(); } x;''
    // while function is accessible as x.foo()

    // skip this (zero) level:

    val parent = clang_getCursorSemanticParent(cursor)
    if (clang_isDeclaration(parent.kind) == 0)
        return if (pkg.isNotEmpty()) pkg.joinToString("::") else null

    val type = clang_getCursorType(parent)
    if (type.kind == CXTypeKind.CXType_Record)
        return clang_getTypeSpelling(type).convertAndDispose()

    val nextPkg = if (parent.kind == CXCursorKind.CXCursor_Namespace) listOf(parent.spelling) + pkg else pkg
    return getParentName(parent, nextPkg)
}


internal class NativeIndexImpl(val library: NativeLibrary, val verbose: Boolean = false) : NativeIndex() {

    private sealed class DeclarationID {
        data class USR(val usr: String) : DeclarationID()
        object VaList : DeclarationID()
        object VaListTag : DeclarationID()
        object BuiltinVaList : DeclarationID()
        object Protocol : DeclarationID()
    }

    private inner class TypeDeclarationRegistry<D : TypeDeclaration> {
        private val all = mutableMapOf<DeclarationID, D>()

        val included = mutableListOf<D>()

        inline fun getOrPut(cursor: CValue<CXCursor>, create: () -> D) = getOrPut(cursor, create, configure = {})

        inline fun getOrPut(cursor: CValue<CXCursor>, create: () -> D, configure: (D) -> Unit): D {
            val key = getDeclarationId(cursor)
            return all.getOrElse(key) {

                val value = create()
                all[key] = value

                val headerId = getHeaderId(getContainingFile(cursor))
                if (!library.headerExclusionPolicy.excludeAll(headerId)) {
                    // This declaration is used, and thus should be included:
                    included.add(value)
                }

                configure(value)
                value
            }
        }

    }

    internal fun getHeaderId(file: CXFile?): HeaderId = getHeaderId(this.library, file)

    private fun getLocation(cursor: CValue<CXCursor>): Location {
        val headerId = getHeaderId(getContainingFile(cursor))
        return Location(headerId)
    }

    override val structs: List<StructDecl> get() = structRegistry.included
    private val structRegistry = TypeDeclarationRegistry<StructDeclImpl>()

    override val enums: List<EnumDef> get() = enumRegistry.included
    private val enumRegistry = TypeDeclarationRegistry<EnumDefImpl>()

    override val objCClasses: List<ObjCClass> get() = objCClassRegistry.included
    private val objCClassRegistry = TypeDeclarationRegistry<ObjCClassImpl>()

    override val objCProtocols: List<ObjCProtocol> get() = objCProtocolRegistry.included
    private val objCProtocolRegistry = TypeDeclarationRegistry<ObjCProtocolImpl>()

    override val objCCategories: Collection<ObjCCategory> get() = objCCategoryById.values
    private val objCCategoryById = mutableMapOf<DeclarationID, ObjCCategoryImpl>()

    override val typedefs get() = typedefRegistry.included
    private val typedefRegistry = TypeDeclarationRegistry<TypedefDef>()

    private val functionById = mutableMapOf<DeclarationID, FunctionDecl?>()

    override val functions: Collection<FunctionDecl>
        get() = functionById.values.filterNotNull()

    override val macroConstants = mutableListOf<ConstantDef>()
    override val wrappedMacros = mutableListOf<WrappedMacroDef>()

    private val globalById = mutableMapOf<DeclarationID, GlobalDecl>()

    override val globals: Collection<GlobalDecl>
        get() = globalById.values

    override lateinit var includedHeaders: List<HeaderId>

    private fun log(message: String) {
        if (verbose) {
            println(message)
        }
    }

    private fun getDeclarationId(cursor: CValue<CXCursor>): DeclarationID {
        val usr = clang_getCursorUSR(cursor).convertAndDispose()
        if (usr == "") {
            val kind = cursor.kind
            val spelling = getCursorSpelling(cursor)
            return when (kind to spelling) {
                CXCursorKind.CXCursor_StructDecl to "__va_list_tag" -> DeclarationID.VaListTag
                CXCursorKind.CXCursor_StructDecl to "__va_list" -> DeclarationID.VaList
                CXCursorKind.CXCursor_TypedefDecl to "__builtin_va_list" -> DeclarationID.BuiltinVaList
                CXCursorKind.CXCursor_ObjCInterfaceDecl to "Protocol" -> DeclarationID.Protocol
                else -> error(kind to spelling)
            }
        }

        return DeclarationID.USR(usr)
    }

    private fun getStructDeclAt(
            cursor: CValue<CXCursor>
    ): StructDeclImpl = structRegistry.getOrPut(cursor, { createStructDecl(cursor) }) { decl ->
        val definitionCursor = clang_getCursorDefinition(cursor)
        if (clang_Cursor_isNull(definitionCursor) == 0) {
            assert(clang_isCursorDefinition(definitionCursor) != 0)
            createStructDef(decl, cursor)
        }
    }

    private fun createStructDecl(cursor: CValue<CXCursor>): StructDeclImpl {
        val cursorType = clang_getCursorType(cursor)
        val typeSpelling = clang_getTypeSpelling(cursorType).convertAndDispose()

        return StructDeclImpl(typeSpelling, getLocation(cursor))
    }

    private fun visitClass(cursor: CValue<CXCursor>, clazz: StructDefImpl)  {

        // TODO skip method (function) when encounter UnsupportedType in params or ret value. Otherwise all class methods will be lost due to exception (?)
        visitChildren(cursor) { cursor, _ ->
            if (cursor.isPublic) {
                // TODO If a kotlin class is _conceptually_ derived from its c++ counterpart, then it shall be able to override virtual private and access protected
                when (cursor.kind) {
                    CXCursorKind.CXCursor_CXXMethod -> {
                        val isOperatorFunction = (clang_getCursorSpelling(cursor).convertAndDispose().take(8) == "operator")
                        // operators are Not Implemented Yet
                        if (!isOperatorFunction) {
                            if (clang_isFunctionTypeVariadic(clang_getCursorType(cursor)) == 0) // FIXME why it doesn't work???
                                getFunction(cursor, clazz.decl)?.let { clazz.methods.add(it) }
                        }
                    }
                    CXCursorKind.CXCursor_Constructor ->
                        getFunction(cursor, clazz.decl)?.let { clazz.methods.add(it) }
                    CXCursorKind.CXCursor_Destructor ->
                        getFunction(cursor, clazz.decl)?.let { clazz.methods.add(it) }

                    CXCursorKind.CXCursor_VarDecl -> {
                        clazz.staticFields.add(GlobalDecl(
                                name =getCursorSpelling(cursor),
                                type = convertCursorType(cursor),
                                isConst = clang_isConstQualifiedType(clang_getCursorType(cursor)) != 0,
                                parentName = clazz.decl.spelling)
                        )
                    }

                    else -> {
                    }
                }
            }
            CXChildVisitResult.CXChildVisit_Continue
        }
    }

    private fun createStructDef(structDecl: StructDeclImpl, cursor: CValue<CXCursor>) {
        val type = clang_getCursorType(cursor)

        val fields = mutableListOf<StructMember>()
        addDeclaredFields(fields, type, type)

        val size = clang_Type_getSizeOf(type)
        val align = clang_Type_getAlignOf(type).toInt()

        val structDef = StructDefImpl(
                size, align, structDecl,
                when (cursor.kind) {
                    CXCursorKind.CXCursor_UnionDecl -> StructDef.Kind.UNION
                    CXCursorKind.CXCursor_StructDecl -> StructDef.Kind.STRUCT
                    CXCursorKind.CXCursor_ClassDecl -> StructDef.Kind.CLASS
                    else -> error(cursor.kind)
                }
        )

        structDef.members += fields
        visitClass(cursor, structDef)

        structDecl.def = structDef
    }

    private fun addDeclaredFields(result: MutableList<StructMember>, structType: CValue<CXType>, containerType: CValue<CXType>) {
        getFields(containerType).filter { it.isPublic }.forEach { fieldCursor ->
            val name = getCursorSpelling(fieldCursor)
            if (name.isNotEmpty()) {
                val fieldType = convertCursorType(fieldCursor)
                val offset = clang_Type_getOffsetOf(structType, name)
                val member = if (offset < 0) {
                    IncompleteField(name, fieldType)
                } else if (clang_Cursor_isBitField(fieldCursor) == 0) {
                    val canonicalFieldType = clang_getCanonicalType(clang_getCursorType(fieldCursor))
                    Field(
                            name,
                            fieldType,
                            offset,
                            clang_Type_getSizeOf(canonicalFieldType),
                            clang_Type_getAlignOf(canonicalFieldType)
                    )
                } else {
                    val size = clang_getFieldDeclBitWidth(fieldCursor)
                    BitField(name, fieldType, offset, size)
                }
                result.add(member)
            } else {
                // Unnamed field.
                val fieldType = clang_getCursorType(fieldCursor)
                when (fieldType.kind) {
                    CXTypeKind.CXType_Record -> {
                        // Unnamed struct fields also contribute their fields:
                        addDeclaredFields(result, structType, fieldType)
                    }
                    else -> {
                        // Nothing.
                    }
                }
            }
        }
    }

    private fun getEnumDefAt(cursor: CValue<CXCursor>): EnumDefImpl {
        if (clang_isCursorDefinition(cursor) == 0) {
            val definitionCursor = clang_getCursorDefinition(cursor)
            if (clang_isCursorDefinition(definitionCursor) != 0) {
                return getEnumDefAt(definitionCursor)
            } else {
                TODO("support enum forward declarations: " +
                        clang_getTypeSpelling(clang_getCursorType(cursor)).convertAndDispose())
            }
        }

        return enumRegistry.getOrPut(cursor) {
            val cursorType = clang_getCursorType(cursor)
            val typeSpelling = clang_getTypeSpelling(cursorType).convertAndDispose()

            val baseType = convertType(clang_getEnumDeclIntegerType(cursor))

            val enumDef = EnumDefImpl(typeSpelling, baseType, getLocation(cursor))

            visitChildren(cursor) { childCursor, _ ->
                if (clang_getCursorKind(childCursor) == CXCursorKind.CXCursor_EnumConstantDecl) {
                    val name = clang_getCursorSpelling(childCursor).convertAndDispose()
                    val value = clang_getEnumConstantDeclValue(childCursor)

                    val constant = EnumConstant(name, value, isExplicitlyDefined = childCursor.hasExpressionChild())
                    enumDef.constants.add(constant)
                }

                CXChildVisitResult.CXChildVisit_Continue
            }

            enumDef
        }
    }

    private fun getObjCCategoryClassCursor(cursor: CValue<CXCursor>): CValue<CXCursor> {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCCategoryDecl)
        var classRef: CValue<CXCursor>? = null
        visitChildren(cursor) { child, _ ->
            if (child.kind == CXCursorKind.CXCursor_ObjCClassRef) {
                classRef = child
                CXChildVisitResult.CXChildVisit_Break
            } else {
                CXChildVisitResult.CXChildVisit_Continue
            }
        }

        return clang_getCursorReferenced(classRef!!).apply {
            assert(this.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl)
        }
    }

    private fun isObjCInterfaceDeclForward(cursor: CValue<CXCursor>): Boolean {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl) { cursor.kind }

        // It is forward declaration <=> the first child is reference to it:
        var result = false
        visitChildren(cursor) { child, _ ->
            result = (child.kind == CXCursorKind.CXCursor_ObjCClassRef && clang_getCursorReferenced(child) == cursor)
            CXChildVisitResult.CXChildVisit_Break
        }
        return result
    }

    private fun getObjCClassAt(cursor: CValue<CXCursor>): ObjCClassImpl {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl) { cursor.kind }

        val name = clang_getCursorDisplayName(cursor).convertAndDispose()

        if (isObjCInterfaceDeclForward(cursor)) {
            return objCClassRegistry.getOrPut(cursor) {
                ObjCClassImpl(name, getLocation(cursor), isForwardDeclaration = true, binaryName = null)
            }
        }

        return objCClassRegistry.getOrPut(cursor, {
            ObjCClassImpl(name, getLocation(cursor), isForwardDeclaration = false,
                    binaryName = getObjCBinaryName(cursor).takeIf { it != name })
        }) {
            addChildrenToObjCContainer(cursor, it)
        }

    }

    private fun getObjCProtocolAt(cursor: CValue<CXCursor>): ObjCProtocolImpl {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCProtocolDecl) { cursor.kind }
        val name = clang_getCursorDisplayName(cursor).convertAndDispose()

        if (clang_isCursorDefinition(cursor) == 0) {
            val definition = clang_getCursorDefinition(cursor)
            return if (clang_isCursorDefinition(definition) != 0) {
                getObjCProtocolAt(definition)
            } else {
                objCProtocolRegistry.getOrPut(cursor) {
                    ObjCProtocolImpl(name, getLocation(cursor), isForwardDeclaration = true)
                }
            }
        }

        return objCProtocolRegistry.getOrPut(cursor, {
            ObjCProtocolImpl(name, getLocation(cursor), isForwardDeclaration = false)
        }) {
            addChildrenToObjCContainer(cursor, it)
        }
    }

    private fun getObjCBinaryName(cursor: CValue<CXCursor>): String {
        val prefix = "_OBJC_CLASS_\$_"
        val symbolName = clang_Cursor_getObjCManglings(cursor)!!.convertAndDispose()
                .single { it.startsWith(prefix) }

        return symbolName.substring(prefix.length)
    }

    private fun getObjCCategoryAt(cursor: CValue<CXCursor>): ObjCCategoryImpl? {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCCategoryDecl) { cursor.kind }

        val classCursor = getObjCCategoryClassCursor(cursor)
        if (!isAvailable(classCursor)) return null

        val name = clang_getCursorDisplayName(cursor).convertAndDispose()
        val declarationId = getDeclarationId(cursor)

        return objCCategoryById.getOrPut(declarationId) {
            val clazz = getObjCClassAt(classCursor)
            val category = ObjCCategoryImpl(name, clazz)
            addChildrenToObjCContainer(cursor, category)
            category
        }

    }

    private fun addChildrenToObjCContainer(cursor: CValue<CXCursor>, result: ObjCContainerImpl) {
        visitChildren(cursor) { child, _ ->
            when (child.kind) {
                CXCursorKind.CXCursor_ObjCSuperClassRef -> {
                    assert(cursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl)
                    result as ObjCClassImpl

                    assert(result.baseClass == null)
                    result.baseClass = getObjCClassAt(clang_getCursorReferenced(child))
                }
                CXCursorKind.CXCursor_ObjCProtocolRef -> {
                    val protocol = getObjCProtocolAt(clang_getCursorReferenced(child))
                    if (protocol !in result.protocols) {
                        result.protocols.add(protocol)
                    }
                }
                CXCursorKind.CXCursor_ObjCClassMethodDecl, CXCursorKind.CXCursor_ObjCInstanceMethodDecl -> {
                    getObjCMethod(child)?.let { method ->
                        result.methods.removeAll { method.replaces(it) }
                        result.methods.add(method)
                    }
                }
                else -> {
                }
            }
            CXChildVisitResult.CXChildVisit_Continue
        }
    }

    fun getTypedef(type: CValue<CXType>): Type {
        val declCursor = clang_getTypeDeclaration(type)
        val name = getCursorSpelling(declCursor)

        val underlying = convertType(clang_getTypedefDeclUnderlyingType(declCursor))

        if (underlying == UnsupportedType) return underlying

        if (clang_getCursorLexicalParent(declCursor).kind != CXCursorKind.CXCursor_TranslationUnit) {
            // Objective-C type parameters are represented as non-top-level typedefs.
            // Erase for now:
            return underlying
        }

        if (library.language == Language.OBJECTIVE_C) {
            if (name == "BOOL" || name == "Boolean") {
                assert(clang_Type_getSizeOf(type) == 1L)
                return ObjCBoolType
            }

            if (underlying is ObjCPointer && (name == "Class" || name == "id") ||
                    underlying is PointerType && name == "SEL") {

                // Ignore implicit Objective-C typedefs:
                return underlying
            }
        }



        if ((underlying is RecordType && underlying.decl.spelling.split(' ').last() == name) ||
                (underlying is EnumType && underlying.def.spelling.split(' ').last() == name)) {

            // special handling for:
            // typedef struct { ... } name;
            // typedef enum { ... } name;
            // FIXME: implement better solution
            return underlying
        }

        val typedefDef = typedefRegistry.getOrPut(declCursor) {

            TypedefDef(underlying, name, getLocation(declCursor))
        }

        return Typedef(typedefDef)
    }

    private fun convertCursorType(cursor: CValue<CXCursor>) =
            convertType(clang_getCursorType(cursor), clang_getDeclTypeAttributes(cursor))

    private inline fun objCType(supplier: () -> ObjCPointer) = when (library.language) {
        Language.C, Language.CPP   -> UnsupportedType
        Language.OBJECTIVE_C -> supplier()
    }

    // We omit `const` qualifier for IntegerType and FloatingType to make `CBridgeGen` simpler.
    // See KT-28102.
    private fun String.dropConstQualifier() =
            substringAfterLast("const ")

    private fun convertUnqualifiedPrimitiveType(type: CValue<CXType>): Type = when (type.kind) {
        CXTypeKind.CXType_Char_U, CXTypeKind.CXType_Char_S -> {
            assert(type.getSize() == 1L)
            CharType
        }

        CXTypeKind.CXType_UChar, CXTypeKind.CXType_UShort,
        CXTypeKind.CXType_UInt, CXTypeKind.CXType_ULong, CXTypeKind.CXType_ULongLong -> IntegerType(
                size = type.getSize().toInt(),
                isSigned = false,
                spelling = clang_getTypeSpelling(type).convertAndDispose().dropConstQualifier()
        )

        CXTypeKind.CXType_SChar, CXTypeKind.CXType_Short,
        CXTypeKind.CXType_Int, CXTypeKind.CXType_Long, CXTypeKind.CXType_LongLong -> IntegerType(
                size = type.getSize().toInt(),
                isSigned = true,
                spelling = clang_getTypeSpelling(type).convertAndDispose().dropConstQualifier()
        )

        CXTypeKind.CXType_Float, CXTypeKind.CXType_Double -> FloatingType(
                size = type.getSize().toInt(),
                spelling = clang_getTypeSpelling(type).convertAndDispose().dropConstQualifier()
        )

        CXType_Unexposed -> {
            // FIXME  Remove this cludge for libclang version >= 9 (CINDEX_VERSION > 55)
            if (clang_isExtVectorType(type) != 0) {
                val size = clang_Type_getSizeOf(type)
                if (size == 16L) {
                    //  ExtVector elementType and elementCount are ignored for now but stubs are still needed for
                    //  CXType_Vector compatibility. Incoming clang v9 provide CXType_ExtVector compatible with CXType_Vector
                    val spelling = "__attribute__((__vector_size__($size))) float"
                    VectorType(FloatingType(4, "float"), 4, spelling)
                } else {
                    UnsupportedType
                }
            } else {
                UnsupportedType
            }
        }

        CXType_Vector -> {
            val elementCXType = clang_getElementType(type)
            val elementType = convertType(elementCXType)
            val size = clang_Type_getSizeOf(type)
            val elemSize = clang_Type_getSizeOf(elementCXType)
            val elementCount = clang_getNumElements(type)
            assert(size >= elemSize * elementCount && size % elemSize == 0L)

            // Spelling example: `__attribute__((__vector_size__(4 * sizeof(float)))) const float`
            // Re-generate spelling removing constness and typedefs to limit number of variants for bridge generator
            // Supposed to be the same (i.e. natively compatible) as clang_getTypeSpelling(type) aka type.name
            val spelling = "__attribute__((__vector_size__($size))) ${clang_getCanonicalType(elementCXType).name}"

            if (size == 16L) {
                VectorType(elementType, elementCount.toInt(), spelling)
            } else {
                UnsupportedType
            }
        }

        CXTypeKind.CXType_Bool -> CBoolType

        else -> UnsupportedType
    }

    fun convertType(type: CValue<CXType>, typeAttributes: CValue<CXTypeAttributes>? = null): Type {
        val primitiveType = convertUnqualifiedPrimitiveType(type)
        if (primitiveType != UnsupportedType) {
            return primitiveType
        }

        val kind = type.kind

        return when (kind) {
            CXType_Elaborated -> convertType(clang_Type_getNamedType(type))

            CXType_Unexposed -> {
                if (clang_getResultType(type).kind != CXTypeKind.CXType_Invalid) {
                    convertFunctionType(type)
                } else {
                    val canonicalType = clang_getCanonicalType(type)
                    if (canonicalType.kind != CXType_Unexposed) {
                        convertType(canonicalType)
                    } else {
                        UnsupportedType
                    }
                }
            }

            CXType_Void -> VoidType

            CXType_Typedef -> {
                val declCursor = clang_getTypeDeclaration(type)
                val declSpelling = getCursorSpelling(declCursor)
                val underlying = convertType(clang_getTypedefDeclUnderlyingType(declCursor))
                when {
                    declSpelling == "instancetype" && underlying is ObjCPointer ->
                        ObjCInstanceType(getNullability(type, typeAttributes))

                    else -> getTypedef(type)
                }
            }

            CXType_Record -> RecordType(getStructDeclAt(clang_getTypeDeclaration(type)))
            CXType_Enum -> EnumType(getEnumDefAt(clang_getTypeDeclaration(type)))

            CXType_Pointer, CXType_LValueReference -> {
                val pointeeType = clang_getPointeeType(type)
                val pointeeIsConst =
                        (clang_isConstQualifiedType(clang_getCanonicalType(pointeeType)) != 0)

                val convertedPointeeType = convertType(pointeeType)
                PointerType(
                        if (convertedPointeeType == UnsupportedType) VoidType else convertedPointeeType,
                        pointeeIsConst = pointeeIsConst,
                        isLVReference = (kind == CXType_LValueReference),
                        spelling = type.name
                )
            }

            CXType_ConstantArray -> {
                val elementType = convertType(clang_getArrayElementType(type))
                val length = clang_getArraySize(type)
                ConstArrayType(elementType, length)
            }

            CXType_IncompleteArray -> {
                val elementType = convertType(clang_getArrayElementType(type))
                IncompleteArrayType(elementType)
            }

            CXType_VariableArray -> {
                val elementType = convertType(clang_getArrayElementType(type))
                VariableArrayType(elementType)
            }

            CXType_FunctionProto -> {
                convertFunctionType(type)
            }

            CXType_ObjCObjectPointer -> objCType {
                val declaration = clang_getTypeDeclaration(clang_getPointeeType(type))
                val declarationKind = declaration.kind
                val nullability = getNullability(type, typeAttributes)
                when (declarationKind) {
                    CXCursorKind.CXCursor_NoDeclFound -> ObjCIdType(nullability, getProtocols(type))

                    CXCursorKind.CXCursor_ObjCInterfaceDecl ->
                        ObjCObjectPointer(getObjCClassAt(declaration), nullability, getProtocols(type))

                    CXCursorKind.CXCursor_TypedefDecl ->
                        // typedef to Objective-C class itself, e.g. `typedef NSObject Object;`,
                        //   (as opposed to `typedef NSObject* Object;`).
                        // Note: it is not yet represented as Kotlin `typealias`.
                        ObjCObjectPointer(
                                getObjCClassAt(getTypedefUnderlyingObjCClass(declaration)),
                                nullability,
                                getProtocols(type)
                        )

                    else -> TODO("${declarationKind.toString()} ${clang_getTypeSpelling(type).convertAndDispose()}")
                }
            }

            CXType_ObjCId -> objCType { ObjCIdType(getNullability(type, typeAttributes), getProtocols(type)) }

            CXType_ObjCClass -> objCType { ObjCClassPointer(getNullability(type, typeAttributes), getProtocols(type)) }

            CXType_ObjCSel -> PointerType(VoidType)

            CXType_BlockPointer -> objCType { convertBlockPointerType(type, typeAttributes) }

            else -> UnsupportedType
        }
    }

    private tailrec fun getTypedefUnderlyingObjCClass(typedefDecl: CValue<CXCursor>): CValue<CXCursor> {
        assert(typedefDecl.kind == CXCursorKind.CXCursor_TypedefDecl)
        val underlyingType = clang_getTypedefDeclUnderlyingType(typedefDecl)
        val underlyingTypeDecl = clang_getTypeDeclaration(underlyingType)

        return when (underlyingTypeDecl.kind) {
            CXCursorKind.CXCursor_TypedefDecl -> getTypedefUnderlyingObjCClass(underlyingTypeDecl)
            CXCursorKind.CXCursor_ObjCInterfaceDecl -> underlyingTypeDecl
            else -> TODO(
                    """typedef = ${getCursorSpelling(typedefDecl)}
                        |underlying decl kind = ${underlyingTypeDecl.kind}
                        |underlying = ${clang_getTypeSpelling(underlyingType).convertAndDispose()}""".trimMargin()
            )
        }
    }

    private fun getNullability(
            type: CValue<CXType>, typeAttributes: CValue<CXTypeAttributes>?
    ): ObjCPointer.Nullability {

        if (typeAttributes == null) return ObjCPointer.Nullability.Unspecified

        return when (clang_Type_getNullabilityKind(type, typeAttributes)) {
            CXNullabilityKind.CXNullabilityKind_Nullable -> ObjCPointer.Nullability.Nullable
            CXNullabilityKind.CXNullabilityKind_NonNull -> ObjCPointer.Nullability.NonNull
            CXNullabilityKind.CXNullabilityKind_Unspecified -> ObjCPointer.Nullability.Unspecified
        }
    }

    private fun getProtocols(type: CValue<CXType>): List<ObjCProtocol> {
        val num = clang_Type_getNumProtocols(type)
        return (0 until num).map { index ->
            getObjCProtocolAt(clang_Type_getProtocol(type, index))
        }
    }

    private fun convertFunctionType(type: CValue<CXType>): Type {
        val kind = type.kind
        assert(kind == CXType_Unexposed || kind == CXType_FunctionProto || kind == CXType_FunctionNoProto) { kind }

        if (clang_isFunctionTypeVariadic(type) != 0) {
            return VoidType // make this function pointer opaque.
        } else {
            val returnType = convertType(clang_getResultType(type))
            val numArgs = clang_getNumArgTypes(type)

            // Ignore functions with long signatures since we have no basic class for such functional types in the stdlib.
            // TODO: Remove this limitation when functional types with long signatures are supported.
            if (numArgs > 22) {
                log("Warning: cannot generate a Kotlin functional type for a pointer to a function with more than 22 parameters. " +
                        "An opaque pointer will be used instead.")
                return VoidType
            }

            val paramTypes = (0..numArgs - 1).map {
                convertType(clang_getArgType(type, it))
            }

            return if (returnType == UnsupportedType || paramTypes.any { it == UnsupportedType }) {
                VoidType
            } else {
                FunctionType(paramTypes, returnType)
            }
        }
    }

    private fun convertBlockPointerType(type: CValue<CXType>, typeAttributes: CValue<CXTypeAttributes>?): ObjCPointer {
        val kind = type.kind
        assert(kind == CXType_BlockPointer)

        val pointee = clang_getPointeeType(type)
        val nullability = getNullability(type, typeAttributes)

        // TODO: also use nullability attributes of parameters and return value.

        val functionType = convertFunctionType(pointee) as? FunctionType
                ?: return ObjCIdType(nullability, protocols = emptyList())

        return ObjCBlockPointer(nullability, functionType.parameterTypes, functionType.returnType)
    }

    private val TARGET_ATTRIBUTE = "__target__"

    private fun isSuitableFunction(cursor: CValue<CXCursor>): Boolean {
        if (!isAvailable(cursor)) return false

        // If function is specific for certain target, ignore that, as we may be
        // unable to generate machine code for bridge from the bitcode.
        return !functionHasTargetAttribute(cursor)
    }

    private fun functionHasTargetAttribute(cursor: CValue<CXCursor>): Boolean {
        // TODO: this must be implemented with hasAttribute(), but hasAttribute()
        // works for Mac hosts only so far.

        var result = false
        visitChildren(cursor) { child, _ ->
            if (isTargetAttribute(child)) {
                result = true
                CXChildVisitResult.CXChildVisit_Break
            } else {
                CXChildVisitResult.CXChildVisit_Continue
            }
        }
        return result
    }

    private fun isTargetAttribute(cursor: CValue<CXCursor>): Boolean = clang_isAttribute(cursor.kind) != 0 &&
            getExtentFirstToken(cursor) == TARGET_ATTRIBUTE

    private fun getExtentFirstToken(cursor: CValue<CXCursor>) =
            getToken(clang_Cursor_getTranslationUnit(cursor)!!, clang_getRangeStart(clang_getCursorExtent(cursor)))

    // TODO: implement with clang_getToken after updating libclang.
    private fun getToken(translationUnit: CXTranslationUnit, location: CValue<CXSourceLocation>): String? = memScoped {
        val range = clang_getRange(location, location) // Seems to work.

        val tokensVar = alloc<CPointerVar<CXToken>>()
        val numTokensVar = alloc<IntVar>()
        clang_tokenize(translationUnit, range, tokensVar.ptr, numTokensVar.ptr)
        val numTokens = numTokensVar.value
        val tokens = tokensVar.value ?: return null

        try {
            when (numTokens) {
                0 -> null
                1 -> clang_getTokenSpelling(translationUnit, tokens[0].readValue()).convertAndDispose()
                else -> error("Unexpected number of tokens: $numTokens")
            }
        } finally {
            clang_disposeTokens(translationUnit, tokens, numTokens)
        }
    }

    fun indexDeclaration(info: CXIdxDeclInfo): Unit {
        val cursor = info.cursor.readValue()
        val entityInfo = info.entityInfo!!.pointed
        val entityName = entityInfo.name?.toKString()
        val kind = entityInfo.kind

        if (!this.library.includesDeclaration(cursor)) {
            return
        }

        val namespace: Namespace? =
        // semantic parent of any namespace member is always namespace itself (no type aliases etc)
            if (info.semanticContainer!!.pointed.cursor.kind == CXCursorKind.CXCursor_Namespace) {
                val parent = info.semanticContainer!!.pointed.cursor.readValue()
                Namespace(getCursorSpelling(parent), getParentName(parent))
            } else null

        if (!cursor.isRecursivelyPublic()) {
            // c++ : skip anon namespaces, static functions and variables and private inner classes
            return
        }
        /**
         * TODO It may be better to look at CXTypeKind instead of CXIdxEntity to distinguish C++ classes from templates
         * C++ templates are also CXIdxEntity_CXXClass but CXCursor_ClassTemplate,
         * while C++ class is CXCursor_ClassDecl
         * The same for CXCursor_FunctionDecl vs CXCursor_FunctionTemplate
         */
        when (kind) {
            CXIdxEntity_Struct, CXIdxEntity_Union, CXIdxEntity_CXXClass -> {
                if (entityName == null) {
                    // Skip anonymous struct.
                    // (It gets included anyway if used as a named field type).
                } else {
                    if (library.language != Language.CPP) {
                        getStructDeclAt(cursor)
                    }
                }
            }

            CXIdxEntity_Typedef, CXIdxEntity_CXXTypeAlias -> {
                val type = clang_getCursorType(cursor)
                getTypedef(type)
            }

            CXIdxEntity_Function -> {
                if (isSuitableFunction(cursor)
                        && library.language != Language.CPP) {
                    functionById.getOrPut(getDeclarationId(cursor)) {
                        getFunction(cursor)
                    }
                }
            }

            CXIdxEntity_Enum -> {
                getEnumDefAt(cursor)
            }

            CXIdxEntity_Variable -> {
                val parentKind = info.semanticContainer!!.pointed.cursor.kind
                if (parentKind == CXCursorKind.CXCursor_TranslationUnit || parentKind == CXCursorKind.CXCursor_Namespace) {
                    // Top-level or namespace member. Skip class static members - they are loaded by visitClass
                    globalById.getOrPut(getDeclarationId(cursor)) {
                        GlobalDecl(
                                name = entityName!!,
                                type = convertCursorType(cursor),
                                isConst = clang_isConstQualifiedType(clang_getCursorType(cursor)) != 0,
                                parentName = getParentName(cursor)
                        )
                    }
                }
            }

            CXIdxEntity_ObjCClass -> if (cursor.kind != CXCursorKind.CXCursor_ObjCClassRef /* not a forward declaration */) {
                indexObjCClass(cursor)
            }

            CXIdxEntity_ObjCCategory -> {
                if (isAvailable(cursor)) {
                    getObjCCategoryAt(cursor)
                }
            }

            CXIdxEntity_ObjCProtocol -> if (cursor.kind != CXCursorKind.CXCursor_ObjCProtocolRef /* not a forward declaration */) {
                indexObjCProtocol(cursor)
            }

            CXIdxEntity_ObjCProperty -> {
                val container = clang_getCursorSemanticParent(cursor)
                if (isAvailable(cursor) && isAvailable(container)) {
                    val propertyInfo = clang_index_getObjCPropertyDeclInfo(info.ptr)!!.pointed
                    val getter = getObjCMethod(propertyInfo.getter!!.pointed.cursor.readValue())
                    val setter = propertyInfo.setter?.let {
                        getObjCMethod(it.pointed.cursor.readValue())
                    }

                    if (getter != null) {
                        val property = ObjCProperty(entityName!!, getter, setter)
                        val objCContainer: ObjCContainerImpl? = when (container.kind) {
                            CXCursorKind.CXCursor_ObjCCategoryDecl -> getObjCCategoryAt(container)
                            CXCursorKind.CXCursor_ObjCInterfaceDecl -> getObjCClassAt(container)
                            CXCursorKind.CXCursor_ObjCProtocolDecl -> getObjCProtocolAt(container)
                            else -> error(container.kind)
                        }

                        if (objCContainer != null) {
                            objCContainer.properties.removeAll { property.replaces(it) }
                            objCContainer.properties.add(property)
                        }
                    }
                }
            }

            else -> {
                // Ignore declaration.
            }
        }
    }

    fun indexDeclaration(cursor: CValue<CXCursor>): Unit {
        if (!library.includesDeclaration(cursor)) {
            return
        }

        if (cursor.isRecursivelyPublic()) {
            when (cursor.kind) {

                CXCursorKind.CXCursor_ClassDecl, CXCursorKind.CXCursor_StructDecl, CXCursorKind.CXCursor_UnionDecl -> {
                    if (library.language == Language.CPP) {
                        if (cursor.spelling.isEmpty()) {
                            // Skip anonymous struct.
                            // (It gets included anyway if used as a named field type).
                        } else {
                            getStructDeclAt(cursor)
                        }
                    }
                }

                CXCursorKind.CXCursor_FunctionDecl -> {
                    if (library.language == Language.CPP) {
                        indexCxxFunction(cursor)
                    }
                }

                else -> {
                }
            }
        }
    }

    private fun indexCxxFunction(cursor: CValue<CXCursor>) {
        if (isSuitableFunction(cursor)) {
            if (getCursorSpelling(cursor).take(8) == "operator") {
                // not implemented yet
            } else {
                functionById.getOrPut(getDeclarationId(cursor)) {
                    getFunction(cursor)
                }
            }
        }
    }

    fun indexObjCClass(cursor: CValue<CXCursor>) {
        if (isAvailable(cursor)) {
            getObjCClassAt(cursor)
        }
    }

    fun indexObjCProtocol(cursor: CValue<CXCursor>) {
        if (isAvailable(cursor)) {
            getObjCProtocolAt(cursor)
        }
    }

    private fun getFunction(cursor: CValue<CXCursor>, receiver: StructDecl? = null): FunctionDecl? {
        if (!isFuncDeclEligible(cursor)) {
            log("Skip function ${clang_getCursorSpelling(cursor).convertAndDispose()}")
            return null
        }
        var name = clang_getCursorSpelling(cursor).convertAndDispose()
        var returnType = convertType(clang_getCursorResultType(cursor), clang_getCursorResultTypeAttributes(cursor))

        val parameters = mutableListOf<Parameter>()
        parameters += getFunctionParameters(cursor)

        val binaryName = when (library.language) {
            Language.C, Language.CPP, Language.OBJECTIVE_C -> clang_Cursor_getMangling(cursor).convertAndDispose()
        }

        val definitionCursor = clang_getCursorDefinition(cursor)
        val isDefined = (clang_Cursor_isNull(definitionCursor) == 0)

        val isVararg = clang_Cursor_isVariadic(cursor) != 0

        // TODO Do the following if clang_getCursorLanguage(cursor) == CXLanguageKind.CXLanguage_CPlusPlus ...
        val parentName = getParentName(cursor)
        val cxxMethodInfo = receiver?.let { CxxMethodInfo(
                PointerType(RecordType(receiver),
                        clang_CXXMethod_isConst(cursor) != 0), // CXCursor_ConversionFunction has constness too
                when (cursor.kind) {
                    CXCursorKind.CXCursor_Constructor -> {
                        returnType = PointerType(RecordType(receiver))
                        name = "__init__" // It is intended to init preallocated memory with placement new, so it is not "create" factory method. TODO One may want "create" method also.
                        // Parameter type for placement new is void*, but I want to emphasize that memory block ahall have proper size and alignment
                        parameters.add(0, Parameter("self", PointerType(RecordType(receiver)), false))
                        CxxMethodKind.Constructor
                    }
                    CXCursorKind.CXCursor_Destructor -> {
                        name = "__destroy__"
                        parameters.add(0, Parameter("self", PointerType(RecordType(receiver)), false))
                        CxxMethodKind.Destructor
                    }
                    // CXCursorKind.CXCursor_ConversionFunction -> ...
                    CXCursorKind.CXCursor_CXXMethod ->
                        if (clang_CXXMethod_isStatic(cursor) != 0) {
                            CxxMethodKind.StaticMethod
                        } else {
                            parameters.add(0, Parameter("self",
                                    PointerType(RecordType(receiver), clang_CXXMethod_isConst(cursor) != 0),
                                    false))
                            CxxMethodKind.InstanceMethod
                        }
                    else -> CxxMethodKind.None // Not implemented. Not expected, OK to assert (?)
                }
        )
        }

        return FunctionDecl(name, parameters, returnType, binaryName, isDefined, isVararg, parentName, cxxMethodInfo)
    }

    private fun getObjCMethod(cursor: CValue<CXCursor>): ObjCMethod? {
        if (!isAvailable(cursor)) {
            return null
        }

        val selector = clang_getCursorDisplayName(cursor).convertAndDispose()

        // Ignore some very special methods:
        when (selector) {
            "dealloc", "retain", "release", "autorelease", "retainCount", "self" -> return null
        }

        val encoding = clang_getDeclObjCTypeEncoding(cursor).convertAndDispose()
        val returnType = convertType(clang_getCursorResultType(cursor), clang_getCursorResultTypeAttributes(cursor))
        val parameters = getFunctionParameters(cursor)

        if (returnType == UnsupportedType || parameters.any { it.type == UnsupportedType }) {
            return null // TODO: make a more universal fix.
        }

        val isClass = when (cursor.kind) {
            CXCursorKind.CXCursor_ObjCClassMethodDecl -> true
            CXCursorKind.CXCursor_ObjCInstanceMethodDecl -> false
            else -> error(cursor.kind)
        }

        return ObjCMethod(
                selector, encoding, parameters, returnType,
                isVariadic = clang_Cursor_isVariadic(cursor) != 0,
                isClass = isClass,
                nsConsumesSelf = clang_Cursor_isObjCConsumingSelfMethod(cursor) != 0,
                nsReturnsRetained = clang_Cursor_isObjCReturningRetainedMethod(cursor) != 0,
                isOptional = (clang_Cursor_isObjCOptional(cursor) != 0),
                isInit = (clang_Cursor_isObjCInitMethod(cursor) != 0),
                isExplicitlyDesignatedInitializer = hasAttribute(cursor, OBJC_DESGINATED_INITIALIZER)
        )
    }

    // TODO: unavailable declarations should be imported as deprecated.
    private fun isAvailable(cursor: CValue<CXCursor>): Boolean = when (clang_getCursorAvailability(cursor)) {
        CXAvailabilityKind.CXAvailability_Available,
        CXAvailabilityKind.CXAvailability_Deprecated -> true

        CXAvailabilityKind.CXAvailability_NotAvailable,
        CXAvailabilityKind.CXAvailability_NotAccessible -> false
    }

    // Skip functions which parameter or return type is TemplateRef
    private fun isFuncDeclEligible(cursor: CValue<CXCursor>): Boolean {

        var ret = true
        visitChildren(cursor) { cursor, _ ->
            when (cursor.kind) {
                CXCursorKind.CXCursor_TemplateRef -> {
                    ret = false
                    CXChildVisitResult.CXChildVisit_Break
                }
                else -> CXChildVisitResult.CXChildVisit_Recurse
            }
        }
        return ret
    }

    private fun getFunctionParameters(cursor: CValue<CXCursor>): List<Parameter> {
        val argNum = clang_Cursor_getNumArguments(cursor)
        val args = (0..argNum - 1).map {
            val argCursor = clang_Cursor_getArgument(cursor, it)
            val argName = getCursorSpelling(argCursor)
            val type = convertCursorType(argCursor)
            Parameter(argName, type,
                    nsConsumed = hasAttribute(argCursor, NS_CONSUMED))
        }
        return args
    }

    private val NS_CONSUMED = "ns_consumed"
    private val OBJC_DESGINATED_INITIALIZER = "objc_designated_initializer"

    private fun hasAttribute(cursor: CValue<CXCursor>, name: String): Boolean {
        var result = false
        visitChildren(cursor) { child, _ ->
            if (clang_isAttribute(child.kind) != 0 && clang_Cursor_getAttributeSpelling(child)?.toKString() == name) {
                result = true
                CXChildVisitResult.CXChildVisit_Break
            } else {
                CXChildVisitResult.CXChildVisit_Continue
            }
        }
        return result
    }

}

fun buildNativeIndexImpl(library: NativeLibrary, verbose: Boolean): IndexerResult {
    val result = NativeIndexImpl(library, verbose)
    val compilation = indexDeclarations(result)
    return IndexerResult(result, compilation)
}

private fun indexDeclarations(nativeIndex: NativeIndexImpl): CompilationWithPCH {
    withIndex { index ->
        val translationUnit = nativeIndex.library.copyWithArgsForPCH().parse(
                index,
                options = CXTranslationUnit_DetailedPreprocessingRecord or CXTranslationUnit_ForSerialization
        )
        try {
            translationUnit.ensureNoCompileErrors()

            val compilation = nativeIndex.library.withPrecompiledHeader(translationUnit)

            val headers = getFilteredHeaders(nativeIndex, index, translationUnit)

            nativeIndex.includedHeaders = headers.map {
                nativeIndex.getHeaderId(it)
            }

            indexTranslationUnit(index, translationUnit, 0, object : Indexer {
                override fun indexDeclaration(info: CXIdxDeclInfo) {
                    val file = memScoped {
                        val fileVar = alloc<CXFileVar>()
                        clang_indexLoc_getFileLocation(info.loc.readValue(), null, fileVar.ptr, null, null, null)
                        fileVar.value
                    }

                    if (file in headers) {
                        nativeIndex.indexDeclaration(info)
                    }
                }
            })

            visitChildren(clang_getTranslationUnitCursor(translationUnit)) { cursor, _ ->
                if (getContainingFile(cursor) in headers) {
                    nativeIndex.indexDeclaration(cursor)
                }
                CXChildVisitResult.CXChildVisit_Recurse
            }

            visitChildren(clang_getTranslationUnitCursor(translationUnit)) { cursor, _ ->
                val file = getContainingFile(cursor)
                if (file in headers && nativeIndex.library.includesDeclaration(cursor)) {
                    when (cursor.kind) {
                        CXCursorKind.CXCursor_ObjCInterfaceDecl -> nativeIndex.indexObjCClass(cursor)
                        CXCursorKind.CXCursor_ObjCProtocolDecl -> nativeIndex.indexObjCProtocol(cursor)
                        else -> {}
                    }
                }
                CXChildVisitResult.CXChildVisit_Continue
            }

            findMacros(nativeIndex, compilation, translationUnit, headers)

            return compilation
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    }
}
