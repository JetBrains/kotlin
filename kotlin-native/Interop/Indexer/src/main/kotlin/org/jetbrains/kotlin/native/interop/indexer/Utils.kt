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
import kotlinx.cinterop.*
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest

internal val CValue<CXType>.kind: CXTypeKind get() = this.useContents { kind }

internal val CValue<CXCursor>.kind: CXCursorKind get() = this.useContents { kind }

internal val CValue<CXCursor>.type: CValue<CXType> get() = clang_getCursorType(this)
internal val CValue<CXCursor>.spelling: String get() = clang_getCursorSpelling(this).convertAndDispose()
internal val CValue<CXType>.name: String get() = clang_getTypeSpelling(this).convertAndDispose()
internal val CXTypeKind.spelling: String get() = clang_getTypeKindSpelling(this).convertAndDispose()
internal val CXCursorKind.spelling: String get() = clang_getCursorKindSpelling(this).convertAndDispose()

internal fun CValue<CXString>.convertAndDispose(): String {
    try {
        return clang_getCString(this)!!.toKString()
    } finally {
        clang_disposeString(this)
    }
}

internal fun CPointer<CXStringSet>.convertAndDispose(): Set<String> = try {
    (0 until this.pointed.Count).mapTo(mutableSetOf()) {
        clang_getCString(this.pointed.Strings!![it].readValue())!!.toKString()
    }
} finally {
    clang_disposeStringSet(this)
}

internal fun getCursorSpelling(cursor: CValue<CXCursor>) =
        clang_getCursorSpelling(cursor).convertAndDispose()

internal fun CValue<CXType>.getSize(): Long {
    val size = clang_Type_getSizeOf(this)
    if (size < 0) {
        throw Error(size.toString())
    }
    return size
}

internal inline fun <R> withIndex(
        excludeDeclarationsFromPCH: Boolean = false,
        displayDiagnostics: Boolean = false,
        block: (index: CXIndex) -> R
): R {
    val index = clang_createIndex(
            excludeDeclarationsFromPCH = if (excludeDeclarationsFromPCH) 1 else 0,
            displayDiagnostics = if (displayDiagnostics) 1 else 0
    )!!

    return try {
        block(index)
    } finally {
        clang_disposeIndex(index)
    }
}

internal fun parseTranslationUnit(
        index: CXIndex,
        sourceFile: File,
        compilerArgs: List<String>,
        options: Int
): CXTranslationUnit {

    memScoped {
        val resultVar = alloc<CXTranslationUnitVar>()

        val errorCode = clang_parseTranslationUnit2(
                index,
                sourceFile.absolutePath,
                compilerArgs.toNativeStringArray(memScope), compilerArgs.size,
                null, 0,
                options,
                resultVar.ptr
        )

        if (errorCode != CXErrorCode.CXError_Success) {
            val copiedSourceFile = sourceFile.copyTo(createTempFile(suffix = sourceFile.name), overwrite = true)

            error("""
                clang_parseTranslationUnit2 failed with $errorCode;
                sourceFile = ${copiedSourceFile.absolutePath}
                arguments = ${compilerArgs.joinToString(" ")}
                """.trimIndent())
        }

        return resultVar.value!!
    }
}

internal fun Compilation.parse(index: CXIndex, options: Int = 0): CXTranslationUnit =
        parseTranslationUnit(index, this.createTempSource(), this.compilerArgs, options)

internal data class Diagnostic(val severity: CXDiagnosticSeverity, val format: String,
                               val location: CValue<CXSourceLocation>)

internal fun CXTranslationUnit.getDiagnostics(): Sequence<Diagnostic> {
    val numDiagnostics = clang_getNumDiagnostics(this)
    return (0 until numDiagnostics).asSequence()
            .map { index ->
                val diagnostic = clang_getDiagnostic(this, index)
                try {
                    val severity = clang_getDiagnosticSeverity(diagnostic)

                    val format = clang_formatDiagnostic(diagnostic, clang_defaultDiagnosticDisplayOptions())
                            .convertAndDispose()

                    val location = clang_getDiagnosticLocation(diagnostic)

                    Diagnostic(severity, format, location)
                } finally {
                    clang_disposeDiagnostic(diagnostic)
                }
            }
}

internal fun CXTranslationUnit.getCompileErrors(): Sequence<String> =
        getDiagnostics().filter { it.isError() }.map { it.format }

private fun Diagnostic.isError() = (severity == CXDiagnosticSeverity.CXDiagnostic_Error) ||
        (severity == CXDiagnosticSeverity.CXDiagnostic_Fatal)

internal fun CXTranslationUnit.hasCompileErrors() = (this.getCompileErrors().firstOrNull() != null)

internal fun CXTranslationUnit.ensureNoCompileErrors(): CXTranslationUnit {
    val firstError = this.getCompileErrors().firstOrNull() ?: return this
    throw Error(firstError)
}

internal typealias CursorVisitor = (cursor: CValue<CXCursor>, parent: CValue<CXCursor>) -> CXChildVisitResult

internal fun visitChildren(parent: CValue<CXCursor>, visitor: CursorVisitor) {
    val visitorStableRef = StableRef.create(visitor)
    try {
        val clientData = visitorStableRef.asCPointer()
        clang_visitChildren(parent, staticCFunction { cursorIt, parentIt, clientDataIt ->
            val visitorIt = clientDataIt!!.asStableRef<CursorVisitor>().get()
            visitorIt(cursorIt, parentIt)
        }, clientData)
    } finally {
        visitorStableRef.dispose()
    }
}

internal fun visitChildren(translationUnit: CXTranslationUnit, visitor: CursorVisitor) =
        visitChildren(clang_getTranslationUnitCursor(translationUnit), visitor)

internal fun getFields(type: CValue<CXType>): List<CValue<CXCursor>> {
    val result = mutableListOf<CValue<CXCursor>>()
    val resultStableRef = StableRef.create(result)
    try {
        val clientData = resultStableRef.asCPointer()

        @Suppress("NAME_SHADOWING")
        clang_Type_visitFields(type, staticCFunction { cursor, clientData ->
            val result = clientData!!.asStableRef<MutableList<CValue<CXCursor>>>().get()
            result.add(cursor)
            CXVisitorResult.CXVisit_Continue
        }, clientData)

    } finally {
        resultStableRef.dispose()
    }

    return result
}

fun StructDef.fieldsHaveDefaultAlignment(): Boolean {
    fun alignUp(x: Long, alignment: Long): Long = (x + alignment - 1) and (alignment - 1).inv()

    var offset = 0L
    this.members.forEach {
        when (it) {
            is Field -> {
                if (alignUp(offset, it.typeAlign) * 8 != it.offset) return false
                offset = it.offset / 8 + it.typeSize
            }
            is BitField -> return false
        }
    }

    return true
}

internal fun CValue<CXCursor>.hasExpressionChild(): Boolean {
    var result = false

    visitChildren(this) { cursor, _ ->
        if (clang_isExpression(cursor.kind) != 0) {
            result = true
            CXChildVisitResult.CXChildVisit_Break
        } else {
            CXChildVisitResult.CXChildVisit_Continue
        }
    }

    return result
}

internal fun List<String>.toNativeStringArray(scope: AutofreeScope): CArrayPointer<CPointerVar<ByteVar>> {
    return scope.allocArray(this.size) { index ->
        this.value = this@toNativeStringArray[index].cstr.getPointer(scope)
    }
}

val Compilation.preambleLines: List<String>
    get() = this.includes.map { "#include <$it>" } + this.additionalPreambleLines

internal fun Appendable.appendPreamble(compilation: Compilation) = this.apply {
    compilation.preambleLines.forEach {
        this.appendLine(it)
    }
}

/**
 * Creates temporary source file which includes the library.
 */
internal fun Compilation.createTempSource(): File {
    val result = createTempFile(suffix = ".${language.sourceFileExtension}")
    result.deleteOnExit()

    result.bufferedWriter().use { writer ->
        writer.appendPreamble(this)
    }

    return result
}

fun Compilation.copy(
        includes: List<String> = this.includes,
        additionalPreambleLines: List<String> = this.additionalPreambleLines,
        compilerArgs: List<String> = this.compilerArgs,
        language: Language = this.language
): Compilation = CompilationImpl(
        includes = includes,
        additionalPreambleLines = additionalPreambleLines,
        compilerArgs = compilerArgs,
        language = language
)

// Clang-8 crashes when consuming a precompiled header built with -fmodule-map-file argument (see KT-34467).
// We ignore this argument when building a pch to workaround this crash.
fun Compilation.copyWithArgsForPCH(): Compilation =
        copy(compilerArgs = compilerArgs.filterNot { it.startsWith("-fmodule-map-file") })

data class CompilationImpl(
        override val includes: List<String>,
        override val additionalPreambleLines: List<String>,
        override val compilerArgs: List<String>,
        override val language: Language
) : Compilation

/**
 * Precompiles the headers of this library.
 *
 * @return the library which includes the precompiled header instead of original ones.
 */
fun Compilation.precompileHeaders(): CompilationWithPCH = withIndex { index ->
    val options = CXTranslationUnit_ForSerialization
    val translationUnit = copyWithArgsForPCH().parse(index, options)
    try {
        translationUnit.ensureNoCompileErrors()
        withPrecompiledHeader(translationUnit)
    } finally {
        clang_disposeTranslationUnit(translationUnit)
    }
}

internal fun Compilation.withPrecompiledHeader(translationUnit: CXTranslationUnit): CompilationWithPCH {
    val precompiledHeader = createTempFile(suffix = ".pch").apply { this.deleteOnExit() }
    clang_saveTranslationUnit(translationUnit, precompiledHeader.absolutePath, 0)

    return CompilationWithPCH(this.compilerArgs, precompiledHeader.absolutePath, this.language)
}

internal fun NativeLibrary.includesDeclaration(cursor: CValue<CXCursor>): Boolean {
    return if (this.excludeSystemLibs) {
        clang_Location_isInSystemHeader(clang_getCursorLocation(cursor)) == 0
    } else {
        true
    }
}

internal fun CXTranslationUnit.getErrorLineNumbers(): Sequence<Int> =
        getDiagnostics().filter {
            it.isError()
        }.map {
            memScoped {
                val lineNumberVar = alloc<IntVar>()
                clang_getFileLocation(it.location, null, lineNumberVar.ptr, null, null)
                lineNumberVar.value
            }
        }

/**
 * For each list of lines, checks if the code fragment composed from these lines is compilable against given library.
 */
fun List<List<String>>.mapFragmentIsCompilable(originalLibrary: CompilationWithPCH): List<Boolean> {
    val library: CompilationWithPCH = originalLibrary
            .copy(compilerArgs = originalLibrary.compilerArgs + "-ferror-limit=0")

    val indicesOfNonCompilable = mutableSetOf<Int>()

    val fragmentsToCheck = this.withIndex().toMutableList()

    withIndex(excludeDeclarationsFromPCH = true) { index ->
        val sourceFile = library.createTempSource()
        val translationUnit = parseTranslationUnit(index, sourceFile, library.compilerArgs, options = 0)
        try {
            translationUnit.ensureNoCompileErrors()
            while (fragmentsToCheck.isNotEmpty()) {
                // Combine all fragments to be checked in a single file:
                sourceFile.bufferedWriter().use { writer ->
                    writer.appendPreamble(library)
                    fragmentsToCheck.forEach {
                        it.value.forEach {
                            assert(!it.contains('\n'))
                            writer.appendLine(it)
                        }
                    }
                }

                clang_reparseTranslationUnit(translationUnit, 0, null, 0)
                val errorLineNumbers = translationUnit.getErrorLineNumbers().toSet()

                // Retain only those fragments that contain compilation error locations:
                var lastLineNumber = library.preambleLines.size
                fragmentsToCheck.retainAll {
                    val firstLineNumber = lastLineNumber + 1
                    lastLineNumber += it.value.size
                    (firstLineNumber .. lastLineNumber).any { it in errorLineNumbers }
                }

                if (fragmentsToCheck.isNotEmpty()) {
                    // The first fragment is now known to be non-compilable.
                    val firstFragment = fragmentsToCheck.removeAt(0)
                    indicesOfNonCompilable.add(firstFragment.index)
                }

                // The remaining fragments was potentially influenced by the first one,
                // and thus require to be checked again.
            }
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    }

    return this.indices.map { it !in indicesOfNonCompilable }
}

internal interface Indexer {
    /**
     * Called when entered main file.
     */
    fun enteredMainFile(file: CXFile) {}

    /**
     * Called when a file gets #included/#imported.
     */
    fun ppIncludedFile(info: CXIdxIncludedFileInfo) {}

    /**
     * Called when a AST file (PCH or module) gets imported.
     */
    fun importedASTFile(info: CXIdxImportedASTFileInfo) {}

    /**
     * Called to index a declaration.
     */
    fun indexDeclaration(info: CXIdxDeclInfo) {}
}

internal fun indexTranslationUnit(index: CXIndex, translationUnit: CXTranslationUnit, options: Int, indexer: Indexer) {
    val indexerStableRef = StableRef.create(indexer)
    try {
        val clientData = indexerStableRef.asCPointer()
        memScoped {
            val indexerCallbacks = alloc<IndexerCallbacks>().apply {
                abortQuery = null
                diagnostic = null
                enteredMainFile = staticCFunction { clientData, mainFile, _ ->
                    @Suppress("NAME_SHADOWING")
                    val indexer = clientData!!.asStableRef<Indexer>().get()
                    indexer.enteredMainFile(mainFile!!)
                    // We must ensure only interop types exist in function signature.
                    @Suppress("USELESS_CAST")
                    null as CXIdxClientFile?
                }
                ppIncludedFile = staticCFunction { clientData, info ->
                    @Suppress("NAME_SHADOWING")
                    val indexer = clientData!!.asStableRef<Indexer>().get()
                    indexer.ppIncludedFile(info!!.pointed)
                    // We must ensure only interop types exist in function signature.
                    @Suppress("USELESS_CAST")
                    null as CXIdxClientFile?
                }
                importedASTFile = staticCFunction { clientData, info ->
                    @Suppress("NAME_SHADOWING")
                    val indexer = clientData!!.asStableRef<Indexer>().get()
                    indexer.importedASTFile(info!!.pointed)
                    // We must ensure only interop types exist in function signature.
                    @Suppress("USELESS_CAST")
                    null as CXIdxClientFile?
                }
                startedTranslationUnit = null
                indexDeclaration = staticCFunction { clientData, info ->
                    @Suppress("NAME_SHADOWING")
                    val nativeIndex = clientData!!.asStableRef<Indexer>().get()
                    nativeIndex.indexDeclaration(info!!.pointed)
                }
                indexEntityReference = null
            }

            val indexAction = clang_IndexAction_create(index)
            try {
                val result = clang_indexTranslationUnit(indexAction, clientData,
                        indexerCallbacks.ptr, sizeOf<IndexerCallbacks>().toInt(), options, translationUnit)

                if (result != 0) {
                    throw Error("clang_indexTranslationUnit returned $result")
                }
            } finally {
                clang_IndexAction_dispose(indexAction)
            }
        }
    } finally {
        indexerStableRef.dispose()
    }
}

internal class ModulesMap(
        val compilation: Compilation,
        val translationUnit: CXTranslationUnit
) : Closeable {

    private val modularCompilation: ModularCompilation
    private val index: CXIndex
    private val translationUnitWithModules: CXTranslationUnit

    private val arena = Arena()

    private inline fun <T> T.toBeDisposedWith(crossinline block: (T) -> Unit): T = apply {
        arena.defer { block(this) }
    }

    override fun close() {
        arena.clear()
    }

    init {
        try {
            modularCompilation = ModularCompilation(compilation)
                    .toBeDisposedWith { it.dispose() }

            index = clang_createIndex(0, 0)!!
                    .toBeDisposedWith { clang_disposeIndex(it) }

            translationUnitWithModules = modularCompilation.parse(index)
                    .toBeDisposedWith { clang_disposeTranslationUnit(it) }

            translationUnitWithModules.ensureNoCompileErrors()

        } catch (e: Throwable) {
            this.close()
            throw e
        }
    }

    data class Module(private val cxModule: CXModule)

    fun getModule(file: CXFile): Module? {
        // `file` is bound to `translationUnit`, however `translationUnitWithModules` is used to access modules.
        // Find the corresponding file in `translationUnitWithModules`:
        val fileInTuWithModules =
                clang_getFile(translationUnitWithModules, clang_getFileName(file).convertAndDispose())!!

        return clang_getModuleForFile(translationUnitWithModules, fileInTuWithModules)?.let { Module(it) }
    }
}

internal fun getHeaderId(library: NativeLibrary, header: CXFile?): HeaderId {
    if (header == null) {
        return HeaderId("builtins")
    }

    val filePath = header.path
    return library.headerToIdMapper.getHeaderId(filePath)
}

internal fun getFilteredHeaders(
        nativeIndex: NativeIndexImpl,
        index: CXIndex,
        translationUnit: CXTranslationUnit
): Set<CXFile?> = getHeaders(nativeIndex.library, index, translationUnit).ownHeaders

class NativeLibraryHeaders<Header>(val ownHeaders: Set<Header>, val importedHeaders: Set<Header>)

internal fun getHeaders(
        library: NativeLibrary,
        index: CXIndex,
        translationUnit: CXTranslationUnit
): NativeLibraryHeaders<CXFile?> {
    val ownHeaders = mutableSetOf<CXFile?>()
    val allHeaders = mutableSetOf<CXFile?>(null)

    val filter = library.headerFilter

    when (filter) {
        is NativeLibraryHeaderFilter.NameBased ->
            filterHeadersByName(library, filter, index, translationUnit, ownHeaders, allHeaders)

        is NativeLibraryHeaderFilter.Predefined ->
            filterHeadersByPredefined(filter, index, translationUnit, ownHeaders, allHeaders)
    }

    ownHeaders.removeAll { library.headerExclusionPolicy.excludeAll(getHeaderId(library, it)) }

    return NativeLibraryHeaders(ownHeaders, allHeaders - ownHeaders)
}

private fun filterHeadersByName(
        compilation: Compilation,
        filter: NativeLibraryHeaderFilter.NameBased,
        index: CXIndex,
        translationUnit: CXTranslationUnit,
        ownHeaders: MutableSet<CXFile?>,
        allHeaders: MutableSet<CXFile?>
) {
    val topLevelFiles = mutableListOf<CXFile>()
    var mainFile: CXFile? = null

    indexTranslationUnit(index, translationUnit, 0, object : Indexer {
        val headerToName = mutableMapOf<CXFile, String>()
        // The *name* of the header here is the path relative to the include path element., e.g. `curl/curl.h`.

        override fun enteredMainFile(file: CXFile) {
            mainFile = file
            allHeaders += file
        }

        override fun ppIncludedFile(info: CXIdxIncludedFileInfo) {
            val includeLocation = clang_indexLoc_getCXSourceLocation(info.hashLoc.readValue())
            val file = info.file!!

            allHeaders += file

            if (clang_Location_isFromMainFile(includeLocation) != 0) {
                topLevelFiles.add(file)
            }

            val name = info.filename!!.toKString()
            val headerName = if (info.isAngled != 0) {
                // If the header is included with `#include <$name>`, then `name` is probably
                // the path relative to the include path element.
                name
            } else {
                // If it is included with `#include "$name"`, then `name` can also be the path relative to the includer.
                val includerFile = includeLocation.getContainingFile()!!
                val includerName = headerToName[includerFile] ?: ""
                val includerPath = includerFile.path

                if (clang_getFile(translationUnit, Paths.get(includerPath).resolveSibling(name).toString()) == file) {
                    // included file is accessible from the includer by `name` used as relative path, so
                    // `name` seems to be relative to the includer:
                    Paths.get(includerName).resolveSibling(name).normalize().toString()
                } else {
                    name
                }
            }

            headerToName[file] = headerName
            if (!filter.policy.excludeUnused(headerName)) {
                ownHeaders.add(file)
            }
        }
    })

    if (filter.excludeDepdendentModules) {
        ModulesMap(compilation, translationUnit).use { modulesMap ->
            val topLevelModules = topLevelFiles.map { modulesMap.getModule(it) }.toSet()
            ownHeaders.removeAll {
                val module = modulesMap.getModule(it!!)
                module !in topLevelModules
            }
            // Note: if some of the top-level headers don't belong to modules,
            // then all non-modular headers are included.
        }
    } else {
        if (!filter.policy.excludeUnused(headerName = null)) {
            // Builtins.
            ownHeaders.add(null)
        }
    }

    ownHeaders.add(mainFile!!)
}

private fun filterHeadersByPredefined(
        filter: NativeLibraryHeaderFilter.Predefined,
        index: CXIndex,
        translationUnit: CXTranslationUnit,
        ownHeaders: MutableSet<CXFile?>,
        allHeaders: MutableSet<CXFile?>
) {
    // Note: suboptimal but simple.
    indexTranslationUnit(index, translationUnit, 0, object : Indexer {
        override fun ppIncludedFile(info: CXIdxIncludedFileInfo) {
            val file = info.file
            allHeaders += file
            if (file?.canonicalPath in filter.headers) {
                ownHeaders += file
            }
        }
    })
}

fun NativeLibrary.getHeaderPaths(): NativeLibraryHeaders<String> {
    withIndex { index ->
        val translationUnit =
                this.parse(index, options = CXTranslationUnit_DetailedPreprocessingRecord).ensureNoCompileErrors()
        try {

            fun getPath(file: CXFile?) = if (file == null) "<builtins>" else file.canonicalPath

            val headers = getHeaders(this, index, translationUnit)
            return NativeLibraryHeaders(
                    headers.ownHeaders.map(::getPath).toSet(),
                    headers.importedHeaders.map(::getPath).toSet()
            )
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    }
}

fun ObjCMethod.replaces(other: ObjCMethod): Boolean =
        this.isClass == other.isClass && this.selector == other.selector

fun ObjCProperty.replaces(other: ObjCProperty): Boolean =
        this.getter.replaces(other.getter)

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    DigestInputStream(this.inputStream(), digest).use { dis ->
        val buffer = ByteArray(8192)
        // Read all bytes:
        while (dis.read(buffer, 0, buffer.size) != -1) {}
    }
    // Convert to hex:
    return digest.digest().joinToString("") {
        Integer.toHexString((it.toInt() and 0xff) + 0x100).substring(1)
    }
}

fun headerContentsHash(filePath: String) = File(filePath).sha256()

internal fun CValue<CXSourceLocation>.getContainingFile(): CXFile? = memScoped {
    val fileVar = alloc<CXFileVar>()
    clang_getFileLocation(this@getContainingFile, fileVar.ptr, null, null, null)
    fileVar.value
}

@JvmName("getFileContainingCursor")
internal fun getContainingFile(cursor: CValue<CXCursor>): CXFile? {
    return clang_getCursorLocation(cursor).getContainingFile()
}

internal val CXFile.path: String get() = clang_getFileName(this).convertAndDispose()
internal val CXFile.canonicalPath: String get() = File(this.path).canonicalPath

private fun createVfsOverlayFileContents(virtualPathToReal: Map<Path, Path>): ByteArray {
    val overlay = clang_VirtualFileOverlay_create(0)

    try {
        fun addFileMapping(realPath: Path, virtualPath: Path) {
            clang_VirtualFileOverlay_addFileMapping(
                    overlay,
                    virtualPath = virtualPath.toAbsolutePath().toString(),
                    realPath = realPath.toAbsolutePath().toString()
            )
        }

        virtualPathToReal.forEach { virtualPath, realPath ->
            if (Files.isDirectory(realPath)) {
                realPath.toFile().walkTopDown().forEach {
                    if (!it.isDirectory) {
                        addFileMapping(
                                realPath = it.toPath(),
                                virtualPath = virtualPath.resolve(realPath.relativize(it.toPath()))
                        )
                    }
                }
            } else {
                addFileMapping(realPath = realPath, virtualPath = virtualPath)
            }
        }

        memScoped {
            val bufferVar = alloc<CPointerVar<ByteVar>>().apply { value = null }
            val bufferSizeVar = alloc<IntVar>()

            val res = clang_VirtualFileOverlay_writeToBuffer(overlay, 0, bufferVar.ptr, bufferSizeVar.ptr)
            if (res != CXErrorCode.CXError_Success) {
                // TODO: shall we free the buffer in this case?
                error(res)
            }

            return bufferVar.value!!.readBytes(bufferSizeVar.value)
        }
    } finally {
        clang_VirtualFileOverlay_dispose(overlay)
    }
}

fun createVfsOverlayFile(virtualPathToReal: Map<Path, Path>): Path {
    val bytes = createVfsOverlayFileContents(virtualPathToReal)

    return createTempFile(prefix = "konan", suffix = ".vfsoverlay").apply {
        bufferedWriter().use {
            writeBytes(bytes)
        }
        deleteOnExit()
    }.toPath()
}

tailrec fun Type.unwrapTypedefs(): Type = if (this is Typedef) {
    this.def.aliased.unwrapTypedefs()
} else {
    this
}

fun Type.canonicalIsPointerToChar(): Boolean {
    val unwrappedType = this.unwrapTypedefs()
    return unwrappedType is PointerType && unwrappedType.pointeeType.unwrapTypedefs() == CharType
}

internal interface Disposable {
    fun dispose()
}

internal inline fun <T : Disposable, R> T.use(block: (T) -> R): R = try {
    block(this)
} finally {
    this.dispose()
}
