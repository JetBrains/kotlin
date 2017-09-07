package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification


class PathSpecification extends BaseKonanSpecification {

    def 'Plugin should create all necessary directories'() {
        when:
        def project = KonanInteropProject.create(projectDirectory)
        def result = project.createRunner().withArguments('build').build()

        then:
        def konan = project.konanBuildDir
        new File("$konan/bin").listFiles().findAll {
            File it -> it.file && it.name.matches('^main\\.[^.]+')
        }.size() > 0
        def klib = new File("$konan/c_interop/stdio.klib")
        klib.exists() && klib.file
    }

    def 'Plugin should stop building if the compiler classpath is empty'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory)
        project.propertiesFile.write("konan.home=${projectDirectory.canonicalPath}}")
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.task(project.defaultCompilationTask()).outcome == TaskOutcome.FAILED
    }

    def 'Plugin should stop building if the stub generator classpath is empty'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory)
        project.propertiesFile.write("konan.home=${projectDirectory.canonicalPath}}")
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.task(project.defaultStubGenerationTask()).outcome == TaskOutcome.FAILED
    }

}
