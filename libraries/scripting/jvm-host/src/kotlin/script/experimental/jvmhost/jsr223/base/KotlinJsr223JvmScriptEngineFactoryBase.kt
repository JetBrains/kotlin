/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.script.experimental.jvmhost.jsr223.base

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import kotlin.script.experimental.jvm.jsr223.base.KotlinJsr223ScriptEngineFactoryBase

/**
 * A [KotlinJsr223ScriptEngineFactoryBase] that reports the bundled Kotlin compiler's version.
 * This is the only part of the JSR-223 factory boilerplate that requires the compiler on the
 * classpath, and therefore the only part that is not in the `kotlin-scripting-jvm` artifact.
 */
abstract class KotlinJsr223JvmScriptEngineFactoryBase : KotlinJsr223ScriptEngineFactoryBase() {

    override fun getLanguageVersion(): String = KotlinCompilerVersion.VERSION
    override fun getEngineVersion(): String = KotlinCompilerVersion.VERSION
}
