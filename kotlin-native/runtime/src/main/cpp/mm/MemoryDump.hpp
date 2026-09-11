/*
 * Copyright 2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Utils.hpp"

namespace kotlin::mm {

/**
 * RAII guard ensuring only one dump runs at a time.
 *
 * Uses non-blocking try-lock semantics: if another dump is already in
 * progress, acquisition fails and `operator bool()` returns false.
 */
class DumpGuard : private Pinned {
public:
    DumpGuard() noexcept;
    ~DumpGuard();
    explicit operator bool() const noexcept { return acquired_; }

private:
    bool acquired_;
};

/**
 * Dumps memory into the given POSIX file in Kotlin/Native Dump file format, and
 * returns success flag. Must be called during STW.
 *
 * The dump includes raw contents of Kotlin objects in binary form, together
 * with corresponding type layouts. The dump can be combined with additional
 * metadata emitted by the compiler and converted to the "hprof" format by an
 * external tool.
 */
bool DumpMemory(int fd) noexcept;

/**
 * Like [DumpMemory], with optional omitted primitive-array payloads and gzip
 * wrapping. Must be called during STW.
 *
 * When `omitPayloads` is true, primitive array contents are omitted (count is
 * preserved). Object arrays and native-pointer arrays are still written in full
 * so the heap graph can be reconstructed.
 *
 * When `gzip` is true, the dump is written as a gzip member. `kdumputil` detects
 * the gzip magic and decompresses before parsing.
 */
bool DumpMemory(int fd, bool omitPayloads, bool gzip) noexcept;

} // namespace kotlin::mm
