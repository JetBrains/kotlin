/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.PropertiesCollection
import kotlin.script.experimental.util.getOrNull

interface ScriptingEnvironment : PropertiesCollection {

    companion object : ScriptingEnvironment {

        class Builder internal constructor() : PropertiesCollection.Builder(), ScriptingEnvironment {
            override val properties = data
        }

        fun create(body: Builder.() -> Unit): ScriptingEnvironment = Builder().apply(body)
    }
}

// should contain all dependencies needed for baseClass and compilationConfigurator
val ScriptingEnvironment.configurationDependencies by PropertiesCollection.key<List<ScriptDependency>>()

// do not use configurationDependencies as script dependencies, so only the dependencies defined by compilationConfigurator will be used
// (NOTE: in this case they should include the dependencies for the base class anyway, since this class is needed for script
// compilation and instantiation, but compilationConfigurator could be excluded)
val ScriptingEnvironment.isolatedDependencies by PropertiesCollection.key<Boolean>(false)

// a "class loader" for KotlinTypes
val ScriptingEnvironment.getScriptingClass by PropertiesCollection.key<GetScriptingClass>()

interface GetScriptingClass {
    operator fun invoke(classType: KotlinType, contextClass: KClass<*>, environment: ScriptingEnvironment): KClass<*>
}

fun ScriptingEnvironment.getScriptingClass(type: KotlinType, contextClass: KClass<*>): KClass<*> {
    val getClass = getOrNull(ScriptingEnvironment.getScriptingClass)
        ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting environment: unable to load scripting class $type")
    return getClass(type, contextClass, this)
}

fun ScriptingEnvironment.getScriptingClass(type: KotlinType, context: Any): KClass<*> = getScriptingClass(type, context::class)

