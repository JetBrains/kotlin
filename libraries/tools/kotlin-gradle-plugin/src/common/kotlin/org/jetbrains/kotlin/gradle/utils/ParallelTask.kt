/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.Serializable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

abstract class EnableParallelWork : WorkAction<EnableParallelWork.Work> {
    interface Work : WorkParameters {
        var work: HackGradleSerialization
    }
    override fun execute() {
        parameters.work.work()
    }
}

class HackGradleSerialization(
    @Transient
    var work: () -> Unit,
) : Serializable {

    private val id = UUID.randomUUID().toString()
    private fun writeObject(stream: java.io.ObjectOutputStream) {
        lambdaMap.put(id, work)
        stream.writeObject(id)
    }

    private fun readObject(stream: java.io.ObjectInputStream) {
        val myId = stream.readObject() as String
        work = lambdaMap.remove(myId)!!
    }

    private fun readObjectNoData() {
        error("???")
    }

    companion object {
        val lambdaMap = ConcurrentHashMap<String, () -> Unit>(0)
    }
}


abstract class ParallelTask : DefaultTask(), Serializable {

    @get:Inject
    abstract val executor: WorkerExecutor

    abstract fun parallelWork()

    @TaskAction
    fun action() {
        parallelWork()
//        executor.noIsolation().submit(EnableParallelWork::class.java) {
//            it.work = HackGradleSerialization {
//                parallelWork()
//            }
//        }
    }
}
