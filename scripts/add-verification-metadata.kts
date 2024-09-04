import java.io.File

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Adds checksums from a directory maven repo to verification-metadata.xml
 */

if (args.size != 2) {
    println("Usage: kotlinc -script add-verification-metadata.kts path/to/bootstrap/repo path/to/verification-metadata.xml")
    System.exit(1)
}

val (repoPath, metadataPath) = args

val repoFile = File(repoPath)
val metadataFile = File(metadataPath)

data class MetadataItem(
    val group: String,
    val name: String,
    val version: String,
    val artifactName: String,
    val md5: String
) {
    fun toXML(): String = """
            <component group="$group" name="$name" version="$version">
               <artifact name="$artifactName">
                   <md5 value="$md5" origin="Bootstrapping Test"/>
               </artifact>
            </component>
        """.trimIndent()
}

val md5ChecksumExtension = "md5"
val checksumFiles = repoFile.walkTopDown()
    .filter { it.name.endsWith(".$md5ChecksumExtension") }
    .filter {
        it.name.substringBefore(".$md5ChecksumExtension")
            .substringAfterLast(".") in listOf("jar", "klib", "gz", "zip")
    }
    .filterNot {
        it.name.substringAfterLast("-")
            .substringBefore(".") in listOf("sources", "javadoc")
    }

val repoMetadata = checksumFiles.map { checksum ->
    val pathElements =
        checksum.canonicalPath.substringAfter(repoFile.canonicalPath).trim('/').split('/')

    val group = pathElements.dropLast(3).joinToString(".")
    val (name, version, checksumFileName) = pathElements.takeLast(3)
    val artifactName = checksumFileName.substringBefore(".$md5ChecksumExtension")
    MetadataItem(group, name, version, artifactName, checksum.readText())
}

val repoMetadataText = repoMetadata.joinToString("\n") {
    it.toXML().prependIndent("      ")
}

val componentsTag = "<components>"
val originalMetadataText = metadataFile.readText()
val componentsIndex = originalMetadataText.indexOf(componentsTag)
if (componentsIndex == -1) {
    error("$componentsTag is not found in $metadataPath")
}

val metadataText = StringBuilder(originalMetadataText)
    .insert(componentsIndex + componentsTag.length, "\n$repoMetadataText")
    .toString()

metadataFile.writeText(metadataText)
