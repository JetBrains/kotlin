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
import re
import struct
import time
import logging
from enum import Enum

import lldb

_NULL = "null"
_BENCH_LOGGING = False
_FAST_ARRAY_PREFETCH_RADIUS = 200
_INFERIOR_SCRATCH_BUFFER_SIZE = 10 * 1024 * 1024
_FIELD_NAME_BATCH_BUFFER_SIZE_PER_ITEM = 128
_TYPE_NAME_BATCH_BUFFER_SIZE_PER_ITEM = 128
_SUMMARY_BATCH_BUFFER_SIZE_PER_ITEM = 512
_MIN_BATCH_STRING_BUFFER_SIZE = 4096
_RUNTIME_TYPE_INVALID = 0
_RUNTIME_TYPE_OBJECT = 1
_RUNTIME_TYPE_VECTOR128 = 10
# Inferior scratch buffer is intentionally process-global and never deallocated.
_INFERIOR_SCRATCH_BUFFER_PROCESS_KEY = None
_INFERIOR_SCRATCH_BUFFER_ADDRESS = 0


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
    return


def _log_inspection_timing(name, start, detail=""):
    return


def print_inspection_timing():
    return


def reset_inspection_timing():
    return


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
    _log_inspection_timing("is_string_or_array", start, f"value={value_str}")
    return soa


def _is_kotlin_list(value):
    start = time.monotonic()
    list_addr = _symbol_loaded_address("kclass:kotlin.collections.List")
    if list_addr == 0:
        return False
    result = _evaluate(
        f"(int)Konan_DebugIsInstance({_hex(value.unsigned)}, {_hex(list_addr)})"
    ).unsigned != 0
    _log_inspection_timing(
        "is_kotlin_list", start, f"value={_hex(value.unsigned)}"
    )
    return result


def _is_kotlin_map(value):
    start = time.monotonic()
    map_addr = _symbol_loaded_address("kclass:kotlin.collections.Map")
    if map_addr == 0:
        return False
    result = _evaluate(
        f"(int)Konan_DebugIsInstance({_hex(value.unsigned)}, {_hex(map_addr)})"
    ).unsigned != 0
    _log_inspection_timing(
        "is_kotlin_map", start, f"value={_hex(value.unsigned)}"
    )
    return result


def _is_kotlin_set(value):
    start = time.monotonic()
    set_addr = _symbol_loaded_address("kclass:kotlin.collections.Set")
    if set_addr == 0:
        return False
    result = _evaluate(
        f"(int)Konan_DebugIsInstance({_hex(value.unsigned)}, {_hex(set_addr)})"
    ).unsigned != 0
    _log_inspection_timing(
        "is_kotlin_set", start, f"value={_hex(value.unsigned)}"
    )
    return result


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
    prefix = _struct_prefix_for_target(target)

    def read_pointer(address):
        error = lldb.SBError()
        raw = process.ReadMemory(address, pointer_size, error)
        if not error.Success():
            return 0
        return struct.unpack(f"{prefix}{pointer_format}", raw)[0]

    type_info = read_pointer(object_address) & ~0x3
    return type_info if type_info and read_pointer(type_info) == type_info else None


def _struct_prefix_for_target(target):
    return ">" if target.GetByteOrder() == lldb.eByteOrderBig else "<"


def _children_count(value):
    value_str = f"{_hex(value.unsigned)}"
    return (
        0
        if value.GetValueAsUnsigned() == 0
        else _evaluate(f"(int)Konan_DebugGetFieldCount({value_str})").signed
    )


def _align_up(value, alignment):
    return (value + alignment - 1) & ~(alignment - 1)


def _allocate_inferior_memory(process, size, permissions, what):
    error = lldb.SBError()
    address = process.AllocateMemory(size, permissions, error)
    if (
        not error.Success()
        or not address
        or address == lldb.LLDB_INVALID_ADDRESS
    ):
        raise DebuggerException(
            f"Failed to allocate inferior memory for {what}"
        )
    return address


def _deallocate_inferior_memory(process, address):
    if not address or address == lldb.LLDB_INVALID_ADDRESS:
        return
    process.DeallocateMemory(address)


def _inferior_process_key(process):
    if process is None or not process.IsValid():
        return None
    return process.GetUniqueID()


def _ensure_inferior_scratch_buffer(process):
    global _INFERIOR_SCRATCH_BUFFER_ADDRESS
    global _INFERIOR_SCRATCH_BUFFER_PROCESS_KEY

    process_key = _inferior_process_key(process)
    if process_key is None:
        raise DebuggerException("No valid process for inferior scratch buffer")

    if (
        _INFERIOR_SCRATCH_BUFFER_PROCESS_KEY == process_key
        and _INFERIOR_SCRATCH_BUFFER_ADDRESS
        and _INFERIOR_SCRATCH_BUFFER_ADDRESS != lldb.LLDB_INVALID_ADDRESS
    ):
        return _INFERIOR_SCRATCH_BUFFER_ADDRESS

    permissions = lldb.ePermissionsReadable | lldb.ePermissionsWritable
    _INFERIOR_SCRATCH_BUFFER_ADDRESS = _allocate_inferior_memory(
        process,
        _INFERIOR_SCRATCH_BUFFER_SIZE,
        permissions,
        "scratch buffer",
    )
    _INFERIOR_SCRATCH_BUFFER_PROCESS_KEY = process_key
    return _INFERIOR_SCRATCH_BUFFER_ADDRESS


def _inferior_scratch_slice(process, specs, what):
    base_address = _ensure_inferior_scratch_buffer(process)
    offset = 0
    addresses = []
    for alignment, size in specs:
        offset = _align_up(offset, alignment)
        end = offset + size
        if end > _INFERIOR_SCRATCH_BUFFER_SIZE:
            raise DebuggerException(
                f"Inferior scratch buffer is too small for {what}"
            )
        addresses.append(base_address + offset)
        offset = end
    return addresses

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
    _log_inspection_timing("select_provider", start, f"value={value_str}")
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

    def _read_pointer(self, address):
        pointer_size = self._target.GetAddressByteSize()
        error = lldb.SBError()
        raw = self._process.ReadMemory(address, pointer_size, error)
        if not error.Success():
            return 0
        pointer_format = "Q" if pointer_size == 8 else "I"
        return struct.unpack(
            f"{_struct_prefix_for_target(self._target)}{pointer_format}", raw
        )[0]

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

    def _run_batch_child_metadata_request(self, include_names=True):
        pointer_size = self._target.GetAddressByteSize()
        pointer_format = "Q" if pointer_size == 8 else "I"
        result_slot_count = 9
        permissions = lldb.ePermissionsReadable | lldb.ePermissionsWritable
        result_addr = _allocate_inferior_memory(
            self._process,
            pointer_size * result_slot_count,
            permissions,
            "object child metadata request",
        )

        count = 0
        field_names_addr = 0
        field_names_size = 0
        field_types_addr = 0
        field_addresses_addr = 0
        type_names_addr = 0
        type_names_size = 0
        summaries_addr = 0
        summaries_size = 0
        try:
            names_setup_expr = (
                "const int initialFieldNamesCapacity = 4096;"
                "char* fieldNamesData = (char*)(void*)malloc(initialFieldNamesCapacity);"
                "if (fieldNamesData == 0) {"
                "  (void)free(fieldTypesData);"
                "  (void)free(fieldAddressesData);"
                "  (void)free(typeNamesData);"
                "  (void)free(summariesData);"
                "  return 0;"
                "}"
                "int fieldNamesCapacity = initialFieldNamesCapacity;"
                "int fieldNamesUsed = 0;"
            ) if include_names else (
                "char* fieldNamesData = 0;"
                "int fieldNamesCapacity = 0;"
                "int fieldNamesUsed = 0;"
            )
            names_append_expr = (
                "  if (!appendCString(&fieldNamesData, &fieldNamesCapacity, &fieldNamesUsed, (const char*)Konan_DebugGetFieldName(obj, i))) {"
                "    (void)free(fieldNamesData);"
                "    (void)free(fieldTypesData);"
                "    (void)free(fieldAddressesData);"
                "    (void)free(typeNamesData);"
                "    (void)free(summariesData);"
                "    return 0;"
                "  }"
            ) if include_names else ""
            _evaluate(
                (
                    "([]() -> int {"
                    "void** result = "
                    f"(void **){_hex(result_addr)};"
                    f"for (int i = 0; i < {result_slot_count}; ++i) result[i] = 0;"
                    "auto appendCString = [](char** buffer, int* capacity, int* used, const char* text) -> int {"
                    "  if (text == 0) text = \"\";"
                    "  if (*buffer == 0 || *capacity <= 0) return 0;"
                    "  int length = 0;"
                    "  while (text[length] != '\\0') ++length;"
                    "  int required = *used + length + 1;"
                    "  if (required > *capacity) {"
                    "    int newCapacity = *capacity;"
                    "    while (required > newCapacity) newCapacity *= 2;"
                    "    char* newBuffer = (char*)(void*)realloc(*buffer, newCapacity);"
                    "    if (newBuffer == 0) return 0;"
                    "    *buffer = newBuffer;"
                    "    *capacity = newCapacity;"
                    "  }"
                    "  for (int i = 0; i < length; ++i) (*buffer)[*used + i] = text[i];"
                    "  (*buffer)[*used + length] = '\\0';"
                    "  *used += length + 1;"
                    "  return 1;"
                    "};"
                    f"void* obj = (void *){_hex(self._valobj.unsigned)};"
                    "int count = (int)Konan_DebugGetFieldCount(obj);"
                    "if (count < 0) count = 0;"
                    "result[0] = (void *)(unsigned long long)count;"
                    "if (count == 0) return 0;"
                    "int* fieldTypesData = (int*)(void*)malloc((unsigned long long)count * sizeof(int));"
                    "if (fieldTypesData == 0) return 0;"
                    "void** fieldAddressesData = (void**)(void*)malloc((unsigned long long)count * sizeof(void*));"
                    "if (fieldAddressesData == 0) {"
                    "  (void)free(fieldTypesData);"
                    "  return 0;"
                    "}"
                    "const int initialTypeNamesCapacity = 4096;"
                    "char* typeNamesData = (char*)(void*)malloc(initialTypeNamesCapacity);"
                    "if (typeNamesData == 0) {"
                    "  (void)free(fieldTypesData);"
                    "  (void)free(fieldAddressesData);"
                    "  return 0;"
                    "}"
                    "int typeNamesCapacity = initialTypeNamesCapacity;"
                    "int typeNamesUsed = 0;"
                    "const int initialSummariesCapacity = 4096;"
                    "char* summariesData = (char*)(void*)malloc(initialSummariesCapacity);"
                    "if (summariesData == 0) {"
                    "  (void)free(fieldTypesData);"
                    "  (void)free(fieldAddressesData);"
                    "  (void)free(typeNamesData);"
                    "  return 0;"
                    "}"
                    "int summariesCapacity = initialSummariesCapacity;"
                    "int summariesUsed = 0;"
                    + names_setup_expr
                    + "for (int i = 0; i < count; ++i) {"
                    "  fieldTypesData[i] = (int)Konan_DebugGetFieldType(obj, i);"
                    "  void* fieldAddress = (void*)Konan_DebugGetFieldAddress(obj, i);"
                    "  fieldAddressesData[i] = fieldAddress;"
                    + names_append_expr
                    + "  const char* typeName = \"\";"
                    "  const char* summary = \"\";"
                    "  char summaryBuffer[1024];"
                    "  summaryBuffer[0] = '\\0';"
                    "  if (fieldTypesData[i] == 1 && fieldAddress != 0) {"
                    "    void* child = *reinterpret_cast<void**>(fieldAddress);"
                    "    if (child != 0) {"
                    "      const char* candidateTypeName = (const char*)Konan_DebugGetTypeName(child);"
                    "      if (candidateTypeName != 0) typeName = candidateTypeName;"
                    "      int written = (int)Konan_DebugObjectToUtf8Array(child, summaryBuffer, 1024);"
                    "      if (written > 0) {"
                    "        if (written >= 1024) summaryBuffer[1023] = '\\0';"
                    "        summary = summaryBuffer;"
                    "      }"
                    "    }"
                    "  }"
                    "  if (!appendCString(&typeNamesData, &typeNamesCapacity, &typeNamesUsed, typeName)) {"
                    "    (void)free(fieldNamesData);"
                    "    (void)free(fieldTypesData);"
                    "    (void)free(fieldAddressesData);"
                    "    (void)free(typeNamesData);"
                    "    (void)free(summariesData);"
                    "    return 0;"
                    "  }"
                    "  if (!appendCString(&summariesData, &summariesCapacity, &summariesUsed, summary)) {"
                    "    (void)free(fieldNamesData);"
                    "    (void)free(fieldTypesData);"
                    "    (void)free(fieldAddressesData);"
                    "    (void)free(typeNamesData);"
                    "    (void)free(summariesData);"
                    "    return 0;"
                    "  }"
                    "}"
                    "result[1] = fieldNamesData;"
                    "result[2] = (void *)(unsigned long long)fieldNamesUsed;"
                    "result[3] = fieldTypesData;"
                    "result[4] = fieldAddressesData;"
                    "result[5] = typeNamesData;"
                    "result[6] = (void *)(unsigned long long)typeNamesUsed;"
                    "result[7] = summariesData;"
                    "result[8] = (void *)(unsigned long long)summariesUsed;"
                    "return 0;"
                    "})()"
                )
            )

            error = lldb.SBError()
            raw_result = self._process.ReadMemory(
                result_addr, pointer_size * result_slot_count, error
            )
            if not error.Success():
                raise DebuggerException(
                    "Failed to read FastKonanObjectSyntheticProvider child metadata result"
                )

            prefix = _struct_prefix_for_target(self._target)
            (
                count,
                field_names_addr,
                field_names_size,
                field_types_addr,
                field_addresses_addr,
                type_names_addr,
                type_names_size,
                summaries_addr,
                summaries_size,
            ) = struct.unpack(
                f"{prefix}{result_slot_count}{pointer_format}", raw_result
            )
            self._children_count = count
            if count <= 0:
                return []
            if (
                (include_names and field_names_addr == 0)
                or field_types_addr == 0
                or field_addresses_addr == 0
                or type_names_addr == 0
                or summaries_addr == 0
            ):
                raise DebuggerException(
                    "FastKonanObjectSyntheticProvider child metadata was not fetched"
                )

            raw_field_names = b""
            if include_names:
                raw_field_names = self._process.ReadMemory(
                    field_names_addr, field_names_size, error
                )
                if not error.Success():
                    raise DebuggerException(
                        "Failed to read FastKonanObjectSyntheticProvider field names"
                    )

            raw_field_types = self._process.ReadMemory(
                field_types_addr, count * 4, error
            )
            if not error.Success():
                raise DebuggerException(
                    "Failed to read FastKonanObjectSyntheticProvider field types"
                )

            raw_field_addresses = self._process.ReadMemory(
                field_addresses_addr, count * pointer_size, error
            )
            if not error.Success():
                raise DebuggerException(
                    "Failed to read FastKonanObjectSyntheticProvider field addresses"
                )

            raw_type_names = self._process.ReadMemory(
                type_names_addr, type_names_size, error
            )
            if not error.Success():
                raise DebuggerException(
                    "Failed to read FastKonanObjectSyntheticProvider type names"
                )

            raw_summaries = self._process.ReadMemory(
                summaries_addr, summaries_size, error
            )
            if not error.Success():
                raise DebuggerException(
                    "Failed to read FastKonanObjectSyntheticProvider summaries"
                )

            def _decode_c_string_array(raw_bytes):
                values = [
                    item.decode("utf-8", errors="replace")
                    for item in raw_bytes.split(b"\0")[:count]
                ]
                while len(values) < count:
                    values.append("")
                return values

            if include_names:
                names = _decode_c_string_array(raw_field_names)
            else:
                names = [str(index) for index in range(count)]
            type_names = _decode_c_string_array(raw_type_names)
            summaries = _decode_c_string_array(raw_summaries)
            types = list(struct.unpack(f"{prefix}{count}i", raw_field_types))
            addresses = list(
                struct.unpack(
                    f"{prefix}{count}{pointer_format}", raw_field_addresses
                )
            )

            cached_addresses = _get_cached_child_addresses_by_index(self._valobj)
            if cached_addresses is not None:
                for index, address in enumerate(addresses):
                    cached_addresses[index] = address
            cached_types = _get_cached_child_types_by_index(self._valobj)
            if cached_types is not None:
                for index, child_type in enumerate(types):
                    cached_types[index] = child_type
            for field_address, child_type, type_name, summary in zip(
                addresses, types, type_names, summaries
            ):
                if child_type == _RUNTIME_TYPE_OBJECT and field_address:
                    child_key = self._read_pointer(field_address)
                    if type_name:
                        _set_cached_field_type_result_for_key(
                            self._process, child_key, type_name
                        )
                    if summary:
                        _set_cached_summary_for_key(
                            self._process, child_key, summary
                        )
                elif field_address:
                    derived_type_name = _type_name_for_runtime_type(child_type)
                    if derived_type_name is not None:
                        _set_cached_field_type_result_for_key(
                            self._process, field_address, derived_type_name
                        )
        finally:
            if (
                field_names_addr
                or field_types_addr
                or field_addresses_addr
                or type_names_addr
                or summaries_addr
            ):
                _evaluate(
                    (
                        "([]() -> int {"
                        f"(void)free((void *){_hex(field_names_addr)});"
                        f"(void)free((void *){_hex(field_types_addr)});"
                        f"(void)free((void *){_hex(field_addresses_addr)});"
                        f"(void)free((void *){_hex(type_names_addr)});"
                        f"(void)free((void *){_hex(summaries_addr)});"
                        "return 0;"
                        "})()"
                    )
                )
            _deallocate_inferior_memory(self._process, result_addr)
        return names

    def _ensure_children(self, reason):
        start = time.monotonic()
        if self._children is None:
            try:
                self._children = self._run_batch_child_metadata_request()
            except DebuggerException:
                self._children = [
                    self._field_name(i) for i in range(self._children_count)
                ]
            self._log.debug(
                "%s _children: %s",
                _hex(self._valobj.unsigned),
                self._children,
            )
        _log_inspection_timing(
            "fast_object_ensure_children",
            start,
            f"value={_hex(self._valobj.unsigned)} reason={reason}",
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
        start = time.monotonic()
        value_str = _hex(self._valobj.unsigned)
        self._log.debug("%s, %s", value_str, name)
        index = self._ensure_children(
            f"resolve requested field '{name}'"
        ).index(name)
        self._log.debug("%s index=%s", value_str, name)
        _log_inspection_timing(
            "fast_object_get_child_index",
            start,
            f"value={value_str} name={name}",
        )
        return index

    def get_child_at_index(self, index):
        start = time.monotonic()
        self._log.debug("%s, %s", _hex(self._valobj.unsigned), index)
        result = self._read_value(index)
        _log_inspection_timing(
            "fast_object_get_child_at_index",
            start,
            f"value={_hex(self._valobj.unsigned)} index={index}",
        )
        return result

    def _field_address(self, index):
        cached_addresses = _get_cached_child_addresses_by_index(self._valobj)
        if cached_addresses is not None:
            if index not in cached_addresses:
                self._ensure_children(
                    f"prefetch requested field address '{index}'"
                )
            if index in cached_addresses:
                return cached_addresses[index]
        return super()._field_address(index)

    def _field_type(self, index):
        cached_types = _get_cached_child_types_by_index(self._valobj)
        if cached_types is not None:
            if index not in cached_types:
                self._ensure_children(
                    f"prefetch requested field type '{index}'"
                )
            if index in cached_types:
                return cached_types[index]
        return super()._field_type(index)

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

    def _run_batch_child_metadata_request(self):
        return FastKonanObjectSyntheticProvider._run_batch_child_metadata_request(
            self, include_names=False
        )

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
        has_address = index in cached_addresses
        has_type = cached_types is None or index in cached_types
        if has_address and has_type:
            return
        self._run_batch_child_metadata_request()

    def _string_batch_buffer_size(self, count, per_item_size):
        return max(
            _MIN_BATCH_STRING_BUFFER_SIZE,
            count * per_item_size,
        )

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

    def _run_batch_address_request(self, indices, count, prefix):
        start = time.monotonic()
        pointer_size = self._target.GetAddressByteSize()
        indices_addr, result_addr = _inferior_scratch_slice(
            self._process,
            [
                (4, count * 4),
                (pointer_size, count * pointer_size),
            ],
            "address request",
        )
        self._write_batch_indices(indices, count, prefix, indices_addr)
        _evaluate(
            (
                "([]() -> int {"
                f"int32_t* indices = (int32_t *){_hex(indices_addr)};"
                f"void** result = (void **){_hex(result_addr)};"
                "auto getFieldAddress = "
                "(void *(*)(void *, int32_t))Konan_DebugGetFieldAddress;"
                f"void* obj = (void *){_hex(self._valobj.unsigned)};"
                f"for (int i = 0; i < {count}; ++i) "
                "{ result[i] = getFieldAddress(obj, indices[i]); }"
                "return 0;"
                "})()"
            )
        )
        error = lldb.SBError()
        raw_addresses = self._process.ReadMemory(
            result_addr, count * pointer_size, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider addresses"
            )
        pointer_format = "Q" if pointer_size == 8 else "I"
        result = struct.unpack(
            f"{prefix}{count}{pointer_format}", raw_addresses
        )
        _log_inspection_timing(
            "batch_address_request", start, f"count={count}"
        )
        return result

    def _run_batch_field_type_request(self, indices, count, prefix):
        start = time.monotonic()
        indices_addr, result_addr = _inferior_scratch_slice(
            self._process,
            [
                (4, count * 4),
                (4, count * 4),
            ],
            "field type request",
        )
        self._write_batch_indices(indices, count, prefix, indices_addr)
        _evaluate(
            (
                "([]() -> int {"
                f"int32_t* indices = (int32_t *){_hex(indices_addr)};"
                f"int32_t* result = (int32_t *){_hex(result_addr)};"
                "auto getFieldType = "
                "(int32_t (*)(void *, int32_t))Konan_DebugGetFieldType;"
                f"void* obj = (void *){_hex(self._valobj.unsigned)};"
                f"for (int i = 0; i < {count}; ++i) "
                "{ result[i] = getFieldType(obj, indices[i]); }"
                "return 0;"
                "})()"
            )
        )
        error = lldb.SBError()
        raw_types = self._process.ReadMemory(result_addr, count * 4, error)
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider field types"
            )
        result = struct.unpack(f"{prefix}{count}i", raw_types)
        _log_inspection_timing(
            "batch_field_type_request", start, f"count={count}"
        )
        return result

    def _run_batch_summary_request(self, indices, count, prefix):
        if self._element_runtime_type != _RUNTIME_TYPE_OBJECT:
            return ([-1] * count, [0] * count, None)

        start = time.monotonic()
        buffer_size = self._string_batch_buffer_size(
            count, _SUMMARY_BATCH_BUFFER_SIZE_PER_ITEM
        )
        indices_addr, offsets_addr, lengths_addr, buffer_addr = (
            _inferior_scratch_slice(
                self._process,
                [
                    (4, count * 4),
                    (4, count * 4),
                    (4, count * 4),
                    (1, buffer_size),
                ],
                "summary request",
            )
        )
        self._write_batch_indices(indices, count, prefix, indices_addr)
        _evaluate(
            (
                "([]() -> int {"
                f"int32_t* indices = (int32_t *){_hex(indices_addr)};"
                f"int32_t* offsets = (int32_t *){_hex(offsets_addr)};"
                f"int32_t* lengths = (int32_t *){_hex(lengths_addr)};"
                f"char* buffer = (char *){_hex(buffer_addr)};"
                f"const int bufferSize = {buffer_size};"
                "auto getFieldAddress = "
                "(void *(*)(void *, int32_t))Konan_DebugGetFieldAddress;"
                "auto getFieldType = "
                "(int32_t (*)(void *, int32_t))Konan_DebugGetFieldType;"
                "auto objectToUtf8Array = "
                "(int32_t (*)(void *, char *, int32_t))Konan_DebugObjectToUtf8Array;"
                f"void* obj = (void *){_hex(self._valobj.unsigned)};"
                f"for (int i = 0; i < {count}; ++i) {{ offsets[i] = -1; lengths[i] = 0; }}"
                "int used = 0;"
                f"for (int i = 0; i < {count}; ++i) {{"
                "  void* fieldAddress = getFieldAddress(obj, indices[i]);"
                "  if (fieldAddress == nullptr) continue;"
                f"  if (getFieldType(obj, indices[i]) != {_RUNTIME_TYPE_OBJECT}) continue;"
                "  void* child = *reinterpret_cast<void**>(fieldAddress);"
                "  if (child == nullptr || used >= bufferSize) continue;"
                "  int written = objectToUtf8Array(child, buffer + used, bufferSize - used);"
                "  if (written <= 0) continue;"
                "  offsets[i] = used;"
                "  lengths[i] = written;"
                "  used += written;"
                "}"
                "return 0;"
                "})()"
            )
        )

        error = lldb.SBError()
        raw_summary_offsets = self._process.ReadMemory(
            offsets_addr, count * 4, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider summary offsets"
            )
        raw_summary_lengths = self._process.ReadMemory(
            lengths_addr, count * 4, error
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
            _log_inspection_timing(
                "batch_summary_request", start, f"count={count}"
            )
            return (summary_offsets, summary_lengths, None)

        summary_data = self._process.ReadMemory(
            buffer_addr, max_summary_end, error
        )
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider summaries"
            )
        result = (summary_offsets, summary_lengths, summary_data)
        _log_inspection_timing(
            "batch_summary_request", start, f"count={count}"
        )
        return result

    def _run_batch_type_name_request(self, indices, count, prefix):
        if self._element_runtime_type != _RUNTIME_TYPE_OBJECT:
            return ([-1] * count, [0] * count, None)

        start = time.monotonic()
        buffer_size = self._string_batch_buffer_size(
            count, _TYPE_NAME_BATCH_BUFFER_SIZE_PER_ITEM
        )
        indices_addr, offsets_addr, lengths_addr, buffer_addr = (
            _inferior_scratch_slice(
                self._process,
                [
                    (4, count * 4),
                    (4, count * 4),
                    (4, count * 4),
                    (1, buffer_size),
                ],
                "type name request",
            )
        )
        self._write_batch_indices(indices, count, prefix, indices_addr)
        _evaluate(
            (
                "([]() -> int {"
                f"int32_t* indices = (int32_t *){_hex(indices_addr)};"
                f"int32_t* offsets = (int32_t *){_hex(offsets_addr)};"
                f"int32_t* lengths = (int32_t *){_hex(lengths_addr)};"
                f"char* buffer = (char *){_hex(buffer_addr)};"
                f"const int bufferSize = {buffer_size};"
                "auto getFieldAddress = "
                "(void *(*)(void *, int32_t))Konan_DebugGetFieldAddress;"
                "auto getFieldType = "
                "(int32_t (*)(void *, int32_t))Konan_DebugGetFieldType;"
                "auto getTypeName = "
                "(const char *(*)(void *))Konan_DebugGetTypeName;"
                f"void* obj = (void *){_hex(self._valobj.unsigned)};"
                f"for (int i = 0; i < {count}; ++i) {{ offsets[i] = -1; lengths[i] = 0; }}"
                "int used = 0;"
                f"for (int i = 0; i < {count}; ++i) {{"
                "  void* fieldAddress = getFieldAddress(obj, indices[i]);"
                "  if (fieldAddress == nullptr) continue;"
                f"  if (getFieldType(obj, indices[i]) != {_RUNTIME_TYPE_OBJECT}) continue;"
                "  void* child = *reinterpret_cast<void**>(fieldAddress);"
                "  if (child == nullptr || used >= bufferSize) continue;"
                "  const char* typeName = getTypeName(child);"
                "  if (typeName == nullptr) continue;"
                "  int written = 0;"
                "  while (used + written + 1 < bufferSize && typeName[written] != '\\0') {"
                "    buffer[used + written] = typeName[written];"
                "    ++written;"
                "  }"
                "  if (used + written >= bufferSize) continue;"
                "  buffer[used + written] = '\\0';"
                "  offsets[i] = used;"
                "  lengths[i] = written + 1;"
                "  used += written + 1;"
                "}"
                "return 0;"
                "})()"
            )
        )

        error = lldb.SBError()
        raw_offsets = self._process.ReadMemory(offsets_addr, count * 4, error)
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider type name offsets"
            )
        raw_lengths = self._process.ReadMemory(lengths_addr, count * 4, error)
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
            _log_inspection_timing(
                "batch_type_name_request", start, f"count={count}"
            )
            return (offsets, lengths, None)

        type_name_data = self._process.ReadMemory(buffer_addr, max_end, error)
        if not error.Success():
            raise DebuggerException(
                "Failed to read FastKonanArraySyntheticProvider type names"
            )
        result = (offsets, lengths, type_name_data)
        _log_inspection_timing(
            "batch_type_name_request", start, f"count={count}"
        )
        return result

    def _prefetch_field_types(self, indices, count, prefix):
        if self._element_runtime_type != _RUNTIME_TYPE_INVALID:
            return [self._element_runtime_type] * count
        return self._run_batch_field_type_request(indices, count, prefix)

    def _struct_prefix(self):
        return _struct_prefix_for_target(self._target)

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
    start = time.monotonic()
    try:
        field_index = object_proxy.get_child_index(field_name)
    except (DebuggerException, ValueError):
        _log_inspection_timing(
            "object_field_lookup",
            start,
            f"proxy={object_proxy.__class__.__name__} field={field_name} missing",
        )
        return None
    result = object_proxy.get_child_at_index(field_index)
    _log_inspection_timing(
        "object_field_lookup",
        start,
        f"proxy={object_proxy.__class__.__name__} field={field_name}",
    )
    return result


def _object_field_unsigned(object_proxy, field_name):
    start = time.monotonic()
    value = _object_field(object_proxy, field_name)
    if value is None or not value.IsValid():
        _log_inspection_timing(
            "object_field_unsigned",
            start,
            f"proxy={object_proxy.__class__.__name__} field={field_name} missing",
        )
        return None
    result = value.GetValueAsUnsigned()
    _log_inspection_timing(
        "object_field_unsigned",
        start,
        f"proxy={object_proxy.__class__.__name__} field={field_name}",
    )
    return result


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
        _log_inspection_timing("get_proxy", start, f"value={value_str}")
        self._log.debug(
            "%s _proxy: %s", value_str, proxy.__class__.__name__
        )
        _set_cached_resolved_proxy(self._valobj, proxy)
        return proxy

    @staticmethod
    def _collection_kind(valobj, type_info):
        start = time.monotonic()
        if _is_kotlin_list(valobj):
            result = CollectionKind.LIST
        elif _is_kotlin_map(valobj):
            result = CollectionKind.MAP
        elif _is_kotlin_set(valobj):
            result = CollectionKind.SET
        else:
            result = None
        _log_inspection_timing(
            "collection_kind", start, f"value={_hex(valobj.unsigned)}"
        )
        return result

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
    process = variable.GetProcess()
    if process is None or not process.IsValid():
        return ""
    if variable is None or not variable.IsValid():
        return ""
    if variable.GetTypeName() != "ObjHeader *":
        return ""
    if variable.GetValueAsUnsigned() == 0:
        return ""

    buffer_size = 512
    (buffer_addr,) = _inferior_scratch_slice(
        process, [(1, buffer_size)], "runtime type buffer"
    )

    length = _evaluate(
        (
            "([]() -> int {"
            f"char* dst = (char *){_hex(buffer_addr)};"
            "const char* src = "
            f"(const char*)Konan_DebugGetTypeName({_hex(variable.unsigned)});"
            "if (src == nullptr) { dst[0] = '\\0'; return 0; }"
            "int i = 0;"
            f"for (; i < {buffer_size - 1} && src[i] != '\\0'; ++i) "
            "{ dst[i] = src[i]; }"
            "dst[i] = '\\0';"
            "return i;"
            "})()"
        )
    ).signed
    if length <= 0:
        return ""

    error = lldb.SBError()
    type_name = process.ReadCStringFromMemory(
        int(buffer_addr), int(length + 1), error
    )
    if not error.Success():
        raise DebuggerException()
    return type_name


_FALLBACK_DISPLAY_TYPES = {
    "signed char": "Byte",
    "char": "Byte",
    "short": "Short",
    "int": "Int",
    "long": "Long",
    "long long": "Long",
    "float": "Float",
    "double": "Double",
    "bool": "Boolean",
    "void *": "NativePtr",
}


def _fallback_display_type(variable):
    if variable is None or not variable.IsValid():
        return ""
    return _FALLBACK_DISPLAY_TYPES.get(variable.GetTypeName(), "")


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
            try:
                provider = KonanProxyTypeProvider(variable, internal_dict)
                field_index = provider.get_child_index(field_name)
                if field_index < 0:
                    variable = None
                    break
                variable = provider.get_child_at_index(field_index)
            except (DebuggerException, ValueError):
                variable = None
                break
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
            else:
                fallback = _fallback_display_type(variable)
                if len(fallback) > 0:
                    desc = fallback

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


def _init_logger():
    return


def __lldb_init_module(debugger, _):
    _init_logger()
    target = debugger.GetSelectedTarget()
    process = None if not target or not target.IsValid() else target.GetProcess()
    if process is not None and process.IsValid():
        try:
            _ensure_inferior_scratch_buffer(process)
        except DebuggerException:
            pass
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
