package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.model.ObjectFactory
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContains

class KotlinWebpackConfigTest {
    private lateinit var objects: ObjectFactory

    @Before
    fun setUp() {
        val project = ProjectBuilder.builder().build()
        objects = project.objects
    }

    @Test
    fun testWebpackConfigOutputWithDefaults() {
        val config = KotlinWebpackConfig(rules = objects.webpackRulesContainer())

        val builder = StringBuilder()
        config.appendTo(builder)

        assertContains(
            builder.toString(), """
            let config = {
              mode: 'development',
              resolve: {
                modules: [
                  "node_modules"
                ]
              },
              plugins: [],
              module: {
                rules: []
              },
              
            };
            """.trimIndent()
        )
    }

    @Test
    fun testWebpackConfigOutputWithDevServerProxy() {
        val config = KotlinWebpackConfig(
            devServer = KotlinWebpackConfig.DevServer(
                proxy = mutableListOf(
                    KotlinWebpackConfig.DevServer.Proxy(
                        context = mutableListOf("testContext"),
                        target = "testTarget",
                        ws = true,
                        changeOrigin = true,
                    )
                ),
            ), rules = objects.webpackRulesContainer()
        )

        val builder = StringBuilder()
        config.appendTo(builder)

        assertContains(
            builder.toString(), """
            // dev server
            config.devServer = {
              "open": true,
              "proxy": [
                {
                  "context": [
                    "testContext"
                  ],
                  "target": "testTarget",
                  "changeOrigin": true,
                  "ws": true
                }
              ]
            };
            """.trimIndent()
        )
    }
}