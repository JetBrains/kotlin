/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.host

import kotlin.reflect.KClass
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptDependency
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptingHostConfigurationKeys

/**
 * The container for script evaluation configuration
 * For usages see actual code examples
 */
class ScriptingHostConfiguration(baseScriptingConfigurations: Iterable<ScriptingHostConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseScriptingConfigurations).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptingHostConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseScriptingHostConfigurations: Iterable<ScriptingHostConfiguration>) :
        ScriptingHostConfigurationKeys,
        PropertiesCollection.Builder(baseScriptingHostConfigurations)
    
    companion object : ScriptingHostConfigurationKeys
}

/**
 * The list of all dependencies required for the script base class and refinement callbacks
 */
val ScriptingHostConfigurationKeys.configurationDependencies by PropertiesCollection.key<List<ScriptDependency>>()

/**
 * The pointer to the generic "class loader" for the types used in the script configurations
 */
val ScriptingHostConfigurationKeys.getScriptingClass by PropertiesCollection.key<GetScriptingClass>()

/**
 * The interface to the generic "class loader" for the types used in the script configurations
 */
interface GetScriptingClass {
    operator fun invoke(classType: KotlinType, contextClass: KClass<*>, hostConfiguration: ScriptingHostConfiguration): KClass<*>
}

// helper method
fun ScriptingHostConfiguration.getScriptingClass(type: KotlinType, contextClass: KClass<*>): KClass<*> {
    val getClass = get(ScriptingHostConfiguration.getScriptingClass)
        ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting environment: unable to load scripting class $type")
    return getClass(type, contextClass, this)
}

// helper method
fun ScriptingHostConfiguration.getScriptingClass(type: KotlinType, context: Any): KClass<*> = getScriptingClass(type, context::class)

