package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.*
import java.nio.file.Files

data class ModulesInfo(val topLevelHeaders: List<String>, val ownHeaders: Set<String>)

fun getModulesInfo(compilation: Compilation, modules: List<String>): ModulesInfo {
    if (modules.isEmpty()) return ModulesInfo(emptyList(), emptySet())

    withIndex { index ->
        ModularCompilation(compilation).use {
            val modulesASTFiles = getModulesASTFiles(index, it, modules)
            return buildModulesInfo(index, modules, modulesASTFiles)
        }
    }
}

private fun buildModulesInfo(index: CXIndex, modules: List<String>, modulesASTFiles: List<String>): ModulesInfo {
    val ownHeaders = mutableSetOf<String>()
    val topLevelHeaders = linkedSetOf<String>()
    modulesASTFiles.forEach {
        val moduleTranslationUnit = clang_createTranslationUnit(index, it)!!
        try {
            val modulesHeaders = getModulesHeaders(index, moduleTranslationUnit, modules.toSet(), topLevelHeaders)
            modulesHeaders.mapTo(ownHeaders) { it.canonicalPath }
        } finally {
            clang_disposeTranslationUnit(moduleTranslationUnit)
        }
    }

    return ModulesInfo(topLevelHeaders.toList(), ownHeaders)
}

internal open class ModularCompilation(compilation: Compilation): Compilation by compilation, Disposable {

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

    val translationUnit = compilationWithImports.parse(
            index,
            options = CXTranslationUnit_DetailedPreprocessingRecord
    )
    try {
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
        topLevelHeaders: LinkedHashSet<String>
): Set<CXFile> {
    val nonModularIncludes = mutableMapOf<CXFile, MutableSet<CXFile>>()
    val result = mutableSetOf<CXFile>()

    indexTranslationUnit(index, translationUnit, 0, object : Indexer {
        override fun ppIncludedFile(info: CXIdxIncludedFileInfo) {
            val file = info.file!!
            val includer = clang_indexLoc_getCXSourceLocation(info.hashLoc.readValue()).getContainingFile()

            if (includer == null) {
                // i.e. the header is included by the module itself.
                topLevelHeaders += file.path
            }

            val module = clang_getModuleForFile(translationUnit, file)

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
