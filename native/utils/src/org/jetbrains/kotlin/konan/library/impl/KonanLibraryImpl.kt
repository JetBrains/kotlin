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
import org.jetbrains.kotlin.konan.file.ZipFileSystemInPlaceAccessor
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.defaultTargetSubstitutions
import org.jetbrains.kotlin.konan.util.substitute
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.*
import java.nio.file.Paths

open class TargetedLibraryImpl(
    private val access: TargetedLibraryAccess<TargetedKotlinLibraryLayout>,
    private val base: BaseKotlinLibrary
) : TargetedLibrary, BaseKotlinLibrary by base {

    private val target: KonanTarget? get() = access.target

    override val manifestProperties: Properties by lazy {
        val properties = base.manifestProperties
        target?.let { substitute(properties, defaultTargetSubstitutions(it)) }
        properties
    }
}

class KonanLibraryImpl(
    override val location: File,
    zipFileSystemAccessor: ZipFileSystemAccessor,
    targeted: TargetedLibraryImpl,
) : KonanLibrary,
    BaseKotlinLibrary by targeted,
    TargetedLibrary by targeted {

    private val components = KlibComponentsCache(
        layoutReaderFactory = KlibLayoutReaderFactory(
            klibFile = location,
            zipFileSystemAccessor = zipFileSystemAccessor
        )
    )

    override fun <KC : KlibComponent> getComponent(kind: KlibComponent.Kind<KC, *>) = components.getComponent(kind)

    override val attributes = KlibAttributes()
}

fun createKonanLibrary(
    libraryFilePossiblyDenormalized: File,
    component: String,
    target: KonanTarget? = null,
    isDefault: Boolean = false,
    zipFileSystemAccessor: ZipFileSystemAccessor? = null,
): KonanLibrary {
    val nonNullZipFileSystemAccessor = zipFileSystemAccessor ?: ZipFileSystemInPlaceAccessor

    // KT-58979: The following access classes need normalized klib path to correctly provide symbols from resolved klibs
    val libraryFile = Paths.get(libraryFilePossiblyDenormalized.absolutePath).normalize().File()
    val baseAccess = BaseLibraryAccess<KotlinLibraryLayout>(libraryFile, component, nonNullZipFileSystemAccessor)
    val targetedAccess = TargetedLibraryAccess<TargetedKotlinLibraryLayout>(libraryFile, component, target, nonNullZipFileSystemAccessor)

    val base = BaseKotlinLibraryImpl(baseAccess, isDefault)
    val targeted = TargetedLibraryImpl(targetedAccess, base)

    return KonanLibraryImpl(libraryFile, nonNullZipFileSystemAccessor, targeted)
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
