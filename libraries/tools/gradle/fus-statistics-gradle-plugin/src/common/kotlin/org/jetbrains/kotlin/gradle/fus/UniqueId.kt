/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus

import java.io.Serializable

/**
 * UniqueId allows identifying where the metrics are expected to be logged, additionally validating and aggregating the values further.
 */
interface UniqueId : Comparable<UniqueId>, Serializable {
    companion object {
        val DEFAULT = object : UniqueId {
            override val projectName: String? = null
        }
    }

    val projectName: String?

    override fun compareTo(other: UniqueId): Int = compareValuesBy(this, other, { it.projectName }, { it.javaClass.simpleName })

}

data class TaskId(override val projectName: String?, val taskName: String) : UniqueId {
    override fun compareTo(other: UniqueId): Int = when (other) {
        is TaskId -> compareValuesBy(this, other, { it.projectName }, { it.taskName })
        else -> super.compareTo(other)
    }
}

data class BuildServiceId(override val projectName: String?, val buildServiceName: String) : UniqueId {
    override fun compareTo(other: UniqueId): Int = when (other) {
        is BuildServiceId -> compareValuesBy(this, other, { it.projectName }, { it.buildServiceName })
        else -> super.compareTo(other)
    }
}
