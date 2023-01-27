package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

/**
 * An interface that any test that works with ExecutorService
 * should implement to be run on different platforms.
 */
interface KonanTestExecutable : Task {
    /**
     * Test executable to be run by the service.
     */
    @get:Input
    val executable: String

    /**
     * Action that configures task or does some workload before the test will be executed.
     * Could be done as a first step in the test or just as a `doFirst` action in the test task.
     */
    @get:Nested
    var doBeforeRun: Action<in Task>?

    /**
     * Action that configures task or does some workload before the test will be built.
     * Depending on the test task implementation this action is done before the build task
     * or as its `doFirst` action.
     */
    @get:Nested
    var doBeforeBuild: Action<in Task>?

    /**
     * Build tasks that this [executable] depends on, or is built from.
     */
    @get:Internal
    val buildTasks: List<Task>
}