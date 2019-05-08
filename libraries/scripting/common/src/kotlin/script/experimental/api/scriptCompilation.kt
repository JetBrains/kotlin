/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptCompilationConfigurationKeys

/**
 * The container for script compilation configuration
 * For usages see {@link KotlinScript} and actual code examples
 */
open class ScriptCompilationConfiguration(baseConfigurations: Iterable<ScriptCompilationConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptCompilationConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseConfigurations: Iterable<ScriptCompilationConfiguration>) :
        ScriptCompilationConfigurationKeys,
        PropertiesCollection.Builder(baseConfigurations)

    // inherited from script compilationConfiguration for using as a keys anchor
    companion object : ScriptCompilationConfigurationKeys

    object Default : ScriptCompilationConfiguration()
}

/**
 * The script type display name
 */
val ScriptCompilationConfigurationKeys.displayName by PropertiesCollection.key<String>("Kotlin script")

/**
 * The script filename extension
 */
val ScriptCompilationConfigurationKeys.fileExtension by PropertiesCollection.key<String>("kts")

/**
 * The superclass for target script class
 */
val ScriptCompilationConfigurationKeys.baseClass by PropertiesCollection.key<KotlinType>() // script base class

/**
 * The list of classes that will be used as implicit receivers in the script body, as if the whole body is wrapped with "with" calls:
 * <pre>
 * {@code
 *   with (receiver1) {
 *     ...
 *       with (receiverN) {
 *         // script body
 *       }
 *   }
 * }
 * </pre>
 *
 * Note: the actual receivers values should be passed to the constructor of the generated script class
 */
val ScriptCompilationConfigurationKeys.implicitReceivers by PropertiesCollection.key<List<KotlinType>>() // in the order from outer to inner scope

/**
 * The map of names to the types
 */
val ScriptCompilationConfigurationKeys.providedProperties by PropertiesCollection.key<Map<String, KotlinType>>() // external variables

/**
 * The list of import expressions that will be implicitly applied to the script body, the syntax is the same as for the "import" statement
 */
val ScriptCompilationConfigurationKeys.defaultImports by PropertiesCollection.key<List<String>>()

/**
 * The list of script sources that should be compiled along with the script and imported into it
 */
val ScriptCompilationConfigurationKeys.importScripts by PropertiesCollection.key<List<SourceCode>>()

/**
 * The list of script dependencies - platform specific
 */
val ScriptCompilationConfigurationKeys.dependencies by PropertiesCollection.key<List<ScriptDependency>>()

/**
 * The list of compiler options that will be applied on script compilation, the syntax is the same as for CLI compiler
 */
val ScriptCompilationConfigurationKeys.compilerOptions by PropertiesCollection.key<List<String>>() // Q: CommonCompilerOptions instead?

/**
 * The callback that will be called on the script compilation before parsing the script
 */
val ScriptCompilationConfigurationKeys.refineConfigurationBeforeParsing by PropertiesCollection.key<RefineConfigurationUnconditionallyData>()

/**
 * The callback that will be called on the script compilation after parsing script file annotations
 */
val ScriptCompilationConfigurationKeys.refineConfigurationOnAnnotations by PropertiesCollection.key<RefineConfigurationOnAnnotationsData>()

/**
 * The callback that will be called on the script compilation immediately before starting the compilation
 */
val ScriptCompilationConfigurationKeys.refineConfigurationBeforeCompiling by PropertiesCollection.key<RefineConfigurationUnconditionallyData>()

/**
 * The list of script fragments that should be compiled intead of the whole text
 * (for use primary with the refinement callbacks)
 */
val ScriptCompilationConfigurationKeys.sourceFragments by PropertiesCollection.key<List<ScriptSourceNamedFragment>>()

/**
 * Scripting host configuration
 */
val ScriptCompilationConfigurationKeys.hostConfiguration by PropertiesCollection.key<ScriptingHostConfiguration>()

/**
 * The sub-builder DSL for configuring refinement callbacks
 */
val ScriptCompilationConfiguration.Builder.refineConfiguration get() = RefineConfigurationBuilder()


class RefineConfigurationBuilder : PropertiesCollection.Builder() {

    /**
     * The callback that will be called on the script compilation before parsing the script
     * @param handler the callback that will be called
     */
    fun beforeParsing(handler: RefineScriptCompilationConfigurationHandler) {
        set(ScriptCompilationConfiguration.refineConfigurationBeforeParsing, RefineConfigurationUnconditionallyData(handler))
    }

    /**
     * The callback that will be called on the script compilation after parsing script file annotations
     * @param annotations the list of annotations to trigger the callback on
     * @param handler the callback that will be called
     */
    fun onAnnotations(annotations: List<KotlinType>, handler: RefineScriptCompilationConfigurationHandler) {
        // TODO: implement handlers composition
        set(ScriptCompilationConfiguration.refineConfigurationOnAnnotations, RefineConfigurationOnAnnotationsData(annotations, handler))
    }

    /**
     * The callback that will be called on the script compilation after parsing script file annotations
     * @param annotations the list of annotations to trigger the callback on
     * @param handler the callback that will be called
     */
    fun onAnnotations(vararg annotations: KotlinType, handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(annotations.asList(), handler)
    }

    /**
     * The callback that will be called on the script compilation after parsing script file annotations
     * @param T the annotation to trigger the callback on
     * @param handler the callback that will be called
     */
    inline fun <reified T : Annotation> onAnnotations(noinline handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(listOf(KotlinType(T::class)), handler)
    }

    /**
     * The callback that will be called on the script compilation after parsing script file annotations
     * @param annotations the list of annotations to trigger the callback on
     * @param handler the callback that will be called
     */
    fun onAnnotations(vararg annotations: KClass<out Annotation>, handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(annotations.map { KotlinType(it) }, handler)
    }

    /**
     * The callback that will be called on the script compilation after parsing script file annotations
     * @param annotations the list of annotations to trigger the callback on
     * @param handler the callback that will be called
     */
    fun onAnnotations(annotations: Iterable<KClass<out Annotation>>, handler: RefineScriptCompilationConfigurationHandler) {
        onAnnotations(annotations.map { KotlinType(it) }, handler)
    }

    /**
     * The callback that will be called on the script compilation  immediately before starting the compilation
     * @param handler the callback that will be called
     */
    fun beforeCompiling(handler: RefineScriptCompilationConfigurationHandler) {
        set(ScriptCompilationConfiguration.refineConfigurationBeforeCompiling, RefineConfigurationUnconditionallyData(handler))
    }
}

/**
 * The refinement callback function signature
 */
typealias RefineScriptCompilationConfigurationHandler =
            (ScriptConfigurationRefinementContext) -> ResultWithDiagnostics<ScriptCompilationConfiguration>

data class RefineConfigurationUnconditionallyData(
    val handler: RefineScriptCompilationConfigurationHandler
) : Serializable {
    companion object { private const val serialVersionUID: Long = 1L }
}

data class RefineConfigurationOnAnnotationsData(
    val annotations: List<KotlinType>,
    val handler: RefineScriptCompilationConfigurationHandler
) : Serializable {
    companion object { private const val serialVersionUID: Long = 1L }
}


/**
 * The functional interface to the script compiler
 */
interface ScriptCompiler {

    /**
     * Compiles the [script] according to the [scriptCompilationConfiguration]
     * @param script the interface to the script source code
     * @param scriptCompilationConfiguration the script compilation configuration properties
     * @return result wrapper, if successful - with compiled script
     */
    suspend operator fun invoke(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>>
}

/**
 * The interface to the compiled script
 */
interface CompiledScript<out ScriptBase : Any> {

    /**
     * The location identifier for the script source, taken from SourceCode.locationId
     */
    val sourceLocationId: String?
        get() = null

    /**
     * The compilation configuration used for script compilation
     */
    val compilationConfiguration: ScriptCompilationConfiguration

    /**
     * The function that loads compiled script class
     * @param scriptEvaluationConfiguration the script evaluation configuration properties
     * @return result wrapper, if successful - with loaded KClass
     */
    suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>>

    /**
     * The scripts compiled along with this one in one module, imported or otherwise included into compilation
     */
    val otherScripts: List<CompiledScript<*>>
        get() = emptyList()

    /**
     * The name and the type of the script's result field, if any
     */
    val resultField: Pair<String, KotlinType>?
        get() = null
}
