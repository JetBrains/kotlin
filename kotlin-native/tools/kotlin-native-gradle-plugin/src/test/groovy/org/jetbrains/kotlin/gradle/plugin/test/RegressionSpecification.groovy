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

class RegressionSpecification extends BaseKonanSpecification {

    def 'KT-19916'() {
        when:
        def project = KonanProject.createEmpty(getProjectDirectory()) { KonanProject prj ->
            prj.generateSettingsFile("include ':subproject'")
            def subprojectDir = prj.projectPath.resolve("subproject").toFile()
            subprojectDir.mkdirs()
            subprojectDir.toPath().resolve("build.gradle").write("""
                dependencies {
                    libs gradleApi()
                }
            """.stripIndent())

            prj.buildFile.append("""
                subprojects {
                    apply plugin: 'konan'
                    apply plugin: Foo
                }
                
                class Foo implements Plugin<Project> {
                    void apply(Project project) {
                        project.configurations.maybeCreate("libs")
                    }
                }
            """.stripIndent())
        }

        def result = project.createRunner().withArguments('tasks').build()

        then:
        result.task(':tasks').outcome == TaskOutcome.SUCCESS
    }

    // Ensure gradle plugin fails in case of linker errors.
    def 'KT-20192'() {
        when:
        def project = KonanProject.createEmpty(getProjectDirectory()) { KonanProject prj ->
            prj.addCompilerArtifact(KonanProject.DEFAULT_ARTIFACT_NAME,"""
                external fun foo()

                fun main(args: Array<String>) {
                    foo()
                }
            """, ArtifactType.PROGRAM)
        }
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.taskPaths(TaskOutcome.FAILED).contains(project.defaultCompilationTask())
    }

}
