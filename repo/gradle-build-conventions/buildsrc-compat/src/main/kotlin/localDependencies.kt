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

// usages in build scripts are not tracked properly
@file:Suppress("unused")

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.kotlin.dsl.extra
import java.io.File

private fun Project.kotlinBuildLocalDependenciesDir(): File =
    (findProperty("kotlin.build.dependencies.dir") as String?)?.let(::File)
        ?: rootProject.gradle.gradleUserHomeDir.resolve("kotlin-build-dependencies")

private fun Project.kotlinBuildLocalRepoDir(): File = kotlinBuildLocalDependenciesDir().resolve("repo")

fun Project.ideModuleName() = when (IdeVersionConfigurator.currentIde.kind) {
    Ide.Kind.AndroidStudio -> "android-studio-ide"
    Ide.Kind.IntelliJ -> "ideaIC"
}

private fun Project.ideModuleVersion(forIde: Boolean) = when (IdeVersionConfigurator.currentIde.kind) {
    Ide.Kind.AndroidStudio -> rootProject.findProperty("versions.androidStudioBuild")
    Ide.Kind.IntelliJ -> {
        if (forIde) {
            intellijSdkVersionForIde()
                ?: error("Please specify 'attachedIntellijVersion' in your local.properties")
        } else {
            rootProject.findProperty("versions.intellijSdk")
        }
    }
}

fun Project.intellijSdkVersionForIde(): String? {
    val majorVersion = kotlinBuildProperties.getOrNull("attachedIntellijVersion") as? String ?: return null
    return rootProject.findProperty("versions.intellijSdk.forIde.$majorVersion") as? String
}

fun RepositoryHandler.kotlinBuildLocalRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = project.kotlinBuildLocalRepoDir()
    url = baseDir.toURI()

    patternLayout {
        ivy("[organisation]/[module]/[revision]/[module].ivy.xml")
        ivy("[organisation]/[module]/[revision]/ivy/[module].ivy.xml")
        ivy("[organisation]/${project.ideModuleName()}/[revision]/ivy/[module].ivy.xml") // bundled plugins

        artifact("[organisation]/[module]/[revision]/artifacts/lib/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/[module]/[revision]/artifacts/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/intellij-core/[revision]/artifacts/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/${project.ideModuleName()}/[revision]/artifacts/plugins/[module]/lib/[artifact](-[classifier]).[ext]") // bundled plugins
        artifact("[organisation]/sources/[artifact]-[revision](-[classifier]).[ext]")
        artifact("[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]")
    }

    metadataSources {
        ivyDescriptor()
    }
}

/* <used only for cooperative development for non kt-master branches> */
@JvmOverloads
fun Project.intellijDep(module: String? = null, forIde: Boolean = false) =
    "kotlin.build:${module ?: ideModuleName()}:${ideModuleVersion(forIde)}"


fun Project.intellijCoreDep() = "kotlin.build:intellij-core:${rootProject.extra["versions.intellijSdk"]}"

fun Project.intellijPluginDep(plugin: String, forIde: Boolean = false) = intellijDep(plugin, forIde)

/* </used only for cooperative development for non kt-master branches> */


/* <used only for cooperative development for kt-master branch> */
fun Project.intellijMavenDep(subsystem: String, artifact: String) =
    "com.jetbrains.$subsystem:$artifact:${ideModuleVersion(forIde = true)}"


/* </used only for cooperative development for kt-master branch> */


fun ModuleDependency.includeJars(vararg names: String, rootProject: Project? = null) {
    names.forEach {
        var baseName = it.removeSuffix(".jar")
        if (rootProject != null && rootProject.extra.has("ignore.$baseName")) {
            return@forEach
        }
        if (rootProject != null && rootProject.extra.has("versions.$baseName")) {
            baseName += "-${rootProject.extra["versions.$baseName"]}"
        }
        artifact {
            name = baseName
            type = "jar"
            extension = "jar"
        }
    }
}

// Workaround. Top-level Kotlin function in a default package can't be called from a non-default package
object IntellijRootUtils {
    fun getRepositoryRootDir(project: Project): File = with(project.rootProject) {
        return File(kotlinBuildLocalRepoDir(), "kotlin.build")
    }

    fun getIntellijRootDir(project: Project): File = with(project.rootProject) {
        return File(
            getRepositoryRootDir(this),
            "${ideModuleName()}/${ideModuleVersion(forIde = false)}/artifacts"
        )
    }
}

fun Project.ideaHomePathForTests() = rootProject.layout.buildDirectory.dir("ideaHomeForTests")

fun Project.ideaBuildNumberFileForTests() = objects.directoryProperty().value(ideaHomePathForTests()).file("build.txt")

fun Project.writeIdeaBuildNumberForTests() {
    ideaHomePathForTests().get().asFile.mkdirs()
    ideaBuildNumberFileForTests().get().asFile.writeText("IC-${rootProject.extra["versions.intellijSdk"]}")
}