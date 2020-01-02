/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.service

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class ServicesManager(private val services: List<WizardService>, private val serviceSelector: (List<WizardService>) -> WizardService?) {
    @Suppress("UNCHECKED_CAST")
    fun <S : WizardService> serviceByClass(klass: KClass<S>, filter: (S) -> Boolean): S? =
        services.filter { service ->
            service::class.isSubclassOf(klass) && filter(service as S)
        }.let(serviceSelector) as? S

    fun withServices(services: List<WizardService>) =
        ServicesManager(this.services + services, serviceSelector)
}