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

package org.jetbrains.kotlin.lang.resolve.android

import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileInputStream
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import kotlin.properties.Delegates

public class CliAndroidResourceManager(
        project: Project,
        private val manifestPath: String,
        private val mainResDirectory: String
) : AndroidResourceManager(project) {

    override val androidModuleInfo by Delegates.lazy {
        AndroidModuleInfo(getApplicationPackage(manifestPath), mainResDirectory)
    }

    val saxParser: SAXParser = initSAX()

    protected fun initSAX(): SAXParser {
        val saxFactory = SAXParserFactory.newInstance()
        saxFactory.setNamespaceAware(true)
        return saxFactory.newSAXParser()
    }

    private fun getApplicationPackage(manifestPath: String): String {
        try {
            val manifestXml = File(manifestPath)
            var applicationPackage: String = ""
            try {
                saxParser.parse(FileInputStream(manifestXml), object : DefaultHandler() {
                    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                        if (localName == "manifest")
                            applicationPackage = attributes.toMap()["package"] ?: ""
                    }
                })
            }
            catch (e: Exception) {
                throw e
            }
            return applicationPackage
        }
        catch (e: Exception) {
            throw AndroidUIXmlProcessor.NoAndroidManifestFound()
        }
    }

}
