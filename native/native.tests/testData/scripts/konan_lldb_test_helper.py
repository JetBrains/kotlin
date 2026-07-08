#!/usr/bin/python

# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

# Unlike konan_lldb.py, this file contains only scripts useful for internal testing of the compiler.

import lldb
import os
import threading
import time

# All the limits below must fire well before the test runner's execution timeout
# (10 minutes by default) kills the whole LLDB process: when that happens, everything
# buffered in the command result is lost and the test fails with no diagnostics at all.
SINGLE_STEP_TIMEOUT_SECONDS = 60
TOTAL_STEPPING_TIMEOUT_SECONDS = 300
MAX_STEP_COMMANDS = 10000

def _describe_process_state(process):
    lines = []
    for thread in process:
        lines.append(str(thread))
        for frame in thread:
            lines.append("    " + str(frame))
    return "\n".join(lines)

@lldb.command()
def step_through_current_frame(debugger, command, ctx, result, internal_dict):
    """Call step-into in a loop until the code exits the current frame, report each step in a new line"""
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    initial_stack_depth = thread.num_frames

    deadline = time.monotonic() + TOTAL_STEPPING_TIMEOUT_SECONDS
    steps = []

    def fail(reason):
        raise AssertionError(
            reason
            + "\nProcess state:\n" + _describe_process_state(process)
            + "\nRecorded steps: {} total, last 20:\n".format(len(steps))
            + "\n".join(steps[-20:])
        )

    step_timed_out = threading.Event()

    def interrupt_stuck_step():
        step_timed_out.set()
        process.SendAsyncInterrupt()

    step_commands = 0
    while True:
        step_commands += 1
        if step_commands > MAX_STEP_COMMANDS:
            fail(f"Didn't leave the initial frame after {MAX_STEP_COMMANDS} step-into commands")
        if time.monotonic() > deadline:
            fail(f"Stepping through the frame didn't finish in {TOTAL_STEPPING_TIMEOUT_SECONDS} seconds")

        watchdog = threading.Timer(SINGLE_STEP_TIMEOUT_SECONDS, interrupt_stuck_step)
        watchdog.start()
        try:
            thread.StepInto()
        finally:
            watchdog.cancel()

        if not process.is_alive:
            break
        if step_timed_out.is_set() and thread.stop_reason != lldb.eStopReasonPlanComplete:
            # The step-into didn't finish in time, and the process was forcibly interrupted
            # by the watchdog. (If the step still managed to complete, i.e. won the race against
            # the interrupt, the leftover interrupt surfaces as a signal stop and is skipped below.)
            fail(f"A single step-into command didn't complete in {SINGLE_STEP_TIMEOUT_SECONDS} seconds")
        if thread.stop_reason == lldb.eStopReasonSignal:
            # One possible reason for eStopReasonSignal is because of throwing a Kotlin exception.
            # Unfortunately, there doesn't seem to be a reliable way to verify that.
            # But if that's the case, the current location is likely outside of kotlin code
            # (e.g. somewhere in the K/N runtime or OS), so don't report it.
            continue
        if thread.stop_reason != lldb.eStopReasonPlanComplete:
            # The program has stopped execution for a reason other than the step-into requested earlier.
            # The debug information at this point may be unreliable, so to avoid flakiness we fail early.
            fail(f"Unexpected LLDB stop reason: {thread.stop_reason}")
        if thread.num_frames < initial_stack_depth:
            break

        frame = thread.frame[0]
        file_path = os.path.normpath(frame.line_entry.file.fullpath)
        line_number = frame.line_entry.line
        function_name = frame.function.name
        step = "//step " + "\u001f".join((file_path, str(line_number), function_name))
        steps.append(step)
        result.AppendMessage(step)