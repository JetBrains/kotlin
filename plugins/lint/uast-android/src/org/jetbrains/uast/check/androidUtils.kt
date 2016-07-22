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
@file:JvmName("UastAndroidUtils")
package org.jetbrains.uast.check

import com.android.tools.klint.detector.api.Location
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.uast.UElement
import org.jetbrains.uast.psi.PsiElementBacked

fun UElement?.getLocation(): Location? {
    val psiElementBacked = this as? PsiElementBacked ?: return null
    val psiElement = psiElementBacked.psi
    val psiFile = psiElement?.containingFile ?: return null
    val vfile = psiFile.virtualFile
    val file = VfsUtilCore.virtualToIoFile(vfile)
    val range = psiElement.textRange ?: return null
    return Location.create(file, psiFile.text, range.startOffset, range.endOffset)
}