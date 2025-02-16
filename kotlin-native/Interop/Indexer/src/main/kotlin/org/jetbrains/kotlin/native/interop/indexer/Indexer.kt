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

private class StructDeclImpl(
        spelling: String,
        override val isAnonymous: Boolean,
        override val location: Location
) : StructDecl(spelling) {
    override var def: StructDefImpl? = null
}

private class StructDefImpl(
    size: Long,
    align: Int,
    override val kind: Kind,
    override val members: List<StructMember>,
    override val methods: List<FunctionDecl>,
    override val staticFields: List<GlobalDecl>
) : StructDef(size, align)

private class EnumDefImpl(
        spelling: String,
        type: Type,
        override val isAnonymous: Boolean,
        override val location: Location
) : EnumDef(spelling, type) {
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
    override val includedCategories = mutableListOf<ObjCCategory>()
}

private class ObjCCategoryImpl(
        name: String, clazz: ObjCClass,
        override val location: Location
) : ObjCCategory(name, clazz), ObjCContainerImpl {
    override val protocols = mutableListOf<ObjCProtocol>()
    override val methods = mutableListOf<ObjCMethod>()
    override val properties = mutableListOf<ObjCProperty>()
}

public open class NativeIndexImpl(val library: NativeLibrary, val verbose: Boolean = false) : NativeIndex() {

    private sealed class DeclarationID {
        data class USR(val usr: String) : DeclarationID()
        object VaList : DeclarationID()
        object VaListTag : DeclarationID()
        object BuiltinVaList : DeclarationID()
        object Protocol : DeclarationID()
    }

    private open inner class LocatableDeclarationRegistry<D : LocatableDeclaration> {
        private val all = mutableMapOf<DeclarationID, D>()

        val included = mutableListOf<D>()

        protected open fun shouldBeIncluded(declaration: D, headerId: HeaderId): Boolean =
                !library.headerExclusionPolicy.excludeAll(headerId)

        inline fun getOrPut(cursor: CValue<CXCursor>, create: () -> D) = getOrPut(cursor, create, configure = {})

        inline fun getOrPut(cursor: CValue<CXCursor>, create: () -> D, configure: (D) -> Unit): D {
            val key = getDeclarationId(cursor)
            return all.getOrElse(key) {

                val value = create()
                all[key] = value

                val headerId = getHeaderId(getContainingFile(cursor))
                if (shouldBeIncluded(value, headerId)) {
                    // This declaration is used, and thus should be included:
                    included.add(value)
                }

                configure(value)
                value
            }
        }

    }

    private inner class ObjCClassOrProtocolRegistry<D : ObjCClassOrProtocol> : LocatableDeclarationRegistry<D>() {
        override fun shouldBeIncluded(declaration: D, headerId: HeaderId): Boolean {
            if (!declaration.isForwardDeclaration) {
                return super.shouldBeIncluded(declaration, headerId)
            }

            /*
            Objective-C forward declarations tend to be tricky.
            TL;DR: treating them as always included workarounds a few quirks in libclang and cinterop "separate compilation"
            (e.g. `HeaderExclusionPolicy` and `Imports`).
            On the other hand, this is absolutely safe, because forward declarations can be safely redeclared
            in different cinterop klibs without side effects.
            See detailed explanation below.
            */
            return true

            /*
            There are a few problems with Objective-C forward declarations.

            1.
            libclang indexer ignores forward declarations in system headers, and there is no way to configure that.
            Technically, it is supposed to ignore only references:
            https://github.com/Kotlin/llvm-project/blob/e3ca3c13204b52534ee555031b099657f253b1bb/clang/lib/Index/IndexingContext.cpp#L422
            But Objective-C forward declarations are treated as references:
            https://github.com/Kotlin/llvm-project/blob/75e16fd2c656fb7d27e6edc46dc1a63ff8323999/clang/lib/Index/IndexDecl.cpp#L432
            The behavior can be changed using C++ API, but this is not available through libclang C API:
            https://github.com/Kotlin/llvm-project/blob/d8d780bff4b23834a9ee8981f6a1193d181891aa/clang/include/clang/Index/IndexingOptions.h#L28
            https://github.com/Kotlin/llvm-project/blob/1b5110b59f85e7c9824bcc299b002ec933758932/clang/tools/libclang/Indexing.cpp#L424

            2.
            There are forward declarations that are provided by "builtins" (i.e. when containing `CXFile` is `null`).
            For example, `@class Protocol`.
            But builtins aren't indexed like regular headers -- indexing just doesn't traverse such declarations.
            So, even though a forward declaration is "declared" in builtins, it doesn't get added to the native index
            of a library that "includes" builtins (unless there are references from regular headers).
            Moreover, within platform libraries, "builtins" are included in `platform.posix`, which is configured as
            a C library. So when indexing it, libclang simply won't provide an Objective-C forward declaration in any case.


            `super.shouldBeIncluded` assumes that if the declaration is reported as declared in an imported header,
            then it should have been indexed as a part of that native index. But with both cases, this doesn't happen,
            so such forward declarations aren't added to any index at all.
            As a result, cinterop produces klibs that refer to this forward declaration, but don't declare it
            (i.e., don't list them in `includedForwardDeclarations` manifest property).
            The compiler then just has an unresolved reference. That's what happened in https://youtrack.jetbrains.com/issue/KT-71435
            (case 2, to be specific).

            Instead, we can just treat all Obj-C forward declarations as always included. This is pretty safe:
            forward declarations are simply listed in cinterop klib manifests, instructing the compiler to synthesize
            them when requested. Having a forward declaration listed in two klibs instead of one has no side effect at all.

            The only side effect of this hack is: if the declaration is referenced but never declared, synthesize the declaration
            anyway. Which is exactly the problematic case.
            */
        }
    }

    internal fun getHeaderId(file: CXFile?): HeaderId = getHeaderId(this.library, file)

    private fun getLocation(cursor: CValue<CXCursor>): Location {
        val headerId = getHeaderId(getContainingFile(cursor))
        return Location(headerId)
    }

    override val structs: List<StructDecl> get() = structRegistry.included
    private val structRegistry = LocatableDeclarationRegistry<StructDeclImpl>()

    override val enums: List<EnumDef> get() = enumRegistry.included
    private val enumRegistry = LocatableDeclarationRegistry<EnumDefImpl>()

    override val objCClasses: List<ObjCClass> get() = objCClassRegistry.included
    private val objCClassRegistry = ObjCClassOrProtocolRegistry<ObjCClassImpl>()

    override val objCProtocols: List<ObjCProtocol> get() = objCProtocolRegistry.included
    private val objCProtocolRegistry = ObjCClassOrProtocolRegistry<ObjCProtocolImpl>()

    override val objCCategories: Collection<ObjCCategory> get() = objCCategoryById.included
    private val objCCategoryById = LocatableDeclarationRegistry<ObjCCategoryImpl>()

    override val typedefs get() = typedefRegistry.included
    private val typedefRegistry = LocatableDeclarationRegistry<TypedefDef>()


    private val functionById = mutableMapOf<DeclarationID, FunctionDecl?>()

    override val functions: Collection<FunctionDecl>
        get() = functionById.values.filterNotNull()

    override val macroConstants = mutableListOf<ConstantDef>()
    override val wrappedMacros = mutableListOf<WrappedMacroDef>()

    private val globalById = mutableMapOf<DeclarationID, GlobalDecl>()

    override val globals: Collection<GlobalDecl>
        get() = globalById.values

    override lateinit var includedHeaders: List<HeaderId>

    internal fun log(message: String) {
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

    protected fun getStructDeclAt(
            cursor: CValue<CXCursor>
    ): StructDecl = structRegistry.getOrPut(cursor, { createStructDecl(cursor) }) { decl ->
        val definitionCursor = clang_getCursorDefinition(cursor)
        if (clang_Cursor_isNull(definitionCursor) == 0) {
            decl.def = createStructDef(definitionCursor, definitionCursor.type)
        }
    }


    private fun createStructDecl(cursor: CValue<CXCursor>): StructDeclImpl =
            StructDeclImpl(
                    cursor.type.name,
                    isAnonymous = clang_Cursor_isAnonymous(cursor) != 0,
                    getLocation(cursor)
            )

    private fun createStructDef(cursor: CValue<CXCursor>, structType: CValue<CXType>): StructDefImpl {
        assert(clang_isCursorDefinition(cursor) != 0)
        val type = clang_getCursorType(cursor)
        val size = clang_Type_getSizeOf(type)
        val align = clang_Type_getAlignOf(type).toInt()
        val members = getMembers(cursor, structType)
        return StructDefImpl(
                size, align,
                when (cursor.kind) {
                    CXCursorKind.CXCursor_UnionDecl -> StructDef.Kind.UNION
                    CXCursorKind.CXCursor_StructDecl -> StructDef.Kind.STRUCT
                    CXCursorKind.CXCursor_ClassDecl -> StructDef.Kind.CLASS
                    else -> error(cursor.kind)
                },
                members,
                emptyList(),
                emptyList()
        )
    }

    // cursor may be at the root struct or at a inner anonymous struct or union,
    // while structType is always the nearest named enclosing struct/union (i.e. root struct)
    // All offsets are calculated relative to this named parent
    private fun getMembers(cursor: CValue<CXCursor>, structType: CValue<CXType>): List<StructMember> =
            getFields(cursor.type).map { fieldCursor ->
                /*
                 * We want to identify anonymous struct/union member, according with definition (ISO/IEC 9899):
                 *  "An unnamed member whose type specifier is a structure specifier with no tag is called an anonymous structure"
                 * `clang_Cursor_isAnonymous` intended to identify such entity and distinguish with cases alike:
                 *     struct {
                 *         struct {int x; } f;  // named member of anonymous type "struct with no tag"
                 *         int : 16;            // anonymous bitfield
                 *         typedef struct { int z; } foo;  // c++ only; anon struct but the struct tag is implicitly assigned by the compiler; clang_getTypeSpelling still empty
                 *         struct { int a; };   // this is the only one that we are looking for, i.e. anonymous struct member
                 *     }
                 *   `clang_Cursor_isAnonymous` implementation has been changed since LLVM 8 so we have to additionally check type.kind == CXType_Record
                 *   Starting from LLVM 9 a new function `clang_Cursor_isAnonymousRecordDecl` provided specifically for that.
                 *   Also, both `clang_Cursor_isAnonymous` and `clang_Cursor_isAnonymousRecordDecl` expect StructDecl cursor but we got
                 *   FieldDecl cursor now, so we have to convert cursor to type declaration cursor first (e.g. StructDecl))
                 */
                val declCursor = clang_getTypeDeclaration(fieldCursor.type)

                // Behavior of clang_Cursor_isAnonymous is changing starting from LLVM 8.
                // Use lately introduced clang_Cursor_isAnonymousRecordDecl when available (LLVM 9)
                val isAnonymousRecordType = (fieldCursor.type.kind == CXType_Record) && (clang_Cursor_isAnonymous(declCursor) == 1)
                when {
                    isAnonymousRecordType -> {
                        // TODO: clang_Cursor_getOffsetOfField is OK for anonymous, but only for the 1st level of such nesting
                        AnonymousInnerRecord(
                                createStructDef(clang_getCursorDefinition(declCursor), structType))
                    }
                    else -> {
                        val name = getCursorSpelling(fieldCursor)
                        val fieldType = convertCursorType(fieldCursor)
                        val offset = clang_Type_getOffsetOf(structType, name)
                        if (offset < 0) {
                            IncompleteField(name)
                        } else if (clang_Cursor_isBitField(fieldCursor) == 0) {
                            val canonicalFieldType = clang_getCanonicalType(fieldCursor.type)
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
                    }
                }
            }

    private fun getEnumDefAt(cursor: CValue<CXCursor>): EnumDefImpl {
        if (clang_isCursorDefinition(cursor) == 0) {
            val definitionCursor = clang_getCursorDefinition(cursor)
            if (clang_isCursorDefinition(definitionCursor) != 0) {
                return getEnumDefAt(definitionCursor)
            } else {
                // FIXME("enum declaration without constants might be not a typedef, but a forward declaration instead")
                return enumRegistry.getOrPut(cursor) { createEnumDefImpl(cursor) }
            }
        }

        return enumRegistry.getOrPut(cursor) {
            val enumDef = createEnumDefImpl(cursor)

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

    private fun createEnumDefImpl(cursor: CValue<CXCursor>): EnumDefImpl {
        val cursorType = clang_getCursorType(cursor)
        val typeSpelling = clang_getTypeSpelling(cursorType).convertAndDispose()
        val baseType = convertType(clang_getEnumDeclIntegerType(cursor))
        return EnumDefImpl(
                typeSpelling,
                baseType,
                isAnonymous = clang_Cursor_isAnonymous(cursor) != 0,
                getLocation(cursor)
        )
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
        }) { objcClass ->
            addChildrenToObjCContainer(cursor, objcClass)
            if (name in this.library.objCClassesIncludingCategories) {
                // We don't include methods from categories to class during indexing
                // because indexing does not care about how class is represented in Kotlin.
                // Instead, it should be done during StubIR construction.
                objcClass.includedCategories += collectClassCategories(cursor, name).mapNotNull { getObjCCategoryAt(it) }
            }
        }
    }

    /**
     * Find all categories for a class that is pointed by [classCursor] in the same file.
     * NB: Current implementation is rather slow as it walks the whole translation unit.
     */
    private fun collectClassCategories(classCursor: CValue<CXCursor>, className: String): List<CValue<CXCursor>> {
        assert(classCursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl) { classCursor.kind }
        val classFile = getContainingFile(classCursor)
        val result = mutableListOf<CValue<CXCursor>>()
        // Accessing the whole translation unit (TU) is overkill, but it is the simplest solution which is doable
        // since we use this function for a narrow set of cases.
        // Possible improvements:
        // 1. Find/create a function that returns a file scope. `clang_findReferencesInFile` does not seem to work because for categories
        // it returns `CXCursor_ObjCClassRef` (@interface >CLASS_REFERENCE<(CategoryName)) and there is no easy way to access category from
        // there.
        // 2. Extract categories collection into a separate TU pass and create Class -> [Category] mapping. This way we can avoid visiting
        // TU for every class.
        val translationUnit = clang_getCursorLexicalParent(classCursor)
        visitChildren(translationUnit) { childCursor, _ ->
            if (childCursor.kind == CXCursorKind.CXCursor_ObjCCategoryDecl) {
                val categoryClassCursor = getObjCCategoryClassCursor(childCursor)
                val categoryClassName = clang_getCursorDisplayName(categoryClassCursor).convertAndDispose()
                if (className == categoryClassName) {
                    val categoryFile = getContainingFile(childCursor)
                    val isCategoryInTheSameFileAsClass = clang_File_isEqual(categoryFile, classFile) != 0
                    val isCategoryFromDefFile = library.allowIncludingObjCCategoriesFromDefFile
                            && clang_Location_isFromMainFile(clang_getCursorLocation(childCursor)) != 0
                    if (isCategoryInTheSameFileAsClass || isCategoryFromDefFile) {
                        result += childCursor
                    }
                }
            }
            CXChildVisitResult.CXChildVisit_Continue
        }
        return result
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

        return objCCategoryById.getOrPut(cursor) {
            val clazz = getObjCClassAt(classCursor)
            val category = ObjCCategoryImpl(name, clazz, getLocation(cursor))
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

        CXType_Vector, CXType_ExtVector -> {
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

    open fun convertType(type: CValue<CXType>, typeAttributes: CValue<CXTypeAttributes>? = null): Type {
        val primitiveType = convertUnqualifiedPrimitiveType(type)
        if (primitiveType != UnsupportedType) {
            return primitiveType
        }

        val kind = type.kind

        return when (kind) {
            CXType_Elaborated -> {
                convertType(clang_Type_getNamedType(type))
            }

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
                val pointeeType = clang_getPointeeType(type)
                val declaration = clang_getTypeDeclaration(pointeeType)
                val declarationKind = declaration.kind
                val nullability = getNullability(type, typeAttributes)
                when (declarationKind) {
                    CXCursorKind.CXCursor_NoDeclFound -> ObjCIdType(nullability, getProtocols(pointeeType))

                    CXCursorKind.CXCursor_ObjCInterfaceDecl ->
                        ObjCObjectPointer(getObjCClassAt(declaration), nullability, getProtocols(pointeeType))

                    CXCursorKind.CXCursor_TypedefDecl -> {
                        // typedef to Objective-C class itself, e.g. `typedef NSObject Object;`,
                        //   (as opposed to `typedef NSObject* Object;`).
                        // Note: it is not yet represented as Kotlin `typealias`.
                        val objectType = getTypedefUnderlyingObjCObjectType(declaration)

                        ObjCObjectPointer(
                                getObjCClassAt(clang_getTypeDeclaration(objectType)),
                                nullability,
                                getProtocols(objectType)
                        )
                    }

                    else -> TODO("${declarationKind.toString()} ${clang_getTypeSpelling(type).convertAndDispose()}")
                }
            }

            CXType_ObjCId -> objCType {
                ObjCIdType(
                        getNullability(type, typeAttributes),
                        protocols = emptyList() // `CXType_ObjCId` means `id` without any protocols.
                )
            }

            CXType_ObjCClass -> objCType {
                ObjCClassPointer(
                        getNullability(type, typeAttributes),
                        protocols = emptyList() // `CXType_ObjCClass` means `Class` without any protocols.
                )
            }

            CXType_ObjCSel -> PointerType(VoidType)

            CXType_BlockPointer -> objCType { convertBlockPointerType(type, typeAttributes) }

            else -> UnsupportedType
        }
    }

    private tailrec fun getTypedefUnderlyingObjCObjectType(typedefDecl: CValue<CXCursor>): CValue<CXType> {
        assert(typedefDecl.kind == CXCursorKind.CXCursor_TypedefDecl)
        val underlyingType = clang_getTypedefDeclUnderlyingType(typedefDecl)
        val underlyingTypeDecl = clang_getTypeDeclaration(underlyingType)

        return when (underlyingTypeDecl.kind) {
            CXCursorKind.CXCursor_TypedefDecl -> getTypedefUnderlyingObjCObjectType(underlyingTypeDecl)
            CXCursorKind.CXCursor_ObjCInterfaceDecl -> underlyingType
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

    private fun getProtocols(objectType: CValue<CXType>): List<ObjCProtocol> {
        val num = clang_Type_getNumObjCProtocolRefs(objectType)
        return (0..<num).map { index ->
            getObjCProtocolAt(clang_Type_getObjCProtocolDecl(objectType, index))
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

    private val TARGET_ATTRIBUTE_NAMES = setOf("__target__", "target")

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
            getExtentFirstToken(cursor) in TARGET_ATTRIBUTE_NAMES

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

        when (kind) {
            CXIdxEntity_Struct, CXIdxEntity_Union -> {
                if (entityName == null) {
                    // Skip anonymous struct.
                    // (It gets included anyway if used as a named field type).
                } else {
                    getStructDeclAt(cursor)
                }
            }

            CXIdxEntity_Typedef -> {
                val type = clang_getCursorType(cursor)
                getTypedef(type)
            }

            CXIdxEntity_Function -> {
                if (isSuitableFunction(cursor)) {
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
                                parentName = null
                        )
                    }
                }
            }

            CXIdxEntity_ObjCClass -> if (cursor.kind != CXCursorKind.CXCursor_ObjCClassRef /* not a forward declaration */) {
                indexObjCClass(cursor)
            } else {
                // It is a class reference. To get the declaration cursor, we can use clang_getCursorReferenced.
                // If there is a real declaration besides this forward declaration, the function will automatically
                // resolve it.
                indexObjCClass(clang_getCursorReferenced(cursor))
            }

            CXIdxEntity_ObjCCategory -> {
                if (isAvailable(cursor)) {
                    getObjCCategoryAt(cursor)
                }
            }

            CXIdxEntity_ObjCProtocol -> if (cursor.kind != CXCursorKind.CXCursor_ObjCProtocolRef /* not a forward declaration */) {
                indexObjCProtocol(cursor)
            } else {
                // It is a protocol reference. To get the declaration cursor, we can use clang_getCursorReferenced.
                // If there is a real declaration besides this forward declaration, the function will automatically
                // resolve it.
                indexObjCProtocol(clang_getCursorReferenced(cursor))
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

    fun indexObjCCategory(cursor: CValue<CXCursor>) {
        if (isAvailable(cursor)) {
            getObjCCategoryAt(cursor)
        }
    }

    protected open fun String.isUnknownTemplate() = false

    private fun getFunction(cursor: CValue<CXCursor>): FunctionDecl? {
        if (!isFuncDeclEligible(cursor)) {
            log("Skip function ${clang_getCursorSpelling(cursor).convertAndDispose()}")
            return null
        }
        var name = cursor.spelling

        val cursorReturnType = clang_getCursorResultType(cursor)
        if (cursorReturnType.name.isUnknownTemplate()) return null

        var returnType = convertType(cursorReturnType, clang_getCursorResultTypeAttributes(cursor))

        val parameters = mutableListOf<Parameter>()
        parameters += getFunctionParameters(cursor) ?: return null

        val binaryName = clang_Cursor_getMangling(cursor).convertAndDispose()

        val definitionCursor = clang_getCursorDefinition(cursor)
        val isDefined = (clang_Cursor_isNull(definitionCursor) == 0)

        val isVararg = clang_Cursor_isVariadic(cursor) != 0

        return FunctionDecl(name, parameters, returnType, isVararg)
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
        val parameters = getFunctionParameters(cursor)!!

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
            isExplicitlyDesignatedInitializer = hasAttribute(cursor, OBJC_DESIGNATED_INITIALIZER),
            isDirect = hasAttribute(cursor, OBJC_DIRECT),
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
    protected open fun isFuncDeclEligible(cursor: CValue<CXCursor>): Boolean {
        var ret = true
        visitChildren(cursor) { childCursor, _ ->
            when (childCursor.kind) {
                CXCursorKind.CXCursor_TemplateRef -> {
                    ret = false
                    CXChildVisitResult.CXChildVisit_Break
                }
                else -> CXChildVisitResult.CXChildVisit_Recurse
            }
        }
        return ret
    }

    private fun getFunctionParameters(cursor: CValue<CXCursor>): List<Parameter>? {
        val argNum = clang_Cursor_getNumArguments(cursor)
        val args = (0..argNum - 1).map {
            val argCursor = clang_Cursor_getArgument(cursor, it)
            if (argCursor.type.name.isUnknownTemplate()) {
                return null
            }
            val argName = getCursorSpelling(argCursor)
            val type = convertCursorType(argCursor)
            Parameter(argName, type,
                    nsConsumed = hasAttribute(argCursor, NS_CONSUMED))
        }
        return args
    }

    private val NS_CONSUMED = "ns_consumed"
    private val OBJC_DESIGNATED_INITIALIZER = "objc_designated_initializer"
    private val OBJC_DIRECT = "objc_direct"

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

fun buildNativeIndexImpl(library: NativeLibrary, verbose: Boolean, allowPrecompiledHeaders: Boolean): IndexerResult {
    val result = NativeIndexImpl(library, verbose)
    return buildNativeIndexImpl(result, allowPrecompiledHeaders)
}

fun buildNativeIndexImpl(index: NativeIndexImpl, allowPrecompiledHeaders: Boolean): IndexerResult {
    val compilation = indexDeclarations(index, allowPrecompiledHeaders)
    return IndexerResult(index, compilation)
}

private fun indexDeclarations(nativeIndex: NativeIndexImpl, allowPrecompiledHeaders: Boolean): Compilation {
    // Below, declarations from PCH should be excluded to restrict `visitChildren` to visit local declarations only
    withIndex(excludeDeclarationsFromPCH = true) { index ->
        val errors = mutableListOf<Diagnostic>()
        val translationUnit = nativeIndex.library.let {
            if (allowPrecompiledHeaders) it.copyWithArgsForPCH() else it
        }.parse(
                index,
                options = CXTranslationUnit_DetailedPreprocessingRecord or CXTranslationUnit_ForSerialization,
                diagnosticHandler = { if (it.isError()) errors.add(it) }
        )
        try {
            if (errors.isNotEmpty()) {
                error(errors.take(10).joinToString("\n") { it.format })
            }
            translationUnit.ensureNoCompileErrors()

            val compilation = nativeIndex.library.let {
                if (allowPrecompiledHeaders) it.withPrecompiledHeader(translationUnit) else it
            }

            UnitsHolder(index).use { unitsHolder ->
                val (headers, ownTranslationUnits) = getHeadersAndUnits(nativeIndex.library, index, translationUnit, unitsHolder)
                val ownHeaders = headers.ownHeaders
                val headersCanonicalPaths = ownHeaders.map { it?.canonicalPath }.toSet()

                val unitsToProcess = (ownTranslationUnits + setOf(translationUnit)).toList()

                nativeIndex.includedHeaders = ownHeaders.map {
                    nativeIndex.getHeaderId(it)
                }

                unitsToProcess.forEach {
                    indexTranslationUnit(index, it, 0, object : Indexer {
                        override fun indexDeclaration(info: CXIdxDeclInfo) {
                            val file = memScoped {
                                val fileVar = alloc<CXFileVar>()
                                clang_indexLoc_getFileLocation(info.loc.readValue(), null, fileVar.ptr, null, null, null)
                                fileVar.value
                            }

                            if (file?.canonicalPath in headersCanonicalPaths) {
                                nativeIndex.indexDeclaration(info)
                            }
                        }
                    })
                }

                unitsToProcess.forEach {
                    visitChildren(clang_getTranslationUnitCursor(it)) { cursor, _ ->
                        val file = getContainingFile(cursor)
                        if (file in ownHeaders && nativeIndex.library.includesDeclaration(cursor)) {
                            when (cursor.kind) {
                                CXCursorKind.CXCursor_ObjCInterfaceDecl -> nativeIndex.indexObjCClass(cursor)
                                CXCursorKind.CXCursor_ObjCProtocolDecl -> nativeIndex.indexObjCProtocol(cursor)
                                CXCursorKind.CXCursor_ObjCCategoryDecl -> {
                                    // This fixes https://youtrack.jetbrains.com/issue/KT-49455, which effectively seems to be a bug in libclang:
                                    // the libclang indexer doesn't properly index categories with
                                    // `__attribute__((external_source_symbol(language="Swift",...)))`.
                                    // As a workaround, additionally enumerate all the categories explicitly.
                                    nativeIndex.indexObjCCategory(cursor)
                                }

                                else -> {}
                            }
                        }
                        CXChildVisitResult.CXChildVisit_Continue
                    }
                }

                val compilationWithPCH = if (allowPrecompiledHeaders)
                    compilation as CompilationWithPCH
                else
                    compilation.withPrecompiledHeader(translationUnit)
                findMacros(nativeIndex, compilationWithPCH, unitsToProcess, ownHeaders)

                return compilation
            }
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    }
}
