/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(FreezingIsDeprecated::class)
package test.native.concurrent

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.concurrent.WorkerBoundReference
import kotlin.test.*

private class C(var a: Int)
private class Wrapper(val ref: WorkerBoundReference<C>)

private val global1: WorkerBoundReference<C> = WorkerBoundReference(C(3))
private val global2: WorkerBoundReference<C> = WorkerBoundReference(C(3))
private val global3: WorkerBoundReference<C> = WorkerBoundReference(C(3).freeze())
private val global4: WorkerBoundReference<C> = WorkerBoundReference(C(3))
private val global5: WorkerBoundReference<C> = WorkerBoundReference(C(3))
private val global6: WorkerBoundReference<C> = WorkerBoundReference(C(3))
private val global7: WorkerBoundReference<C> = WorkerBoundReference(C(3))

class WorkerBoundReference {
    @Test
    fun global() {
        assertEquals(3, global1.value.a)
        assertEquals(3, global1.valueOrNull?.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, {}) {
            global1
        }

        val value = future.result
        assertEquals(3, value.value.a)
        assertEquals(3, value.valueOrNull?.a)
        worker.requestTermination().result
    }

    @Test
    fun globalOnWorker() {
        assertEquals(3, global2.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, {}) {
            assertEquals(global2.value, global2.valueOrNull)
            global2.value.a
        }

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun globalOnWorkerFrozenInitially() {
        assertEquals(3, global3.value.a)
        assertEquals(3, global3.valueOrNull?.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, {}) {
            global3.value.a
        }

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun globalOnWorkerFrozenBeforePassing() {
        assertEquals(3, global4.value.a)
        global4.value.freeze()

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, {}) {
            global4.value.a
        }

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun globalOnWorkerFrozenBeforeAccess() {
        val semaphore = AtomicInt(0)

        assertEquals(3, global5.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
            semaphore.incrementAndGet()
            while (semaphore.value < 2) {
            }

            global5.value.a
        }

        while (semaphore.value < 1) {
        }
        global5.value.freeze()
        semaphore.incrementAndGet()

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun globalModification() {
        val semaphore = AtomicInt(0)

        assertEquals(3, global6.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { semaphore }) { semaphore ->
            semaphore.incrementAndGet()
            while (semaphore.value < 2) {
            }
            global6
        }

        while (semaphore.value < 1) {
        }
        global6.value.a = 4
        semaphore.incrementAndGet()

        val value = future.result
        assertEquals(4, value.value.a)
        assertEquals(4, value.valueOrNull?.a)
        worker.requestTermination().result
    }

    @Test
    fun globalGetWorker() {
        val ownerId = Worker.current.id
        assertEquals(ownerId, global7.worker.id)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { ownerId }) { ownerId ->
            assertEquals(ownerId, global7.worker.id)
        }

        future.result
        worker.requestTermination().result
    }

    @Test
    fun local() {
        val local = WorkerBoundReference(C(3))
        assertEquals(3, local.value.a)
        assertEquals(3, local.valueOrNull?.a)
    }

    @Test
    fun localFrozen() {
        val local = WorkerBoundReference(C(3)).freeze()
        assertEquals(3, local.value.a)
        assertEquals(3, local.valueOrNull?.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { local }) { local ->
            local
        }

        val value = future.result
        assertEquals(3, value.value.a)
        assertEquals(3, value.valueOrNull?.a)
        worker.requestTermination().result
    }

    @Test
    fun localOnWorkerFrozen() {
        val local = WorkerBoundReference(C(3)).freeze()
        assertEquals(3, local.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { local }) { local ->
            assertEquals(local.value, local.valueOrNull)
            local.value.a
        }

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun localOnWorkerFrozenInitiallyFrozen() {
        val local = WorkerBoundReference(C(3).freeze()).freeze()
        assertEquals(3, local.value.a)
        assertEquals(3, local.valueOrNull?.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { local }) { local ->
            local.value.a
        }

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun localOnWorkerFrozenBeforePassingFrozen() {
        val local = WorkerBoundReference(C(3)).freeze()
        assertEquals(3, local.value.a)
        local.value.freeze()

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { local }) { local ->
            local.value.a
        }

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun localOnWorkerFrozenBeforeAccessFrozen() {
        val semaphore = AtomicInt(0)

        val local = WorkerBoundReference(C(3)).freeze()
        assertEquals(3, local.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
            semaphore.incrementAndGet()
            while (semaphore.value < 2) {
            }

            local.value.a
        }

        while (semaphore.value < 1) {
        }
        local.value.freeze()
        semaphore.incrementAndGet()

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun localOnMainThread() {
        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, {}) {
            WorkerBoundReference(C(3))
        }

        assertEquals(3, future.result.value.a)

        worker.requestTermination().result
    }

    @Test
    fun localOnMainThreadFrozen() {
        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, {}) {
            WorkerBoundReference(C(3)).freeze()
        }

        val value = future.result
        assertEquals(3, value.value.a)
        assertEquals(value.value, value.valueOrNull)

        worker.requestTermination().result
    }

    @Test
    fun localModificationFrozen() {
        val semaphore = AtomicInt(0)

        val local = WorkerBoundReference(C(3)).freeze()
        assertEquals(3, local.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { Pair(local, semaphore) }) { (local, semaphore) ->
            semaphore.incrementAndGet()
            while (semaphore.value < 2) {
            }
            local
        }

        while (semaphore.value < 1) {
        }
        local.value.a = 4
        semaphore.incrementAndGet()

        val value = future.result
        assertEquals(4, value.value.a)
        assertEquals(4, value.valueOrNull?.a)
        worker.requestTermination().result
    }

    @Test
    fun localGetWorkerFrozen() {
        val local = WorkerBoundReference(C(3)).freeze()

        val ownerId = Worker.current.id
        assertEquals(ownerId, local.worker.id)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { Pair(local, ownerId) }) { (local, ownerId) ->
            assertEquals(ownerId, local.worker.id)
        }

        future.result
        worker.requestTermination().result
    }

    @Test
    fun localForeignGetWorker() {
        val worker = Worker.start()
        val ownerId = worker.id
        val future = worker.execute(TransferMode.SAFE, { ownerId }) { ownerId ->
            val local = WorkerBoundReference(C(3))
            assertEquals(ownerId, local.worker.id)
            local
        }

        val value = future.result
        assertEquals(ownerId, value.worker.id)

        worker.requestTermination().result
    }

    @Test
    fun localForeignGetWorkerFrozen() {
        val worker = Worker.start()
        val ownerId = worker.id
        val future = worker.execute(TransferMode.SAFE, { ownerId }) { ownerId ->
            val local = WorkerBoundReference(C(3)).freeze()
            assertEquals(ownerId, local.worker.id)
            local
        }

        val value = future.result
        assertEquals(ownerId, value.worker.id)

        worker.requestTermination().result
    }

    @Test
    fun localWithWrapperFrozen() {
        val local = Wrapper(WorkerBoundReference(C(3))).freeze()
        assertEquals(3, local.ref.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { local }) { local ->
            local
        }

        val value = future.result
        assertEquals(3, value.ref.value.a)
        worker.requestTermination().result
    }

    @Test
    fun localAccessWithWrapperFrozen() {
        val local = Wrapper(WorkerBoundReference(C(3))).freeze()
        assertEquals(3, local.ref.value.a)

        val worker = Worker.start()
        val future = worker.execute(TransferMode.SAFE, { local }) { local ->
            assertEquals(local.ref.value, local.ref.valueOrNull)
            local.ref.value.a
        }

        val value = future.result
        assertEquals(3, value)
        worker.requestTermination().result
    }

    @Test
    fun concurrentAccessFrozen() {
        val workerCount = 10
        val workerUnlocker = AtomicInt(0)

        val ref = WorkerBoundReference(C(3)).freeze()
        assertEquals(3, ref.value.a)

        val workers = Array(workerCount) {
            Worker.start()
        }
        val futures = Array(workers.size) {
            workers[it].execute(TransferMode.SAFE, { Pair(ref, workerUnlocker) }) { (ref, workerUnlocker) ->
                while (workerUnlocker.value < 1) {
                }

                ref.value.a
            }
        }
        workerUnlocker.incrementAndGet()

        for (future in futures) {
            val value = future.result
            assertEquals(3, value)
        }

        for (worker in workers) {
            worker.requestTermination().result
        }
    }

    @Test
    fun doubleFreeze() {
        val ref = WorkerBoundReference(C(3))
        val wrapper = Wrapper(ref)
        ref.freeze()
        ref.freeze()
        wrapper.freeze()
    }
}