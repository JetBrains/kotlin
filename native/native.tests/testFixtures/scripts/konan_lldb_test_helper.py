#!/usr/bin/python

##
# Copyright 2010-2025 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Unlike konan_lldb.py, this file contains only scripts useful for internal testing of the compiler.

import lldb

@lldb.command()
def step_through_current_frame(debugger, command, ctx, result, internal_dict):
    """Call step-into in a loop until the code exits the current frame, report each step in a new line"""
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    initial_stack_depth = len(thread.frames)

    while True:
        thread.StepInto()
        if len(thread.frames) < initial_stack_depth:
            break

        frame = thread.frame[0]
        file_name = ''
        line_number = 0
        function_name = ''
        if frame.line_entry:
            line_number = frame.line_entry.line
            if frame.line_entry.file:
                file_name = frame.line_entry.file.basename
        if frame.function:
            function_name = frame.function.name

        result.AppendMessage("//step " + "\u001f".join((file_name, str(line_number), function_name)))