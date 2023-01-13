/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include "SourceInfo.h"
#include "backtrace.h"

#include <cstring>

extern "C" int Kotlin_getSourceInfo_libbacktrace(void* addr, SourceInfo *result, int result_size) {
    if (result_size == 0) return 0;
    auto ignore_error = [](void*, const char*, int){};
    static auto state = backtrace_create_state(nullptr, 1, ignore_error, nullptr);
    if (!state) return 0;
    struct callback_arg_t {
        SourceInfo *result;
        int result_ptr;
        int result_size;
        int total_count;
    } callback_arg;
    callback_arg.result = result;
    callback_arg.result_ptr = 0;
    callback_arg.result_size = result_size;
    callback_arg.total_count = 0;
    auto process_line = [](void *data, uintptr_t pc, const char *filename, int lineno, int column, const char *function, int is_nodebug) -> int {
        auto &callback_arg = *static_cast<callback_arg_t*>(data);
        // Non-inlined frame would be last one, it's better to have it, then intermediate ones
        if (callback_arg.result_ptr == callback_arg.result_size) {
            callback_arg.result_ptr--;
        }
        auto &info = callback_arg.result[callback_arg.result_ptr];
        info.setFilename(filename);
        info.lineNumber = lineno;
        info.column = column;
        info.nodebug = is_nodebug;
        callback_arg.result_ptr++;
        callback_arg.total_count++;
        // Let's stop at least at some point
        // Probably, this can happen only if debug info is corrupted
        return callback_arg.total_count > callback_arg.result_size * 10;
    };
    backtrace_pcinfo(state, reinterpret_cast<uintptr_t>(addr), process_line, ignore_error, &callback_arg);
    return callback_arg.total_count;
}