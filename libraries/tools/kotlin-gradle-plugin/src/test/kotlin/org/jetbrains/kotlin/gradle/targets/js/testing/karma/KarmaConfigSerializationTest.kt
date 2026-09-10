/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `karma.conf.js` is generated JavaScript read by karma and its plugins, so these pin the emitted JSON: property
 * order, the dropped `null`s and the empty collections that are written anyway.
 */
@OptIn(ExternalKotlinTargetApi::class)
class KarmaConfigSerializationTest {

    @Test
    fun `a configured KarmaConfig keeps its JSON shape`() {
        val config = KarmaConfig(
            basePath = "/tmp/project",
            files = mutableListOf(
                "kotlin-test-karma-runner.js",
                // karma also accepts a file pattern object, which is why the property is typed `Any`
                mapOf("pattern" to "*.wasm", "included" to false, "watched" to true),
            ),
            frameworks = mutableListOf("mocha"),
            client = KarmaClient(mutableListOf("--include", "MyTest")),
            browsers = mutableListOf("ChromeHeadless"),
            customLaunchers = mutableMapOf(
                "ChromeHeadlessNoSandbox" to CustomLauncher("ChromeHeadless").apply {
                    flags.add("--no-sandbox")
                    debug = true
                }
            ),
            customContextFile = "context.html",
            reporters = mutableListOf("mocha"),
            preprocessors = mutableMapOf("main.js" to mutableListOf("sourcemap")),
            proxies = mutableMapOf("/x" to "/y"),
            port = 9876,
            webpackCopy = mutableListOf("main.wasm.map"),
        )

        // `customDebugFile` is left unset: a null property is dropped rather than written as `null`
        assertEquals(
            """
            {
              "singleRun": true,
              "autoWatch": false,
              "basePath": "/tmp/project",
              "files": [
                "kotlin-test-karma-runner.js",
                {
                  "pattern": "*.wasm",
                  "included": false,
                  "watched": true
                }
              ],
              "frameworks": [
                "mocha"
              ],
              "client": {
                "args": [
                  "--include",
                  "MyTest"
                ]
              },
              "browsers": [
                "ChromeHeadless"
              ],
              "customLaunchers": {
                "ChromeHeadlessNoSandbox": {
                  "base": "ChromeHeadless",
                  "flags": [
                    "--no-sandbox"
                  ],
                  "debug": true
                }
              },
              "customContextFile": "context.html",
              "failOnFailingTestSuite": false,
              "failOnEmptyTestSuite": false,
              "reporters": [
                "mocha"
              ],
              "preprocessors": {
                "main.js": [
                  "sourcemap"
                ]
              },
              "proxies": {
                "/x": "/y"
              },
              "port": 9876,
              "webpackCopy": [
                "main.wasm.map"
              ]
            }
            """.trimIndent(),
            karmaConfigToJson(config)
        )
    }

    @Test
    fun `an empty KarmaConfig still writes its collections`() {
        assertEquals(
            """
            {
              "singleRun": true,
              "autoWatch": false,
              "files": [],
              "frameworks": [],
              "client": {
                "args": []
              },
              "browsers": [],
              "customLaunchers": {},
              "failOnFailingTestSuite": false,
              "failOnEmptyTestSuite": false,
              "reporters": [],
              "preprocessors": {},
              "proxies": {},
              "webpackCopy": []
            }
            """.trimIndent(),
            karmaConfigToJson(KarmaConfig())
        )
    }
}
