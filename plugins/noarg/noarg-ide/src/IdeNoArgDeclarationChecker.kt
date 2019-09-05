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

package org.jetbrains.kotlin.noarg.ide

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.annotation.plugin.ide.AnnotationBasedLightClassApplicabilityExtension
import org.jetbrains.kotlin.annotation.plugin.ide.CachedAnnotationNames
import org.jetbrains.kotlin.annotation.plugin.ide.getAnnotationNames
import org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor
import org.jetbrains.kotlin.noarg.diagnostic.AbstractNoArgDeclarationChecker
import org.jetbrains.kotlin.psi.KtModifierListOwner

internal val NO_ARG_ANNOTATION_OPTION_PREFIX =
    "plugin:${NoArgCommandLineProcessor.PLUGIN_ID}:${NoArgCommandLineProcessor.ANNOTATION_OPTION.optionName}="

class IdeNoArgApplicabilityExtension(project: Project) :
    AnnotationBasedLightClassApplicabilityExtension(project, NO_ARG_ANNOTATION_OPTION_PREFIX)

class IdeNoArgDeclarationChecker(project: Project) : AbstractNoArgDeclarationChecker() {

    private val cachedAnnotationNames = CachedAnnotationNames(project, NO_ARG_ANNOTATION_OPTION_PREFIX)

    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?): List<String> =
        cachedAnnotationNames.getAnnotationNames(modifierListOwner)
}