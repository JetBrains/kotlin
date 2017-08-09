package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification


class TaskSpecification extends BaseKonanSpecification {

    def 'Configs should allow user to add dependencies to them'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory) { KonanInteropProject it ->
            it.generateSrcFile("main.kt")
        }
        project.buildFile.append("""
            task beforeInterop(type: DefaultTask) { doLast { println("Before Interop") } }
            task beforeCompilation(type: DefaultTask) { doLast { println("Before compilation") } }
        """.stripIndent())
        project.addInteropSetting("dependsOn", "beforeInterop")
        project.addCompilationSetting("dependsOn", "beforeCompilation")
        def result = project.createRunner().withArguments('build').build()

        then:
        def beforeInterop = result.task(":beforeInterop")
        beforeInterop != null && beforeInterop.outcome == TaskOutcome.SUCCESS
        def beforeCompilation = result.task(":beforeCompilation")
        beforeCompilation != null && beforeCompilation.outcome == TaskOutcome.SUCCESS
    }

}
