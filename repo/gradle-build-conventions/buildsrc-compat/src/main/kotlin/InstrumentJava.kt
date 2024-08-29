/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.kotlin.dsl.withNormalizer

internal class InstrumentJava(private val javaInstrumentatorClasspath: FileCollection) : Action<Task> {

    override fun execute(task: Task) {
        require(task is JavaCompile) { "$task is not of type JavaCompile!" }

        val instrumentatorClasspath = javaInstrumentatorClasspath
        task.inputs.files(instrumentatorClasspath)
            .withPropertyName("instrumentatorClasspath")
            .withNormalizer(ClasspathNormalizer::class)

        // There is a requirement for Javac.execute() to have an existing non-empty directory as a source dir for the compilation
        // even if compilation is disabled.
        //
        // So we create an empty dummy directory during the execution.
        //
        // See:
        // Javac.execute() - https://github.com/apache/ant/blob/9943641/src/main/org/apache/tools/ant/taskdefs/Javac.java#L1086
        // InstrumentIdeaExtensions - https://github.com/JetBrains/intellij-community/blob/9c40bdd/java/compiler/javac2/src/com/intellij/ant/InstrumentIdeaExtensions.java
        // Javac2.compile() - https://github.com/JetBrains/intellij-community/blob/9c40bdd/java/compiler/javac2/src/com/intellij/ant/Javac2.java#L237
        val dummyInstrumentSrcDir = task.project.layout.buildDirectory.dir("instrument_dummy_src").get().asFile
        val dummyInstrumentSrcRelativePath = dummyInstrumentSrcDir.relativeTo(task.project.projectDir).invariantSeparatorsPath

        task.doLast {
            task.ant.withGroovyBuilder {
                "taskdef"(
                    "name" to "instrumentIdeaExtensions",
                    "classpath" to instrumentatorClasspath.asPath,
                    "loaderref" to "java2.loader",
                    "classname" to "com.intellij.ant.InstrumentIdeaExtensions"
                )
            }

            dummyInstrumentSrcDir.deleteRecursively()
            dummyInstrumentSrcDir.mkdirs()

            task.ant.withGroovyBuilder {
                "instrumentIdeaExtensions"(
                    "srcdir" to dummyInstrumentSrcRelativePath,
                    "destdir" to task.destinationDirectory.asFile.get(),
                    "classpath" to task.classpath.asPath,
                    "includeantruntime" to false,
                    "instrumentNotNull" to true
                )
            }
        }
    }
}
