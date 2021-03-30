/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/
// Copyright 2019-2020 JetBrains s.r.o.

#pragma once
#ifndef MIMALLOC_INTERNAL_H
#define MIMALLOC_INTERNAL_H

#include "mimalloc-types.h"

#if (MI_DEBUG>0)
#define mi_trace_message(...)  _mi_trace_message(__VA_ARGS__)
#else
#define mi_trace_message(...)
#endif

#define MI_CACHE_LINE          64
#if defined(_MSC_VER)
#pragma warning(disable:4127)   // suppress constant conditional warning (due to MI_SECURE paths)
#define mi_decl_noinline        __declspec(noinline)
#define mi_decl_thread          __declspec(thread)
#define mi_decl_cache_align     __declspec(align(MI_CACHE_LINE))
#elif (defined(__GNUC__) && (__GNUC__>=3))  // includes clang and icc
#define mi_decl_noinline        __attribute__((noinline))
#define mi_decl_thread          __thread
#define mi_decl_cache_align     __attribute__((aligned(MI_CACHE_LINE)))
#else
#define mi_decl_noinline
#define mi_decl_thread          __thread        // hope for the best :-)
#define mi_decl_cache_align
#endif


// "options.c"
void       _mi_fputs(mi_output_fun* out, void* arg, const char* prefix, const char* message);
void       _mi_fprintf(mi_output_fun* out, void* arg, const char* fmt, ...);
void       _mi_warning_message(const char* fmt, ...);
void       _mi_verbose_message(const char* fmt, ...);
void       _mi_trace_message(const char* fmt, ...);
void       _mi_options_init(void);
void       _mi_error_message(int err, const char* fmt, ...);

// random.c
void       _mi_random_init(mi_random_ctx_t* ctx);
void       _mi_random_split(mi_random_ctx_t* ctx, mi_random_ctx_t* new_ctx);
uintptr_t  _mi_random_next(mi_random_ctx_t* ctx);
uintptr_t  _mi_heap_random_next(mi_heap_t* heap);
uintptr_t  _os_random_weak(uintptr_t extra_seed);
static inline uintptr_t _mi_random_shuffle(uintptr_t x);

// init.c
extern mi_stats_t       _mi_stats_main;
extern const mi_page_t  _mi_page_empty;
bool       _mi_is_main_thread(void);
bool       _mi_preloading();  // true while the C runtime is not ready

// os.c
size_t     _mi_os_page_size(void);
void       _mi_os_init(void);                                      // called from process init
void*      _mi_os_alloc(size_t size, mi_stats_t* stats);           // to allocate thread local data
void       _mi_os_free(void* p, size_t size, mi_stats_t* stats);   // to free thread local data
size_t     _mi_os_good_alloc_size(size_t size);

// memory.c
void*      _mi_mem_alloc_aligned(size_t size, size_t alignment, bool* commit, bool* large, bool* is_zero, size_t* id, mi_os_tld_t* tld);
void       _mi_mem_free(void* p, size_t size, size_t id, bool fully_committed, bool any_reset, mi_os_tld_t* tld);

bool       _mi_mem_reset(void* p, size_t size, mi_os_tld_t* tld);
bool       _mi_mem_unreset(void* p, size_t size, bool* is_zero, mi_os_tld_t* tld);
bool       _mi_mem_commit(void* p, size_t size, bool* is_zero, mi_os_tld_t* tld);
bool       _mi_mem_protect(void* addr, size_t size);
bool       _mi_mem_unprotect(void* addr, size_t size);

void        _mi_mem_collect(mi_os_tld_t* tld);

// "segment.c"
mi_page_t* _mi_segment_page_alloc(mi_heap_t* heap, size_t block_wsize, mi_segments_tld_t* tld, mi_os_tld_t* os_tld);
void       _mi_segment_page_free(mi_page_t* page, bool force, mi_segments_tld_t* tld);
void       _mi_segment_page_abandon(mi_page_t* page, mi_segments_tld_t* tld);
uint8_t*   _mi_segment_page_start(const mi_segment_t* segment, const mi_page_t* page, size_t block_size, size_t* page_size, size_t* pre_size); // page start for any page
void       _mi_segment_huge_page_free(mi_segment_t* segment, mi_page_t* page, mi_block_t* block);

void       _mi_segment_thread_collect(mi_segments_tld_t* tld);
void       _mi_abandoned_reclaim_all(mi_heap_t* heap, mi_segments_tld_t* tld);
void       _mi_abandoned_await_readers(void);



// "page.c"
void*      _mi_malloc_generic(mi_heap_t* heap, size_t size)  mi_attr_noexcept mi_attr_malloc;

void       _mi_page_retire(mi_page_t* page);                                  // free the page if there are no other pages with many free blocks
void       _mi_page_unfull(mi_page_t* page);
void       _mi_page_free(mi_page_t* page, mi_page_queue_t* pq, bool force);   // free the page
void       _mi_page_abandon(mi_page_t* page, mi_page_queue_t* pq);            // abandon the page, to be picked up by another thread...
void       _mi_heap_delayed_free(mi_heap_t* heap);
void       _mi_heap_collect_retired(mi_heap_t* heap, bool force);

void       _mi_page_use_delayed_free(mi_page_t* page, mi_delayed_t delay, bool override_never);
size_t     _mi_page_queue_append(mi_heap_t* heap, mi_page_queue_t* pq, mi_page_queue_t* append);
void       _mi_deferred_free(mi_heap_t* heap, bool force);

void       _mi_page_free_collect(mi_page_t* page,bool force);
void       _mi_page_reclaim(mi_heap_t* heap, mi_page_t* page);   // callback from segments

size_t     _mi_bin_size(uint8_t bin);           // for stats
uint8_t    _mi_bin(size_t size);                // for stats
uint8_t    _mi_bsr(uintptr_t x);                // bit-scan-right, used on BSD in "os.c"

// "heap.c"
void       _mi_heap_destroy_pages(mi_heap_t* heap);
void       _mi_heap_collect_abandon(mi_heap_t* heap);
void       _mi_heap_set_default_direct(mi_heap_t* heap);

// "stats.c"
void       _mi_stats_done(mi_stats_t* stats);

mi_msecs_t  _mi_clock_now(void);
mi_msecs_t  _mi_clock_end(mi_msecs_t start);
mi_msecs_t  _mi_clock_start(void);

// "alloc.c"
void*       _mi_page_malloc(mi_heap_t* heap, mi_page_t* page, size_t size) mi_attr_noexcept;  // called from `_mi_malloc_generic`
void*       _mi_heap_malloc_zero(mi_heap_t* heap, size_t size, bool zero);
void*       _mi_heap_realloc_zero(mi_heap_t* heap, void* p, size_t newsize, bool zero);
mi_block_t* _mi_page_ptr_unalign(const mi_segment_t* segment, const mi_page_t* page, const void* p);
bool        _mi_free_delayed_block(mi_block_t* block);
void        _mi_block_zero_init(const mi_page_t* page, void* p, size_t size);

#if MI_DEBUG>1
bool        _mi_page_is_valid(mi_page_t* page);
#endif


// ------------------------------------------------------
// Branches
// ------------------------------------------------------

#if defined(__GNUC__) || defined(__clang__)
#define mi_unlikely(x)     __builtin_expect((x),0)
#define mi_likely(x)       __builtin_expect((x),1)
#else
#define mi_unlikely(x)     (x)
#define mi_likely(x)       (x)
#endif

#ifndef __has_builtin
#define __has_builtin(x)  0
#endif


/* -----------------------------------------------------------
  Error codes passed to `_mi_fatal_error`
  All are recoverable but EFAULT is a serious error and aborts by default in secure mode.
  For portability define undefined error codes using common Unix codes:
  <https://www-numi.fnal.gov/offline_software/srt_public_context/WebDocs/Errors/unix_system_errors.html>
----------------------------------------------------------- */
#include <errno.h>
#ifndef EAGAIN         // double free
#define EAGAIN (11)
#endif
#ifndef ENOMEM         // out of memory
#define ENOMEM (12)
#endif
#ifndef EFAULT         // corrupted free-list or meta-data
#define EFAULT (14)
#endif
#ifndef EINVAL         // trying to free an invalid pointer
#define EINVAL (22)
#endif
#ifndef EOVERFLOW      // count*size overflow
#define EOVERFLOW (75)
#endif


/* -----------------------------------------------------------
  Inlined definitions
----------------------------------------------------------- */
#define UNUSED(x)     (void)(x)
#if (MI_DEBUG>0)
#define UNUSED_RELEASE(x)
#else
#define UNUSED_RELEASE(x)  UNUSED(x)
#endif

#define MI_INIT4(x)   x(),x(),x(),x()
#define MI_INIT8(x)   MI_INIT4(x),MI_INIT4(x)
#define MI_INIT16(x)  MI_INIT8(x),MI_INIT8(x)
#define MI_INIT32(x)  MI_INIT16(x),MI_INIT16(x)
#define MI_INIT64(x)  MI_INIT32(x),MI_INIT32(x)
#define MI_INIT128(x) MI_INIT64(x),MI_INIT64(x)
#define MI_INIT256(x) MI_INIT128(x),MI_INIT128(x)


// Is `x` a power of two? (0 is considered a power of two)
static inline bool _mi_is_power_of_two(uintptr_t x) {
  return ((x & (x - 1)) == 0);
}

// Align upwards
static inline uintptr_t _mi_align_up(uintptr_t sz, size_t alignment) {
  mi_assert_internal(alignment != 0);
  uintptr_t mask = alignment - 1;
  if ((alignment & mask) == 0) {  // power of two?
    return ((sz + mask) & ~mask);
  }
  else {
    return (((sz + mask)/alignment)*alignment);
  }
}

// Divide upwards: `s <= _mi_divide_up(s,d)*d < s+d`.
static inline uintptr_t _mi_divide_up(uintptr_t size, size_t divider) {
  mi_assert_internal(divider != 0);
  return (divider == 0 ? size : ((size + divider - 1) / divider));
}

// Is memory zero initialized?
static inline bool mi_mem_is_zero(void* p, size_t size) {
  for (size_t i = 0; i < size; i++) {
    if (((uint8_t*)p)[i] != 0) return false;
  }
  return true;
}

// Align a byte size to a size in _machine words_,
// i.e. byte size == `wsize*sizeof(void*)`.
static inline size_t _mi_wsize_from_size(size_t size) {
  mi_assert_internal(size <= SIZE_MAX - sizeof(uintptr_t));
  return (size + sizeof(uintptr_t) - 1) / sizeof(uintptr_t);
}

// Does malloc satisfy the alignment constraints already?
static inline bool mi_malloc_satisfies_alignment(size_t alignment, size_t size) {
  return (alignment == sizeof(void*) || (alignment == MI_MAX_ALIGN_SIZE && size > (MI_MAX_ALIGN_SIZE/2)));
}

// Overflow detecting multiply
#if __has_builtin(__builtin_umul_overflow) || __GNUC__ >= 5
#include <limits.h>      // UINT_MAX, ULONG_MAX
#if defined(_CLOCK_T)    // for Illumos
#undef _CLOCK_T
#endif

static inline bool mi_mul_overflow(size_t count, size_t size, size_t* total) {
// Changed order for armv7 (ULONG_MAX == UINT_MAX, but size_t = unsigned long)
#if defined(__MACH__) && KONAN_MI_MALLOC
  #if (SIZE_MAX == ULONG_MAX)
    return __builtin_umull_overflow(count, size, total);
  #elif (SIZE_MAX == UINT_MAX)
    return __builtin_umul_overflow(count, size, total);
  #else
    return __builtin_umulll_overflow(count, size, total);
  #endif
#else // KONAN_MI_MALLOC
  #if (SIZE_MAX == UINT_MAX)
    return __builtin_umul_overflow(count, size, total);
  #elif (SIZE_MAX == ULONG_MAX)
    return __builtin_umull_overflow(count, size, total);
  #else
    return __builtin_umulll_overflow(count, size, total);
  #endif
#endif // KONAN_MI_MALLOC
}
#else /* __builtin_umul_overflow is unavailable */
static inline bool mi_mul_overflow(size_t count, size_t size, size_t* total) {
  #define MI_MUL_NO_OVERFLOW ((size_t)1 << (4*sizeof(size_t)))  // sqrt(SIZE_MAX)
  *total = count * size;
  return ((size >= MI_MUL_NO_OVERFLOW || count >= MI_MUL_NO_OVERFLOW)
    && size > 0 && (SIZE_MAX / size) < count);
}
#endif

// Safe multiply `count*size` into `total`; return `true` on overflow.
static inline bool mi_count_size_overflow(size_t count, size_t size, size_t* total) {
  if (count==1) {  // quick check for the case where count is one (common for C++ allocators)
    *total = size;
    return false;
  }
  else if (mi_unlikely(mi_mul_overflow(count, size, total))) {
    _mi_error_message(EOVERFLOW, "allocation request is too large (%zu * %zu bytes)\n", count, size);
    *total = SIZE_MAX;
    return true;
  }
  else return false;
}


/* ----------------------------------------------------------------------------------------
The thread local default heap: `_mi_get_default_heap` returns the thread local heap.
On most platforms (Windows, Linux, FreeBSD, NetBSD, etc), this just returns a
__thread local variable (`_mi_heap_default`). With the initial-exec TLS model this ensures
that the storage will always be available (allocated on the thread stacks).
On some platforms though we cannot use that when overriding `malloc` since the underlying
TLS implementation (or the loader) will call itself `malloc` on a first access and recurse.
We try to circumvent this in an efficient way:
- macOSX : we use an unused TLS slot from the OS allocated slots (MI_TLS_SLOT). On OSX, the
           loader itself calls `malloc` even before the modules are initialized.
- OpenBSD: we use an unused slot from the pthread block (MI_TLS_PTHREAD_SLOT_OFS).
- DragonFly: not yet working.
------------------------------------------------------------------------------------------- */

extern const mi_heap_t _mi_heap_empty;  // read-only empty heap, initial value of the thread local default heap
extern bool _mi_process_is_initialized;
mi_heap_t*  _mi_heap_main_get(void);    // statically allocated main backing heap

#if defined(MI_MALLOC_OVERRIDE)
#if defined(__MACH__) // OSX
#define MI_TLS_SLOT               89  // seems unused?
// other possible unused ones are 9, 29, __PTK_FRAMEWORK_JAVASCRIPTCORE_KEY4 (94), __PTK_FRAMEWORK_GC_KEY9 (112) and __PTK_FRAMEWORK_OLDGC_KEY9 (89)
// see <https://github.com/rweichler/substrate/blob/master/include/pthread_machdep.h>
#elif defined(__OpenBSD__)
// use end bytes of a name; goes wrong if anyone uses names > 23 characters (ptrhread specifies 16)
// see <https://github.com/openbsd/src/blob/master/lib/libc/include/thread_private.h#L371>
#define MI_TLS_PTHREAD_SLOT_OFS   (6*sizeof(int) + 4*sizeof(void*) + 24)
#elif defined(__DragonFly__)
#warning "mimalloc is not working correctly on DragonFly yet."
#define MI_TLS_PTHREAD_SLOT_OFS   (4 + 1*sizeof(void*))  // offset `uniqueid` (also used by gdb?) <https://github.com/DragonFlyBSD/DragonFlyBSD/blob/master/lib/libthread_xu/thread/thr_private.h#L458>
#endif
#endif

#if defined(MI_TLS_SLOT)
static inline void* mi_tls_slot(size_t slot) mi_attr_noexcept;   // forward declaration
#elif defined(MI_TLS_PTHREAD_SLOT_OFS)
#include <pthread.h>
static inline mi_heap_t** mi_tls_pthread_heap_slot(void) {
  pthread_t self = pthread_self();
  #if defined(__DragonFly__)
  if (self==NULL) {
    static mi_heap_t* pheap_main = _mi_heap_main_get();
    return &pheap_main;
  }
  #endif
  return (mi_heap_t**)((uint8_t*)self + MI_TLS_PTHREAD_SLOT_OFS);
}
#elif defined(MI_TLS_PTHREAD)
#include <pthread.h>
extern pthread_key_t _mi_heap_default_key;
#else
extern mi_decl_thread mi_heap_t* _mi_heap_default;  // default heap to allocate from
#endif

static inline mi_heap_t* mi_get_default_heap(void) {
#if defined(MI_TLS_SLOT)
  mi_heap_t* heap = (mi_heap_t*)mi_tls_slot(MI_TLS_SLOT);
  return (mi_unlikely(heap == NULL) ? (mi_heap_t*)&_mi_heap_empty : heap);
#elif defined(MI_TLS_PTHREAD_SLOT_OFS)
  mi_heap_t* heap = *mi_tls_pthread_heap_slot();
  return (mi_unlikely(heap == NULL) ? (mi_heap_t*)&_mi_heap_empty : heap);
#elif defined(MI_TLS_PTHREAD)
  mi_heap_t* heap = (mi_unlikely(_mi_heap_default_key == (pthread_key_t)(-1)) ? _mi_heap_main_get() : (mi_heap_t*)pthread_getspecific(_mi_heap_default_key));
  return (mi_unlikely(heap == NULL) ? (mi_heap_t*)&_mi_heap_empty : heap);
#else
  #if defined(MI_TLS_RECURSE_GUARD)
  if (mi_unlikely(!_mi_process_is_initialized)) return _mi_heap_main_get();
  #endif
  return _mi_heap_default;
#endif
}

static inline bool mi_heap_is_default(const mi_heap_t* heap) {
  return (heap == mi_get_default_heap());
}

static inline bool mi_heap_is_backing(const mi_heap_t* heap) {
  return (heap->tld->heap_backing == heap);
}

static inline bool mi_heap_is_initialized(mi_heap_t* heap) {
  mi_assert_internal(heap != NULL);
  return (heap != &_mi_heap_empty);
}

static inline uintptr_t _mi_ptr_cookie(const void* p) {
  extern mi_heap_t _mi_heap_main;
  mi_assert_internal(_mi_heap_main.cookie != 0);
  return ((uintptr_t)p ^ _mi_heap_main.cookie);
}

/* -----------------------------------------------------------
  Pages
----------------------------------------------------------- */

static inline mi_page_t* _mi_heap_get_free_small_page(mi_heap_t* heap, size_t size) {
  mi_assert_internal(size <= (MI_SMALL_SIZE_MAX + MI_PADDING_SIZE));
  const size_t idx = _mi_wsize_from_size(size);
  mi_assert_internal(idx < MI_PAGES_DIRECT);
  return heap->pages_free_direct[idx];
}

// Get the page belonging to a certain size class
static inline mi_page_t* _mi_get_free_small_page(size_t size) {
  return _mi_heap_get_free_small_page(mi_get_default_heap(), size);
}

// Segment that contains the pointer
static inline mi_segment_t* _mi_ptr_segment(const void* p) {
  // mi_assert_internal(p != NULL);
  return (mi_segment_t*)((uintptr_t)p & ~MI_SEGMENT_MASK);
}

// Segment belonging to a page
static inline mi_segment_t* _mi_page_segment(const mi_page_t* page) {
  mi_segment_t* segment = _mi_ptr_segment(page);
  mi_assert_internal(segment == NULL || page == &segment->pages[page->segment_idx]);
  return segment;
}

// used internally
static inline uintptr_t _mi_segment_page_idx_of(const mi_segment_t* segment, const void* p) {
  // if (segment->page_size > MI_SEGMENT_SIZE) return &segment->pages[0];  // huge pages
  ptrdiff_t diff = (uint8_t*)p - (uint8_t*)segment;
  mi_assert_internal(diff >= 0 && (size_t)diff < MI_SEGMENT_SIZE);
  uintptr_t idx = (uintptr_t)diff >> segment->page_shift;
  mi_assert_internal(idx < segment->capacity);
  mi_assert_internal(segment->page_kind <= MI_PAGE_MEDIUM || idx == 0);
  return idx;
}

// Get the page containing the pointer
static inline mi_page_t* _mi_segment_page_of(const mi_segment_t* segment, const void* p) {
  uintptr_t idx = _mi_segment_page_idx_of(segment, p);
  return &((mi_segment_t*)segment)->pages[idx];
}

// Quick page start for initialized pages
static inline uint8_t* _mi_page_start(const mi_segment_t* segment, const mi_page_t* page, size_t* page_size) {
  const size_t bsize = page->xblock_size;
  mi_assert_internal(bsize > 0 && (bsize%sizeof(void*)) == 0);
  return _mi_segment_page_start(segment, page, bsize, page_size, NULL);
}

// Get the page containing the pointer
static inline mi_page_t* _mi_ptr_page(void* p) {
  return _mi_segment_page_of(_mi_ptr_segment(p), p);
}

// Get the block size of a page (special cased for huge objects)
static inline size_t mi_page_block_size(const mi_page_t* page) {
  const size_t bsize = page->xblock_size;
  mi_assert_internal(bsize > 0);
  if (mi_likely(bsize < MI_HUGE_BLOCK_SIZE)) {
    return bsize;
  }
  else {
    size_t psize;
    _mi_segment_page_start(_mi_page_segment(page), page, bsize, &psize, NULL);
    return psize;
  }
}

// Get the usable block size of a page without fixed padding.
// This may still include internal padding due to alignment and rounding up size classes.
static inline size_t mi_page_usable_block_size(const mi_page_t* page) {
  return mi_page_block_size(page) - MI_PADDING_SIZE;
}


// Thread free access
static inline mi_block_t* mi_page_thread_free(const mi_page_t* page) {
  return (mi_block_t*)(mi_atomic_load_relaxed(&((mi_page_t*)page)->xthread_free) & ~3);
}

static inline mi_delayed_t mi_page_thread_free_flag(const mi_page_t* page) {
  return (mi_delayed_t)(mi_atomic_load_relaxed(&((mi_page_t*)page)->xthread_free) & 3);
}

// Heap access
static inline mi_heap_t* mi_page_heap(const mi_page_t* page) {
  return (mi_heap_t*)(mi_atomic_load_relaxed(&((mi_page_t*)page)->xheap));
}

static inline void mi_page_set_heap(mi_page_t* page, mi_heap_t* heap) {
  mi_assert_internal(mi_page_thread_free_flag(page) != MI_DELAYED_FREEING);
  mi_atomic_store_release(&page->xheap,(uintptr_t)heap);
}

// Thread free flag helpers
static inline mi_block_t* mi_tf_block(mi_thread_free_t tf) {
  return (mi_block_t*)(tf & ~0x03);
}
static inline mi_delayed_t mi_tf_delayed(mi_thread_free_t tf) {
  return (mi_delayed_t)(tf & 0x03);
}
static inline mi_thread_free_t mi_tf_make(mi_block_t* block, mi_delayed_t delayed) {
  return (mi_thread_free_t)((uintptr_t)block | (uintptr_t)delayed);
}
static inline mi_thread_free_t mi_tf_set_delayed(mi_thread_free_t tf, mi_delayed_t delayed) {
  return mi_tf_make(mi_tf_block(tf),delayed);
}
static inline mi_thread_free_t mi_tf_set_block(mi_thread_free_t tf, mi_block_t* block) {
  return mi_tf_make(block, mi_tf_delayed(tf));
}

// are all blocks in a page freed?
// note: needs up-to-date used count, (as the `xthread_free` list may not be empty). see `_mi_page_collect_free`.
static inline bool mi_page_all_free(const mi_page_t* page) {
  mi_assert_internal(page != NULL);
  return (page->used == 0);
}

// are there any available blocks?
static inline bool mi_page_has_any_available(const mi_page_t* page) {
  mi_assert_internal(page != NULL && page->reserved > 0);
  return (page->used < page->reserved || (mi_page_thread_free(page) != NULL));
}

// are there immediately available blocks, i.e. blocks available on the free list.
static inline bool mi_page_immediate_available(const mi_page_t* page) {
  mi_assert_internal(page != NULL);
  return (page->free != NULL);
}

// is more than 7/8th of a page in use?
static inline bool mi_page_mostly_used(const mi_page_t* page) {
  if (page==NULL) return true;
  uint16_t frac = page->reserved / 8U;
  return (page->reserved - page->used <= frac);
}

static inline mi_page_queue_t* mi_page_queue(const mi_heap_t* heap, size_t size) {
  return &((mi_heap_t*)heap)->pages[_mi_bin(size)];
}



//-----------------------------------------------------------
// Page flags
//-----------------------------------------------------------
static inline bool mi_page_is_in_full(const mi_page_t* page) {
  return page->flags.x.in_full;
}

static inline void mi_page_set_in_full(mi_page_t* page, bool in_full) {
  page->flags.x.in_full = in_full;
}

static inline bool mi_page_has_aligned(const mi_page_t* page) {
  return page->flags.x.has_aligned;
}

static inline void mi_page_set_has_aligned(mi_page_t* page, bool has_aligned) {
  page->flags.x.has_aligned = has_aligned;
}


/* -------------------------------------------------------------------
Encoding/Decoding the free list next pointers

This is to protect against buffer overflow exploits where the
free list is mutated. Many hardened allocators xor the next pointer `p`
with a secret key `k1`, as `p^k1`. This prevents overwriting with known
values but might be still too weak: if the attacker can guess
the pointer `p` this  can reveal `k1` (since `p^k1^p == k1`).
Moreover, if multiple blocks can be read as well, the attacker can
xor both as `(p1^k1) ^ (p2^k1) == p1^p2` which may reveal a lot
about the pointers (and subsequently `k1`).

Instead mimalloc uses an extra key `k2` and encodes as `((p^k2)<<<k1)+k1`.
Since these operations are not associative, the above approaches do not
work so well any more even if the `p` can be guesstimated. For example,
for the read case we can subtract two entries to discard the `+k1` term,
but that leads to `((p1^k2)<<<k1) - ((p2^k2)<<<k1)` at best.
We include the left-rotation since xor and addition are otherwise linear
in the lowest bit. Finally, both keys are unique per page which reduces
the re-use of keys by a large factor.

We also pass a separate `null` value to be used as `NULL` or otherwise
`(k2<<<k1)+k1` would appear (too) often as a sentinel value.
------------------------------------------------------------------- */

static inline bool mi_is_in_same_segment(const void* p, const void* q) {
  return (_mi_ptr_segment(p) == _mi_ptr_segment(q));
}

static inline bool mi_is_in_same_page(const void* p, const void* q) {
  mi_segment_t* segmentp = _mi_ptr_segment(p);
  mi_segment_t* segmentq = _mi_ptr_segment(q);
  if (segmentp != segmentq) return false;
  uintptr_t idxp = _mi_segment_page_idx_of(segmentp, p);
  uintptr_t idxq = _mi_segment_page_idx_of(segmentq, q);
  return (idxp == idxq);
}

static inline uintptr_t mi_rotl(uintptr_t x, uintptr_t shift) {
  shift %= MI_INTPTR_BITS;
  return (shift==0 ? x : ((x << shift) | (x >> (MI_INTPTR_BITS - shift))));
}
static inline uintptr_t mi_rotr(uintptr_t x, uintptr_t shift) {
  shift %= MI_INTPTR_BITS;
  return (shift==0 ? x : ((x >> shift) | (x << (MI_INTPTR_BITS - shift))));
}

static inline void* mi_ptr_decode(const void* null, const mi_encoded_t x, const uintptr_t* keys) {
  void* p = (void*)(mi_rotr(x - keys[0], keys[0]) ^ keys[1]);
  return (mi_unlikely(p==null) ? NULL : p);
}

static inline mi_encoded_t mi_ptr_encode(const void* null, const void* p, const uintptr_t* keys) {
  uintptr_t x = (uintptr_t)(mi_unlikely(p==NULL) ? null : p);
  return mi_rotl(x ^ keys[1], keys[0]) + keys[0];
}

static inline mi_block_t* mi_block_nextx( const void* null, const mi_block_t* block, const uintptr_t* keys ) {
  #ifdef MI_ENCODE_FREELIST
  return (mi_block_t*)mi_ptr_decode(null, block->next, keys);
  #else
  UNUSED(keys); UNUSED(null);
  return (mi_block_t*)block->next;
  #endif
}

static inline void mi_block_set_nextx(const void* null, mi_block_t* block, const mi_block_t* next, const uintptr_t* keys) {
  #ifdef MI_ENCODE_FREELIST
  block->next = mi_ptr_encode(null, next, keys);
  #else
  UNUSED(keys); UNUSED(null);
  block->next = (mi_encoded_t)next;
  #endif
}

static inline mi_block_t* mi_block_next(const mi_page_t* page, const mi_block_t* block) {
  #ifdef MI_ENCODE_FREELIST
  mi_block_t* next = mi_block_nextx(page,block,page->keys);
  // check for free list corruption: is `next` at least in the same page?
  // TODO: check if `next` is `page->block_size` aligned?
  if (mi_unlikely(next!=NULL && !mi_is_in_same_page(block, next))) {
    _mi_error_message(EFAULT, "corrupted free list entry of size %zub at %p: value 0x%zx\n", mi_page_block_size(page), block, (uintptr_t)next);
    next = NULL;
  }
  return next;
  #else
  UNUSED(page);
  return mi_block_nextx(page,block,NULL);
  #endif
}

static inline void mi_block_set_next(const mi_page_t* page, mi_block_t* block, const mi_block_t* next) {
  #ifdef MI_ENCODE_FREELIST
  mi_block_set_nextx(page,block,next, page->keys);
  #else
  UNUSED(page);
  mi_block_set_nextx(page,block,next,NULL);
  #endif
}

// -------------------------------------------------------------------
// Fast "random" shuffle
// -------------------------------------------------------------------

static inline uintptr_t _mi_random_shuffle(uintptr_t x) {
  if (x==0) { x = 17; }   // ensure we don't get stuck in generating zeros
#if (MI_INTPTR_SIZE==8)
  // by Sebastiano Vigna, see: <http://xoshiro.di.unimi.it/splitmix64.c>
  x ^= x >> 30;
  x *= 0xbf58476d1ce4e5b9UL;
  x ^= x >> 27;
  x *= 0x94d049bb133111ebUL;
  x ^= x >> 31;
#elif (MI_INTPTR_SIZE==4)
  // by Chris Wellons, see: <https://nullprogram.com/blog/2018/07/31/>
  x ^= x >> 16;
  x *= 0x7feb352dUL;
  x ^= x >> 15;
  x *= 0x846ca68bUL;
  x ^= x >> 16;
#endif
  return x;
}

// -------------------------------------------------------------------
// Optimize numa node access for the common case (= one node)
// -------------------------------------------------------------------

int    _mi_os_numa_node_get(mi_os_tld_t* tld);
size_t _mi_os_numa_node_count_get(void);

extern size_t _mi_numa_node_count;
static inline int _mi_os_numa_node(mi_os_tld_t* tld) {
  if (mi_likely(_mi_numa_node_count == 1)) return 0;
  else return _mi_os_numa_node_get(tld);
}
static inline size_t _mi_os_numa_node_count(void) {
  if (mi_likely(_mi_numa_node_count>0)) return _mi_numa_node_count;
  else return _mi_os_numa_node_count_get();
}


// -------------------------------------------------------------------
// Getting the thread id should be performant as it is called in the
// fast path of `_mi_free` and we specialize for various platforms.
// -------------------------------------------------------------------
#if defined(_WIN32)
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
static inline uintptr_t _mi_thread_id(void) mi_attr_noexcept {
  // Windows: works on Intel and ARM in both 32- and 64-bit
  return (uintptr_t)NtCurrentTeb();
}

#elif defined(__GNUC__) && \
      (defined(__x86_64__) || defined(__i386__) || defined(__arm__) || defined(__aarch64__))

#if KONAN_MI_MALLOC
  #include <pthread.h>
  pthread_t pthread_self(void);
#endif // KONAN_MI_MALLOC

// TLS register on x86 is in the FS or GS register, see: https://akkadia.org/drepper/tls.pdf
static inline void* mi_tls_slot(size_t slot) mi_attr_noexcept {
  void* res;
  const size_t ofs = (slot*sizeof(void*));
#if defined(__i386__)
  __asm__("movl %%gs:%1, %0" : "=r" (res) : "m" (*((void**)ofs)) : );  // 32-bit always uses GS
#elif defined(__MACH__) && defined(__x86_64__)
  __asm__("movq %%gs:%1, %0" : "=r" (res) : "m" (*((void**)ofs)) : );  // x86_64 macOSX uses GS
#elif defined(__x86_64__)
  __asm__("movq %%fs:%1, %0" : "=r" (res) : "m" (*((void**)ofs)) : );  // x86_64 Linux, BSD uses FS
#elif defined(__arm__)
  void** tcb; UNUSED(ofs);
  __asm__ volatile ("mrc p15, 0, %0, c13, c0, 3\nbic %0, %0, #3" : "=r" (tcb));
  res = tcb[slot];
#elif defined(__aarch64__)
  void** tcb; UNUSED(ofs);
  __asm__ volatile ("mrs %0, tpidr_el0" : "=r" (tcb));
  res = tcb[slot];
#endif
  return res;
}

// setting is only used on macOSX for now
static inline void mi_tls_slot_set(size_t slot, void* value) mi_attr_noexcept {
  const size_t ofs = (slot*sizeof(void*));
#if defined(__i386__)
  __asm__("movl %1,%%gs:%0" : "=m" (*((void**)ofs)) : "rn" (value) : );  // 32-bit always uses GS
#elif defined(__MACH__) && defined(__x86_64__)
  __asm__("movq %1,%%gs:%0" : "=m" (*((void**)ofs)) : "rn" (value) : );  // x86_64 macOSX uses GS
#elif defined(__x86_64__)
  __asm__("movq %1,%%fs:%1" : "=m" (*((void**)ofs)) : "rn" (value) : );  // x86_64 Linux, BSD uses FS
#elif defined(__arm__)
  void** tcb; UNUSED(ofs);
  __asm__ volatile ("mrc p15, 0, %0, c13, c0, 3\nbic %0, %0, #3" : "=r" (tcb));
  tcb[slot] = value;
#elif defined(__aarch64__)
  void** tcb; UNUSED(ofs);
  __asm__ volatile ("mrs %0, tpidr_el0" : "=r" (tcb));
  tcb[slot] = value;
#endif
}

static inline uintptr_t _mi_thread_id(void) mi_attr_noexcept {
  #if defined(__MACH__) && KONAN_MI_MALLOC
    #include <TargetConditionals.h>
    #if TARGET_OS_EMBEDDED // iOS/tvOS/watchOS devices.
      return pthread_mach_thread_np(pthread_self());
    #else
      // in all our targets, slot 0 is the pointer to the thread control block
      return (uintptr_t)mi_tls_slot(0);
    #endif
  #else // KONAN_MI_MALLOC
    // in all our targets, slot 0 is the pointer to the thread control block
    return (uintptr_t)mi_tls_slot(0);
  #endif // KONAN_MI_MALLOC
}
#else
// otherwise use standard C
static inline uintptr_t _mi_thread_id(void) mi_attr_noexcept {
  return (uintptr_t)&_mi_heap_default;
}
#endif


#endif
