package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class TaskSpecification extends BaseKonanSpecification {

    def 'Configs should allow user to add dependencies to them'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY)
        project.buildFile.append("""
            task beforeInterop(type: DefaultTask) { doLast { println("Before Interop") } }
            task beforeCompilation(type: DefaultTask) { doLast { println("Before compilation") } }
        """.stripIndent())
        project.addSetting(KonanProject.DEFAULT_INTEROP_NAME,"dependsOn", "beforeInterop")
        project.addSetting("dependsOn", "beforeCompilation")
        def result = project.createRunner().withArguments('build').build()

        then:
        def beforeInterop = result.task(":beforeInterop")
        beforeInterop != null && beforeInterop.outcome == TaskOutcome.SUCCESS
        def beforeCompilation = result.task(":beforeCompilation")
        beforeCompilation != null && beforeCompilation.outcome == TaskOutcome.SUCCESS
    }

    def 'Compiler should print time measurements if measureTime flag is set'() {
        when:
        def project = KonanProject.create(projectDirectory, ArtifactType.LIBRARY)
        project.addSetting("measureTime", "true")
        def result = project.createRunner().withArguments('build').build()

        then:
        result.output.findAll(~/FRONTEND:\s+\d+\s+msec/).size() == 1
        result.output.findAll(~/BACKEND:\s+\d+\s+msec/).size() == 1
    }

    def 'Plugin should support headerFilterAdditionalSearchPrefix option for cinterop'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("interopLib", "headers=foo.h\nheaderFilter=foo.h bar.h", ArtifactType.INTEROP)
            it.generateSrcFile(it.projectPath, "foo.h", "#include <bar.h>")
            def fooDir = it.projectPath.resolve("foo")
            it.generateSrcFile(fooDir, "bar.h", "const int foo = 5;")
            it.addSetting("interopLib", "headerFilterAdditionalSearchPrefix", fooDir.toFile())
            it.addSetting("interopLib", "headerFilterAdditionalSearchPrefix", it.projectDir)
        }
        project.createRunner().withArguments("build").build()
        def wrong_path = project.createFile("wrong_path", "")
        project.addSetting("interopLib", "headerFilterAdditionalSearchPrefix", wrong_path)
        project.createRunner().withArguments("build").buildAndFail()
    }

    BuildResult failOnPropertyAccess(KonanProject project, String property) {
         project.buildFile.append("""
            task testTask(type: DefaultTask) {
                doLast {
                    println(${project.defaultInteropConfig()}.$property)
                }
            }
        """.stripIndent())
        return project.createRunner().withArguments("testTask").buildAndFail()
    }

    BuildResult failOnTaskAccess(KonanProject project, String task) {
        project.buildFile.append("""
            task testTask(type: DefaultTask) {
                dependsOn $task
            }
        """.stripIndent())
        return project.createRunner().withArguments("testTask").buildAndFail()
    }
}
