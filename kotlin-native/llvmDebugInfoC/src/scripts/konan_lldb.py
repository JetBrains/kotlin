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
_RUNTIME_TYPE_INVALID = 0
_RUNTIME_TYPE_OBJECT = 1
_BENCH_LOGGING = False


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
        f"(int)Konan_DebugIsInstance({value_str}, {_hex(string_addr)}) ? 1 "
        f": ((int)Konan_DebugIsArray({value_str})) ? 2 : 0"
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
_SBVALUE_CACHE = {}
_SB_VALUE_CACHE_PROCESS_HASH = None
_MAP_ENTRY_TYPE = None
_MAP_ENTRY_TYPE_PROCESS_ID = None
_TO_STRING_DEPTH = 2
_ARRAY_TO_STRING_LIMIT = 10
_CHILD_CACHE_LINE_SIZE = 200
_TOTAL_MEMBERS_LIMIT = 50


class CachedSBValueInfo:
    def __init__(self):
        self.children_count = None
        self.child_addresses_by_index = None
        self.child_names_by_index = None
        self.child_types_by_index = None
        self.resolved_proxy = None
        self.summary = None
        self.type_name = None


def _clear_sbvalue_query_cache():
    global _SBVALUE_CACHE
    global _SB_VALUE_CACHE_PROCESS_HASH
    _SBVALUE_CACHE = {}
    _SB_VALUE_CACHE_PROCESS_HASH = None


def _get_process_state_hash(process):
    return process.GetUniqueID(), process.GetStopID(False)


def _clear_stale_cache(process):
    global _SB_VALUE_CACHE_PROCESS_HASH
    state = _get_process_state_hash(process)
    if _SB_VALUE_CACHE_PROCESS_HASH != state:
        _clear_sbvalue_query_cache()
        _SB_VALUE_CACHE_PROCESS_HASH = state
        return False
    return True


def _get_cached_sbvalue_info(value):
    if value is None or value.GetValueAsUnsigned() == 0:
        return None
    return _get_cached_sbvalue_info_for_key(
        value.GetProcess(), value.GetValueAsUnsigned()
    )


def _get_or_create_cached_sbvalue_info(value):
    if value is None or value.GetValueAsUnsigned() == 0:
        return None
    return _get_or_create_cached_sbvalue_info_for_key(
        value.GetProcess(), value.GetValueAsUnsigned()
    )


def _get_cached_sbvalue_info_for_key(process, key):
    if key == 0:
        return None
    _clear_stale_cache(process)
    return _SBVALUE_CACHE.get(key)


def _get_or_create_cached_sbvalue_info_for_key(process, key):
    if key == 0:
        return None
    _clear_stale_cache(process)
    cached_info = _SBVALUE_CACHE.get(key)
    if cached_info is None:
        cached_info = CachedSBValueInfo()
        _SBVALUE_CACHE[key] = cached_info
    return cached_info


def _get_cached_child_address(value, index):
    cached_info = _get_cached_sbvalue_info(value)
    cached_addresses = None if cached_info is None else cached_info.child_addresses_by_index
    return None if cached_addresses is None else cached_addresses.get(index)


def _set_cached_child_address(value, index, address):
    cached_info = _get_or_create_cached_sbvalue_info(value)
    if cached_info is None:
        return
    if cached_info.child_addresses_by_index is None:
        cached_info.child_addresses_by_index = {}
    cached_info.child_addresses_by_index[index] = address


def _get_cached_children_count(value):
    cached_info = _get_cached_sbvalue_info(value)
    return None if cached_info is None else cached_info.children_count


def _set_cached_children_count(value, children_count):
    cached_info = _get_or_create_cached_sbvalue_info(value)
    if cached_info is not None:
        cached_info.children_count = children_count


def _get_cached_child_name(value, index):
    cached_info = _get_cached_sbvalue_info(value)
    cached_names = None if cached_info is None else cached_info.child_names_by_index
    return None if cached_names is None else cached_names.get(index)


def _set_cached_child_name(value, index, name):
    cached_info = _get_or_create_cached_sbvalue_info(value)
    if cached_info is None:
        return
    if cached_info.child_names_by_index is None:
        cached_info.child_names_by_index = {}
    cached_info.child_names_by_index[index] = name


def _get_cached_child_type(value, index):
    cached_info = _get_cached_sbvalue_info(value)
    cached_types = None if cached_info is None else cached_info.child_types_by_index
    return None if cached_types is None else cached_types.get(index)


def _set_cached_child_type(value, index, child_type):
    cached_info = _get_or_create_cached_sbvalue_info(value)
    if cached_info is None:
        return
    if cached_info.child_types_by_index is None:
        cached_info.child_types_by_index = {}
    cached_info.child_types_by_index[index] = child_type


def _get_cached_proxy(value):
    cached_info = _get_cached_sbvalue_info(value)
    return None if cached_info is None else cached_info.resolved_proxy


def _set_cached_proxy(value, proxy):
    if proxy is None:
        return
    cached_info = _get_or_create_cached_sbvalue_info(value)
    if cached_info is not None:
        cached_info.resolved_proxy = proxy


def _get_cached_summary(process, key):
    cached_info = _get_cached_sbvalue_info_for_key(process, key)
    return None if cached_info is None else cached_info.summary


def _set_cached_summary(process, key, summary):
    if summary is None:
        return
    cached_info = _get_or_create_cached_sbvalue_info_for_key(process, key)
    if cached_info is not None:
        cached_info.summary = summary


def _get_cached_type_name(variable):
    if variable is None or not variable.IsValid():
        return None
    if variable.GetTypeName() != "ObjHeader *":
        return None
    cached_info = _get_cached_sbvalue_info(variable)
    return None if cached_info is None else cached_info.type_name


def _set_cached_type_name(process, key, type_name):
    if type_name is None:
        return
    cached_info = _get_or_create_cached_sbvalue_info_for_key(process, key)
    if cached_info is not None:
        cached_info.type_name = type_name


def _decode_c_string_array(raw_bytes, count):
    values = [
        item.decode("utf-8", errors="replace")
        for item in raw_bytes.split(b"\0")[:count]
    ]
    while len(values) < count:
        values.append("")
    return values


def _read_pointer(process, target, address):
    pointer_size = target.GetAddressByteSize()
    error = lldb.SBError()
    raw = process.ReadMemory(address, pointer_size, error)
    if not error.Success():
        return 0
    pointer_format = _pointer_format_for_target(target)
    byte_order = _byte_order_prefix_for_target(target)
    return struct.unpack(f"{byte_order}{pointer_format}", raw)[0]


def _fast_type_info(value):
    process = value.GetProcess()
    target = lldb.debugger.GetSelectedTarget()
    object_address = value.GetValueAsUnsigned()
    if not process.IsValid() or not target.IsValid() or object_address == 0:
        return None

    type_info = _read_pointer(process, target, object_address) & ~0x3
    return (
        type_info
        if type_info and _read_pointer(process, target, type_info) == type_info
        else None
    )


def _byte_order_prefix_for_target(target):
    return ">" if target.GetByteOrder() == lldb.eByteOrderBig else "<"


def _pointer_format_for_target(target):
    return "Q" if target.GetAddressByteSize() == 8 else "I"


def _children_count(value):
    cached_children_count = _get_cached_children_count(value)
    if cached_children_count is not None:
        return cached_children_count
    value_str = f"{_hex(value.unsigned)}"
    children_count = (
        0
        if value.GetValueAsUnsigned() == 0
        else _evaluate(f"(int)Konan_DebugGetFieldCount({value_str})").signed
    )
    _set_cached_children_count(value, children_count)
    return children_count


def _allocate_inferior_memory(process, size, permissions):
    error = lldb.SBError()
    address = process.AllocateMemory(size, permissions, error)
    if (
        not error.Success()
        or not address
        or address == lldb.LLDB_INVALID_ADDRESS
    ):
        raise DebuggerException("Failed to allocate inferior memory")
    return address


def _deallocate_inferior_memory(process, address):
    if not address or address == lldb.LLDB_INVALID_ADDRESS:
        return
    process.DeallocateMemory(address)


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


def _resolve_lldb_type(valobj, runtime_type):
    if runtime_type <= _RUNTIME_TYPE_INVALID or runtime_type >= len(_TYPES):
        return None
    return _TYPES[runtime_type](valobj)


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


def _get_map_entry_type(process):
    global _MAP_ENTRY_TYPE
    global _MAP_ENTRY_TYPE_PROCESS_ID

    process_id = process.GetUniqueID()
    if (
        _MAP_ENTRY_TYPE_PROCESS_ID == process_id
        and _MAP_ENTRY_TYPE is not None
        and _MAP_ENTRY_TYPE.IsValid()
    ):
        return _MAP_ENTRY_TYPE

    value = _evaluate(
        """
        struct ObjHeader;
        struct _KotlinMapEntry {
            ObjHeader *key;
            ObjHeader *value;
        };
        _KotlinMapEntry{(ObjHeader *)0, (ObjHeader *)0}
        """
    )

    if value is None or not value.IsValid():
        return None

    error = value.GetError()
    if error.Fail():
        return None

    entry_type = value.GetType()
    if entry_type is None or not entry_type.IsValid():
        return None

    _MAP_ENTRY_TYPE = entry_type
    _MAP_ENTRY_TYPE_PROCESS_ID = process_id
    return _MAP_ENTRY_TYPE


def _render_object(addr):
    process = lldb.debugger.GetSelectedTarget().GetProcess()
    cached = _get_cached_summary(process, addr)
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


def kotlin_map_entry_type_summary(lldb_val, _):
    synthetic = _synthetic_value_or_self(lldb_val)
    if synthetic is None:
        return _NULL

    summaries = []
    for index in range(2):
        child = synthetic.GetChildAtIndex(index)
        if child is None or not child.IsValid():
            summaries.append(_NULL)
            continue
        summary = child.GetSummary()
        value = summary if summary is not None else child.GetValue()
        summaries.append(_NULL if value is None else value)
    return " = ".join(summaries)


def _collection_kind(valobj):
    if _is_kotlin_list(valobj):
        return CollectionKind.LIST
    if _is_kotlin_map(valobj):
        return CollectionKind.MAP
    if _is_kotlin_set(valobj):
        return CollectionKind.SET
    return None


def _select_provider(lldb_val, internal_dict):
    start = time.monotonic()
    value_str = f"{_hex(lldb_val.unsigned)}"
    tip = _fast_type_info(lldb_val) or _type_info(lldb_val)
    logging.debug(
        "%s name:%s tip:%s",
        value_str,
        lldb_val.name,
        _hex(tip) if tip else None,
    )

    if lldb_val.unsigned == 0:
        ret = KonanNullSyntheticProvider(lldb_val)
    elif not tip:
        ret = KonanNotInitializedObjectSyntheticProvider(lldb_val)
    else:
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

        # Optionally map the KonanObjectSyntheticProvider to an additional collection provider,
        # which implement the user-friendly formatting for known collections.
        ret = raw_provider
        if isinstance(raw_provider, KonanObjectSyntheticProvider):
            collection_kind = _collection_kind(lldb_val)
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
        child_name = _get_cached_child_name(self._valobj, index)
        if child_name is not None:
            return child_name

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


def _run_batch_child_metadata_request(provider, start_index, include_names=True):
    total_count = _children_count(provider._valobj)
    if total_count <= 0 or start_index >= total_count:
        return
    count = min(_CHILD_CACHE_LINE_SIZE, total_count - start_index)

    pointer_size = provider._target.GetAddressByteSize()
    result_slot_count = 6
    permissions = lldb.ePermissionsReadable | lldb.ePermissionsWritable
    result_addr = _allocate_inferior_memory(
        provider._process,
        pointer_size * result_slot_count,
        permissions,
    )

    metadata_addrs = (0, 0, 0, 0)
    try:
        _evaluate_batch_child_metadata_request(
            provider,
            result_addr,
            result_slot_count,
            start_index,
            count,
            include_names,
        )
        (
            metadata_addrs,
            names,
            types,
            addresses,
            type_names,
        ) = _read_child_metadata(
            provider,
            result_addr,
            result_slot_count,
            start_index,
            count,
            include_names,
        )
        _cache_child_metadata(
            provider,
            start_index,
            names,
            types,
            addresses,
            type_names,
            include_names,
        )
    finally:
        _free_child_prefetch_memory(provider._process, result_addr, metadata_addrs)


def _get_cpp_helpers_string():
    return f"""
auto freeAll = [](int* fieldTypesData, void** fieldAddressesData, char* fieldNamesData, char* typeNamesData) {{
    (void)free(fieldNamesData);
    (void)free(fieldTypesData);
    (void)free(fieldAddressesData);
    (void)free(typeNamesData);
}};

auto appendCString = [](char** buffer, int* capacity, int* used, const char* text) -> int {{
    if (text == 0) text = "";
    if (*buffer == 0 || *capacity <= 0) return 0;
    int length = 0;
    while (text[length] != '\\0') ++length;
    int required = *used + length + 1;
    if (required > *capacity) {{
        int newCapacity = *capacity;
        while (required > newCapacity) newCapacity *= 2;
        char* newBuffer = (char*)(void*)realloc(*buffer, newCapacity);
        if (newBuffer == 0) return 0;
        *buffer = newBuffer;
        *capacity = newCapacity;
    }}
    for (int i = 0; i < length; ++i) (*buffer)[*used + i] = text[i];
    (*buffer)[*used + length] = '\\0';
    *used += length + 1;
    return 1;
}};

auto getObjectTypeName = [](int fieldType, void* fieldAddress) -> const char* {{
    if (fieldType != {_RUNTIME_TYPE_OBJECT} || fieldAddress == 0) return "";
    void* child = *reinterpret_cast<void**>(fieldAddress);
    if (child == 0) return "";
    const char* typeName = (const char*)Konan_DebugGetTypeName(child);
    return typeName != 0 ? typeName : "";
}};
"""


def _evaluate_batch_child_metadata_request(provider, result_addr, result_slot_count, start_index, count, include_names):
    _evaluate(
        f"""
        ([]() -> int {{
            void** result = (void **){_hex(result_addr)};
            for (int i = 0; i < {result_slot_count}; ++i) result[i] = 0;
            void* obj = (void *){_hex(provider._valobj.unsigned)};
            int startIndex = {start_index};
            int count = {count};
            const bool includeFieldNames = {'true' if include_names else 'false'};
            {_get_cpp_helpers_string()}

            int* fieldTypesData = (int*)(void*)malloc((unsigned long long)count * sizeof(int));
            void** fieldAddressesData = (void**)(void*)malloc((unsigned long long)count * sizeof(void*));
            const int initialTypeNamesCapacity = 4096;
            char* typeNamesData = (char*)(void*)malloc(initialTypeNamesCapacity);
            const int initialFieldNamesCapacity = 4096;
            char* fieldNamesData = includeFieldNames ? (char*)(void*)malloc(initialFieldNamesCapacity) : 0;

            if (fieldTypesData == 0 || fieldAddressesData == 0 || typeNamesData == 0 || (includeFieldNames && fieldNamesData == 0)) {{
                freeAll(fieldTypesData, fieldAddressesData, fieldNamesData, typeNamesData);
                return 0;
            }}

            int typeNamesCapacity = initialTypeNamesCapacity;
            int typeNamesUsed = 0;
            int fieldNamesCapacity = includeFieldNames ? initialFieldNamesCapacity : 0;
            int fieldNamesUsed = 0;

            for (int i = 0; i < count; ++i) {{
                int fieldIndex = startIndex + i;
                fieldTypesData[i] = (int)Konan_DebugGetFieldType(obj, fieldIndex);
                void* fieldAddress = (void*)Konan_DebugGetFieldAddress(obj, fieldIndex);
                fieldAddressesData[i] = fieldAddress;
                const char* typeName = getObjectTypeName(fieldTypesData[i], fieldAddress);
                appendCString(&typeNamesData, &typeNamesCapacity, &typeNamesUsed, typeName);
                if (includeFieldNames) {{
                    const char* fieldName = (const char*)Konan_DebugGetFieldName(obj, fieldIndex);
                    appendCString(&fieldNamesData, &fieldNamesCapacity, &fieldNamesUsed, fieldName);
                }}
            }}

            result[0] = fieldNamesData;
            result[1] = (void *)(unsigned long long)fieldNamesUsed;
            result[2] = fieldTypesData;
            result[3] = fieldAddressesData;
            result[4] = typeNamesData;
            result[5] = (void *)(unsigned long long)typeNamesUsed;
            return 0;
        }})()
        """
    )


def _read_memory(process, address, size):
    error = lldb.SBError()
    raw_value = process.ReadMemory(address, size, error)
    if not error.Success():
        raise DebuggerException("Failed to read inferior memory")
    return raw_value


def _read_child_metadata(provider, result_addr, result_slot_count, start_index, count, include_names):

    # Read the array of pointers from the inferior.
    # These pointers point to arrays which contain the prefetched data.
    pointer_size = provider._target.GetAddressByteSize()
    pointer_format = _pointer_format_for_target(provider._target)
    prefix = _byte_order_prefix_for_target(provider._target)
    raw_result = _read_memory(
        provider._process,
        result_addr,
        pointer_size * result_slot_count,
    )
    (
        field_names_addr,
        field_names_size,
        field_types_addr,
        field_addresses_addr,
        type_names_addr,
        type_names_size,
    ) = struct.unpack(f"{prefix}{result_slot_count}{pointer_format}", raw_result)

    if ((include_names and field_names_addr == 0) or field_types_addr == 0 or field_addresses_addr == 0 or type_names_addr == 0):
        raise DebuggerException("Could not read result of a child prefetch")

    # Move the prefetched arrays to the debugger's memory
    raw_field_names = b""
    if include_names:
        raw_field_names = _read_memory(
            provider._process,
            field_names_addr,
            field_names_size,
        )
    raw_field_types = _read_memory(
        provider._process,
        field_types_addr,
        count * 4,
    )
    raw_field_addresses = _read_memory(
        provider._process,
        field_addresses_addr,
        count * pointer_size,
    )
    raw_type_names = _read_memory(
        provider._process,
        type_names_addr,
        type_names_size,
    )

    # Decode the prefetched arrays to python data types
    if include_names:
        names = _decode_c_string_array(raw_field_names, count)
    else:
        names = [str(start_index + index) for index in range(count)]
    type_names = _decode_c_string_array(raw_type_names, count)
    types = list(struct.unpack(f"{prefix}{count}i", raw_field_types))
    addresses = list(struct.unpack(f"{prefix}{count}{pointer_format}", raw_field_addresses))

    return (
        (
            field_names_addr,
            field_types_addr,
            field_addresses_addr,
            type_names_addr,
        ),
        names,
        types,
        addresses,
        type_names,
    )


def _cache_child_metadata(provider, start_index, names, types, addresses, type_names, include_names):
    if include_names:
        for offset, name in enumerate(names):
            index = start_index + offset
            _set_cached_child_name(provider._valobj, index, name)
    for offset, address in enumerate(addresses):
        index = start_index + offset
        _set_cached_child_address(provider._valobj, index, address)
    for offset, child_type in enumerate(types):
        index = start_index + offset
        _set_cached_child_type(provider._valobj, index, child_type)
    for field_address, child_type, type_name in zip(addresses, types, type_names):
        if child_type == _RUNTIME_TYPE_OBJECT and field_address and type_name:
            child_key = _read_pointer(provider._process, provider._target, field_address)
            _set_cached_type_name(provider._process, child_key, type_name)


def _free_child_prefetch_memory(process, result_addr, metadata_addrs):
    (
        field_names_addr,
        field_types_addr,
        field_addresses_addr,
        type_names_addr,
    ) = metadata_addrs
    if (
        field_names_addr
        or field_types_addr
        or field_addresses_addr
        or type_names_addr
    ):
        _evaluate(
            (
                "([]() -> int {"
                f"(void)free((void *){_hex(field_names_addr)});"
                f"(void)free((void *){_hex(field_types_addr)});"
                f"(void)free((void *){_hex(field_addresses_addr)});"
                f"(void)free((void *){_hex(type_names_addr)});"
                "return 0;"
                "})()"
            )
        )
    _deallocate_inferior_memory(process, result_addr)


def _ensure_cached_child_metadata_line(provider, index, include_names):
    """Prefetch the cache line containing index when required child metadata is missing."""
    if index < 0:
        return

    if (
        _get_cached_child_address(provider._valobj, index) is None
        or _get_cached_child_type(provider._valobj, index) is None
        or (include_names and _get_cached_child_name(provider._valobj, index) is None)
    ):
        _run_batch_child_metadata_request(
            provider,
            index - (index % _CHILD_CACHE_LINE_SIZE),
            include_names=include_names,
        )


class KonanObjectSyntheticProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj, internal_dict):
        super().__init__(valobj)
        self._target = valobj.GetTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj

    def num_children(self):
        return _children_count(self._valobj)

    def get_child_index(self, name):
        children_count = self.num_children()
        for index in range(children_count):
            _ensure_cached_child_metadata_line(self, index, True)
            if _get_cached_child_name(self._valobj, index) == name:
                return index
        return -1

    def get_child_at_index(self, index):
        if not (0 <= index < self.num_children()):
            return None

        address = self._field_address(index)
        if address is None:
            return None

        child_name = self._child_name(index)
        if child_name is None:
            return None

        lldb_type = _resolve_lldb_type(self._valobj, self._field_type(index))
        if lldb_type is None:
            return None

        child = self._valobj.CreateValueFromAddress(
            str(child_name),
            address,
            lldb_type,
        )
        return child if child is not None and child.IsValid() else None

    def _field_address(self, index):
        _ensure_cached_child_metadata_line(self, index, True)
        return _get_cached_child_address(self._valobj, index)

    def _field_type(self, index):
        _ensure_cached_child_metadata_line(self, index, True)
        child_type = _get_cached_child_type(self._valobj, index)
        return _RUNTIME_TYPE_INVALID if child_type is None else child_type

    def _child_name(self, index):
        _ensure_cached_child_metadata_line(self, index, True)
        return _get_cached_child_name(self._valobj, index)

    def update(self):
        return False


class KonanArraySyntheticProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj, _, internal_dict):
        super().__init__(valobj)
        self._target = valobj.GetTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj

    def num_children(self):
        return _children_count(self._valobj)

    def get_child_index(self, name):
        index = int(name)
        return index if (0 <= index < self.num_children()) else -1

    def get_child_at_index(self, index):
        if not (0 <= index < self.num_children()):
            return None

        address = self._field_address(index)
        if address is None:
            return None

        lldb_type = _resolve_lldb_type(self._valobj, self._field_type(index))
        if lldb_type is None:
            return None

        child = self._valobj.CreateValueFromAddress(
            str(index),
            address,
            lldb_type,
        )
        return child if child.IsValid() else None

    def update(self):
        return False

    def _field_address(self, index):
        _ensure_cached_child_metadata_line(self, index, False)
        return _get_cached_child_address(self._valobj, index)

    def _field_type(self, index):
        _ensure_cached_child_metadata_line(self, index, False)
        child_type = _get_cached_child_type(self._valobj, index)
        return _RUNTIME_TYPE_INVALID if child_type is None else child_type


def _object_field_value(object_proxy, field_name):
    field_index = object_proxy.get_child_index(field_name)
    return object_proxy.get_child_at_index(field_index)


def _synthetic_value_or_self(value):
    if value is None or not value.IsValid():
        return None
    synthetic = value.GetSyntheticValue()
    if synthetic is not None and synthetic.IsValid():
        return synthetic
    return value


def _synthetic_child_index(value, name):
    synthetic = _synthetic_value_or_self(value)
    if synthetic is None:
        return -1
    return synthetic.GetIndexOfChildWithName(name)

def _synthetic_child_at_index(value, index):
    synthetic = _synthetic_value_or_self(value)
    if synthetic is None:
        return None
    child = synthetic.GetChildAtIndex(index)
    return child if child is not None and child.IsValid() else None


class KonanListSyntheticProvider:
    def __init__(self, valobj, backing, children_count):
        self._valobj = valobj
        self._backing = backing
        self._children_count = (
            0 if children_count is None else children_count
        )

    @staticmethod
    def fromObjectProxy(valobj, object_proxy, internal_dict):
        if object_proxy is None:
            return None

        backing = None
        for field_name in ("backing", "$this_asList", "backingArray"):
            candidate = _object_field_value(object_proxy, field_name)
            if candidate is not None and candidate.IsValid() and candidate.unsigned != 0:
                backing = candidate
                break

        if backing is None:
            return None

        size = None
        size_value = _object_field_value(object_proxy, "length")
        if size_value is not None and size_value.IsValid():
            size = size_value.GetValueAsUnsigned()
        else:
            synthetic = _synthetic_value_or_self(backing)
            size = None if synthetic is None else synthetic.GetNumChildren()

        return KonanListSyntheticProvider(valobj, backing, size)

    def num_children(self):
        return self._children_count

    def get_child_index(self, name):
        child_index = _synthetic_child_index(self._backing, name)
        return child_index if 0 <= child_index < self.num_children() else -1

    def get_child_at_index(self, index):
        if not 0 <= index < self.num_children():
            return None
        return _synthetic_child_at_index(self._backing, index)

    def update(self):
        return False


class KonanSetSyntheticProvider:
    def __init__(self, valobj, keys, children_count):
        self._valobj = valobj
        self._keys = keys
        self._children_count = (
            0 if children_count is None else children_count
        )

    @staticmethod
    def fromObjectProxy(valobj, object_proxy, internal_dict):
        if object_proxy is None:
            return None

        backing = _object_field_value(object_proxy, "backing")
        if backing is None or not backing.IsValid() or backing.unsigned == 0:
            return None

        backing_object_proxy = KonanObjectSyntheticProvider(backing, internal_dict)

        keys = _object_field_value(backing_object_proxy, "keysArray")
        if keys is None or not keys.IsValid() or keys.unsigned == 0:
            return None

        size_value = _object_field_value(backing_object_proxy, "length")
        children_count = (
            size_value.GetValueAsUnsigned()
            if size_value is not None and size_value.IsValid()
            else None
        )
        if children_count is None:
            synthetic = _synthetic_value_or_self(keys)
            children_count = 0 if synthetic is None else synthetic.GetNumChildren()

        return KonanSetSyntheticProvider(
            valobj,
            keys,
            children_count,
        )

    def num_children(self):
        return self._children_count

    def get_child_index(self, name):
        child_index = _synthetic_child_index(self._keys, name)
        return child_index if 0 <= child_index < self.num_children() else -1

    def get_child_at_index(self, index):
        if not 0 <= index < self.num_children():
            return None
        return _synthetic_child_at_index(self._keys, index)

    def update(self):
        return False


class KonanMapSyntheticProvider:
    def __init__(self, valobj, keys, values, children_count):
        self._valobj = valobj
        self._keys = keys
        self._values = values
        self._children_count = (
            0 if children_count is None else children_count
        )
        self._entry_type = None

    @staticmethod
    def fromObjectProxy(valobj, object_proxy, internal_dict):
        if object_proxy is None:
            return None

        keys = _object_field_value(object_proxy, "keysArray")
        values = _object_field_value(object_proxy, "valuesArray")
        if (
            keys is None
            or not keys.IsValid()
            or keys.unsigned == 0
            or values is None
            or not values.IsValid()
            or values.unsigned == 0
        ):
            return None

        size_value = _object_field_value(object_proxy, "length")
        size = (
            size_value.GetValueAsUnsigned()
            if size_value is not None and size_value.IsValid()
            else None
        )
        return KonanMapSyntheticProvider(valobj, keys, values, size)

    def num_children(self):
        return self._children_count

    def get_child_index(self, name):
        try:
            index = int(name.strip("[]"))
        except ValueError:
            return -1
        return index if 0 <= index < self.num_children() else -1

    def get_child_at_index(self, index):
        if not 0 <= index < self.num_children():
            return None

        key = _synthetic_child_at_index(self._keys, index)
        value = _synthetic_child_at_index(self._values, index)
        if key is None or value is None or not key.IsValid() or not value.IsValid():
            return None

        if self._entry_type is None:
            self._entry_type = _get_map_entry_type(self._valobj.GetProcess())
        if self._entry_type is None or not self._entry_type.IsValid():
            return None

        target = self._valobj.GetTarget()
        pointer_size = target.GetAddressByteSize()
        addresses = [key.GetValueAsUnsigned(), value.GetValueAsUnsigned()]
        if pointer_size == 8:
            data = lldb.SBData.CreateDataFromUInt64Array(
                target.GetByteOrder(), pointer_size, addresses
            )
        elif pointer_size == 4:
            data = lldb.SBData.CreateDataFromUInt32Array(
                target.GetByteOrder(), pointer_size, addresses
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

    def type_name(self):
        valobj = self._valobj
        if valobj is None or not valobj.IsValid():
            return ""
        if valobj.GetTypeName() != "ObjHeader *":
            return ""
        if valobj.GetValueAsUnsigned() == 0:
            return ""

        cached_type_name = _get_cached_type_name(valobj)
        if cached_type_name is not None:
            return cached_type_name

        type_name = _evaluate(
            f"(char *)Konan_DebugGetTypeName({_hex(valobj.unsigned)})"
        ).summary

        type_name = "" if type_name is None else type_name.strip('"')
        _set_cached_type_name(valobj.GetProcess(), valobj.GetValueAsUnsigned(), type_name)
        return type_name

    def _get_proxy(self):
        cached_proxy = _get_cached_proxy(self._valobj)
        if cached_proxy is not None:
            return cached_proxy

        start = time.monotonic()

        proxy = _select_provider(self._valobj, self._internal_dict)

        value_str = _hex(self._valobj.unsigned)
        _bench(start, lambda: f"KonanProxyTypeProvider({value_str})")
        self._log.debug(
            "%s _proxy: %s", value_str, proxy.__class__.__name__
        )

        _set_cached_proxy(self._valobj, proxy)
        return proxy

    def get_value(self):
        return self._valobj.GetValue()

    def num_children(self):
        return self._get_proxy().num_children()

    def update(self):
        return False

    def has_children(self):
        return self._valobj is not None and self._valobj.IsValid() and self._valobj.GetValueAsUnsigned() != 0

    def get_child_index(self, name):
        proxy = self._get_proxy()
        return proxy.get_child_index(name)

    def get_child_at_index(self, index):
        proxy = self._get_proxy()
        return proxy.get_child_at_index(index)

    def __getattr__(self, item):
        return getattr(self._get_proxy(), item)

def field_type_command(_, field_address, exe_ctx, result, internal_dict):
    """
    Returns runtime type of foo.bar.baz field in the form of
    '(foo.bar.baz <TYPE_NAME>)'. If requested field could not be traced,
    then '<NO_FIELD_FOUND>' plug is used for type name.
    """
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
        if variable.GetTypeName() == "ObjHeader *":
            type_name = KonanProxyTypeProvider(variable, internal_dict).type_name()
            if len(type_name) > 0:
                desc = type_name
        else:
            type_name = variable.GetTypeName()
            if type_name is not None and len(type_name) > 0:
                desc = type_name

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
    _FACTORY["object"] = lambda x, y, z: KonanObjectSyntheticProvider(x, z)
    _FACTORY["array"] = lambda x, y, z: KonanArraySyntheticProvider(x, y, z)
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
            "--python-function konan_lldb.kotlin_map_entry_type_summary "
            '"_KotlinMapEntry" '
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
