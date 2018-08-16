/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.PropertiesCollection

interface ScriptingEnvironmentKeys

class ScriptingEnvironment(baseScriptingEnvironments: Iterable<ScriptingEnvironment>, body: Builder.() -> Unit) :
    PropertiesCollection(Builder(baseScriptingEnvironments).apply(body).data) {

    constructor(body: Builder.() -> Unit = {}) : this(emptyList(), body)
    constructor(
        vararg baseScriptingEnvironments: ScriptingEnvironment, body: Builder.() -> Unit = {}
    ) : this(baseScriptingEnvironments.asIterable(), body)

    class Builder internal constructor(baseScriptingEnvironments: Iterable<ScriptingEnvironment>) : 
        ScriptingEnvironmentKeys, 
        PropertiesCollection.Builder(baseScriptingEnvironments)
    
    companion object : ScriptingEnvironmentKeys
}

// should contain all dependencies needed for baseClass and compilationConfigurator
val ScriptingEnvironmentKeys.configurationDependencies by PropertiesCollection.key<List<ScriptDependency>>()

// do not use configurationDependencies as script dependencies, so only the dependencies defined by compilationConfigurator will be used
// (NOTE: in this case they should include the dependencies for the base class anyway, since this class is needed for script
// compilation and instantiation, but compilationConfigurator could be excluded)
val ScriptingEnvironmentKeys.isolatedDependencies by PropertiesCollection.key<Boolean>(false)

// a "class loader" for KotlinTypes
val ScriptingEnvironmentKeys.getScriptingClass by PropertiesCollection.key<GetScriptingClass>()


interface GetScriptingClass {
    operator fun invoke(classType: KotlinType, contextClass: KClass<*>, environment: ScriptingEnvironment): KClass<*>
}

fun ScriptingEnvironment.getScriptingClass(type: KotlinType, contextClass: KClass<*>): KClass<*> {
    val getClass = get(ScriptingEnvironment.getScriptingClass)
        ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting environment: unable to load scripting class $type")
    return getClass(type, contextClass, this)
}

fun ScriptingEnvironment.getScriptingClass(type: KotlinType, context: Any): KClass<*> = getScriptingClass(type, context::class)

