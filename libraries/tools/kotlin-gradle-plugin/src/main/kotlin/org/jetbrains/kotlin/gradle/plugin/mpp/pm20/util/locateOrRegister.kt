/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider

fun <T> NamedDomainObjectContainer<T>.locateOrRegister(name: String, configure: T.() -> Unit = {}): NamedDomainObjectProvider<T> {
    return if (name in names) named(name, configure)
    else register(name, configure)
}
