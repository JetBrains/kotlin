/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"LICENSE" at the root of this distribution.
-----------------------------------------------------------------------------*/

#include "mimalloc.h"
#include "mimalloc-internal.h"

#if defined(MI_MALLOC_OVERRIDE)

#if !defined(__APPLE__)
#error "this file should only be included on macOS"
#endif

/* ------------------------------------------------------
   Override system malloc on macOS
   This is done through the malloc zone interface.
------------------------------------------------------ */

#include <AvailabilityMacros.h>
#include <malloc/malloc.h>
#include <string.h>  // memset

#if defined(MAC_OS_X_VERSION_10_6) && \
    MAC_OS_X_VERSION_MAX_ALLOWED >= MAC_OS_X_VERSION_10_6
// only available from OSX 10.6
extern malloc_zone_t* malloc_default_purgeable_zone(void) __attribute__((weak_import));
#endif


/* ------------------------------------------------------
   malloc zone members
------------------------------------------------------ */

static size_t zone_size(malloc_zone_t* zone, const void* p) {
  return 0; // as we cannot guarantee that `p` comes from us, just return 0
}

static void* zone_malloc(malloc_zone_t* zone, size_t size) {
  return mi_malloc(size);
}

static void* zone_calloc(malloc_zone_t* zone, size_t count, size_t size) {
  return mi_calloc(count, size);
}

static void* zone_valloc(malloc_zone_t* zone, size_t size) {
  return mi_malloc_aligned(size, _mi_os_page_size());
}

static void zone_free(malloc_zone_t* zone, void* p) {
  return mi_free(p);
}

static void* zone_realloc(malloc_zone_t* zone, void* p, size_t newsize) {
  return mi_realloc(p, newsize);
}

static void* zone_memalign(malloc_zone_t* zone, size_t alignment, size_t size) {
  return mi_malloc_aligned(size,alignment);
}

static void zone_destroy(malloc_zone_t* zone) {
  // todo: ignore for now?
}

static unsigned zone_batch_malloc(malloc_zone_t* zone, size_t size, void** ps, unsigned count) {
  size_t i;
  for (i = 0; i < count; i++) {
    ps[i] = zone_malloc(zone, size);
    if (ps[i] == NULL) break;
  }
  return i;
}

static void zone_batch_free(malloc_zone_t* zone, void** ps, unsigned count) {
  for(size_t i = 0; i < count; i++) {
    zone_free(zone, ps[i]);
    ps[i] = NULL;
  }
}

static size_t zone_pressure_relief(malloc_zone_t* zone, size_t size) {
  mi_collect(false);
  return 0;
}

static void zone_free_definite_size(malloc_zone_t* zone, void* p, size_t size) {
  zone_free(zone,p);
}


/* ------------------------------------------------------
   Introspection members
------------------------------------------------------ */

static kern_return_t intro_enumerator(task_t task, void* p,
                            unsigned type_mask, vm_address_t zone_address,
                            memory_reader_t reader,
                            vm_range_recorder_t recorder)
{
  // todo: enumerate all memory
  return KERN_SUCCESS;
}

static size_t intro_good_size(malloc_zone_t* zone, size_t size) {
  return mi_good_size(size);
}

static boolean_t intro_check(malloc_zone_t* zone) {
  return true;
}

static void intro_print(malloc_zone_t* zone, boolean_t verbose) {
  mi_stats_print(NULL);
}

static void intro_log(malloc_zone_t* zone, void* p) {
  // todo?
}

static void intro_force_lock(malloc_zone_t* zone) {
  // todo?
}

static void intro_force_unlock(malloc_zone_t* zone) {
  // todo?
}

static void intro_statistics(malloc_zone_t* zone, malloc_statistics_t* stats) {
  // todo...
  stats->blocks_in_use = 0;
  stats->size_in_use = 0;
  stats->max_size_in_use = 0;
  stats->size_allocated = 0;
}

static boolean_t intro_zone_locked(malloc_zone_t* zone) {
  return false;
}


/* ------------------------------------------------------
  At process start, override the default allocator
------------------------------------------------------ */

static malloc_zone_t* mi_get_default_zone()
{
  // The first returned zone is the real default
  malloc_zone_t** zones = NULL;
  unsigned count = 0;
  kern_return_t ret = malloc_get_all_zones(0, NULL, (vm_address_t**)&zones, &count);
  if (ret == KERN_SUCCESS && count > 0) {
    return zones[0];
  }
  else {
    // fallback
    return malloc_default_zone();
  }
}


static void __attribute__((constructor)) _mi_macos_override_malloc()
{
  static malloc_introspection_t intro;
  memset(&intro, 0, sizeof(intro));

  intro.enumerator = &intro_enumerator;
  intro.good_size = &intro_good_size;
  intro.check = &intro_check;
  intro.print = &intro_print;
  intro.log = &intro_log;
  intro.force_lock = &intro_force_lock;
  intro.force_unlock = &intro_force_unlock;

  static malloc_zone_t zone;
  memset(&zone, 0, sizeof(zone));

  zone.version = 4;
  zone.zone_name = "mimalloc";
  zone.size = &zone_size;
  zone.introspect = &intro;
  zone.malloc = &zone_malloc;
  zone.calloc = &zone_calloc;
  zone.valloc = &zone_valloc;
  zone.free = &zone_free;
  zone.realloc = &zone_realloc;
  zone.destroy = &zone_destroy;
  zone.batch_malloc = &zone_batch_malloc;
  zone.batch_free = &zone_batch_free;

  malloc_zone_t* purgeable_zone = NULL;

#if defined(MAC_OS_X_VERSION_10_6) && \
    MAC_OS_X_VERSION_MAX_ALLOWED >= MAC_OS_X_VERSION_10_6
  // switch to version 9 on OSX 10.6 to support memalign.
  zone.version = 9;
  zone.memalign = &zone_memalign;
  zone.free_definite_size = &zone_free_definite_size;
  zone.pressure_relief = &zone_pressure_relief;
  intro.zone_locked = &intro_zone_locked;

  // force the purgeable zone to exist to avoid strange bugs
  if (malloc_default_purgeable_zone) {
    purgeable_zone = malloc_default_purgeable_zone();
  }
#endif

  // Register our zone
  malloc_zone_register(&zone);

  // Unregister the default zone, this makes our zone the new default
  // as that was the last registered.
  malloc_zone_t *default_zone = mi_get_default_zone();
  malloc_zone_unregister(default_zone);

  // Reregister the default zone so free and realloc in that zone keep working.
  malloc_zone_register(default_zone);

  // Unregister, and re-register the purgeable_zone to avoid bugs if it occurs
  // earlier than the default zone.
  if (purgeable_zone != NULL) {
    malloc_zone_unregister(purgeable_zone);
    malloc_zone_register(purgeable_zone);
  }
}

#endif // MI_MALLOC_OVERRIDE
