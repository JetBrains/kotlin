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

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.module.Module

fun Module.getSpecialAnnotations(prefix: String): List<String> {
    val kotlinFacet = org.jetbrains.kotlin.idea.facet.KotlinFacet.get(this) ?: return emptyList()
    val commonArgs = kotlinFacet.configuration.settings.compilerArguments ?: return emptyList()

    return commonArgs.pluginOptions
        ?.filter { it.startsWith(prefix) }
        ?.map { it.substring(prefix.length) }
        ?: emptyList()
}