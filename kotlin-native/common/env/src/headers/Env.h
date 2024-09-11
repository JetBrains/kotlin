/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#ifndef COMMON_ENV_H
#define COMMON_ENV_H

#ifdef __cplusplus
extern "C" {
#endif

void setEnv(const char* name, const char* value);

#ifdef __cplusplus
}
#endif

#endif // COMMON_ENV_H