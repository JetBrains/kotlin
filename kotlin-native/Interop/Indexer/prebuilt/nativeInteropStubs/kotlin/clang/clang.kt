@file:JvmName("clang")
@file:Suppress("UNUSED_VARIABLE", "UNUSED_EXPRESSION", "DEPRECATION")
package clang

import kotlinx.cinterop.*

// NOTE THIS FILE IS AUTO-GENERATED

@CNaturalStruct("data", "private_flags")
class CXString(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var data: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(0).value
        set(value) { memberAt<COpaquePointerVar>(0).value = value }
    
    var private_flags: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
}

@CNaturalStruct("Strings", "Count")
class CXStringSet(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var Strings: CPointer<CXString>?
        get() = memberAt<CPointerVar<CXString>>(0).value
        set(value) { memberAt<CPointerVar<CXString>>(0).value = value }
    
    var Count: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
}

class CXVirtualFileOverlayImpl(rawPtr: NativePtr) : COpaque(rawPtr)

class CXModuleMapDescriptorImpl(rawPtr: NativePtr) : COpaque(rawPtr)

class CXTargetInfoImpl(rawPtr: NativePtr) : COpaque(rawPtr)

class CXTranslationUnitImpl(rawPtr: NativePtr) : COpaque(rawPtr)

@CNaturalStruct("Filename", "Contents", "Length")
class CXUnsavedFile(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    var Filename: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(0).value
        set(value) { memberAt<CPointerVar<ByteVar>>(0).value = value }
    
    var Contents: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(8).value
        set(value) { memberAt<CPointerVar<ByteVar>>(8).value = value }
    
    var Length: Long
        get() = memberAt<LongVar>(16).value
        set(value) { memberAt<LongVar>(16).value = value }
}

@CNaturalStruct("Major", "Minor", "Subminor")
class CXVersion(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(12, 4)
    
    var Major: Int
        get() = memberAt<IntVar>(0).value
        set(value) { memberAt<IntVar>(0).value = value }
    
    var Minor: Int
        get() = memberAt<IntVar>(4).value
        set(value) { memberAt<IntVar>(4).value = value }
    
    var Subminor: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
}

@CNaturalStruct("data")
class CXFileUniqueID(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    @CLength(3)
    val data: CArrayPointer<LongVar>
        get() = arrayMemberAt(0)
}

@CNaturalStruct("ptr_data", "int_data")
class CXSourceLocation(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    @CLength(2)
    val ptr_data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(0)
    
    var int_data: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
}

@CNaturalStruct("ptr_data", "begin_int_data", "end_int_data")
class CXSourceRange(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    @CLength(2)
    val ptr_data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(0)
    
    var begin_int_data: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
    
    var end_int_data: Int
        get() = memberAt<IntVar>(20).value
        set(value) { memberAt<IntVar>(20).value = value }
}

@CNaturalStruct("count", "ranges")
class CXSourceRangeList(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var count: Int
        get() = memberAt<IntVar>(0).value
        set(value) { memberAt<IntVar>(0).value = value }
    
    var ranges: CPointer<CXSourceRange>?
        get() = memberAt<CPointerVar<CXSourceRange>>(8).value
        set(value) { memberAt<CPointerVar<CXSourceRange>>(8).value = value }
}

@CNaturalStruct("kind", "amount")
class CXTUResourceUsageEntry(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var kind: CXTUResourceUsageKind
        get() = memberAt<CXTUResourceUsageKind.Var>(0).value
        set(value) { memberAt<CXTUResourceUsageKind.Var>(0).value = value }
    
    var amount: Long
        get() = memberAt<LongVar>(8).value
        set(value) { memberAt<LongVar>(8).value = value }
}

@CNaturalStruct("data", "numEntries", "entries")
class CXTUResourceUsage(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    var data: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(0).value
        set(value) { memberAt<COpaquePointerVar>(0).value = value }
    
    var numEntries: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
    var entries: CPointer<CXTUResourceUsageEntry>?
        get() = memberAt<CPointerVar<CXTUResourceUsageEntry>>(16).value
        set(value) { memberAt<CPointerVar<CXTUResourceUsageEntry>>(16).value = value }
}

@CNaturalStruct("kind", "xdata", "data")
class CXCursor(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(32, 8)
    
    var kind: CXCursorKind
        get() = memberAt<CXCursorKind.Var>(0).value
        set(value) { memberAt<CXCursorKind.Var>(0).value = value }
    
    var xdata: Int
        get() = memberAt<IntVar>(4).value
        set(value) { memberAt<IntVar>(4).value = value }
    
    @CLength(3)
    val data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(8)
}

@CNaturalStruct("Platform", "Introduced", "Deprecated", "Obsoleted", "Unavailable", "Message")
class CXPlatformAvailability(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(72, 8)
    
    val Platform: CXString
        get() = memberAt(0)
    
    val Introduced: CXVersion
        get() = memberAt(16)
    
    val Deprecated: CXVersion
        get() = memberAt(28)
    
    val Obsoleted: CXVersion
        get() = memberAt(40)
    
    var Unavailable: Int
        get() = memberAt<IntVar>(52).value
        set(value) { memberAt<IntVar>(52).value = value }
    
    val Message: CXString
        get() = memberAt(56)
}

class CXCursorSetImpl(rawPtr: NativePtr) : COpaque(rawPtr)

@CNaturalStruct("kind", "data")
class CXType(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    var kind: CXTypeKind
        get() = memberAt<CXTypeKind.Var>(0).value
        set(value) { memberAt<CXTypeKind.Var>(0).value = value }
    
    @CLength(2)
    val data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(8)
}

@CNaturalStruct("int_data", "ptr_data")
class CXToken(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    @CLength(4)
    val int_data: CArrayPointer<IntVar>
        get() = arrayMemberAt(0)
    
    var ptr_data: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(16).value
        set(value) { memberAt<COpaquePointerVar>(16).value = value }
}

@CNaturalStruct("CursorKind", "CompletionString")
class CXCompletionResult(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var CursorKind: CXCursorKind
        get() = memberAt<CXCursorKind.Var>(0).value
        set(value) { memberAt<CXCursorKind.Var>(0).value = value }
    
    var CompletionString: CXCompletionString?
        get() = memberAt<CXCompletionStringVar>(8).value
        set(value) { memberAt<CXCompletionStringVar>(8).value = value }
}

@CNaturalStruct("Results", "NumResults")
class CXCodeCompleteResults(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var Results: CPointer<CXCompletionResult>?
        get() = memberAt<CPointerVar<CXCompletionResult>>(0).value
        set(value) { memberAt<CPointerVar<CXCompletionResult>>(0).value = value }
    
    var NumResults: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
}

@CNaturalStruct("context", "visit")
class CXCursorAndRangeVisitor(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var context: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(0).value
        set(value) { memberAt<COpaquePointerVar>(0).value = value }
    
    var visit: CPointer<CFunction<(COpaquePointer?, CValue<CXCursor>, CValue<CXSourceRange>) -> CXVisitorResult>>?
        get() = memberAt<CPointerVar<CFunction<(COpaquePointer?, CValue<CXCursor>, CValue<CXSourceRange>) -> CXVisitorResult>>>(8).value
        set(value) { memberAt<CPointerVar<CFunction<(COpaquePointer?, CValue<CXCursor>, CValue<CXSourceRange>) -> CXVisitorResult>>>(8).value = value }
}

@CNaturalStruct("ptr_data", "int_data")
class CXIdxLoc(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    @CLength(2)
    val ptr_data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(0)
    
    var int_data: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
}

@CNaturalStruct("hashLoc", "filename", "file", "isImport", "isAngled", "isModuleImport")
class CXIdxIncludedFileInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(56, 8)
    
    val hashLoc: CXIdxLoc
        get() = memberAt(0)
    
    var filename: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(24).value
        set(value) { memberAt<CPointerVar<ByteVar>>(24).value = value }
    
    var file: CXFile?
        get() = memberAt<CXFileVar>(32).value
        set(value) { memberAt<CXFileVar>(32).value = value }
    
    var isImport: Int
        get() = memberAt<IntVar>(40).value
        set(value) { memberAt<IntVar>(40).value = value }
    
    var isAngled: Int
        get() = memberAt<IntVar>(44).value
        set(value) { memberAt<IntVar>(44).value = value }
    
    var isModuleImport: Int
        get() = memberAt<IntVar>(48).value
        set(value) { memberAt<IntVar>(48).value = value }
}

@CNaturalStruct("file", "module", "loc", "isImplicit")
class CXIdxImportedASTFileInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(48, 8)
    
    var file: CXFile?
        get() = memberAt<CXFileVar>(0).value
        set(value) { memberAt<CXFileVar>(0).value = value }
    
    var module: CXModule?
        get() = memberAt<CXModuleVar>(8).value
        set(value) { memberAt<CXModuleVar>(8).value = value }
    
    val loc: CXIdxLoc
        get() = memberAt(16)
    
    var isImplicit: Int
        get() = memberAt<IntVar>(40).value
        set(value) { memberAt<IntVar>(40).value = value }
}

@CNaturalStruct("kind", "cursor", "loc")
class CXIdxAttrInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(64, 8)
    
    var kind: CXIdxAttrKind
        get() = memberAt<CXIdxAttrKindVar>(0).value
        set(value) { memberAt<CXIdxAttrKindVar>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
}

@CNaturalStruct("kind", "templateKind", "lang", "name", "USR", "cursor", "attributes", "numAttributes")
class CXIdxEntityInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(80, 8)
    
    var kind: CXIdxEntityKind
        get() = memberAt<CXIdxEntityKind.Var>(0).value
        set(value) { memberAt<CXIdxEntityKind.Var>(0).value = value }
    
    var templateKind: CXIdxEntityCXXTemplateKind
        get() = memberAt<CXIdxEntityCXXTemplateKindVar>(4).value
        set(value) { memberAt<CXIdxEntityCXXTemplateKindVar>(4).value = value }
    
    var lang: CXIdxEntityLanguage
        get() = memberAt<CXIdxEntityLanguageVar>(8).value
        set(value) { memberAt<CXIdxEntityLanguageVar>(8).value = value }
    
    var name: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(16).value
        set(value) { memberAt<CPointerVar<ByteVar>>(16).value = value }
    
    var USR: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(24).value
        set(value) { memberAt<CPointerVar<ByteVar>>(24).value = value }
    
    val cursor: CXCursor
        get() = memberAt(32)
    
    var attributes: CPointer<CPointerVar<CXIdxAttrInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(64).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(64).value = value }
    
    var numAttributes: Int
        get() = memberAt<IntVar>(72).value
        set(value) { memberAt<IntVar>(72).value = value }
}

@CNaturalStruct("cursor")
class CXIdxContainerInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(32, 8)
    
    val cursor: CXCursor
        get() = memberAt(0)
}

@CNaturalStruct("attrInfo", "objcClass", "classCursor", "classLoc")
class CXIdxIBOutletCollectionAttrInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(72, 8)
    
    var attrInfo: CPointer<CXIdxAttrInfo>?
        get() = memberAt<CPointerVar<CXIdxAttrInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxAttrInfo>>(0).value = value }
    
    var objcClass: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(8).value = value }
    
    val classCursor: CXCursor
        get() = memberAt(16)
    
    val classLoc: CXIdxLoc
        get() = memberAt(48)
}

@CNaturalStruct("entityInfo", "cursor", "loc", "semanticContainer", "lexicalContainer", "isRedeclaration", "isDefinition", "isContainer", "declAsContainer", "isImplicit", "attributes", "numAttributes", "flags")
class CXIdxDeclInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(128, 8)
    
    var entityInfo: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
    var semanticContainer: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(64).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(64).value = value }
    
    var lexicalContainer: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(72).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(72).value = value }
    
    var isRedeclaration: Int
        get() = memberAt<IntVar>(80).value
        set(value) { memberAt<IntVar>(80).value = value }
    
    var isDefinition: Int
        get() = memberAt<IntVar>(84).value
        set(value) { memberAt<IntVar>(84).value = value }
    
    var isContainer: Int
        get() = memberAt<IntVar>(88).value
        set(value) { memberAt<IntVar>(88).value = value }
    
    var declAsContainer: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(96).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(96).value = value }
    
    var isImplicit: Int
        get() = memberAt<IntVar>(104).value
        set(value) { memberAt<IntVar>(104).value = value }
    
    var attributes: CPointer<CPointerVar<CXIdxAttrInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(112).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(112).value = value }
    
    var numAttributes: Int
        get() = memberAt<IntVar>(120).value
        set(value) { memberAt<IntVar>(120).value = value }
    
    var flags: Int
        get() = memberAt<IntVar>(124).value
        set(value) { memberAt<IntVar>(124).value = value }
}

@CNaturalStruct("declInfo", "kind")
class CXIdxObjCContainerDeclInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var declInfo: CPointer<CXIdxDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxDeclInfo>>(0).value = value }
    
    var kind: CXIdxObjCContainerKind
        get() = memberAt<CXIdxObjCContainerKindVar>(8).value
        set(value) { memberAt<CXIdxObjCContainerKindVar>(8).value = value }
}

@CNaturalStruct("base", "cursor", "loc")
class CXIdxBaseClassInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(64, 8)
    
    var base: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
}

@CNaturalStruct("protocol", "cursor", "loc")
class CXIdxObjCProtocolRefInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(64, 8)
    
    var protocol: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
}

@CNaturalStruct("protocols", "numProtocols")
class CXIdxObjCProtocolRefListInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(16, 8)
    
    var protocols: CPointer<CPointerVar<CXIdxObjCProtocolRefInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxObjCProtocolRefInfo>>>(0).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxObjCProtocolRefInfo>>>(0).value = value }
    
    var numProtocols: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
}

@CNaturalStruct("containerInfo", "superInfo", "protocols")
class CXIdxObjCInterfaceDeclInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    var containerInfo: CPointer<CXIdxObjCContainerDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value = value }
    
    var superInfo: CPointer<CXIdxBaseClassInfo>?
        get() = memberAt<CPointerVar<CXIdxBaseClassInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxBaseClassInfo>>(8).value = value }
    
    var protocols: CPointer<CXIdxObjCProtocolRefListInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(16).value
        set(value) { memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(16).value = value }
}

@CNaturalStruct("containerInfo", "objcClass", "classCursor", "classLoc", "protocols")
class CXIdxObjCCategoryDeclInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(80, 8)
    
    var containerInfo: CPointer<CXIdxObjCContainerDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value = value }
    
    var objcClass: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(8).value = value }
    
    val classCursor: CXCursor
        get() = memberAt(16)
    
    val classLoc: CXIdxLoc
        get() = memberAt(48)
    
    var protocols: CPointer<CXIdxObjCProtocolRefListInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(72).value
        set(value) { memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(72).value = value }
}

@CNaturalStruct("declInfo", "getter", "setter")
class CXIdxObjCPropertyDeclInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    var declInfo: CPointer<CXIdxDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxDeclInfo>>(0).value = value }
    
    var getter: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(8).value = value }
    
    var setter: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(16).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(16).value = value }
}

@CNaturalStruct("declInfo", "bases", "numBases")
class CXIdxCXXClassDeclInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(24, 8)
    
    var declInfo: CPointer<CXIdxDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxDeclInfo>>(0).value = value }
    
    var bases: CPointer<CPointerVar<CXIdxBaseClassInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxBaseClassInfo>>>(8).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxBaseClassInfo>>>(8).value = value }
    
    var numBases: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
}

@CNaturalStruct("kind", "cursor", "loc", "referencedEntity", "parentEntity", "container", "role")
class CXIdxEntityRefInfo(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(96, 8)
    
    var kind: CXIdxEntityRefKind
        get() = memberAt<CXIdxEntityRefKindVar>(0).value
        set(value) { memberAt<CXIdxEntityRefKindVar>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
    var referencedEntity: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(64).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(64).value = value }
    
    var parentEntity: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(72).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(72).value = value }
    
    var container: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(80).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(80).value = value }
    
    var role: CXSymbolRole
        get() = memberAt<CXSymbolRoleVar>(88).value
        set(value) { memberAt<CXSymbolRoleVar>(88).value = value }
}

@CNaturalStruct("abortQuery", "diagnostic", "enteredMainFile", "ppIncludedFile", "importedASTFile", "startedTranslationUnit", "indexDeclaration", "indexEntityReference")
class IndexerCallbacks(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(64, 8)
    
    var abortQuery: CPointer<CFunction<(CXClientData?, COpaquePointer?) -> Int>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> Int>>>(0).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> Int>>>(0).value = value }
    
    var diagnostic: CPointer<CFunction<(CXClientData?, CXDiagnosticSet?, COpaquePointer?) -> Unit>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CXDiagnosticSet?, COpaquePointer?) -> Unit>>>(8).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CXDiagnosticSet?, COpaquePointer?) -> Unit>>>(8).value = value }
    
    var enteredMainFile: CPointer<CFunction<(CXClientData?, CXFile?, COpaquePointer?) -> CXIdxClientFile?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CXFile?, COpaquePointer?) -> CXIdxClientFile?>>>(16).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CXFile?, COpaquePointer?) -> CXIdxClientFile?>>>(16).value = value }
    
    var ppIncludedFile: CPointer<CFunction<(CXClientData?, CPointer<CXIdxIncludedFileInfo>?) -> CXIdxClientFile?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxIncludedFileInfo>?) -> CXIdxClientFile?>>>(24).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxIncludedFileInfo>?) -> CXIdxClientFile?>>>(24).value = value }
    
    var importedASTFile: CPointer<CFunction<(CXClientData?, CPointer<CXIdxImportedASTFileInfo>?) -> CXIdxClientASTFile?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxImportedASTFileInfo>?) -> CXIdxClientASTFile?>>>(32).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxImportedASTFileInfo>?) -> CXIdxClientASTFile?>>>(32).value = value }
    
    var startedTranslationUnit: CPointer<CFunction<(CXClientData?, COpaquePointer?) -> CXIdxClientContainer?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> CXIdxClientContainer?>>>(40).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> CXIdxClientContainer?>>>(40).value = value }
    
    var indexDeclaration: CPointer<CFunction<(CXClientData?, CPointer<CXIdxDeclInfo>?) -> Unit>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxDeclInfo>?) -> Unit>>>(48).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxDeclInfo>?) -> Unit>>>(48).value = value }
    
    var indexEntityReference: CPointer<CFunction<(CXClientData?, CPointer<CXIdxEntityRefInfo>?) -> Unit>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxEntityRefInfo>?) -> Unit>>>(56).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxEntityRefInfo>?) -> Unit>>>(56).value = value }
}

@CNaturalStruct("typeOpaquePtr")
class CXTypeAttributes(rawPtr: NativePtr) : CStructVar(rawPtr) {
    
    companion object : CStructVar.Type(8, 8)
    
    var typeOpaquePtr: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(0).value
        set(value) { memberAt<COpaquePointerVar>(0).value = value }
}

enum class CXErrorCode(override val value: Int) : CEnum {
    CXError_Success(0),
    CXError_Failure(1),
    CXError_Crashed(2),
    CXError_InvalidArguments(3),
    CXError_ASTReadError(4),
    CXError_RefactoringActionUnavailable(5),
    CXError_RefactoringNameSizeMismatch(6),
    CXError_RefactoringNameInvalid(7),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXErrorCode.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXErrorCode
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXAvailabilityKind(override val value: Int) : CEnum {
    CXAvailability_Available(0),
    CXAvailability_Deprecated(1),
    CXAvailability_NotAvailable(2),
    CXAvailability_NotAccessible(3),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXAvailabilityKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXAvailabilityKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXCursor_ExceptionSpecificationKind(override val value: Int) : CEnum {
    CXCursor_ExceptionSpecificationKind_None(0),
    CXCursor_ExceptionSpecificationKind_DynamicNone(1),
    CXCursor_ExceptionSpecificationKind_Dynamic(2),
    CXCursor_ExceptionSpecificationKind_MSAny(3),
    CXCursor_ExceptionSpecificationKind_BasicNoexcept(4),
    CXCursor_ExceptionSpecificationKind_ComputedNoexcept(5),
    CXCursor_ExceptionSpecificationKind_Unevaluated(6),
    CXCursor_ExceptionSpecificationKind_Uninstantiated(7),
    CXCursor_ExceptionSpecificationKind_Unparsed(8),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXCursor_ExceptionSpecificationKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXCursor_ExceptionSpecificationKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXDiagnosticSeverity(override val value: Int) : CEnum {
    CXDiagnostic_Ignored(0),
    CXDiagnostic_Note(1),
    CXDiagnostic_Warning(2),
    CXDiagnostic_Error(3),
    CXDiagnostic_Fatal(4),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXDiagnosticSeverity.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXDiagnosticSeverity
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXLoadDiag_Error(override val value: Int) : CEnum {
    CXLoadDiag_None(0),
    CXLoadDiag_Unknown(1),
    CXLoadDiag_CannotLoad(2),
    CXLoadDiag_InvalidFile(3),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXLoadDiag_Error.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXLoadDiag_Error
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXSaveError(override val value: Int) : CEnum {
    CXSaveError_None(0),
    CXSaveError_Unknown(1),
    CXSaveError_TranslationErrors(2),
    CXSaveError_InvalidTU(3),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXSaveError.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXSaveError
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXTUResourceUsageKind(override val value: Int) : CEnum {
    CXTUResourceUsage_AST(1),
    CXTUResourceUsage_Identifiers(2),
    CXTUResourceUsage_Selectors(3),
    CXTUResourceUsage_GlobalCompletionResults(4),
    CXTUResourceUsage_SourceManagerContentCache(5),
    CXTUResourceUsage_AST_SideTables(6),
    CXTUResourceUsage_SourceManager_Membuffer_Malloc(7),
    CXTUResourceUsage_SourceManager_Membuffer_MMap(8),
    CXTUResourceUsage_ExternalASTSource_Membuffer_Malloc(9),
    CXTUResourceUsage_ExternalASTSource_Membuffer_MMap(10),
    CXTUResourceUsage_Preprocessor(11),
    CXTUResourceUsage_PreprocessingRecord(12),
    CXTUResourceUsage_SourceManager_DataStructures(13),
    CXTUResourceUsage_Preprocessor_HeaderSearch(14),
    ;
    
    companion object {
        val CXTUResourceUsage_MEMORY_IN_BYTES_BEGIN = CXTUResourceUsage_AST
        val CXTUResourceUsage_First = CXTUResourceUsage_AST
        val CXTUResourceUsage_MEMORY_IN_BYTES_END = CXTUResourceUsage_Preprocessor_HeaderSearch
        val CXTUResourceUsage_Last = CXTUResourceUsage_Preprocessor_HeaderSearch
        
        fun byValue(value: Int) = CXTUResourceUsageKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXTUResourceUsageKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXCursorKind(override val value: Int) : CEnum {
    CXCursor_UnexposedDecl(1),
    CXCursor_StructDecl(2),
    CXCursor_UnionDecl(3),
    CXCursor_ClassDecl(4),
    CXCursor_EnumDecl(5),
    CXCursor_FieldDecl(6),
    CXCursor_EnumConstantDecl(7),
    CXCursor_FunctionDecl(8),
    CXCursor_VarDecl(9),
    CXCursor_ParmDecl(10),
    CXCursor_ObjCInterfaceDecl(11),
    CXCursor_ObjCCategoryDecl(12),
    CXCursor_ObjCProtocolDecl(13),
    CXCursor_ObjCPropertyDecl(14),
    CXCursor_ObjCIvarDecl(15),
    CXCursor_ObjCInstanceMethodDecl(16),
    CXCursor_ObjCClassMethodDecl(17),
    CXCursor_ObjCImplementationDecl(18),
    CXCursor_ObjCCategoryImplDecl(19),
    CXCursor_TypedefDecl(20),
    CXCursor_CXXMethod(21),
    CXCursor_Namespace(22),
    CXCursor_LinkageSpec(23),
    CXCursor_Constructor(24),
    CXCursor_Destructor(25),
    CXCursor_ConversionFunction(26),
    CXCursor_TemplateTypeParameter(27),
    CXCursor_NonTypeTemplateParameter(28),
    CXCursor_TemplateTemplateParameter(29),
    CXCursor_FunctionTemplate(30),
    CXCursor_ClassTemplate(31),
    CXCursor_ClassTemplatePartialSpecialization(32),
    CXCursor_NamespaceAlias(33),
    CXCursor_UsingDirective(34),
    CXCursor_UsingDeclaration(35),
    CXCursor_TypeAliasDecl(36),
    CXCursor_ObjCSynthesizeDecl(37),
    CXCursor_ObjCDynamicDecl(38),
    CXCursor_CXXAccessSpecifier(39),
    CXCursor_ObjCSuperClassRef(40),
    CXCursor_ObjCProtocolRef(41),
    CXCursor_ObjCClassRef(42),
    CXCursor_TypeRef(43),
    CXCursor_CXXBaseSpecifier(44),
    CXCursor_TemplateRef(45),
    CXCursor_NamespaceRef(46),
    CXCursor_MemberRef(47),
    CXCursor_LabelRef(48),
    CXCursor_OverloadedDeclRef(49),
    CXCursor_VariableRef(50),
    CXCursor_InvalidFile(70),
    CXCursor_NoDeclFound(71),
    CXCursor_NotImplemented(72),
    CXCursor_InvalidCode(73),
    CXCursor_UnexposedExpr(100),
    CXCursor_DeclRefExpr(101),
    CXCursor_MemberRefExpr(102),
    CXCursor_CallExpr(103),
    CXCursor_ObjCMessageExpr(104),
    CXCursor_BlockExpr(105),
    CXCursor_IntegerLiteral(106),
    CXCursor_FloatingLiteral(107),
    CXCursor_ImaginaryLiteral(108),
    CXCursor_StringLiteral(109),
    CXCursor_CharacterLiteral(110),
    CXCursor_ParenExpr(111),
    CXCursor_UnaryOperator(112),
    CXCursor_ArraySubscriptExpr(113),
    CXCursor_BinaryOperator(114),
    CXCursor_CompoundAssignOperator(115),
    CXCursor_ConditionalOperator(116),
    CXCursor_CStyleCastExpr(117),
    CXCursor_CompoundLiteralExpr(118),
    CXCursor_InitListExpr(119),
    CXCursor_AddrLabelExpr(120),
    CXCursor_StmtExpr(121),
    CXCursor_GenericSelectionExpr(122),
    CXCursor_GNUNullExpr(123),
    CXCursor_CXXStaticCastExpr(124),
    CXCursor_CXXDynamicCastExpr(125),
    CXCursor_CXXReinterpretCastExpr(126),
    CXCursor_CXXConstCastExpr(127),
    CXCursor_CXXFunctionalCastExpr(128),
    CXCursor_CXXTypeidExpr(129),
    CXCursor_CXXBoolLiteralExpr(130),
    CXCursor_CXXNullPtrLiteralExpr(131),
    CXCursor_CXXThisExpr(132),
    CXCursor_CXXThrowExpr(133),
    CXCursor_CXXNewExpr(134),
    CXCursor_CXXDeleteExpr(135),
    CXCursor_UnaryExpr(136),
    CXCursor_ObjCStringLiteral(137),
    CXCursor_ObjCEncodeExpr(138),
    CXCursor_ObjCSelectorExpr(139),
    CXCursor_ObjCProtocolExpr(140),
    CXCursor_ObjCBridgedCastExpr(141),
    CXCursor_PackExpansionExpr(142),
    CXCursor_SizeOfPackExpr(143),
    CXCursor_LambdaExpr(144),
    CXCursor_ObjCBoolLiteralExpr(145),
    CXCursor_ObjCSelfExpr(146),
    CXCursor_OMPArraySectionExpr(147),
    CXCursor_ObjCAvailabilityCheckExpr(148),
    CXCursor_FixedPointLiteral(149),
    CXCursor_UnexposedStmt(200),
    CXCursor_LabelStmt(201),
    CXCursor_CompoundStmt(202),
    CXCursor_CaseStmt(203),
    CXCursor_DefaultStmt(204),
    CXCursor_IfStmt(205),
    CXCursor_SwitchStmt(206),
    CXCursor_WhileStmt(207),
    CXCursor_DoStmt(208),
    CXCursor_ForStmt(209),
    CXCursor_GotoStmt(210),
    CXCursor_IndirectGotoStmt(211),
    CXCursor_ContinueStmt(212),
    CXCursor_BreakStmt(213),
    CXCursor_ReturnStmt(214),
    CXCursor_GCCAsmStmt(215),
    CXCursor_ObjCAtTryStmt(216),
    CXCursor_ObjCAtCatchStmt(217),
    CXCursor_ObjCAtFinallyStmt(218),
    CXCursor_ObjCAtThrowStmt(219),
    CXCursor_ObjCAtSynchronizedStmt(220),
    CXCursor_ObjCAutoreleasePoolStmt(221),
    CXCursor_ObjCForCollectionStmt(222),
    CXCursor_CXXCatchStmt(223),
    CXCursor_CXXTryStmt(224),
    CXCursor_CXXForRangeStmt(225),
    CXCursor_SEHTryStmt(226),
    CXCursor_SEHExceptStmt(227),
    CXCursor_SEHFinallyStmt(228),
    CXCursor_MSAsmStmt(229),
    CXCursor_NullStmt(230),
    CXCursor_DeclStmt(231),
    CXCursor_OMPParallelDirective(232),
    CXCursor_OMPSimdDirective(233),
    CXCursor_OMPForDirective(234),
    CXCursor_OMPSectionsDirective(235),
    CXCursor_OMPSectionDirective(236),
    CXCursor_OMPSingleDirective(237),
    CXCursor_OMPParallelForDirective(238),
    CXCursor_OMPParallelSectionsDirective(239),
    CXCursor_OMPTaskDirective(240),
    CXCursor_OMPMasterDirective(241),
    CXCursor_OMPCriticalDirective(242),
    CXCursor_OMPTaskyieldDirective(243),
    CXCursor_OMPBarrierDirective(244),
    CXCursor_OMPTaskwaitDirective(245),
    CXCursor_OMPFlushDirective(246),
    CXCursor_SEHLeaveStmt(247),
    CXCursor_OMPOrderedDirective(248),
    CXCursor_OMPAtomicDirective(249),
    CXCursor_OMPForSimdDirective(250),
    CXCursor_OMPParallelForSimdDirective(251),
    CXCursor_OMPTargetDirective(252),
    CXCursor_OMPTeamsDirective(253),
    CXCursor_OMPTaskgroupDirective(254),
    CXCursor_OMPCancellationPointDirective(255),
    CXCursor_OMPCancelDirective(256),
    CXCursor_OMPTargetDataDirective(257),
    CXCursor_OMPTaskLoopDirective(258),
    CXCursor_OMPTaskLoopSimdDirective(259),
    CXCursor_OMPDistributeDirective(260),
    CXCursor_OMPTargetEnterDataDirective(261),
    CXCursor_OMPTargetExitDataDirective(262),
    CXCursor_OMPTargetParallelDirective(263),
    CXCursor_OMPTargetParallelForDirective(264),
    CXCursor_OMPTargetUpdateDirective(265),
    CXCursor_OMPDistributeParallelForDirective(266),
    CXCursor_OMPDistributeParallelForSimdDirective(267),
    CXCursor_OMPDistributeSimdDirective(268),
    CXCursor_OMPTargetParallelForSimdDirective(269),
    CXCursor_OMPTargetSimdDirective(270),
    CXCursor_OMPTeamsDistributeDirective(271),
    CXCursor_OMPTeamsDistributeSimdDirective(272),
    CXCursor_OMPTeamsDistributeParallelForSimdDirective(273),
    CXCursor_OMPTeamsDistributeParallelForDirective(274),
    CXCursor_OMPTargetTeamsDirective(275),
    CXCursor_OMPTargetTeamsDistributeDirective(276),
    CXCursor_OMPTargetTeamsDistributeParallelForDirective(277),
    CXCursor_OMPTargetTeamsDistributeParallelForSimdDirective(278),
    CXCursor_OMPTargetTeamsDistributeSimdDirective(279),
    CXCursor_TranslationUnit(300),
    CXCursor_UnexposedAttr(400),
    CXCursor_IBActionAttr(401),
    CXCursor_IBOutletAttr(402),
    CXCursor_IBOutletCollectionAttr(403),
    CXCursor_CXXFinalAttr(404),
    CXCursor_CXXOverrideAttr(405),
    CXCursor_AnnotateAttr(406),
    CXCursor_AsmLabelAttr(407),
    CXCursor_PackedAttr(408),
    CXCursor_PureAttr(409),
    CXCursor_ConstAttr(410),
    CXCursor_NoDuplicateAttr(411),
    CXCursor_CUDAConstantAttr(412),
    CXCursor_CUDADeviceAttr(413),
    CXCursor_CUDAGlobalAttr(414),
    CXCursor_CUDAHostAttr(415),
    CXCursor_CUDASharedAttr(416),
    CXCursor_VisibilityAttr(417),
    CXCursor_DLLExport(418),
    CXCursor_DLLImport(419),
    CXCursor_NSReturnsRetained(420),
    CXCursor_NSReturnsNotRetained(421),
    CXCursor_NSReturnsAutoreleased(422),
    CXCursor_NSConsumesSelf(423),
    CXCursor_NSConsumed(424),
    CXCursor_ObjCException(425),
    CXCursor_ObjCNSObject(426),
    CXCursor_ObjCIndependentClass(427),
    CXCursor_ObjCPreciseLifetime(428),
    CXCursor_ObjCReturnsInnerPointer(429),
    CXCursor_ObjCRequiresSuper(430),
    CXCursor_ObjCRootClass(431),
    CXCursor_ObjCSubclassingRestricted(432),
    CXCursor_ObjCExplicitProtocolImpl(433),
    CXCursor_ObjCDesignatedInitializer(434),
    CXCursor_ObjCRuntimeVisible(435),
    CXCursor_ObjCBoxable(436),
    CXCursor_FlagEnum(437),
    CXCursor_PreprocessingDirective(500),
    CXCursor_MacroDefinition(501),
    CXCursor_MacroExpansion(502),
    CXCursor_InclusionDirective(503),
    CXCursor_ModuleImportDecl(600),
    CXCursor_TypeAliasTemplateDecl(601),
    CXCursor_StaticAssert(602),
    CXCursor_LastExtraDecl(603),
    CXCursor_OverloadCandidate(700),
    ;
    
    companion object {
        val CXCursor_FirstDecl = CXCursor_UnexposedDecl
        val CXCursor_LastDecl = CXCursor_CXXAccessSpecifier
        val CXCursor_FirstRef = CXCursor_ObjCSuperClassRef
        val CXCursor_LastRef = CXCursor_VariableRef
        val CXCursor_FirstInvalid = CXCursor_InvalidFile
        val CXCursor_LastInvalid = CXCursor_InvalidCode
        val CXCursor_FirstExpr = CXCursor_UnexposedExpr
        val CXCursor_LastExpr = CXCursor_FixedPointLiteral
        val CXCursor_FirstStmt = CXCursor_UnexposedStmt
        val CXCursor_AsmStmt = CXCursor_GCCAsmStmt
        val CXCursor_LastStmt = CXCursor_OMPTargetTeamsDistributeSimdDirective
        val CXCursor_FirstAttr = CXCursor_UnexposedAttr
        val CXCursor_LastAttr = CXCursor_FlagEnum
        val CXCursor_FirstPreprocessing = CXCursor_PreprocessingDirective
        val CXCursor_MacroInstantiation = CXCursor_MacroExpansion
        val CXCursor_LastPreprocessing = CXCursor_InclusionDirective
        val CXCursor_FirstExtraDecl = CXCursor_ModuleImportDecl
        val CXCursor_FriendDecl = CXCursor_LastExtraDecl
        
        fun byValue(value: Int) = CXCursorKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXCursorKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXLinkageKind(override val value: Int) : CEnum {
    CXLinkage_Invalid(0),
    CXLinkage_NoLinkage(1),
    CXLinkage_Internal(2),
    CXLinkage_UniqueExternal(3),
    CXLinkage_External(4),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXLinkageKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXLinkageKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXVisibilityKind(override val value: Int) : CEnum {
    CXVisibility_Invalid(0),
    CXVisibility_Hidden(1),
    CXVisibility_Protected(2),
    CXVisibility_Default(3),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXVisibilityKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXVisibilityKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXLanguageKind(override val value: Int) : CEnum {
    CXLanguage_Invalid(0),
    CXLanguage_C(1),
    CXLanguage_ObjC(2),
    CXLanguage_CPlusPlus(3),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXLanguageKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXLanguageKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXTypeKind(override val value: Int) : CEnum {
    CXType_Invalid(0),
    CXType_Unexposed(1),
    CXType_Void(2),
    CXType_Bool(3),
    CXType_Char_U(4),
    CXType_UChar(5),
    CXType_Char16(6),
    CXType_Char32(7),
    CXType_UShort(8),
    CXType_UInt(9),
    CXType_ULong(10),
    CXType_ULongLong(11),
    CXType_UInt128(12),
    CXType_Char_S(13),
    CXType_SChar(14),
    CXType_WChar(15),
    CXType_Short(16),
    CXType_Int(17),
    CXType_Long(18),
    CXType_LongLong(19),
    CXType_Int128(20),
    CXType_Float(21),
    CXType_Double(22),
    CXType_LongDouble(23),
    CXType_NullPtr(24),
    CXType_Overload(25),
    CXType_Dependent(26),
    CXType_ObjCId(27),
    CXType_ObjCClass(28),
    CXType_ObjCSel(29),
    CXType_Float128(30),
    CXType_Half(31),
    CXType_Float16(32),
    CXType_ShortAccum(33),
    CXType_Accum(34),
    CXType_LongAccum(35),
    CXType_UShortAccum(36),
    CXType_UAccum(37),
    CXType_ULongAccum(38),
    CXType_Complex(100),
    CXType_Pointer(101),
    CXType_BlockPointer(102),
    CXType_LValueReference(103),
    CXType_RValueReference(104),
    CXType_Record(105),
    CXType_Enum(106),
    CXType_Typedef(107),
    CXType_ObjCInterface(108),
    CXType_ObjCObjectPointer(109),
    CXType_FunctionNoProto(110),
    CXType_FunctionProto(111),
    CXType_ConstantArray(112),
    CXType_Vector(113),
    CXType_IncompleteArray(114),
    CXType_VariableArray(115),
    CXType_DependentSizedArray(116),
    CXType_MemberPointer(117),
    CXType_Auto(118),
    CXType_Elaborated(119),
    CXType_Pipe(120),
    CXType_OCLImage1dRO(121),
    CXType_OCLImage1dArrayRO(122),
    CXType_OCLImage1dBufferRO(123),
    CXType_OCLImage2dRO(124),
    CXType_OCLImage2dArrayRO(125),
    CXType_OCLImage2dDepthRO(126),
    CXType_OCLImage2dArrayDepthRO(127),
    CXType_OCLImage2dMSAARO(128),
    CXType_OCLImage2dArrayMSAARO(129),
    CXType_OCLImage2dMSAADepthRO(130),
    CXType_OCLImage2dArrayMSAADepthRO(131),
    CXType_OCLImage3dRO(132),
    CXType_OCLImage1dWO(133),
    CXType_OCLImage1dArrayWO(134),
    CXType_OCLImage1dBufferWO(135),
    CXType_OCLImage2dWO(136),
    CXType_OCLImage2dArrayWO(137),
    CXType_OCLImage2dDepthWO(138),
    CXType_OCLImage2dArrayDepthWO(139),
    CXType_OCLImage2dMSAAWO(140),
    CXType_OCLImage2dArrayMSAAWO(141),
    CXType_OCLImage2dMSAADepthWO(142),
    CXType_OCLImage2dArrayMSAADepthWO(143),
    CXType_OCLImage3dWO(144),
    CXType_OCLImage1dRW(145),
    CXType_OCLImage1dArrayRW(146),
    CXType_OCLImage1dBufferRW(147),
    CXType_OCLImage2dRW(148),
    CXType_OCLImage2dArrayRW(149),
    CXType_OCLImage2dDepthRW(150),
    CXType_OCLImage2dArrayDepthRW(151),
    CXType_OCLImage2dMSAARW(152),
    CXType_OCLImage2dArrayMSAARW(153),
    CXType_OCLImage2dMSAADepthRW(154),
    CXType_OCLImage2dArrayMSAADepthRW(155),
    CXType_OCLImage3dRW(156),
    CXType_OCLSampler(157),
    CXType_OCLEvent(158),
    CXType_OCLQueue(159),
    CXType_OCLReserveID(160),
    CXType_ObjCObject(161),
    CXType_ObjCTypeParam(162),
    CXType_Attributed(163),
    CXType_OCLIntelSubgroupAVCMcePayload(164),
    CXType_OCLIntelSubgroupAVCImePayload(165),
    CXType_OCLIntelSubgroupAVCRefPayload(166),
    CXType_OCLIntelSubgroupAVCSicPayload(167),
    CXType_OCLIntelSubgroupAVCMceResult(168),
    CXType_OCLIntelSubgroupAVCImeResult(169),
    CXType_OCLIntelSubgroupAVCRefResult(170),
    CXType_OCLIntelSubgroupAVCSicResult(171),
    CXType_OCLIntelSubgroupAVCImeResultSingleRefStreamout(172),
    CXType_OCLIntelSubgroupAVCImeResultDualRefStreamout(173),
    CXType_OCLIntelSubgroupAVCImeSingleRefStreamin(174),
    CXType_OCLIntelSubgroupAVCImeDualRefStreamin(175),
    ;
    
    companion object {
        val CXType_FirstBuiltin = CXType_Void
        val CXType_LastBuiltin = CXType_ULongAccum
        
        fun byValue(value: Int) = CXTypeKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXTypeKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXCallingConv(override val value: Int) : CEnum {
    CXCallingConv_Default(0),
    CXCallingConv_C(1),
    CXCallingConv_X86StdCall(2),
    CXCallingConv_X86FastCall(3),
    CXCallingConv_X86ThisCall(4),
    CXCallingConv_X86Pascal(5),
    CXCallingConv_AAPCS(6),
    CXCallingConv_AAPCS_VFP(7),
    CXCallingConv_X86RegCall(8),
    CXCallingConv_IntelOclBicc(9),
    CXCallingConv_Win64(10),
    CXCallingConv_X86_64SysV(11),
    CXCallingConv_X86VectorCall(12),
    CXCallingConv_Swift(13),
    CXCallingConv_PreserveMost(14),
    CXCallingConv_PreserveAll(15),
    CXCallingConv_AArch64VectorCall(16),
    CXCallingConv_Invalid(100),
    CXCallingConv_Unexposed(200),
    ;
    
    companion object {
        val CXCallingConv_X86_64Win64 = CXCallingConv_Win64
        
        fun byValue(value: Int) = CXCallingConv.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXCallingConv
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXTemplateArgumentKind(override val value: Int) : CEnum {
    CXTemplateArgumentKind_Null(0),
    CXTemplateArgumentKind_Type(1),
    CXTemplateArgumentKind_Declaration(2),
    CXTemplateArgumentKind_NullPtr(3),
    CXTemplateArgumentKind_Integral(4),
    CXTemplateArgumentKind_Template(5),
    CXTemplateArgumentKind_TemplateExpansion(6),
    CXTemplateArgumentKind_Expression(7),
    CXTemplateArgumentKind_Pack(8),
    CXTemplateArgumentKind_Invalid(9),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXTemplateArgumentKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXTemplateArgumentKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CX_CXXAccessSpecifier(override val value: Int) : CEnum {
    CX_CXXInvalidAccessSpecifier(0),
    CX_CXXPublic(1),
    CX_CXXProtected(2),
    CX_CXXPrivate(3),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CX_CXXAccessSpecifier.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CX_CXXAccessSpecifier
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CX_StorageClass(override val value: Int) : CEnum {
    CX_SC_Invalid(0),
    CX_SC_None(1),
    CX_SC_Extern(2),
    CX_SC_Static(3),
    CX_SC_PrivateExtern(4),
    CX_SC_OpenCLWorkGroupLocal(5),
    CX_SC_Auto(6),
    CX_SC_Register(7),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CX_StorageClass.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CX_StorageClass
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXChildVisitResult(override val value: Int) : CEnum {
    CXChildVisit_Break(0),
    CXChildVisit_Continue(1),
    CXChildVisit_Recurse(2),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXChildVisitResult.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXChildVisitResult
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXTokenKind(override val value: Int) : CEnum {
    CXToken_Punctuation(0),
    CXToken_Keyword(1),
    CXToken_Identifier(2),
    CXToken_Literal(3),
    CXToken_Comment(4),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXTokenKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXTokenKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXCompletionChunkKind(override val value: Int) : CEnum {
    CXCompletionChunk_Optional(0),
    CXCompletionChunk_TypedText(1),
    CXCompletionChunk_Text(2),
    CXCompletionChunk_Placeholder(3),
    CXCompletionChunk_Informative(4),
    CXCompletionChunk_CurrentParameter(5),
    CXCompletionChunk_LeftParen(6),
    CXCompletionChunk_RightParen(7),
    CXCompletionChunk_LeftBracket(8),
    CXCompletionChunk_RightBracket(9),
    CXCompletionChunk_LeftBrace(10),
    CXCompletionChunk_RightBrace(11),
    CXCompletionChunk_LeftAngle(12),
    CXCompletionChunk_RightAngle(13),
    CXCompletionChunk_Comma(14),
    CXCompletionChunk_ResultType(15),
    CXCompletionChunk_Colon(16),
    CXCompletionChunk_SemiColon(17),
    CXCompletionChunk_Equal(18),
    CXCompletionChunk_HorizontalSpace(19),
    CXCompletionChunk_VerticalSpace(20),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXCompletionChunkKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXCompletionChunkKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXEvalResultKind(override val value: Int) : CEnum {
    CXEval_Int(1),
    CXEval_Float(2),
    CXEval_ObjCStrLiteral(3),
    CXEval_StrLiteral(4),
    CXEval_CFStr(5),
    CXEval_Other(6),
    CXEval_UnExposed(0),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXEvalResultKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXEvalResultKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXVisitorResult(override val value: Int) : CEnum {
    CXVisit_Break(0),
    CXVisit_Continue(1),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXVisitorResult.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXVisitorResult
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXResult(override val value: Int) : CEnum {
    CXResult_Success(0),
    CXResult_Invalid(1),
    CXResult_VisitBreak(2),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXResult.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXResult
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXIdxEntityKind(override val value: Int) : CEnum {
    CXIdxEntity_Unexposed(0),
    CXIdxEntity_Typedef(1),
    CXIdxEntity_Function(2),
    CXIdxEntity_Variable(3),
    CXIdxEntity_Field(4),
    CXIdxEntity_EnumConstant(5),
    CXIdxEntity_ObjCClass(6),
    CXIdxEntity_ObjCProtocol(7),
    CXIdxEntity_ObjCCategory(8),
    CXIdxEntity_ObjCInstanceMethod(9),
    CXIdxEntity_ObjCClassMethod(10),
    CXIdxEntity_ObjCProperty(11),
    CXIdxEntity_ObjCIvar(12),
    CXIdxEntity_Enum(13),
    CXIdxEntity_Struct(14),
    CXIdxEntity_Union(15),
    CXIdxEntity_CXXClass(16),
    CXIdxEntity_CXXNamespace(17),
    CXIdxEntity_CXXNamespaceAlias(18),
    CXIdxEntity_CXXStaticVariable(19),
    CXIdxEntity_CXXStaticMethod(20),
    CXIdxEntity_CXXInstanceMethod(21),
    CXIdxEntity_CXXConstructor(22),
    CXIdxEntity_CXXDestructor(23),
    CXIdxEntity_CXXConversionFunction(24),
    CXIdxEntity_CXXTypeAlias(25),
    CXIdxEntity_CXXInterface(26),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXIdxEntityKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXIdxEntityKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXNullabilityKind(override val value: Int) : CEnum {
    CXNullabilityKind_Nullable(0),
    CXNullabilityKind_NonNull(1),
    CXNullabilityKind_Unspecified(2),
    ;
    
    companion object {
        
        fun byValue(value: Int) = CXNullabilityKind.values().find { it.value == value }!!
    }
    
    class Var(rawPtr: NativePtr) : CEnumVar(rawPtr) {
        companion object : Type(IntVar.size.toInt())
        var value: CXNullabilityKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

fun clang_getCString(string: CValue<CXString>): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge0(string.getPointer(memScope).rawValue))
    }
}

fun clang_disposeString(string: CValue<CXString>): Unit {
    memScoped {
        return kniBridge1(string.getPointer(memScope).rawValue)
    }
}

fun clang_disposeStringSet(set: CValuesRef<CXStringSet>?): Unit {
    memScoped {
        return kniBridge2(set?.getPointer(memScope).rawValue)
    }
}

fun clang_getBuildSessionTimestamp(): Long {
    return kniBridge3()
}

fun clang_VirtualFileOverlay_create(options: Int): CXVirtualFileOverlay? {
    return interpretCPointer<CXVirtualFileOverlayImpl>(kniBridge4(options))
}

fun clang_VirtualFileOverlay_addFileMapping(arg0: CXVirtualFileOverlay?, virtualPath: String?, realPath: String?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge5(arg0.rawValue, virtualPath?.cstr?.getPointer(memScope).rawValue, realPath?.cstr?.getPointer(memScope).rawValue))
    }
}

fun clang_VirtualFileOverlay_setCaseSensitivity(arg0: CXVirtualFileOverlay?, caseSensitive: Int): CXErrorCode {
    return CXErrorCode.byValue(kniBridge6(arg0.rawValue, caseSensitive))
}

fun clang_VirtualFileOverlay_writeToBuffer(arg0: CXVirtualFileOverlay?, options: Int, out_buffer_ptr: CValuesRef<CPointerVar<ByteVar>>?, out_buffer_size: CValuesRef<IntVar>?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge7(arg0.rawValue, options, out_buffer_ptr?.getPointer(memScope).rawValue, out_buffer_size?.getPointer(memScope).rawValue))
    }
}

fun clang_free(buffer: CValuesRef<*>?): Unit {
    memScoped {
        return kniBridge8(buffer?.getPointer(memScope).rawValue)
    }
}

fun clang_VirtualFileOverlay_dispose(arg0: CXVirtualFileOverlay?): Unit {
    return kniBridge9(arg0.rawValue)
}

fun clang_ModuleMapDescriptor_create(options: Int): CXModuleMapDescriptor? {
    return interpretCPointer<CXModuleMapDescriptorImpl>(kniBridge10(options))
}

fun clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge11(arg0.rawValue, name?.cstr?.getPointer(memScope).rawValue))
    }
}

fun clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge12(arg0.rawValue, name?.cstr?.getPointer(memScope).rawValue))
    }
}

fun clang_ModuleMapDescriptor_writeToBuffer(arg0: CXModuleMapDescriptor?, options: Int, out_buffer_ptr: CValuesRef<CPointerVar<ByteVar>>?, out_buffer_size: CValuesRef<IntVar>?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge13(arg0.rawValue, options, out_buffer_ptr?.getPointer(memScope).rawValue, out_buffer_size?.getPointer(memScope).rawValue))
    }
}

fun clang_ModuleMapDescriptor_dispose(arg0: CXModuleMapDescriptor?): Unit {
    return kniBridge14(arg0.rawValue)
}

fun clang_createIndex(excludeDeclarationsFromPCH: Int, displayDiagnostics: Int): CXIndex? {
    return interpretCPointer<COpaque>(kniBridge15(excludeDeclarationsFromPCH, displayDiagnostics))
}

fun clang_disposeIndex(index: CXIndex?): Unit {
    return kniBridge16(index.rawValue)
}

fun clang_CXIndex_setGlobalOptions(arg0: CXIndex?, options: Int): Unit {
    return kniBridge17(arg0.rawValue, options)
}

fun clang_CXIndex_getGlobalOptions(arg0: CXIndex?): Int {
    return kniBridge18(arg0.rawValue)
}

fun clang_CXIndex_setInvocationEmissionPathOption(arg0: CXIndex?, Path: String?): Unit {
    memScoped {
        return kniBridge19(arg0.rawValue, Path?.cstr?.getPointer(memScope).rawValue)
    }
}

fun clang_getFileName(SFile: CXFile?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge20(SFile.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getFileTime(SFile: CXFile?): time_t {
    return kniBridge21(SFile.rawValue)
}

fun clang_getFileUniqueID(file: CXFile?, outID: CValuesRef<CXFileUniqueID>?): Int {
    memScoped {
        return kniBridge22(file.rawValue, outID?.getPointer(memScope).rawValue)
    }
}

fun clang_isFileMultipleIncludeGuarded(tu: CXTranslationUnit?, file: CXFile?): Int {
    return kniBridge23(tu.rawValue, file.rawValue)
}

fun clang_getFile(tu: CXTranslationUnit?, file_name: String?): CXFile? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge24(tu.rawValue, file_name?.cstr?.getPointer(memScope).rawValue))
    }
}

fun clang_getFileContents(tu: CXTranslationUnit?, file: CXFile?, size: CValuesRef<size_tVar>?): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge25(tu.rawValue, file.rawValue, size?.getPointer(memScope).rawValue))
    }
}

fun clang_File_isEqual(file1: CXFile?, file2: CXFile?): Int {
    return kniBridge26(file1.rawValue, file2.rawValue)
}

fun clang_File_tryGetRealPathName(file: CXFile?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge27(file.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getNullLocation(): CValue<CXSourceLocation> {
    val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
    try {
        kniBridge28(kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_equalLocations(loc1: CValue<CXSourceLocation>, loc2: CValue<CXSourceLocation>): Int {
    memScoped {
        return kniBridge29(loc1.getPointer(memScope).rawValue, loc2.getPointer(memScope).rawValue)
    }
}

fun clang_getLocation(tu: CXTranslationUnit?, file: CXFile?, line: Int, column: Int): CValue<CXSourceLocation> {
    val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
    try {
        kniBridge30(tu.rawValue, file.rawValue, line, column, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getLocationForOffset(tu: CXTranslationUnit?, file: CXFile?, offset: Int): CValue<CXSourceLocation> {
    val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
    try {
        kniBridge31(tu.rawValue, file.rawValue, offset, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_Location_isInSystemHeader(location: CValue<CXSourceLocation>): Int {
    memScoped {
        return kniBridge32(location.getPointer(memScope).rawValue)
    }
}

fun clang_Location_isFromMainFile(location: CValue<CXSourceLocation>): Int {
    memScoped {
        return kniBridge33(location.getPointer(memScope).rawValue)
    }
}

fun clang_getNullRange(): CValue<CXSourceRange> {
    val kniRetVal = nativeHeap.alloc<CXSourceRange>()
    try {
        kniBridge34(kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getRange(begin: CValue<CXSourceLocation>, end: CValue<CXSourceLocation>): CValue<CXSourceRange> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceRange>()
        try {
            kniBridge35(begin.getPointer(memScope).rawValue, end.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_equalRanges(range1: CValue<CXSourceRange>, range2: CValue<CXSourceRange>): Int {
    memScoped {
        return kniBridge36(range1.getPointer(memScope).rawValue, range2.getPointer(memScope).rawValue)
    }
}

fun clang_Range_isNull(range: CValue<CXSourceRange>): Int {
    memScoped {
        return kniBridge37(range.getPointer(memScope).rawValue)
    }
}

fun clang_getExpansionLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge38(location.getPointer(memScope).rawValue, file?.getPointer(memScope).rawValue, line?.getPointer(memScope).rawValue, column?.getPointer(memScope).rawValue, offset?.getPointer(memScope).rawValue)
    }
}

fun clang_getPresumedLocation(location: CValue<CXSourceLocation>, filename: CValuesRef<CXString>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge39(location.getPointer(memScope).rawValue, filename?.getPointer(memScope).rawValue, line?.getPointer(memScope).rawValue, column?.getPointer(memScope).rawValue)
    }
}

fun clang_getInstantiationLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge40(location.getPointer(memScope).rawValue, file?.getPointer(memScope).rawValue, line?.getPointer(memScope).rawValue, column?.getPointer(memScope).rawValue, offset?.getPointer(memScope).rawValue)
    }
}

fun clang_getSpellingLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge41(location.getPointer(memScope).rawValue, file?.getPointer(memScope).rawValue, line?.getPointer(memScope).rawValue, column?.getPointer(memScope).rawValue, offset?.getPointer(memScope).rawValue)
    }
}

fun clang_getFileLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge42(location.getPointer(memScope).rawValue, file?.getPointer(memScope).rawValue, line?.getPointer(memScope).rawValue, column?.getPointer(memScope).rawValue, offset?.getPointer(memScope).rawValue)
    }
}

fun clang_getRangeStart(range: CValue<CXSourceRange>): CValue<CXSourceLocation> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
        try {
            kniBridge43(range.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getRangeEnd(range: CValue<CXSourceRange>): CValue<CXSourceLocation> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
        try {
            kniBridge44(range.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getSkippedRanges(tu: CXTranslationUnit?, file: CXFile?): CPointer<CXSourceRangeList>? {
    return interpretCPointer<CXSourceRangeList>(kniBridge45(tu.rawValue, file.rawValue))
}

fun clang_getAllSkippedRanges(tu: CXTranslationUnit?): CPointer<CXSourceRangeList>? {
    return interpretCPointer<CXSourceRangeList>(kniBridge46(tu.rawValue))
}

fun clang_disposeSourceRangeList(ranges: CValuesRef<CXSourceRangeList>?): Unit {
    memScoped {
        return kniBridge47(ranges?.getPointer(memScope).rawValue)
    }
}

fun clang_getNumDiagnosticsInSet(Diags: CXDiagnosticSet?): Int {
    return kniBridge48(Diags.rawValue)
}

fun clang_getDiagnosticInSet(Diags: CXDiagnosticSet?, Index: Int): CXDiagnostic? {
    return interpretCPointer<COpaque>(kniBridge49(Diags.rawValue, Index))
}

fun clang_loadDiagnostics(file: String?, error: CValuesRef<CXLoadDiag_Error.Var>?, errorString: CValuesRef<CXString>?): CXDiagnosticSet? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge50(file?.cstr?.getPointer(memScope).rawValue, error?.getPointer(memScope).rawValue, errorString?.getPointer(memScope).rawValue))
    }
}

fun clang_disposeDiagnosticSet(Diags: CXDiagnosticSet?): Unit {
    return kniBridge51(Diags.rawValue)
}

fun clang_getChildDiagnostics(D: CXDiagnostic?): CXDiagnosticSet? {
    return interpretCPointer<COpaque>(kniBridge52(D.rawValue))
}

fun clang_getNumDiagnostics(Unit: CXTranslationUnit?): Int {
    return kniBridge53(Unit.rawValue)
}

fun clang_getDiagnostic(Unit: CXTranslationUnit?, Index: Int): CXDiagnostic? {
    return interpretCPointer<COpaque>(kniBridge54(Unit.rawValue, Index))
}

fun clang_getDiagnosticSetFromTU(Unit: CXTranslationUnit?): CXDiagnosticSet? {
    return interpretCPointer<COpaque>(kniBridge55(Unit.rawValue))
}

fun clang_disposeDiagnostic(Diagnostic: CXDiagnostic?): Unit {
    return kniBridge56(Diagnostic.rawValue)
}

fun clang_formatDiagnostic(Diagnostic: CXDiagnostic?, Options: Int): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge57(Diagnostic.rawValue, Options, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_defaultDiagnosticDisplayOptions(): Int {
    return kniBridge58()
}

fun clang_getDiagnosticSeverity(arg0: CXDiagnostic?): CXDiagnosticSeverity {
    return CXDiagnosticSeverity.byValue(kniBridge59(arg0.rawValue))
}

fun clang_getDiagnosticLocation(arg0: CXDiagnostic?): CValue<CXSourceLocation> {
    val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
    try {
        kniBridge60(arg0.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getDiagnosticSpelling(arg0: CXDiagnostic?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge61(arg0.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getDiagnosticOption(Diag: CXDiagnostic?, Disable: CValuesRef<CXString>?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge62(Diag.rawValue, Disable?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getDiagnosticCategory(arg0: CXDiagnostic?): Int {
    return kniBridge63(arg0.rawValue)
}

fun clang_getDiagnosticCategoryName(Category: Int): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge64(Category, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getDiagnosticCategoryText(arg0: CXDiagnostic?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge65(arg0.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getDiagnosticNumRanges(arg0: CXDiagnostic?): Int {
    return kniBridge66(arg0.rawValue)
}

fun clang_getDiagnosticRange(Diagnostic: CXDiagnostic?, Range: Int): CValue<CXSourceRange> {
    val kniRetVal = nativeHeap.alloc<CXSourceRange>()
    try {
        kniBridge67(Diagnostic.rawValue, Range, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getDiagnosticNumFixIts(Diagnostic: CXDiagnostic?): Int {
    return kniBridge68(Diagnostic.rawValue)
}

fun clang_getDiagnosticFixIt(Diagnostic: CXDiagnostic?, FixIt: Int, ReplacementRange: CValuesRef<CXSourceRange>?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge69(Diagnostic.rawValue, FixIt, ReplacementRange?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getTranslationUnitSpelling(CTUnit: CXTranslationUnit?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge70(CTUnit.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_createTranslationUnitFromSourceFile(CIdx: CXIndex?, source_filename: String?, num_clang_command_line_args: Int, clang_command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_unsaved_files: Int, unsaved_files: CValuesRef<CXUnsavedFile>?): CXTranslationUnit? {
    memScoped {
        return interpretCPointer<CXTranslationUnitImpl>(kniBridge71(CIdx.rawValue, source_filename?.cstr?.getPointer(memScope).rawValue, num_clang_command_line_args, clang_command_line_args?.getPointer(memScope).rawValue, num_unsaved_files, unsaved_files?.getPointer(memScope).rawValue))
    }
}

fun clang_createTranslationUnit(CIdx: CXIndex?, ast_filename: String?): CXTranslationUnit? {
    memScoped {
        return interpretCPointer<CXTranslationUnitImpl>(kniBridge72(CIdx.rawValue, ast_filename?.cstr?.getPointer(memScope).rawValue))
    }
}

fun clang_createTranslationUnit2(CIdx: CXIndex?, ast_filename: String?, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge73(CIdx.rawValue, ast_filename?.cstr?.getPointer(memScope).rawValue, out_TU?.getPointer(memScope).rawValue))
    }
}

fun clang_defaultEditingTranslationUnitOptions(): Int {
    return kniBridge74()
}

fun clang_parseTranslationUnit(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CXTranslationUnit? {
    memScoped {
        return interpretCPointer<CXTranslationUnitImpl>(kniBridge75(CIdx.rawValue, source_filename?.cstr?.getPointer(memScope).rawValue, command_line_args?.getPointer(memScope).rawValue, num_command_line_args, unsaved_files?.getPointer(memScope).rawValue, num_unsaved_files, options))
    }
}

fun clang_parseTranslationUnit2(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge76(CIdx.rawValue, source_filename?.cstr?.getPointer(memScope).rawValue, command_line_args?.getPointer(memScope).rawValue, num_command_line_args, unsaved_files?.getPointer(memScope).rawValue, num_unsaved_files, options, out_TU?.getPointer(memScope).rawValue))
    }
}

fun clang_parseTranslationUnit2FullArgv(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    memScoped {
        return CXErrorCode.byValue(kniBridge77(CIdx.rawValue, source_filename?.cstr?.getPointer(memScope).rawValue, command_line_args?.getPointer(memScope).rawValue, num_command_line_args, unsaved_files?.getPointer(memScope).rawValue, num_unsaved_files, options, out_TU?.getPointer(memScope).rawValue))
    }
}

fun clang_defaultSaveOptions(TU: CXTranslationUnit?): Int {
    return kniBridge78(TU.rawValue)
}

fun clang_saveTranslationUnit(TU: CXTranslationUnit?, FileName: String?, options: Int): Int {
    memScoped {
        return kniBridge79(TU.rawValue, FileName?.cstr?.getPointer(memScope).rawValue, options)
    }
}

fun clang_suspendTranslationUnit(arg0: CXTranslationUnit?): Int {
    return kniBridge80(arg0.rawValue)
}

fun clang_disposeTranslationUnit(arg0: CXTranslationUnit?): Unit {
    return kniBridge81(arg0.rawValue)
}

fun clang_defaultReparseOptions(TU: CXTranslationUnit?): Int {
    return kniBridge82(TU.rawValue)
}

fun clang_reparseTranslationUnit(TU: CXTranslationUnit?, num_unsaved_files: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, options: Int): Int {
    memScoped {
        return kniBridge83(TU.rawValue, num_unsaved_files, unsaved_files?.getPointer(memScope).rawValue, options)
    }
}

fun clang_getTUResourceUsageName(kind: CXTUResourceUsageKind): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge84(kind.value))
}

fun clang_getCXTUResourceUsage(TU: CXTranslationUnit?): CValue<CXTUResourceUsage> {
    val kniRetVal = nativeHeap.alloc<CXTUResourceUsage>()
    try {
        kniBridge85(TU.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_disposeCXTUResourceUsage(usage: CValue<CXTUResourceUsage>): Unit {
    memScoped {
        return kniBridge86(usage.getPointer(memScope).rawValue)
    }
}

fun clang_getTranslationUnitTargetInfo(CTUnit: CXTranslationUnit?): CXTargetInfo? {
    return interpretCPointer<CXTargetInfoImpl>(kniBridge87(CTUnit.rawValue))
}

fun clang_TargetInfo_dispose(Info: CXTargetInfo?): Unit {
    return kniBridge88(Info.rawValue)
}

fun clang_TargetInfo_getTriple(Info: CXTargetInfo?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge89(Info.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_TargetInfo_getPointerWidth(Info: CXTargetInfo?): Int {
    return kniBridge90(Info.rawValue)
}

fun clang_getNullCursor(): CValue<CXCursor> {
    val kniRetVal = nativeHeap.alloc<CXCursor>()
    try {
        kniBridge91(kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getTranslationUnitCursor(arg0: CXTranslationUnit?): CValue<CXCursor> {
    val kniRetVal = nativeHeap.alloc<CXCursor>()
    try {
        kniBridge92(arg0.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_equalCursors(arg0: CValue<CXCursor>, arg1: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge93(arg0.getPointer(memScope).rawValue, arg1.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isNull(cursor: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge94(cursor.getPointer(memScope).rawValue)
    }
}

fun clang_hashCursor(arg0: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge95(arg0.getPointer(memScope).rawValue)
    }
}

fun clang_getCursorKind(arg0: CValue<CXCursor>): CXCursorKind {
    memScoped {
        return CXCursorKind.byValue(kniBridge96(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_isDeclaration(arg0: CXCursorKind): Int {
    return kniBridge97(arg0.value)
}

fun clang_isInvalidDeclaration(arg0: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge98(arg0.getPointer(memScope).rawValue)
    }
}

fun clang_isReference(arg0: CXCursorKind): Int {
    return kniBridge99(arg0.value)
}

fun clang_isExpression(arg0: CXCursorKind): Int {
    return kniBridge100(arg0.value)
}

fun clang_isStatement(arg0: CXCursorKind): Int {
    return kniBridge101(arg0.value)
}

fun clang_isAttribute(arg0: CXCursorKind): Int {
    return kniBridge102(arg0.value)
}

fun clang_Cursor_hasAttrs(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge103(C.getPointer(memScope).rawValue)
    }
}

fun clang_isInvalid(arg0: CXCursorKind): Int {
    return kniBridge104(arg0.value)
}

fun clang_isTranslationUnit(arg0: CXCursorKind): Int {
    return kniBridge105(arg0.value)
}

fun clang_isPreprocessing(arg0: CXCursorKind): Int {
    return kniBridge106(arg0.value)
}

fun clang_isUnexposed(arg0: CXCursorKind): Int {
    return kniBridge107(arg0.value)
}

fun clang_getCursorLinkage(cursor: CValue<CXCursor>): CXLinkageKind {
    memScoped {
        return CXLinkageKind.byValue(kniBridge108(cursor.getPointer(memScope).rawValue))
    }
}

fun clang_getCursorVisibility(cursor: CValue<CXCursor>): CXVisibilityKind {
    memScoped {
        return CXVisibilityKind.byValue(kniBridge109(cursor.getPointer(memScope).rawValue))
    }
}

fun clang_getCursorAvailability(cursor: CValue<CXCursor>): CXAvailabilityKind {
    memScoped {
        return CXAvailabilityKind.byValue(kniBridge110(cursor.getPointer(memScope).rawValue))
    }
}

fun clang_getCursorPlatformAvailability(cursor: CValue<CXCursor>, always_deprecated: CValuesRef<IntVar>?, deprecated_message: CValuesRef<CXString>?, always_unavailable: CValuesRef<IntVar>?, unavailable_message: CValuesRef<CXString>?, availability: CValuesRef<CXPlatformAvailability>?, availability_size: Int): Int {
    memScoped {
        return kniBridge111(cursor.getPointer(memScope).rawValue, always_deprecated?.getPointer(memScope).rawValue, deprecated_message?.getPointer(memScope).rawValue, always_unavailable?.getPointer(memScope).rawValue, unavailable_message?.getPointer(memScope).rawValue, availability?.getPointer(memScope).rawValue, availability_size)
    }
}

fun clang_disposeCXPlatformAvailability(availability: CValuesRef<CXPlatformAvailability>?): Unit {
    memScoped {
        return kniBridge112(availability?.getPointer(memScope).rawValue)
    }
}

fun clang_getCursorLanguage(cursor: CValue<CXCursor>): CXLanguageKind {
    memScoped {
        return CXLanguageKind.byValue(kniBridge113(cursor.getPointer(memScope).rawValue))
    }
}

fun clang_getCursorTLSKind(cursor: CValue<CXCursor>): CXTLSKind {
    memScoped {
        return kniBridge114(cursor.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_getTranslationUnit(arg0: CValue<CXCursor>): CXTranslationUnit? {
    memScoped {
        return interpretCPointer<CXTranslationUnitImpl>(kniBridge115(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_createCXCursorSet(): CXCursorSet? {
    return interpretCPointer<CXCursorSetImpl>(kniBridge116())
}

fun clang_disposeCXCursorSet(cset: CXCursorSet?): Unit {
    return kniBridge117(cset.rawValue)
}

fun clang_CXCursorSet_contains(cset: CXCursorSet?, cursor: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge118(cset.rawValue, cursor.getPointer(memScope).rawValue)
    }
}

fun clang_CXCursorSet_insert(cset: CXCursorSet?, cursor: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge119(cset.rawValue, cursor.getPointer(memScope).rawValue)
    }
}

fun clang_getCursorSemanticParent(cursor: CValue<CXCursor>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge120(cursor.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorLexicalParent(cursor: CValue<CXCursor>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge121(cursor.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getOverriddenCursors(cursor: CValue<CXCursor>, overridden: CValuesRef<CPointerVar<CXCursor>>?, num_overridden: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge122(cursor.getPointer(memScope).rawValue, overridden?.getPointer(memScope).rawValue, num_overridden?.getPointer(memScope).rawValue)
    }
}

fun clang_disposeOverriddenCursors(overridden: CValuesRef<CXCursor>?): Unit {
    memScoped {
        return kniBridge123(overridden?.getPointer(memScope).rawValue)
    }
}

fun clang_getIncludedFile(cursor: CValue<CXCursor>): CXFile? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge124(cursor.getPointer(memScope).rawValue))
    }
}

fun clang_getCursor(arg0: CXTranslationUnit?, arg1: CValue<CXSourceLocation>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge125(arg0.rawValue, arg1.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorLocation(arg0: CValue<CXCursor>): CValue<CXSourceLocation> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
        try {
            kniBridge126(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorExtent(arg0: CValue<CXCursor>): CValue<CXSourceRange> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceRange>()
        try {
            kniBridge127(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorType(C: CValue<CXCursor>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge128(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getTypeSpelling(CT: CValue<CXType>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge129(CT.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getTypedefDeclUnderlyingType(C: CValue<CXCursor>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge130(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getEnumDeclIntegerType(C: CValue<CXCursor>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge131(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getEnumConstantDeclValue(C: CValue<CXCursor>): Long {
    memScoped {
        return kniBridge132(C.getPointer(memScope).rawValue)
    }
}

fun clang_getEnumConstantDeclUnsignedValue(C: CValue<CXCursor>): Long {
    memScoped {
        return kniBridge133(C.getPointer(memScope).rawValue)
    }
}

fun clang_getFieldDeclBitWidth(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge134(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_getNumArguments(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge135(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_getArgument(C: CValue<CXCursor>, i: Int): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge136(C.getPointer(memScope).rawValue, i, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getNumTemplateArguments(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge137(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_getTemplateArgumentKind(C: CValue<CXCursor>, I: Int): CXTemplateArgumentKind {
    memScoped {
        return CXTemplateArgumentKind.byValue(kniBridge138(C.getPointer(memScope).rawValue, I))
    }
}

fun clang_Cursor_getTemplateArgumentType(C: CValue<CXCursor>, I: Int): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge139(C.getPointer(memScope).rawValue, I, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getTemplateArgumentValue(C: CValue<CXCursor>, I: Int): Long {
    memScoped {
        return kniBridge140(C.getPointer(memScope).rawValue, I)
    }
}

fun clang_Cursor_getTemplateArgumentUnsignedValue(C: CValue<CXCursor>, I: Int): Long {
    memScoped {
        return kniBridge141(C.getPointer(memScope).rawValue, I)
    }
}

fun clang_equalTypes(A: CValue<CXType>, B: CValue<CXType>): Int {
    memScoped {
        return kniBridge142(A.getPointer(memScope).rawValue, B.getPointer(memScope).rawValue)
    }
}

fun clang_getCanonicalType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge143(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_isConstQualifiedType(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge144(T.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isMacroFunctionLike(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge145(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isMacroBuiltin(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge146(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isFunctionInlined(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge147(C.getPointer(memScope).rawValue)
    }
}

fun clang_isVolatileQualifiedType(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge148(T.getPointer(memScope).rawValue)
    }
}

fun clang_isRestrictQualifiedType(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge149(T.getPointer(memScope).rawValue)
    }
}

fun clang_getAddressSpace(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge150(T.getPointer(memScope).rawValue)
    }
}

fun clang_getTypedefName(CT: CValue<CXType>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge151(CT.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getPointeeType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge152(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getTypeDeclaration(T: CValue<CXType>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge153(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getDeclObjCTypeEncoding(C: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge154(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_getObjCEncoding(type: CValue<CXType>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge155(type.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getTypeKindSpelling(K: CXTypeKind): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge156(K.value, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getFunctionTypeCallingConv(T: CValue<CXType>): CXCallingConv {
    memScoped {
        return CXCallingConv.byValue(kniBridge157(T.getPointer(memScope).rawValue))
    }
}

fun clang_getResultType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge158(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getExceptionSpecificationType(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge159(T.getPointer(memScope).rawValue)
    }
}

fun clang_getNumArgTypes(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge160(T.getPointer(memScope).rawValue)
    }
}

fun clang_getArgType(T: CValue<CXType>, i: Int): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge161(T.getPointer(memScope).rawValue, i, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_getObjCObjectBaseType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge162(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_getNumObjCProtocolRefs(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge163(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getObjCProtocolDecl(T: CValue<CXType>, i: Int): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge164(T.getPointer(memScope).rawValue, i, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_getNumObjCTypeArgs(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge165(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getObjCTypeArg(T: CValue<CXType>, i: Int): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge166(T.getPointer(memScope).rawValue, i, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_isFunctionTypeVariadic(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge167(T.getPointer(memScope).rawValue)
    }
}

fun clang_getCursorResultType(C: CValue<CXCursor>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge168(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorExceptionSpecificationType(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge169(C.getPointer(memScope).rawValue)
    }
}

fun clang_isPODType(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge170(T.getPointer(memScope).rawValue)
    }
}

fun clang_getElementType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge171(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getNumElements(T: CValue<CXType>): Long {
    memScoped {
        return kniBridge172(T.getPointer(memScope).rawValue)
    }
}

fun clang_getArrayElementType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge173(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getArraySize(T: CValue<CXType>): Long {
    memScoped {
        return kniBridge174(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getNamedType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge175(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_isTransparentTagTypedef(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge176(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getNullability(T: CValue<CXType>): CXTypeNullabilityKind {
    memScoped {
        return kniBridge177(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getAlignOf(T: CValue<CXType>): Long {
    memScoped {
        return kniBridge178(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getClassType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge179(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_getSizeOf(T: CValue<CXType>): Long {
    memScoped {
        return kniBridge180(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getOffsetOf(T: CValue<CXType>, S: String?): Long {
    memScoped {
        return kniBridge181(T.getPointer(memScope).rawValue, S?.cstr?.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getModifiedType(T: CValue<CXType>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge182(T.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getOffsetOfField(C: CValue<CXCursor>): Long {
    memScoped {
        return kniBridge183(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isAnonymous(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge184(C.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getNumTemplateArguments(T: CValue<CXType>): Int {
    memScoped {
        return kniBridge185(T.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getTemplateArgumentAsType(T: CValue<CXType>, i: Int): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge186(T.getPointer(memScope).rawValue, i, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_getCXXRefQualifier(T: CValue<CXType>): CXRefQualifierKind {
    memScoped {
        return kniBridge187(T.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isBitField(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge188(C.getPointer(memScope).rawValue)
    }
}

fun clang_isVirtualBase(arg0: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge189(arg0.getPointer(memScope).rawValue)
    }
}

fun clang_getCXXAccessSpecifier(arg0: CValue<CXCursor>): CX_CXXAccessSpecifier {
    memScoped {
        return CX_CXXAccessSpecifier.byValue(kniBridge190(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_Cursor_getStorageClass(arg0: CValue<CXCursor>): CX_StorageClass {
    memScoped {
        return CX_StorageClass.byValue(kniBridge191(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_getNumOverloadedDecls(cursor: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge192(cursor.getPointer(memScope).rawValue)
    }
}

fun clang_getOverloadedDecl(cursor: CValue<CXCursor>, index: Int): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge193(cursor.getPointer(memScope).rawValue, index, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getIBOutletCollectionType(arg0: CValue<CXCursor>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge194(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_visitChildren(parent: CValue<CXCursor>, visitor: CXCursorVisitor?, client_data: CXClientData?): Int {
    memScoped {
        return kniBridge195(parent.getPointer(memScope).rawValue, visitor.rawValue, client_data.rawValue)
    }
}

fun clang_getCursorUSR(arg0: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge196(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_constructUSR_ObjCClass(class_name: String?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge197(class_name?.cstr?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_constructUSR_ObjCCategory(class_name: String?, category_name: String?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge198(class_name?.cstr?.getPointer(memScope).rawValue, category_name?.cstr?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_constructUSR_ObjCProtocol(protocol_name: String?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge199(protocol_name?.cstr?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_constructUSR_ObjCIvar(name: String?, classUSR: CValue<CXString>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge200(name?.cstr?.getPointer(memScope).rawValue, classUSR.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_constructUSR_ObjCMethod(name: String?, isInstanceMethod: Int, classUSR: CValue<CXString>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge201(name?.cstr?.getPointer(memScope).rawValue, isInstanceMethod, classUSR.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_constructUSR_ObjCProperty(property: String?, classUSR: CValue<CXString>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge202(property?.cstr?.getPointer(memScope).rawValue, classUSR.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorSpelling(arg0: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge203(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getSpellingNameRange(arg0: CValue<CXCursor>, pieceIndex: Int, options: Int): CValue<CXSourceRange> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceRange>()
        try {
            kniBridge204(arg0.getPointer(memScope).rawValue, pieceIndex, options, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_PrintingPolicy_getProperty(Policy: CXPrintingPolicy?, Property: CXPrintingPolicyProperty): Int {
    return kniBridge205(Policy.rawValue, Property)
}

fun clang_PrintingPolicy_setProperty(Policy: CXPrintingPolicy?, Property: CXPrintingPolicyProperty, Value: Int): Unit {
    return kniBridge206(Policy.rawValue, Property, Value)
}

fun clang_getCursorPrintingPolicy(arg0: CValue<CXCursor>): CXPrintingPolicy? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge207(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_PrintingPolicy_dispose(Policy: CXPrintingPolicy?): Unit {
    return kniBridge208(Policy.rawValue)
}

fun clang_getCursorPrettyPrinted(Cursor: CValue<CXCursor>, Policy: CXPrintingPolicy?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge209(Cursor.getPointer(memScope).rawValue, Policy.rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorDisplayName(arg0: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge210(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorReferenced(arg0: CValue<CXCursor>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge211(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorDefinition(arg0: CValue<CXCursor>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge212(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_isCursorDefinition(arg0: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge213(arg0.getPointer(memScope).rawValue)
    }
}

fun clang_getCanonicalCursor(arg0: CValue<CXCursor>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge214(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getObjCSelectorIndex(arg0: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge215(arg0.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isDynamicCall(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge216(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_getReceiverType(C: CValue<CXCursor>): CValue<CXType> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXType>()
        try {
            kniBridge217(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getObjCPropertyAttributes(C: CValue<CXCursor>, reserved: Int): Int {
    memScoped {
        return kniBridge218(C.getPointer(memScope).rawValue, reserved)
    }
}

fun clang_Cursor_getObjCPropertyGetterName(C: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge219(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getObjCPropertySetterName(C: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge220(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getObjCDeclQualifiers(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge221(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isObjCOptional(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge222(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isVariadic(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge223(C.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isExternalSymbol(C: CValue<CXCursor>, language: CValuesRef<CXString>?, definedIn: CValuesRef<CXString>?, isGenerated: CValuesRef<IntVar>?): Int {
    memScoped {
        return kniBridge224(C.getPointer(memScope).rawValue, language?.getPointer(memScope).rawValue, definedIn?.getPointer(memScope).rawValue, isGenerated?.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_getCommentRange(C: CValue<CXCursor>): CValue<CXSourceRange> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceRange>()
        try {
            kniBridge225(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getRawCommentText(C: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge226(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getBriefCommentText(C: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge227(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getMangling(arg0: CValue<CXCursor>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge228(arg0.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_getCXXManglings(arg0: CValue<CXCursor>): CPointer<CXStringSet>? {
    memScoped {
        return interpretCPointer<CXStringSet>(kniBridge229(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_Cursor_getObjCManglings(arg0: CValue<CXCursor>): CPointer<CXStringSet>? {
    memScoped {
        return interpretCPointer<CXStringSet>(kniBridge230(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_Cursor_getModule(C: CValue<CXCursor>): CXModule? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge231(C.getPointer(memScope).rawValue))
    }
}

fun clang_getModuleForFile(arg0: CXTranslationUnit?, arg1: CXFile?): CXModule? {
    return interpretCPointer<COpaque>(kniBridge232(arg0.rawValue, arg1.rawValue))
}

fun clang_Module_getASTFile(Module: CXModule?): CXFile? {
    return interpretCPointer<COpaque>(kniBridge233(Module.rawValue))
}

fun clang_Module_getParent(Module: CXModule?): CXModule? {
    return interpretCPointer<COpaque>(kniBridge234(Module.rawValue))
}

fun clang_Module_getName(Module: CXModule?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge235(Module.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_Module_getFullName(Module: CXModule?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge236(Module.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_Module_isSystem(Module: CXModule?): Int {
    return kniBridge237(Module.rawValue)
}

fun clang_Module_getNumTopLevelHeaders(arg0: CXTranslationUnit?, Module: CXModule?): Int {
    return kniBridge238(arg0.rawValue, Module.rawValue)
}

fun clang_Module_getTopLevelHeader(arg0: CXTranslationUnit?, Module: CXModule?, Index: Int): CXFile? {
    return interpretCPointer<COpaque>(kniBridge239(arg0.rawValue, Module.rawValue, Index))
}

fun clang_CXXConstructor_isConvertingConstructor(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge240(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXConstructor_isCopyConstructor(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge241(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXConstructor_isDefaultConstructor(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge242(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXConstructor_isMoveConstructor(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge243(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXField_isMutable(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge244(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXMethod_isDefaulted(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge245(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXMethod_isPureVirtual(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge246(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXMethod_isStatic(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge247(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXMethod_isVirtual(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge248(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXRecord_isAbstract(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge249(C.getPointer(memScope).rawValue)
    }
}

fun clang_EnumDecl_isScoped(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge250(C.getPointer(memScope).rawValue)
    }
}

fun clang_CXXMethod_isConst(C: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge251(C.getPointer(memScope).rawValue)
    }
}

fun clang_getTemplateCursorKind(C: CValue<CXCursor>): CXCursorKind {
    memScoped {
        return CXCursorKind.byValue(kniBridge252(C.getPointer(memScope).rawValue))
    }
}

fun clang_getSpecializedCursorTemplate(C: CValue<CXCursor>): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge253(C.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorReferenceNameRange(C: CValue<CXCursor>, NameFlags: Int, PieceIndex: Int): CValue<CXSourceRange> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceRange>()
        try {
            kniBridge254(C.getPointer(memScope).rawValue, NameFlags, PieceIndex, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getToken(TU: CXTranslationUnit?, Location: CValue<CXSourceLocation>): CPointer<CXToken>? {
    memScoped {
        return interpretCPointer<CXToken>(kniBridge255(TU.rawValue, Location.getPointer(memScope).rawValue))
    }
}

fun clang_getTokenKind(arg0: CValue<CXToken>): CXTokenKind {
    memScoped {
        return CXTokenKind.byValue(kniBridge256(arg0.getPointer(memScope).rawValue))
    }
}

fun clang_getTokenSpelling(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge257(arg0.rawValue, arg1.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getTokenLocation(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXSourceLocation> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
        try {
            kniBridge258(arg0.rawValue, arg1.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getTokenExtent(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXSourceRange> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceRange>()
        try {
            kniBridge259(arg0.rawValue, arg1.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_tokenize(TU: CXTranslationUnit?, Range: CValue<CXSourceRange>, Tokens: CValuesRef<CPointerVar<CXToken>>?, NumTokens: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge260(TU.rawValue, Range.getPointer(memScope).rawValue, Tokens?.getPointer(memScope).rawValue, NumTokens?.getPointer(memScope).rawValue)
    }
}

fun clang_annotateTokens(TU: CXTranslationUnit?, Tokens: CValuesRef<CXToken>?, NumTokens: Int, Cursors: CValuesRef<CXCursor>?): Unit {
    memScoped {
        return kniBridge261(TU.rawValue, Tokens?.getPointer(memScope).rawValue, NumTokens, Cursors?.getPointer(memScope).rawValue)
    }
}

fun clang_disposeTokens(TU: CXTranslationUnit?, Tokens: CValuesRef<CXToken>?, NumTokens: Int): Unit {
    memScoped {
        return kniBridge262(TU.rawValue, Tokens?.getPointer(memScope).rawValue, NumTokens)
    }
}

fun clang_getCursorKindSpelling(Kind: CXCursorKind): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge263(Kind.value, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getDefinitionSpellingAndExtent(arg0: CValue<CXCursor>, startBuf: CValuesRef<CPointerVar<ByteVar>>?, endBuf: CValuesRef<CPointerVar<ByteVar>>?, startLine: CValuesRef<IntVar>?, startColumn: CValuesRef<IntVar>?, endLine: CValuesRef<IntVar>?, endColumn: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge264(arg0.getPointer(memScope).rawValue, startBuf?.getPointer(memScope).rawValue, endBuf?.getPointer(memScope).rawValue, startLine?.getPointer(memScope).rawValue, startColumn?.getPointer(memScope).rawValue, endLine?.getPointer(memScope).rawValue, endColumn?.getPointer(memScope).rawValue)
    }
}

fun clang_enableStackTraces(): Unit {
    return kniBridge265()
}

fun clang_executeOnThread(fn: CPointer<CFunction<(COpaquePointer?) -> Unit>>?, user_data: CValuesRef<*>?, stack_size: Int): Unit {
    memScoped {
        return kniBridge266(fn.rawValue, user_data?.getPointer(memScope).rawValue, stack_size)
    }
}

fun clang_getCompletionChunkKind(completion_string: CXCompletionString?, chunk_number: Int): CXCompletionChunkKind {
    return CXCompletionChunkKind.byValue(kniBridge267(completion_string.rawValue, chunk_number))
}

fun clang_getCompletionChunkText(completion_string: CXCompletionString?, chunk_number: Int): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge268(completion_string.rawValue, chunk_number, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getCompletionChunkCompletionString(completion_string: CXCompletionString?, chunk_number: Int): CXCompletionString? {
    return interpretCPointer<COpaque>(kniBridge269(completion_string.rawValue, chunk_number))
}

fun clang_getNumCompletionChunks(completion_string: CXCompletionString?): Int {
    return kniBridge270(completion_string.rawValue)
}

fun clang_getCompletionPriority(completion_string: CXCompletionString?): Int {
    return kniBridge271(completion_string.rawValue)
}

fun clang_getCompletionAvailability(completion_string: CXCompletionString?): CXAvailabilityKind {
    return CXAvailabilityKind.byValue(kniBridge272(completion_string.rawValue))
}

fun clang_getCompletionNumAnnotations(completion_string: CXCompletionString?): Int {
    return kniBridge273(completion_string.rawValue)
}

fun clang_getCompletionAnnotation(completion_string: CXCompletionString?, annotation_number: Int): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge274(completion_string.rawValue, annotation_number, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getCompletionParent(completion_string: CXCompletionString?, kind: CValuesRef<CXCursorKind.Var>?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge275(completion_string.rawValue, kind?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCompletionBriefComment(completion_string: CXCompletionString?): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge276(completion_string.rawValue, kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_getCursorCompletionString(cursor: CValue<CXCursor>): CXCompletionString? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge277(cursor.getPointer(memScope).rawValue))
    }
}

fun clang_getCompletionNumFixIts(results: CValuesRef<CXCodeCompleteResults>?, completion_index: Int): Int {
    memScoped {
        return kniBridge278(results?.getPointer(memScope).rawValue, completion_index)
    }
}

fun clang_getCompletionFixIt(results: CValuesRef<CXCodeCompleteResults>?, completion_index: Int, fixit_index: Int, replacement_range: CValuesRef<CXSourceRange>?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge279(results?.getPointer(memScope).rawValue, completion_index, fixit_index, replacement_range?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_defaultCodeCompleteOptions(): Int {
    return kniBridge280()
}

fun clang_codeCompleteAt(TU: CXTranslationUnit?, complete_filename: String?, complete_line: Int, complete_column: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CPointer<CXCodeCompleteResults>? {
    memScoped {
        return interpretCPointer<CXCodeCompleteResults>(kniBridge281(TU.rawValue, complete_filename?.cstr?.getPointer(memScope).rawValue, complete_line, complete_column, unsaved_files?.getPointer(memScope).rawValue, num_unsaved_files, options))
    }
}

fun clang_sortCodeCompletionResults(Results: CValuesRef<CXCompletionResult>?, NumResults: Int): Unit {
    memScoped {
        return kniBridge282(Results?.getPointer(memScope).rawValue, NumResults)
    }
}

fun clang_disposeCodeCompleteResults(Results: CValuesRef<CXCodeCompleteResults>?): Unit {
    memScoped {
        return kniBridge283(Results?.getPointer(memScope).rawValue)
    }
}

fun clang_codeCompleteGetNumDiagnostics(Results: CValuesRef<CXCodeCompleteResults>?): Int {
    memScoped {
        return kniBridge284(Results?.getPointer(memScope).rawValue)
    }
}

fun clang_codeCompleteGetDiagnostic(Results: CValuesRef<CXCodeCompleteResults>?, Index: Int): CXDiagnostic? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge285(Results?.getPointer(memScope).rawValue, Index))
    }
}

fun clang_codeCompleteGetContexts(Results: CValuesRef<CXCodeCompleteResults>?): Long {
    memScoped {
        return kniBridge286(Results?.getPointer(memScope).rawValue)
    }
}

fun clang_codeCompleteGetContainerKind(Results: CValuesRef<CXCodeCompleteResults>?, IsIncomplete: CValuesRef<IntVar>?): CXCursorKind {
    memScoped {
        return CXCursorKind.byValue(kniBridge287(Results?.getPointer(memScope).rawValue, IsIncomplete?.getPointer(memScope).rawValue))
    }
}

fun clang_codeCompleteGetContainerUSR(Results: CValuesRef<CXCodeCompleteResults>?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge288(Results?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_codeCompleteGetObjCSelector(Results: CValuesRef<CXCodeCompleteResults>?): CValue<CXString> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXString>()
        try {
            kniBridge289(Results?.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getClangVersion(): CValue<CXString> {
    val kniRetVal = nativeHeap.alloc<CXString>()
    try {
        kniBridge290(kniRetVal.rawPtr)
        return kniRetVal.readValue()
    } finally { nativeHeap.free(kniRetVal) }
}

fun clang_toggleCrashRecovery(isEnabled: Int): Unit {
    return kniBridge291(isEnabled)
}

fun clang_getInclusions(tu: CXTranslationUnit?, visitor: CXInclusionVisitor?, client_data: CXClientData?): Unit {
    return kniBridge292(tu.rawValue, visitor.rawValue, client_data.rawValue)
}

fun clang_Cursor_Evaluate(C: CValue<CXCursor>): CXEvalResult? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge293(C.getPointer(memScope).rawValue))
    }
}

fun clang_EvalResult_getKind(E: CXEvalResult?): CXEvalResultKind {
    return CXEvalResultKind.byValue(kniBridge294(E.rawValue))
}

fun clang_EvalResult_getAsInt(E: CXEvalResult?): Int {
    return kniBridge295(E.rawValue)
}

fun clang_EvalResult_getAsLongLong(E: CXEvalResult?): Long {
    return kniBridge296(E.rawValue)
}

fun clang_EvalResult_isUnsignedInt(E: CXEvalResult?): Int {
    return kniBridge297(E.rawValue)
}

fun clang_EvalResult_getAsUnsigned(E: CXEvalResult?): Long {
    return kniBridge298(E.rawValue)
}

fun clang_EvalResult_getAsDouble(E: CXEvalResult?): Double {
    return kniBridge299(E.rawValue)
}

fun clang_EvalResult_getAsStr(E: CXEvalResult?): CPointer<ByteVar>? {
    return interpretCPointer<ByteVar>(kniBridge300(E.rawValue))
}

fun clang_EvalResult_dispose(E: CXEvalResult?): Unit {
    return kniBridge301(E.rawValue)
}

fun clang_getRemappings(path: String?): CXRemapping? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge302(path?.cstr?.getPointer(memScope).rawValue))
    }
}

fun clang_getRemappingsFromFileList(filePaths: CValuesRef<CPointerVar<ByteVar>>?, numFiles: Int): CXRemapping? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge303(filePaths?.getPointer(memScope).rawValue, numFiles))
    }
}

fun clang_remap_getNumFiles(arg0: CXRemapping?): Int {
    return kniBridge304(arg0.rawValue)
}

fun clang_remap_getFilenames(arg0: CXRemapping?, index: Int, original: CValuesRef<CXString>?, transformed: CValuesRef<CXString>?): Unit {
    memScoped {
        return kniBridge305(arg0.rawValue, index, original?.getPointer(memScope).rawValue, transformed?.getPointer(memScope).rawValue)
    }
}

fun clang_remap_dispose(arg0: CXRemapping?): Unit {
    return kniBridge306(arg0.rawValue)
}

fun clang_findReferencesInFile(cursor: CValue<CXCursor>, file: CXFile?, visitor: CValue<CXCursorAndRangeVisitor>): CXResult {
    memScoped {
        return CXResult.byValue(kniBridge307(cursor.getPointer(memScope).rawValue, file.rawValue, visitor.getPointer(memScope).rawValue))
    }
}

fun clang_findIncludesInFile(TU: CXTranslationUnit?, file: CXFile?, visitor: CValue<CXCursorAndRangeVisitor>): CXResult {
    memScoped {
        return CXResult.byValue(kniBridge308(TU.rawValue, file.rawValue, visitor.getPointer(memScope).rawValue))
    }
}

fun clang_index_isEntityObjCContainerKind(arg0: CXIdxEntityKind): Int {
    return kniBridge309(arg0.value)
}

fun clang_index_getObjCContainerDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCContainerDeclInfo>? {
    memScoped {
        return interpretCPointer<CXIdxObjCContainerDeclInfo>(kniBridge310(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_getObjCInterfaceDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCInterfaceDeclInfo>? {
    memScoped {
        return interpretCPointer<CXIdxObjCInterfaceDeclInfo>(kniBridge311(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_getObjCCategoryDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCCategoryDeclInfo>? {
    memScoped {
        return interpretCPointer<CXIdxObjCCategoryDeclInfo>(kniBridge312(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_getObjCProtocolRefListInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCProtocolRefListInfo>? {
    memScoped {
        return interpretCPointer<CXIdxObjCProtocolRefListInfo>(kniBridge313(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_getObjCPropertyDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCPropertyDeclInfo>? {
    memScoped {
        return interpretCPointer<CXIdxObjCPropertyDeclInfo>(kniBridge314(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_getIBOutletCollectionAttrInfo(arg0: CValuesRef<CXIdxAttrInfo>?): CPointer<CXIdxIBOutletCollectionAttrInfo>? {
    memScoped {
        return interpretCPointer<CXIdxIBOutletCollectionAttrInfo>(kniBridge315(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_getCXXClassDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxCXXClassDeclInfo>? {
    memScoped {
        return interpretCPointer<CXIdxCXXClassDeclInfo>(kniBridge316(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_getClientContainer(arg0: CValuesRef<CXIdxContainerInfo>?): CXIdxClientContainer? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge317(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_setClientContainer(arg0: CValuesRef<CXIdxContainerInfo>?, arg1: CXIdxClientContainer?): Unit {
    memScoped {
        return kniBridge318(arg0?.getPointer(memScope).rawValue, arg1.rawValue)
    }
}

fun clang_index_getClientEntity(arg0: CValuesRef<CXIdxEntityInfo>?): CXIdxClientEntity? {
    memScoped {
        return interpretCPointer<COpaque>(kniBridge319(arg0?.getPointer(memScope).rawValue))
    }
}

fun clang_index_setClientEntity(arg0: CValuesRef<CXIdxEntityInfo>?, arg1: CXIdxClientEntity?): Unit {
    memScoped {
        return kniBridge320(arg0?.getPointer(memScope).rawValue, arg1.rawValue)
    }
}

fun clang_IndexAction_create(CIdx: CXIndex?): CXIndexAction? {
    return interpretCPointer<COpaque>(kniBridge321(CIdx.rawValue))
}

fun clang_IndexAction_dispose(arg0: CXIndexAction?): Unit {
    return kniBridge322(arg0.rawValue)
}

fun clang_indexSourceFile(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CValuesRef<CXTranslationUnitVar>?, TU_options: Int): Int {
    memScoped {
        return kniBridge323(arg0.rawValue, client_data.rawValue, index_callbacks?.getPointer(memScope).rawValue, index_callbacks_size, index_options, source_filename?.cstr?.getPointer(memScope).rawValue, command_line_args?.getPointer(memScope).rawValue, num_command_line_args, unsaved_files?.getPointer(memScope).rawValue, num_unsaved_files, out_TU?.getPointer(memScope).rawValue, TU_options)
    }
}

fun clang_indexSourceFileFullArgv(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CValuesRef<CXTranslationUnitVar>?, TU_options: Int): Int {
    memScoped {
        return kniBridge324(arg0.rawValue, client_data.rawValue, index_callbacks?.getPointer(memScope).rawValue, index_callbacks_size, index_options, source_filename?.cstr?.getPointer(memScope).rawValue, command_line_args?.getPointer(memScope).rawValue, num_command_line_args, unsaved_files?.getPointer(memScope).rawValue, num_unsaved_files, out_TU?.getPointer(memScope).rawValue, TU_options)
    }
}

fun clang_indexTranslationUnit(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, arg5: CXTranslationUnit?): Int {
    memScoped {
        return kniBridge325(arg0.rawValue, client_data.rawValue, index_callbacks?.getPointer(memScope).rawValue, index_callbacks_size, index_options, arg5.rawValue)
    }
}

fun clang_indexLoc_getFileLocation(loc: CValue<CXIdxLoc>, indexFile: CValuesRef<CXIdxClientFileVar>?, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    memScoped {
        return kniBridge326(loc.getPointer(memScope).rawValue, indexFile?.getPointer(memScope).rawValue, file?.getPointer(memScope).rawValue, line?.getPointer(memScope).rawValue, column?.getPointer(memScope).rawValue, offset?.getPointer(memScope).rawValue)
    }
}

fun clang_indexLoc_getCXSourceLocation(loc: CValue<CXIdxLoc>): CValue<CXSourceLocation> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXSourceLocation>()
        try {
            kniBridge327(loc.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_visitFields(T: CValue<CXType>, visitor: CXFieldVisitor?, client_data: CXClientData?): Int {
    memScoped {
        return kniBridge328(T.getPointer(memScope).rawValue, visitor.rawValue, client_data.rawValue)
    }
}

fun clang_Cursor_getAttributeSpelling(cursor: CValue<CXCursor>): CPointer<ByteVar>? {
    memScoped {
        return interpretCPointer<ByteVar>(kniBridge329(cursor.getPointer(memScope).rawValue))
    }
}

fun clang_getDeclTypeAttributes(cursor: CValue<CXCursor>): CValue<CXTypeAttributes> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXTypeAttributes>()
        try {
            kniBridge330(cursor.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getResultTypeAttributes(typeAttributes: CValue<CXTypeAttributes>): CValue<CXTypeAttributes> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXTypeAttributes>()
        try {
            kniBridge331(typeAttributes.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_getCursorResultTypeAttributes(cursor: CValue<CXCursor>): CValue<CXTypeAttributes> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXTypeAttributes>()
        try {
            kniBridge332(cursor.getPointer(memScope).rawValue, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Type_getNullabilityKind(type: CValue<CXType>, attributes: CValue<CXTypeAttributes>): CXNullabilityKind {
    memScoped {
        return CXNullabilityKind.byValue(kniBridge333(type.getPointer(memScope).rawValue, attributes.getPointer(memScope).rawValue))
    }
}

fun clang_Type_getNumProtocols(type: CValue<CXType>): Int {
    memScoped {
        return kniBridge334(type.getPointer(memScope).rawValue)
    }
}

fun clang_Type_getProtocol(type: CValue<CXType>, index: Int): CValue<CXCursor> {
    memScoped {
        val kniRetVal = nativeHeap.alloc<CXCursor>()
        try {
            kniBridge335(type.getPointer(memScope).rawValue, index, kniRetVal.rawPtr)
            return kniRetVal.readValue()
        } finally { nativeHeap.free(kniRetVal) }
    }
}

fun clang_Cursor_isObjCInitMethod(cursor: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge336(cursor.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isObjCReturningRetainedMethod(cursor: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge337(cursor.getPointer(memScope).rawValue)
    }
}

fun clang_Cursor_isObjCConsumingSelfMethod(cursor: CValue<CXCursor>): Int {
    memScoped {
        return kniBridge338(cursor.getPointer(memScope).rawValue)
    }
}

fun clang_isExtVectorType(type: CValue<CXType>): Int {
    memScoped {
        return kniBridge339(type.getPointer(memScope).rawValue)
    }
}

val CINDEX_VERSION_MAJOR: Int get() = 0

val CINDEX_VERSION_MINOR: Int get() = 50

val CINDEX_VERSION: Int get() = 50

val CINDEX_VERSION_STRING: String get() = "0.50"

typealias CXVirtualFileOverlayVar = CPointerVarOf<CXVirtualFileOverlay>

typealias CXVirtualFileOverlay = CPointer<CXVirtualFileOverlayImpl>

typealias CXModuleMapDescriptorVar = CPointerVarOf<CXModuleMapDescriptor>

typealias CXModuleMapDescriptor = CPointer<CXModuleMapDescriptorImpl>

typealias CXIndexVar = CPointerVarOf<CXIndex>

typealias CXIndex = COpaquePointer

typealias CXTargetInfoVar = CPointerVarOf<CXTargetInfo>

typealias CXTargetInfo = CPointer<CXTargetInfoImpl>

typealias CXTranslationUnitVar = CPointerVarOf<CXTranslationUnit>

typealias CXTranslationUnit = CPointer<CXTranslationUnitImpl>

typealias CXClientDataVar = CPointerVarOf<CXClientData>

typealias CXClientData = COpaquePointer

typealias CXFileVar = CPointerVarOf<CXFile>

typealias CXFile = COpaquePointer

typealias __darwin_time_tVar = LongVarOf<__darwin_time_t>

typealias __darwin_time_t = Long

typealias time_tVar = LongVarOf<time_t>

typealias time_t = __darwin_time_t

typealias __darwin_size_tVar = LongVarOf<__darwin_size_t>

typealias __darwin_size_t = Long

typealias size_tVar = LongVarOf<size_t>

typealias size_t = __darwin_size_t

typealias CXDiagnosticVar = CPointerVarOf<CXDiagnostic>

typealias CXDiagnostic = COpaquePointer

typealias CXDiagnosticSetVar = CPointerVarOf<CXDiagnosticSet>

typealias CXDiagnosticSet = COpaquePointer

typealias CXCursorSetVar = CPointerVarOf<CXCursorSet>

typealias CXCursorSet = CPointer<CXCursorSetImpl>

typealias CXCursorVisitorVar = CPointerVarOf<CXCursorVisitor>

typealias CXCursorVisitor = CPointer<CFunction<(CValue<CXCursor>, CValue<CXCursor>, CXClientData?) -> CXChildVisitResult>>

typealias CXPrintingPolicyVar = CPointerVarOf<CXPrintingPolicy>

typealias CXPrintingPolicy = COpaquePointer

typealias CXModuleVar = CPointerVarOf<CXModule>

typealias CXModule = COpaquePointer

typealias CXCompletionStringVar = CPointerVarOf<CXCompletionString>

typealias CXCompletionString = COpaquePointer

typealias CXInclusionVisitorVar = CPointerVarOf<CXInclusionVisitor>

typealias CXInclusionVisitor = CPointer<CFunction<(CXFile?, CPointer<CXSourceLocation>?, Int, CXClientData?) -> Unit>>

typealias CXEvalResultVar = CPointerVarOf<CXEvalResult>

typealias CXEvalResult = COpaquePointer

typealias CXRemappingVar = CPointerVarOf<CXRemapping>

typealias CXRemapping = COpaquePointer

typealias CXIdxClientFileVar = CPointerVarOf<CXIdxClientFile>

typealias CXIdxClientFile = COpaquePointer

typealias CXIdxClientEntityVar = CPointerVarOf<CXIdxClientEntity>

typealias CXIdxClientEntity = COpaquePointer

typealias CXIdxClientContainerVar = CPointerVarOf<CXIdxClientContainer>

typealias CXIdxClientContainer = COpaquePointer

typealias CXIdxClientASTFileVar = CPointerVarOf<CXIdxClientASTFile>

typealias CXIdxClientASTFile = COpaquePointer

typealias CXIndexActionVar = CPointerVarOf<CXIndexAction>

typealias CXIndexAction = COpaquePointer

typealias CXFieldVisitorVar = CPointerVarOf<CXFieldVisitor>

typealias CXFieldVisitor = CPointer<CFunction<(CValue<CXCursor>, CXClientData?) -> CXVisitorResult>>


val CXGlobalOpt_None: CXGlobalOptFlags get() = 0

val CXGlobalOpt_ThreadBackgroundPriorityForIndexing: CXGlobalOptFlags get() = 1

val CXGlobalOpt_ThreadBackgroundPriorityForEditing: CXGlobalOptFlags get() = 2

val CXGlobalOpt_ThreadBackgroundPriorityForAll: CXGlobalOptFlags get() = 3

typealias CXGlobalOptFlagsVar = IntVarOf<CXGlobalOptFlags>

typealias CXGlobalOptFlags = Int


val CXDiagnostic_DisplaySourceLocation: CXDiagnosticDisplayOptions get() = 1

val CXDiagnostic_DisplayColumn: CXDiagnosticDisplayOptions get() = 2

val CXDiagnostic_DisplaySourceRanges: CXDiagnosticDisplayOptions get() = 4

val CXDiagnostic_DisplayOption: CXDiagnosticDisplayOptions get() = 8

val CXDiagnostic_DisplayCategoryId: CXDiagnosticDisplayOptions get() = 16

val CXDiagnostic_DisplayCategoryName: CXDiagnosticDisplayOptions get() = 32

typealias CXDiagnosticDisplayOptionsVar = IntVarOf<CXDiagnosticDisplayOptions>

typealias CXDiagnosticDisplayOptions = Int


val CXTranslationUnit_None: CXTranslationUnit_Flags get() = 0

val CXTranslationUnit_DetailedPreprocessingRecord: CXTranslationUnit_Flags get() = 1

val CXTranslationUnit_Incomplete: CXTranslationUnit_Flags get() = 2

val CXTranslationUnit_PrecompiledPreamble: CXTranslationUnit_Flags get() = 4

val CXTranslationUnit_CacheCompletionResults: CXTranslationUnit_Flags get() = 8

val CXTranslationUnit_ForSerialization: CXTranslationUnit_Flags get() = 16

val CXTranslationUnit_CXXChainedPCH: CXTranslationUnit_Flags get() = 32

val CXTranslationUnit_SkipFunctionBodies: CXTranslationUnit_Flags get() = 64

val CXTranslationUnit_IncludeBriefCommentsInCodeCompletion: CXTranslationUnit_Flags get() = 128

val CXTranslationUnit_CreatePreambleOnFirstParse: CXTranslationUnit_Flags get() = 256

val CXTranslationUnit_KeepGoing: CXTranslationUnit_Flags get() = 512

val CXTranslationUnit_SingleFileParse: CXTranslationUnit_Flags get() = 1024

val CXTranslationUnit_LimitSkipFunctionBodiesToPreamble: CXTranslationUnit_Flags get() = 2048

val CXTranslationUnit_IncludeAttributedTypes: CXTranslationUnit_Flags get() = 4096

val CXTranslationUnit_VisitImplicitAttributes: CXTranslationUnit_Flags get() = 8192

typealias CXTranslationUnit_FlagsVar = IntVarOf<CXTranslationUnit_Flags>

typealias CXTranslationUnit_Flags = Int


val CXSaveTranslationUnit_None: CXSaveTranslationUnit_Flags get() = 0

typealias CXSaveTranslationUnit_FlagsVar = IntVarOf<CXSaveTranslationUnit_Flags>

typealias CXSaveTranslationUnit_Flags = Int


val CXReparse_None: CXReparse_Flags get() = 0

typealias CXReparse_FlagsVar = IntVarOf<CXReparse_Flags>

typealias CXReparse_Flags = Int


val CXTLS_None: CXTLSKind get() = 0

val CXTLS_Dynamic: CXTLSKind get() = 1

val CXTLS_Static: CXTLSKind get() = 2

typealias CXTLSKindVar = IntVarOf<CXTLSKind>

typealias CXTLSKind = Int


val CXTypeNullability_NonNull: CXTypeNullabilityKind get() = 0

val CXTypeNullability_Nullable: CXTypeNullabilityKind get() = 1

val CXTypeNullability_Unspecified: CXTypeNullabilityKind get() = 2

val CXTypeNullability_Invalid: CXTypeNullabilityKind get() = 3

typealias CXTypeNullabilityKindVar = IntVarOf<CXTypeNullabilityKind>

typealias CXTypeNullabilityKind = Int


val CXTypeLayoutError_Invalid: CXTypeLayoutError get() = -1

val CXTypeLayoutError_Incomplete: CXTypeLayoutError get() = -2

val CXTypeLayoutError_Dependent: CXTypeLayoutError get() = -3

val CXTypeLayoutError_NotConstantSize: CXTypeLayoutError get() = -4

val CXTypeLayoutError_InvalidFieldName: CXTypeLayoutError get() = -5

typealias CXTypeLayoutErrorVar = IntVarOf<CXTypeLayoutError>

typealias CXTypeLayoutError = Int


val CXRefQualifier_None: CXRefQualifierKind get() = 0

val CXRefQualifier_LValue: CXRefQualifierKind get() = 1

val CXRefQualifier_RValue: CXRefQualifierKind get() = 2

typealias CXRefQualifierKindVar = IntVarOf<CXRefQualifierKind>

typealias CXRefQualifierKind = Int


val CXPrintingPolicy_Indentation: CXPrintingPolicyProperty get() = 0

val CXPrintingPolicy_SuppressSpecifiers: CXPrintingPolicyProperty get() = 1

val CXPrintingPolicy_SuppressTagKeyword: CXPrintingPolicyProperty get() = 2

val CXPrintingPolicy_IncludeTagDefinition: CXPrintingPolicyProperty get() = 3

val CXPrintingPolicy_SuppressScope: CXPrintingPolicyProperty get() = 4

val CXPrintingPolicy_SuppressUnwrittenScope: CXPrintingPolicyProperty get() = 5

val CXPrintingPolicy_SuppressInitializers: CXPrintingPolicyProperty get() = 6

val CXPrintingPolicy_ConstantArraySizeAsWritten: CXPrintingPolicyProperty get() = 7

val CXPrintingPolicy_AnonymousTagLocations: CXPrintingPolicyProperty get() = 8

val CXPrintingPolicy_SuppressStrongLifetime: CXPrintingPolicyProperty get() = 9

val CXPrintingPolicy_SuppressLifetimeQualifiers: CXPrintingPolicyProperty get() = 10

val CXPrintingPolicy_SuppressTemplateArgsInCXXConstructors: CXPrintingPolicyProperty get() = 11

val CXPrintingPolicy_Bool: CXPrintingPolicyProperty get() = 12

val CXPrintingPolicy_Restrict: CXPrintingPolicyProperty get() = 13

val CXPrintingPolicy_Alignof: CXPrintingPolicyProperty get() = 14

val CXPrintingPolicy_UnderscoreAlignof: CXPrintingPolicyProperty get() = 15

val CXPrintingPolicy_UseVoidForZeroParams: CXPrintingPolicyProperty get() = 16

val CXPrintingPolicy_TerseOutput: CXPrintingPolicyProperty get() = 17

val CXPrintingPolicy_PolishForDeclaration: CXPrintingPolicyProperty get() = 18

val CXPrintingPolicy_Half: CXPrintingPolicyProperty get() = 19

val CXPrintingPolicy_MSWChar: CXPrintingPolicyProperty get() = 20

val CXPrintingPolicy_IncludeNewlines: CXPrintingPolicyProperty get() = 21

val CXPrintingPolicy_MSVCFormatting: CXPrintingPolicyProperty get() = 22

val CXPrintingPolicy_ConstantsAsWritten: CXPrintingPolicyProperty get() = 23

val CXPrintingPolicy_SuppressImplicitBase: CXPrintingPolicyProperty get() = 24

val CXPrintingPolicy_FullyQualifiedName: CXPrintingPolicyProperty get() = 25

val CXPrintingPolicy_LastProperty: CXPrintingPolicyProperty get() = 25

typealias CXPrintingPolicyPropertyVar = IntVarOf<CXPrintingPolicyProperty>

typealias CXPrintingPolicyProperty = Int


val CXObjCPropertyAttr_noattr: CXObjCPropertyAttrKind get() = 0

val CXObjCPropertyAttr_readonly: CXObjCPropertyAttrKind get() = 1

val CXObjCPropertyAttr_getter: CXObjCPropertyAttrKind get() = 2

val CXObjCPropertyAttr_assign: CXObjCPropertyAttrKind get() = 4

val CXObjCPropertyAttr_readwrite: CXObjCPropertyAttrKind get() = 8

val CXObjCPropertyAttr_retain: CXObjCPropertyAttrKind get() = 16

val CXObjCPropertyAttr_copy: CXObjCPropertyAttrKind get() = 32

val CXObjCPropertyAttr_nonatomic: CXObjCPropertyAttrKind get() = 64

val CXObjCPropertyAttr_setter: CXObjCPropertyAttrKind get() = 128

val CXObjCPropertyAttr_atomic: CXObjCPropertyAttrKind get() = 256

val CXObjCPropertyAttr_weak: CXObjCPropertyAttrKind get() = 512

val CXObjCPropertyAttr_strong: CXObjCPropertyAttrKind get() = 1024

val CXObjCPropertyAttr_unsafe_unretained: CXObjCPropertyAttrKind get() = 2048

val CXObjCPropertyAttr_class: CXObjCPropertyAttrKind get() = 4096

typealias CXObjCPropertyAttrKindVar = IntVarOf<CXObjCPropertyAttrKind>

typealias CXObjCPropertyAttrKind = Int


val CXObjCDeclQualifier_None: CXObjCDeclQualifierKind get() = 0

val CXObjCDeclQualifier_In: CXObjCDeclQualifierKind get() = 1

val CXObjCDeclQualifier_Inout: CXObjCDeclQualifierKind get() = 2

val CXObjCDeclQualifier_Out: CXObjCDeclQualifierKind get() = 4

val CXObjCDeclQualifier_Bycopy: CXObjCDeclQualifierKind get() = 8

val CXObjCDeclQualifier_Byref: CXObjCDeclQualifierKind get() = 16

val CXObjCDeclQualifier_Oneway: CXObjCDeclQualifierKind get() = 32

typealias CXObjCDeclQualifierKindVar = IntVarOf<CXObjCDeclQualifierKind>

typealias CXObjCDeclQualifierKind = Int


val CXNameRange_WantQualifier: CXNameRefFlags get() = 1

val CXNameRange_WantTemplateArgs: CXNameRefFlags get() = 2

val CXNameRange_WantSinglePiece: CXNameRefFlags get() = 4

typealias CXNameRefFlagsVar = IntVarOf<CXNameRefFlags>

typealias CXNameRefFlags = Int


val CXCodeComplete_IncludeMacros: CXCodeComplete_Flags get() = 1

val CXCodeComplete_IncludeCodePatterns: CXCodeComplete_Flags get() = 2

val CXCodeComplete_IncludeBriefComments: CXCodeComplete_Flags get() = 4

val CXCodeComplete_SkipPreamble: CXCodeComplete_Flags get() = 8

val CXCodeComplete_IncludeCompletionsWithFixIts: CXCodeComplete_Flags get() = 16

typealias CXCodeComplete_FlagsVar = IntVarOf<CXCodeComplete_Flags>

typealias CXCodeComplete_Flags = Int


val CXCompletionContext_Unexposed: CXCompletionContext get() = 0

val CXCompletionContext_AnyType: CXCompletionContext get() = 1

val CXCompletionContext_AnyValue: CXCompletionContext get() = 2

val CXCompletionContext_ObjCObjectValue: CXCompletionContext get() = 4

val CXCompletionContext_ObjCSelectorValue: CXCompletionContext get() = 8

val CXCompletionContext_CXXClassTypeValue: CXCompletionContext get() = 16

val CXCompletionContext_DotMemberAccess: CXCompletionContext get() = 32

val CXCompletionContext_ArrowMemberAccess: CXCompletionContext get() = 64

val CXCompletionContext_ObjCPropertyAccess: CXCompletionContext get() = 128

val CXCompletionContext_EnumTag: CXCompletionContext get() = 256

val CXCompletionContext_UnionTag: CXCompletionContext get() = 512

val CXCompletionContext_StructTag: CXCompletionContext get() = 1024

val CXCompletionContext_ClassTag: CXCompletionContext get() = 2048

val CXCompletionContext_Namespace: CXCompletionContext get() = 4096

val CXCompletionContext_NestedNameSpecifier: CXCompletionContext get() = 8192

val CXCompletionContext_ObjCInterface: CXCompletionContext get() = 16384

val CXCompletionContext_ObjCProtocol: CXCompletionContext get() = 32768

val CXCompletionContext_ObjCCategory: CXCompletionContext get() = 65536

val CXCompletionContext_ObjCInstanceMessage: CXCompletionContext get() = 131072

val CXCompletionContext_ObjCClassMessage: CXCompletionContext get() = 262144

val CXCompletionContext_ObjCSelectorName: CXCompletionContext get() = 524288

val CXCompletionContext_MacroName: CXCompletionContext get() = 1048576

val CXCompletionContext_NaturalLanguage: CXCompletionContext get() = 2097152

val CXCompletionContext_IncludedFile: CXCompletionContext get() = 4194304

val CXCompletionContext_Unknown: CXCompletionContext get() = 8388607

typealias CXCompletionContextVar = IntVarOf<CXCompletionContext>

typealias CXCompletionContext = Int


val CXIdxEntityLang_None: CXIdxEntityLanguage get() = 0

val CXIdxEntityLang_C: CXIdxEntityLanguage get() = 1

val CXIdxEntityLang_ObjC: CXIdxEntityLanguage get() = 2

val CXIdxEntityLang_CXX: CXIdxEntityLanguage get() = 3

val CXIdxEntityLang_Swift: CXIdxEntityLanguage get() = 4

typealias CXIdxEntityLanguageVar = IntVarOf<CXIdxEntityLanguage>

typealias CXIdxEntityLanguage = Int


val CXIdxEntity_NonTemplate: CXIdxEntityCXXTemplateKind get() = 0

val CXIdxEntity_Template: CXIdxEntityCXXTemplateKind get() = 1

val CXIdxEntity_TemplatePartialSpecialization: CXIdxEntityCXXTemplateKind get() = 2

val CXIdxEntity_TemplateSpecialization: CXIdxEntityCXXTemplateKind get() = 3

typealias CXIdxEntityCXXTemplateKindVar = IntVarOf<CXIdxEntityCXXTemplateKind>

typealias CXIdxEntityCXXTemplateKind = Int


val CXIdxAttr_Unexposed: CXIdxAttrKind get() = 0

val CXIdxAttr_IBAction: CXIdxAttrKind get() = 1

val CXIdxAttr_IBOutlet: CXIdxAttrKind get() = 2

val CXIdxAttr_IBOutletCollection: CXIdxAttrKind get() = 3

typealias CXIdxAttrKindVar = IntVarOf<CXIdxAttrKind>

typealias CXIdxAttrKind = Int


val CXIdxDeclFlag_Skipped: CXIdxDeclInfoFlags get() = 1

typealias CXIdxDeclInfoFlagsVar = IntVarOf<CXIdxDeclInfoFlags>

typealias CXIdxDeclInfoFlags = Int


val CXIdxObjCContainer_ForwardRef: CXIdxObjCContainerKind get() = 0

val CXIdxObjCContainer_Interface: CXIdxObjCContainerKind get() = 1

val CXIdxObjCContainer_Implementation: CXIdxObjCContainerKind get() = 2

typealias CXIdxObjCContainerKindVar = IntVarOf<CXIdxObjCContainerKind>

typealias CXIdxObjCContainerKind = Int


val CXIdxEntityRef_Direct: CXIdxEntityRefKind get() = 1

val CXIdxEntityRef_Implicit: CXIdxEntityRefKind get() = 2

typealias CXIdxEntityRefKindVar = IntVarOf<CXIdxEntityRefKind>

typealias CXIdxEntityRefKind = Int


val CXSymbolRole_None: CXSymbolRole get() = 0

val CXSymbolRole_Declaration: CXSymbolRole get() = 1

val CXSymbolRole_Definition: CXSymbolRole get() = 2

val CXSymbolRole_Reference: CXSymbolRole get() = 4

val CXSymbolRole_Read: CXSymbolRole get() = 8

val CXSymbolRole_Write: CXSymbolRole get() = 16

val CXSymbolRole_Call: CXSymbolRole get() = 32

val CXSymbolRole_Dynamic: CXSymbolRole get() = 64

val CXSymbolRole_AddressOf: CXSymbolRole get() = 128

val CXSymbolRole_Implicit: CXSymbolRole get() = 256

typealias CXSymbolRoleVar = IntVarOf<CXSymbolRole>

typealias CXSymbolRole = Int


val CXIndexOpt_None: CXIndexOptFlags get() = 0

val CXIndexOpt_SuppressRedundantRefs: CXIndexOptFlags get() = 1

val CXIndexOpt_IndexFunctionLocalSymbols: CXIndexOptFlags get() = 2

val CXIndexOpt_IndexImplicitTemplateInstantiations: CXIndexOptFlags get() = 4

val CXIndexOpt_SuppressWarnings: CXIndexOptFlags get() = 8

val CXIndexOpt_SkipParsedBodiesInSession: CXIndexOptFlags get() = 16

typealias CXIndexOptFlagsVar = IntVarOf<CXIndexOptFlags>

typealias CXIndexOptFlags = Int
private external fun kniBridge0(p0: NativePtr): NativePtr
private external fun kniBridge1(p0: NativePtr): Unit
private external fun kniBridge2(p0: NativePtr): Unit
private external fun kniBridge3(): Long
private external fun kniBridge4(p0: Int): NativePtr
private external fun kniBridge5(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge6(p0: NativePtr, p1: Int): Int
private external fun kniBridge7(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr): Int
private external fun kniBridge8(p0: NativePtr): Unit
private external fun kniBridge9(p0: NativePtr): Unit
private external fun kniBridge10(p0: Int): NativePtr
private external fun kniBridge11(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge12(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge13(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr): Int
private external fun kniBridge14(p0: NativePtr): Unit
private external fun kniBridge15(p0: Int, p1: Int): NativePtr
private external fun kniBridge16(p0: NativePtr): Unit
private external fun kniBridge17(p0: NativePtr, p1: Int): Unit
private external fun kniBridge18(p0: NativePtr): Int
private external fun kniBridge19(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge20(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge21(p0: NativePtr): Long
private external fun kniBridge22(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge23(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge24(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge25(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge26(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge27(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge28(p0: NativePtr): Unit
private external fun kniBridge29(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge30(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int, p4: NativePtr): Unit
private external fun kniBridge31(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr): Unit
private external fun kniBridge32(p0: NativePtr): Int
private external fun kniBridge33(p0: NativePtr): Int
private external fun kniBridge34(p0: NativePtr): Unit
private external fun kniBridge35(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge36(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge37(p0: NativePtr): Int
private external fun kniBridge38(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): Unit
private external fun kniBridge39(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): Unit
private external fun kniBridge40(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): Unit
private external fun kniBridge41(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): Unit
private external fun kniBridge42(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr): Unit
private external fun kniBridge43(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge44(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge45(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge46(p0: NativePtr): NativePtr
private external fun kniBridge47(p0: NativePtr): Unit
private external fun kniBridge48(p0: NativePtr): Int
private external fun kniBridge49(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge50(p0: NativePtr, p1: NativePtr, p2: NativePtr): NativePtr
private external fun kniBridge51(p0: NativePtr): Unit
private external fun kniBridge52(p0: NativePtr): NativePtr
private external fun kniBridge53(p0: NativePtr): Int
private external fun kniBridge54(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge55(p0: NativePtr): NativePtr
private external fun kniBridge56(p0: NativePtr): Unit
private external fun kniBridge57(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge58(): Int
private external fun kniBridge59(p0: NativePtr): Int
private external fun kniBridge60(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge61(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge62(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge63(p0: NativePtr): Int
private external fun kniBridge64(p0: Int, p1: NativePtr): Unit
private external fun kniBridge65(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge66(p0: NativePtr): Int
private external fun kniBridge67(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge68(p0: NativePtr): Int
private external fun kniBridge69(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr): Unit
private external fun kniBridge70(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge71(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr, p4: Int, p5: NativePtr): NativePtr
private external fun kniBridge72(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge73(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge74(): Int
private external fun kniBridge75(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr, p5: Int, p6: Int): NativePtr
private external fun kniBridge76(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr, p5: Int, p6: Int, p7: NativePtr): Int
private external fun kniBridge77(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: NativePtr, p5: Int, p6: Int, p7: NativePtr): Int
private external fun kniBridge78(p0: NativePtr): Int
private external fun kniBridge79(p0: NativePtr, p1: NativePtr, p2: Int): Int
private external fun kniBridge80(p0: NativePtr): Int
private external fun kniBridge81(p0: NativePtr): Unit
private external fun kniBridge82(p0: NativePtr): Int
private external fun kniBridge83(p0: NativePtr, p1: Int, p2: NativePtr, p3: Int): Int
private external fun kniBridge84(p0: Int): NativePtr
private external fun kniBridge85(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge86(p0: NativePtr): Unit
private external fun kniBridge87(p0: NativePtr): NativePtr
private external fun kniBridge88(p0: NativePtr): Unit
private external fun kniBridge89(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge90(p0: NativePtr): Int
private external fun kniBridge91(p0: NativePtr): Unit
private external fun kniBridge92(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge93(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge94(p0: NativePtr): Int
private external fun kniBridge95(p0: NativePtr): Int
private external fun kniBridge96(p0: NativePtr): Int
private external fun kniBridge97(p0: Int): Int
private external fun kniBridge98(p0: NativePtr): Int
private external fun kniBridge99(p0: Int): Int
private external fun kniBridge100(p0: Int): Int
private external fun kniBridge101(p0: Int): Int
private external fun kniBridge102(p0: Int): Int
private external fun kniBridge103(p0: NativePtr): Int
private external fun kniBridge104(p0: Int): Int
private external fun kniBridge105(p0: Int): Int
private external fun kniBridge106(p0: Int): Int
private external fun kniBridge107(p0: Int): Int
private external fun kniBridge108(p0: NativePtr): Int
private external fun kniBridge109(p0: NativePtr): Int
private external fun kniBridge110(p0: NativePtr): Int
private external fun kniBridge111(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr, p6: Int): Int
private external fun kniBridge112(p0: NativePtr): Unit
private external fun kniBridge113(p0: NativePtr): Int
private external fun kniBridge114(p0: NativePtr): Int
private external fun kniBridge115(p0: NativePtr): NativePtr
private external fun kniBridge116(): NativePtr
private external fun kniBridge117(p0: NativePtr): Unit
private external fun kniBridge118(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge119(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge120(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge121(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge122(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge123(p0: NativePtr): Unit
private external fun kniBridge124(p0: NativePtr): NativePtr
private external fun kniBridge125(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge126(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge127(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge128(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge129(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge130(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge131(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge132(p0: NativePtr): Long
private external fun kniBridge133(p0: NativePtr): Long
private external fun kniBridge134(p0: NativePtr): Int
private external fun kniBridge135(p0: NativePtr): Int
private external fun kniBridge136(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge137(p0: NativePtr): Int
private external fun kniBridge138(p0: NativePtr, p1: Int): Int
private external fun kniBridge139(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge140(p0: NativePtr, p1: Int): Long
private external fun kniBridge141(p0: NativePtr, p1: Int): Long
private external fun kniBridge142(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge143(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge144(p0: NativePtr): Int
private external fun kniBridge145(p0: NativePtr): Int
private external fun kniBridge146(p0: NativePtr): Int
private external fun kniBridge147(p0: NativePtr): Int
private external fun kniBridge148(p0: NativePtr): Int
private external fun kniBridge149(p0: NativePtr): Int
private external fun kniBridge150(p0: NativePtr): Int
private external fun kniBridge151(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge152(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge153(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge154(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge155(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge156(p0: Int, p1: NativePtr): Unit
private external fun kniBridge157(p0: NativePtr): Int
private external fun kniBridge158(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge159(p0: NativePtr): Int
private external fun kniBridge160(p0: NativePtr): Int
private external fun kniBridge161(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge162(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge163(p0: NativePtr): Int
private external fun kniBridge164(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge165(p0: NativePtr): Int
private external fun kniBridge166(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge167(p0: NativePtr): Int
private external fun kniBridge168(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge169(p0: NativePtr): Int
private external fun kniBridge170(p0: NativePtr): Int
private external fun kniBridge171(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge172(p0: NativePtr): Long
private external fun kniBridge173(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge174(p0: NativePtr): Long
private external fun kniBridge175(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge176(p0: NativePtr): Int
private external fun kniBridge177(p0: NativePtr): Int
private external fun kniBridge178(p0: NativePtr): Long
private external fun kniBridge179(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge180(p0: NativePtr): Long
private external fun kniBridge181(p0: NativePtr, p1: NativePtr): Long
private external fun kniBridge182(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge183(p0: NativePtr): Long
private external fun kniBridge184(p0: NativePtr): Int
private external fun kniBridge185(p0: NativePtr): Int
private external fun kniBridge186(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge187(p0: NativePtr): Int
private external fun kniBridge188(p0: NativePtr): Int
private external fun kniBridge189(p0: NativePtr): Int
private external fun kniBridge190(p0: NativePtr): Int
private external fun kniBridge191(p0: NativePtr): Int
private external fun kniBridge192(p0: NativePtr): Int
private external fun kniBridge193(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge194(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge195(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge196(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge197(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge198(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge199(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge200(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge201(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr): Unit
private external fun kniBridge202(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge203(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge204(p0: NativePtr, p1: Int, p2: Int, p3: NativePtr): Unit
private external fun kniBridge205(p0: NativePtr, p1: Int): Int
private external fun kniBridge206(p0: NativePtr, p1: Int, p2: Int): Unit
private external fun kniBridge207(p0: NativePtr): NativePtr
private external fun kniBridge208(p0: NativePtr): Unit
private external fun kniBridge209(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge210(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge211(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge212(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge213(p0: NativePtr): Int
private external fun kniBridge214(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge215(p0: NativePtr): Int
private external fun kniBridge216(p0: NativePtr): Int
private external fun kniBridge217(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge218(p0: NativePtr, p1: Int): Int
private external fun kniBridge219(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge220(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge221(p0: NativePtr): Int
private external fun kniBridge222(p0: NativePtr): Int
private external fun kniBridge223(p0: NativePtr): Int
private external fun kniBridge224(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): Int
private external fun kniBridge225(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge226(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge227(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge228(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge229(p0: NativePtr): NativePtr
private external fun kniBridge230(p0: NativePtr): NativePtr
private external fun kniBridge231(p0: NativePtr): NativePtr
private external fun kniBridge232(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge233(p0: NativePtr): NativePtr
private external fun kniBridge234(p0: NativePtr): NativePtr
private external fun kniBridge235(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge236(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge237(p0: NativePtr): Int
private external fun kniBridge238(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge239(p0: NativePtr, p1: NativePtr, p2: Int): NativePtr
private external fun kniBridge240(p0: NativePtr): Int
private external fun kniBridge241(p0: NativePtr): Int
private external fun kniBridge242(p0: NativePtr): Int
private external fun kniBridge243(p0: NativePtr): Int
private external fun kniBridge244(p0: NativePtr): Int
private external fun kniBridge245(p0: NativePtr): Int
private external fun kniBridge246(p0: NativePtr): Int
private external fun kniBridge247(p0: NativePtr): Int
private external fun kniBridge248(p0: NativePtr): Int
private external fun kniBridge249(p0: NativePtr): Int
private external fun kniBridge250(p0: NativePtr): Int
private external fun kniBridge251(p0: NativePtr): Int
private external fun kniBridge252(p0: NativePtr): Int
private external fun kniBridge253(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge254(p0: NativePtr, p1: Int, p2: Int, p3: NativePtr): Unit
private external fun kniBridge255(p0: NativePtr, p1: NativePtr): NativePtr
private external fun kniBridge256(p0: NativePtr): Int
private external fun kniBridge257(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge258(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge259(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge260(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr): Unit
private external fun kniBridge261(p0: NativePtr, p1: NativePtr, p2: Int, p3: NativePtr): Unit
private external fun kniBridge262(p0: NativePtr, p1: NativePtr, p2: Int): Unit
private external fun kniBridge263(p0: Int, p1: NativePtr): Unit
private external fun kniBridge264(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr, p6: NativePtr): Unit
private external fun kniBridge265(): Unit
private external fun kniBridge266(p0: NativePtr, p1: NativePtr, p2: Int): Unit
private external fun kniBridge267(p0: NativePtr, p1: Int): Int
private external fun kniBridge268(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge269(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge270(p0: NativePtr): Int
private external fun kniBridge271(p0: NativePtr): Int
private external fun kniBridge272(p0: NativePtr): Int
private external fun kniBridge273(p0: NativePtr): Int
private external fun kniBridge274(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge275(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge276(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge277(p0: NativePtr): NativePtr
private external fun kniBridge278(p0: NativePtr, p1: Int): Int
private external fun kniBridge279(p0: NativePtr, p1: Int, p2: Int, p3: NativePtr, p4: NativePtr): Unit
private external fun kniBridge280(): Int
private external fun kniBridge281(p0: NativePtr, p1: NativePtr, p2: Int, p3: Int, p4: NativePtr, p5: Int, p6: Int): NativePtr
private external fun kniBridge282(p0: NativePtr, p1: Int): Unit
private external fun kniBridge283(p0: NativePtr): Unit
private external fun kniBridge284(p0: NativePtr): Int
private external fun kniBridge285(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge286(p0: NativePtr): Long
private external fun kniBridge287(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge288(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge289(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge290(p0: NativePtr): Unit
private external fun kniBridge291(p0: Int): Unit
private external fun kniBridge292(p0: NativePtr, p1: NativePtr, p2: NativePtr): Unit
private external fun kniBridge293(p0: NativePtr): NativePtr
private external fun kniBridge294(p0: NativePtr): Int
private external fun kniBridge295(p0: NativePtr): Int
private external fun kniBridge296(p0: NativePtr): Long
private external fun kniBridge297(p0: NativePtr): Int
private external fun kniBridge298(p0: NativePtr): Long
private external fun kniBridge299(p0: NativePtr): Double
private external fun kniBridge300(p0: NativePtr): NativePtr
private external fun kniBridge301(p0: NativePtr): Unit
private external fun kniBridge302(p0: NativePtr): NativePtr
private external fun kniBridge303(p0: NativePtr, p1: Int): NativePtr
private external fun kniBridge304(p0: NativePtr): Int
private external fun kniBridge305(p0: NativePtr, p1: Int, p2: NativePtr, p3: NativePtr): Unit
private external fun kniBridge306(p0: NativePtr): Unit
private external fun kniBridge307(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge308(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge309(p0: Int): Int
private external fun kniBridge310(p0: NativePtr): NativePtr
private external fun kniBridge311(p0: NativePtr): NativePtr
private external fun kniBridge312(p0: NativePtr): NativePtr
private external fun kniBridge313(p0: NativePtr): NativePtr
private external fun kniBridge314(p0: NativePtr): NativePtr
private external fun kniBridge315(p0: NativePtr): NativePtr
private external fun kniBridge316(p0: NativePtr): NativePtr
private external fun kniBridge317(p0: NativePtr): NativePtr
private external fun kniBridge318(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge319(p0: NativePtr): NativePtr
private external fun kniBridge320(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge321(p0: NativePtr): NativePtr
private external fun kniBridge322(p0: NativePtr): Unit
private external fun kniBridge323(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: Int, p5: NativePtr, p6: NativePtr, p7: Int, p8: NativePtr, p9: Int, p10: NativePtr, p11: Int): Int
private external fun kniBridge324(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: Int, p5: NativePtr, p6: NativePtr, p7: Int, p8: NativePtr, p9: Int, p10: NativePtr, p11: Int): Int
private external fun kniBridge325(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: Int, p4: Int, p5: NativePtr): Int
private external fun kniBridge326(p0: NativePtr, p1: NativePtr, p2: NativePtr, p3: NativePtr, p4: NativePtr, p5: NativePtr): Unit
private external fun kniBridge327(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge328(p0: NativePtr, p1: NativePtr, p2: NativePtr): Int
private external fun kniBridge329(p0: NativePtr): NativePtr
private external fun kniBridge330(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge331(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge332(p0: NativePtr, p1: NativePtr): Unit
private external fun kniBridge333(p0: NativePtr, p1: NativePtr): Int
private external fun kniBridge334(p0: NativePtr): Int
private external fun kniBridge335(p0: NativePtr, p1: Int, p2: NativePtr): Unit
private external fun kniBridge336(p0: NativePtr): Int
private external fun kniBridge337(p0: NativePtr): Int
private external fun kniBridge338(p0: NativePtr): Int
private external fun kniBridge339(p0: NativePtr): Int
private val loadLibrary = loadKonanLibrary("clangstubs")
