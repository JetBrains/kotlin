/*
 * Copyright 2010-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache License, Version 2.0 that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.jps.build.fixtures

class JpsDependencyGraphFixture {
    private val properties = mapOf(
        "kotlin.jps.classPrefixesToLoadByParent" to "kotlin.",
        "jps.use.dependency.graph" to "true",
        "kotlin.jps.dumb.mode" to "true",
        "kotlin.jps.enable.lookups.in.dumb.mode" to "true",
        "jvm-inc-builder.test.track.mock.annotations" to "true",
    )
    private val previousPropertyValues = mutableMapOf<String, String?>()
    private val enableICFixture = EnableICFixture()

    fun setUp() {
        properties.forEach { (key, value) ->
            previousPropertyValues[key] = System.getProperty(key)
            System.setProperty(key, value)
        }
        enableICFixture.setUp()
    }

    fun tearDown() {
        try {
            enableICFixture.tearDown()
        } finally {
            previousPropertyValues.forEach { (key, value) ->
                if (value == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, value)
                }
            }
            previousPropertyValues.clear()
        }
    }
}
