/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.android

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.util.HashMap
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.PsiFile
import org.jetbrains.android.facet.AndroidFacet
import com.intellij.openapi.module.ModuleManager
import java.util.ArrayList
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiManager
import org.jetbrains.jet.lang.resolve.android.AndroidResourceManager
import org.jetbrains.jet.lang.resolve.android.AndroidModuleInfo
import kotlin.properties.Delegates

public class IDEAndroidResourceManager(val module: Module) : AndroidResourceManager(module.getProject()) {

    override val androidModuleInfo: AndroidModuleInfo? by Delegates.lazy { module.androidFacet?.toAndroidModuleInfo() }

    override fun idToXmlAttribute(id: String): PsiElement? {
        var ret: PsiElement? = null
        for (file in getLayoutXmlFiles()) {
            file.accept(AndroidXmlVisitor(this, { retId, wClass, valueElement ->
                if (retId == id) ret = valueElement
            }))
        }
        return ret
    }

    private val Module.androidFacet: AndroidFacet?
        get() = AndroidFacet.getInstance(this)

    private fun AndroidFacet.toAndroidModuleInfo(): AndroidModuleInfo {
        val applicationPackage = getManifest().getPackage().toString()
        val mainResDirectory = getAllResourceDirectories().firstOrNull()?.getPath()
        return AndroidModuleInfo(applicationPackage, mainResDirectory)
    }

}
