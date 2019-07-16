/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#include <stdlib.h>
#include <stdio.h>

extern "C" {
void* mi_calloc(size_t, size_t);
void mi_free(void*);
void* konan_calloc_impl(size_t n_elements, size_t elem_size) {
 return mi_calloc(n_elements, elem_size);
}
void konan_free_impl (void* mem) {
  mi_free(mem);
}
}  // extern "C"
