/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtScript
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.templates.standard.ScriptTemplateWithArgs

// Legacy definition, will be obsolete soon
// TODO: make deprecated and drop usages where possible
open class KotlinScriptDefinition(open val template: KClass<out Any>) : UserDataHolderBase() {

    open val name: String = "Kotlin Script"

    // TODO: consider creating separate type (subtype? for kotlin scripts)
    open val fileType: LanguageFileType = KotlinFileType.INSTANCE

    open val annotationsForSamWithReceivers: List<String>
        get() = emptyList()

    open fun isScript(fileName: String): Boolean =
        fileName.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

    open fun getScriptName(script: KtScript): Name =
        NameUtils.getScriptNameForFile(script.containingKtFile.name)

    open val fileExtension: String
        get() = "kts"

    // Target platform for script, ex. "JVM", "JS", "NATIVE"
    open val platform: String
        get() = "JVM"

    open val dependencyResolver: DependenciesResolver get() = DependenciesResolver.NoDependencies

    open val acceptedAnnotations: List<KClass<out Annotation>> get() = emptyList()

    @Deprecated("temporary workaround for missing functionality, will be replaced by the new API soon")
    open val additionalCompilerArguments: Iterable<String>? = null

    @Suppress("DEPRECATION")
    open val scriptExpectedLocations: List<kotlin.script.experimental.location.ScriptExpectedLocation> =
        listOf(
            kotlin.script.experimental.location.ScriptExpectedLocation.SourcesOnly,
            kotlin.script.experimental.location.ScriptExpectedLocation.TestsOnly
        )

    open val implicitReceivers: List<KType> get() = emptyList()

    open val providedProperties: List<Pair<String, KType>> get() = emptyList()

    open val targetClassAnnotations: List<Annotation> get() = emptyList()

    open val targetMethodAnnotations: List<Annotation> get() = emptyList()
}

object StandardScriptDefinition : KotlinScriptDefinition(ScriptTemplateWithArgs::class)

