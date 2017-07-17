package org.jetbrains.kotlin.gradle.plugin.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification


class PathSpecification extends Specification {
    @Rule
    TemporaryFolder tmpFolder = new TemporaryFolder()

    def 'Plugin should create all necessary directories'() {
        when:
        def project = KonanInteropProject.create(tmpFolder)
        def result = project.createRunner().withArguments('build').build()

        then:
        def konan = "${tmpFolder.root}/build/konan"
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

}
