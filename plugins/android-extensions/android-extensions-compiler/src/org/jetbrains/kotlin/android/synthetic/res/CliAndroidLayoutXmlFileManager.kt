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
import org.jetbrains.kotlin.android.synthetic.AndroidXmlHandler
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.ByteArrayInputStream
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class CliAndroidLayoutXmlFileManager(
        project: Project,
        applicationPackage: String,
        variants: List<AndroidVariant>
) : AndroidLayoutXmlFileManager(project) {
    override val androidModule = AndroidModule(applicationPackage, variants)

    private val saxParser: SAXParser = initSAX()

    override fun doExtractResources(layoutGroup: AndroidLayoutGroupData, module: ModuleDescriptor): AndroidLayoutGroup {
        val layouts = layoutGroup.layouts.map { layout ->
            val resources = arrayListOf<AndroidResource>()

            val inputStream = ByteArrayInputStream(layout.virtualFile.contentsToByteArray())
            saxParser.parse(inputStream, AndroidXmlHandler { id, tag ->
                resources += parseAndroidResource(id, tag, null)
            })

            AndroidLayout(resources)
        }

        return AndroidLayoutGroup(layoutGroup.name, layouts)
    }

    private fun initSAX(): SAXParser {
        val saxFactory = SAXParserFactory.newInstance()
        saxFactory.isNamespaceAware = true
        return saxFactory.newSAXParser()
    }

}
