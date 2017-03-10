package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator

/**
 * We use the follwing properties:
 *      kotlin.native.home       - directory where compiler is located (aka dist in konan project output)
 */

// konanHome extension is set by downloadKonanCompiler task.
internal val Project.konanHome: String
    get() {
        assert(extensions.extraProperties.has(KonanPlugin.KONAN_HOME_PROPERTY_NAME))
        return extensions.extraProperties.get(KonanPlugin.KONAN_HOME_PROPERTY_NAME).toString()
    }

internal val Project.konanBuildRoot             get() = "${buildDir.canonicalPath}/konan"
internal val Project.konanCompilerOutputDir     get() = "${konanBuildRoot}/bin"
internal val Project.konanInteropStubsOutputDir get() = "${konanBuildRoot}/interopStubs"
internal val Project.konanInteropLibsOutputDir  get() = "${konanBuildRoot}/nativelibs"

internal val Project.konanArtifactsContainer: KonanArtifactsContainer
    get() = extensions.getByName(KonanPlugin.COMPILER_EXTENSION_NAME) as KonanArtifactsContainer
internal val Project.konanInteropContainer: KonanInteropContainer
    get() = extensions.getByName(KonanPlugin.INTEROP_EXTENSION_NAME) as KonanInteropContainer

internal val Project.konanCompilerDownloadTask  get() = tasks.getByName(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)

class KonanArtifactsContainer(val project: ProjectInternal): AbstractNamedDomainObjectContainer<KonanCompilerConfig>(
        KonanCompilerConfig::class.java,
        project.gradle.services.get(Instantiator::class.java)) {

    override fun doCreate(name: String): KonanCompilerConfig =
            KonanCompilerConfig(name, project)
}

class KonanInteropContainer(val project: ProjectInternal): AbstractNamedDomainObjectContainer<KonanInteropConfig>(
        KonanInteropConfig::class.java,
        project.gradle.services.get(Instantiator::class.java)) {

    override fun doCreate(name: String): KonanInteropConfig =
            KonanInteropConfig(name, project)
}

class KonanPlugin: Plugin<ProjectInternal> {

    companion object {
        internal const val COMPILER_EXTENSION_NAME = "konanArtifacts"
        internal const val INTEROP_EXTENSION_NAME = "konanInterop"
        internal const val KONAN_HOME_PROPERTY_NAME = "kotlin.native.home"
        internal const val KONAN_DOWNLOAD_TASK_NAME = "downloadKonanCompiler"

        // TODO: Move in config.
        internal const val KONAN_VERSION = "0.1"
    }

    /**
     * Looks for task with given name in the given project.
     * If such task isn't found, will create it. Returns created/found task.
     */
    private fun getTask(project: ProjectInternal, name: String): Task {
        var tasks = project.getTasksByName(name, false)
        assert(tasks.size <= 1)
        return if (tasks.isEmpty()) {
            project.tasks.create(name, DefaultTask::class.java)
        } else {
            tasks.single()
        }
    }

    // TODO: Create default config? what about test sources?
    override fun apply(project: ProjectInternal?) {
        if (project == null) { return }
        project.tasks.create(KONAN_DOWNLOAD_TASK_NAME, CompilerDownloadTask::class.java)
        project.extensions.add(COMPILER_EXTENSION_NAME, KonanArtifactsContainer(project))
        project.extensions.add(INTEROP_EXTENSION_NAME, KonanInteropContainer(project))
        getTask(project, "clean").doLast {
            project.delete(project.konanBuildRoot)
        }
        getTask(project, "build").apply {
            dependsOn(project.tasks.withType(KonanCompileTask::class.java))
            dependsOn(project.tasks.withType(KonanInteropTask::class.java))
        }
    }

}
