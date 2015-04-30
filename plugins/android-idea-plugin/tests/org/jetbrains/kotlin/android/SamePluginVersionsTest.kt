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

package org.jetbrains.kotlin.android

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.regex.Pattern

public class SamePluginVersionsTest {

    private companion object {
        val IDEA_VERSION_PATTERN = Pattern.compile("<idea-version since-build=\"([0-9\\.]+)\" until-build=\"([0-9\\.]+)\"/>")
        val PLUGIN_VERSION_PATTERN = Pattern.compile("<version>(.+?)</version>")
    }

    private fun extractIdeaVersion(pluginFile: File, pluginXml: String): Pair<String, String> {
        val matcher = IDEA_VERSION_PATTERN.matcher(pluginXml)
        assertTrue("Can't find tag <idea-version> in ${pluginFile.getAbsolutePath()}", matcher.find())
        return matcher.group(1) to matcher.group(2)
    }

    private fun extractPluginVersion(pluginFile: File, pluginXml: String): String {
        val matcher = PLUGIN_VERSION_PATTERN.matcher(pluginXml)
        assertTrue("Can't find tag <version> in ${pluginFile.getAbsolutePath()}", matcher.find())
        return matcher.group(1)
    }

    Test
    public fun sameIdeaVersionsTest() {
        val mainPluginXmlFile = File("idea/src/META-INF/plugin.xml")
        val androidExtensionsPluginXmlFile = File("plugins/android-idea-plugin/src/META-INF/plugin.xml")

        assertTrue("Main Kotlin IDEA plugin.xml file does not exist", mainPluginXmlFile.exists())
        assertTrue("Android Extensions IDEA plugin.xml file does not exist", androidExtensionsPluginXmlFile.exists())

        val mainPluginXml = mainPluginXmlFile.readText()
        val mainPluginVersion = extractPluginVersion(mainPluginXmlFile, mainPluginXml)
        if ("@snapshot@" == mainPluginVersion) return

        val androidExtensionsPluginXml = androidExtensionsPluginXmlFile.readText()

        val mainIdeaVersion = extractIdeaVersion(mainPluginXmlFile, mainPluginXml)
        val androidExtensionsIdeaVersion = extractIdeaVersion(androidExtensionsPluginXmlFile, androidExtensionsPluginXml)

        assertEquals("IDEA dependency versions are different (main: $mainIdeaVersion, androidExtensions: $androidExtensionsIdeaVersion)",
                     mainIdeaVersion, androidExtensionsIdeaVersion)
    }

}