/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.ide

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.util.GradleConstants

object ParcelizeAvailability {
    fun isAvailable(element: PsiElement): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return true
        }

        val module = element.module ?: return false
        return isAvailable(module)
    }

    fun isAvailable(module: Module): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return true
        }

        val path = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return false
        val externalProjectInfo = ExternalSystemUtil.getExternalProjectInfo(module.project, GradleConstants.SYSTEM_ID, path) ?: return false
        val moduleData = GradleProjectResolverUtil.findModule(externalProjectInfo.externalProjectStructure, path) ?: return false
        return ExternalSystemApiUtil.find(moduleData, ParcelizeIdeModel.KEY)?.data?.isEnabled ?: false
    }
}