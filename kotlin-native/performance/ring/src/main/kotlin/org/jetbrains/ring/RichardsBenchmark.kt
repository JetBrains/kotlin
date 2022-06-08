/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This benchmark is a port of the V8 JavaScript benchmark suite
// richards benchmark:
//   https://chromium.googlesource.com/external/v8/+/ba56077937e154aa0adbabd8abb9c24e53aae85d/benchmarks/richards.js

// Copyright 2006-2008 the V8 project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of Google Inc. nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES LOSS OF USE,
// DATA, OR PROFITS OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

/**
 * The Richards benchmark simulates the task dispatcher of an
 * operating system.
 **/
class RichardsBenchmark {
    fun runRichards() {
        val scheduler = Scheduler()
        scheduler.addIdleTask(ID_IDLE, 0, null, COUNT)

        var queue = Packet(null, ID_WORKER, KIND_WORK)
        queue = Packet(queue,  ID_WORKER, KIND_WORK)
        scheduler.addWorkerTask(ID_WORKER, 1000, queue)

        queue = Packet(null, ID_DEVICE_A, KIND_DEVICE)
        queue = Packet(queue,  ID_DEVICE_A, KIND_DEVICE)
        queue = Packet(queue,  ID_DEVICE_A, KIND_DEVICE)
        scheduler.addHandlerTask(ID_HANDLER_A, 2000, queue)

        queue = Packet(null, ID_DEVICE_B, KIND_DEVICE)
        queue = Packet(queue,  ID_DEVICE_B, KIND_DEVICE)
        queue = Packet(queue,  ID_DEVICE_B, KIND_DEVICE)
        scheduler.addHandlerTask(ID_HANDLER_B, 3000, queue)

        scheduler.addDeviceTask(ID_DEVICE_A, 4000, null)

        scheduler.addDeviceTask(ID_DEVICE_B, 5000, null)

        scheduler.schedule()

        if (scheduler.queueCount != EXPECTED_QUEUE_COUNT ||
            scheduler.holdCount != EXPECTED_HOLD_COUNT) {
            val msg =
                "Error during execution: queueCount = " + scheduler.queueCount +
                ", holdCount = " + scheduler.holdCount + "."
            throw Error(msg)
        }
    }
}

var COUNT = 1000

/**
 * These two constants specify how many times a packet is queued and
 * how many times a task is put on hold in a correct run of richards.
 * They don't have any meaning a such but are characteristic of a
 * correct run so if the actual queue or hold count is different from
 * the expected there must be a bug in the implementation.
 **/
var EXPECTED_QUEUE_COUNT = 2322
var EXPECTED_HOLD_COUNT = 928


/**
 * A scheduler can be used to schedule a set of tasks based on their relative
 * priorities.  Scheduling is done by maintaining a list of task control blocks
 * which holds tasks and the data queue they are processing.
 * @constructor
 */
class Scheduler {
    var queueCount = 0
    var holdCount = 0
    var blocks = Array<TaskControlBlock?>(NUMBER_OF_IDS) { null }
    var list: TaskControlBlock? = null
    var currentTcb: TaskControlBlock? = null
    var currentId = 0

    /**
     * Add an idle task to this scheduler.
     * @param {int} id the identity of the task
     * @param {int} priority the task's priority
     * @param {Packet} queue the queue of work to be processed by the task
     * @param {int} count the number of times to schedule the task
     */
    fun addIdleTask(id: Int, priority: Int, queue: Packet?, count: Int) {
        this.addRunningTask(id, priority, queue, IdleTask(this, 1, count))
    }

    /**
     * Add a work task to this scheduler.
     * @param {int} id the identity of the task
     * @param {int} priority the task's priority
     * @param {Packet} queue the queue of work to be processed by the task
     */
    fun addWorkerTask(id: Int, priority: Int, queue: Packet?) {
        this.addTask(id, priority, queue, WorkerTask(this, ID_HANDLER_A, 0))
    }

    /**
     * Add a handler task to this scheduler.
     * @param {int} id the identity of the task
     * @param {int} priority the task's priority
     * @param {Packet} queue the queue of work to be processed by the task
     */
    fun addHandlerTask(id: Int, priority: Int, queue: Packet?) {
        this.addTask(id, priority, queue, HandlerTask(this))
    }

    /**
     * Add a handler task to this scheduler.
     * @param {int} id the identity of the task
     * @param {int} priority the task's priority
     * @param {Packet} queue the queue of work to be processed by the task
     */
    fun addDeviceTask(id: Int, priority: Int, queue: Packet?) {
        this.addTask(id, priority, queue, DeviceTask(this))
    }

    /**
     * Add the specified task and mark it as running.
     * @param {int} id the identity of the task
     * @param {int} priority the task's priority
     * @param {Packet} queue the queue of work to be processed by the task
     * @param {Task} task the task to add
     */
    fun addRunningTask(id: Int, priority: Int, queue: Packet?, task: Task) {
        this.addTask(id, priority, queue, task)
        this.currentTcb!!.setRunning()
    }

    /**
     * Add the specified task to this scheduler.
     * @param {int} id the identity of the task
     * @param {int} priority the task's priority
     * @param {Packet} queue the queue of work to be processed by the task
     * @param {Task} task the task to add
     */
    fun addTask(id: Int, priority: Int, queue: Packet?, task: Task) {
        this.currentTcb = TaskControlBlock(this.list, id, priority, queue, task)
        this.list = this.currentTcb
        this.blocks[id] = this.currentTcb
    }

    /**
     * Execute the tasks managed by this scheduler.
     */
    fun schedule() {
        this.currentTcb = this.list
        while (this.currentTcb != null) {
            if (this.currentTcb!!.isHeldOrSuspended()) {
                this.currentTcb = this.currentTcb!!.link
            } else {
                this.currentId = this.currentTcb!!.id
                this.currentTcb = this.currentTcb!!.run()
            }
        }
    }

    /**
     * Release a task that is currently blocked and return the next block to run.
     * @param {int} id the id of the task to suspend
     */
    fun release(id: Int): TaskControlBlock? {
        val tcb = this.blocks[id]
        if (tcb == null) return tcb
        tcb.markAsNotHeld()
        if (tcb.priority > this.currentTcb!!.priority) {
            return tcb
        } else {
            return this.currentTcb
        }
    }

    /**
     * Block the currently executing task and return the next task control block
     * to run.  The blocked task will not be made runnable until it is explicitly
     * released, even if new work is added to it.
     */
    fun holdCurrent(): TaskControlBlock? {
        this.holdCount++
        this.currentTcb!!.markAsHeld()
        return this.currentTcb!!.link
    }

    /**
     * Suspend the currently executing task and return the next task control block
     * to run.  If new work is added to the suspended task it will be made runnable.
     */
    fun suspendCurrent(): TaskControlBlock? {
        this.currentTcb!!.markAsSuspended()
        return this.currentTcb
    }

    /**
     * Add the specified packet to the end of the work list used by the task
     * associated with the packet and make the task runnable if it is currently
     * suspended.
     * @param {Packet} packet the packet to add
     */
    fun queue(packet: Packet): TaskControlBlock? {
        val t = this.blocks[packet.id]
        if (t == null) return t
        this.queueCount++
        packet.link = null
        packet.id = this.currentId
        return t.checkPriorityAdd(this.currentTcb!!, packet)
    }
}

var ID_IDLE       = 0
var ID_WORKER     = 1
var ID_HANDLER_A  = 2
var ID_HANDLER_B  = 3
var ID_DEVICE_A   = 4
var ID_DEVICE_B   = 5
var NUMBER_OF_IDS = 6

var KIND_DEVICE   = 0
var KIND_WORK     = 1

/**
 * A task control block manages a task and the queue of work packages associated
 * with it.
 * @param {TaskControlBlock} link the preceding block in the linked block list
 * @param {int} id the id of this block
 * @param {int} priority the priority of this block
 * @param {Packet} queue the queue of packages to be processed by the task
 * @param {Task} task the task
 * @constructor
 */
class TaskControlBlock(var link: TaskControlBlock?, var id: Int, var priority: Int, var queue: Packet?, var task: Task) {
    var state = 0
    init {
        if (queue == null) {
            this.state = STATE_SUSPENDED
        } else {
            this.state = STATE_SUSPENDED_RUNNABLE
        }
    }

    fun setRunning() {
        this.state = STATE_RUNNING
    }

    fun markAsNotHeld() {
        this.state = this.state and STATE_NOT_HELD
    }

    fun markAsHeld() {
        this.state = this.state or STATE_HELD
    }

    fun isHeldOrSuspended(): Boolean {
        return (this.state and STATE_HELD) != 0 || (this.state == STATE_SUSPENDED)
    }

    fun markAsSuspended() {
        this.state = this.state or STATE_SUSPENDED
    }

    fun markAsRunnable() {
        this.state = this.state or STATE_RUNNABLE
    }

    /**
     * Runs this task, if it is ready to be run, and returns the next task to run.
     */
    fun run(): TaskControlBlock? {
        val packet: Packet?
        if (this.state == STATE_SUSPENDED_RUNNABLE) {
            packet = this.queue
            this.queue = packet?.link
            if (this.queue == null) {
                this.state = STATE_RUNNING
            } else {
                this.state = STATE_RUNNABLE
            }
        } else {
            packet = null
        }
        return this.task.run(packet)
    }

    /**
     * Adds a packet to the work list of this block's task, marks this as runnable if
     * necessary, and returns the next runnable object to run (the one
     * with the highest priority).
     */
    fun checkPriorityAdd(task: TaskControlBlock, packet: Packet): TaskControlBlock {
        if (this.queue == null) {
            this.queue = packet
            this.markAsRunnable()
            if (this.priority > task.priority) return this
        } else {
            this.queue = packet.addTo(this.queue)
        }
        return task
    }

    override fun toString(): String {
        return "tcb { " + this.task + "@" + this.state + " }"
    }

}

/**
 * The task is running and is currently scheduled.
 */
var STATE_RUNNING = 0

/**
 * The task has packets left to process.
 */
var STATE_RUNNABLE = 1

/**
 * The task is not currently running.  The task is not blocked as such and may
 * be started by the scheduler.
 */
var STATE_SUSPENDED = 2

/**
 * The task is blocked and cannot be run until it is explicitly released.
 */
var STATE_HELD = 4

var STATE_SUSPENDED_RUNNABLE = STATE_SUSPENDED or STATE_RUNNABLE
var STATE_NOT_HELD = STATE_HELD.inv()

interface Task {
    fun run(packet: Packet?): TaskControlBlock?
}

/**
 * An idle task doesn't do any work itself but cycles control between the two
 * device tasks.
 * @param {Scheduler} scheduler the scheduler that manages this task
 * @param {int} v1 a seed value that controls how the device tasks are scheduled
 * @param {int} count the number of times this task should be scheduled
 * @constructor
 */
class IdleTask(var scheduler: Scheduler, var v1: Int, var count: Int): Task {
    override fun run(packet: Packet?): TaskControlBlock? {
        this.count--
        if (this.count == 0) return this.scheduler.holdCurrent()
        if ((this.v1 and 1) == 0) {
            this.v1 = this.v1 shr 1
            return this.scheduler.release(ID_DEVICE_A)
        } else {
            this.v1 = (this.v1 shr 1) xor 0xD008
            return this.scheduler.release(ID_DEVICE_B)
        }
    }

    override fun toString(): String {
        return "IdleTask"
    }
}

/**
 * A task that suspends itself after each time it has been run to simulate
 * waiting for data from an external device.
 * @param {Scheduler} scheduler the scheduler that manages this task
 * @constructor
 */
class DeviceTask(var scheduler: Scheduler): Task {
    var v1: Packet? = null

    override fun run(packet: Packet?): TaskControlBlock? {
        if (packet == null) {
            if (this.v1 == null) return this.scheduler.suspendCurrent()
            val v = this.v1
            this.v1 = null
            return this.scheduler.queue(v!!)
        } else {
            this.v1 = packet
            return this.scheduler.holdCurrent()
        }
    }

    override fun toString(): String {
        return "DeviceTask"
    }
}

/**
 * A task that manipulates work packets.
 * @param {Scheduler} scheduler the scheduler that manages this task
 * @param {int} v1 a seed used to specify how work packets are manipulated
 * @param {int} v2 another seed used to specify how work packets are manipulated
 * @constructor
 */
class WorkerTask(var scheduler: Scheduler, var v1: Int, var v2: Int): Task {
    override fun run(packet: Packet?): TaskControlBlock? {
        if (packet == null) {
            return this.scheduler.suspendCurrent()
        } else {
            if (this.v1 == ID_HANDLER_A) {
                this.v1 = ID_HANDLER_B
            } else {
                this.v1 = ID_HANDLER_A
            }
            packet.id = this.v1
            packet.a1 = 0
            for (i in 0 until DATA_SIZE) {
                this.v2++
                if (this.v2 > 26) this.v2 = 1
                packet.a2[i] = this.v2
            }
            return this.scheduler.queue(packet)
        }
    }

    override fun toString(): String {
        return "WorkerTask"
    }
}

/**
 * A task that manipulates work packets and then suspends itself.
 * @param {Scheduler} scheduler the scheduler that manages this task
 * @constructor
 */
class HandlerTask(var scheduler: Scheduler): Task {
    var v1: Packet? = null
    var v2: Packet? = null

    override fun run(packet: Packet?): TaskControlBlock? {
        if (packet != null) {
            if (packet.kind == KIND_WORK) {
                this.v1 = packet.addTo(this.v1)
            } else {
                this.v2 = packet.addTo(this.v2)
            }
        }
        this.v1?.let { v1 ->
            val count = this.v1!!.a1
            if (count < DATA_SIZE) {
                this.v2?.let { v2 ->
                    val v = v2
                    this.v2 = v2.link
                    v.a1 = v1.a2[count]
                    v1.a1 = count + 1
                    return this.scheduler.queue(v)
                }
            } else {
                val v = v1
                this.v1 = v1.link
                return this.scheduler.queue(v)
            }
        }
        return this.scheduler.suspendCurrent()
    }

    override fun toString(): String {
        return "HandlerTask"
    }
}

/* --- *
 * P a c k e t
 * --- */

var DATA_SIZE = 4

/**
 * A simple package of data that is manipulated by the tasks.  The exact layout
 * of the payload data carried by a packet is not important, and neither is the
 * nature of the work performed on packets by the tasks.
 *
 * Besides carrying data, packets form linked lists and are hence used both as
 * data and work lists.
 * @param {Packet} link the tail of the linked list of packets
 * @param {int} id an ID for this packet
 * @param {int} kind the type of this packet
 * @constructor
 */
class Packet(var link: Packet?, var id: Int, var kind: Int) {
    var a1: Int = 0
    var a2 = IntArray(DATA_SIZE)

    /**
     * Add this packet to the end of a work list, and return the work list.
     * @param {Packet} queue the work list to add this packet to
     */
    fun addTo(queue: Packet?): Packet {
        this.link = null
        if (queue == null) return this
        var next: Packet = queue
        var peek = next.link
        while (peek != null) {
            next = peek
            peek = next.link
        }
        next.link = this
        return queue
    }

    override fun toString(): String { 
        return "Packet"
    }
}
