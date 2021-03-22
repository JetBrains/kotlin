/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.resolve.jvm.NotFoundPackagesCachingStrategy

object ReplNotFoundPackagesCachingStrategy : NotFoundPackagesCachingStrategy {
    override fun chooseStrategy(isLibrarySearchScope: Boolean, qualifiedName: String): NotFoundPackagesCachingStrategy.CacheType {
        return NotFoundPackagesCachingStrategy.CacheType.NO_CACHING
    }
}
