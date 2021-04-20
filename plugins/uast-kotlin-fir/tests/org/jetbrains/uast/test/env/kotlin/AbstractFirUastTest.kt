/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.env.kotlin

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.registerServiceInstance
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.common.kotlin.FirUastPluginSelection
import org.jetbrains.uast.kotlin.FirKotlinUastResolveProviderService
import org.jetbrains.uast.kotlin.firKotlinUastPlugin
import org.jetbrains.uast.kotlin.internal.FirCliKotlinUastResolveProviderService
import java.io.File

abstract class AbstractFirUastTest : KotlinLightCodeInsightFixtureTestCase(), FirUastPluginSelection {
    private fun registerExtensionPointAndServiceIfNeeded() {
        if (!isFirPlugin) return

        val area = Extensions.getRootArea()
        CoreApplicationEnvironment.registerExtensionPoint(
            area,
            UastLanguagePlugin.extensionPointName,
            UastLanguagePlugin::class.java
        )
        area.getExtensionPoint(UastLanguagePlugin.extensionPointName)
            .registerExtension(firKotlinUastPlugin, testRootDisposable)
        project.registerServiceInstance(
            FirKotlinUastResolveProviderService::class.java,
            FirCliKotlinUastResolveProviderService()
        )
    }

    override fun setUp() {
        super.setUp()
        registerExtensionPointAndServiceIfNeeded()
    }

    override fun isFirPlugin(): Boolean = true

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    private fun getVirtualFile(filepath: String): VirtualFile {
        val vfs = VirtualFileManager.getInstance().getFileSystem(URLUtil.FILE_PROTOCOL)
        return vfs.findFileByPath(filepath)!!
    }

    abstract fun check(filePath: String, file: UFile)

    protected fun doCheck(filePath: String, checkCallback: (String, UFile) -> Unit = { _filePath, file -> check(_filePath, file) }) {
        val virtualFile = getVirtualFile(filePath)

        val testName = filePath.substring(filePath.lastIndexOf('/') + 1).removeSuffix(".kt")
        val psiFile = myFixture.configureByText(virtualFile.name, File(virtualFile.canonicalPath!!).readText())
        val uFile = UastFacade.convertElementWithParent(psiFile, null) ?: error("Can't get UFile for $testName")
        checkCallback(filePath, uFile as UFile)
    }
}
