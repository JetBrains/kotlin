/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"LICENSE" at the root of this distribution.
-----------------------------------------------------------------------------*/
#pragma once
#ifndef MIMALLOC_INTERNAL_H
#define MIMALLOC_INTERNAL_H

#include "mimalloc-types.h"

#if defined(MI_MALLOC_OVERRIDE) && (defined(__APPLE__) || defined(__OpenBSD__))
#define MI_TLS_RECURSE_GUARD
#endif

#if (MI_DEBUG>0)
#define mi_trace_message(...)  _mi_trace_message(__VA_ARGS__)
#else
#define mi_trace_message(...)  
#endif

#if defined(_MSC_VER)
#define mi_decl_noinline   __declspec(noinline)
#define mi_attr_noreturn 
#elif defined(__GNUC__) || defined(__clang__)
#define mi_decl_noinline   __attribute__((noinline))
#define mi_attr_noreturn   __attribute__((noreturn))
#else
#define mi_decl_noinline
#define mi_attr_noreturn   
#endif


// "options.c"
void       _mi_fputs(mi_output_fun* out, const char* prefix, const char* message);
void       _mi_fprintf(mi_output_fun* out, const char* fmt, ...);
void       _mi_error_message(const char* fmt, ...);
void       _mi_warning_message(const char* fmt, ...);
void       _mi_verbose_message(const char* fmt, ...);
void       _mi_trace_message(const char* fmt, ...);
void       _mi_options_init(void);
void       _mi_fatal_error(const char* fmt, ...) mi_attr_noreturn;

// "init.c"
extern mi_stats_t       _mi_stats_main;
extern const mi_page_t  _mi_page_empty;
bool       _mi_is_main_thread(void);
uintptr_t  _mi_random_shuffle(uintptr_t x);
uintptr_t  _mi_random_init(uintptr_t seed /* can be zero */);
bool       _mi_preloading();  // true while the C runtime is not ready

// os.c
size_t     _mi_os_page_size(void);
void       _mi_os_init(void);                                      // called from process init
void*      _mi_os_alloc(size_t size, mi_stats_t* stats);           // to allocate thread local data
void       _mi_os_free(void* p, size_t size, mi_stats_t* stats);   // to free thread local data
size_t     _mi_os_good_alloc_size(size_t size);

// memory.c
void*      _mi_mem_alloc_aligned(size_t size, size_t alignment, bool* commit, bool* large, bool* is_zero, size_t* id, mi_os_tld_t* tld);
void       _mi_mem_free(void* p, size_t size, size_t id, mi_stats_t* stats);

bool       _mi_mem_reset(void* p, size_t size, mi_stats_t* stats);
bool       _mi_mem_unreset(void* p, size_t size, bool* is_zero, mi_stats_t* stats);
bool       _mi_mem_commit(void* p, size_t size, bool* is_zero, mi_stats_t* stats);
bool       _mi_mem_protect(void* addr, size_t size);
bool       _mi_mem_unprotect(void* addr, size_t size);

void        _mi_mem_collect(mi_stats_t* stats);

// "segment.c"
mi_page_t* _mi_segment_page_alloc(size_t block_wsize, mi_segments_tld_t* tld, mi_os_tld_t* os_tld);
void       _mi_segment_page_free(mi_page_t* page, bool force, mi_segments_tld_t* tld);
void       _mi_segment_page_abandon(mi_page_t* page, mi_segments_tld_t* tld);
bool       _mi_segment_try_reclaim_abandoned( mi_heap_t* heap, bool try_all, mi_segments_tld_t* tld);
void       _mi_segment_thread_collect(mi_segments_tld_t* tld);
uint8_t*   _mi_segment_page_start(const mi_segment_t* segment, const mi_page_t* page, size_t block_size, size_t* page_size); // page start for any page

// "page.c"
void*      _mi_malloc_generic(mi_heap_t* heap, size_t size)  mi_attr_noexcept mi_attr_malloc;

void       _mi_page_retire(mi_page_t* page);                                  // free the page if there are no other pages with many free blocks
void       _mi_page_unfull(mi_page_t* page);
void       _mi_page_free(mi_page_t* page, mi_page_queue_t* pq, bool force);   // free the page
void       _mi_page_abandon(mi_page_t* page, mi_page_queue_t* pq);            // abandon the page, to be picked up by another thread...
void       _mi_heap_delayed_free(mi_heap_t* heap);

void       _mi_page_use_delayed_free(mi_page_t* page, mi_delayed_t delay);
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
uintptr_t  _mi_heap_random(mi_heap_t* heap);
void       _mi_heap_set_default_direct(mi_heap_t* heap);

// "stats.c"
void       _mi_stats_done(mi_stats_t* stats);
double     _mi_clock_end(double start);
double     _mi_clock_start(void);

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


// Overflow detecting multiply
#define MI_MUL_NO_OVERFLOW ((size_t)1 << (4*sizeof(size_t)))  // sqrt(SIZE_MAX)
static inline bool mi_mul_overflow(size_t count, size_t size, size_t* total) {
#if __has_builtin(__builtin_umul_overflow) || __GNUC__ >= 5
#include <limits.h>   // UINT_MAX, ULONG_MAX
#if (SIZE_MAX == UINT_MAX)
  return __builtin_umul_overflow(count, size, total);
#elif (SIZE_MAX == ULONG_MAX)
  return __builtin_umull_overflow(count, size, total);
#else
  return __builtin_umulll_overflow(count, size, total);
#endif
#else /* __builtin_umul_overflow is unavailable */
  *total = count * size;
  return ((size >= MI_MUL_NO_OVERFLOW || count >= MI_MUL_NO_OVERFLOW)
          && size > 0 && (SIZE_MAX / size) < count);
#endif
}

// Is `x` a power of two? (0 is considered a power of two)
static inline bool _mi_is_power_of_two(uintptr_t x) {
  return ((x & (x - 1)) == 0);
}

// Align upwards
static inline uintptr_t _mi_align_up(uintptr_t sz, size_t alignment) {
  uintptr_t mask = alignment - 1;
  if ((alignment & mask) == 0) {  // power of two?
    return ((sz + mask) & ~mask);
  }
  else {
    return (((sz + mask)/alignment)*alignment);
  }
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


/* -----------------------------------------------------------
  The thread local default heap
----------------------------------------------------------- */

extern const mi_heap_t _mi_heap_empty;  // read-only empty heap, initial value of the thread local default heap
extern mi_heap_t _mi_heap_main;         // statically allocated main backing heap
extern bool _mi_process_is_initialized;

extern mi_decl_thread mi_heap_t* _mi_heap_default;  // default heap to allocate from

static inline mi_heap_t* mi_get_default_heap(void) {
#ifdef MI_TLS_RECURSE_GUARD
  // on some platforms, like macOS, the dynamic loader calls `malloc`
  // to initialize thread local data. To avoid recursion, we need to avoid
  // accessing the thread local `_mi_default_heap` until our module is loaded
  // and use the statically allocated main heap until that time.
  // TODO: patch ourselves dynamically to avoid this check every time?
  if (!_mi_process_is_initialized) return &_mi_heap_main;
#endif
  return _mi_heap_default;
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
  return ((uintptr_t)p ^ _mi_heap_main.cookie);
}

/* -----------------------------------------------------------
  Pages
----------------------------------------------------------- */

static inline mi_page_t* _mi_heap_get_free_small_page(mi_heap_t* heap, size_t size) {
  mi_assert_internal(size <= MI_SMALL_SIZE_MAX);
  return heap->pages_free_direct[_mi_wsize_from_size(size)];
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
  mi_assert_internal(diff >= 0 && diff < MI_SEGMENT_SIZE);
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
  return _mi_segment_page_start(segment, page, page->block_size, page_size);
}

// Get the page containing the pointer
static inline mi_page_t* _mi_ptr_page(void* p) {
  return _mi_segment_page_of(_mi_ptr_segment(p), p);
}

// Thread free access
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
static inline bool mi_page_all_free(const mi_page_t* page) {
  mi_assert_internal(page != NULL);
  return (page->used - page->thread_freed == 0);
}

// are there immediately available blocks
static inline bool mi_page_immediate_available(const mi_page_t* page) {
  mi_assert_internal(page != NULL);
  return (page->free != NULL);
}
// are there free blocks in this page?
static inline bool mi_page_has_free(mi_page_t* page) {
  mi_assert_internal(page != NULL);
  bool hasfree = (mi_page_immediate_available(page) || page->local_free != NULL || (mi_tf_block(page->thread_free) != NULL));
  mi_assert_internal(hasfree || page->used - page->thread_freed == page->capacity);
  return hasfree;
}

// are all blocks in use?
static inline bool mi_page_all_used(mi_page_t* page) {
  mi_assert_internal(page != NULL);
  return !mi_page_has_free(page);
}

// is more than 7/8th of a page in use?
static inline bool mi_page_mostly_used(const mi_page_t* page) {
  if (page==NULL) return true;
  uint16_t frac = page->reserved / 8U;
  return (page->reserved - page->used + page->thread_freed <= frac);
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


// -------------------------------------------------------------------
// Encoding/Decoding the free list next pointers
// Note: we pass a `null` value to be used as the `NULL` value for the 
// end of a free list. This is to prevent the cookie itself to ever 
// be present among user blocks (as `cookie^0==cookie`).
// -------------------------------------------------------------------

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

static inline mi_block_t* mi_block_nextx( const void* null, const mi_block_t* block, uintptr_t cookie ) {
  #ifdef MI_ENCODE_FREELIST
  mi_block_t* b = (mi_block_t*)(block->next ^ cookie);
  if (mi_unlikely((void*)b==null)) { b = NULL; }
  return b;
  #else
  UNUSED(cookie); UNUSED(null);
  return (mi_block_t*)block->next;
  #endif
}

static inline void mi_block_set_nextx(const void* null, mi_block_t* block, const mi_block_t* next, uintptr_t cookie) {
  #ifdef MI_ENCODE_FREELIST
  if (mi_unlikely(next==NULL)) { next = (mi_block_t*)null; }
  block->next = (mi_encoded_t)next ^ cookie;
  #else
  UNUSED(cookie); UNUSED(null);
  block->next = (mi_encoded_t)next;
  #endif
}

static inline mi_block_t* mi_block_next(const mi_page_t* page, const mi_block_t* block) {
  #ifdef MI_ENCODE_FREELIST
  mi_block_t* next = mi_block_nextx(page,block,page->cookie);
  // check for free list corruption: is `next` at least in our segment range?
  // TODO: check if `next` is `page->block_size` aligned?
  if (next!=NULL && !mi_is_in_same_page(block, next)) {
    _mi_fatal_error("corrupted free list entry of size %zub at %p: value 0x%zx\n", page->block_size, block, (uintptr_t)next);
    next = NULL;
  }   
  return next;
  #else
  UNUSED(page);
  return mi_block_nextx(page,block,0);
  #endif
}

static inline void mi_block_set_next(const mi_page_t* page, mi_block_t* block, const mi_block_t* next) {
  #ifdef MI_ENCODE_FREELIST
  mi_block_set_nextx(page,block,next, page->cookie);
  #else
  UNUSED(page);
  mi_block_set_nextx(page,block, next,0);
  #endif
}

// -------------------------------------------------------------------
// Getting the thread id should be performant
// as it is called in the fast path of `_mi_free`,
// so we specialize for various platforms.
// -------------------------------------------------------------------
#if defined(_WIN32)
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
static inline uintptr_t _mi_thread_id(void) mi_attr_noexcept {
  // Windows: works on Intel and ARM in both 32- and 64-bit
  return (uintptr_t)NtCurrentTeb();
}
#elif (defined(__GNUC__) || defined(__clang__)) && \
      (defined(__x86_64__) || defined(__i386__) || defined(__arm__) || defined(__aarch64__))
// TLS register on x86 is in the FS or GS register
// see: https://akkadia.org/drepper/tls.pdf
static inline uintptr_t _mi_thread_id(void) mi_attr_noexcept {
  uintptr_t tid;
  #if defined(__i386__)
  __asm__("movl %%gs:0, %0" : "=r" (tid) : : );  // 32-bit always uses GS
  #elif defined(__MACH__)
  __asm__("movq %%gs:0, %0" : "=r" (tid) : : );  // x86_64 macOS uses GS
  #elif defined(__x86_64__)
  __asm__("movq %%fs:0, %0" : "=r" (tid) : : );  // x86_64 Linux, BSD uses FS
  #elif defined(__arm__)
  asm volatile ("mrc p15, 0, %0, c13, c0, 3" : "=r" (tid));
  #elif defined(__aarch64__)
  asm volatile ("mrs %0, tpidr_el0" : "=r" (tid));
  #endif
  return tid;
}
#else
// otherwise use standard C
static inline uintptr_t _mi_thread_id(void) mi_attr_noexcept {
  return (uintptr_t)&_mi_heap_default;
}
#endif


#endif
