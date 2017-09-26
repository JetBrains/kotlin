#!/usr/bin/python

##
# Copyright 2010-2017 JetBrains s.r.o.
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

#
# (lldb) command script import llvmDebugInfoC/src/scripts/konan_lldb.py
# (lldb) p kotlin_variable
#

import lldb
import ctypes

def kotlin_object_type_summary(lldb_val, internal_dict):
    """Hook that is run by lldb to display a Kotlin object."""
    fallback = lldb_val.GetValue()
    if str(lldb_val.type) != "struct ObjHeader *":
        return fallback

    def evaluate(expr):
        return lldb_val.GetTarget().EvaluateExpression(expr, lldb.SBExpressionOptions())

    buff_len = evaluate(
        "Konan_DebugObjectToUtf8Array((struct ObjHeader *) %s, Konan_DebugBuffer(), Konan_DebugBufferSize());" % lldb_val.GetValueAsUnsigned()
    ).unsigned

    if not buff_len:
        return fallback

    buff_addr = evaluate("Konan_DebugBuffer()").unsigned

    error = lldb.SBError()
    s = lldb_val.GetProcess().ReadCStringFromMemory(int(buff_addr), int(buff_len), error)
    return s if error.Success() else fallback


def __lldb_init_module(debugger, internal_dict):
    debugger.HandleCommand('\
        type summary add \
        --no-value \
        --python-function konan_lldb.kotlin_object_type_summary \
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('type category enable Kotlin')
