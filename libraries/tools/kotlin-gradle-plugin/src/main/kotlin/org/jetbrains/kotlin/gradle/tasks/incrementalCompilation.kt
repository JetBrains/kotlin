package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.mapClassesFqNamesToFiles
import org.jetbrains.kotlin.incremental.mapLookupSymbolsToFiles
import org.jetbrains.kotlin.incremental.snapshots.FileCollectionDiff
import org.jetbrains.kotlin.incremental.withSubtypes
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.*

internal sealed class CompilationMode {
    class Incremental(val dirtyFiles: Set<File>) : CompilationMode()
    class Rebuild : CompilationMode()
}

internal fun calculateSourcesToCompile(
        javaFilesProcessor: ChangedJavaFilesProcessor,
        caches: IncrementalCachesManager,
        lastBuildInfo: BuildInfo?,
        changedFiles: ChangedFiles,
        logger: Logger,
        classpath: Iterable<File>,
        dirtySourcesSinceLastTimeFile: File,
        filesToString: (Iterable<File>)-> String,
        logAction: (String)->Unit,
        relativePathOrCanonical: (File)->String,
        artifactDifferenceRegistry: ArtifactDifferenceRegistry?
): CompilationMode {
    fun rebuild(reason: String): CompilationMode {
        logger.kotlinInfo("Non-incremental compilation will be performed: $reason")
        caches.clean()
        dirtySourcesSinceLastTimeFile.delete()
        return CompilationMode.Rebuild()
    }

    if (changedFiles !is ChangedFiles.Known) return rebuild("inputs' changes are unknown (first or clean build)")

    val removedClassFiles = changedFiles.removed.filter(File::isClassFile)
    if (removedClassFiles.any()) return rebuild("Removed class files: ${filesToString(removedClassFiles)}")

    val modifiedClassFiles = changedFiles.modified.filter(File::isClassFile)
    if (modifiedClassFiles.any()) return rebuild("Modified class files: ${filesToString(modifiedClassFiles)}")

    val classpathSet = classpath.toHashSet()
    val modifiedClasspathEntries = changedFiles.modified.filter { it in classpathSet }
    val classpathChanges = getClasspathChanges(modifiedClasspathEntries, lastBuildInfo, logger, artifactDifferenceRegistry)
    if (classpathChanges is ChangesEither.Unknown) {
        return rebuild("could not get changes from modified classpath entries: ${filesToString(modifiedClasspathEntries)}")
    }
    if (classpathChanges !is ChangesEither.Known) {
        throw AssertionError("Unknown implementation of ChangesEither: ${classpathChanges.javaClass}")
    }
    val javaFilesDiff = FileCollectionDiff(
            newOrModified = changedFiles.modified.filter(File::isJavaFile),
            removed = changedFiles.removed.filter(File::isJavaFile))
    val javaFilesChanges = javaFilesProcessor.process(javaFilesDiff)
    val affectedJavaSymbols = when (javaFilesChanges) {
        is ChangesEither.Known -> javaFilesChanges.lookupSymbols
        is ChangesEither.Unknown -> return rebuild("Could not get changes for java files")
    }

    val dirtyFiles = HashSet<File>(with(changedFiles) { modified.size + removed.size })
    with(changedFiles) {
        modified.asSequence() + removed.asSequence()
    }.forEach { if (it.isKotlinFile()) dirtyFiles.add(it) }

    val lookupSymbols = HashSet<LookupSymbol>()
    lookupSymbols.addAll(affectedJavaSymbols)
    lookupSymbols.addAll(classpathChanges.lookupSymbols)

    if (lookupSymbols.any()) {
        val dirtyFilesFromLookups = mapLookupSymbolsToFiles(caches.lookupCache, lookupSymbols, logAction, relativePathOrCanonical)
        dirtyFiles.addAll(dirtyFilesFromLookups)
    }

    val dirtyClassesFqNames = classpathChanges.fqNames.flatMap { withSubtypes(it, listOf(caches.incrementalCache)) }
    if (dirtyClassesFqNames.any()) {
        val dirtyFilesFromFqNames = mapClassesFqNamesToFiles(listOf(caches.incrementalCache), dirtyClassesFqNames, logAction, relativePathOrCanonical)
        dirtyFiles.addAll(dirtyFilesFromFqNames)
    }

    if (dirtySourcesSinceLastTimeFile.exists()) {
        val files = dirtySourcesSinceLastTimeFile.readLines().map(::File).filter(File::exists)
        if (files.isNotEmpty()) {
            logger.kotlinDebug { "Source files added since last compilation: $files" }
        }

        dirtyFiles.addAll(files)
    }

    return CompilationMode.Incremental(dirtyFiles)
}

private fun getClasspathChanges(
        modifiedClasspath: List<File>,
        lastBuildInfo: BuildInfo?,
        logger: Logger,
        artifactDifferenceRegistry: ArtifactDifferenceRegistry?
): ChangesEither {
    if (modifiedClasspath.isEmpty()) {
        logger.kotlinDebug { "No classpath changes" }
        return ChangesEither.Known()
    }
    if (artifactDifferenceRegistry == null) {
        logger.kotlinDebug { "No artifact history provider" }
        return ChangesEither.Unknown()
    }

    val lastBuildTS = lastBuildInfo?.startTS
    if (lastBuildTS == null) {
        logger.kotlinDebug { "Could not determine last build timestamp" }
        return ChangesEither.Unknown()
    }

    val symbols = HashSet<LookupSymbol>()
    val fqNames = HashSet<FqName>()
    for (file in modifiedClasspath) {
        val diffs = artifactDifferenceRegistry[file]
        if (diffs == null) {
            logger.kotlinDebug { "Could not get changes for file: $file" }
            return ChangesEither.Unknown()
        }

        val (beforeLastBuild, afterLastBuild) = diffs.partition { it.buildTS < lastBuildTS }
        if (beforeLastBuild.isEmpty()) {
            logger.kotlinDebug { "No known build preceding timestamp $lastBuildTS for file $file" }
            return ChangesEither.Unknown()
        }

        afterLastBuild.forEach {
            symbols.addAll(it.dirtyData.dirtyLookupSymbols)
            fqNames.addAll(it.dirtyData.dirtyClassesFqNames)
        }
    }

    return ChangesEither.Known(symbols, fqNames)
}
