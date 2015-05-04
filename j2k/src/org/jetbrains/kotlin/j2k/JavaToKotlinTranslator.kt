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
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.codeInsight.ContainerProvider
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.psi.FileContextProvider
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.PsiElementFinder
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

public object JavaToKotlinTranslator {
    private fun createFile(text: String, project: Project): PsiFile? {
        return PsiFileFactory.getInstance(project).createFileFromText("test.java", JavaLanguage.INSTANCE, text)
    }

    public fun prettify(code: String?): String {
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

    public fun generateKotlinCode(javaCode: String, project: Project): String {
        val file = createFile(javaCode, project)
        if (file is PsiJavaFile) {
            val converter = JavaToKotlinConverter(file.getProject(), ConverterSettings.defaultSettings, EmptyReferenceSearcher, EmptyResolverForConverter, null)
            return prettify(converter.elementsToKotlin(listOf(JavaToKotlinConverter.InputElement(file, null))).results.single()!!.text)
        }
        return ""
    }
}

//used in Kotlin Web Demo
public fun translateToKotlin(code: String, project: Project): String {
    return JavaToKotlinTranslator.generateKotlinCode(code, project)
}
