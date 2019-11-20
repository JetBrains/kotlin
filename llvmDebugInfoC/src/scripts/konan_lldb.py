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

def is_instance_of(addr, typeinfo):
    return evaluate("(bool)IsInstance({}, {})".format(addr, typeinfo)).GetValue() == "true"

def is_string_or_array(value):
    return evaluate("(bool)IsInstance({0}, theStringTypeInfo) ? 1 : ((int)Konan_DebugIsArray({0}) ? 2 : 0)".format(lldb_val_to_ptr(value))).unsigned

def type_info(value):
    """This method checks self-referencing of pointer of first member of TypeInfo including case when object has an
    meta-object pointed by TypeInfo. Two lower bits are reserved for memory management needs see runtime/src/main/cpp/Memory.h."""
    if str(value.type) != "struct ObjHeader *":
        return False
    expr = "*(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) == **(void***)((uintptr_t)(*(void**){0:#x}) & ~0x3) ? *(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) : (void *)0".format(value.unsigned)
    result = evaluate(expr)
    return result.unsigned if result.IsValid() and result.unsigned != 0 else None


__FACTORY = {}


# Cache type info pointer to [ChildMetaInfo]
SYNTHETIC_OBJECT_LAYOUT_CACHE = {}
TO_STRING_DEPTH = 2
ARRAY_TO_STRING_LIMIT = 10

def kotlin_object_type_summary(lldb_val, internal_dict = []):
    """Hook that is run by lldb to display a Kotlin object."""
    log(lambda: "kotlin_object_type_summary({:#x})".format(lldb_val.unsigned))
    fallback = lldb_val.GetValue()
    if str(lldb_val.type) != "struct ObjHeader *":
        return fallback

    ptr = lldb_val_to_ptr(lldb_val)
    if ptr is None:
        return fallback

    tip = internal_dict["type_info"] if "type_info" in internal_dict.keys() else type_info(lldb_val)
    if not tip:
        return fallback

    return select_provider(lldb_val, tip, internal_dict).to_string()


def select_provider(lldb_val, tip, internal_dict):
    soa = is_string_or_array(lldb_val)
    return __FACTORY['string'](lldb_val, tip, internal_dict) if soa == 1 else __FACTORY['array'](lldb_val, tip, internal_dict) if soa == 2 \
        else __FACTORY['object'](lldb_val, tip, internal_dict)

class KonanHelperProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj, amString, internal_dict = []):
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
        buff_len = evaluate(
            '(int)Konan_DebugObjectToUtf8Array({}, (char *)Konan_DebugBuffer(), (int)Konan_DebugBufferSize());'.format(
                self._ptr)
        ).unsigned

        if not buff_len:
            self._representation = fallback
            return

        buff_addr = evaluate("(char *)Konan_DebugBuffer()").unsigned

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
        name =  self._read_string("(const char *)Konan_DebugGetFieldName({}, (int){})".format(self._ptr, index), error)
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
        if not tip:
            return
        self._proxy = select_provider(valobj, tip, internal_dict)
        self.update()

    def __getattr__(self, item):
        return getattr(self._proxy, item)

def print_this_command(debugger, command, result, internal_dict):
    pthis = lldb.frame.FindVariable('<this>')
    print(pthis)

def clear_cache_command(debugger, command, result, internal_dict):
    SYNTHETIC_OBJECT_LAYOUT_CACHE.clear()

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
    debugger.HandleCommand('command script add -f {}.print_this_command print_this'.format(__name__))
    debugger.HandleCommand('command script add -f {}.clear_cache_command clear_kotlin_cache'.format(__name__))
