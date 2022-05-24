/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import java.io.Serializable

sealed class KpmModuleIdentifier(open val moduleClassifier: String?) : Serializable

data class KpmLocalModuleIdentifier(
    val buildId: String,
    val projectId: String,
    override val moduleClassifier: String?
) : KpmModuleIdentifier(moduleClassifier) {
    companion object {
        private const val SINGLE_BUILD_ID = ":"
    }

    override fun toString(): String =
        "project '$projectId'" +
                moduleClassifier?.let { " / $it" }.orEmpty() +
                buildId.takeIf { it != SINGLE_BUILD_ID }?.let { "(build '$it')" }.orEmpty()
}

data class KpmMavenModuleIdentifier(
    val group: String,
    val name: String,
    override val moduleClassifier: String?
) : KpmModuleIdentifier(moduleClassifier) {
    override fun toString(): String = "$group:$name" + moduleClassifier?.let { " / $it" }.orEmpty()
}
