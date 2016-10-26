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

import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer
import org.jetbrains.kotlin.config.KotlinFacetSettings

object JpsKotlinFacetConfigurationSerializer : JpsFacetConfigurationSerializer<JpsKotlinFacetModuleExtension>(
        JpsKotlinFacetModuleExtension.KIND,
        JpsKotlinFacetModuleExtension.FACET_TYPE_ID,
        JpsKotlinFacetModuleExtension.FACET_NAME
) {
    override fun loadExtension(
            facetConfigurationElement: Element,
            name: String,
            parent: JpsElement?,
            module: JpsModule
    ): JpsKotlinFacetModuleExtension {
        return JpsKotlinFacetModuleExtension(XmlSerializer.deserialize(facetConfigurationElement, KotlinFacetSettings::class.java)!!)
    }

    override fun saveExtension(
            extension: JpsKotlinFacetModuleExtension?,
            facetConfigurationTag: Element,
            module: JpsModule
    ) {
        XmlSerializer.serializeInto((extension as JpsKotlinFacetModuleExtension).settings, facetConfigurationTag)
    }
}