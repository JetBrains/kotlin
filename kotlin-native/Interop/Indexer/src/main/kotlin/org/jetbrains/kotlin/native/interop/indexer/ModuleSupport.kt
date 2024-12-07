package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.readValue
import java.nio.file.Files

data class ModulesInfo(val topLevelHeaders: List<IncludeInfo>, val ownHeaders: Set<String>, val modules: List<String>)

fun getModulesInfo(compilation: Compilation, modules: List<String>): ModulesInfo {
    if (modules.isEmpty()) return ModulesInfo(emptyList(), emptySet(), emptyList())

    val areModulesEnabled = compilation.compilerArgs.contains("-fmodules")
    withIndex(excludeDeclarationsFromPCH = false) { index ->
        ModularCompilation(compilation).use {
            val modulesASTFiles = getModulesASTFiles(index, it, modules)
            return buildModulesInfo(index, modules, modulesASTFiles, areModulesEnabled)
        }
    }
}

data class IncludeInfo(val headerPath: String, val moduleName: String?)

private fun buildModulesInfo(
        index: CXIndex,
        modules: List<String>,
        modulesASTFiles: List<String>,
        areModulesEnabled: Boolean
): ModulesInfo {
    val ownHeaders = mutableSetOf<String>()
    val topLevelHeaders = linkedSetOf<IncludeInfo>()
    modulesASTFiles.forEach {
        val moduleTranslationUnit = clang_createTranslationUnit(index, it)!!
        try {
            val modulesHeaders = getModulesHeaders(index, moduleTranslationUnit, modules.toSet(), topLevelHeaders, areModulesEnabled)
            modulesHeaders.mapTo(ownHeaders) { it.canonicalPath }
        } finally {
            clang_disposeTranslationUnit(moduleTranslationUnit)
        }
    }

    return ModulesInfo(topLevelHeaders.toList(), ownHeaders, modules)
}

internal open class ModularCompilation(compilation: Compilation) : Compilation by compilation, Disposable {

    companion object {
        private const val moduleCacheFlag = "-fmodules-cache-path="
    }

    private val moduleCacheDirectory = if (compilation.compilerArgs.none { it.startsWith(moduleCacheFlag) }) {
        Files.createTempDirectory("ModuleCache").toAbsolutePath().toFile()
    } else {
        null
    }

    override val compilerArgs: List<String> = compilation.compilerArgs +
            listOfNotNull("-fmodules", moduleCacheDirectory?.let { "$moduleCacheFlag${it}" })

    override fun dispose() {
        moduleCacheDirectory?.deleteRecursively()
    }
}

private fun getModulesASTFiles(index: CXIndex, compilation: ModularCompilation, modules: List<String>): List<String> {
    val compilationWithImports = compilation.copy(
            additionalPreambleLines = modules.map { "@import $it;" } + compilation.additionalPreambleLines
    )

    val result = linkedSetOf<String>()
    val errors = mutableListOf<Diagnostic>()

    val translationUnit = compilationWithImports.parse(
            index,
            options = CXTranslationUnit_DetailedPreprocessingRecord,
            diagnosticHandler = { if (it.isError()) errors.add(it) }
    )
    try {
        if (errors.isNotEmpty()) {
            val errorMessage = errors.take(10).joinToString("\n") { it.format }
            throw Error(errorMessage)
        }

        translationUnit.ensureNoCompileErrors()

        indexTranslationUnit(index, translationUnit, 0, object : Indexer {
            override fun importedASTFile(info: CXIdxImportedASTFileInfo) {
                result += info.file!!.canonicalPath
            }
        })
    } finally {
        clang_disposeTranslationUnit(translationUnit)
    }
    return result.toList()
}

private fun getModulesHeaders(
        index: CXIndex,
        translationUnit: CXTranslationUnit,
        modules: Set<String>,
        topLevelHeaders: LinkedHashSet<IncludeInfo>,
        areModulesEnabled: Boolean
): Set<CXFile> {
    val nonModularIncludes = mutableMapOf<CXFile, MutableSet<CXFile>>()
    val result = mutableSetOf<CXFile>()
    val errors = mutableListOf<Throwable>()

    indexTranslationUnit(index, translationUnit, 0, object : Indexer {
        override fun importedASTFile(info: CXIdxImportedASTFileInfo) {
            val isModuleImport = info.isImplicit == 0
            if (isModuleImport && !areModulesEnabled) {
                val name = clang_Module_getFullName(info.module).convertAndDispose()
                val headerPath = clang_indexLoc_getCXSourceLocation(info.loc.readValue()).getContainingFile()?.canonicalPath
                val message = buildString {
                    appendLine("use of '@import' when modules are disabled")
                    appendLine("header: '$headerPath'")
                    appendLine("module name: '$name'")
                }
                errors.add(Error(message))
            }
        }

        override fun ppIncludedFile(info: CXIdxIncludedFileInfo) {
            val file = info.file!!
            val includer = clang_indexLoc_getCXSourceLocation(info.hashLoc.readValue()).getContainingFile()

            val module = clang_getModuleForFile(translationUnit, file)

            if (includer == null) {
                // i.e. the header is included by the module itself.
                topLevelHeaders += IncludeInfo(file.path, clang_Module_getFullName(module).convertAndDispose())
            }

            if (module != null) {
                val moduleWithParents = generateSequence(module, { clang_Module_getParent(it) }).map {
                    clang_Module_getFullName(it).convertAndDispose()
                }

                if (moduleWithParents.any { it in modules }) {
                    result += file
                }
            } else if (includer != null) {
                nonModularIncludes.getOrPut(includer, { mutableSetOf() }) += file
            }
        }
    })

    if (errors.isNotEmpty()) throw errors.first()

    // There are cases when non-modular includes should also be considered as a part of module. For example:
    // 1. Some module maps are broken,
    //    e.g. system header `IOKit/hid/IOHIDProperties.h` isn't included to framework module map at all.
    // 2. Textual headers are reported as non-modular by libclang.
    //
    // Find and include non-modular headers too:
    result += findReachable(roots = result, arcs = nonModularIncludes)

    return result
}

private fun <T> findReachable(roots: Set<T>, arcs: Map<T, Set<T>>): Set<T> {
    val visited = mutableSetOf<T>()

    fun dfs(vertex: T) {
        if (!visited.add(vertex)) return
        arcs[vertex].orEmpty().forEach { dfs(it) }
    }

    roots.forEach { dfs(it) }

    return visited
}
