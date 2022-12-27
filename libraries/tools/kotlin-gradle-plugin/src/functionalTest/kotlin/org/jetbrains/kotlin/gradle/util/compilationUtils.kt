/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

val NamedDomainObjectCollection<out KotlinCompilation<*>>.main: KotlinCompilation<*> get() = getByName("main")
val NamedDomainObjectCollection<out KotlinCompilation<*>>.test: KotlinCompilation<*> get() = getByName("test")
