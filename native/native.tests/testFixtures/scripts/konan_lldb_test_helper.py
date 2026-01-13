#!/usr/bin/python

# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

# Unlike konan_lldb.py, this file contains only scripts useful for internal testing of the compiler.

import lldb

@lldb.command()
def step_through_current_frame(debugger, command, ctx, result, internal_dict):
    """Call step-into in a loop until the code exits the current frame, report each step in a new line"""
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    initial_stack_depth = thread.num_frames

    while True:
        thread.StepInto()

        if not process.is_alive:
            break
        if thread.stop_reason == lldb.eStopReasonSignal:
            # One possible reason for eStopReasonSignal is because of throwing a Kotlin exception.
            # Unfortunately, there doesn't seem to be a reliable way to verify that.
            # But if that's the case, the current location is likely outside of kotlin code
            # (e.g. somewhere in the K/N runtime or OS), so don't report it.
            continue
        if thread.stop_reason != lldb.eStopReasonPlanComplete:
            # The program has stopped execution for a reason other than the step-into requested earlier.
            # The debug information at this point may be unreliable, so to avoid flakiness we fail early.
            raise AssertionError(f"Unexpected LLDB stop reason: {thread.stop_reason}")
        if thread.num_frames < initial_stack_depth:
            break

        frame = thread.frame[0]
        file_name = frame.line_entry.file.basename
        line_number = frame.line_entry.line
        function_name = frame.function.name
        result.AppendMessage("//step " + "\u001f".join((file_name, str(line_number), function_name)))