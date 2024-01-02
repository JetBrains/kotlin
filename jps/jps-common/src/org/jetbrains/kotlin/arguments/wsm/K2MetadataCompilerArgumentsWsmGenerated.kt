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

package org.jetbrains.kotlin.arguments.wsm

//import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion

//@Serializable
class K2MetadataCompilerArgumentsWsm : CommonCompilerArgumentsWsm() {
    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    var destination: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var classpath: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var moduleName: String? = null
        set(value) {
            
            field = if (value.isNullOrEmpty()) null else value
        }

    var enabledInJps = false
        set(value) {
            
            field = value
        }

    var friendPaths: Array<String>? = null
        set(value) {
            
            field = value
        }

    var refinesPaths: Array<String>? = null
        set(value) {
            
            field = value
        }



    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> =
        super.configureAnalysisFlags(collector, languageVersion).also {
            it[AnalysisFlags.metadataCompilation] = true
        }

    override fun configureExtraLanguageFeatures(map: HashMap<LanguageFeature, LanguageFeature.State>) {
        map[LanguageFeature.MultiPlatformProjects] = LanguageFeature.State.ENABLED
    }
}