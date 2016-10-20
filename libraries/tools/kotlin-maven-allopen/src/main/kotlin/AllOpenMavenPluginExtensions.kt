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

package org.jetbrains.kotlin.test

import org.apache.maven.plugin.*
import org.apache.maven.project.*
import org.codehaus.plexus.component.annotations.*
import org.codehaus.plexus.logging.*
import org.jetbrains.kotlin.maven.*

val ALLOPEN_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.allopen"

@Component(role = KotlinMavenPluginExtension::class, hint = "all-open")
class KotlinAllOpenMavenPluginExtension : KotlinMavenPluginExtension {
    @Requirement
    lateinit var logger: Logger

    override fun getCompilerPluginId() = ALLOPEN_COMPILER_PLUGIN_ID

    override fun isApplicable(project: MavenProject, execution: MojoExecution) = true

    override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> {
        logger.debug("Loaded Maven plugin " + javaClass.name)
        return emptyList()
    }
}

@Component(role = KotlinMavenPluginExtension::class, hint = "spring")
class KotlinSpringMavenPluginExtension : KotlinMavenPluginExtension {
    private companion object {
        val ANNOTATIONS_ARG_NAME = "annotation"

        val SPRING_ANNOTATIONS = listOf(
                "org.springframework.stereotype.Component",
                "org.springframework.transaction.annotation.Transactional",
                "org.springframework.scheduling.annotation.Async",
                "org.springframework.cache.annotation.Cacheable"
        )
    }

    override fun getCompilerPluginId() = ALLOPEN_COMPILER_PLUGIN_ID

    @Requirement
    lateinit var logger: Logger

    override fun isApplicable(project: MavenProject, execution: MojoExecution) = true

    override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> {
        logger.debug("Loaded Maven plugin " + javaClass.name)
        return SPRING_ANNOTATIONS.map { PluginOption(ALLOPEN_COMPILER_PLUGIN_ID, ANNOTATIONS_ARG_NAME, it) }
    }
}
