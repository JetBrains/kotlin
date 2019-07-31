/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.test

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

class ConfigurationDslTest : TestCase() {

    @Test
    fun testComposableRefinementHandlers() {
        val baseConfig = createJvmCompilationConfigurationFromTemplate<SimpleScript> {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            defaultImports(MyTestAnnotation1::class, MyTestAnnotation2::class)
            refineConfiguration {
                beforeParsing { (_, config, _) ->
                    config.with {
                        implicitReceivers(Int::class)
                    }.asSuccess()
                }
                onAnnotations<MyTestAnnotation1> { (_, config, _) ->
                    config.with {
                        providedProperties("ann1" to String::class)
                    }.asSuccess()
                }
                onAnnotations(MyTestAnnotation1::class, MyTestAnnotation2::class) { (_, config, _) ->
                    config.with {
                        providedProperties("ann12" to Int::class)
                    }.asSuccess()
                }
                onAnnotations<MyTestAnnotation2> { (_, config, _) ->
                    config.with {
                        providedProperties("ann2" to String::class)
                    }.asSuccess()
                }
                beforeCompiling { (_, config, _) ->
                    config.with {
                        compilerOptions("-version")
                    }.asSuccess()
                }
                beforeCompiling { (_, config, _) ->
                    config.with {
                        implicitReceivers(Float::class)
                    }.asSuccess()
                }
            }
        }

        Assert.assertNull(baseConfig[ScriptCompilationConfiguration.implicitReceivers])
        Assert.assertNull(baseConfig[ScriptCompilationConfiguration.providedProperties])
        Assert.assertNull(baseConfig[ScriptCompilationConfiguration.compilerOptions])

        val script = "@file:MyTestAnnotation1\nann1+ann12".toScriptSource()

        val compiledScript = runBlocking {
            JvmScriptCompiler(defaultJvmScriptingHostConfiguration).invoke(script, baseConfig).valueOrThrow()
        }
        val finalConfig = compiledScript.compilationConfiguration

        Assert.assertEquals(
            listOf(KotlinType(Int::class), KotlinType(Float::class)),
            finalConfig[ScriptCompilationConfiguration.implicitReceivers]
        )
        Assert.assertEquals(
            mapOf("ann1" to KotlinType(String::class), "ann12" to KotlinType(Int::class)),
            finalConfig[ScriptCompilationConfiguration.providedProperties]
        )
        Assert.assertEquals(
            listOf("-version"),
            finalConfig[ScriptCompilationConfiguration.compilerOptions]
        )

        val implicitReceiver1 = 10
        val implicitReceiver2 = 2.0f
        val propAnn1 = "a1"
        val propAnn12 = 12

        val baseEvalConfig = createJvmEvaluationConfigurationFromTemplate<SimpleScript> {
            refineConfigurationBeforeEvaluate { (_, config, _) ->
                config.with {
                    implicitReceivers(implicitReceiver1)
                    providedProperties("ann1" to propAnn1)
                }.asSuccess()
            }
            refineConfigurationBeforeEvaluate { (_, config, _) ->
                config.with {
                    implicitReceivers(implicitReceiver2)
                    providedProperties("ann12" to propAnn12)
                }.asSuccess()
            }
        }

        Assert.assertNull(baseEvalConfig[ScriptCompilationConfiguration.implicitReceivers])
        Assert.assertNull(baseEvalConfig[ScriptCompilationConfiguration.providedProperties])

        val evalRes = runBlocking {
            BasicJvmScriptEvaluator().invoke(compiledScript, baseEvalConfig).valueOrThrow()
        }
        val finalEvalConfig = evalRes.configuration!!

        Assert.assertEquals(
            listOf<Any>(implicitReceiver1, implicitReceiver2),
            finalEvalConfig[ScriptEvaluationConfiguration.implicitReceivers]
        )
        Assert.assertEquals(
            mapOf("ann1" to propAnn1, "ann12" to propAnn12),
            finalEvalConfig[ScriptEvaluationConfiguration.providedProperties]
        )

        Assert.assertEquals(propAnn1 + propAnn12, (evalRes.returnValue as ResultValue.Value).value)
    }

    @Test
    fun testDefaultConfiguration() {
        val script = "val x = 1".toScriptSource()

        val evalRes = runBlocking {
            JvmScriptCompiler(defaultJvmScriptingHostConfiguration).invoke(script, ScriptCompilationConfiguration()).onSuccess {
                BasicJvmScriptEvaluator().invoke(it, ScriptEvaluationConfiguration())
            }.valueOrThrow()
        }

        val scriptObj = evalRes.returnValue.scriptInstance!!

        Assert.assertEquals(Any::class.java, scriptObj::class.java.superclass)
    }

    @Test
    fun testReplaceOnlyDefault() {
        val conf = ScriptCompilationConfiguration {
            displayName("1")
            baseClass(KotlinType(Any::class))
        }

        val conf2 = conf.with {
            displayName.replaceOnlyDefault("2")
            baseClass.replaceOnlyDefault(KotlinType(Int::class))
            fileExtension.replaceOnlyDefault("ktx")
            filePathPattern.replaceOnlyDefault("x.*x")
        }

        Assert.assertEquals("1", conf2[ScriptCompilationConfiguration.displayName])
        Assert.assertEquals(KotlinType(Int::class), conf2[ScriptCompilationConfiguration.baseClass])
        Assert.assertEquals("ktx", conf2[ScriptCompilationConfiguration.fileExtension])
        Assert.assertEquals("x.*x", conf2[ScriptCompilationConfiguration.filePathPattern])
    }
}

@Target(AnnotationTarget.FILE)
annotation class MyTestAnnotation1

@Target(AnnotationTarget.FILE)
annotation class MyTestAnnotation2
