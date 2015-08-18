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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result
import java.io.ByteArrayInputStream
import org.jetbrains.kotlin.android.synthetic.AndroidXmlHandler
import org.jetbrains.kotlin.android.synthetic.parseAndroidResource
import kotlin.properties.Delegates

public class CliSyntheticFileGenerator(
        project: Project,
        private val manifestPath: String,
        private val resDirectories: List<String>,
        private val supportV4: Boolean
) : SyntheticFileGenerator(project) {

    override fun supportV4(): Boolean {
        return supportV4
    }

    override val layoutXmlFileManager: CliAndroidLayoutXmlFileManager by Delegates.lazy {
        CliAndroidLayoutXmlFileManager(project, manifestPath, resDirectories)
    }

    override val cachedSources: CachedValue<List<AndroidSyntheticFile>> by lazy {
        cachedValue {
            Result.create(generateSyntheticFiles(), ModificationTracker.NEVER_CHANGED)
        }
    }

    override fun extractLayoutResources(files: List<PsiFile>): List<AndroidResource> {
        val resources = arrayListOf<AndroidResource>()
        val handler = AndroidXmlHandler { id, widgetType -> resources.add(parseAndroidResource(id, widgetType)) }

        try {
            for (file in files) {
                val inputStream = ByteArrayInputStream(file.getVirtualFile().contentsToByteArray())
                layoutXmlFileManager.saxParser.parse(inputStream, handler)
            }
            return filterDuplicates(resources)
        }
        catch (e: Throwable) {
            LOG.error(e)
            return listOf()
        }
    }
}

