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
import sys
import time
import logging

import lldb

_NULL = "null"
_BENCH_LOGGING = False


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

    result = frame.EvaluateExpression(expr)

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
        f": ((int)Konan_DebugIsArray({value_str})) ? 2 : 0)"
    )
    soa = _evaluate(expr).unsigned
    logging.debug("%s: %s", value_str, soa)
    _bench(start, lambda: f"is_string_or_array({value_str}) = {soa}")
    return soa


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

# Cache type info pointer to [ChildMetaInfo]
_SYNTHETIC_OBJECT_LAYOUT_CACHE = {}
_TO_STRING_DEPTH = 2
_ARRAY_TO_STRING_LIMIT = 10
_TOTAL_MEMBERS_LIMIT = 50

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
    return _render_object(lldb_val.unsigned)


def _select_provider(lldb_val, tip, internal_dict):
    start = time.monotonic()
    value_str = f"{_hex(lldb_val.unsigned)}"
    logging.debug("%s name:%s tip:%s", value_str, lldb_val.name, _hex(tip))
    soa = _is_string_or_array(lldb_val)
    logging.debug("%s soa: %s", value_str, soa)
    ret = (
        _FACTORY["string"](lldb_val, tip, internal_dict)
        if soa == 1
        else (
            _FACTORY["array"](lldb_val, tip, internal_dict)
            if soa == 2
            else _FACTORY["object"](lldb_val, tip, internal_dict)
        )
    )
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
            children_count = _evaluate(
                f"(int)Konan_DebugGetFieldCount({value_str})"
            ).signed
            self._log.debug(
                "(int)[%s].Konan_DebugGetFieldCount(%s) = %s",
                self._valobj.name,
                value_str,
                children_count,
            )
            self._children_count = children_count

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
        return _TYPE_CONVERSION[int(value_type)](
            self, self._valobj, address, str(self._field_name(index))
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
        self._children_count = 0
        super(KonanStringSyntheticProvider, self).__init__(
            valobj, True, "StringProvider", {}
        )
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


class KonanZerroSyntheticProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj):
        super().__init__(valobj)
        self._log = logging.getLogger(self.__class__.__name__)
        logging.debug(valobj.name)

    def num_children(self):
        self._log.debug("")
        return 0

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
        start = time.monotonic()
        value_str = _hex(valobj.unsigned)
        value_name = valobj.name
        self._log.debug("%s, name: %s", value_str, value_name)
        if valobj.unsigned == 0:
            self._log.debug(
                "%s, name: %s NULL syntectic %s",
                value_str,
                value_name,
                valobj.IsValid(),
            )
            _bench(start, lambda: f"KonanProxyTypeProvider({value_str})")
            self._proxy = KonanNullSyntheticProvider(valobj)
            return

        tip = _type_info(valobj)
        if not tip:
            self._log.debug(
                "%s, name: %s NULL syntectic %s",
                value_str,
                value_name,
                valobj.IsValid(),
            )
            _bench(start, lambda: f"KonanProxyTypeProvider({value_str})")
            self._proxy = KonanNotInitializedObjectSyntheticProvider(valobj)
            return
        self._log.debug("%s tip: %s", value_str, _hex(tip))
        self._proxy = _select_provider(valobj, tip, internal_dict)
        _bench(start, lambda: f"KonanProxyTypeProvider({value_str})")
        self._log.debug(
            "%s _proxy: %s", value_str, self._proxy.__class__.__name__
        )

    def __getattr__(self, item):
        return getattr(self._proxy, item)


def _get_runtime_type(variable):
    type_name = _evaluate(
        f"(char *)Konan_DebugGetTypeName({_hex(variable.unsigned)})"
    ).summary
    return "" if type_name is None else type_name.strip('"')


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
            provider = KonanProxyTypeProvider(variable, internal_dict)
            field_index = provider.get_child_index(field_name)
            variable = provider.get_child_at_index(field_index)
        else:
            break

    desc = "<NO_FIELD_FOUND>"

    if variable is not None:
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
    result.AppendMessage(f"DEBUG: {command}")
    tokens = command.split()
    target = debugger.GetSelectedTarget()
    types = _type_info_by_address(tokens[0])
    result.AppendMessage(f"DEBUG: {types}")
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
    _FACTORY["object"] = lambda x, y, z: KonanObjectSyntheticProvider(x, y, z)
    _FACTORY["array"] = lambda x, y, z: KonanArraySyntheticProvider(x, z)
    _FACTORY["string"] = lambda x, y, _: KonanStringSyntheticProvider(x)
    debugger.HandleCommand(
        (
            "type summary add "
            "--no-value "
            "--python-function konan_lldb.kotlin_object_type_summary "
            "ObjHeader *"
            "--category Kotlin"
        )
    )
    debugger.HandleCommand(
        (
            "type synthetic add "
            "--python-class konan_lldb.KonanProxyTypeProvider "
            "ObjHeader *"
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
