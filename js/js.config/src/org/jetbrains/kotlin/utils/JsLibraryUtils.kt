/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.Processor
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object JsLibraryUtils {
    private val LOG = Logger.getInstance(JsLibraryUtils::class.java)

    private val META_INF_RESOURCES = "${LibraryUtils.META_INF}resources/"

    // Also used in K2JSCompilerMojo
    @JvmStatic
    fun isKotlinJavascriptLibrary(library: File): Boolean =
        KotlinJavascriptMetadataUtils.loadMetadata(library).isNotEmpty()

    // Also used in K2JSCompilerMojo
    @Suppress("unused")
    @JvmStatic
    fun isKotlinJavascriptIrLibrary(candidate: File): Boolean {
        return when {
            isZippedKlib(candidate) -> true
            FileUtil.isJarOrZip(candidate) -> isZippedKlibInZip(candidate)
            !candidate.resolve("default").isDirectory -> false
            !candidate.resolve("default").resolve("manifest").isFile -> false
            !candidate.resolve("default").resolve("ir").isDirectory -> false
            else -> true
        }
    }

    @JvmStatic fun copyJsFilesFromLibraries(libraries: List<String>, outputLibraryJsPath: String, copySourceMap: Boolean = false) {
        for (library in libraries) {
            val file = File(library)
            assert(file.exists()) { "Library $library not found" }

            if (file.isDirectory) {
                copyJsFilesFromDirectory(file, outputLibraryJsPath, copySourceMap)
            }
            else {
                copyJsFilesFromZip(file, outputLibraryJsPath, copySourceMap)
            }
        }
    }

    @JvmStatic fun traverseJsLibraries(libs: List<File>, action: (JsLibrary) -> Unit) {
        libs.forEach { traverseJsLibrary(it, action) }
    }

    @JvmStatic fun traverseJsLibrary(lib: File, action: (JsLibrary) -> Unit) {
        when {
            lib.isDirectory -> traverseDirectory(lib, action)
            FileUtil.isJarOrZip(lib) -> traverseArchive(lib, action)
            lib.name.endsWith(KotlinJavascriptMetadataUtils.JS_EXT) -> {
                lib.runIfFileExists(lib.path, action)
                val jsFile = lib.withReplacedExtensionOrNull(
                        KotlinJavascriptMetadataUtils.META_JS_SUFFIX, KotlinJavascriptMetadataUtils.JS_EXT
                )
                jsFile?.runIfFileExists(jsFile.path, action)
            }
        }
    }

    private fun isZippedKlibInZip(candidate: File): Boolean {
        var manifestFound = false
        var irFound = false
        ZipFile(candidate).use {
            for (entry in it.entries()) {
                if (entry.name == "default/manifest") manifestFound = true
                if (entry.name == "default/ir/") irFound = true
            }
        }

        return manifestFound && irFound
    }

    private fun isZippedKlib(candidate: File): Boolean =
        candidate.extension == "klib"

    private fun File.runIfFileExists(relativePath: String, action: (JsLibrary) -> Unit) {
        if (isFile) {
            action(JsLibrary(readText(), relativePath, correspondingSourceMapFile().contentIfExists(), this))
        }
    }

    private fun copyJsFilesFromDirectory(dir: File, outputLibraryJsPath: String, copySourceMap: Boolean) {
        traverseDirectory(dir) { copyLibrary(outputLibraryJsPath, it, copySourceMap) }
    }

    private fun File.contentIfExists(): String? = if (exists()) readText() else null

    private fun File.correspondingSourceMapFile(): File = File(parentFile, name + ".map")

    private fun processDirectory(dir: File, action: (JsLibrary) -> Unit) {
        FileUtil.processFilesRecursively(dir, Processor<File> { file ->
            val relativePath = FileUtil.getRelativePath(dir, file)
                               ?: throw IllegalArgumentException("relativePath should not be null $dir $file")
            if (relativePath.endsWith(KotlinJavascriptMetadataUtils.JS_EXT)) {
                val suggestedRelativePath = getSuggestedPath(relativePath) ?: return@Processor true
                file.runIfFileExists(suggestedRelativePath, action)
            }
            true
        })
    }

    private fun traverseDirectory(dir: File, action: (JsLibrary) -> Unit) {
        try {
            processDirectory(dir, action)
        }
        catch (ex: IOException) {
            LOG.error("Could not read files from directory ${dir.name}: ${ex.message}")
        }
    }

    private fun copyJsFilesFromZip(file: File, outputLibraryJsPath: String, copySourceMap: Boolean) {
        traverseArchive(file) { copyLibrary(outputLibraryJsPath, it, copySourceMap) }
    }

    private fun copyLibrary(outputPath: String, library: JsLibrary, copySourceMap: Boolean) {
        val targetFile = File(outputPath, library.path)
        targetFile.parentFile.mkdirs()
        targetFile.writeText(library.content)
        if (copySourceMap) {
            library.sourceMapContent?.let { File(targetFile.parent, targetFile.name + ".map").writeText(it) }
        }
    }

    private fun traverseArchive(file: File, action: (JsLibrary) -> Unit) {
        val zipFile = try {
            ZipFile(file.path)
        } catch (e: ZipException) {
            throw IOException("Failed to open zip file: $file", e)
        }
        try {
            val zipEntries = zipFile.entries()
            val librariesWithoutSourceMaps = mutableListOf<JsLibrary>()
            val possibleMapFiles = mutableMapOf<String, ZipEntry>()

            while (zipEntries.hasMoreElements()) {
                val entry = zipEntries.nextElement()
                val entryName = entry.name
                if (!entry.isDirectory) {
                    if (entryName.endsWith(KotlinJavascriptMetadataUtils.JS_EXT)) {
                        val relativePath = getSuggestedPath(entryName) ?: continue

                        val stream = zipFile.getInputStream(entry)
                        val content = stream.reader().readText()
                        librariesWithoutSourceMaps += JsLibrary(content, relativePath, null, null)
                    }
                    else if (entryName.endsWith(KotlinJavascriptMetadataUtils.JS_MAP_EXT)) {
                        val correspondingJsPath = entryName.removeSuffix(KotlinJavascriptMetadataUtils.JS_MAP_EXT) +
                                                  KotlinJavascriptMetadataUtils.JS_EXT
                        possibleMapFiles[correspondingJsPath] = entry
                    }
                }
            }

            librariesWithoutSourceMaps
                    .map {
                        val zipEntry = possibleMapFiles[it.path]
                        if (zipEntry != null) {
                            val stream = zipFile.getInputStream(zipEntry)
                            val content = stream.reader().readText()
                            it.copy(sourceMapContent = content)
                        }
                        else {
                            it
                        }
                    }
                    .forEach(action)
        }
        catch (ex: IOException) {
            LOG.error("Could not extract files from archive ${file.name}: ${ex.message}")
        }
        finally {
            zipFile.close()
        }
    }

    private fun getSuggestedPath(path: String): String? {
        val systemIndependentPath = FileUtil.toSystemIndependentName(path)
        if (systemIndependentPath.startsWith(LibraryUtils.META_INF)) {
            if (systemIndependentPath.startsWith(META_INF_RESOURCES)) {
                return path.substring(META_INF_RESOURCES.length)
            }
            return null
        }

        return path
    }
}

data class JsLibrary(val content: String, val path: String, val sourceMapContent: String?, val file: File?)
