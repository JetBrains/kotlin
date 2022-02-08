/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.intellij

import com.intellij.openapi.extensions.ExtensionPointName
import java.io.File
import kotlin.script.experimental.host.ScriptDefinition
import kotlin.script.experimental.host.ScriptingHostConfiguration

/**
 * IntelliJ extension point for providing script definitions
 *
 * The scripting infrastructure will load this extension point on project instantiation, and then collect all definitions
 * provided by the extension point, combining 3 ways, depending on the data returned from the interface members:
 *  - for all FQNs of classes returned from the [getDefinitionClasses] function, it will load the class with the classpath from
 *    the [getDefinitionsClassPath] and create it's definition from the KotlinScript annotation
 *  - if [useDiscovery] method returns true, the classpath returned by [getDefinitionsClassPath] will be scanned for the discovery
 *    markers, and found script definitions will be loaded and created the same way as ones returned from [getDefinitionClasses]
 * After collecting all definitions will be passed to the [provideDefinitions] for possible modifications. The implementation
 * may also remove or add new definitions at this point.
 * Processed definitions are provided to the scripting support infrastructure.
 */
interface ScriptDefinitionsProvider {
    /**
     * A display name used to identify particular providers
     */
    val id: String

    /**
     * Should return a list of the FQNs of the script definition template classes to load explicitly, if any
     */
    fun getDefinitionClasses(): Iterable<String>

    /**
     * Should return a classpath required for loading script definition template classes
     */
    fun getDefinitionsClassPath(): Iterable<File>

    /**
     * if returns true, the IntelliJ will scan the classpath from [getDefinitionClasses] to discover script definition templates
     * using definition markers in the "META-INF/kotlin/script/templates/" folder
     */
    fun useDiscovery(): Boolean

    /**
     * The callback to update/add/remove script definitions after loading, if needed
     */
    @Suppress("DEPRECATION") // To be replaced with -Xjvm-default=all-compatibility.
    @JvmDefault
    fun provideDefinitions(
        baseHostConfiguration: ScriptingHostConfiguration,
        loadedScriptDefinitions: List<ScriptDefinition>
    ): Iterable<ScriptDefinition> = loadedScriptDefinitions

    companion object {
        val EP_NAME: ExtensionPointName<ScriptDefinitionsProvider> =
            ExtensionPointName.create("org.jetbrains.kotlin.scriptDefinitionsProvider")
    }
}
