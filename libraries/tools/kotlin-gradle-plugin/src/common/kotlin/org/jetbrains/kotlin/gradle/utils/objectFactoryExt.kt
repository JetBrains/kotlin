/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory

internal inline fun <reified T : Named> ObjectFactory.named(name: String): T =
    named(T::class.java, name)

internal inline fun <reified T : Named> ObjectFactory.newInstance(vararg args: Any?): T =
    newInstance(T::class.java, *args)

internal inline fun <reified T> ObjectFactory.domainObjectSet() =
    domainObjectSet(T::class.java)