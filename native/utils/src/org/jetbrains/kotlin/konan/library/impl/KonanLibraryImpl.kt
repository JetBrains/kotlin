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
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.konan.util.substitute
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.KlibMetadataComponentLayout
import org.jetbrains.kotlin.library.impl.*
import java.nio.file.Paths

open class TargetedLibraryImpl(
    private val access: TargetedLibraryAccess<TargetedKotlinLibraryLayout>,
    private val base: BaseKotlinLibrary
) : TargetedLibrary, BaseKotlinLibrary by base {

    private val target: KonanTarget? get() = access.target

    override val targetList: List<String>
        get() = commonizerNativeTargets?.takeIf { it.isNotEmpty() }
                ?: nativeTargets.takeIf { it.isNotEmpty() }
                ?: // TODO: We have a choice: either assume it is the CURRENT TARGET
                //  or a list of ALL KNOWN targets.
                listOfNotNull(access.target?.visibleName)


    override val manifestProperties: Properties by lazy {
        val properties = base.manifestProperties
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
        get() = access.realFiles {
            it.nativeDir.listFilesOrEmpty.map { it.absolutePath }
        }
}

class KonanLibraryImpl(
    override val location: File,
    targeted: TargetedLibraryImpl,
    ir: IrLibraryImpl,
    bitcode: BitcodeLibraryImpl
) : KonanLibrary,
    BaseKotlinLibrary by targeted,
    IrLibrary by ir,
    BitcodeLibrary by bitcode {

    private val components: Map<KlibComponent.Kind<*>, KlibComponent> = KlibComponentsBuilder(
        layoutReaderFactory = KlibLayoutReaderFactory(
            klibFile = location,
            zipFileSystemAccessor = ir.access.klibZipAccessor
        )
    )
        .withMandatory(KlibMetadataComponent.Kind, ::KlibMetadataComponentLayout, ::KlibMetadataComponentImpl)
        .withOptional(KlibIrComponent.Kind.Main, KlibIrComponentLayout::createForMainIr, ::KlibIrComponentImpl)
        .withOptional(KlibIrComponent.Kind.InlinableFunctions, KlibIrComponentLayout::createForInlinableFunctionsIr, ::KlibIrComponentImpl)
        .build()

    override fun <KC : KlibMandatoryComponent> getComponent(kind: KlibMandatoryComponent.Kind<KC>): KC {
        @Suppress("UNCHECKED_CAST")
        val component = components[kind] as KC?
        return component ?: error("Unregistered component $kind")
    }

    override fun <KC : KlibOptionalComponent> getComponent(kind: KlibOptionalComponent.Kind<KC, *>): KC? {
        @Suppress("UNCHECKED_CAST")
        return components[kind] as KC?
    }

    override val linkerOpts: List<String>
        get() = manifestProperties.propertyList(KLIB_PROPERTY_LINKED_OPTS, escapeInQuotes = true)
}


fun createKonanLibrary(
    libraryFilePossiblyDenormalized: File,
    component: String,
    target: KonanTarget? = null,
    isDefault: Boolean = false,
    zipFileSystemAccessor: ZipFileSystemAccessor? = null,
): KonanLibrary {
    // KT-58979: The following access classes need normalized klib path to correctly provide symbols from resolved klibs
    val libraryFile = Paths.get(libraryFilePossiblyDenormalized.absolutePath).normalize().File()
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, component, zipFileSystemAccessor)
    val targetedAccess = TargetedLibraryAccess<TargetedKotlinLibraryLayout>(libraryFile, component, target, zipFileSystemAccessor)
    val irAccess = IrLibraryAccess<IrKotlinLibraryLayout>(libraryFile, component, zipFileSystemAccessor)
    val bitcodeAccess = BitcodeLibraryAccess<BitcodeKotlinLibraryLayout>(libraryFile, component, target, zipFileSystemAccessor)

    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    val targeted = TargetedLibraryImpl(targetedAccess, base)
    val ir = IrLibraryImpl(irAccess)
    val bitcode = BitcodeLibraryImpl(bitcodeAccess, targeted)

    return KonanLibraryImpl(libraryFile, targeted, ir, bitcode)
}

fun createKonanLibraryComponents(
    libraryFile: File,
    target: KonanTarget? = null,
    isDefault: Boolean = true,
    zipFileSystemAccessor: ZipFileSystemAccessor? = null,
) : List<KonanLibrary> {
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, null)
    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    return base.componentList.map {
        createKonanLibrary(libraryFile, it, target, isDefault, zipFileSystemAccessor)
    }
}
