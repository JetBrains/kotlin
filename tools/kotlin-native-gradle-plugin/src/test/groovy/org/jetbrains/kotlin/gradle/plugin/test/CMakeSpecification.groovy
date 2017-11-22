package org.jetbrains.kotlin.gradle.plugin.test

class CMakeSpecification extends BaseKonanSpecification {

    def 'Plugin should generate CMake from project without additional settings'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.buildFile.write("""
            plugins { id 'konan' }
            konanArtifacts {
                interop('stdio')
                interop('sdl') {
                    defFile 'src/main/c_interop/sdl.def'
                    includeDirs '/usr/include/SDL2'
                }
                library('main_lib')
                program('Main') {
                    libraries {
                        artifact 'stdio'
                        artifact 'main_lib'
                        linkerOpts '-L/usr/lib/x86_64-linux-gnu'
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
                COMPILER_OPTS -I/usr/include/SDL2)
            
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
                LINKER_OPTS -L/usr/lib/x86_64-linux-gnu)
            """.stripIndent().trim()

        then:
        cMakeModule.exists()
        cMakeLists.exists()

        def actualCMakeLists = cMakeLists.text.trim()
        actualCMakeLists == expectedCMakeLists
    }

}
