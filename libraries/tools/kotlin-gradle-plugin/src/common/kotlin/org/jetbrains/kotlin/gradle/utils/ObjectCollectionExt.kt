/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider

internal inline fun <reified S> DomainObjectCollection<in S>.withType(): DomainObjectCollection<S> =
    withType(S::class.java)


@Suppress("extension_shadowed_by_member", "UNCHECKED_CAST")
inline fun <reified T : Task> TaskCollection<out Task>.named(name: String): TaskProvider<T> =
    (this as TaskCollection<T>).named(name, T::class.java)

