package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

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

    @Unroll('Plugin should support #option option for cinterop')
    def 'Plugin should support includeDir option for cinterop'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("interopLib", "headers=foo.h\n$headerFilter", ArtifactType.INTEROP)
            it.generateSrcFile(it.projectPath, "foo.h", "#include <bar.h>")
            def fooDir = it.projectPath.resolve("foo")
            it.generateSrcFile(fooDir, "bar.h", "const int foo = 5;")
            it.addSetting("interopLib", option, fooDir.toFile())
            it.addSetting("interopLib", option, it.projectDir)
        }
        project.createRunner().withArguments("build").build()

        where:
        option                         | headerFilter
        "includeDirs.headerFilterOnly" | "headerFilter=foo.h bar.h"
        "includeDirs.allHeaders"       | ""
        "includeDirs"                  | ""
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
