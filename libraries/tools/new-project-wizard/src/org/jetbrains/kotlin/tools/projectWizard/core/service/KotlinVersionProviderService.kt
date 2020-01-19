/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

interface KotlinVersionProviderService : WizardService {
    fun getKotlinVersion(): Version
}

class KotlinVersionProviderServiceImpl : KotlinVersionProviderService, IdeaIndependentWizardService {
    override fun getKotlinVersion(): Version = DEFAULT

    companion object {
        val DEFAULT = Version.fromString("1.3.61")
    }
}


val Version.kotlinVersionKind
    get() = when {
        "eap" in toString().toLowerCase() -> KotlinVersionKind.EAP
        "dev" in toString().toLowerCase() -> KotlinVersionKind.DEV
        "m" in toString().toLowerCase() -> KotlinVersionKind.M
        else -> KotlinVersionKind.STABLE
    }

enum class KotlinVersionKind(val repository: Repository?) {
    STABLE(repository = null),
    EAP(repository = Repositories.KOTLIN_EAP_BINTRAY),
    DEV(repository = Repositories.KOTLIN_DEV_BINTRAY),
    M(repository = Repositories.KOTLIN_EAP_BINTRAY)
}