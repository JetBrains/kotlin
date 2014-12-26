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

import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader
import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.psi.PsiModifierListOwner
import com.intellij.openapi.extensions.Extensions
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.psi.impl.compiled.ClsStubBuilderFactory
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.codeInsight.ContainerProvider
import com.intellij.openapi.fileTypes.ContentBasedFileSubstitutor
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.psi.FileContextProvider
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.PsiElementFinder
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.psi.PsiElement

public object JavaToKotlinTranslator {
    val DISPOSABLE = Disposer.newDisposable()

    private fun createFile(text: String): PsiFile? {
        val javaCoreEnvironment: JavaCoreProjectEnvironment? = setUpJavaCoreEnvironment()
        return PsiFileFactory.getInstance(javaCoreEnvironment?.getProject()!!).createFileFromText("test.java", JavaLanguage.INSTANCE, text)
    }

    fun setUpJavaCoreEnvironment(): JavaCoreProjectEnvironment {
        Extensions.cleanRootArea(DISPOSABLE)
        val area = Extensions.getRootArea()

        registerExtensionPoints(area)

        val applicationEnvironment = JavaCoreApplicationEnvironment(DISPOSABLE)
        val javaCoreEnvironment = object : JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment) {
            override fun preregisterServices() {
                val projectArea = Extensions.getArea(getProject())
                CoreApplicationEnvironment.registerExtensionPoint(projectArea, PsiTreeChangePreprocessor.EP_NAME, javaClass<PsiTreeChangePreprocessor>())
                CoreApplicationEnvironment.registerExtensionPoint(projectArea, PsiElementFinder.EP_NAME, javaClass<PsiElementFinder>())
            }
        };

        javaCoreEnvironment.getProject().registerService(javaClass<NullableNotNullManager>(), object : NullableNotNullManager() {
            override fun isNullable(owner: PsiModifierListOwner, checkBases: Boolean) = !isNotNull(owner, checkBases)
            override fun isNotNull(owner: PsiModifierListOwner, checkBases: Boolean) = true
            override fun hasHardcodedContracts(element: PsiElement): Boolean = false
        })

        for (root in PathUtil.getJdkClassesRoots()) {
            javaCoreEnvironment.addJarToClassPath(root)
        }
        val annotations: File? = findAnnotations()
        if (annotations != null && annotations.exists()) {
            javaCoreEnvironment.addJarToClassPath(annotations)
        }
        return javaCoreEnvironment
    }

    private fun registerExtensionPoints(area: ExtensionsArea) {
        CoreApplicationEnvironment.registerExtensionPoint(area, ContentBasedFileSubstitutor.EP_NAME, javaClass<ContentBasedFileSubstitutor>())
        CoreApplicationEnvironment.registerExtensionPoint(area, BinaryFileStubBuilders.EP_NAME, javaClass<FileTypeExtensionPoint<Any>>())
        CoreApplicationEnvironment.registerExtensionPoint(area, FileContextProvider.EP_NAME, javaClass<FileContextProvider>())
        //
        CoreApplicationEnvironment.registerExtensionPoint(area, MetaDataContributor.EP_NAME, javaClass<MetaDataContributor>())
        CoreApplicationEnvironment.registerExtensionPoint(area, ClsStubBuilderFactory.EP_NAME, javaClass<ClsStubBuilderFactory<PsiFile>>())
        CoreApplicationEnvironment.registerExtensionPoint(area, PsiAugmentProvider.EP_NAME, javaClass<PsiAugmentProvider>())
        CoreApplicationEnvironment.registerExtensionPoint(area, JavaMainMethodProvider.EP_NAME, javaClass<JavaMainMethodProvider>())
        //
        CoreApplicationEnvironment.registerExtensionPoint(area, ContainerProvider.EP_NAME, javaClass<ContainerProvider>())
        CoreApplicationEnvironment.registerExtensionPoint(area, ClsCustomNavigationPolicy.EP_NAME, javaClass<ClsCustomNavigationPolicy>())
        CoreApplicationEnvironment.registerExtensionPoint(area, ClassFileDecompilers.EP_NAME, javaClass<ClassFileDecompilers.Decompiler>())
    }

    fun prettify(code: String?): String {
        if (code == null) {
            return ""
        }

        return code
                .trim()
                .replaceAll("\r\n", "\n")
                .replaceAll(" \n", "\n")
                .replaceAll("\n ", "\n")
                .replaceAll("\n+", "\n")
                .replaceAll(" +", " ")
                .trim()
    }

    fun findAnnotations(): File? {
        var classLoader = javaClass<JavaToKotlinTranslator>().getClassLoader()
        while (classLoader != null) {
            val loader = classLoader
            if (loader is URLClassLoader) {
                for (url in loader.getURLs()) {
                    if ("file" == url.getProtocol() && url.getFile()!!.endsWith("/annotations.jar")) {
                        return File(url.getFile()!!)
                    }
                }
            }
            classLoader = classLoader?.getParent()
        }
        return null
    }

    fun generateKotlinCode(javaCode: String): String {
        val file = createFile(javaCode)
        if (file is PsiJavaFile) {
            val converter = JavaToKotlinConverter(file.getProject(), ConverterSettings.defaultSettings, FilesConversionScope(listOf(file)), EmptyReferenceSearcher, EmptyResolverForConverter)
            return prettify(converter.elementsToKotlin(listOf(file to null))[0])
        }
        return ""
    }
}

fun main(args: Array<String>) {
    if (args.size() == 1) {
        try {
            val kotlinCode = JavaToKotlinTranslator.generateKotlinCode(args[0])
            if (kotlinCode.isEmpty()) {
                println("EXCEPTION: generated code is empty.")
            }
            else {
                println(kotlinCode)
            }
        }
        catch (e: Exception) {
            println("EXCEPTION: " + e.getMessage())
        }
    }
    else {
        println("EXCEPTION: wrong number of arguments (should be 1).")
    }
}

//used in Kotlin Web Demo
public fun translateToKotlin(code: String): String {
    return JavaToKotlinTranslator.generateKotlinCode(code)
}
