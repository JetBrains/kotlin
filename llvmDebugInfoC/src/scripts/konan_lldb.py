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

NULL = 'null'

def log(msg):
    if False:
        print(msg())

def exelog(stmt):
    if False:
        f = open(os.getenv('HOME', '') + "/lldbexelog.txt", "a")
        f.write(stmt())
        f.write("\n")
        f.close()

def lldb_val_to_ptr(lldb_val):
    addr = lldb_val.GetValueAsUnsigned()
    return '((struct ObjHeader *) {:#x})'.format(addr)


def evaluate(expr):
    result = lldb.debugger.GetSelectedTarget().EvaluateExpression(expr, lldb.SBExpressionOptions())
    evallog = lambda : "{} => {}".format(expr, result)
    log(evallog)
    exelog(evallog)
    return result

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
    return evaluate("(bool)IsInstance({}, {:#x})".format(addr, typeinfo)).GetValue() == "true"

def is_string_or_array(value):
    return evaluate("(int)IsInstance({0}, {1}) ? 1 : ((int)Konan_DebugIsArray({0}) ? 2 : 0)".format(lldb_val_to_ptr(value), _symbol_loaded_address('kclass:kotlin.String'))).unsigned

def type_info(value):
    """This method checks self-referencing of pointer of first member of TypeInfo including case when object has an
    meta-object pointed by TypeInfo. Two lower bits are reserved for memory management needs see runtime/src/main/cpp/Memory.h."""
    if value.GetTypeName() != "ObjHeader *":
        return False
    expr = "*(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) == **(void***)((uintptr_t)(*(void**){0:#x}) & ~0x3) ? *(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) : (void *)0".format(value.unsigned)
    result = evaluate(expr)
    return result.unsigned if result.IsValid() and result.unsigned != 0 else None


__FACTORY = {}


# Cache type info pointer to [ChildMetaInfo]
SYNTHETIC_OBJECT_LAYOUT_CACHE = {}
TO_STRING_DEPTH = 2
ARRAY_TO_STRING_LIMIT = 10

def kotlin_object_type_summary(lldb_val, internal_dict = {}):
    """Hook that is run by lldb to display a Kotlin object."""
    log(lambda: "kotlin_object_type_summary({:#x})".format(lldb_val.unsigned))
    fallback = lldb_val.GetValue()
    if str(lldb_val.type) != "struct ObjHeader *":
        if lldb_val.GetValue() is None:
            return NULL
        return lldb_val.GetValueAsSigned()

    if lldb_val.unsigned == 0:
            return NULL
    tip = internal_dict["type_info"] if "type_info" in internal_dict.keys() else type_info(lldb_val)

    if not tip:
        return fallback

    return select_provider(lldb_val, tip, internal_dict).to_string()


def select_provider(lldb_val, tip, internal_dict):
    soa = is_string_or_array(lldb_val)
    log(lambda : "select_provider: {} : {}".format(lldb_val, soa))
    return __FACTORY['string'](lldb_val, tip, internal_dict) if soa == 1 else __FACTORY['array'](lldb_val, tip, internal_dict) if soa == 2 \
        else __FACTORY['object'](lldb_val, tip, internal_dict)

class KonanHelperProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj, amString, internal_dict = {}):
        self._target = lldb.debugger.GetSelectedTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj
        self._ptr = lldb_val_to_ptr(self._valobj)
        if amString:
            return
        self._internal_dict = internal_dict.copy()
        self._to_string_depth = TO_STRING_DEPTH if "to_string_depth" not in self._internal_dict.keys() else  self._internal_dict["to_string_depth"]
        if self._children_count == 0:
            self._children_count = evaluate("(int)Konan_DebugGetFieldCount({})".format(self._ptr)).signed
        self._children = []
        self._type_conversion = [
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(void *){:#x}".format(address)),
            lambda address, name: self._create_synthetic_child(address, name),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(int8_t *){:#x}".format(address)),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(int16_t *){:#x}".format(address)),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(int32_t *){:#x}".format(address)),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(int64_t *){:#x}".format(address)),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(float *){:#x}".format(address)),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(double *){:#x}".format(address)),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(void **){:#x}".format(address)),
            lambda address, name: self._valobj.CreateValueFromExpression(name, "(bool *){:#x}".format(address)),
            lambda address, name: None]

        self._types = [
            valobj.GetType().GetBasicType(lldb.eBasicTypeVoid).GetPointerType(),
            valobj.GetType(),
            valobj.GetType().GetBasicType(lldb.eBasicTypeChar),
            valobj.GetType().GetBasicType(lldb.eBasicTypeShort),
            valobj.GetType().GetBasicType(lldb.eBasicTypeInt),
            valobj.GetType().GetBasicType(lldb.eBasicTypeLongLong),
            valobj.GetType().GetBasicType(lldb.eBasicTypeFloat),
            valobj.GetType().GetBasicType(lldb.eBasicTypeDouble),
            valobj.GetType().GetBasicType(lldb.eBasicTypeVoid).GetPointerType(),
            valobj.GetType().GetBasicType(lldb.eBasicTypeBool)
        ]

    def _read_string(self, expr, error):
        return self._process.ReadCStringFromMemory(evaluate(expr).unsigned, 0x1000, error)

    def _read_value(self, index):
        value_type = self._children[index].type()
        address = self._valobj.unsigned + self._children[index].offset()
        return self._type_conversion[int(value_type)](address, str(self._children[index].name()))

    def _create_synthetic_child(self, address, name):
        if self._to_string_depth == 0:
           return None
        index = self.get_child_index(name)
        value = self._valobj.CreateChildAtOffset(str(name),
                                                 self._children[index].offset(),
                                                 self._read_type(index))
        value.SetSyntheticChildrenGenerated(True)
        value.SetPreferSyntheticValue(True)
        return value

    def _read_type(self, index):
        type = self._types[self._children[index].type()]
        log(lambda: "type:{0} of {1:#x} of {2:#x}".format(type, self._valobj.unsigned, self._valobj.unsigned + self._children[index].offset()))
        return type

    def _deref_or_obj_summary(self, index, internal_dict):
        value = self._values[index]
        if not value:
            log(lambda : "_deref_or_obj_summary: value none, index:{}, type:{}".format(index, self._children[index].type()))
            return None

        tip = type_info(value)
        if tip:
            internal_dict["type_info"] = tip
            return kotlin_object_type_summary(value, internal_dict)
        tip = type_info(value.deref)

        if tip:
            internal_dict["type_info"] = tip
            return kotlin_object_type_summary(value.deref, internal_dict)

        return kotlin_object_type_summary(value.deref, internal_dict)

    def _field_address(self, index):
        return evaluate("(void *)Konan_DebugGetFieldAddress({}, {})".format(self._ptr, index)).unsigned

    def _field_type(self, index):
        return evaluate("(int)Konan_DebugGetFieldType({}, {})".format(self._ptr, index)).unsigned

class KonanStringSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        self._children_count = 0
        super(KonanStringSyntheticProvider, self).__init__(valobj, True)
        fallback = valobj.GetValue()
        buff_addr = evaluate("(void *)Konan_DebugBuffer()").unsigned
        buff_len = evaluate(
            '(int)Konan_DebugObjectToUtf8Array({}, (void *){:#x}, (int)Konan_DebugBufferSize());'.format(
                self._ptr, buff_addr)
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

    def to_string(self):
        return self._representation


class DebuggerException(Exception):
    pass

class MemberLayout:
    def __init__(self, name, type, offset):
        self._name = name
        self._type = type
        self._offset = offset

    def name(self):
        return self._name

    def type(self):
        return self._type

    def offset(self):
        return self._offset

class KonanObjectSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, tip, internal_dict):
        # Save an extra call into the process
        if tip in SYNTHETIC_OBJECT_LAYOUT_CACHE:
            log(lambda : "TIP: {:#x} EARLYHIT".format(tip))
            self._children = SYNTHETIC_OBJECT_LAYOUT_CACHE[tip]
            self._children_count = len(self._children)
        else:
            self._children_count = 0

        super(KonanObjectSyntheticProvider, self).__init__(valobj, False, internal_dict)

        if not tip in SYNTHETIC_OBJECT_LAYOUT_CACHE:
            SYNTHETIC_OBJECT_LAYOUT_CACHE[tip] = [
                MemberLayout(self._field_name(i), self._field_type(i), self._field_address(i) - self._valobj.unsigned)
                for i in range(self._children_count)]
            log(lambda : "TIP: {:#x} MISSED".format(tip))
        else:
            log(lambda : "TIP: {:#x} HIT".format(tip))
        self._children = SYNTHETIC_OBJECT_LAYOUT_CACHE[tip]
        self._values = [self._read_value(index) for index in range(self._children_count)]


    def _field_name(self, index):
        error = lldb.SBError()
        name =  self._read_string("(void *)Konan_DebugGetFieldName({}, (int){})".format(self._ptr, index), error)
        if not error.Success():
            raise DebuggerException()
        return name

    def num_children(self):
        return self._children_count

    def has_children(self):
        return self._children_count > 0

    def get_child_index(self, name):
        def __none(iterable, f):
            return not any(f(x) for x in iterable)
        if __none(self._children, lambda x: x.name() == name):
            return -1
        return next(i for i,v in enumerate(self._children) if v.name() == name)

    def get_child_at_index(self, index):
        result = self._values[index]
        if result is None:
            result = self._read_value(index)
            self._values[index] = result
        return result


    # TODO: fix cyclic structures stringification.
    def to_string(self):
        if self._to_string_depth == 0:
            return "..."
        else:
            internal_dict = self._internal_dict.copy()
            internal_dict["to_string_depth"] = self._to_string_depth - 1
            return dict([(self._children[i].name(), self._deref_or_obj_summary(i, internal_dict)) for i in range(self._children_count)])

class KonanArraySyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, internal_dict):
        self._children_count = 0
        super(KonanArraySyntheticProvider, self).__init__(valobj, False, internal_dict)
        if self._ptr is None:
            return
        valobj.SetSyntheticChildrenGenerated(True)
        type = self._field_type(0)
        zerro_address = self._field_address(0)
        first_address = self._field_address(1)
        offset = zerro_address - valobj.unsigned
        size = first_address - zerro_address
        self._children = [MemberLayout(str(x), type, offset + x * size) for x in range(self.num_children())]
        self._values = [self._read_value(i) for i in range(min(ARRAY_TO_STRING_LIMIT, self._children_count))]


    def cap_children_count(self):
        return self._children_count

    def num_children(self):
        return self.cap_children_count()

    def has_children(self):
        return self._children_count > 0

    def get_child_index(self, name):
        index = int(name)
        return index if (0 <= index < self._children_count) else -1

    def get_child_at_index(self, index):
        result = self._values[index]
        if result is None:
            result = self._read_value(index)
            self._values[index] = result
        return result

    def to_string(self):
        return [self._deref_or_obj_summary(i, self._internal_dict.copy()) for i in range(min(ARRAY_TO_STRING_LIMIT, self._children_count))]


class KonanProxyTypeProvider:
    def __init__(self, valobj, internal_dict):
        log(lambda : "proxy: {:#x}".format(valobj.unsigned))
        tip = type_info(valobj)
        log(lambda : "KonanProxyTypeProvider: tip: {:#x}".format(tip))
        if not tip:
            return
        self._proxy = select_provider(valobj, tip, internal_dict)
        log(lambda: "KonanProxyTypeProvider: _proxy: {}".format(self._proxy.__class__.__name__))
        self.update()

    def __getattr__(self, item):
        return getattr(self._proxy, item)

def clear_cache_command(debugger, command, result, internal_dict):
    SYNTHETIC_OBJECT_LAYOUT_CACHE.clear()


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
       (c_type, extractor) = __TYPES_KONAN_TO_C[type] if type in __TYPES_KONAN_TO_C.keys() else ('struct ObjHeader *', lambda v: kotlin_object_type_summary(v))
       value = evaluate('(({0} (*)()){1:#x})()'.format(c_type, address))
       str_value = extractor(value)
       result.AppendMessage('{} {}: {}'.format(type, name, str_value))

def __lldb_init_module(debugger, _):
    __FACTORY['object'] = lambda x, y, z: KonanObjectSyntheticProvider(x, y, z)
    __FACTORY['array'] = lambda x, y, z: KonanArraySyntheticProvider(x, z)
    __FACTORY['string'] = lambda x, y, _: KonanStringSyntheticProvider(x)
    debugger.HandleCommand('\
        type summary add \
        --no-value \
        --expand \
        --python-function konan_lldb.kotlin_object_type_summary \
        "struct ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('\
        type synthetic add \
        --python-class konan_lldb.KonanProxyTypeProvider\
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('type category enable Kotlin')
    debugger.HandleCommand('command script add -f {}.clear_cache_command clear_kotlin_cache'.format(__name__))
    debugger.HandleCommand('command script add -f {}.type_name_command type_name'.format(__name__))
    debugger.HandleCommand('command script add -f {}.type_by_address_command type_by_address'.format(__name__))
    debugger.HandleCommand('command script add -f {}.symbol_by_name_command symbol_by_name'.format(__name__))

