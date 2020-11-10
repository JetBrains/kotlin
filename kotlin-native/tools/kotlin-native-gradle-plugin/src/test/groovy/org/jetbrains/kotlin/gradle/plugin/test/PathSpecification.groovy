/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager

class PathSpecification extends BaseKonanSpecification {

    boolean fileExists(KonanProject project, String path) {
        project.konanBuildDir.toPath().resolve(path).toFile().exists()
    }

    def platformManager = new PlatformManager(new Distribution(KonanProject.konanHome, false, null), false)

    def 'Plugin should provide a correct path to the artifacts created'() {
        expect:
        def project = KonanProject.createEmpty(
                projectDirectory,
                platformManager.enabled.collect { t -> t.visibleName }
        ) { KonanProject it ->
            it.generateSrcFile("main.kt")
            it.generateDefFile("interop.def", "")
            it.buildFile.append("""
                konanArtifacts {
                    program('program')
                    library('library')
                    bitcode('bitcode')
                    interop('interop')
                    framework('framework')
                    dynamic('dynamic')
                }

                task checkArtifacts(type: DefaultTask) {
                    dependsOn(':build')
                    doLast {
                        for(artifact in konanArtifacts) {
                            for (target in artifact) {
                                if (!target.artifact.exists()) throw new Exception("Artifact doesn't exist. Type: \${artifact.name}, target: \${target.target}")
                            }
                        }
                        for (target in konanArtifacts.dynamic) {
                            if (!target.headerFile.exists()) throw new Exception("Header file doesn't exist. Target: \${target.target}")
                        }
                    }
                }
            """.stripIndent())
        }
        project.createRunner().withArguments("checkArtifacts").build()

    }

    def 'Plugin should create all necessary directories'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory)
        project.addCompilerArtifact("lib", "fun foo() {}", ArtifactType.LIBRARY)
        project.addCompilerArtifact("bit", "fun bar() {}", ArtifactType.BITCODE)
        project.createRunner().withArguments('build').build()

        then:
        project.konanBuildDir.toPath().resolve("bin/$KonanProject.HOST").toFile().listFiles().findAll {
            File it -> it.file && it.name.matches("^${KonanProject.DEFAULT_ARTIFACT_NAME}\\.[^.]+")
        }.size() > 0

        fileExists(project, "libs/$KonanProject.HOST/${KonanProject.DEFAULT_INTEROP_NAME}.klib")
        fileExists(project, "libs/$KonanProject.HOST/lib.klib")
        fileExists(project, "bitcode/$KonanProject.HOST/bit.bc")
    }

    def 'Plugin should stop building if the compiler classpath is empty'() {
        when:
        def project = KonanProject.create(projectDirectory)
        project.propertiesFile.write("konan.home=fakepath")
        def result = project.createRunner().withArguments('build').buildAndFail()
        def task = result.task(project.defaultCompilationTask())

        then:
        task == null || task.outcome == TaskOutcome.FAILED
    }

    def 'Plugin should stop building if the stub generator classpath is empty'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory)
        project.propertiesFile.write("konan.home=fakepath")
        def result = project.createRunner().withArguments('build').buildAndFail()
        def task = result.task(project.compilationTask(KonanProject.DEFAULT_INTEROP_NAME))

        then:
        task == null || task.outcome == TaskOutcome.FAILED
    }

    def 'Plugin should remove custom output directories'() {
        when:
        def customOutputDir = projectDirectory.toPath().resolve("foo").toFile()
        def project = KonanProject.create(projectDirectory, ArtifactType.LIBRARY) { KonanProject it ->
            it.addSetting("baseDir", customOutputDir)
        }

        def res1 = project.createRunner().withArguments("build").build()
        def artifactExistsAfterBuild = customOutputDir.toPath()
                .resolve("${KonanProject.HOST}/${KonanProject.DEFAULT_ARTIFACT_NAME}.klib").toFile()
                .exists()

        def res2 = project.createRunner().withArguments("clean").build()
        def artifactExistsAfterClean = customOutputDir.toPath()
                .resolve("${KonanProject.HOST}/${KonanProject.DEFAULT_ARTIFACT_NAME}.klib").toFile()
                .exists()

        then:
        res1.taskPaths(TaskOutcome.SUCCESS).containsAll(project.buildingTasks)
        res2.taskPaths(TaskOutcome.SUCCESS).contains(":clean")
        artifactExistsAfterBuild
        !artifactExistsAfterClean
    }
}
