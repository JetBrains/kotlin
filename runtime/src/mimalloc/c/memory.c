/* ----------------------------------------------------------------------------
Copyright (c) 2019, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/

/* ----------------------------------------------------------------------------
This implements a layer between the raw OS memory (VirtualAlloc/mmap/sbrk/..)
and the segment and huge object allocation by mimalloc. There may be multiple
implementations of this (one could be the identity going directly to the OS,
another could be a simple cache etc), but the current one uses large "regions".
In contrast to the rest of mimalloc, the "regions" are shared between threads and
need to be accessed using atomic operations.
We need this memory layer between the raw OS calls because of:
1. on `sbrk` like systems (like WebAssembly) we need our own memory maps in order
   to reuse memory effectively.
2. It turns out that for large objects, between 1MiB and 32MiB (?), the cost of
   an OS allocation/free is still (much) too expensive relative to the accesses in that
   object :-( (`malloc-large` tests this). This means we need a cheaper way to
   reuse memory.
3. This layer can help with a NUMA aware allocation in the future.

Possible issues:
- (2) can potentially be addressed too with a small cache per thread which is much
  simpler. Generally though that requires shrinking of huge pages, and may overuse
  memory per thread. (and is not compatible with `sbrk`).
- Since the current regions are per-process, we need atomic operations to
  claim blocks which may be contended
- In the worst case, we need to search the whole region map (16KiB for 256GiB)
  linearly. At what point will direct OS calls be faster? Is there a way to
  do this better without adding too much complexity?
-----------------------------------------------------------------------------*/
#include "mimalloc.h"
#include "mimalloc-internal.h"
#include "mimalloc-atomic.h"

#include <string.h>  // memset

// Internal raw OS interface
size_t  _mi_os_large_page_size();
bool    _mi_os_protect(void* addr, size_t size);
bool    _mi_os_unprotect(void* addr, size_t size);
bool    _mi_os_commit(void* p, size_t size, bool* is_zero, mi_stats_t* stats);
bool    _mi_os_decommit(void* p, size_t size, mi_stats_t* stats);
bool    _mi_os_reset(void* p, size_t size, mi_stats_t* stats);
bool    _mi_os_unreset(void* p, size_t size, bool* is_zero, mi_stats_t* stats);
void*   _mi_os_alloc_aligned(size_t size, size_t alignment, bool commit, bool* large, mi_os_tld_t* tld);
void    _mi_os_free_ex(void* p, size_t size, bool was_committed, mi_stats_t* stats);
void*   _mi_os_try_alloc_from_huge_reserved(size_t size, size_t try_alignment);
bool    _mi_os_is_huge_reserved(void* p);

// Constants
#if (MI_INTPTR_SIZE==8)
#define MI_HEAP_REGION_MAX_SIZE    (256 * (1ULL << 30))  // 256GiB => 16KiB for the region map
#elif (MI_INTPTR_SIZE==4)
#define MI_HEAP_REGION_MAX_SIZE    (3 * (1UL << 30))    // 3GiB => 196 bytes for the region map
#else
#error "define the maximum heap space allowed for regions on this platform"
#endif

#define MI_SEGMENT_ALIGN          MI_SEGMENT_SIZE

#define MI_REGION_MAP_BITS        (MI_INTPTR_SIZE * 8)
#define MI_REGION_SIZE            (MI_SEGMENT_SIZE * MI_REGION_MAP_BITS)
#define MI_REGION_MAX_ALLOC_SIZE  ((MI_REGION_MAP_BITS/4)*MI_SEGMENT_SIZE)  // 64MiB
#define MI_REGION_MAX             (MI_HEAP_REGION_MAX_SIZE / MI_REGION_SIZE)
#define MI_REGION_MAP_FULL        UINTPTR_MAX


typedef uintptr_t mi_region_info_t;

static inline mi_region_info_t mi_region_info_create(void* start, bool is_large, bool is_committed) {
  return ((uintptr_t)start | ((uintptr_t)(is_large?1:0) << 1) | (is_committed?1:0));
}

static inline void* mi_region_info_read(mi_region_info_t info, bool* is_large, bool* is_committed) {
  if (is_large) *is_large = ((info&0x02) != 0);
  if (is_committed) *is_committed = ((info&0x01) != 0);
  return (void*)(info & ~0x03);
}


// A region owns a chunk of REGION_SIZE (256MiB) (virtual) memory with
// a bit map with one bit per MI_SEGMENT_SIZE (4MiB) block.
typedef struct mem_region_s {
  volatile _Atomic(uintptr_t)        map;   // in-use bit per MI_SEGMENT_SIZE block
  volatile _Atomic(mi_region_info_t) info;  // start of virtual memory area, and flags
  volatile _Atomic(uintptr_t)        dirty_mask; // bit per block if the contents are not zero'd
} mem_region_t;


// The region map; 16KiB for a 256GiB HEAP_REGION_MAX
// TODO: in the future, maintain a map per NUMA node for numa aware allocation
static mem_region_t regions[MI_REGION_MAX];

static volatile _Atomic(uintptr_t) regions_count; // = 0;        // allocated regions


/* ----------------------------------------------------------------------------
Utility functions
-----------------------------------------------------------------------------*/

// Blocks (of 4MiB) needed for the given size.
static size_t mi_region_block_count(size_t size) {
  mi_assert_internal(size <= MI_REGION_MAX_ALLOC_SIZE);
  return (size + MI_SEGMENT_SIZE - 1) / MI_SEGMENT_SIZE;
}

// The bit mask for a given number of blocks at a specified bit index.
static uintptr_t mi_region_block_mask(size_t blocks, size_t bitidx) {
  mi_assert_internal(blocks + bitidx <= MI_REGION_MAP_BITS);
  return ((((uintptr_t)1 << blocks) - 1) << bitidx);
}

// Return a rounded commit/reset size such that we don't fragment large OS pages into small ones.
static size_t mi_good_commit_size(size_t size) {
  if (size > (SIZE_MAX - _mi_os_large_page_size())) return size;
  return _mi_align_up(size, _mi_os_large_page_size());
}

// Return if a pointer points into a region reserved by us.
bool mi_is_in_heap_region(const void* p) mi_attr_noexcept {
  if (p==NULL) return false;
  size_t count = mi_atomic_read_relaxed(&regions_count);
  for (size_t i = 0; i < count; i++) {
    uint8_t* start = (uint8_t*)mi_region_info_read( mi_atomic_read_relaxed(&regions[i].info), NULL, NULL);
    if (start != NULL && (uint8_t*)p >= start && (uint8_t*)p < start + MI_REGION_SIZE) return true;
  }
  return false;
}


/* ----------------------------------------------------------------------------
Commit from a region
-----------------------------------------------------------------------------*/

// Commit the `blocks` in `region` at `idx` and `bitidx` of a given `size`.
// Returns `false` on an error (OOM); `true` otherwise. `p` and `id` are only written
// if the blocks were successfully claimed so ensure they are initialized to NULL/SIZE_MAX before the call.
// (not being able to claim is not considered an error so check for `p != NULL` afterwards).
static bool mi_region_commit_blocks(mem_region_t* region, size_t idx, size_t bitidx, size_t blocks, 
                                    size_t size, bool* commit, bool* allow_large, bool* is_zero, void** p, size_t* id, mi_os_tld_t* tld)
{
  size_t mask = mi_region_block_mask(blocks,bitidx);
  mi_assert_internal(mask != 0);
  mi_assert_internal((mask & mi_atomic_read_relaxed(&region->map)) == mask);
  mi_assert_internal(&regions[idx] == region);

  // ensure the region is reserved
  mi_region_info_t info = mi_atomic_read(&region->info);
  if (info == 0) 
  {
    bool region_commit = mi_option_is_enabled(mi_option_eager_region_commit);
    bool region_large  = *allow_large;
    void* start = NULL;
    if (region_large) {
      start = _mi_os_try_alloc_from_huge_reserved(MI_REGION_SIZE, MI_SEGMENT_ALIGN);
      if (start != NULL) { region_commit = true; }
    }
    if (start == NULL) {
      start = _mi_os_alloc_aligned(MI_REGION_SIZE, MI_SEGMENT_ALIGN, region_commit, &region_large, tld);
    }
    mi_assert_internal(!(region_large && !*allow_large));

    if (start == NULL) {
      // failure to allocate from the OS! unclaim the blocks and fail
      size_t map;
      do {
        map = mi_atomic_read_relaxed(&region->map);
      } while (!mi_atomic_cas_weak(&region->map, map & ~mask, map));
      return false;
    }

    // set the newly allocated region
    info = mi_region_info_create(start,region_large,region_commit);
    if (mi_atomic_cas_strong(&region->info, info, 0)) {
      // update the region count
      mi_atomic_increment(&regions_count);
    }
    else {
      // failed, another thread allocated just before us!
      // we assign it to a later slot instead (up to 4 tries).
      for(size_t i = 1; i <= 4 && idx + i < MI_REGION_MAX; i++) {
        if (mi_atomic_cas_strong(&regions[idx+i].info, info, 0)) {
          mi_atomic_increment(&regions_count);
          start = NULL;
          break;
        }
      }
      if (start != NULL) {
        // free it if we didn't succeed to save it to some other region
        _mi_os_free_ex(start, MI_REGION_SIZE, region_commit, tld->stats);
      }
      // and continue with the memory at our index
      info = mi_atomic_read(&region->info);
    }
  }
  mi_assert_internal(info == mi_atomic_read(&region->info));
  mi_assert_internal(info != 0);

  // Commit the blocks to memory
  bool region_is_committed = false;
  bool region_is_large = false;
  void* start = mi_region_info_read(info,&region_is_large,&region_is_committed);  
  mi_assert_internal(!(region_is_large && !*allow_large));
  mi_assert_internal(start!=NULL);

  // set dirty bits
  uintptr_t m;
  do {
    m = mi_atomic_read(&region->dirty_mask);
  } while (!mi_atomic_cas_weak(&region->dirty_mask, m | mask, m));
  *is_zero = ((m & mask) == 0); // no dirty bit set in our claimed range?

  void* blocks_start = (uint8_t*)start + (bitidx * MI_SEGMENT_SIZE);
  if (*commit && !region_is_committed) {
    // ensure commit 
    bool commit_zero = false;
    _mi_os_commit(blocks_start, mi_good_commit_size(size), &commit_zero, tld->stats);  // only commit needed size (unless using large OS pages)
    if (commit_zero) *is_zero = true;
  }
  else if (!*commit && region_is_committed) {
    // but even when no commit is requested, we might have committed anyway (in a huge OS page for example)
    *commit = true;
  }

  // and return the allocation  
  mi_assert_internal(blocks_start != NULL);
  *allow_large = region_is_large;
  *p  = blocks_start;
  *id = (idx*MI_REGION_MAP_BITS) + bitidx;
  return true;
}

// Use bit scan forward to quickly find the first zero bit if it is available
#if defined(_MSC_VER)
#define MI_HAVE_BITSCAN
#include <intrin.h>
static inline size_t mi_bsf(uintptr_t x) {
  if (x==0) return 8*MI_INTPTR_SIZE;
  DWORD idx;
  #if (MI_INTPTR_SIZE==8)
  _BitScanForward64(&idx, x);
  #else
  _BitScanForward(&idx, x);
  #endif
  return idx;
}
static inline size_t mi_bsr(uintptr_t x) {
  if (x==0) return 8*MI_INTPTR_SIZE;
  DWORD idx;
  #if (MI_INTPTR_SIZE==8)
  _BitScanReverse64(&idx, x);
  #else
  _BitScanReverse(&idx, x);
  #endif
  return idx;
}
#elif defined(__GNUC__) || defined(__clang__)
#define MI_HAVE_BITSCAN
static inline size_t mi_bsf(uintptr_t x) {
  return (x==0 ? 8*MI_INTPTR_SIZE : __builtin_ctzl(x));
}
static inline size_t mi_bsr(uintptr_t x) {
  return (x==0 ? 8*MI_INTPTR_SIZE : (8*MI_INTPTR_SIZE - 1) - __builtin_clzl(x));
}
#endif

// Allocate `blocks` in a `region` at `idx` of a given `size`.
// Returns `false` on an error (OOM); `true` otherwise. `p` and `id` are only written
// if the blocks were successfully claimed so ensure they are initialized to NULL/SIZE_MAX before the call.
// (not being able to claim is not considered an error so check for `p != NULL` afterwards).
static bool mi_region_alloc_blocks(mem_region_t* region, size_t idx, size_t blocks, size_t size, 
                                   bool* commit, bool* allow_large, bool* is_zero, void** p, size_t* id, mi_os_tld_t* tld)
{
  mi_assert_internal(p != NULL && id != NULL);
  mi_assert_internal(blocks < MI_REGION_MAP_BITS);

  const uintptr_t mask = mi_region_block_mask(blocks, 0);
  const size_t bitidx_max = MI_REGION_MAP_BITS - blocks;
  uintptr_t map = mi_atomic_read(&region->map);
  if (map==MI_REGION_MAP_FULL) return true;

  #ifdef MI_HAVE_BITSCAN
  size_t bitidx = mi_bsf(~map);    // quickly find the first zero bit if possible
  #else
  size_t bitidx = 0;               // otherwise start at 0
  #endif
  uintptr_t m = (mask << bitidx);     // invariant: m == mask shifted by bitidx

  // scan linearly for a free range of zero bits
  while(bitidx <= bitidx_max) {
    if ((map & m) == 0) {  // are the mask bits free at bitidx?
      mi_assert_internal((m >> bitidx) == mask); // no overflow?
      uintptr_t newmap = map | m;
      mi_assert_internal((newmap^map) >> bitidx == mask);
      if (!mi_atomic_cas_weak(&region->map, newmap, map)) {  // TODO: use strong cas here?
        // no success, another thread claimed concurrently.. keep going
        map = mi_atomic_read(&region->map);
        continue;
      }
      else {
        // success, we claimed the bits
        // now commit the block memory -- this can still fail
        return mi_region_commit_blocks(region, idx, bitidx, blocks, 
                                       size, commit, allow_large, is_zero, p, id, tld);
      }
    }
    else {
      // on to the next bit range
      #ifdef MI_HAVE_BITSCAN
      size_t shift = (blocks == 1 ? 1 : mi_bsr(map & m) - bitidx + 1);
      mi_assert_internal(shift > 0 && shift <= blocks);
      #else
      size_t shift = 1;
      #endif
      bitidx += shift;
      m <<= shift;
    }
  }
  // no error, but also no bits found
  return true;
}

// Try to allocate `blocks` in a `region` at `idx` of a given `size`. Does a quick check before trying to claim.
// Returns `false` on an error (OOM); `true` otherwise. `p` and `id` are only written
// if the blocks were successfully claimed so ensure they are initialized to NULL/0 before the call.
// (not being able to claim is not considered an error so check for `p != NULL` afterwards).
static bool mi_region_try_alloc_blocks(size_t idx, size_t blocks, size_t size, 
                                       bool* commit, bool* allow_large, bool* is_zero, 
                                       void** p, size_t* id, mi_os_tld_t* tld)
{
  // check if there are available blocks in the region..
  mi_assert_internal(idx < MI_REGION_MAX);
  mem_region_t* region = &regions[idx];
  uintptr_t m = mi_atomic_read_relaxed(&region->map);
  if (m != MI_REGION_MAP_FULL) {  // some bits are zero    
    bool ok = (*commit || *allow_large); // committing or allow-large is always ok
    if (!ok) {
      // otherwise skip incompatible regions if possible. 
      // this is not guaranteed due to multiple threads allocating at the same time but
      // that's ok. In secure mode, large is never allowed for any thread, so that works out; 
      // otherwise we might just not be able to reset/decommit individual pages sometimes.
      mi_region_info_t info = mi_atomic_read_relaxed(&region->info);
      bool is_large;
      bool is_committed;
      void* start = mi_region_info_read(info,&is_large,&is_committed);
      ok = (start == NULL || (*commit || !is_committed) || (*allow_large || !is_large)); // Todo: test with one bitmap operation?
    }
    if (ok) {
      return mi_region_alloc_blocks(region, idx, blocks, size, commit, allow_large, is_zero, p, id, tld);
    }
  }
  return true;  // no error, but no success either
}

/* ----------------------------------------------------------------------------
 Allocation
-----------------------------------------------------------------------------*/

// Allocate `size` memory aligned at `alignment`. Return non NULL on success, with a given memory `id`.
// (`id` is abstract, but `id = idx*MI_REGION_MAP_BITS + bitidx`)
void* _mi_mem_alloc_aligned(size_t size, size_t alignment, bool* commit, bool* large, bool* is_zero, 
                            size_t* id, mi_os_tld_t* tld)
{
  mi_assert_internal(id != NULL && tld != NULL);
  mi_assert_internal(size > 0);
  *id = SIZE_MAX;
  *is_zero = false;
  bool default_large = false;
  if (large==NULL) large = &default_large;  // ensure `large != NULL`  

  // use direct OS allocation for huge blocks or alignment (with `id = SIZE_MAX`)
  if (size > MI_REGION_MAX_ALLOC_SIZE || alignment > MI_SEGMENT_ALIGN) {
    *is_zero = true;
    return _mi_os_alloc_aligned(mi_good_commit_size(size), alignment, *commit, large, tld);  // round up size
  }

  // always round size to OS page size multiple (so commit/decommit go over the entire range)
  // TODO: use large OS page size here?
  size = _mi_align_up(size, _mi_os_page_size());

  // calculate the number of needed blocks
  size_t blocks = mi_region_block_count(size);
  mi_assert_internal(blocks > 0 && blocks <= 8*MI_INTPTR_SIZE);

  // find a range of free blocks
  void* p = NULL;
  size_t count = mi_atomic_read(&regions_count);
  size_t idx = tld->region_idx; // start at 0 to reuse low addresses? Or, use tld->region_idx to reduce contention?
  for (size_t visited = 0; visited < count; visited++, idx++) {
    if (idx >= count) idx = 0;  // wrap around
    if (!mi_region_try_alloc_blocks(idx, blocks, size, commit, large, is_zero, &p, id, tld)) return NULL; // error
    if (p != NULL) break;
  }

  if (p == NULL) {
    // no free range in existing regions -- try to extend beyond the count.. but at most 8 regions
    for (idx = count; idx < mi_atomic_read_relaxed(&regions_count) + 8 && idx < MI_REGION_MAX; idx++) {
      if (!mi_region_try_alloc_blocks(idx, blocks, size, commit, large, is_zero, &p, id, tld)) return NULL; // error
      if (p != NULL) break;
    }
  }

  if (p == NULL) {
    // we could not find a place to allocate, fall back to the os directly
    _mi_warning_message("unable to allocate from region: size %zu\n", size);
    *is_zero = true;
    p = _mi_os_alloc_aligned(size, alignment, commit, large, tld);
  }
  else {
    tld->region_idx = idx;  // next start of search? currently not used as we use first-fit
  }

  mi_assert_internal( p == NULL || (uintptr_t)p % alignment == 0);
  return p;
}



/* ----------------------------------------------------------------------------
Free
-----------------------------------------------------------------------------*/

// Free previously allocated memory with a given id.
void _mi_mem_free(void* p, size_t size, size_t id, mi_stats_t* stats) {
  mi_assert_internal(size > 0 && stats != NULL);
  if (p==NULL) return;
  if (size==0) return;
  if (id == SIZE_MAX) {
   // was a direct OS allocation, pass through
    _mi_os_free(p, size, stats);
  }
  else {
    // allocated in a region
    mi_assert_internal(size <= MI_REGION_MAX_ALLOC_SIZE); if (size > MI_REGION_MAX_ALLOC_SIZE) return;
    // we can align the size up to page size (as we allocate that way too)
    // this ensures we fully commit/decommit/reset
    size = _mi_align_up(size, _mi_os_page_size());
    size_t idx = (id / MI_REGION_MAP_BITS);
    size_t bitidx = (id % MI_REGION_MAP_BITS);
    size_t blocks = mi_region_block_count(size);
    size_t mask = mi_region_block_mask(blocks, bitidx);
    mi_assert_internal(idx < MI_REGION_MAX); if (idx >= MI_REGION_MAX) return; // or `abort`?
    mem_region_t* region = &regions[idx];
    mi_assert_internal((mi_atomic_read_relaxed(&region->map) & mask) == mask ); // claimed?
    mi_region_info_t info = mi_atomic_read(&region->info);
    bool is_large;
    bool is_eager_committed;
    void* start = mi_region_info_read(info,&is_large,&is_eager_committed);
    mi_assert_internal(start != NULL);
    void* blocks_start = (uint8_t*)start + (bitidx * MI_SEGMENT_SIZE);
    mi_assert_internal(blocks_start == p); // not a pointer in our area?
    mi_assert_internal(bitidx + blocks <= MI_REGION_MAP_BITS);
    if (blocks_start != p || bitidx + blocks > MI_REGION_MAP_BITS) return; // or `abort`?

    // decommit (or reset) the blocks to reduce the working set.
    // TODO: implement delayed decommit/reset as these calls are too expensive
    // if the memory is reused soon.
    // reset: 10x slowdown on malloc-large, decommit: 17x slowdown on malloc-large
    if (!is_large) {
      if (mi_option_is_enabled(mi_option_segment_reset)) {
        if (!is_eager_committed &&  // cannot reset large pages
          (mi_option_is_enabled(mi_option_eager_commit) ||  // cannot reset halfway committed segments, use `option_page_reset` instead
            mi_option_is_enabled(mi_option_reset_decommits))) // but we can decommit halfway committed segments
        {
          _mi_os_reset(p, size, stats);
          //_mi_os_decommit(p, size, stats);  // todo: and clear dirty bits?
        }
      }
    }    
    if (!is_eager_committed) {
      // adjust commit statistics as we commit again when re-using the same slot
      _mi_stat_decrease(&stats->committed, mi_good_commit_size(size));
    }

    // TODO: should we free empty regions? currently only done _mi_mem_collect.
    // this frees up virtual address space which might be useful on 32-bit systems?

    // and unclaim
    uintptr_t map;
    uintptr_t newmap;
    do {
      map = mi_atomic_read_relaxed(&region->map);
      newmap = map & ~mask;
    } while (!mi_atomic_cas_weak(&region->map, newmap, map));
  }
}


/* ----------------------------------------------------------------------------
  collection
-----------------------------------------------------------------------------*/
void _mi_mem_collect(mi_stats_t* stats) {
  // free every region that has no segments in use.
  for (size_t i = 0; i < regions_count; i++) {
    mem_region_t* region = &regions[i];
    if (mi_atomic_read_relaxed(&region->map) == 0) {
      // if no segments used, try to claim the whole region
      uintptr_t m;
      do {
        m = mi_atomic_read_relaxed(&region->map);
      } while(m == 0 && !mi_atomic_cas_weak(&region->map, ~((uintptr_t)0), 0 ));
      if (m == 0) {
        // on success, free the whole region (unless it was huge reserved)
        bool is_eager_committed;
        void* start = mi_region_info_read(mi_atomic_read(&region->info), NULL, &is_eager_committed);
        if (start != NULL && !_mi_os_is_huge_reserved(start)) {
          _mi_os_free_ex(start, MI_REGION_SIZE, is_eager_committed, stats);
        }
        // and release
        mi_atomic_write(&region->info,0);
        mi_atomic_write(&region->map,0);
      }
    }
  }
}

/* ----------------------------------------------------------------------------
  Other
-----------------------------------------------------------------------------*/

bool _mi_mem_commit(void* p, size_t size, bool* is_zero, mi_stats_t* stats) {
  return _mi_os_commit(p, size, is_zero, stats);
}

bool _mi_mem_decommit(void* p, size_t size, mi_stats_t* stats) {
  return _mi_os_decommit(p, size, stats);
}

bool _mi_mem_reset(void* p, size_t size, mi_stats_t* stats) {
  return _mi_os_reset(p, size, stats);
}

bool _mi_mem_unreset(void* p, size_t size, bool* is_zero, mi_stats_t* stats) {
  return _mi_os_unreset(p, size, is_zero, stats);
}

bool _mi_mem_protect(void* p, size_t size) {
  return _mi_os_protect(p, size);
}

bool _mi_mem_unprotect(void* p, size_t size) {
  return _mi_os_unprotect(p, size);
}
