package org.jetbrains.kotlin.gradle.plugin.test

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

class CMakeSpecification extends BaseKonanSpecification {
    def 'Plugin should generate CMake from project without additional settings'() {
        when:
        def sdlIncludePath
        def sdlLinkOptions
        switch (HostManager.host.family) {
            case Family.WINDOWS:
                sdlIncludePath = "C:/SDL2/include"
                sdlLinkOptions = "C:/SDL2/lib"
                break
            case Family.OSX:
                sdlIncludePath = "/usr/include/SDL2"
                sdlLinkOptions = "-F /Library/Frameworks -framework SDL2"
                break
            case Family.LINUX:
                sdlIncludePath = "/usr/include/SDL2"
                sdlLinkOptions = "-L/usr/lib/x86_64-linux-gnu"
                break
            default:
                throw IllegalStateException("Unknown host: ${HostManager.hostName}")
                break
        }

        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.buildFile.write("""
            plugins { id 'konan' }
            konanArtifacts {
                interop('stdio')
                interop('sdl') {
                    defFile 'src/main/c_interop/sdl.def'
                    target('mingw') { includeDirs 'C:/SDL2/include' }
                    target('macbook') { includeDirs '/usr/include/SDL2' }
                    target('linux') { includeDirs '/usr/include/SDL2' }
                }
                library('main_lib')
                program('Main') {
                    libraries {
                        artifact 'stdio'
                        artifact 'main_lib'
                        target('mingw') { linkerOpts 'C:/SDL2/lib' }
                        target('macbook') { linkerOpts '-F /Library/Frameworks -framework SDL2' }
                        target('linux') { linkerOpts '-L/usr/lib/x86_64-linux-gnu' }
                    }
                }
            }
            """.stripIndent())
            it.generateDefFile("stdio.def", "")
            it.generateSrcFile("main.kt")
        }
        project.createRunner().withArguments('generateCMake').build()

        def cMakeModule = new File(projectDirectory, "KotlinCMakeModule")
        def cMakeLists = new File(projectDirectory, "CMakeLists.txt")
        def expectedCMakeLists = """
            cmake_minimum_required(VERSION 3.8)
            
            set(CMAKE_MODULE_PATH \${CMAKE_CURRENT_LIST_DIR}/KotlinCMakeModule)
            
            project(${projectDirectory.name} Kotlin)
            
            cinterop(
                NAME sdl
                DEF_FILE src/main/c_interop/sdl.def
                COMPILER_OPTS -I$sdlIncludePath)
            
            cinterop(
                NAME stdio
                DEF_FILE src/main/c_interop/stdio.def)
            
            konanc_library(
                NAME main_lib
                SOURCES src/main/kotlin/main.kt)
                
            konanc_executable(
                NAME Main
                SOURCES src/main/kotlin/main.kt
                LIBRARIES stdio main_lib
                LINKER_OPTS $sdlLinkOptions)
            """.stripIndent().trim()

        then:
        cMakeModule.exists()
        cMakeLists.exists()

        def actualCMakeLists = cMakeLists.text.trim().replace('\r\n', '\n')
        actualCMakeLists == expectedCMakeLists
    }

}
