/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.util.Consumer
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

// TODO: Now IDEA can provide filter from gutters only for Test task (JVM tests)
//  Need to create fake Test task to copy filters from it to custom non-JVM test task
class KotlinNonJvmGutterConfigurator : AbstractProjectResolverExtension() {
    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        initScriptConsumer.consume(
            //language=Gradle
            """
            ({
                if (org.gradle.util.GradleVersion.current() >= org.gradle.util.GradleVersion.version("4.0")) {
                    Class kotlinTestClass = null
                    try {
                         kotlinTestClass = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinTest")
                    } catch (ClassNotFoundException ex) {
                        // ignore, class not available
                    }
                    
                    if (kotlinTestClass != null) {
                        gradle.afterProject { project ->
                            // Test task should have some parameters
                            // Create dummy task to consume outputs by Test task
                            project.tasks.create("nonJvmTestIdeSupportDummy")
                            
                            // IDEA now process filter parameters only for Test tasks
                            project.tasks.create('nonJvmTestIdeSupport', Test) {
                                testClassesDirs = project.tasks["nonJvmTestIdeSupportDummy"].outputs.files
                                classpath = project.tasks["nonJvmTestIdeSupportDummy"].outputs.files
                            }
                            
                            project.afterEvaluate {
                                project.tasks.withType(kotlinTestClass) { Task task ->
                                    task.dependsOn('nonJvmTestIdeSupport')
                                }
                            }
                        }
                        
                        gradle.taskGraph.beforeTask { Task task ->
                            if (kotlinTestClass.isAssignableFrom(task.class)) {
                                task.filter.includePatterns = task.project.tasks['nonJvmTestIdeSupport'].filter.includePatterns
                            }
                        }
                    }
                }
            })()
            """.trimIndent()
        )
    }
}