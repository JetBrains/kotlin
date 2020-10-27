/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef COMMON_BASE64_H
#define COMMON_BASE64_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int EncodeBase64(
    const void* input, uint32_t inputLen, void* output, uint32_t outputLen);

int DecodeBase64(
    const char* input, uint32_t inputLen, void* output, uint32_t* outputLen);

#ifdef __cplusplus
}
#endif

#endif // COMMON_BASE64_H
