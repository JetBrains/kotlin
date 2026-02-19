/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus

import java.io.Serializable


data class Metric(val name: String, val value: Any, val id: UniqueId = UniqueId.DEFAULT) : Comparable<Metric>, Serializable {
    override fun compareTo(other: Metric) = compareValuesBy(this, other, { it.name }, { it.id })

    override fun toString(): String {
        val suffix = if (id.projectName == null) "" else ".${id.projectName}"
        return "${name + suffix}=$value"
    }
}