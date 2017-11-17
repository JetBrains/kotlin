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

def lldb_val_to_ptr(lldb_val):
    addr = lldb_val.GetValueAsUnsigned()
    if addr == 0:
        return None
    return '((struct ObjHeader *) {})'.format(addr)

def kotlin_object_type_summary(lldb_val, internal_dict):
    """Hook that is run by lldb to display a Kotlin object."""
    fallback = lldb_val.GetValue()
    if str(lldb_val.type) != "struct ObjHeader *":
        return fallback

    ptr = lldb_val_to_ptr(lldb_val)
    if ptr is None:
        return fallback

    def evaluate(expr):
        return lldb_val.GetTarget().EvaluateExpression(expr, lldb.SBExpressionOptions())


    buff_len = evaluate(
        "(int)Konan_DebugObjectToUtf8Array({}, (char *)Konan_DebugBuffer(), (int)Konan_DebugBufferSize());".format(ptr)
    ).unsigned

    if not buff_len:
        return fallback

    buff_addr = evaluate("(char *)Konan_DebugBuffer()").unsigned

    error = lldb.SBError()
    s = lldb_val.GetProcess().ReadCStringFromMemory(int(buff_addr), int(buff_len), error)
    return s if error.Success() else fallback


class KonanArraySyntheticChildrenProvider:
    def __init__(self, valobj, internal_dict):
        self.valobj = valobj

        self._element_type = None
        self._fn_prefix = None
        self._num_children = None
        self._ptr = None

    def update(self):
        self._element_type = None
        self._fn_prefix = None
        self._num_children = None
        self._ptr = lldb_val_to_ptr(self.valobj)

        if self._ptr is None:
            return

        types = [("Boolean", "uint8_t"), 
                 ("Byte", "int8_t"),
                 ("Char", "uint16_t"), 
                 ("Short", "int16_t"),
                 ("Int", "int32_t"),
                 ("Long", "int64_t"),
                 ("Float", "float"), 
                 ("Double", "double")]

        for (ktype, ctype) in types:
            instance_check = "IsInstance({self._ptr}, the{ktype}ArrayTypeInfo)".format(
                self=self, ktype=ktype
            )
            if self._evaluate_bool(instance_check):
                self._element_type = ctype
                self._fn_prefix = "Kotlin_{}Array".format(ktype)
                break
        else:
            return

        self._num_children = self._evaluate_int(
            "%s_getArrayLength(%s)" % (self._fn_prefix, self._ptr)
        )

    def num_children(self):
        if self._element_type is None:
            return None

        return self._num_children

    def has_children(self):
        if self._element_type is None:
            return None

        return self._num_children > 0

    def get_child_index(self, name):
        if self._element_type is None:
            return None

        try:
            index = int(name)
        except ValueError:
            return None

        return index if (0 <= index < self._num_children) else None
        
    def get_child_at_index(self, index):
        if self._element_type is None:
            return None

        if not (0 <= index < self._num_children):
            return None
        return self._evaluate(
            "({self._element_type}){self._fn_prefix}_get({self._ptr}, {index})".format(self=self, index=index),
            name=str(index)
        )

    def _evaluate(self, expr, name="tmp"):
        return self.valobj.CreateValueFromExpression(name, expr)

    def _evaluate_bool(self, expr):
        return self._evaluate("(uint8_t)" + expr).unsigned == 1

    def _evaluate_int(self, expr):
        return self._evaluate("(int32_t)" + expr).signed


def __lldb_init_module(debugger, internal_dict):
    debugger.HandleCommand('\
        type summary add \
        --no-value \
        --expand \
        --python-function konan_lldb.kotlin_object_type_summary \
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('\
        type synthetic add \
        --python-class konan_lldb.KonanArraySyntheticChildrenProvider\
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('type category enable Kotlin')
