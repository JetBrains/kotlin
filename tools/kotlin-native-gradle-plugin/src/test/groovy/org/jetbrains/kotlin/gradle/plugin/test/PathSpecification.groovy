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
        def klib = new File("$konan/interopCompiledStubs/stdioInteropStubs/stdioInteropStubs.klib")
        klib.exists() && klib.file
        def stdioKt = new File("$konan/interopStubs/genStdioInteropStubs/stdio/stdio.kt")
        stdioKt.exists() && stdioKt.file
        def manifest = new File("$konan/interopStubs/genStdioInteropStubs/manifest.properties")
        manifest.exists() && manifest.file
        def nativeLib = new File("$konan/nativelibs/genStdioInteropStubs/stdiostubs.bc")
        nativeLib.exists() && nativeLib.file
    }

    def 'Plugin should stop building if the compiler classpath is empty'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory)
        project.propertiesFile.write("konan.home=${projectDirectory.canonicalPath}}")
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.task(project.downloadTask).outcome == TaskOutcome.FAILED
    }

    def 'Plugin should stop building if the stub generator classpath is empty'() {
        when:
        def project = KonanInteropProject.createEmpty(projectDirectory)
        project.propertiesFile.write("konan.home=${projectDirectory.canonicalPath}}")
        def result = project.createRunner().withArguments('build').buildAndFail()

        then:
        result.task(project.downloadTask).outcome == TaskOutcome.FAILED
    }

}
