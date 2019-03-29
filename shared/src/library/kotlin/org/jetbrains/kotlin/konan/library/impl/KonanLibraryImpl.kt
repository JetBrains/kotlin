/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.konan.util.substitute

class KonanLibraryImpl(
        override val libraryFile: File,
        internal val target: KonanTarget?,
        override val isDefault: Boolean,
        private val metadataReader: MetadataReader
) : KonanLibrary {

    // For the zipped libraries inPlace gives files from zip file system
    // whereas realFiles extracts them to /tmp.
    // For unzipped libraries inPlace and realFiles are the same
    // providing files in the library directory.

    private val layout = createKonanLibraryLayout(libraryFile, target)

    override val libraryName: String by lazy { layout.inPlace { it.libraryName } }

    override val manifestProperties: Properties by lazy {
        val properties = layout.inPlace { it.manifestFile.loadProperties() }
        if (target != null) substitute(properties, defaultTargetSubstitutions(target))
        properties
    }

    override val versions: KonanLibraryVersioning
        get() = manifestProperties.readKonanLibraryVersioning()

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList(KLIB_PROPERTY_LINKED_OPTS, escapeInQuotes = true)

    override val bitcodePaths: List<String>
        get() = layout.realFiles { (it.kotlinDir.listFilesOrEmpty + it.nativeDir.listFilesOrEmpty).map { it.absolutePath } }

    override val includedPaths: List<String>
        get() = layout.realFiles { it.includedDir.listFilesOrEmpty.map { it.absolutePath } }

    override val targetList by lazy { layout.inPlace { it.targetsDir.listFiles.map { it.name } } }

    override val dataFlowGraph by lazy { layout.inPlace { it.dataFlowGraphFile.let { if (it.exists) it.readBytes() else null } } }

    override val moduleHeaderData: ByteArray by lazy { layout.inPlace { metadataReader.loadSerializedModule(it) } }

    override fun packageMetadata(packageFqName: String, partName: String) =
            layout.inPlace { metadataReader.loadSerializedPackageFragment(it, packageFqName, partName) }

    override fun packageMetadataParts(fqName: String): Set<String> =
            layout.inPlace { inPlaceLayout ->
                val fileList =
                        inPlaceLayout.packageFragmentsDir(fqName)
                                .listFiles
                                .mapNotNull {
                                    it.name
                                            .substringBeforeLast(KLIB_METADATA_FILE_EXTENSION_WITH_DOT, missingDelimiterValue = "")
                                            .takeIf { it.isNotEmpty() }
                                }

                fileList.toSortedSet().also {
                    require(it.size == fileList.size) { "Duplicated names: ${fileList.groupingBy { it }.eachCount().filter { (_, count) -> count > 1 }}" }
                }
            }

    override val irHeader: ByteArray? by lazy { layout.inPlace { library -> library.irHeader.let {
        if (it.exists) loadIrHeader() else null }
    }}

    override fun irDeclaration(index: Long, isLocal: Boolean) = loadIrDeclaraton(index, isLocal)

    override fun toString() = "$libraryName[default=$isDefault]"

    private val combinedDeclarations: CombinedIrFileReader by lazy {
        CombinedIrFileReader(layout.realFiles {
            it.irFile
        })
    }
    private fun loadIrHeader(): ByteArray =
            layout.inPlace {  it.irHeader.readBytes() }

    private fun loadIrDeclaraton(index: Long, isLocal: Boolean) =
            combinedDeclarations.declarationBytes(DeclarationId(index, isLocal))
}
