/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

private val contractAnnotationRegex = Regex("Lorg/jetbrains/kotlin/testFederation/(?<name>\\w+)Contract;")

/**
 * Tests marked as contract (aka 'Contract Tests') require the review of the 'target subsystem owner'.
 * For example, a test marked as a contract with the compiler (@CompilerContract) requires the review of the compiler team
 * to be checked in or removed. This task will collect all contracts from the current project and dumps them into
 * the home directory of the target subsystem.
 */
@Suppress("unused")
open class TestFederationContractDumpTask : DefaultTask() {

    data class Contract(val target: Subsystem, val fqn: String)

    @Classpath
    val classesDirs: ConfigurableFileCollection = project.files()

    private val outputFiles: Map<Subsystem, RegularFile> = SubsystemInfo.all.associate { systemInfo ->
        systemInfo.system to project.isolated.rootProject.projectDirectory.dir(systemInfo.home)
            .file("contracts/" + project.contractsFileName)
    }

    @OutputFiles
    val outputs = project.files(outputFiles.values)

    @TaskAction
    fun dumpContracts() {
        val contracts = mutableListOf<Contract>()

        classesDirs.files.forEach { classesDir ->
            classesDir.walk().filter { it.name.endsWith(".class") }.forEach { classFile ->
                val classNode = ClassNode()
                ClassReader(classFile.readBytes()).accept(classNode, 0)

                classNode.visibleAnnotations.orEmpty().forEach { node ->
                    val system = node.findContract() ?: return@forEach
                    contracts.add(Contract(system, classNode.name))
                }

                classNode.methods.forEach { method ->
                    method.visibleAnnotations.orEmpty().forEach { node ->
                        val system = node.findContract() ?: return@forEach
                        contracts.add(Contract(system, classNode.name + "#" + method.name + method.desc))
                    }
                }
            }
        }

        contracts.groupBy { it.target }.forEach { (target, contracts) ->
            val file = outputFiles.getValue(target)
            contracts.sortedBy { it.fqn }
            file.asFile.toPath().createParentDirectories().writeText(contracts.joinToString("\n") { it.fqn })
        }
    }

    private fun AnnotationNode.findContract(): Subsystem? {
        val match = contractAnnotationRegex.matchEntire(desc) ?: return null
        val name = match.groups["name"]?.value ?: error("Missing 'name' group")
        return Subsystem.valueOf(name)
    }

    private val Project.contractsFileName: String
        get() = path.removePrefix(":").replace(":", "_") + "-contracts.txt"
}
