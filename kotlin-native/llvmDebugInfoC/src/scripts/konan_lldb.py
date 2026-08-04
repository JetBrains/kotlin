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

#
# (lldb) command script import llvmDebugInfoC/src/scripts/konan_lldb.py
# (lldb) p kotlin_variable
#

import io
import os
import re
import struct
import sys
import time
import logging
from enum import Enum

import lldb

_NULL = "null"
_BENCH_LOGGING = False
_FAST_ARRAY_PREFETCH_RADIUS = 200
_RUNTIME_TYPE_INVALID = 0
_RUNTIME_TYPE_OBJECT = 1
_RUNTIME_TYPE_VECTOR128 = 10


class CollectionKind(Enum):
    LIST = "list"
    MAP = "map"
    SET = "set"


def initialize_expression_options():
    options = lldb.SBExpressionOptions()
    options.SetIgnoreBreakpoints(True)
    options.SetAutoApplyFixIts(False)
    options.SetFetchDynamicValue(False)
    options.SetGenerateDebugInfo(False)
    options.SetSuppressPersistentResult(True)
    options.SetREPLMode(False)
    options.SetAllowJIT(True)
    options.SetLanguage(lldb.eLanguageTypeC_plus_plus_20)
    return options


_EXPRESSION_OPTIONS = initialize_expression_options()


def _bench(start, msg):
    if _BENCH_LOGGING:
        print(f"{msg()}: {time.monotonic() - start}")

def _evaluate(expr):
    target = lldb.debugger.GetSelectedTarget()
    if not target:
        raise DebuggerException("No target selected")

    process = target.GetProcess()
    if not process:
        raise DebuggerException("No process found")

    thread = process.GetSelectedThread()
    if not thread:
        # Try to select the first thread if none is selected
        if process.GetNumThreads() > 0:
            thread = process.GetThreadAtIndex(0)
            process.SetSelectedThread(thread)
        else:
            raise DebuggerException("No threads available")

    frame = thread.GetSelectedFrame()
    if not frame:
        # Try to select the first frame if none is selected
        if thread.GetNumFrames() > 0:
            frame = thread.GetFrameAtIndex(0)
            thread.SetSelectedFrame(0)
        else:
            raise DebuggerException("No frames available")

    # Store original frame information
    original_frame = frame
    original_frame_id = frame.GetFrameID()

    result = frame.EvaluateExpression(expr, _EXPRESSION_OPTIONS)

    # Try to find and restore the original frame
    current_frame = thread.GetSelectedFrame()
    if current_frame != original_frame:
        logging.debug(
            "Warning: Frame changed during evaluation from %s to %s",
            original_frame,
            current_frame,
        )
        # Try to find and restore the original frame
        for idx in range(thread.GetNumFrames()):
            frame = thread.GetFrameAtIndex(idx)
            if frame.GetFrameID() == original_frame_id:
                thread.SetSelectedFrame(idx)
                logging.debug("Restored original frame")
                break

    err = result.GetError()
    if not err.Success():
        logging.debug(
            "Expression evaluation failed: %s - %s", expr, err.description
        )
        raise EvaluateDebuggerException(expr, err)
    logging.debug("%s => %s", expr, result)
    return result


class DebuggerException(Exception):
    pass


class EvaluateDebuggerException(DebuggerException):
    def __init__(self, expression, error):
        self.expression = expression
        self.error = error

    def __str__(self):
        return (
            f"Error evaluating `{self.expression}`: "
            f"{self.error.description}"
        )


def _max_children_count():
    v = lldb.debugger.GetInternalVariableValue(
        "target.max-children-count", lldb.debugger.GetInstanceName()
    ).GetStringAtIndex(0)
    return int(v)

def _symbol_loaded_address(name, debugger=lldb.debugger):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()
    candidates = frame.module.symbol[name]
    # take first
    for candidate in candidates:
        address = candidate.GetStartAddress().GetLoadAddress(target)
        logging.debug("%s %s", name, _hex(address))
        return address

    return 0


def _type_info_by_address(address, debugger=lldb.debugger):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()
    candidates = list(
        filter(
            lambda x: x.GetStartAddress().GetLoadAddress(target) == address,
            frame.module.symbols,
        )
    )
    return candidates


def _is_string_or_array(value):
    start = time.monotonic()
    value_str = f"{_hex(value.unsigned)}"
    string_addr = _symbol_loaded_address("kclass:kotlin.String")
    expr = (
        f"((int)Konan_DebugIsInstance({value_str}, {_hex(string_addr)}) ? 1 "
        f": (((int)Konan_DebugIsArray({value_str})) ? 2 : 0))"
    )
    soa = _evaluate(expr).unsigned
    logging.debug("%s: %s", value_str, soa)
    _bench(start, lambda: f"is_string_or_array({value_str}) = {soa}")
    return soa


def _is_kotlin_list(value):
    list_addr = _symbol_loaded_address("kclass:kotlin.collections.List")
    if list_addr == 0:
        return False
    return _evaluate(
        f"(int)Konan_DebugIsInstance({_hex(value.unsigned)}, {_hex(list_addr)})"
    ).unsigned != 0


def _is_kotlin_map(value):
    map_addr = _symbol_loaded_address("kclass:kotlin.collections.Map")
    if map_addr == 0:
        return False
    return _evaluate(
        f"(int)Konan_DebugIsInstance({_hex(value.unsigned)}, {_hex(map_addr)})"
    ).unsigned != 0


def _is_kotlin_set(value):
    set_addr = _symbol_loaded_address("kclass:kotlin.collections.Set")
    if set_addr == 0:
        return False
    return _evaluate(
        f"(int)Konan_DebugIsInstance({_hex(value.unsigned)}, {_hex(set_addr)})"
    ).unsigned != 0


def _type_info(value):
    """
    This method checks self-referencing of pointer of first member of TypeInfo
    including a case when an object has a meta-object pointed by TypeInfo.

    Two lower bits are reserved for memory management needs,
    see runtime/src/main/cpp/Memory.h.
    """
    value_str = f"{_hex(value.unsigned)}"
    logging.debug("%s: %s", value_str, value.GetTypeName())
    if value.GetTypeName() != "ObjHeader *":
        return None
    result = _evaluate(
        (
            f"*(void **)((uintptr_t)(*(void**){value_str}) & ~0x3)"
            f" == "
            f"**(void***)((uintptr_t)(*(void**){value_str}) & ~0x3)"
            f" ? "
            f"*(void **)((uintptr_t)(*(void**){value_str}) & ~0x3)"
            f" : "
            f"(void *)0"
        )
    )

    return (
        result.unsigned if result.IsValid() and result.unsigned != 0 else None
    )


_FACTORY = {}
_ARRAY_CHILD_QUERY_CACHE = {}
_ARRAY_ELEMENT_SUMMARY_CACHE = {}
_ARRAY_ELEMENT_FIELD_TYPE_RESULT_CACHE = {}
_ARRAY_QUERY_CACHE_STATE = None
_TO_STRING_DEPTH = 2
_ARRAY_TO_STRING_LIMIT = 10
_TOTAL_MEMBERS_LIMIT = 50


class CachedArrayResponses:
    def __init__(self):
        self.child_addresses_by_index = {}
        self.child_types_by_index = {}
        self.resolved_proxy = None


def _clear_sbvalue_query_cache(reason="manual"):
    global _ARRAY_CHILD_QUERY_CACHE, _ARRAY_ELEMENT_SUMMARY_CACHE
    global _ARRAY_ELEMENT_FIELD_TYPE_RESULT_CACHE
    global _ARRAY_QUERY_CACHE_STATE
    _ARRAY_CHILD_QUERY_CACHE = {}
    _ARRAY_ELEMENT_SUMMARY_CACHE = {}
    _ARRAY_ELEMENT_FIELD_TYPE_RESULT_CACHE = {}
    _ARRAY_QUERY_CACHE_STATE = None


def _array_query_cache_state(process):
    if process is None or not process.IsValid():
        return None
    return process.GetUniqueID(), process.GetStopID(False)


def _ensure_array_query_cache_state(process):
    global _ARRAY_QUERY_CACHE_STATE
    state = _array_query_cache_state(process)
    if state is None:
        _clear_sbvalue_query_cache("invalid-process")
        return None, False
    if _ARRAY_QUERY_CACHE_STATE != state:
        _clear_sbvalue_query_cache("state-change")
        _ARRAY_QUERY_CACHE_STATE = state
        return state, False
    return state, True


def _array_query_cache_key(value):
    return value.GetValueAsUnsigned()


def _get_cached_array_responses(value):
    _, is_cache_valid = _ensure_array_query_cache_state(value.GetProcess())
    if not is_cache_valid:
        return None
    return _ARRAY_CHILD_QUERY_CACHE.get(_array_query_cache_key(value))


def _get_or_create_cached_array_responses(value):
    process = value.GetProcess()
    return _get_or_create_cached_array_responses_for_key(
        process, _array_query_cache_key(value)
    )


def _get_or_create_cached_array_responses_for_key(process, key):
    global _ARRAY_QUERY_CACHE_STATE
    state = _array_query_cache_state(process)
    if state is None:
        _clear_sbvalue_query_cache("invalid-process-create")
        return None
    if _ARRAY_QUERY_CACHE_STATE != state:
        _clear_sbvalue_query_cache("state-change-create")
        _ARRAY_QUERY_CACHE_STATE = state
    responses = _ARRAY_CHILD_QUERY_CACHE.get(key)
    if responses is None:
        responses = CachedArrayResponses()
        _ARRAY_CHILD_QUERY_CACHE[key] = responses
    return responses


def _get_cached_child_addresses_by_index(value):
    responses = _get_or_create_cached_array_responses(value)
    return None if responses is None else responses.child_addresses_by_index


def _get_cached_child_types_by_index(value):
    responses = _get_or_create_cached_array_responses(value)
    return None if responses is None else responses.child_types_by_index


def _cache_child_address(value, index, address):
    cached_addresses = _get_cached_child_addresses_by_index(value)
    if cached_addresses is not None:
        cached_addresses[index] = address


def _cache_child_type(value, index, child_type):
    cached_types = _get_cached_child_types_by_index(value)
    if cached_types is not None:
        cached_types[index] = child_type


def _get_cached_resolved_proxy(value):
    if value is None or value.GetValueAsUnsigned() == 0:
        return None
    responses = _get_cached_array_responses(value)
    return None if responses is None else responses.resolved_proxy


def _set_cached_resolved_proxy(value, proxy):
    if value is None or value.GetValueAsUnsigned() == 0 or proxy is None:
        return
    responses = _get_or_create_cached_array_responses(value)
    if responses is not None:
        responses.resolved_proxy = proxy


def _get_cached_summary_for_key(process, key):
    if key == 0:
        return None
    _, is_cache_valid = _ensure_array_query_cache_state(process)
    if not is_cache_valid:
        return None
    return _ARRAY_ELEMENT_SUMMARY_CACHE.get(key)


def _set_cached_summary_for_key(process, key, summary):
    if key == 0 or summary is None:
        return
    _get_or_create_cached_array_responses_for_key(process, key)
    _ARRAY_ELEMENT_SUMMARY_CACHE[key] = summary


def _get_cached_field_type_result_for_key(process, key):
    if key == 0:
        return None
    _, is_cache_valid = _ensure_array_query_cache_state(process)
    if not is_cache_valid:
        return None
    return _ARRAY_ELEMENT_FIELD_TYPE_RESULT_CACHE.get(key)


def _set_cached_field_type_result_for_key(process, key, type_name):
    if key == 0 or type_name is None or len(type_name) == 0:
        return
    _get_or_create_cached_array_responses_for_key(process, key)
    _ARRAY_ELEMENT_FIELD_TYPE_RESULT_CACHE[key] = type_name


def _type_name_for_runtime_type(runtime_type):
    return {
        1: "ObjHeader",
        2: "Byte",
        3: "Short",
        4: "Int",
        5: "Long",
        6: "Float",
        7: "Double",
        8: "NativePtr",
        9: "Boolean",
        10: "Vector128",
    }.get(runtime_type)


def _object_type_info_from_memory(value):
    process = value.GetProcess()
    target = lldb.debugger.GetSelectedTarget()
    object_address = value.GetValueAsUnsigned()
    if not process.IsValid() or not target.IsValid() or object_address == 0:
        return None

    pointer_size = target.GetAddressByteSize()
    pointer_format = "Q" if pointer_size == 8 else "I"
    prefix = ">" if target.GetByteOrder() == lldb.eByteOrderBig else "<"

    def read_pointer(address):
        error = lldb.SBError()
        raw = process.ReadMemory(address, pointer_size, error)
        if not error.Success():
            return 0
        return struct.unpack(f"{prefix}{pointer_format}", raw)[0]

    type_info = read_pointer(object_address) & ~0x3
    return type_info if type_info and read_pointer(type_info) == type_info else None


def _children_count(value):
    value_str = f"{_hex(value.unsigned)}"
    return (
        0
        if value.GetValueAsUnsigned() == 0
        else _evaluate(f"(int)Konan_DebugGetFieldCount({value_str})").signed
    )

_TYPE_CONVERSION = [
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(void *){_hex(address)}"
    ),
    lambda obj, value, address, name: value.CreateValueFromAddress(
        name, address, value.type
    ),
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(int8_t *){_hex(address)}"
    ).deref,
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(int16_t *){_hex(address)}"
    ).deref,
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(int32_t *){_hex(address)}"
    ).deref,
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(int64_t *){_hex(address)}"
    ).deref,
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(float *){_hex(address)}"
    ).deref,
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(double *){_hex(address)}"
    ).deref,
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(void **){_hex(address)}"
    ),
    lambda obj, value, address, name: value.CreateValueFromExpression(
        name, f"(bool *){_hex(address)}"
    ).deref,
    lambda obj, value, address, name: None,
]

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
    lambda x: x.GetType().GetBasicType(lldb.eBasicTypeBool),
]


def _read_string(addr, size):
    error = lldb.SBError()
    s = (
        lldb.debugger.GetSelectedTarget()
        .GetProcess()
        .ReadCStringFromMemory(int(addr), int(size), error)
    )
    if not error.Success():
        raise DebuggerException()
    return s


def _render_object(addr):
    process = lldb.debugger.GetSelectedTarget().GetProcess()
    cached = _get_cached_summary_for_key(process, addr)
    if cached is not None:
        return cached
    buff_addr = _evaluate("(void *)Konan_DebugBuffer()").unsigned
    buff_len = _evaluate(
        (
            f"(int)Konan_DebugObjectToUtf8Array("
            f"{_hex(addr)}, "
            f"(void *){_hex(buff_addr)}, "
            f"(int)Konan_DebugBufferSize()"
            f");"
        )
    ).signed
    return _read_string(buff_addr, buff_len)


def kotlin_object_type_summary(lldb_val, _):
    """
    Hook that is run by lldb to display a Kotlin object.
    """
    logging.debug("%s: %s", _hex(lldb_val.unsigned), lldb_val.type.name)
    if lldb_val.GetTypeName() != "ObjHeader *":
        if lldb_val.GetValue() is None:
            return _NULL
        return lldb_val.value

    if lldb_val.unsigned == 0:
        return _NULL
    summary = _render_object(lldb_val.unsigned)
    return "\"\"" if summary == "" else summary


def kotlin_object_pair_type_summary(lldb_val, _):
    summaries = []
    for index in range(2):
        child = lldb_val.GetChildAtIndex(index)
        summary = child.GetSummary()
        value = summary if summary is not None else child.GetValue()
        summaries.append(_NULL if value is None else value)
    return " = ".join(summaries)


def _select_provider(lldb_val, tip, internal_dict):
    start = time.monotonic()
    value_str = f"{_hex(lldb_val.unsigned)}"
    logging.debug("%s name:%s tip:%s", value_str, lldb_val.name, _hex(tip))
    soa = _is_string_or_array(lldb_val)
    logging.debug("%s soa: %s", value_str, soa)
    raw_provider = (
        _FACTORY["string"](lldb_val, tip, internal_dict)
        if soa == 1
        else (
            _FACTORY["array"](lldb_val, tip, internal_dict)
            if soa == 2
            else _FACTORY["object"](lldb_val, tip, internal_dict)
        )
    )

    ret = raw_provider
    if isinstance(raw_provider, FastKonanObjectSyntheticProvider):
        collection_kind = KonanProxyTypeProvider._collection_kind(lldb_val, tip)
        if collection_kind is CollectionKind.LIST:
            collection_proxy = KonanListSyntheticProvider.fromObjectProxy(
                lldb_val, raw_provider, internal_dict
            )
        elif collection_kind is CollectionKind.SET:
            collection_proxy = KonanSetSyntheticProvider.fromObjectProxy(
                lldb_val, raw_provider, internal_dict
            )
        elif collection_kind is CollectionKind.MAP:
            collection_proxy = KonanMapSyntheticProvider.fromObjectProxy(
                lldb_val, raw_provider, internal_dict
            )
        else:
            collection_proxy = None
        if collection_proxy is not None:
            ret = collection_proxy

    logging.debug("%s = %s", value_str, ret)
    _bench(start, lambda: f"select_provider({value_str})")
    return ret


# noinspection PyUnresolvedReferences
class KonanHelperProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj, am_string, type_name, _):
        super().__init__(valobj)
        self._log = logging.getLogger(self.__class__.__name__)
        self._target = lldb.debugger.GetSelectedTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj
        self._type_name = type_name
        if am_string:
            return
        if self._children_count == 0:
            value_str = f"{_hex(self._valobj.unsigned)}"
            children_count = _children_count(self._valobj)
            self._log.debug(
                "(int)[%s].Konan_DebugGetFieldCount(%s) = %s",
                self._valobj.name,
                value_str,
                children_count,
            )
            self._children_count = children_count

    def update(self):
        if self._valobj is not None:
            self._children_count = _children_count(self._valobj)
        return False

    def _read_string(self, expr, error):
        return self._process.ReadCStringFromMemory(
            _evaluate(expr).unsigned, 0x1000, error
        )

    def _read_value(self, index):
        value_type = self._field_type(index)
        address = self._field_address(index)
        self._log.debug(
            "[%s, type:%s, address:%s]", index, value_type, _hex(address)
        )
        field_name = self._child_name(index)
        return _TYPE_CONVERSION[int(value_type)](
            self, self._valobj, address, str(field_name)
        )

    def _read_type(self, index):
        obj_type = _TYPES[self._field_type(index)](self._valobj)
        child = self._valobj.unsigned + self._children[index].offset()
        self._log.debug(
            "type:%s of %s of %s",
            obj_type,
            _hex(self._valobj.unsigned),
            _hex(child),
        )
        return obj_type

    def _field_address(self, index):
        return _evaluate(
            (
                f"(void *)Konan_DebugGetFieldAddress("
                f"{_hex(self._valobj.unsigned)}, {index}"
                f")"
            )
        ).unsigned

    def _field_type(self, index):
        return _evaluate(
            (
                f"(int)Konan_DebugGetFieldType("
                f"{_hex(self._valobj.unsigned)}, {index}"
                f")"
            )
        ).unsigned

    def _child_name(self, index):
        children = getattr(self, "_children", None)
        if children is not None and 0 <= index < len(children):
            return children[index]

        return self._field_name(index)

    def _render_string(self, representation):
        writer = io.StringIO()
        max_children_count = _max_children_count()
        limit = min(self._children_count, max_children_count)
        for i in range(limit):
            writer.write(representation(i))
            if i != limit - 1:
                writer.write(", ")
        if max_children_count < self._children_count:
            writer.write(", ...")
        return f"[{writer.getvalue()}]"


# noinspection PyUnresolvedReferences
class KonanStringSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        self._log = logging.getLogger(self.__class__.__name__)
        self._log.debug("%s name:%s", _hex(valobj.unsigned), valobj.name)
        self._children_count = 1
        super(KonanStringSyntheticProvider, self).__init__(
            valobj, True, "StringProvider", {}
        )
        self._string_size_in_bytes = 0
        self._size_in_bytes_child = None
        fallback = valobj.GetValue()
        buff_addr = _evaluate("(void *)Konan_DebugBuffer()").unsigned
        buff_len = _evaluate(
            (
                f"(int)Konan_DebugObjectToUtf8Array("
                f"{_hex(self._valobj.unsigned)}, "
                f"(void *){_hex(buff_addr)}, "
                f"(int)Konan_DebugBufferSize()"
                f");"
            )
        ).signed
        self._string_size_in_bytes = max(buff_len - 1, 0)

        if not buff_len:
            self._representation = fallback
            return

        error = lldb.SBError()
        s = self._process.ReadCStringFromMemory(
            int(buff_addr), int(buff_len), error
        )
        if not error.Success():
            raise DebuggerException()
        self._representation = s if error.Success() else fallback
        self._logger = lldb.formatters.Logger.Logger()

    def _create_size_in_bytes_child(self):
        data = lldb.SBData.CreateDataFromUInt32Array(
            self._target.GetByteOrder(),
            self._target.GetAddressByteSize(),
            [self._string_size_in_bytes],
        )
        return self._valobj.CreateValueFromData(
            "size_in_bytes",
            data,
            self._valobj.GetType().GetBasicType(lldb.eBasicTypeInt),
        )

    def update(self):
        return False

    def num_children(self):
        return 1

    def has_children(self):
        return True

    def get_child_index(self, name):
        return 0 if name == "size_in_bytes" else -1

    def get_child_at_index(self, index):
        if index != 0:
            return None
        if (
            self._size_in_bytes_child is None
            or not self._size_in_bytes_child.IsValid()
        ):
            self._size_in_bytes_child = self._create_size_in_bytes_child()
        return self._size_in_bytes_child


class KonanObjectSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, _, internal_dict):
        self._log = logging.getLogger(self.__class__.__name__)
        self._log.debug(_hex(valobj.unsigned))
        self._children_count = 0
        super(KonanObjectSyntheticProvider, self).__init__(
            valobj, False, "ObjectProvider", internal_dict
        )
        self._children = [
            self._field_name(i) for i in range(self._children_count)
        ]
        self._log.debug(
            "%s _children: %s", _hex(self._valobj.unsigned), self._children
        )

    def _field_name(self, index):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        error = lldb.SBError()
        name = self._read_string(
            (
                f"(char *)Konan_DebugGetFieldName("
                f"{_hex(self._valobj.unsigned)}, (int){index}"
                f")"
            ),
            error,
        )
        if not error.Success():
            raise DebuggerException()
        logging.debug(
            "KonanObjectSyntheticProvider (%s, %s) = %s",
            _hex(self._valobj.unsigned),
            index,
            name,
        )
        return name

    def num_children(self):
        self._log.debug(
            "%s = %s", _hex(self._valobj.unsigned), self._children_count
        )
        return self._children_count

    def has_children(self):
        self._log.debug(
            "%s = %s",
            _hex(self._valobj.unsigned),
            self._children_count > 0,
        )
        return self._children_count > 0

    def get_child_index(self, name):
        value_str = _hex(self._valobj.unsigned)
        self._log.debug("%s, %s", value_str, name)
        index = self._children.index(name)
        self._log.debug("%s index=%s", value_str, name)
        return index

    def get_child_at_index(self, index):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        return self._read_value(index)

    def update(self):
        super(KonanObjectSyntheticProvider, self).update()
        self._children = [
            self._field_name(i) for i in range(self._children_count)
        ]
        return False


class FastKonanObjectSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, _, internal_dict):
        self._log = logging.getLogger(self.__class__.__name__)
        self._log.debug(_hex(valobj.unsigned))
        self._children_count = 0
        self._children = None
        super(FastKonanObjectSyntheticProvider, self).__init__(
            valobj, False, "FastObjectProvider", internal_dict
        )

    def _field_name(self, index):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        error = lldb.SBError()
        name = self._read_string(
            (
                f"(char *)Konan_DebugGetFieldName("
                f"{_hex(self._valobj.unsigned)}, (int){index}"
                f")"
            ),
            error,
        )
        if not error.Success():
            raise DebuggerException()
        logging.debug(
            "FastKonanObjectSyntheticProvider (%s, %s) = %s",
            _hex(self._valobj.unsigned),
            index,
            name,
        )
        return name

    def _ensure_children(self, reason):
        if self._children is None:
            self._children = [
                self._field_name(i) for i in range(self._children_count)
            ]
            self._log.debug(
                "%s _children: %s",
                _hex(self._valobj.unsigned),
                self._children,
            )
        return self._children

    def num_children(self):
        self._log.debug(
            "%s = %s", _hex(self._valobj.unsigned), self._children_count
        )
        return self._children_count

    def has_children(self):
        self._log.debug(
            "%s = %s",
            _hex(self._valobj.unsigned),
            self._children_count > 0,
        )
        return self._children_count > 0

    def get_child_index(self, name):
        value_str = _hex(self._valobj.unsigned)
        self._log.debug("%s, %s", value_str, name)
        index = self._ensure_children(
            f"resolve requested field '{name}'"
        ).index(name)
        self._log.debug("%s index=%s", value_str, name)
        return index

    def get_child_at_index(self, index):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        return self._read_value(index)

    def update(self):
        super(FastKonanObjectSyntheticProvider, self).update()
        self._children = None
        return False


class KonanArraySyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, internal_dict):
        self._log = logging.getLogger(self.__class__.__name__)
        self._children_count = 0
        super(KonanArraySyntheticProvider, self).__init__(
            valobj, False, "ArrayProvider", internal_dict
        )
        self._log.debug("valobj: %s", _hex(valobj.unsigned))
        if self._valobj is None:
            return
        valobj.SetSyntheticChildrenGenerated(True)

    def num_children(self):
        self._log.debug(
            "(%s) = %s", _hex(self._valobj.unsigned), self._children_count
        )
        return self._children_count

    def has_children(self):
        self._log.debug(
            "(%s) = %s",
            _hex(self._valobj.unsigned),
            self._children_count > 0,
        )
        return self._children_count > 0

    def get_child_index(self, name):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), name)
        index = int(name)
        return index if (0 <= index < self._children_count) else -1

    def get_child_at_index(self, index):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        return self._read_value(index)

    def _field_name(self, index):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        return str(index)

    def update(self):
        super(KonanArraySyntheticProvider, self).update()
        return False


class FastKonanArraySyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, tip, internal_dict):
        self._log = logging.getLogger(self.__class__.__name__)
        self._children_count = 0
        self._tip = tip
        self._element_runtime_type = _RUNTIME_TYPE_INVALID
        self._element_lldb_type = None
        super(FastKonanArraySyntheticProvider, self).__init__(
            valobj, False, "FastArrayProvider", internal_dict
        )
        self._log.debug("valobj: %s", _hex(valobj.unsigned))
        if self._valobj is None:
            return
        self._element_runtime_type = self._resolve_element_runtime_type()
        self._element_lldb_type = self._resolve_element_lldb_type()
        valobj.SetSyntheticChildrenGenerated(True)

    def num_children(self):
        self._log.debug(
            "(%s) = %s", _hex(self._valobj.unsigned), self._children_count
        )
        return self._children_count

    def has_children(self):
        self._log.debug(
            "(%s) = %s",
            _hex(self._valobj.unsigned),
            self._children_count > 0,
        )
        return self._children_count > 0

    def get_child_index(self, name):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), name)
        index = int(name)
        return index if (0 <= index < self._children_count) else -1

    def get_child_at_index(self, index):
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        if not (0 <= index < self._children_count):
            return None

        cached_addresses = _get_cached_child_addresses_by_index(self._valobj)
        if cached_addresses is None:
            return None
        if index not in cached_addresses:
            self._prefetch_child_data(index, cached_addresses)

        address = cached_addresses.get(index)
        if not address:
            return None

        child = self._create_child_value(index, address)
        return child if child.IsValid() else None

    def _field_address(self, index):
        cached_addresses = _get_cached_child_addresses_by_index(self._valobj)
        if cached_addresses is None:
            return super()._field_address(index)
        if index not in cached_addresses:
            self._prefetch_child_data(index, cached_addresses)
        return cached_addresses.get(index, 0)

    def _field_type(self, index):
        cached_types = _get_cached_child_types_by_index(self._valobj)
        if cached_types is not None and index in cached_types:
            return cached_types[index]

        cached_addresses = _get_cached_child_addresses_by_index(self._valobj)
        if cached_addresses is not None:
            self._prefetch_child_data(index, cached_addresses, cached_types)
            if cached_types is not None and index in cached_types:
                return cached_types[index]

        child_type = self._element_runtime_type
        if child_type == _RUNTIME_TYPE_INVALID:
            child_type = super()._field_type(index)
        _cache_child_type(self._valobj, index, child_type)
        return child_type

    def update(self):
        super(FastKonanArraySyntheticProvider, self).update()
        self._element_runtime_type = self._resolve_element_runtime_type()
        self._element_lldb_type = self._resolve_element_lldb_type()
        return False

    def _prefetch_child_data(
        self, index, cached_addresses, cached_types=None
    ):
        if cached_types is None:
            cached_types = _get_cached_child_types_by_index(self._valobj)

        start = index
        end = min(
            self._children_count - 1,
            index + _FAST_ARRAY_PREFETCH_RADIUS,
        )
        missing_address_indices = [
            child_index
            for child_index in range(start, end + 1)
            if child_index not in cached_addresses
        ]
        missing_type_indices = []
        if cached_types is not None:
            missing_type_indices = [
                child_index
                for child_index in range(start, end + 1)
                if child_index not in cached_types
            ]
        if not missing_address_indices and not missing_type_indices:
            return

        buffer_addr = _evaluate("(void *)Konan_DebugBuffer()").unsigned
        buffer_size = _evaluate("(int)Konan_DebugBufferSize()").signed
        max_batch_count = self._max_batch_count(buffer_addr, buffer_size)
        if max_batch_count <= 0:
            raise DebuggerException(
                "Konan_DebugBuffer is too small for FastKonanArraySyntheticProvider"
            )

        prefix = self._struct_prefix()
        for batch_offset in range(
            0, len(missing_address_indices), max_batch_count
        ):
            indices = missing_address_indices[
                batch_offset : batch_offset + max_batch_count
            ]
            count = len(indices)
            address_layout = self._address_batch_layout(
                buffer_addr, buffer_size, count
            )
            summary_layout = self._summary_batch_layout(
                buffer_addr, buffer_size, count
            )

            addresses = self._run_batch_address_request(
                indices, count, prefix, address_layout
            )
            field_types = self._prefetch_field_types(
                indices, count, prefix, address_layout
            )
            type_name_offsets, type_name_lengths, type_name_data = (
                self._run_batch_type_name_request(
                    indices, count, prefix, summary_layout
                )
            )

            summary_offsets, summary_lengths, summary_data = (
                self._run_batch_summary_request(
                    indices, count, prefix, summary_layout
                )
            )
            for (
                child_index,
                child_address,
                child_type,
                type_name_offset,
                type_name_length,
                summary_offset,
                summary_length,
            ) in zip(
                indices,
                addresses,
                field_types,
                type_name_offsets,
                type_name_lengths,
                summary_offsets,
                summary_lengths,
            ):
                cached_addresses[child_index] = child_address
                if cached_types is not None:
                    cached_types[child_index] = child_type
                if child_type == _RUNTIME_TYPE_OBJECT and child_address:
                    child_key = self._read_pointer(child_address)
                    if (
                        type_name_data is not None
                        and type_name_offset >= 0
                        and type_name_length > 0
                    ):
                        type_name = type_name_data[
                            type_name_offset : type_name_offset
                            + type_name_length
                            - 1
                        ].decode("utf-8", errors="replace")
                        _set_cached_field_type_result_for_key(
                            self._process, child_key, type_name
                        )
                    if (
                        summary_data is not None
                        and summary_offset >= 0
                        and summary_length > 0
                    ):
                        summary = summary_data[
                            summary_offset : summary_offset + summary_length - 1
                        ].decode("utf-8", errors="replace")
                        _set_cached_summary_for_key(
                            self._process, child_key, summary
                        )
                elif child_address:
                    type_name = _type_name_for_runtime_type(child_type)
                    if type_name is not None:
                        _set_cached_field_type_result_for_key(
                            self._process, child_address, type_name
                        )

        if not missing_type_indices:
            return

        remaining_type_indices = [
            child_index
            for child_index in missing_type_indices
            if child_index not in missing_address_indices
        ]
        if not remaining_type_indices or cached_types is None:
            return

        if self._element_runtime_type != _RUNTIME_TYPE_INVALID:
            for child_index in remaining_type_indices:
                cached_types[child_index] = self._element_runtime_type
            return

        for batch_offset in range(
            0, len(remaining_type_indices), max_batch_count
        ):
            indices = remaining_type_indices[
                batch_offset : batch_offset + max_batch_count
            ]
            count = len(indices)
            layout = self._address_batch_layout(buffer_addr, buffer_size, count)
            field_types = self._run_batch_field_type_request(
                indices, count, prefix, layout
            )
            for child_index, child_type in zip(indices, field_types):
                cached_types[child_index] = child_type

    def _max_batch_count(self, buffer_addr, buffer_size):
        low = 0
        high = self._children_count
        while low < high:
            mid = (low + high + 1) // 2
            if self._batch_layout_fits(buffer_addr, buffer_size, mid):
                low = mid
            else:
                high = mid - 1
        return low

    def _address_batch_layout(self, buffer_addr, buffer_size, count):
        pointer_size = self._target.GetAddressByteSize()
        indices_addr = self._align_up(buffer_addr, 4)
        result_addr = self._align_up(
            indices_addr + count * 4, pointer_size
        )
        required_size = (result_addr - buffer_addr) + count * pointer_size

        return {
            "indices_addr": indices_addr,
            "result_addr": result_addr,
            "fits": required_size <= buffer_size,
        }

    def _summary_batch_layout(self, buffer_addr, buffer_size, count):
        indices_addr = self._align_up(buffer_addr, 4)
        summary_offsets_addr = self._align_up(indices_addr + count * 4, 4)
        summary_lengths_addr = self._align_up(
            summary_offsets_addr + count * 4, 4
        )
        summary_buffer_addr = summary_lengths_addr + count * 4
        required_size = summary_buffer_addr - buffer_addr
        fits = required_size < buffer_size
        summary_buffer_size = buffer_size - required_size if fits else 0

        return {
            "indices_addr": indices_addr,
            "summary_offsets_addr": summary_offsets_addr,
            "summary_lengths_addr": summary_lengths_addr,
            "summary_buffer_addr": summary_buffer_addr,
            "summary_buffer_size": summary_buffer_size,
            "fits": fits,
        }

    def _batch_layout_fits(self, buffer_addr, buffer_size, count):
        if not self._address_batch_layout(
            buffer_addr, buffer_size, count
        )["fits"]:
            return False
        if self._element_runtime_type != _RUNTIME_TYPE_OBJECT:
            return True
        return self._summary_batch_layout(
            buffer_addr, buffer_size, count
        )["fits"]

    def _write_batch_indices(self, indices, count, prefix, indices_addr):
        indices_bytes = struct.pack(f"{prefix}{count}i", *indices)
        error = lldb.SBError()
        bytes_written = self._process.WriteMemory(
            indices_addr, indices_bytes, error
        )
        if not error.Success() or bytes_written != len(indices_bytes):
            raise DebuggerException(
                "Failed to write FastKonanArraySyntheticProvider indices"
            )

    def _run_batch_address_request(self, indices, count, prefix, layout):
        self._write_batch_indices(indices, count, prefix, layout["indices_addr"])
        _evaluate(
            (
                f"((void)Konan_DebugBatchGetFieldAddress("
                f"{_hex(self._valobj.unsigned)}, "
                f"(int32_t *){_hex(layout['indices_addr'])}, "
                f"{count}, "
                f"(void **){_hex(layout['result_addr'])}"
                f"), (void *){_hex(layout['result_addr'])})"
            )
        )
        error = lldb.SBError()
        pointer_size = self._target.GetAddressByteSize()
        raw_addresses = self._process.ReadMemory(
            layout["result_addr"], count * pointer_size, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider addresses"
            )
        pointer_format = "Q" if pointer_size == 8 else "I"
        return struct.unpack(f"{prefix}{count}{pointer_format}", raw_addresses)

    def _run_batch_field_type_request(self, indices, count, prefix, layout):
        self._write_batch_indices(indices, count, prefix, layout["indices_addr"])
        _evaluate(
            (
                f"((void)Konan_DebugBatchGetFieldType("
                f"{_hex(self._valobj.unsigned)}, "
                f"(int32_t *){_hex(layout['indices_addr'])}, "
                f"{count}, "
                f"(int32_t *){_hex(layout['result_addr'])}"
                f"), (void *){_hex(layout['result_addr'])})"
            )
        )
        error = lldb.SBError()
        raw_types = self._process.ReadMemory(
            layout["result_addr"], count * 4, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider field types"
            )
        return struct.unpack(f"{prefix}{count}i", raw_types)

    def _run_batch_summary_request(self, indices, count, prefix, layout):
        if self._element_runtime_type != _RUNTIME_TYPE_OBJECT:
            return ([-1] * count, [0] * count, None)

        self._write_batch_indices(indices, count, prefix, layout["indices_addr"])
        _evaluate(
            (
                f"((void)Konan_DebugBatchObjectToUtf8Array("
                f"{_hex(self._valobj.unsigned)}, "
                f"(int32_t *){_hex(layout['indices_addr'])}, "
                f"{count}, "
                f"(int32_t *){_hex(layout['summary_offsets_addr'])}, "
                f"(int32_t *){_hex(layout['summary_lengths_addr'])}, "
                f"(char *){_hex(layout['summary_buffer_addr'])}, "
                f"{layout['summary_buffer_size']}"
                f"), (void *){_hex(layout['summary_buffer_addr'])})"
            )
        )

        error = lldb.SBError()
        raw_summary_offsets = self._process.ReadMemory(
            layout["summary_offsets_addr"], count * 4, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider summary offsets"
            )
        raw_summary_lengths = self._process.ReadMemory(
            layout["summary_lengths_addr"], count * 4, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider summary lengths"
            )
        summary_offsets = struct.unpack(
            f"{prefix}{count}i", raw_summary_offsets
        )
        summary_lengths = struct.unpack(
            f"{prefix}{count}i", raw_summary_lengths
        )
        max_summary_end = max(
            (
                offset + length
                for offset, length in zip(summary_offsets, summary_lengths)
                if offset >= 0 and length > 0
            ),
            default=0,
        )
        if max_summary_end <= 0:
            return (summary_offsets, summary_lengths, None)

        summary_data = self._process.ReadMemory(
            layout["summary_buffer_addr"], max_summary_end, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider summaries"
            )
        return (summary_offsets, summary_lengths, summary_data)

    def _run_batch_type_name_request(self, indices, count, prefix, layout):
        if self._element_runtime_type != _RUNTIME_TYPE_OBJECT:
            return ([-1] * count, [0] * count, None)

        self._write_batch_indices(indices, count, prefix, layout["indices_addr"])
        _evaluate(
            (
                f"((void)Konan_DebugBatchGetTypeName("
                f"{_hex(self._valobj.unsigned)}, "
                f"(int32_t *){_hex(layout['indices_addr'])}, "
                f"{count}, "
                f"(int32_t *){_hex(layout['summary_offsets_addr'])}, "
                f"(int32_t *){_hex(layout['summary_lengths_addr'])}, "
                f"(char *){_hex(layout['summary_buffer_addr'])}, "
                f"{layout['summary_buffer_size']}"
                f"), (void *){_hex(layout['summary_buffer_addr'])})"
            )
        )

        error = lldb.SBError()
        raw_offsets = self._process.ReadMemory(
            layout["summary_offsets_addr"], count * 4, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider type name offsets"
            )
        raw_lengths = self._process.ReadMemory(
            layout["summary_lengths_addr"], count * 4, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider type name lengths"
            )
        offsets = struct.unpack(f"{prefix}{count}i", raw_offsets)
        lengths = struct.unpack(f"{prefix}{count}i", raw_lengths)
        max_end = max(
            (
                offset + length
                for offset, length in zip(offsets, lengths)
                if offset >= 0 and length > 0
            ),
            default=0,
        )
        if max_end <= 0:
            return (offsets, lengths, None)

        type_name_data = self._process.ReadMemory(
            layout["summary_buffer_addr"], max_end, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider type names"
            )
        return (offsets, lengths, type_name_data)

    def _prefetch_field_types(self, indices, count, prefix, layout):
        if self._element_runtime_type != _RUNTIME_TYPE_INVALID:
            return [self._element_runtime_type] * count
        return self._run_batch_field_type_request(
            indices, count, prefix, layout
        )

    def _struct_prefix(self):
        byte_order = self._target.GetByteOrder()
        if byte_order == lldb.eByteOrderBig:
            return ">"
        return "<"

    def _create_child_value(self, index, address):
        if self._element_lldb_type is None:
            return None

        return self._valobj.CreateValueFromAddress(
            str(index),
            address,
            self._element_lldb_type,
        )

    def _resolve_element_runtime_type(self):
        if self._tip is None:
            return self._fallback_element_runtime_type()

        pointer_size = self._target.GetAddressByteSize()
        extended_info_addr = self._read_pointer(self._tip + pointer_size)
        if not extended_info_addr:
            return self._fallback_element_runtime_type()

        fields_count = self._read_int32(extended_info_addr)
        if fields_count is None or fields_count >= 0:
            return self._fallback_element_runtime_type()

        runtime_type = -fields_count
        self._log.debug(
            "array runtime type for %s resolved from type info: %s",
            _hex(self._valobj.unsigned),
            runtime_type,
        )
        return runtime_type

    def _fallback_element_runtime_type(self):
        if self._children_count <= 0:
            return _RUNTIME_TYPE_INVALID

        runtime_type = self._field_type(0)
        self._log.debug(
            "array runtime type for %s resolved via fallback: %s",
            _hex(self._valobj.unsigned),
            runtime_type,
        )
        return runtime_type

    def _resolve_element_lldb_type(self):
        runtime_type = self._element_runtime_type
        if runtime_type == _RUNTIME_TYPE_OBJECT:
            return self._valobj.type
        if runtime_type == 2:
            return self._valobj.type.GetBasicType(lldb.eBasicTypeSignedChar)
        if runtime_type == 3:
            return self._valobj.type.GetBasicType(lldb.eBasicTypeShort)
        if runtime_type == 4:
            return self._valobj.type.GetBasicType(lldb.eBasicTypeInt)
        if runtime_type == 5:
            return self._valobj.type.GetBasicType(lldb.eBasicTypeLongLong)
        if runtime_type == 6:
            return self._valobj.type.GetBasicType(lldb.eBasicTypeFloat)
        if runtime_type == 7:
            return self._valobj.type.GetBasicType(lldb.eBasicTypeDouble)
        if runtime_type == 8:
            return self._valobj.type.GetBasicType(
                lldb.eBasicTypeVoid
            ).GetPointerType()
        if runtime_type == 9:
            return self._valobj.type.GetBasicType(lldb.eBasicTypeBool)
        return None

    def _read_pointer(self, address):
        pointer_size = self._target.GetAddressByteSize()
        error = lldb.SBError()
        raw = self._process.ReadMemory(address, pointer_size, error)
        if not error.Success():
            return 0
        pointer_format = "Q" if pointer_size == 8 else "I"
        return struct.unpack(
            f"{self._struct_prefix()}{pointer_format}", raw
        )[0]

    def _read_int32(self, address):
        error = lldb.SBError()
        raw = self._process.ReadMemory(address, 4, error)
        if not error.Success():
            return None
        return struct.unpack(f"{self._struct_prefix()}i", raw)[0]

    @staticmethod
    def _align_up(value, alignment):
        return (value + alignment - 1) & ~(alignment - 1)


def _object_field(object_proxy, field_name):
    try:
        field_index = object_proxy.get_child_index(field_name)
    except (DebuggerException, ValueError):
        return None
    return object_proxy.get_child_at_index(field_index)


def _object_field_unsigned(object_proxy, field_name):
    value = _object_field(object_proxy, field_name)
    if value is None or not value.IsValid():
        return None
    return value.GetValueAsUnsigned()


class KonanDelegatingSyntheticProvider:
    def __init__(self, valobj, proxy, children_count=None):
        self._valobj = valobj
        self._proxy = proxy
        self._children_count = children_count

    def num_children(self):
        proxy_children_count = self._proxy.num_children()
        children_count = proxy_children_count
        if self._children_count is not None:
            children_count = min(proxy_children_count, self._children_count)
        return children_count

    def has_children(self):
        return self.num_children() > 0

    def get_child_index(self, name):
        child_index = self._proxy.get_child_index(name)
        return child_index if 0 <= child_index < self.num_children() else -1

    def get_child_at_index(self, index):
        if not 0 <= index < self.num_children():
            return None
        return self._proxy.get_child_at_index(index)

    def update(self):
        return self._proxy.update()

    def __getattr__(self, item):
        return getattr(self._proxy, item)


class KonanListSyntheticProvider(KonanDelegatingSyntheticProvider):
    def __init__(self, valobj, backing_proxy, children_count):
        super().__init__(valobj, backing_proxy, children_count)
        self._log = logging.getLogger(self.__class__.__name__)

    @staticmethod
    def fromObjectProxy(valobj, object_proxy, internal_dict):
        if object_proxy is None:
            return None

        visible_children_count = _object_field_unsigned(object_proxy, "length")
        for field_name in ("backing", "$this_asList", "backingArray"):
            backing = _object_field(object_proxy, field_name)
            if backing is not None and backing.IsValid() and backing.unsigned != 0:
                return KonanListSyntheticProvider(
                    valobj,
                    KonanProxyTypeProvider(backing, internal_dict),
                    visible_children_count,
                )

        logging.getLogger(KonanListSyntheticProvider.__name__).debug(
            "%s has no supported List backing field", _hex(valobj.unsigned)
        )
        return None


class KonanSetSyntheticProvider(KonanDelegatingSyntheticProvider):
    def __init__(self, valobj, keys_proxy, children_count):
        super().__init__(valobj, keys_proxy, children_count)
        self._log = logging.getLogger(self.__class__.__name__)

    @staticmethod
    def fromObjectProxy(valobj, object_proxy, internal_dict):
        if object_proxy is None:
            return None

        backing = _object_field(object_proxy, "backing")
        if backing is None or not backing.IsValid() or backing.unsigned == 0:
            logging.getLogger(KonanSetSyntheticProvider.__name__).debug(
                "%s has no supported Set backing field", _hex(valobj.unsigned)
            )
            return None

        backing_type_info = _type_info(backing)
        if not backing_type_info:
            return None
        backing_object_proxy = _select_provider(
            backing, backing_type_info, internal_dict
        )
        keys = _object_field(backing_object_proxy, "keysArray")
        if keys is None or not keys.IsValid() or keys.unsigned == 0:
            return None

        visible_children_count = _object_field_unsigned(
            backing_object_proxy, "length"
        )
        return KonanSetSyntheticProvider(
            valobj,
            KonanProxyTypeProvider(keys, internal_dict),
            visible_children_count,
        )


class KonanMapSyntheticProvider:
    def __init__(self, valobj, keys, values, children_count, internal_dict):
        self._log = logging.getLogger(self.__class__.__name__)
        self._valobj = valobj
        self._target = lldb.debugger.GetSelectedTarget()
        self._internal_dict = internal_dict
        self._keys = KonanProxyTypeProvider(keys, internal_dict)
        self._values = KonanProxyTypeProvider(values, internal_dict)
        self._children_count = children_count
        self._entry_type = None

    @staticmethod
    def fromObjectProxy(valobj, object_proxy, internal_dict):
        if object_proxy is None:
            return None

        keys = _object_field(object_proxy, "keysArray")
        values = _object_field(object_proxy, "valuesArray")
        if (
            keys is None
            or not keys.IsValid()
            or keys.unsigned == 0
            or values is None
            or not values.IsValid()
            or values.unsigned == 0
        ):
            return None

        visible_children_count = None
        for field_name in ("length",):
            value = _object_field(object_proxy, field_name)
            if value is not None and value.IsValid():
                visible_children_count = value.GetValueAsUnsigned()
                break
        return KonanMapSyntheticProvider(
            valobj, keys, values, visible_children_count, internal_dict
        )

    def num_children(self):
        keys_count = self._keys.num_children()
        values_count = self._values.num_children()
        count = min(keys_count, values_count)
        if self._children_count is not None:
            count = min(count, self._children_count)
        return count

    def has_children(self):
        return self.num_children() > 0

    def get_child_index(self, name):
        try:
            index = int(name.strip("[]"))
        except ValueError:
            return -1
        return index if 0 <= index < self.num_children() else -1

    def get_child_at_index(self, index):
        if not 0 <= index < self.num_children():
            return None

        key = self._keys.get_child_at_index(index)
        value = self._values.get_child_at_index(index)
        if key is None or value is None or not key.IsValid() or not value.IsValid():
            return None

        if self._entry_type is None:
            self._entry_type = key.GetType().GetArrayType(2)
        if not self._entry_type.IsValid():
            return None

        pointer_size = self._target.GetAddressByteSize()
        addresses = [key.GetValueAsUnsigned(), value.GetValueAsUnsigned()]
        if pointer_size == 8:
            data = lldb.SBData.CreateDataFromUInt64Array(
                self._target.GetByteOrder(), pointer_size, addresses
            )
        elif pointer_size == 4:
            data = lldb.SBData.CreateDataFromUInt32Array(
                self._target.GetByteOrder(), pointer_size, addresses
            )
        else:
            return None

        entry = self._valobj.CreateValueFromData(
            f"[{index}]", data, self._entry_type
        )
        return entry if entry.IsValid() else None

    def update(self):
        return False


class KonanZerroSyntheticProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj):
        super().__init__(valobj)
        self._log = logging.getLogger(self.__class__.__name__)
        logging.debug(valobj.name)

    def num_children(self):
        self._log.debug("")
        return 0

    def update(self):
        return False

    def has_children(self):
        self._log.debug("")
        return False

    def get_child_index(self, name):
        self._log.debug("")
        return 0

    def get_child_at_index(self, index):
        self._log.debug("")
        return None

    def __getattr__(self, item):
        pass


class KonanNullSyntheticProvider(KonanZerroSyntheticProvider):
    def __init__(self, valobj):
        super(KonanNullSyntheticProvider, self).__init__(valobj)


class KonanNotInitializedObjectSyntheticProvider(KonanZerroSyntheticProvider):
    def __init__(self, valobj):
        super(KonanNotInitializedObjectSyntheticProvider, self).__init__(
            valobj
        )


class KonanProxyTypeProvider:
    def __init__(self, valobj, internal_dict):
        self._log = logging.getLogger(self.__class__.__name__)
        self._valobj = valobj
        self._internal_dict = internal_dict
        self._log.debug("%s, name: %s", _hex(valobj.unsigned), valobj.name)

    def _type_info(self):
        return _object_type_info_from_memory(self._valobj) or _type_info(
            self._valobj
        )

    def _is_kotlin_string(self):
        string_addr = _symbol_loaded_address("kclass:kotlin.String")
        if string_addr == 0 or self._valobj.unsigned == 0:
            return False
        return _evaluate(
            f"(int)Konan_DebugIsInstance({_hex(self._valobj.unsigned)}, {_hex(string_addr)})"
        ).unsigned != 0

    def _get_proxy(self):
        cached_proxy = _get_cached_resolved_proxy(self._valobj)
        if cached_proxy is not None:
            return cached_proxy

        start = time.monotonic()
        valobj = self._valobj
        value_str = _hex(valobj.unsigned)
        value_name = valobj.name

        # TODO: Check that we dont need to return the null provider if the value changed to 0 in the meantime
        if valobj.unsigned == 0:
            self._log.debug(
                "%s, name: %s NULL syntectic %s",
                value_str,
                value_name,
                valobj.IsValid(),
            )
            proxy = KonanNullSyntheticProvider(valobj)
        else:
            tip = self._type_info()
            if not tip:
                self._log.debug(
                    "%s, name: %s NULL syntectic %s",
                    value_str,
                    value_name,
                    valobj.IsValid(),
                )
                proxy = KonanNotInitializedObjectSyntheticProvider(
                    valobj
                )
            else:
                self._log.debug("%s tip: %s", value_str, _hex(tip))
                proxy = _select_provider(
                    valobj, tip, self._internal_dict
                )

        _bench(start, lambda: f"KonanProxyTypeProvider({value_str})")
        self._log.debug(
            "%s _proxy: %s", value_str, proxy.__class__.__name__
        )
        _set_cached_resolved_proxy(self._valobj, proxy)
        return proxy

    @staticmethod
    def _collection_kind(valobj, type_info):
        if _is_kotlin_list(valobj):
            return CollectionKind.LIST
        elif _is_kotlin_map(valobj):
            return CollectionKind.MAP
        elif _is_kotlin_set(valobj):
            return CollectionKind.SET
        return None

    def get_value(self):
        return self._valobj.GetValue()

    def num_children(self):
        return self._get_proxy().num_children()

    def update(self):
        return False

    def has_children(self):
        return True

    def get_child_index(self, name):
        proxy = self._get_proxy()
        child_index = proxy.get_child_index(name)
        return child_index if 0 <= child_index < proxy.num_children() else -1

    def get_child_at_index(self, index):
        proxy = self._get_proxy()
        if not 0 <= index < proxy.num_children():
            return None
        return proxy.get_child_at_index(index)

    def __getattr__(self, item):
        return getattr(self._get_proxy(), item)


def _get_runtime_type(variable):
    type_name = _evaluate(
        f"(char *)Konan_DebugGetTypeName({_hex(variable.unsigned)})"
    ).summary
    return "" if type_name is None else type_name.strip('"')


def _cached_field_type_result_key(variable):
    if variable is None or not variable.IsValid():
        return None
    if variable.GetTypeName() == "ObjHeader *":
        key = variable.GetValueAsUnsigned()
        return key if key != 0 else None
    try:
        address_of = variable.AddressOf()
    except Exception:
        address_of = None
    if address_of is None or not address_of.IsValid():
        return None
    key = address_of.GetValueAsUnsigned()
    return key if key != 0 else None


def _get_cached_field_type_result(variable):
    key = _cached_field_type_result_key(variable)
    if key is None:
        return None
    return _get_cached_field_type_result_for_key(variable.GetProcess(), key)


def field_type_command(_, field_address, exe_ctx, result, internal_dict):
    fields = field_address.split(".")

    variable = exe_ctx.GetFrame().FindVariable(fields[0])

    for field_name in fields[1:]:
        if variable is not None:
            provider = KonanProxyTypeProvider(variable, internal_dict)
            field_index = provider.get_child_index(field_name)
            variable = provider.get_child_at_index(field_index)
        else:
            break

    desc = "<NO_FIELD_FOUND>"

    if variable is not None:
        cached_type_name = _get_cached_field_type_result(variable)
        if cached_type_name is not None:
            desc = cached_type_name
        else:
            rt = _get_runtime_type(variable)
            if len(rt) > 0:
                desc = rt

    result.write(f"{desc}")


_KONAN_VARIABLE = re.compile("kvar:(.*)#internal")
_KONAN_VARIABLE_TYPE = re.compile("^kfun:<get-(.*)>\\(\\)(.*)$")
_TYPES_KONAN_TO_C = {
    "kotlin.Byte": ("int8_t", lambda v: v.signed),
    "kotlin.Short": ("short", lambda v: v.signed),
    "kotlin.Int": ("int", lambda v: v.signed),
    "kotlin.Long": ("long", lambda v: v.signed),
    "kotlin.UByte": ("int8_t", lambda v: v.unsigned),
    "kotlin.UShort": ("short", lambda v: v.unsigned),
    "kotlin.UInt": ("int", lambda v: v.unsigned),
    "kotlin.ULong": ("long", lambda v: v.unsigned),
    "kotlin.Char": ("short", lambda v: v.signed),
    "kotlin.Boolean": ("bool", lambda v: v.signed),
    "kotlin.Float": ("float", lambda v: v.value),
    "kotlin.Double": ("double", lambda v: v.value),
}


def type_by_address_command(debugger, command, result, _):
    tokens = command.split()
    target = debugger.GetSelectedTarget()
    types = _type_info_by_address(tokens[0])
    for t in types:
        result.AppendMessage(
            f"{t.name}: {_hex(t.GetStartAddress().GetLoadAddress(target))}"
        )


def symbol_by_name_command(debugger, command, result):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()
    tokens = command.split()
    mask = re.compile(tokens[0])
    symbols = list(
        filter(lambda v: mask.match(v.name), frame.GetModule().symbols)
    )
    visited = list()
    for symbol in symbols:
        name = symbol.name
        if name in visited:
            continue
        visited.append(name)
        result.AppendMessage(
            f"{name}: {symbol.GetStartAddress().GetLoadAddress(target)}"
        )


def konan_globals_command(debugger, _, result, __):
    target = debugger.GetSelectedTarget()
    process = target.GetProcess()
    thread = process.GetSelectedThread()
    frame = thread.GetSelectedFrame()

    konan_variable_symbols = list(
        filter(
            lambda v: _KONAN_VARIABLE.match(v.name),
            frame.GetModule().symbols,
        )
    )
    visited = list()
    for symbol in konan_variable_symbols:
        name = _KONAN_VARIABLE.search(symbol.name).group(1)

        if name in visited:
            continue
        visited.append(name)

        def match(v):
            return re.match(f"^kfun:<get-{name}>\\(\\).*$", v.name)

        getters = list(filter(match, frame.module.symbols))
        if not getters:
            result.AppendMessage(f"storage not found for name:{name}")
            continue

        getter_functions = frame.module.FindFunctions(getters[0].name)
        if not getter_functions:
            continue

        address = (
            getter_functions[0]
            .function.GetStartAddress()
            .GetLoadAddress(target)
        )
        t = _KONAN_VARIABLE_TYPE.search(getters[0].name).group(2)
        (c_type, extractor) = (
            _TYPES_KONAN_TO_C[t]
            if t in _TYPES_KONAN_TO_C.keys()
            else ("ObjHeader *", lambda v: kotlin_object_type_summary(v, {}))
        )
        value = _evaluate(f"(({c_type} (*)()){_hex(address)})()")
        str_value = extractor(value)
        result.AppendMessage(f"{t} {name}: {str_value}")


def _hex(value):
    return f"0x{value:x}"


_LOGGING = False


def _init_logger():
    formatter = logging.Formatter(
        "%(levelname)s - %(name)s - %(funcName)s: %(message)s"
    )

    # Same as in LLDBFrontend
    if os.getenv("GLOG_log_dir") is not None:
        handler = logging.FileHandler(
            filename=os.getenv("GLOG_log_dir", "") + "/konan_lldb.log"
        )
        handler.setFormatter(formatter)
        logging.getLogger().addHandler(handler)
        logging.getLogger().setLevel(logging.DEBUG)

    if _LOGGING:
        handler = logging.StreamHandler(stream=sys.stderr)
        handler.setFormatter(formatter)
        logging.getLogger().addHandler(handler)
        logging.getLogger().setLevel(logging.DEBUG)


def __lldb_init_module(debugger, _):
    _init_logger()
    logging.debug("init start")
    _FACTORY["object"] = lambda x, y, z: FastKonanObjectSyntheticProvider(
        x, y, z
    )
    _FACTORY["array"] = lambda x, y, z: FastKonanArraySyntheticProvider(x, y, z)
    _FACTORY["string"] = lambda x, y, _: KonanStringSyntheticProvider(x)
    debugger.HandleCommand(
        (
            "type summary add "
            "--no-value "
            "--python-function konan_lldb.kotlin_object_type_summary "
            '"ObjHeader *" '
            "--category Kotlin"
        )
    )
    debugger.HandleCommand(
        (
            "type summary add "
            "--no-value "
            "--python-function konan_lldb.kotlin_object_pair_type_summary "
            '"ObjHeader *[2]" '
            "--category Kotlin"
        )
    )
    debugger.HandleCommand(
        (
            "type synthetic add "
            "--python-class konan_lldb.KonanProxyTypeProvider "
            '"ObjHeader *" '
            "--category Kotlin"
        )
    )
    debugger.HandleCommand("type category enable Kotlin")
    debugger.HandleCommand(
        f"command script add -f {__name__}.field_type_command field_type"
    )
    debugger.HandleCommand(
        (
            f"command script add -f "
            f"{__name__}.type_by_address_command type_by_address"
        )
    )
    debugger.HandleCommand(
        (
            f"command script add -f "
            f"{__name__}.symbol_by_name_command symbol_by_name"
        )
    )
    # Avoid Kotlin/Native runtime
    debugger.HandleCommand(
        "settings set target.process.thread.step-avoid-regexp ^::Kotlin_"
    )
    logging.debug("init end")
