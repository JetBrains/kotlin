/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package test

import java.net.URL
import java.util.jar.Attributes
import java.util.jar.Manifest

val LIBRARIES = listOf(
        "kotlin-stdlib",
        "kotlin-stdlib-common",
        "kotlin-stdlib-jdk7",
        "kotlin-stdlib-jdk8",
        "kotlin-reflect",
        "kotlin-script-runtime"
)

const val KOTLIN_VERSION = "Kotlin-Version"
const val KOTLIN_RUNTIME_COMPONENT = "Kotlin-Runtime-Component"
const val KOTLIN_RUNTIME_COMPONENT_VALUE = "Main"
val KOTLIN_VERSION_VALUE = with(KotlinVersion.CURRENT) { "$major.$minor" }

fun main(args: Array<String>) {
    val implementationTitles = arrayListOf<String>()

    val versionValues = hashMapOf<URL, String?>()
    val runtimeComponentValues = hashMapOf<URL, String?>()

    for (resource in object {}.javaClass.classLoader.getResources("META-INF/MANIFEST.MF")) {
        val manifest = resource.openStream().use(::Manifest).mainAttributes
        val title = manifest.getValue(Attributes.Name.IMPLEMENTATION_TITLE) ?: continue
        if ("kotlin" !in title.toLowerCase()) continue

        implementationTitles.add(title)
        versionValues[resource] = manifest.getValue(KOTLIN_VERSION)
        runtimeComponentValues[resource] = manifest.getValue(KOTLIN_RUNTIME_COMPONENT)
    }

    val errors = StringBuilder()

    val uncheckedLibraries = LIBRARIES - implementationTitles
    if (uncheckedLibraries.isNotEmpty()) {
        errors.appendln("These libraries are not found in the dependencies of this test project, thus their manifests cannot be checked. " +
                        "Please ensure they are listed in the <dependencies> section in the corresponding pom.xml:\n$uncheckedLibraries")
        errors.appendln("(all found libraries: $implementationTitles)")
        errors.appendln()
    }

    fun renderEntry(entry: Map.Entry<URL, String?>) = buildString {
        val (url, value) = entry
        append(url)
        if (value != null) append(" (actual value: $value)")
        else append(" (attribute is not found)")
    }

    val incorrectVersionValues = versionValues.filterValues { it != KOTLIN_VERSION_VALUE }
    if (incorrectVersionValues.isNotEmpty()) {
        errors.appendln("Manifests at these locations do not have the correct value of the $KOTLIN_VERSION attribute ($KOTLIN_VERSION_VALUE). " +
                        "Please ensure that kotlin_language_version in libraries/build.gradle corresponds to the value in kotlin.KotlinVersion:")
        incorrectVersionValues.entries.joinTo(errors, "\n", transform = ::renderEntry)
        errors.appendln()
        errors.appendln()
    }

    val incorrectRuntimeComponentValues = runtimeComponentValues.filterValues { it != KOTLIN_RUNTIME_COMPONENT_VALUE }
    if (incorrectRuntimeComponentValues.isNotEmpty()) {
        errors.appendln("Manifests at these locations do not have the correct value of the $KOTLIN_RUNTIME_COMPONENT attribute ($KOTLIN_RUNTIME_COMPONENT_VALUE):")
        incorrectRuntimeComponentValues.entries.joinTo(errors, "\n", transform = ::renderEntry)
    }

    if (errors.isNotEmpty()) {
        throw AssertionError(errors)
    }
}
