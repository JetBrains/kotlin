/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.native.interop.indexer.*

interface Imports {
    fun getPackage(location: Location): String?

    fun isImported(headerId: HeaderId): Boolean
}


class PackageInfo(val name: String, val library: KonanLibrary)

class ImportsImpl(internal val headerIdToPackage: Map<HeaderId, PackageInfo>) : Imports {

    override fun getPackage(location: Location): String? {
        val packageInfo = headerIdToPackage[location.headerId]
                ?: return null
        accessedLibraries += packageInfo.library
        return packageInfo.name
    }

    override fun isImported(headerId: HeaderId) =
            headerId in headerIdToPackage

    private val accessedLibraries = mutableSetOf<KonanLibrary>()

    val requiredLibraries: Set<KonanLibrary>
        get() = accessedLibraries.toSet()
}

class HeaderInclusionPolicyImpl(
        private val nameGlobs: List<String>,
        private val excludeGlobs: List<String>,
) : HeaderInclusionPolicy {

    override fun excludeUnused(headerName: String?): Boolean {
        // If we don't have any filters then we should keep the header.
        if (nameGlobs.isEmpty() && excludeGlobs.isEmpty()) {
            return false
        }

        if (headerName == null) {
            // Builtins; included only if no globs are specified:
            return true
        }

        // Exclude globs have higher priority then include ones.
        return excludeGlobs.any { headerName.matchesToGlob(it) } || nameGlobs.all { !headerName.matchesToGlob(it) }
    }
}

class HeaderExclusionPolicyImpl(
        private val imports: Imports
) : HeaderExclusionPolicy {

    override fun excludeAll(headerId: HeaderId): Boolean {
        return imports.isImported(headerId)
    }

}

private fun String.matchesToGlob(glob: String): Boolean =
        java.nio.file.FileSystems.getDefault()
                .getPathMatcher("glob:$glob").matches(java.nio.file.Paths.get(this))
