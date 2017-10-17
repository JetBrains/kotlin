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
            """, KonanProject.PROGRAM)
        }
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.taskPaths(TaskOutcome.FAILED).contains(project.defaultCompilationTask())
    }

}
