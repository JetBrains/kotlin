/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.attributes

import org.gradle.api.Named
import org.gradle.api.attributes.MultipleCandidatesDetails

internal fun MultipleCandidatesDetails<out Named>.getCandidateNames() = candidateValues.map { it.name }

internal fun <T : Named> MultipleCandidatesDetails<T>.chooseCandidateByName(name: String?) {
    closestMatch(candidateValues.single { it.name == name })
}