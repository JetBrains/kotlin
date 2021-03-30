/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
#include <stdlib.h>
#include <stdio.h>

extern "C" {
// Memory operations.
void* konan_calloc_impl(size_t n_elements, size_t elem_size) {
  return calloc(n_elements, elem_size);
}

void* konan_calloc_aligned_impl(size_t count, size_t size, size_t alignment) {
  // alignment is not supported by std alloc - use mimalloc
  return calloc(count, size);
}

void konan_free_impl (void* mem) {
  free(mem);
}
}

