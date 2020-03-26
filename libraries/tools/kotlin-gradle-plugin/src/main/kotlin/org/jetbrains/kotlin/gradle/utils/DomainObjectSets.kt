/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.DomainObjectSet

inline fun <reified R> DomainObjectSet<*>.matchingIsInstance(): DomainObjectSet<R> {
    return matching {
        it is R
    } as DomainObjectSet<R>
}