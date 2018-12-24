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


def lldb_val_to_ptr(lldb_val):
    addr = lldb_val.GetValueAsUnsigned()
    return '((struct ObjHeader *) {:#x})'.format(addr)


def evaluate(expr):
    return lldb.debugger.GetSelectedTarget().EvaluateExpression(expr, lldb.SBExpressionOptions())


def is_instance_of(addr, typeinfo):
    return evaluate("(bool)IsInstance({}, {})".format(addr, typeinfo)).GetValue() == "true"


def is_string(value):
    return is_instance_of(lldb_val_to_ptr(value), "theStringTypeInfo")


def is_array(value):
    return int(evaluate("(int)Konan_DebugIsArray({})".format(lldb_val_to_ptr(value))).GetValue()) == 1


def check_type_info(value):
    """This method checks self-referencing of pointer of first member of TypeInfo including case when object has an
    meta-object pointed by TypeInfo. Two lower bits are reserved for memory management needs see runtime/src/main/cpp/Memory.h."""
    if str(value.type) != "struct ObjHeader *":
        return False
    expr = "*(void **)((uintptr_t)(*(void**){0}) & ~0x3) == **(void***)((uintptr_t)(*(void**){0}) & ~0x3)".format(value.unsigned)
    result = evaluate(expr)
    return result.IsValid() and result.GetValue() == "true"


#
# Some kind of forward declaration.


__FACTORY = {}


def kotlin_object_type_summary(lldb_val, internal_dict):
    """Hook that is run by lldb to display a Kotlin object."""
    fallback = lldb_val.GetValue()
    if str(lldb_val.type) != "struct ObjHeader *":
        return fallback

    if not check_type_info(lldb_val):
        return NULL

    ptr = lldb_val_to_ptr(lldb_val)
    if ptr is None:
        return fallback

    return select_provider(lldb_val).to_string()


def select_provider(lldb_val):
    return __FACTORY['string'](lldb_val) if is_string(lldb_val) else __FACTORY['array'](lldb_val) if is_array(
        lldb_val) else __FACTORY['object'](lldb_val)


class KonanHelperProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj):
        self._target = lldb.debugger.GetSelectedTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj
        self._ptr = lldb_val_to_ptr(self._valobj)
        if is_string(valobj):
            return
        self._children_count = int(evaluate("(int)Konan_DebugGetFieldCount({})".format(self._ptr)).GetValue())
        self._children = []
        self._children_types = []
        self._children_type_names = []
        self._children_type_addresses = []
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
        self._children_types = [
            evaluate("(int)Konan_DebugGetFieldType({}, {})".format(self._ptr, child)).GetValueAsUnsigned()
            for child in range(self._children_count)]
        self._children_type_addresses = [
            long(evaluate("(void *)Konan_DebugGetFieldAddress({}, {})".format(self._ptr, index)).GetValue(), 0) for
            index in range(self._children_count)]

    def _read_string(self, expr, error):
        return self._process.ReadCStringFromMemory(long(evaluate(expr).GetValue(), 0), 0x1000, error)

    def _read_value(self, index):
        value_type = self._children_types[index]
        address = self._children_type_addresses[index]
        return self._type_conversion[int(value_type)](address, str(self._children[index]))

    def _create_synthetic_child(self, address, name):
        index = self.get_child_index(name)
        value = self._valobj.CreateChildAtOffset(str(name),
                                                 self._children_type_addresses[
                                                     index] - self._valobj.GetValueAsUnsigned(),
                                                 self._read_type(index))
        value.SetSyntheticChildrenGenerated(True)
        value.SetPreferSyntheticValue(True)
        return value

    def _read_type(self, index):
        return self._types[int(evaluate("(int)Konan_DebugGetFieldType({}, {})".format(self._ptr, index)).GetValue())]

    def _deref_or_obj_summary(self, index):
        value = self._values[index]
        if not value:
            print("_deref_or_obj_summary: value none, index:{}, type:{}".format(index, self._children_types[index]))
            return None
        if check_type_info(value):
            return kotlin_object_type_summary(value, None)
        else:
            return kotlin_object_type_summary(value.deref, None)


class KonanStringSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        super(KonanStringSyntheticProvider, self).__init__(valobj)
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
        s = self._process.ReadCStringFromMemory(long(buff_addr), int(buff_len), error)
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


class KonanObjectSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        super(KonanObjectSyntheticProvider, self).__init__(valobj)
        error = lldb.SBError()
        self._children = [
            self._read_string("(const char *)Konan_DebugGetFieldName({}, (int){})".format(self._ptr, i), error) for i in
            range(self._children_count) if error.Success()]
        if not error.Success():
            raise DebuggerException()
        self._values = [self._read_value(index) for index in range(self._children_count)]

    def num_children(self):
        return self._children_count

    def has_children(self):
        return self._children_count > 0

    def get_child_index(self, name):
        if not name in self._children:
            return -1
        return self._children.index(name)

    def get_child_at_index(self, index):
        return self._values[index]

    # TODO: fix cyclic structures stringification.
    def to_string(self):
        return dict([(self._children[i], self._deref_or_obj_summary(i)) for i in range(self._children_count)])


class KonanArraySyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        super(KonanArraySyntheticProvider, self).__init__(valobj)
        if self._ptr is None:
            return
        valobj.SetSyntheticChildrenGenerated(True)
        self._children = [x for x in range(self.num_children())]
        self._values = [self._read_value(i) for i in range(self._children_count)]

    def num_children(self):
        return self._children_count

    def has_children(self):
        return self._children_count > 0

    def get_child_index(self, name):
        index = int(name)
        return index if (0 <= index < self._children_count) else -1

    def get_child_at_index(self, index):
        return self._read_value(index)

    def to_string(self):
        return [self._deref_or_obj_summary(i) for i in range(self._children_count)]


class KonanProxyTypeProvider:
    def __init__(self, valobj, _):
        if not check_type_info(valobj):
            return
        self._proxy = select_provider(valobj)
        self.update()

    def __getattr__(self, item):
        return getattr(self._proxy, item)

def print_this_command(debugger, command, result, internal_dict):
    pthis = lldb.frame.FindVariable('<this>')
    print(pthis)

def __lldb_init_module(debugger, _):
    __FACTORY['object'] = lambda x: KonanObjectSyntheticProvider(x)
    __FACTORY['array'] = lambda x: KonanArraySyntheticProvider(x)
    __FACTORY['string'] = lambda x: KonanStringSyntheticProvider(x)
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
