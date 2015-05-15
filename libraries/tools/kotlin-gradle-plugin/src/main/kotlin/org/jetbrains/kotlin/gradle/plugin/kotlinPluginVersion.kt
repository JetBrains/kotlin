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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.logging.Logger
import java.io.FileNotFoundException
import java.util.*

private fun Any.loadKotlinVersionFromResource(log: Logger): String {
    log.debug("Loading version information")
    val props = Properties()
    val propFileName = "project.properties"
    val inputStream = javaClass.getClassLoader()!!.getResourceAsStream(propFileName)

    if (inputStream == null) {
        throw FileNotFoundException("property file '" + propFileName + "' not found in the classpath")
    }

    props.load(inputStream)

    val projectVersion = props["project.version"] as String
    log.debug("Found project version [$projectVersion]")
    return projectVersion
}