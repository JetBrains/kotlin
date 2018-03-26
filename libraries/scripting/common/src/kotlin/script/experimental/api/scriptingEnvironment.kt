/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass

typealias ScriptingEnvironment = HeterogeneousMap

object ScriptingEnvironmentParams {

    // required by definitions that extract data from script base class annotations
    val baseClass by typedKey<KClass<*>>()

    open class Builder : HeterogeneousMapBuilder()
}

