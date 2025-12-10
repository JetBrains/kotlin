/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.inline

@PublishedApi
internal fun exposedForInline() {}

@PublishedApi
internal class InternalClassExposed
    @PublishedApi
    internal constructor() {

    @PublishedApi
    internal fun funExposed() {}

    @PublishedApi
    internal var propertyExposed: String? = null

    @JvmField
    @PublishedApi
    internal var fieldExposed: String? = null
}
