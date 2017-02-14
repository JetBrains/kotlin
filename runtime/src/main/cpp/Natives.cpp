#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

// Any.kt
KBoolean Kotlin_Any_equals(KConstRef thiz, KConstRef other) {
  return thiz == other;
}

KInt Kotlin_Any_hashCode(KConstRef thiz) {
  // Here we will use different mechanism for stable hashcode, using meta-objects
  // if moving collector will be used.
  return reinterpret_cast<uintptr_t>(thiz);
}

OBJ_GETTER0(Kotlin_getCurrentStackTrace) {
  RETURN_RESULT_OF0(GetCurrentStackTrace);
}

// TODO: consider handling it with compiler magic instead.
OBJ_GETTER0(Kotlin_konan_internal_undefined) {
  RETURN_OBJ(nullptr);
}

void* Kotlin_interop_malloc(KLong size, KInt align) {
  if (size > SIZE_MAX) {
    return nullptr;
  }

  void* result = malloc(size);
  if ((reinterpret_cast<uintptr_t>(result) & (align - 1)) != 0) {
    // Unaligned!
    RuntimeAssert(false, "unsupported alignment");
  }

  return result;
}

void Kotlin_interop_free(void* ptr) {
  free(ptr);
}

}  // extern "C"
