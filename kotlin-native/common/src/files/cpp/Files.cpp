/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#include "Files.h"

#include <cstdio>

#ifdef __APPLE__

bool renameAtomic(const char* from, const char* to, bool replaceExisting) {
  return renamex_np(from, to, replaceExisting ? 0 : RENAME_EXCL) == 0;
}

#elif _WIN32

#include <windows.h>

bool renameAtomic(const char* from, const char* to, bool replaceExisting) {
    return MoveFileExA(from, to, replaceExisting ? MOVEFILE_REPLACE_EXISTING : 0) != 0;
}

#else

#include <unistd.h>
#include <sys/syscall.h>
#include <linux/fs.h>

bool renameAtomic(const char* from, const char* to, bool replaceExisting) {
  return syscall(SYS_renameat2, 0, from, 0, to, replaceExisting ? 0 : RENAME_NOREPLACE) == 0;
}


#endif