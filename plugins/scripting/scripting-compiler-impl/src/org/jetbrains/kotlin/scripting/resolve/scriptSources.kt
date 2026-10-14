/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.resolve

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty
import kotlin.script.experimental.util.PropertiesCollection

// The script source implementations and the (legacy) configuration wrapper, which do not depend on the FIR.
// The refinement entry points, which do, are located in the scripting compiler plugin (see `refineCompilationConfiguration.kt` there).

/**
 * The implementation of the SourceCode for a script located in a virtual file
 */
open class VirtualFileScriptSource(val virtualFile: VirtualFile, private val preloadedText: String? = null) :
    FileBasedScriptSource() {
    override val file: File get() = File(virtualFile.path)
    override val externalLocation: URL get() = URL(virtualFile.url)
    override val text: String by lazy { preloadedText ?: virtualFile.inputStream.bufferedReader().use { it.readText() } }
    override val name: String? get() = virtualFile.name
    override val locationId: String? get() = virtualFile.path

    override fun equals(other: Any?): Boolean =
        this === other || (other as? VirtualFileScriptSource)?.let { virtualFile == it.virtualFile } == true

    override fun hashCode(): Int = virtualFile.hashCode()
}

/**
 * The implementation of the SourceCode for a script located in a KtFile
 */
open class KtFileScriptSource(val ktFile: KtFile, preloadedText: String? = null) :
    VirtualFileScriptSource(ktFile.virtualFile ?: ktFile.originalFile.virtualFile ?: ktFile.viewProvider.virtualFile, preloadedText) {

    override val text: String by lazy { preloadedText ?: ktFile.text }
    override val name: String? get() = ktFile.name

    override fun equals(other: Any?): Boolean =
        this === other || (other as? KtFileScriptSource)?.let { ktFile == it.ktFile } == true

    override fun hashCode(): Int = ktFile.hashCode()
}

class ScriptLightVirtualFile(name: String, private val _path: String?, text: String) :
    LightVirtualFile(
        name,
        KotlinLanguage.INSTANCE,
        StringUtil.convertLineSeparators(text)
    ) {

    init {
        charset = StandardCharsets.UTF_8
    }

    override fun getPath(): String = _path ?: if (parent != null) parent.path + "/" + name else name

    override fun getCanonicalPath() = path
}

class GenericKtSourceFileScriptSource(val ktSourceFile: KtSourceFile) : SourceCode {
    override val text: String by lazy { ktSourceFile.getContentsAsStream().use { it.reader().readText() } }
    override val name: String get() = ktSourceFile.name
    override val locationId: String? get() = ktSourceFile.path
}

open class LazyTextScriptSource(
    override val name: String? = null,
    locationId: String? = null,
    getSource: () -> String,
) : SourceCode {

    override val text: String by lazy { getSource() }

    override val locationId: String? = locationId ?: name ?: "\$${System.identityHashCode(this).toHexString()}.kts"

    override fun equals(other: Any?): Boolean =
        this === other || (other as? StringScriptSource)?.let { name == it.name && locationId == it.locationId } == true

    override fun hashCode(): Int = name.hashCode() * 17 + locationId.hashCode() * 23
}

@Deprecated("Use APIs that return ScriptCompilationConfiguration or ResultWithDiagnostics<ScriptCompilationConfiguration> instead")
class ScriptCompilationConfigurationWrapper(
    val script: SourceCode,
    val configuration: ScriptCompilationConfiguration?,
) {

    // optimizing most common ops for the IDE
    // TODO: consider dropping after complete migration
    val dependenciesClassPath: List<File> by lazy {
        configuration?.get(ScriptCompilationConfiguration.dependencies).toClassPathOrEmpty()
    }

    val dependenciesSources: List<File> by lazy {
        configuration?.get(ScriptCompilationConfiguration.ide.dependenciesSources).toClassPathOrEmpty()
    }

    val javaHome: File?
        get() = configuration?.get(ScriptCompilationConfiguration.jvm.jdkHome)

    val defaultImports: List<String>
        get() = configuration?.get(ScriptCompilationConfiguration.defaultImports).orEmpty()

    val importedScripts: List<SourceCode>
        get() = (configuration?.get(ScriptCompilationConfiguration.resolvedImportScripts) ?: configuration?.get(
            ScriptCompilationConfiguration.importScripts
        )).orEmpty()

    override fun equals(other: Any?): Boolean = script == (other as? ScriptCompilationConfigurationWrapper)?.script

    override fun hashCode(): Int = script.hashCode()
}

@Deprecated("Use APIs that return ScriptCompilationConfiguration or ResultWithDiagnostics<ScriptCompilationConfiguration> instead")
typealias ScriptCompilationConfigurationResult = ResultWithDiagnostics<ScriptCompilationConfigurationWrapper>

// TODO consider dropping and using disambiguation of the sources collection (KT-83502)
val ScriptCompilationConfigurationKeys.resolvedImportScripts by PropertiesCollection.key<List<SourceCode>>(isTransient = true)
