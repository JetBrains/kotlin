/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.annotations

import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptConfigurator
import kotlin.script.experimental.api.ScriptRunner
import kotlin.script.experimental.api.ScriptSelector
import kotlin.script.experimental.basic.DefaultScriptSelector
import kotlin.script.experimental.basic.DummyRunner
import kotlin.script.experimental.basic.PassThroughConfigurator

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinScript(
    val selector: KClass<out ScriptSelector> = DefaultScriptSelector::class,
    val configurator: KClass<out ScriptConfigurator> = PassThroughConfigurator::class,
    val runner: KClass<out ScriptRunner<*>> = DummyRunner::class
)
