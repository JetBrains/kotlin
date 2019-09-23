/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.URI
import java.util.regex.Pattern

class JavaClassCache() : Serializable {
    private var sourceCache = mutableMapOf<URI, SourceFileStructure>()

    /** Record these separately because we only need to know where each generated type is coming from. */
    private var generatedTypes = mutableMapOf<File, MutableList<String>>()

    /** Map from types to files they are mentioned in. */
    @Transient
    private var dependencyCache = mutableMapOf<String, MutableSet<URI>>()
    @Transient
    private var nonTransitiveCache = mutableMapOf<String, MutableSet<URI>>()

    fun addSourceStructure(sourceStructure: SourceFileStructure) {
        sourceCache[sourceStructure.sourceFile] = sourceStructure
    }

    fun addGeneratedType(type: String, generatedFile: File) {
        val typesInFile = generatedTypes[generatedFile] ?: ArrayList(1)
        typesInFile.add(type)
        generatedTypes[generatedFile] = typesInFile
    }

    fun invalidateGeneratedTypes(files: List<File>): Set<String> {
        return files.mapNotNull { generatedTypes.remove(it) }.flatten().toSet()
    }

    private fun readObject(input: ObjectInputStream) {
        @Suppress("UNCHECKED_CAST")
        sourceCache = input.readObject() as MutableMap<URI, SourceFileStructure>
        @Suppress("UNCHECKED_CAST")
        generatedTypes = input.readObject() as MutableMap<File, MutableList<String>>

        dependencyCache = HashMap(sourceCache.size * 4)
        for (sourceInfo in sourceCache.values) {
            for (mentionedType in sourceInfo.getMentionedTypes()) {
                val dependants = dependencyCache[mentionedType] ?: mutableSetOf()
                dependants.add(sourceInfo.sourceFile)
                dependencyCache[mentionedType] = dependants
            }
            // Treat referred constants as ABI dependencies until we start supporting per-constant classpath updates.
            for (mentionedConstants in sourceInfo.getMentionedConstants().keys) {
                val dependants = dependencyCache[mentionedConstants] ?: mutableSetOf()
                dependants.add(sourceInfo.sourceFile)
                dependencyCache[mentionedConstants] = dependants
            }
        }
        nonTransitiveCache = HashMap(sourceCache.size * 2)
        for (sourceInfo in sourceCache.values) {
            for (privateType in sourceInfo.getPrivateTypes()) {
                val dependants = nonTransitiveCache[privateType] ?: mutableSetOf()
                dependants.add(sourceInfo.sourceFile)
                nonTransitiveCache[privateType] = dependants
            }
        }
    }

    private fun writeObject(output: ObjectOutputStream) {
        output.writeObject(sourceCache)
        output.writeObject(generatedTypes)
    }

    fun isAlreadyProcessed(sourceFile: URI) = sourceCache.containsKey(sourceFile) || generatedTypes.containsKey(File(sourceFile))

    /** Used for testing only. */
    internal fun getStructure(sourceFile: File) = sourceCache[sourceFile.toURI()]

    /**
     * Invalidate cache entries for the specified files, and any files that depend on the changed ones. It returns the set of files that
     * should be re-processed.
     * */
    fun invalidateEntriesForChangedFiles(changes: Changes): SourcesToReprocess {
        val allDirtyFiles = mutableSetOf<URI>()
        var currentDirtyFiles = changes.sourceChanges.map { it.toURI() }.toMutableSet()

        for (classpathFqName in changes.dirtyFqNamesFromClasspath) {
            nonTransitiveCache[classpathFqName]?.let {
                allDirtyFiles.addAll(it)
            }

            dependencyCache[classpathFqName]?.let {
                currentDirtyFiles.addAll(it)
            }
        }

        val allDirtyTypes = mutableSetOf<String>()

        while (currentDirtyFiles.isNotEmpty()) {

            val nextRound = mutableSetOf<URI>()
            for (dirtyFile in currentDirtyFiles) {
                allDirtyFiles.add(dirtyFile)

                val structure = sourceCache.remove(dirtyFile) ?: continue
                val dirtyTypes = structure.getDeclaredTypes()
                allDirtyTypes.addAll(dirtyTypes)

                dirtyTypes.forEach { type ->
                    nonTransitiveCache[type]?.let {
                        allDirtyFiles.addAll(it)
                    }

                    dependencyCache[type]?.let {
                        nextRound.addAll(it)
                    }
                }
            }

            currentDirtyFiles = nextRound.filter { !allDirtyFiles.contains(it) }.toMutableSet()
        }

        return SourcesToReprocess.Incremental(allDirtyFiles.map { File(it) }, allDirtyTypes)
    }

    /**
     * For aggregating annotation processors, we always need to reprocess all files annotated with an annotation claimed by the aggregating
     * annotation processor. This search is not transitive.
     */
    fun invalidateEntriesAnnotatedWith(annotations: Set<String>): Set<File> {
        val patterns: List<Pattern> = if ("*" in annotations) {
            // optimize this case - create only one pattern
            listOf(Pattern.compile(".*"))
        } else {
            annotations.map {
                Pattern.compile(
                    // These are already valid import statements, otherwise run fails when loading the annotation processor.
                    // Handles structure; TypeName [.*] e.g. org.jetbrains.annotations.NotNull and org.jetbrains.annotations.*
                    it.replace(".", "\\.").replace("*", ".+")
                )
            }
        }
        val matchesAnyPattern = { name: String -> patterns.any { it.matcher(name).matches() } }

        val toReprocess = mutableSetOf<URI>()

        for (cacheEntry in sourceCache) {
            if (cacheEntry.value.getMentionedAnnotations().any(matchesAnyPattern)) {
                toReprocess.add(cacheEntry.key)
            }
        }

        toReprocess.forEach {
            sourceCache.remove(it)
        }

        return toReprocess.map { File(it) }.toSet()
    }

    internal fun invalidateAll() {
        sourceCache.clear()
        generatedTypes.clear()
    }
}


private val IGNORE_TYPES = { name: String -> name == "java.lang.Object" }

class SourceFileStructure(
    val sourceFile: URI
) : Serializable {

    private val declaredTypes: MutableSet<String> = mutableSetOf()

    private val mentionedTypes: MutableSet<String> = mutableSetOf()
    private val privateTypes: MutableSet<String> = mutableSetOf()

    private val mentionedAnnotations: MutableSet<String> = mutableSetOf()
    private val mentionedConstants: MutableMap<String, MutableSet<String>> = mutableMapOf()

    fun getDeclaredTypes(): Set<String> = declaredTypes
    fun getMentionedTypes(): Set<String> = mentionedTypes
    fun getPrivateTypes(): Set<String> = privateTypes
    fun getMentionedAnnotations(): Set<String> = mentionedAnnotations
    fun getMentionedConstants(): Map<String, Set<String>> = mentionedConstants

    fun addDeclaredType(declaredType: String) {
        declaredTypes.add(declaredType)
    }

    fun addMentionedType(mentionedType: String) {
        mentionedType.takeUnless(IGNORE_TYPES)?.let {
            mentionedTypes.add(it)
        }
    }

    fun addMentionedAnnotations(name: String) {
        mentionedAnnotations.add(name)
    }

    fun addPrivateType(name: String) {
        privateTypes.add(name)
    }

    fun addMentionedConstant(containingClass: String, name: String) {
        if (!declaredTypes.contains(containingClass)) {
            mentionedConstants.getOrPut(containingClass) { HashSet() }.add(name)
        }
    }
}


class Changes(val sourceChanges: Collection<File>, val dirtyFqNamesFromClasspath: Set<String>)