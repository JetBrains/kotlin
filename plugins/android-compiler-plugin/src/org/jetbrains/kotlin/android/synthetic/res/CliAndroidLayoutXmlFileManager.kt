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
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

public class CliAndroidLayoutXmlFileManager(
        project: Project,
        private val applicationPackage: String,
        private val variants: List<AndroidVariant>
) : AndroidLayoutXmlFileManager(project) {

    override val androidModule by lazy { AndroidModule(applicationPackage, variants) }

    val saxParser: SAXParser = initSAX()

    protected fun initSAX(): SAXParser {
        val saxFactory = SAXParserFactory.newInstance()
        saxFactory.isNamespaceAware = true
        return saxFactory.newSAXParser()
    }

}
