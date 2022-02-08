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
import struct
import re
import sys
import os
import time
import io
import traceback

NULL = 'null'
logging=False
exe_logging=False
bench_logging=False

def log(msg):
    if logging:
        sys.stderr.write(msg())
        sys.stderr.write('\n')
    exelog(msg)

def exelog(stmt):
    if exe_logging:
        f = open(os.getenv('HOME', '') + "/lldbexelog.txt", "a")
        f.write(stmt())
        f.write("\n")
        f.close()

def bench(start, msg):
    if bench_logging:
        print("{}: {}".format(msg(), time.monotonic() - start))

def evaluate(expr):
    result = lldb.debugger.GetSelectedTarget().EvaluateExpression(expr)
    log(lambda : "evaluate: {} => {}".format(expr, result))
    return result


class DebuggerException(Exception):
    pass


_OUTPUT_MAX_CHILDREN = re.compile(r"target.max-children-count \(int\) = (.*)\n")
def _max_children_count():
    result = lldb.SBCommandReturnObject()
    lldb.debugger.GetCommandInterpreter().HandleCommand("settings show target.max-children-count", result, False)
    if not result.Succeeded():
        raise DebuggerException()
    v = _OUTPUT_MAX_CHILDREN.search(result.GetOutput()).group(1)
    return int(v)


def _symbol_loaded_address(name, debugger = lldb.debugger):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()
    candidates = list(filter(lambda x: x.name == name, frame.module.symbols))
    # take first
    for candidate in candidates:
        address = candidate.GetStartAddress().GetLoadAddress(target)
        log(lambda: "_symbol_loaded_address:{} {:#x}".format(name, address))
        return address

def _type_info_by_address(address, debugger = lldb.debugger):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()
    candidates = list(filter(lambda x: x.GetStartAddress().GetLoadAddress(target) == address, frame.module.symbols))
    return candidates

def is_instance_of(addr, typeinfo):
    return evaluate("(bool)IsInstance({:#x}, {:#x})".format(addr, typeinfo)).GetValue() == "true"

def is_string_or_array(value):
    start = time.monotonic()
    soa = evaluate("(int)IsInstance({0:#x}, {1:#x}) ? 1 : ((int)Konan_DebugIsArray({0:#x})) ? 2 : 0)"
                   .format(value.unsigned, _symbol_loaded_address('kclass:kotlin.String'))).unsigned
    log(lambda: "is_string_or_array:{:#x}:{}".format(value.unsigned, soa))
    bench(start, lambda: "is_string_or_array({:#x}) = {}".format(value.unsigned, soa))
    return soa

def type_info(value):
    """This method checks self-referencing of pointer of first member of TypeInfo including case when object has an
    meta-object pointed by TypeInfo. Two lower bits are reserved for memory management needs see runtime/src/main/cpp/Memory.h."""
    log(lambda: "type_info({:#x}: {})".format(value.unsigned, value.GetTypeName()))
    if value.GetTypeName() != "ObjHeader *":
        return None
    expr = "*(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) == **(void***)((uintptr_t)(*(void**){0:#x}) & ~0x3) " \
           "? *(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) : (void *)0".format(value.unsigned)
    result = evaluate(expr)

    return result.unsigned if result.IsValid() and result.unsigned != 0 else None


__FACTORY = {}


# Cache type info pointer to [ChildMetaInfo]
SYNTHETIC_OBJECT_LAYOUT_CACHE = {}
TO_STRING_DEPTH = 2
ARRAY_TO_STRING_LIMIT = 10

_TYPE_CONVERSION = [
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(void *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromAddress(name, address, value.type),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int8_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int16_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int32_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int64_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(float *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(double *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(void **){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(bool *){:#x}".format(address)),
     lambda obj, value, address, name: None]

_TYPES = [
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeVoid).GetPointerType(),
      lambda x: x.GetType(),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeChar),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeShort),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeInt),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeLongLong),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeFloat),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeDouble),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeVoid).GetPointerType(),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeBool)
]

def kotlin_object_type_summary(lldb_val, internal_dict = {}):
    """Hook that is run by lldb to display a Kotlin object."""
    start = time.monotonic()
    log(lambda: "kotlin_object_type_summary({:#x}: {})".format(lldb_val.unsigned, lldb_val.type.name))
    fallback = lldb_val.GetValue()
    if lldb_val.GetTypeName() != "ObjHeader *":
        if lldb_val.GetValue() is None:
            bench(start, lambda: "kotlin_object_type_summary:({:#x}) = NULL".format(lldb_val.unsigned))
            return NULL
        bench(start, lambda: "kotlin_object_type_summary:({:#x}) = {}".format(lldb_val.unsigned, lldb_val.signed))
        return lldb_val.value

    if lldb_val.unsigned == 0:
            bench(start, lambda: "kotlin_object_type_summary:({:#x}) = NULL".format(lldb_val.unsigned))
            return NULL
    tip = internal_dict["type_info"] if "type_info" in internal_dict.keys() else type_info(lldb_val)

    if not tip:
        bench(start, lambda: "kotlin_object_type_summary:({0:#x}) = falback:{0:#x}".format(lldb_val.unsigned))
        return fallback

    value = select_provider(lldb_val, tip, internal_dict)
    bench(start, lambda: "kotlin_object_type_summary:({:#x}) = value:{:#x}".format(lldb_val.unsigned, value._valobj.unsigned))
    start = time.monotonic()
    str0 = value.to_short_string()
    bench(start, lambda: "kotlin_object_type_summary:({:#x}) = str:'{}...'".format(lldb_val.unsigned, str0[:3]))
    return str0


def select_provider(lldb_val, tip, internal_dict):
    start = time.monotonic()
    log(lambda : "select_provider: {:#x} name:{} tip:{:#x}".format(lldb_val.unsigned, lldb_val.name, tip))
    soa = is_string_or_array(lldb_val)
    log(lambda : "select_provider: {:#x} : soa: {}".format(lldb_val.unsigned, soa))
    ret = __FACTORY['string'](lldb_val, tip, internal_dict) if soa == 1 \
        else __FACTORY['array'](lldb_val, tip, internal_dict) if soa == 2 \
        else __FACTORY['object'](lldb_val, tip, internal_dict)
    log(lambda: "select_provider({:#x}) = {}".format(lldb_val.unsigned, ret))
    bench(start, lambda: "select_provider({:#x})".format(lldb_val.unsigned))
    return ret

class KonanHelperProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj, amString, internal_dict = {}):
        self._target = lldb.debugger.GetSelectedTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj
        self._internal_dict = internal_dict.copy()
        if amString:
            return
        if self._children_count == 0:
            children_count = evaluate("(int)Konan_DebugGetFieldCount({:#x})".format(self._valobj.unsigned)).signed
            log(lambda: "(int)[{}].Konan_DebugGetFieldCount({:#x}) = {}".format(self._valobj.name,
                                                                                self._valobj.unsigned, children_count))
            self._children_count = children_count

    def _read_string(self, expr, error):
        return self._process.ReadCStringFromMemory(evaluate(expr).unsigned, 0x1000, error)

    def _read_value(self, index):
        value_type = self._field_type(index)
        address = self._field_address(index)
        log(lambda: "_read_value: [{}, type:{}, address:{:#x}]".format(index, value_type, address))
        return _TYPE_CONVERSION[int(value_type)](self, self._valobj, address, str(self._field_name(index)))

    def _read_type(self, index):
        type = _TYPES[self._field_type(index)](self._valobj)
        log(lambda: "type:{0} of {1:#x} of {2:#x}".format(type, self._valobj.unsigned,
                                                          self._valobj.unsigned + self._children[index].offset()))
        return type

    def _deref_or_obj_summary(self, index, internal_dict = {}):
        value = self._read_value(index)
        if not value:
            log(lambda : "_deref_or_obj_summary: value none, index:{}, type:{}".format(index, self._children[index].type()))
            return None
        return value.value if type_info(value) else value.deref.value

    def _field_address(self, index):
        return evaluate("(void *)Konan_DebugGetFieldAddress({:#x}, {})".format(self._valobj.unsigned, index)).unsigned

    def _field_type(self, index):
        return evaluate("(int)Konan_DebugGetFieldType({:#x}, {})".format(self._valobj.unsigned, index)).unsigned

    def to_string(self, representation):
        writer = io.StringIO()
        max_children_count=_max_children_count()
        limit = min(self._children_count, max_children_count)
        for i in range(limit):
            writer.write(representation(i))
            if (i != limit - 1):
                writer.write(", ")
        if max_children_count < self._children_count:
            writer.write(', ...')
        return "[{}]".format(writer.getvalue())


class KonanStringSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        log(lambda: "KonanStringSyntheticProvider:{:#x} name:{}".format(valobj.unsigned, valobj.name))
        self._children_count = 0
        super(KonanStringSyntheticProvider, self).__init__(valobj, True)
        fallback = valobj.GetValue()
        buff_addr = evaluate("(void *)Konan_DebugBuffer()").unsigned
        buff_len = evaluate(
            '(int)Konan_DebugObjectToUtf8Array({:#x}, (void *){:#x}, (int)Konan_DebugBufferSize());'.format(
                self._valobj.unsigned, buff_addr)
        ).signed

        if not buff_len:
            self._representation = fallback
            return

        error = lldb.SBError()
        s = self._process.ReadCStringFromMemory(int(buff_addr), int(buff_len), error)
        if not error.Success():
            raise DebuggerException()
        self._representation = s if error.Success() else fallback
        self._logger = lldb.formatters.Logger.Logger()

    def update(self):
        pass

    def num_children(self):
        return 0

    def has_children(self):
        return False

    def get_child_index(self, _):
        return None

    def get_child_at_index(self, _):
        return None

    def to_short_string(self):
        return self._representation

    def to_string(self):
        return self._representation


class KonanObjectSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, tip, internal_dict):
        # Save an extra call into the process
        log(lambda: "KonanObjectSyntheticProvider({:#x})".format(valobj.unsigned))
        self._children_count = 0
        super(KonanObjectSyntheticProvider, self).__init__(valobj, False, internal_dict)
        self._children = [self._field_name(i) for i in range(self._children_count)]
        log(lambda: "KonanObjectSyntheticProvider::__init__({:#x}) _children:{}".format(self._valobj.unsigned,
                                                                                        self._children))

    def _field_name(self, index):
        log(lambda: "KonanObjectSyntheticProvider::_field_name({:#x}, {})".format(self._valobj.unsigned, index))
        error = lldb.SBError()
        name =  self._read_string("(char *)Konan_DebugGetFieldName({:#x}, (int){})".format(self._valobj.unsigned,
                                                                                           index),
                                  error)
        if not error.Success():
            raise DebuggerException()
        log(lambda: "KonanObjectSyntheticProvider::_field_name({:#x}, {}) = {}".format(self._valobj.unsigned,
                                                                                       index, name))
        return name

    def num_children(self):
        log(lambda: "KonanObjectSyntheticProvider::num_children({:#x}) = {}".format(self._valobj.unsigned,
                                                                                    self._children_count))
        return self._children_count

    def has_children(self):
        log(lambda: "KonanObjectSyntheticProvider::has_children({:#x}) = {}".format(self._valobj.unsigned,
                                                                                    self._children_count > 0))
        return self._children_count > 0

    def get_child_index(self, name):
        log(lambda: "KonanObjectSyntheticProvider::get_child_index({:#x}, {})".format(self._valobj.unsigned, name))
        index = self._children.index(name)
        log(lambda: "KonanObjectSyntheticProvider::get_child_index({:#x}) index={}".format(self._valobj.unsigned,
                                                                                           index))
        return index

    def get_child_at_index(self, index):
        log(lambda: "KonanObjectSyntheticProvider::get_child_at_index({:#x}, {})".format(self._valobj.unsigned, index))
        return self._read_value(index)

    def to_short_string(self):
        log(lambda: "to_short_string:{:#x}".format(self._valobj.unsigned))
        return super().to_string(lambda index: "{}: ...".format(self._field_name(index)))

    def to_string(self):
        log(lambda: "to_string:{:#x}".format(self._valobj.unsigned))
        return super().to_string(lambda index: "{}: {}".format(self._field_name(index),
                                                               self._deref_or_obj_summary(index)))

class KonanArraySyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, internal_dict):
        self._children_count = 0
        super(KonanArraySyntheticProvider, self).__init__(valobj, False, internal_dict)
        log(lambda: "KonanArraySyntheticProvider: valobj:{:#x}".format(valobj.unsigned))
        if self._valobj is None:
            return
        valobj.SetSyntheticChildrenGenerated(True)
        type = self._field_type(0)

    def num_children(self):
        log(lambda: "KonanArraySyntheticProvider::num_children({:#x}) = {}".format(self._valobj.unsigned,
                                                                                   self._children_count))
        return self._children_count

    def has_children(self):
        log(lambda: "KonanArraySyntheticProvider::has_children({:#x}) = {}".format(self._valobj.unsigned,
                                                                                   self._children_count> 0))
        return self._children_count > 0

    def get_child_index(self, name):
        log(lambda: "KonanArraySyntheticProvider::get_child_index({:#x}, {})".format(self._valobj.unsigned, name))
        index = int(name)
        return index if (0 <= index < self._children_count) else -1

    def get_child_at_index(self, index):
        log(lambda: "KonanArraySyntheticProvider::get_child_at_index({:#x}, {})".format(self._valobj.unsigned, index))
        return self._read_value(index)

    def _field_name(self, index):
        log(lambda: "KonanArraySyntheticProvider::_field_name({:#x}, {})".format(self._valobj.unsigned, index))
        return str(index)

    def to_short_string(self):
        log(lambda: "to_short_string:{:#x}".format(self._valobj.unsigned))
        return super().to_string(lambda index: "...")

    def to_string(self):
        log(lambda: "to_string:{self._valobj.unsigned:#x}")
        return super().to_string(lambda index: "{}".format(self._deref_or_obj_summary(index)))

class KonanZerroSyntheticProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj):
        log(lambda: "KonanZerroSyntheticProvider::__init__ {}".format(valobj.name))


    def num_children(self):
        log(lambda: "KonanZerroSyntheticProvider::num_children")
        return 0

    def has_children(self):
        log(lambda: "KonanZerroSyntheticProvider::has_children")
        return False

    def get_child_index(self, name):
        log(lambda: "KonanZerroSyntheticProvider::get_child_index")
        return 0

    def get_child_at_index(self, index):
        log(lambda: "KonanZerroSyntheticProvider::get_child_at_index")
        return None

    def to_string(self):
        log(lambda: "KonanZerroSyntheticProvider::to_string")
        return NULL

    def to_short_string(self):
        log(lambda: "KonanZerroSyntheticProvider::to_short_string")
        return NULL

    def __getattr__(self, item):
        pass

class KonanNullSyntheticProvider(KonanZerroSyntheticProvider):
    def __init__(self, valobj):
        super(KonanNullSyntheticProvider, self).__init__(valobj)

class KonanNotInitializedObjectSyntheticProvider(KonanZerroSyntheticProvider):
    def __init__(self, valobj):
        super(KonanNotInitializedObjectSyntheticProvider, self).__init__(valobj)


class KonanProxyTypeProvider:
    def __init__(self, valobj, internal_dict):
        start = time.monotonic()
        log(lambda : "KonanProxyTypeProvider:{:#x}, name: {}".format(valobj.unsigned, valobj.name))
        if valobj.unsigned == 0:
           log(lambda : "KonanProxyTypeProvider:{:#x}, name: {} NULL syntectic {}".format(valobj.unsigned, valobj.name,
                                                                                          valobj.IsValid()))
           bench(start, lambda: "KonanProxyTypeProvider({:#x})".format(valobj.unsigned))
           self._proxy = KonanNullSyntheticProvider(valobj)
           return

        tip = type_info(valobj)
        if not tip:
           log(lambda : "KonanProxyTypeProvider:{:#x}, name: {} not initialized syntectic {}".format(valobj.unsigned,
                                                                                                     valobj.name,
                                                                                                     valobj.IsValid()))
           bench(start, lambda: "KonanProxyTypeProvider({:#x})".format(valobj.unsigned))
           self._proxy = KonanNotInitializedObjectSyntheticProvider(valobj)
           return
        log(lambda : "KonanProxyTypeProvider:{:#x} tip: {:#x}".format(valobj.unsigned, tip))
        self._proxy = select_provider(valobj, tip, internal_dict)
        bench(start, lambda: "KonanProxyTypeProvider({:#x})".format(valobj.unsigned))
        log(lambda: "KonanProxyTypeProvider:{:#x} _proxy: {}".format(valobj.unsigned, self._proxy.__class__.__name__))

    def __getattr__(self, item):
       return getattr(self._proxy, item)


def type_name_command(debugger, command, result, internal_dict):
    result.AppendMessage(evaluate('(char *)Konan_DebugGetTypeName({})'.format(command)).summary)


__KONAN_VARIABLE = re.compile('kvar:(.*)#internal')
__KONAN_VARIABLE_TYPE = re.compile('^kfun:<get-(.*)>\\(\\)(.*)$')
__TYPES_KONAN_TO_C = {
   'kotlin.Byte': ('int8_t', lambda v: v.signed),
   'kotlin.Short': ('short', lambda v: v.signed),
   'kotlin.Int': ('int', lambda v: v.signed),
   'kotlin.Long': ('long', lambda v: v.signed),
   'kotlin.UByte': ('int8_t', lambda v: v.unsigned),
   'kotlin.UShort': ('short', lambda v: v.unsigned),
   'kotlin.UInt': ('int', lambda v: v.unsigned),
   'kotlin.ULong': ('long', lambda v: v.unsigned),
   'kotlin.Char': ('short', lambda v: v.signed),
   'kotlin.Boolean': ('bool', lambda v: v.signed),
   'kotlin.Float': ('float', lambda v: v.value),
   'kotlin.Double': ('double', lambda v: v.value)
}


def type_by_address_command(debugger, command, result, internal_dict):
    result.AppendMessage("DEBUG: {}".format(command))
    tokens = command.split()
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    types = _type_info_by_address(tokens[0])
    result.AppendMessage("DEBUG: {}".format(types))
    for t in types:
        result.AppendMessage("{}: {:#x}".format(t.name, t.GetStartAddress().GetLoadAddress(target)))


def symbol_by_name_command(debugger, command, result, internal_dict):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()
    tokens = command.split()
    mask = re.compile(tokens[0])
    symbols = list(filter(lambda v: mask.match(v.name), frame.GetModule().symbols))
    visited = list()
    for symbol in symbols:
       name = symbol.name
       if name in visited:
           continue
       visited.append(name)
       result.AppendMessage("{}: {:#x}".format(name, symbol.GetStartAddress().GetLoadAddress(target)))


def konan_globals_command(debugger, command, result, internal_dict):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()

    konan_variable_symbols = list(filter(lambda v: __KONAN_VARIABLE.match(v.name), frame.GetModule().symbols))
    visited = list()
    for symbol in konan_variable_symbols:
       name = __KONAN_VARIABLE.search(symbol.name).group(1)

       if name in visited:
           continue
       visited.append(name)

       getters = list(filter(lambda v: re.match('^kfun:<get-{}>\\(\\).*$'.format(name), v.name), frame.module.symbols))
       if not getters:
           result.AppendMessage("storage not found for name:{}".format(name))
           continue

       getter_functions = frame.module.FindFunctions(getters[0].name)
       if not getter_functions:
           continue

       address = getter_functions[0].function.GetStartAddress().GetLoadAddress(target)
       type = __KONAN_VARIABLE_TYPE.search(getters[0].name).group(2)
       (c_type, extractor) = __TYPES_KONAN_TO_C[type] if type in __TYPES_KONAN_TO_C.keys() else ('ObjHeader *', lambda v: kotlin_object_type_summary(v))
       value = evaluate('(({0} (*)()){1:#x})()'.format(c_type, address))
       str_value = extractor(value)
       result.AppendMessage('{} {}: {}'.format(type, name, str_value))


def __lldb_init_module(debugger, _):
    log(lambda: "init start")
    __FACTORY['object'] = lambda x, y, z: KonanObjectSyntheticProvider(x, y, z)
    __FACTORY['array'] = lambda x, y, z: KonanArraySyntheticProvider(x, z)
    __FACTORY['string'] = lambda x, y, _: KonanStringSyntheticProvider(x)
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
        --python-class konan_lldb.KonanProxyTypeProvider \
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('type category enable Kotlin')
    debugger.HandleCommand('command script add -f {}.type_name_command type_name'.format(__name__))
    debugger.HandleCommand('command script add -f {}.type_by_address_command type_by_address'.format(__name__))
    debugger.HandleCommand('command script add -f {}.symbol_by_name_command symbol_by_name'.format(__name__))
    log(lambda: "init end")
