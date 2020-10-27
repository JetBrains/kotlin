/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#ifndef COMMON_FILES_H
#define COMMON_FILES_H

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

bool renameAtomic(const char* from, const char* to, bool replaceExisting);

#ifdef __cplusplus
}
#endif

#endif // COMMON_FILES_H
