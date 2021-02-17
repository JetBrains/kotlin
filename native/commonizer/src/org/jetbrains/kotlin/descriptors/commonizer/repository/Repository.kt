/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.repository

import org.jetbrains.kotlin.descriptors.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeLibrary

internal interface Repository {
    fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary>
}

internal operator fun Repository.plus(other: Repository): Repository {
    if (this is CompositeRepository) {
        return CompositeRepository(this.repositories + other)
    }
    return CompositeRepository(listOf(this, other))
}

private class CompositeRepository(val repositories: Iterable<Repository>) : Repository {
    override fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary> {
        return repositories.map { it.getLibraries(target) }.flatten().toSet()
    }
}

internal object EmptyRepository : Repository {
    override fun getLibraries(target: LeafCommonizerTarget): Set<NativeLibrary> {
        return emptySet()
    }
}
