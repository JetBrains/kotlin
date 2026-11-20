/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.NormalizeLineEndings

/**
 * Configures [task] to check ArchUnit rules against a frozen violations store.
 *
 * Run [task] with `-ParchunitRefreeze` to update the store after fixing or adding violations.
 *
 * @param sources compiled classes checked by the rules, passed to the test as the `archUnitClasses` system property.
 * @param store directory of the frozen violations store, committed to the repository.
 */
fun configureArchUnit(
    task: Test,
    sources: FileCollection,
    store: Directory = task.project.layout.projectDirectory.dir("src/${task.name}/archunit-store"),
) {
    // Only one fork, so the frozen violations store is not updated concurrently
    task.maxParallelForks = 1

    task.addClasspathProperty(sources, "archUnitClasses")

    val refreeze = task.project.providers.gradleProperty("archunitRefreeze")
        .map { it.isEmpty() || it.toBoolean() }
        .orElse(false)

    // The store is only read by normal runs, so it is an input.
    // A refreeze run rewrites the store in place, so it must always execute:
    // skipping it as up-to-date or loading it from the build cache would leave the store unchanged.
    task.outputs.upToDateWhen { !refreeze.get() }
    task.outputs.doNotCacheIf("ArchUnit refreeze updates the frozen violations store in place") { refreeze.get() }

    task.jvmArgumentProviders.add(
        task.project.objects.newInstance<ArchUnitFreezeConfiguration>().also { config ->
            config.location.set(store)
            config.refreeze.set(refreeze)
            config.taskPath.set(task.path)
        }
    )
}

/**
 * Configures ArchUnit's frozen violations store for a test task.
 */
abstract class ArchUnitFreezeConfiguration : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val location: DirectoryProperty

    @get:Input
    abstract val refreeze: Property<Boolean>

    /** Shown in the store's failure messages, so the error message is more helpful. */
    @get:Console
    abstract val taskPath: Property<String>

    override fun asArguments(): Iterable<String> {
        val refreeze = refreeze.get()
        return listOf(
            "-Darchunit.freeze.store.default.path=${location.get().asFile.absolutePath}",
            "-Darchunit.freeze.refreeze=$refreeze",
            "-Darchunit.freeze.store.default.allowStoreUpdate=$refreeze",
            "-Darchunit.freeze.store.default.taskPath=${taskPath.get()}",
        )
    }
}
