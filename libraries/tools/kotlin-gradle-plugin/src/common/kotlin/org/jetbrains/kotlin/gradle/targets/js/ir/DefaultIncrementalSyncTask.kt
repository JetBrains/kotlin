package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.targets.js.internal.RewriteSourceMapFilterReader
import org.jetbrains.kotlin.gradle.tasks.IncrementalSyncTask
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault
abstract class DefaultIncrementalSyncTask : DefaultTask(), IncrementalSyncTask {

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val objectFactory: ObjectFactory

    @TaskAction
    fun doCopy(inputChanges: InputChanges) {
        val destinationDir = destinationDirectory.get()
        val commonAction: CopySpec.() -> Unit = {
            into(destinationDir)
            // Rewrite relative paths in sourcemaps in the target directory
            eachFile {
                if (it.name.endsWith(".js.map")) {
                    it.filter(
                        mapOf(
                            RewriteSourceMapFilterReader::srcSourceRoot.name to it.file.parentFile,
                            RewriteSourceMapFilterReader::targetSourceRoot.name to destinationDir
                        ),
                        RewriteSourceMapFilterReader::class.java
                    )
                }
            }
        }

        val work = if (!inputChanges.isIncremental) {
            fs.copy {
                it.from(from)
                it.commonAction()
            }.didWork
        } else {
            val changedFiles = inputChanges.getFileChanges(from)

            val modified = changedFiles
                .filter {
                    it.changeType == ChangeType.ADDED || it.changeType == ChangeType.MODIFIED
                }
                .map { it.file }
                .toSet()

            val forCopy = from.asFileTree
                .matching { patternFilterable ->
                    patternFilterable.exclude {
                        it.file.isFile && it.file !in modified
                    }
                }

            val nonRemovingFiles = mutableSetOf<File>()

            from.asFileTree
                .visit {
                    nonRemovingFiles.add(it.relativePath.getFile(destinationDir))
                }

            val removingFiles = objectFactory.fileTree()
                .from(destinationDir)
                .also { fileTree ->
                    fileTree.exclude {
                        it.file.isFile && it.file in nonRemovingFiles
                    }
                }

            val deleteWork = fs.delete {
                it.delete(removingFiles)
            }

            val copyWork = fs.copy {
                it.from(forCopy)
                it.commonAction()
            }

            deleteWork.didWork || copyWork.didWork
        }

        didWork = work
    }
}