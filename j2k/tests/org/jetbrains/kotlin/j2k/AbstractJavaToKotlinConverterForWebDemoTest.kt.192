/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.MetaLanguage
import com.intellij.lang.jvm.facade.JvmElementProvider
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.augment.TypeAnnotationModifier
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.psi.util.JavaClassSupers
import junit.framework.TestCase
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader

abstract class AbstractJavaToKotlinConverterForWebDemoTest : TestCase() {
    val DISPOSABLE = Disposer.newDisposable()

    fun doTest(javaPath: String) {
        try {
            val fileContents = FileUtil.loadFile(File(javaPath), true)
            val javaCoreEnvironment: JavaCoreProjectEnvironment = setUpJavaCoreEnvironment()
            translateToKotlin(fileContents, javaCoreEnvironment.project)
        }
        finally {
            Disposer.dispose(DISPOSABLE)
        }
    }

    fun setUpJavaCoreEnvironment(): JavaCoreProjectEnvironment {
        Extensions.cleanRootArea(DISPOSABLE)
        val area = Extensions.getRootArea()

        registerExtensionPoints(area)

        val applicationEnvironment = JavaCoreApplicationEnvironment(DISPOSABLE)
        val javaCoreEnvironment = object : JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment) {
            override fun preregisterServices() {
                val projectArea = Extensions.getArea(project)
                CoreApplicationEnvironment.registerExtensionPoint(projectArea, PsiTreeChangePreprocessor.EP_NAME, PsiTreeChangePreprocessor::class.java)
                CoreApplicationEnvironment.registerExtensionPoint(projectArea, PsiElementFinder.EP_NAME, PsiElementFinder::class.java)
                CoreApplicationEnvironment.registerExtensionPoint(projectArea, JvmElementProvider.EP_NAME, JvmElementProvider::class.java)
            }
        }

        javaCoreEnvironment.project.registerService(NullableNotNullManager::class.java, object : NullableNotNullManager(javaCoreEnvironment.project) {
            override fun isNullable(owner: PsiModifierListOwner, checkBases: Boolean) = !isNotNull(owner, checkBases)
            override fun isNotNull(owner: PsiModifierListOwner, checkBases: Boolean) = true
            override fun hasHardcodedContracts(element: PsiElement): Boolean = false
            override fun getNullables() = emptyList<String>()
            override fun setNullables(vararg p0: String) = Unit
            override fun getNotNulls() = emptyList<String>()
            override fun setNotNulls(vararg p0: String) = Unit
            override fun getDefaultNullable() = ""
            override fun setDefaultNullable(defaultNullable: String) = Unit
            override fun getDefaultNotNull() = ""
            override fun setDefaultNotNull(p0: String) = Unit
            override fun setInstrumentedNotNulls(p0: List<String>) = Unit
            override fun getInstrumentedNotNulls() = emptyList<String>()
        })

        applicationEnvironment.application.registerService(JavaClassSupers::class.java, JavaClassSupersImpl::class.java)

        for (root in PathUtil.getJdkClassesRootsFromCurrentJre()) {
            javaCoreEnvironment.addJarToClassPath(root)
        }
        val annotations: File? = findAnnotations()
        if (annotations != null && annotations.exists()) {
            javaCoreEnvironment.addJarToClassPath(annotations)
        }
        return javaCoreEnvironment
    }

    private fun registerExtensionPoints(area: ExtensionsArea) {
        CoreApplicationEnvironment.registerExtensionPoint(area, BinaryFileStubBuilders.EP_NAME, FileTypeExtensionPoint::class.java)
        CoreApplicationEnvironment.registerExtensionPoint(area, FileContextProvider.EP_NAME, FileContextProvider::class.java)

        CoreApplicationEnvironment.registerExtensionPoint(area, MetaDataContributor.EP_NAME, MetaDataContributor::class.java)
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class.java)
        CoreApplicationEnvironment.registerExtensionPoint(area, JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider::class.java)

        CoreApplicationEnvironment.registerExtensionPoint(area, ContainerProvider.EP_NAME, ContainerProvider::class.java)
        CoreApplicationEnvironment.registerExtensionPoint(area, ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler::class.java)

        CoreApplicationEnvironment.registerExtensionPoint(area, TypeAnnotationModifier.EP_NAME, TypeAnnotationModifier::class.java)
        CoreApplicationEnvironment.registerExtensionPoint(area, MetaLanguage.EP_NAME, MetaLanguage::class.java)
        CoreApplicationEnvironment.registerExtensionPoint(area, JavaModuleSystem.EP_NAME, JavaModuleSystem::class.java)
    }

    fun findAnnotations(): File? {
        var classLoader = JavaToKotlinTranslator::class.java.classLoader
        while (classLoader != null) {
            val loader = classLoader
            if (loader is URLClassLoader) {
                for (url in loader.urLs) {
                    if ("file" == url.protocol && url.file!!.endsWith("/annotations.jar")) {
                        return File(url.file!!)
                    }
                }
            }
            classLoader = classLoader.parent
        }
        return null
    }
}
