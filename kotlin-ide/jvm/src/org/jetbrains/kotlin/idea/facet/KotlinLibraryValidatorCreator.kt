/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.impl.ui.libraries.DelegatingLibrariesValidatorContext
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorValidator
import com.intellij.facet.ui.FacetValidatorsManager
import org.jetbrains.kotlin.idea.facet.KotlinFacetEditorGeneralTab.EditorComponent

class KotlinLibraryValidatorCreator : KotlinFacetValidatorCreator() {
    override fun create(
        editor: EditorComponent,
        validatorsManager: FacetValidatorsManager,
        editorContext: FacetEditorContext
    ): FacetEditorValidator = FrameworkLibraryValidatorWithDynamicDescription(
        DelegatingLibrariesValidatorContext(editorContext),
        validatorsManager,
        "kotlin"
    ) { editor.getChosenPlatform() }
}