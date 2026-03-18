/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.jvmOverloads

@JvmOverloads
public fun fileLevelPublic(value: String = "") {  }

@JvmOverloads
internal fun fileLevelInternal(value: String = "") {  }

@Suppress("OVERLOADS_PRIVATE")
@JvmOverloads
private fun fileLevelPrivate(value: String = "") {  }

@PublishedApi
@JvmOverloads
internal fun fileLevelPublished(value: String = "") {  }

class Container {
    @JvmOverloads
    public fun publicFun(value: String = "") {  }

    @JvmOverloads
    internal fun internalFun(value: String = "") {  }

    @Suppress("OVERLOADS_PRIVATE")
    @JvmOverloads
    private fun privateFun(value: String = "") {  }

    @PublishedApi
    @JvmOverloads
    internal fun publishedFun(value: String = "") {  }
}