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

class ScriptingHostConfiguration(baseScriptingEnvironments: Iterable<ScriptingHostConfiguration>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseScriptingEnvironments).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseConfigurations: ScriptingHostConfiguration, body: Builder.() -> Unit = {}
    ) : this(baseConfigurations.asIterable(), body)

    class Builder internal constructor(baseScriptingEnvironments: Iterable<ScriptingHostConfiguration>) :
        ScriptingHostConfigurationKeys,
        PropertiesCollection.Builder(baseScriptingEnvironments)
    
    companion object : ScriptingHostConfigurationKeys
}

// should contain all dependencies needed for baseClass and compilationConfigurator
val ScriptingHostConfigurationKeys.configurationDependencies by PropertiesCollection.key<List<ScriptDependency>>()

// do not use configurationDependencies as script dependencies, so only the dependencies defined by compilationConfigurator will be used
// (NOTE: in this case they should include the dependencies for the base class anyway, since this class is needed for script
// compilation and instantiation, but compilationConfigurator could be excluded)
val ScriptingHostConfigurationKeys.isolatedDependencies by PropertiesCollection.key<Boolean>(false)

// a "class loader" for KotlinTypes
val ScriptingHostConfigurationKeys.getScriptingClass by PropertiesCollection.key<GetScriptingClass>()


interface GetScriptingClass {
    operator fun invoke(classType: KotlinType, contextClass: KClass<*>, hostConfiguration: ScriptingHostConfiguration): KClass<*>
}

fun ScriptingHostConfiguration.getScriptingClass(type: KotlinType, contextClass: KClass<*>): KClass<*> {
    val getClass = get(ScriptingHostConfiguration.getScriptingClass)
        ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting environment: unable to load scripting class $type")
    return getClass(type, contextClass, this)
}

fun ScriptingHostConfiguration.getScriptingClass(type: KotlinType, context: Any): KClass<*> = getScriptingClass(type, context::class)

