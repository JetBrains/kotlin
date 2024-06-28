/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.experimental.test

import org.junit.Test
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.dependencies.impl.makeExternalDependenciesResolverOptions
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver

class MavenAllResolverTest {

    @Test
    fun testProguardedApiWithDefaultValues() {
        // tests that API methods with default values are accessible with proguarded jar, see KT-60862 for details

        data class Repository(val id: String, val url: String, val user: String = "", val password: String = "")

        class DependencyResolver(private val customRepo: Repository) {
            init {
                // NOTE: due to the non-standard dependencies, some code could be red in IDE
                MavenDependenciesResolver().apply {
                    val options = mutableMapOf<String, String>()
                    addRepository(
                        RepositoryCoordinates(customRepo.url),
                        makeExternalDependenciesResolverOptions(options),
//                        null // left here to show that avoiding the default values is a workaround for KT-60862
                    )
                }
            }
        }

        DependencyResolver(
            Repository("id", "http://example.org/repository")
        )
    }
}