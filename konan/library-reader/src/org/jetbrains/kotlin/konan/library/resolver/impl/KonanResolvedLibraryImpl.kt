/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.resolver.impl

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.KonanResolvedLibrary
import org.jetbrains.kotlin.name.FqName

internal class KonanResolvedLibraryImpl(
    override val library: KonanLibrary
) : KonanResolvedLibrary {

    private val _resolvedDependencies = mutableListOf<KonanResolvedLibrary>()
    private val _emptyPackages by lazy { library.moduleHeaderData.emptyPackageList }

    override val resolvedDependencies: List<KonanResolvedLibrary>
        get() = _resolvedDependencies

    internal fun addDependency(resolvedLibrary: KonanResolvedLibrary) = _resolvedDependencies.add(resolvedLibrary)

    override var isNeededForLink: Boolean = false
        private set

    override val isDefault: Boolean
        get() = library.isDefault

    override fun markPackageAccessed(fqName: FqName) {
        if (!isNeededForLink // fast path
            && !_emptyPackages.contains(fqName.asString())
        ) {
            isNeededForLink = true
        }
    }

    override fun toString() = "library=$library, dependsOn=${_resolvedDependencies.joinToString { it.library.toString() }}"
}
