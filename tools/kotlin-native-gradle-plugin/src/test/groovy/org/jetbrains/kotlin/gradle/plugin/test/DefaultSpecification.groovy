package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome

class DefaultSpecification extends BaseKonanSpecification {

    def 'Plugin should build a project without additional settings'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.buildFile.write("""
            plugins { id 'konan' }
            konanArtifacts {
                interop('stdio')
                program('main')
            }
            """.stripIndent())
            it.generateDefFile("stdio.def", "")
            it.generateSrcFile("main.kt")
        }
        def result = project.createRunner().withArguments('build').build()


        then:
        !result.tasks.collect { it.outcome }.contains(TaskOutcome.FAILED)
    }
}
