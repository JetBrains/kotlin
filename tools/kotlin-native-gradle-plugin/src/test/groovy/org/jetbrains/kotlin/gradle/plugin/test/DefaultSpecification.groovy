package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification


class DefaultSpecification extends BaseKonanSpecification {

    def 'Plugin should build a project without additional settings'() {
        when:
        def project = KonanInteropProject.create(projectDirectory) { KonanInteropProject it ->
            it.buildFile.write("""
            plugins { id 'konan' }
            konanInterop { stdio {} }
            konanArtifacts { main {} }
            """.stripIndent())
        }
        def result = project.createRunner().withArguments('build').build()

        then:
        !result.tasks.collect { it.outcome }.contains(TaskOutcome.FAILED)
    }
}
