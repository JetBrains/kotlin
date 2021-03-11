/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler
import com.intellij.ide.fileTemplates.FileTemplate
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded

class KotlinCreateFromTemplateHandler : DefaultCreateFromTemplateHandler() {
    override fun handlesTemplate(template: FileTemplate) = template.isTemplateOfType(KotlinFileType.INSTANCE)

    override fun prepareProperties(props: MutableMap<String, Any>) {
        val packageName = props[FileTemplate.ATTRIBUTE_PACKAGE_NAME] as? String
        if (!packageName.isNullOrEmpty()) {
            props[FileTemplate.ATTRIBUTE_PACKAGE_NAME] = packageName.split('.').joinToString(".", transform = String::quoteIfNeeded)
        }

        val name = props[FileTemplate.ATTRIBUTE_NAME] as? String
        if (name != null) {
            props[FileTemplate.ATTRIBUTE_NAME] = name.quoteIfNeeded()
        }
    }
}