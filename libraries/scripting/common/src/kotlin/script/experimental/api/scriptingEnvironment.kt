/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.ChainedPropertyBag
import kotlin.script.experimental.util.typedKey

typealias ScriptingEnvironment = ChainedPropertyBag

object ScriptingEnvironmentProperties : PropertiesGroup {

    // should contain all dependencies needed for baseClass and compilationConfigurator
    val configurationDependencies by typedKey<List<ScriptDependency>>()

    // do not use configurationDependencies as script dependencies, so only the dependencies defined by compilationConfigurator will be used
    // (NOTE: in this case they should include the dependencies for the base class anyway, since this class is needed for script
    // compilation and instantiation, but compilationConfigurator could be excluded)
    val isolatedDependencies by typedKey(false)

    // a "class loader" for KotlinTypes
    val getScriptingClass by typedKey<GetScriptingClass>()
}

val ScriptingProperties.scriptingEnvironment get() = ScriptDefinitionProperties

interface GetScriptingClass {
    operator fun invoke(classType: KotlinType, contextClass: KClass<*>, environment: ScriptingEnvironment): KClass<*>
}

fun ScriptingEnvironment.getScriptingClass(type: KotlinType, contextClass: KClass<*>): KClass<*> {
    val getClass = getOrNull(ScriptingEnvironmentProperties.getScriptingClass)
        ?: throw IllegalArgumentException("Expecting 'getScriptingClass' property in the scripting environment: unable to load scripting class $type")
    return getClass(type, contextClass, this)
}

fun ScriptingEnvironment.getScriptingClass(type: KotlinType, context: Any): KClass<*> = getScriptingClass(type, context::class)

