/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.test

class LibrarySpecification extends BaseKonanSpecification {
    
    def libraries = [
            [manualDependsOn: true , code: { l1, l2 ->
                "file ${KonanProject.outputAccessCode(l1)}\nfile ${KonanProject.outputAccessCode(l2)}"
            }],
            [manualDependsOn: true , code: { l1, l2 ->
                "files ${KonanProject.outputAccessCode(l1)}, ${KonanProject.outputAccessCode(l2)}"
            }],
            [manualDependsOn: true , code: { l1, l2 ->
                "files project.files(${KonanProject.outputAccessCode(l1)}, ${KonanProject.outputAccessCode(l2)})"
            }],
            [manualDependsOn: true , code: { l1, l2 -> "klib '$l1'\nklib '$l2'" }],
            [manualDependsOn: true , code: { l1, l2 -> "klibs '$l1', '$l2'" }],
            [manualDependsOn: false, code: { l1, l2 -> "artifact '$l1'\nartifact '$l2'" }],
            [manualDependsOn: false, code: { l1, l2 -> "artifact konanArtifacts.$l1\nartifact konanArtifacts.$l2" }],
    ]

    String createMainWithCalls(List<Tuple2<String, String>> functions, Closure<String> callBuilder) {
        def result = new StringBuilder("""
            |fun main(args: Array<String>) {\n
        """.stripMargin())

        functions.forEach {
            result.append(callBuilder(it.first))
            result.append(callBuilder(it.second))
        }

        result.append("}")
        return result.toString()
    }
    
    void createLibraryWithFunction(KonanProject project, String name) {
        project.addCompilerArtifact(name, """
            |package $name
            |
            |fun $name() {
            |    println("$name")
            |}
        """.stripMargin(), ArtifactType.LIBRARY)
        project.addSetting(name, "noDefaultLibs", "true")
        project.addSetting(name, "noEndorsedLibs", "true")
    }

    void createInteropLibrary(KonanProject project, String name) {
        project.addCompilerArtifact(name, "headers = math.h", ArtifactType.INTEROP)
        project.addSetting(name, "noDefaultLibs", "true")
        project.addSetting(name, "noEndorsedLibs", "true")
    }

    KonanProject createProjectWithLibraries(Closure createLibraryFunction, Closure callBuilder) {
        def result = KonanProject.createEmpty(projectDirectory)

        def libraryNames = new ArrayList<Tuple2<String, String>>()
        for (int i = 0; i < libraries.size(); i++) {
            libraryNames.add(new Tuple2("foo$i", "bar$i"))
        }

        libraryNames.forEach {
            createLibraryFunction(result, it.first)
            createLibraryFunction(result, it.second)
        }

        result.addCompilerArtifact("main", createMainWithCalls(libraryNames, callBuilder))
        result.addSetting("main", "noDefaultLibs", "true")
        result.addSetting("main", "noEndorsedLibs", "true")

        for (int i = 0; i < libraries.size(); i++) {
            def foo = libraryNames[i].first
            def bar = libraryNames[i].second
            result.addLibraryToArtifactCustom("main", libraries[i].code(foo, bar) )

            if (libraries[i].manualDependsOn) {
                result.addSetting("main", "dependsOn", "konanArtifacts.$foo")
                result.addSetting("main", "dependsOn", "konanArtifacts.$bar")
            }
        }

        return result
    }


    KonanProject createProjectWithSimpleLibraries() {
        return createProjectWithLibraries(
                { p, n -> createLibraryWithFunction(p, n) },
                { "$it.$it()\n" } )
    }


    KonanProject createProjectWithInteropLibraries() {
        return createProjectWithLibraries(
                { p, n -> createInteropLibrary(p, n) },
                { "println(${it}.cos(0.0))\n" }
        )
    }

    def 'Plugin should support libraries from the same project'() {
        expect:
        createProjectWithSimpleLibraries()
                .createRunner()
                .withArguments(KonanProject.compilationTask("main"))
                .build()
    }

    def 'Plugin should support interop libraries from the same project'() {
        expect:
        createProjectWithInteropLibraries()
                .createRunner()
                .withArguments(KonanProject.compilationTask("main"))
                .build()
    }

    def 'Plugin should support allLibrariesFrom method for the current project'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("foo", "fun foo() { println(42) }", ArtifactType.LIBRARY)
            it.addSetting("foo", "noDefaultLibs", "true")
            it.addSetting("foo", "noEndorsedLibs", "true")

            it.addCompilerArtifact("bar", "fun bar() { println(43) }", ArtifactType.LIBRARY)
            it.addSetting("bar", "noDefaultLibs", "true")
            it.addSetting("bar", "noEndorsedLibs", "true")

            it.addCompilerArtifact("main" ,"fun main(args: Array<String>) { foo(); bar() }")
            it.addSetting("main", "noDefaultLibs", "true" )
            it.addSetting("main", "noEndorsedLibs", "true" )
            it.addLibraryToArtifactCustom("main", "allLibrariesFrom project")
        }
        project.createRunner()
                .withArguments(KonanProject.compilationTask("main"))
                .build()
    }

    def 'Plugin should support allLibrariesFrom method for another project'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory)
        def subproject = KonanProject.createEmpty(project.createSubDir("subproject")) { KonanProject it ->
            it.buildFile.write("apply plugin: 'konan'\n")
        }
        project.settingsFile.append("include ':subproject'")

        project.addCompilerArtifact("wrongFoo","fun foo() { println(24) }", ArtifactType.LIBRARY)
        project.addSetting("wrongFoo", "noDefaultLibs", "true")
        project.addSetting("wrongFoo", "noEndorsedLibs", "true")

        subproject.addCompilerArtifact("foo", "fun foo() { println(42) }", ArtifactType.LIBRARY)
        subproject.addSetting("foo", "noDefaultLibs", "true")
        subproject.addSetting("foo", "noEndorsedLibs", "true")

        subproject.addCompilerArtifact("bar", "fun bar() { println(43) }", ArtifactType.LIBRARY)
        subproject.addSetting("bar", "noDefaultLibs", "true")
        subproject.addSetting("bar", "noEndorsedLibs", "true")

        project.addCompilerArtifact("main" ,"fun main(args: Array<String>) { foo(); bar() }")
        project.addSetting("main", "noDefaultLibs", "true" )
        project.addSetting("main", "noEndorsedLibs", "true" )
        project.addLibraryToArtifactCustom("main", "allLibrariesFrom project('subproject')")

        project.createRunner()
                .withArguments(KonanProject.compilationTask("main"))
                .build()
    }

    def 'Plugin should support allInteropLibrariesFrom method for the current project'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory)
        def subproject = KonanProject.createEmpty(project.createSubDir("subproject")) { KonanProject it ->
            it.buildFile.write("apply plugin: 'konan'\n")
        }
        project.settingsFile.append("include ':subproject'")


        project.addCompilerArtifact("wrongFoo1", "fun foo() { println(42) }", ArtifactType.LIBRARY)
        project.addSetting("wrongFoo1", "noDefaultLibs", "true")
        project.addSetting("wrongFoo1", "noEndorsedLibs", "true")

        subproject.addCompilerArtifact("wrongFoo2", "fun foo() { println(42) }", ArtifactType.LIBRARY)
        subproject.addSetting("wrongFoo2", "noDefaultLibs", "true")
        subproject.addSetting("wrongFoo2", "noEndorsedLibs", "true")

        subproject.addCompilerArtifact("math1", "headers = math.h", ArtifactType.INTEROP)
        subproject.addSetting("math1", "noDefaultLibs", "true")
        subproject.addSetting("math1", "noEndorsedLibs", "true")

        subproject.addCompilerArtifact("math2", "headers = math.h", ArtifactType.INTEROP)
        subproject.addSetting("math2", "noDefaultLibs", "true")
        subproject.addSetting("math2", "noEndorsedLibs", "true")

        project.addCompilerArtifact("main" ,"""
            |fun foo() {}
            |
            |fun main(args: Array<String>) { foo(); math1.cos(0.0); math2.cos(0.0) }
        """.stripMargin())
        project.addSetting("main", "noDefaultLibs", "true" )
        project.addSetting("main", "noEndorsedLibs", "true" )
        project.addLibraryToArtifactCustom("main", "allInteropLibrariesFrom project('subproject')")


        project.createRunner()
                .withArguments(KonanProject.compilationTask("main"))
                .build()
    }

    def 'Plugin should support allInteropLibrariesFrom method for another projecct'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("wrongFoo", "fun foo() { println(42) }", ArtifactType.LIBRARY)
            it.addSetting("wrongFoo", "noDefaultLibs", "true")
            it.addSetting("wrongFoo", "noEndorsedLibs", "true")

            it.addCompilerArtifact("math1", "headers = math.h", ArtifactType.INTEROP)
            it.addSetting("math1", "noDefaultLibs", "true")
            it.addSetting("math1", "noEndorsedLibs", "true")

            it.addCompilerArtifact("math2", "headers = math.h", ArtifactType.INTEROP)
            it.addSetting("math2", "noDefaultLibs", "true")
            it.addSetting("math2", "noEndorsedLibs", "true")

            it.addCompilerArtifact("main" ,"""
                |fun foo() {}
                |
                |fun main(args: Array<String>) { foo(); math1.cos(0.0); math2.cos(0.0) }
            """.stripMargin())
            it.addSetting("main", "noDefaultLibs", "true" )
            it.addSetting("main", "noEndorsedLibs", "true" )
            it.addLibraryToArtifactCustom("main", "allInteropLibrariesFrom project")
        }
        project.createRunner()
                .withArguments(KonanProject.compilationTask("main"))
                .build()
    }

    def 'Plugin should support custom repositories for libraries'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("foo", "fun foo() { println(42) }", ArtifactType.LIBRARY)
            it.addSetting("foo", "noDefaultLibs", "true")
            it.addSetting("foo", "noEndorsedLibs", "true")
            it.addSetting("foo", "baseDir", "file('out')")


            it.addCompilerArtifact("main" ,"fun main(args: Array<String>) { foo() }")
            it.addSetting("main", "noDefaultLibs", "true" )
            it.addSetting("main", "noEndorsedLibs", "true" )
            it.addSetting("main", "dependsOn", "konanArtifacts.foo.$KonanProject.HOST")
            it.addLibraryToArtifactCustom("main", "klib 'foo'")
            it.addLibraryToArtifactCustom("main", "useRepo 'out/$KonanProject.HOST'")
        }
        project.createRunner()
                .withArguments(KonanProject.compilationTask("main"), "-i")
                .build()
    }

    def 'Plugin should support library dependencies in the same project'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("foo", "fun foo() { println(42) }", ArtifactType.LIBRARY)
            it.addSetting("foo", "noDefaultLibs", "true")
            it.addSetting("foo", "noEndorsedLibs", "true")

            it.addCompilerArtifact("bar", "fun bar() { println(43) }", ArtifactType.LIBRARY)
            it.addSetting("bar", "noDefaultLibs", "true")
            it.addSetting("bar", "noEndorsedLibs", "true")
            it.addLibraryToArtifact("bar", "foo")

            it.addCompilerArtifact("main" ,"fun main(args: Array<String>) { foo(); bar() }")
            it.addSetting("main", "noDefaultLibs", "true" )
            it.addSetting("main", "noEndorsedLibs", "true" )
            it.addLibraryToArtifact("main", "bar")
        }

        project.createRunner()
                .withArguments(KonanProject.compilationTask("main"), "-i")
                .build()
    }

    def 'Plugin should support library dependencies from other projects'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory)
        def subproject1 = KonanProject.createEmpty(project.createSubDir("subproject1")) { KonanProject it ->
            it.buildFile.write("apply plugin: 'konan'\n")
        }
        def subproject2 = KonanProject.createEmpty(project.createSubDir("subproject2")) { KonanProject it ->
            it.buildFile.write("apply plugin: 'konan'\n")
        }
        project.settingsFile.append("include ':subproject1'\ninclude ':subproject2'")

        subproject1.addCompilerArtifact("foo", "fun foo() { println(42) }", ArtifactType.LIBRARY)
        subproject1.addSetting("foo", "noDefaultLibs", "true")
        subproject1.addSetting("foo", "noEndorsedLibs", "true")

        subproject2.addCompilerArtifact("bar", "fun bar() { println(43) }", ArtifactType.LIBRARY)
        subproject2.addSetting("bar", "noDefaultLibs", "true")
        subproject2.addSetting("bar", "noEndorsedLibs", "true")
        subproject2.addLibraryToArtifactCustom(
                "bar", "artifact rootProject.project('subproject1'), 'foo'"
        )

        project.addCompilerArtifact("main" ,"fun main(args: Array<String>) { foo(); bar() }")
        project.addSetting("main", "noDefaultLibs", "true" )
        project.addSetting("main", "noEndorsedLibs", "true" )
        project.addLibraryToArtifactCustom(
                "main", "artifact project('subproject2'), 'bar'"
        )

        project.createRunner()
                .withArguments(KonanProject.compilationTask("main"))
                .build()
    }

    // TODO: Add tests for incorrect cases (e.g. attempt to use an executable as a library)
}
