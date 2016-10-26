/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.model

import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.config.KotlinFacetSettings

class JpsKotlinFacetModuleExtension(settings: KotlinFacetSettings) : JpsElementBase<JpsKotlinFacetModuleExtension>() {
    var settings = settings
        private set

    companion object {
        val KIND = JpsElementChildRoleBase.create<JpsKotlinFacetModuleExtension>("kotlin facet extension")
        // These must be changed in sync with KotlinFacetType.TYPE_ID and KotlinFacetType.NAME
        val FACET_TYPE_ID = "kotlin-language"
        val FACET_NAME = "Kotlin"
    }

    override fun createCopy() = JpsKotlinFacetModuleExtension(settings)

    override fun applyChanges(modified: JpsKotlinFacetModuleExtension) {
        this.settings = modified.settings
    }
}

val JpsModule.kotlinFacetExtension: JpsKotlinFacetModuleExtension?
    get() = container.getChild(JpsKotlinFacetModuleExtension.KIND)