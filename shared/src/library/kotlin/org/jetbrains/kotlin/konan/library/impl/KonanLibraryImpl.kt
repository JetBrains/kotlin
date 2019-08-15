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
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.*

open class TargetedLibraryImpl(
    private val access: TargetedLibraryAccess<TargetedKotlinLibraryLayout>,
    private val base: BaseKotlinLibrary
) : TargetedLibrary, BaseKotlinLibrary by base {

    private val target: KonanTarget? get() = access.target

    override val targetList by lazy {
        access.inPlace { it: TargetedKotlinLibraryLayout ->
            it.targetsDir.listFiles.map {
                it.name
            }
        }
    }

    override val manifestProperties: Properties by lazy {
        val properties = access.inPlace {
            it.manifestFile.loadProperties()
        }
        target?.let { substitute(properties, defaultTargetSubstitutions(it)) }
        properties
    }

    override val includedPaths: List<String>
        get() = access.realFiles {
            it.includedDir.listFilesOrEmpty.map { it.absolutePath }
        }
}

open class BitcodeLibraryImpl(
    private val access: BitcodeLibraryAccess<BitcodeKotlinLibraryLayout>,
    targeted: TargetedLibrary
) : BitcodeLibrary, TargetedLibrary by targeted {
    override val bitcodePaths: List<String>
        get() = access.realFiles { it: BitcodeKotlinLibraryLayout ->
            (it.kotlinDir.listFilesOrEmpty + it.nativeDir.listFilesOrEmpty).map { it.absolutePath }
        }
}

class KonanLibraryImpl(
    targeted: TargetedLibraryImpl,
    metadata: MetadataLibraryImpl,
    ir: IrLibraryImpl,
    bitcode: BitcodeLibraryImpl
) : KonanLibrary,
    BaseKotlinLibrary by targeted,
    MetadataLibrary by metadata,
    IrLibrary by ir,
    BitcodeLibrary by bitcode {

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList(KLIB_PROPERTY_LINKED_OPTS, escapeInQuotes = true)
}


fun createKonanLibrary(
    libraryFile: File,
    target: KonanTarget? = null,
    isDefault: Boolean = false
): KonanLibrary {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile)
    val targetedAccess = TargetedLibraryAccess<TargetedKotlinLibraryLayout>(libraryFile, target)
    val metadataAccess = MetadataLibraryAccess<MetadataKotlinLibraryLayout>(libraryFile)
    val irAccess = IrLibraryAccess<IrKotlinLibraryLayout>(libraryFile)
    val bitcodeAccess = BitcodeLibraryAccess<BitcodeKotlinLibraryLayout>(libraryFile, target)

    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    val targeted = TargetedLibraryImpl(targetedAccess, base)
    val metadata = MetadataLibraryImpl(metadataAccess)
    val ir = IrMonoliticLibraryImpl(irAccess)
    val bitcode = BitcodeLibraryImpl(bitcodeAccess, targeted)

    return KonanLibraryImpl(targeted, metadata, ir, bitcode)
}