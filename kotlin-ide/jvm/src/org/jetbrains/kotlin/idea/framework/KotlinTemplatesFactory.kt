/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction
import com.intellij.platform.ProjectTemplate
import com.intellij.platform.ProjectTemplatesFactory
import com.intellij.platform.templates.BuilderBasedTemplate
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

class KotlinTemplatesFactory : ProjectTemplatesFactory() {
    companion object {
        val EP_NAME = ExtensionPointName.create<ModuleBuilder>("org.jetbrains.kotlin.moduleBuilder")

        const val KOTLIN_GROUP_NAME: String = "Kotlin"
        const val KOTLIN_PARENT_GROUP_NAME = "Kotlin Group"
    }

    override fun getGroups() = arrayOf(KOTLIN_GROUP_NAME)
    override fun getGroupIcon(group: String) = KotlinIcons.SMALL_LOGO
    override fun getParentGroup(group: String?): String = KOTLIN_PARENT_GROUP_NAME
    override fun getGroupWeight(group: String?): Int = 1

    override fun createTemplates(group: String?, context: WizardContext?): Array<out ProjectTemplate> {
        val result = mutableListOf<ProjectTemplate>()
        if (isNewModuleAction()) {
            result.add(
                BuilderBasedTemplate(
                    KotlinModuleBuilder(
                        JvmPlatforms.unspecifiedJvmPlatform,
                        "JVM | IDEA",
                        KotlinJvmBundle.message("presentable.name.jvm.idea"),
                        KotlinJvmBundle.message("kotlin.project.with.a.jvm.target.based.on.the.intellij.idea.build.system"),
                        KotlinIcons.SMALL_LOGO
                    )
                )
            )
            result.add(
                BuilderBasedTemplate(
                    KotlinModuleBuilder(
                        JsPlatforms.defaultJsPlatform,
                        "JS | IDEA",
                        KotlinJvmBundle.message("presentable.name.js.idea"),
                        KotlinJvmBundle.message("kotlin.project.with.a.javascript.target.based.on.the.intellij.idea.build.system"),
                        KotlinIcons.JS
                    )
                )
            )
        }

        @Suppress("DEPRECATION")
        result.addAll(Extensions.getExtensions(EP_NAME).map { BuilderBasedTemplate(it) })
        return result.toTypedArray()
    }

    private fun isNewModuleAction(): Boolean {
        return Thread.currentThread().stackTrace.any { element ->
            element.className == NewModuleAction::class.java.name
        }
    }
}
