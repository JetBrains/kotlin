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

package org.jetbrains.kotlin.allopen.ide

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.allopen.AbstractAllOpenDeclarationAttributeAltererExtension
import org.jetbrains.kotlin.allopen.AllOpenCommandLineProcessor.Companion.ANNOTATION_OPTION
import org.jetbrains.kotlin.allopen.AllOpenCommandLineProcessor.Companion.PLUGIN_ID
import org.jetbrains.kotlin.annotation.plugin.ide.CachedAnnotationNames
import org.jetbrains.kotlin.annotation.plugin.ide.getAnnotationNames
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal val ALL_OPEN_ANNOTATION_OPTION_PREFIX = "plugin:$PLUGIN_ID:${ANNOTATION_OPTION.optionName}="

class IdeAllOpenDeclarationAttributeAltererExtension(val project: Project) :
    AbstractAllOpenDeclarationAttributeAltererExtension() {

    private val cachedAnnotationsNames = CachedAnnotationNames(project, ALL_OPEN_ANNOTATION_OPTION_PREFIX)

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> {
        return if (ApplicationManager.getApplication().isUnitTestMode) ANNOTATIONS_FOR_TESTS
        else cachedAnnotationsNames.getAnnotationNames(modifierListOwner)
    }
}