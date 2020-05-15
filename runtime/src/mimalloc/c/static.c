/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/
// Copyright 2019-2020 JetBrains s.r.o.

#if !KONAN_MI_MALLOC
#define _DEFAULT_SOURCE

#include "mimalloc.h"
#include "mimalloc-internal.h"

// For a static override we create a single object file
// containing the whole library. If it is linked first
// it will override all the standard library allocation
// functions (on Unix's).
#include "stats.c"
#include "os.c"
#include "memory.c"
#include "segment.c"
#include "page.c"
#include "heap.c"
#include "alloc.c"
#include "alloc-aligned.c"
#include "alloc-posix.c"
#include "init.c"
#include "options.c"
#endif