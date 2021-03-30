/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.tasks

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.capabilities.Capability
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.tasks.*
import org.gradle.language.cpp.CppBinary
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.nativeplatform.Linkage
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.compatibleVariantIdentity
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.*

internal val Project.host
    get() = HostManager.host.visibleName

internal val Project.simpleOsName
    get() = HostManager.simpleOsName()

/** A task with a KonanTarget specified. */
abstract class KonanTargetableTask: DefaultTask() {

    @get:Input
    val konanTargetName: String
        get() = konanTarget.name

    @get:Internal
    internal lateinit var konanTarget: KonanTarget

    internal open fun init(target: KonanTarget) {
        this.konanTarget = target
    }

    val isCrossCompile: Boolean
        @Internal get() = (konanTarget != HostManager.host)

    val target: String
        @Internal get() = konanTarget.visibleName
}

/** A task building an artifact. */
abstract class KonanArtifactTask: KonanTargetableTask(), KonanArtifactSpec {

    open val artifact: File
        @OutputFile get() = destinationDir.resolve(artifactFullName)

    @Internal lateinit var destinationDir: File
    @Internal lateinit var artifactName: String
    @Internal lateinit var platformConfiguration: Configuration
    @Internal lateinit var configuration: Configuration

    protected val artifactFullName: String
        @Internal get() = "$artifactPrefix$artifactName$artifactSuffix"

    val artifactPath: String
        @Internal get() = artifact.canonicalPath

    protected abstract val artifactSuffix: String
        @Internal get

    protected abstract val artifactPrefix: String
        @Internal get

    internal open fun init(config: KonanBuildingConfig<*>, destinationDir: File, artifactName: String, target: KonanTarget) {
        super.init(target)
        this.destinationDir = destinationDir
        this.artifactName = artifactName
        configuration = project.configurations.maybeCreate("artifact$artifactName")
        platformConfiguration = project.configurations.create("artifact${artifactName}_${target.name}")
        platformConfiguration.extendsFrom(configuration)
        platformConfiguration.attributes{
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.NATIVE_LINK))
            attribute(CppBinary.LINKAGE_ATTRIBUTE, Linkage.STATIC)
            attribute(CppBinary.OPTIMIZED_ATTRIBUTE, false)
            attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, false)
            attribute(Attribute.of("org.gradle.native.kotlin.platform", String::class.java), target.name)
        }

        val artifactNameWithoutSuffix = artifact.name.removeSuffix("$artifactSuffix")
        project.pluginManager.withPlugin("maven-publish") {
            platformConfiguration.artifacts.add(object: PublishArtifact {
                override fun getName(): String = artifactNameWithoutSuffix
                override fun getExtension() = if (artifactSuffix.startsWith('.')) artifactSuffix.substring(1) else artifactSuffix
                override fun getType() = artifactSuffix
                override fun getClassifier():String? = target.name
                override fun getFile() = artifact
                override fun getDate() = Date(artifact.lastModified())
                override fun getBuildDependencies(): TaskDependency =
                        DefaultTaskDependency().apply { add(this@KonanArtifactTask) }
            })
            val objectFactory = project.objects
            val linkUsage = objectFactory.named(Usage::class.java, Usage.NATIVE_LINK)
            val konanSoftwareComponent = config.mainVariant
            val variantName = "${artifactNameWithoutSuffix}_${target.name}"
            val context = DefaultUsageContext(object:UsageContext {
                override fun getUsage(): Usage = linkUsage
                override fun getName(): String = "${variantName}Link"
                override fun getCapabilities(): MutableSet<out Capability> = mutableSetOf()
                override fun getDependencies(): MutableSet<out ModuleDependency> = mutableSetOf()
                override fun getDependencyConstraints(): MutableSet<out DependencyConstraint> = mutableSetOf()
                override fun getArtifacts(): MutableSet<out PublishArtifact> = platformConfiguration.allArtifacts
                override fun getAttributes(): AttributeContainer = platformConfiguration.attributes
                override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()
            }, platformConfiguration.allArtifacts, platformConfiguration)
            konanSoftwareComponent.addVariant(
                compatibleVariantIdentity(
                    project,
                    variantName,
                    project.provider{ artifactName },
                    project.provider{ project.group.toString() },
                    project.provider{ project.version.toString() },
                    false,
                    false,
                    target,
                    context,
                    null
                )
            )
        }
    }

    fun dependencies(closure: Closure<Unit>) {
        if (konanTarget in project.konanTargets)
            project.dependencies(closure)
    }
    // DSL.

    override fun artifactName(name: String) {
        artifactName = name
    }

    fun destinationDir(dir: Any) {
        destinationDir = project.file(dir)
    }
}

/** Task building an artifact with libraries */
abstract class KonanArtifactWithLibrariesTask: KonanArtifactTask(), KonanArtifactWithLibrariesSpec {
    @Nested
    val libraries = KonanLibrariesSpec(this, project)

    @Input
    var noDefaultLibs = false

    @Input
    var noEndorsedLibs = false

    // DSL

    override fun libraries(closure: Closure<Unit>) = libraries(ConfigureUtil.configureUsing(closure))
    override fun libraries(action: Action<KonanLibrariesSpec>) = libraries { action.execute(this) }
    override fun libraries(configure: KonanLibrariesSpec.() -> Unit) { libraries.configure() }

    override fun noDefaultLibs(flag: Boolean) {
        noDefaultLibs = flag
    }

    override fun noEndorsedLibs(flag: Boolean) {
        noEndorsedLibs = flag
    }
}
