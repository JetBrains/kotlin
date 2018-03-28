/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.script.experimental.util.ChainedPropertyBag
import kotlin.script.experimental.util.typedKey

typealias ScriptingEnvironment = ChainedPropertyBag

object ScriptingEnvironmentProperties {

    // required by definitions that extract data from script base class annotations
    val baseClass by typedKey<KClass<*>>()
}

