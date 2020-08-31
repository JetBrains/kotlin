/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode

import kotlin.script.experimental.api.*

/**
 * Creates a version of the compilation configuration that includes the provided generated code
 *
 * For example:
 *
 * ```
 * val newConfiguration = configuration.provide {
 *      dataClass("MyClass") {
 *          property<Int>("secret")
 *
 *          method("checkSecret") { value: Int ->
 *              val secret by property<Int>()
 *              secret == value
 *          }
 *      }
 * }
 * ```
 *
 *  @param init Initializer for generated code using the Generated Code Builder DSL
 */
fun ScriptCompilationConfiguration.provide(
    init: GeneratedCode.Builder.() -> Unit
): ScriptCompilationConfiguration {
    return with {
        provide(init)
    }
}

/**
 * Include generated code into your Script Compilation Configuration
 *
 * For example:
 *
 * ```
 * val configuration = ScriptCompilationConfiguration {
 *      provide {
 *          dataClass("MyClass") {
 *              property<Int>("secret")
 *
 *              method("checkSecret") { value: Int ->
 *                  val secret by property<Int>()
 *                  secret == value
 *              }
 *          }
 *      }
 * }
 * ```
 *
 *  @param init Initializer for generated code using the Generated Code Builder DSL
 */
fun ScriptCompilationConfiguration.Builder.provide(init: GeneratedCode.Builder.() -> Unit) {
    provide(GeneratedCode(init))
}

/**
 * Creates a version of the compilation configuration that includes the provided generated code
 *
 *  @param generatedCode Code generated that should be included in all scripts
 */
fun ScriptCompilationConfiguration.provide(
    generatedCode: GeneratedCode
): ScriptCompilationConfiguration {
    return with {
        provide(generatedCode)
    }
}

/**
 * Creates a version of the compilation configuration that includes the provided generated code
 *
 *  @param generatedCode Code generated that should be included in all scripts
 */
fun ScriptCompilationConfiguration.Builder.provide(generatedCode: GeneratedCode) {
    provide(generatedCode.artifacts())
}

/**
 * Creates a version of the compilation configuration that includes the provided generated code
 *
 *  @param artifacts Artifacts containing generated scripts and relevant imports necessary
 */
fun ScriptCompilationConfiguration.Builder.provide(artifacts: GeneratedCodeArtifacts) {
    defaultImports.append(artifacts.imports)
    importScripts.append(artifacts.importScripts)
}