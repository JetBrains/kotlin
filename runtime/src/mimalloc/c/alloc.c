/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/
#include "mimalloc.h"
#include "mimalloc-internal.h"
#include "mimalloc-atomic.h"

#include <string.h>  // memset, memcpy, strlen
#include <stdlib.h>  // malloc, exit

#define MI_IN_ALLOC_C
#include "alloc-override.c"
#undef MI_IN_ALLOC_C

// ------------------------------------------------------
// Allocation
// ------------------------------------------------------

// Fast allocation in a page: just pop from the free list.
// Fall back to generic allocation only if the list is empty.
extern inline void* _mi_page_malloc(mi_heap_t* heap, mi_page_t* page, size_t size) mi_attr_noexcept {
  mi_assert_internal(page->block_size==0||page->block_size >= size);
  mi_block_t* block = page->free;
  if (mi_unlikely(block == NULL)) {
    return _mi_malloc_generic(heap, size); // slow path
  }
  mi_assert_internal(block != NULL && _mi_ptr_page(block) == page);
  // pop from the free list
  page->free = mi_block_next(page,block);
  page->used++;
  mi_assert_internal(page->free == NULL || _mi_ptr_page(page->free) == page);
#if (MI_DEBUG!=0)
  if (!page->is_zero) { memset(block, MI_DEBUG_UNINIT, size); }
#elif (MI_SECURE!=0)
  block->next = 0;  // don't leak internal data
#endif
#if (MI_STAT>1)
  if(size <= MI_LARGE_OBJ_SIZE_MAX) {
    size_t bin = _mi_bin(size);
    mi_heap_stat_increase(heap,normal[bin], 1);
  }
#endif
  return block;
}

// allocate a small block
extern inline mi_decl_allocator void* mi_heap_malloc_small(mi_heap_t* heap, size_t size) mi_attr_noexcept {
  mi_assert(size <= MI_SMALL_SIZE_MAX);
  mi_page_t* page = _mi_heap_get_free_small_page(heap,size);
  return _mi_page_malloc(heap, page, size);
}

extern inline mi_decl_allocator void* mi_malloc_small(size_t size) mi_attr_noexcept {
  return mi_heap_malloc_small(mi_get_default_heap(), size);
}


// zero initialized small block
mi_decl_allocator void* mi_zalloc_small(size_t size) mi_attr_noexcept {
  void* p = mi_malloc_small(size);
  if (p != NULL) { memset(p, 0, size); }
  return p;
}

// The main allocation function
extern inline mi_decl_allocator void* mi_heap_malloc(mi_heap_t* heap, size_t size) mi_attr_noexcept {
  mi_assert(heap!=NULL);
  mi_assert(heap->thread_id == 0 || heap->thread_id == _mi_thread_id()); // heaps are thread local
  void* p;
  if (mi_likely(size <= MI_SMALL_SIZE_MAX)) {
    p = mi_heap_malloc_small(heap, size);
  }
  else {
    p = _mi_malloc_generic(heap, size);
  }
  #if MI_STAT>1
  if (p != NULL) {
    if (!mi_heap_is_initialized(heap)) { heap = mi_get_default_heap(); }
    mi_heap_stat_increase( heap, malloc, mi_good_size(size) );  // overestimate for aligned sizes
  }
  #endif
  return p;
}

extern inline mi_decl_allocator void* mi_malloc(size_t size) mi_attr_noexcept {
  return mi_heap_malloc(mi_get_default_heap(), size);
}

void _mi_block_zero_init(const mi_page_t* page, void* p, size_t size) {
  // note: we need to initialize the whole block to zero, not just size
  // or the recalloc/rezalloc functions cannot safely expand in place (see issue #63)
  UNUSED(size);
  mi_assert_internal(p != NULL);
  mi_assert_internal(size > 0 && page->block_size >= size);
  mi_assert_internal(_mi_ptr_page(p)==page);
  if (page->is_zero) {
    // already zero initialized memory?
    ((mi_block_t*)p)->next = 0;  // clear the free list pointer
    mi_assert_expensive(mi_mem_is_zero(p,page->block_size));
  }
  else {
    // otherwise memset
    memset(p, 0, page->block_size);
  }
}

void* _mi_heap_malloc_zero(mi_heap_t* heap, size_t size, bool zero) {
  void* p = mi_heap_malloc(heap,size);
  if (zero && p != NULL) {
    _mi_block_zero_init(_mi_ptr_page(p),p,size);  // todo: can we avoid getting the page again?
  }
  return p;
}

extern inline mi_decl_allocator void* mi_heap_zalloc(mi_heap_t* heap, size_t size) mi_attr_noexcept {
  return _mi_heap_malloc_zero(heap, size, true);
}

mi_decl_allocator void* mi_zalloc(size_t size) mi_attr_noexcept {
  return mi_heap_zalloc(mi_get_default_heap(),size);
}


// ------------------------------------------------------
// Check for double free in secure and debug mode 
// This is somewhat expensive so only enabled for secure mode 4
// ------------------------------------------------------

#if (MI_ENCODE_FREELIST && (MI_SECURE>=4 || MI_DEBUG!=0))
// linear check if the free list contains a specific element
static bool mi_list_contains(const mi_page_t* page, const mi_block_t* list, const mi_block_t* elem) {
  while (list != NULL) {
    if (elem==list) return true;
    list = mi_block_next(page, list);
  }
  return false;
}

static mi_decl_noinline bool mi_check_is_double_freex(const mi_page_t* page, const mi_block_t* block, const mi_block_t* n) {
  size_t psize;
  uint8_t* pstart = _mi_page_start(_mi_page_segment(page), page, &psize);
  if (n == NULL || ((uint8_t*)n >= pstart && (uint8_t*)n < (pstart + psize))) {
    // Suspicious: the decoded value is in the same page (or NULL).
    // Walk the free lists to verify positively if it is already freed
    if (mi_list_contains(page, page->free, block) ||
        mi_list_contains(page, page->local_free, block) ||
        mi_list_contains(page, (const mi_block_t*)mi_atomic_read_ptr_relaxed(mi_atomic_cast(void*,&page->thread_free)), block)) 
    {
      _mi_fatal_error("double free detected of block %p with size %zu\n", block, page->block_size);
      return true;
    }
  }
  return false;
}

static inline bool mi_check_is_double_free(const mi_page_t* page, const mi_block_t* block) {
  mi_block_t* n = mi_block_nextx(page, block, page->cookie); // pretend it is freed, and get the decoded first field
  if (((uintptr_t)n & (MI_INTPTR_SIZE-1))==0 &&        // quick check: aligned pointer?
      (n==NULL || mi_is_in_same_segment(block, n)))    // quick check: in same segment or NULL?
  { 
    // Suspicous: decoded value in block is in the same segment (or NULL) -- maybe a double free?
    // (continue in separate function to improve code generation)
    return mi_check_is_double_freex(page, block, n);
  }  
  return false;
}
#else
static inline bool mi_check_is_double_free(const mi_page_t* page, const mi_block_t* block) {
  UNUSED(page);
  UNUSED(block);
  return false;
}
#endif


// ------------------------------------------------------
// Free
// ------------------------------------------------------

// multi-threaded free
static mi_decl_noinline void _mi_free_block_mt(mi_page_t* page, mi_block_t* block)
{
  mi_thread_free_t tfree;
  mi_thread_free_t tfreex;
  bool use_delayed;

  mi_segment_t* segment = _mi_page_segment(page);
  if (segment->page_kind==MI_PAGE_HUGE) {
    // huge page segments are always abandoned and can be freed immediately
    mi_assert_internal(mi_atomic_read_relaxed(&segment->thread_id)==0);
    mi_assert_internal(mi_atomic_read_ptr_relaxed(mi_atomic_cast(void*,&segment->abandoned_next))==NULL);
    // claim it and free
    mi_heap_t* heap = mi_get_default_heap();
    // paranoia: if this it the last reference, the cas should always succeed
    if (mi_atomic_cas_strong(&segment->thread_id,heap->thread_id,0)) {
      mi_block_set_next(page, block, page->free);
      page->free = block;
      page->used--;
      page->is_zero = false;
      mi_assert(page->used == 0);
      mi_tld_t* tld = heap->tld;
      if (page->block_size > MI_HUGE_OBJ_SIZE_MAX) {
        _mi_stat_decrease(&tld->stats.giant, page->block_size);
      }
      else {
        _mi_stat_decrease(&tld->stats.huge, page->block_size);
      }
      _mi_segment_page_free(page,true,&tld->segments);
    }
    return;
  }

  do {
    tfree = page->thread_free;
    use_delayed = (mi_tf_delayed(tfree) == MI_USE_DELAYED_FREE ||
                   (mi_tf_delayed(tfree) == MI_NO_DELAYED_FREE && page->used == mi_atomic_read_relaxed(&page->thread_freed)+1)  // data-race but ok, just optimizes early release of the page
                  );
    if (mi_unlikely(use_delayed)) {
      // unlikely: this only happens on the first concurrent free in a page that is in the full list
      tfreex = mi_tf_set_delayed(tfree,MI_DELAYED_FREEING);
    }
    else {
      // usual: directly add to page thread_free list
      mi_block_set_next(page, block, mi_tf_block(tfree));
      tfreex = mi_tf_set_block(tfree,block);
    }
  } while (!mi_atomic_cas_weak(mi_atomic_cast(uintptr_t,&page->thread_free), tfreex, tfree));

  if (mi_likely(!use_delayed)) {
    // increment the thread free count and return
    mi_atomic_increment(&page->thread_freed);
  }
  else {
    // racy read on `heap`, but ok because MI_DELAYED_FREEING is set (see `mi_heap_delete` and `mi_heap_collect_abandon`)
    mi_heap_t* heap = (mi_heap_t*)mi_atomic_read_ptr(mi_atomic_cast(void*, &page->heap));
    mi_assert_internal(heap != NULL);
    if (heap != NULL) {
      // add to the delayed free list of this heap. (do this atomically as the lock only protects heap memory validity)
      mi_block_t* dfree;
      do {
        dfree = (mi_block_t*)heap->thread_delayed_free;
        mi_block_set_nextx(heap,block,dfree, heap->cookie);
      } while (!mi_atomic_cas_ptr_weak(mi_atomic_cast(void*,&heap->thread_delayed_free), block, dfree));
    }

    // and reset the MI_DELAYED_FREEING flag
    do {
      tfreex = tfree = page->thread_free;
      mi_assert_internal(mi_tf_delayed(tfree) == MI_NEVER_DELAYED_FREE || mi_tf_delayed(tfree) == MI_DELAYED_FREEING);
      if (mi_tf_delayed(tfree) != MI_NEVER_DELAYED_FREE) tfreex = mi_tf_set_delayed(tfree,MI_NO_DELAYED_FREE);
    } while (!mi_atomic_cas_weak(mi_atomic_cast(uintptr_t,&page->thread_free), tfreex, tfree));
  }
}


// regular free
static inline void _mi_free_block(mi_page_t* page, bool local, mi_block_t* block)
{
  #if (MI_DEBUG)
  memset(block, MI_DEBUG_FREED, page->block_size);
  #endif

  // and push it on the free list
  if (mi_likely(local)) {
    // owning thread can free a block directly
    if (mi_check_is_double_free(page, block)) return;
    mi_block_set_next(page, block, page->local_free);
    page->local_free = block;
    page->used--;
    if (mi_unlikely(mi_page_all_free(page))) {
      _mi_page_retire(page);
    }
    else if (mi_unlikely(mi_page_is_in_full(page))) {
      _mi_page_unfull(page);
    }
  }
  else {
    _mi_free_block_mt(page,block);
  }
}


// Adjust a block that was allocated aligned, to the actual start of the block in the page.
mi_block_t* _mi_page_ptr_unalign(const mi_segment_t* segment, const mi_page_t* page, const void* p) {
  mi_assert_internal(page!=NULL && p!=NULL);
  size_t diff   = (uint8_t*)p - _mi_page_start(segment, page, NULL);
  size_t adjust = (diff % page->block_size);
  return (mi_block_t*)((uintptr_t)p - adjust);
}


static void mi_decl_noinline mi_free_generic(const mi_segment_t* segment, mi_page_t* page, bool local, void* p) {
  mi_block_t* block = (mi_page_has_aligned(page) ? _mi_page_ptr_unalign(segment, page, p) : (mi_block_t*)p);
  _mi_free_block(page, local, block);
}

// Free a block
void mi_free(void* p) mi_attr_noexcept
{
#if (MI_DEBUG>0)
  if (mi_unlikely(((uintptr_t)p & (MI_INTPTR_SIZE - 1)) != 0)) {
    _mi_error_message("trying to free an invalid (unaligned) pointer: %p\n", p);
    return;
  }
#endif

  const mi_segment_t* const segment = _mi_ptr_segment(p);
  if (mi_unlikely(segment == NULL)) return;  // checks for (p==NULL)

#if (MI_DEBUG!=0)
  if (mi_unlikely(!mi_is_in_heap_region(p))) {
    _mi_warning_message("possibly trying to free a pointer that does not point to a valid heap region: 0x%p\n"
      "(this may still be a valid very large allocation (over 64MiB))\n", p);
    if (mi_likely(_mi_ptr_cookie(segment) == segment->cookie)) {
      _mi_warning_message("(yes, the previous pointer 0x%p was valid after all)\n", p);
    }
  }
#endif
#if (MI_DEBUG!=0 || MI_SECURE>=4)
  if (mi_unlikely(_mi_ptr_cookie(segment) != segment->cookie)) {
    _mi_error_message("trying to free a pointer that does not point to a valid heap space: %p\n", p);
    return;
  }
#endif

  const uintptr_t tid = _mi_thread_id();
  mi_page_t* const page = _mi_segment_page_of(segment, p);

#if (MI_STAT>1)
  mi_heap_t* heap = mi_heap_get_default();
  mi_heap_stat_decrease(heap, malloc, mi_usable_size(p));
  if (page->block_size <= MI_LARGE_OBJ_SIZE_MAX) {
    mi_heap_stat_decrease(heap, normal[_mi_bin(page->block_size)], 1);
  }
  // huge page stat is accounted for in `_mi_page_retire`
#endif

  if (mi_likely(tid == segment->thread_id && page->flags.full_aligned == 0)) {  // the thread id matches and it is not a full page, nor has aligned blocks
    // local, and not full or aligned
    mi_block_t* block = (mi_block_t*)p;
    if (mi_check_is_double_free(page,block)) return;    
    mi_block_set_next(page, block, page->local_free);
    page->local_free = block;
    page->used--;
    if (mi_unlikely(mi_page_all_free(page))) { _mi_page_retire(page); }
  }
  else {
    // non-local, aligned blocks, or a full page; use the more generic path
    mi_free_generic(segment, page, tid == segment->thread_id, p);
  }
}

bool _mi_free_delayed_block(mi_block_t* block) {
  // get segment and page
  const mi_segment_t* segment = _mi_ptr_segment(block);
  mi_assert_internal(_mi_ptr_cookie(segment) == segment->cookie);
  mi_assert_internal(_mi_thread_id() == segment->thread_id);
  mi_page_t* page = _mi_segment_page_of(segment, block);
  if (mi_tf_delayed(page->thread_free) == MI_DELAYED_FREEING) {
    // we might already start delayed freeing while another thread has not yet
    // reset the delayed_freeing flag; in that case don't free it quite yet if
    // this is the last block remaining.
    if (page->used - page->thread_freed == 1) return false;
  }
  _mi_free_block(page,true,block);
  return true;
}

// Bytes available in a block
size_t mi_usable_size(const void* p) mi_attr_noexcept {
  if (p==NULL) return 0;
  const mi_segment_t* segment = _mi_ptr_segment(p);
  const mi_page_t* page = _mi_segment_page_of(segment,p);
  size_t size = page->block_size;
  if (mi_unlikely(mi_page_has_aligned(page))) {
    ptrdiff_t adjust = (uint8_t*)p - (uint8_t*)_mi_page_ptr_unalign(segment,page,p);
    mi_assert_internal(adjust >= 0 && (size_t)adjust <= size);
    return (size - adjust);
  }
  else {
    return size;
  }
}


// ------------------------------------------------------
// ensure explicit external inline definitions are emitted!
// ------------------------------------------------------

#ifdef __cplusplus
void* _mi_externs[] = {
  (void*)&_mi_page_malloc,
  (void*)&mi_malloc,
  (void*)&mi_malloc_small,
  (void*)&mi_heap_malloc,
  (void*)&mi_heap_zalloc,
  (void*)&mi_heap_malloc_small
};
#endif


// ------------------------------------------------------
// Allocation extensions
// ------------------------------------------------------

void mi_free_size(void* p, size_t size) mi_attr_noexcept {
  UNUSED_RELEASE(size);
  mi_assert(p == NULL || size <= mi_usable_size(p));
  mi_free(p);
}

void mi_free_size_aligned(void* p, size_t size, size_t alignment) mi_attr_noexcept {
  UNUSED_RELEASE(alignment);
  mi_assert(((uintptr_t)p % alignment) == 0);
  mi_free_size(p,size);
}

void mi_free_aligned(void* p, size_t alignment) mi_attr_noexcept {
  UNUSED_RELEASE(alignment);
  mi_assert(((uintptr_t)p % alignment) == 0);
  mi_free(p);
}

extern inline mi_decl_allocator void* mi_heap_calloc(mi_heap_t* heap, size_t count, size_t size) mi_attr_noexcept {
  size_t total;
  if (mi_mul_overflow(count,size,&total)) return NULL;
  return mi_heap_zalloc(heap,total);
}

mi_decl_allocator void* mi_calloc(size_t count, size_t size) mi_attr_noexcept {
  return mi_heap_calloc(mi_get_default_heap(),count,size);
}

// Uninitialized `calloc`
extern mi_decl_allocator void* mi_heap_mallocn(mi_heap_t* heap, size_t count, size_t size) mi_attr_noexcept {
  size_t total;
  if (mi_mul_overflow(count,size,&total)) return NULL;
  return mi_heap_malloc(heap, total);
}

mi_decl_allocator void* mi_mallocn(size_t count, size_t size) mi_attr_noexcept {
  return mi_heap_mallocn(mi_get_default_heap(),count,size);
}

// Expand in place or fail
mi_decl_allocator void* mi_expand(void* p, size_t newsize) mi_attr_noexcept {
  if (p == NULL) return NULL;
  size_t size = mi_usable_size(p);
  if (newsize > size) return NULL;
  return p; // it fits
}

void* _mi_heap_realloc_zero(mi_heap_t* heap, void* p, size_t newsize, bool zero) {
  if (p == NULL) return _mi_heap_malloc_zero(heap,newsize,zero);
  size_t size = mi_usable_size(p);
  if (newsize <= size && newsize >= (size / 2)) {
    return p;  // reallocation still fits and not more than 50% waste
  }
  void* newp = mi_heap_malloc(heap,newsize);
  if (mi_likely(newp != NULL)) {
    if (zero && newsize > size) {
      // also set last word in the previous allocation to zero to ensure any padding is zero-initialized
      size_t start = (size >= sizeof(intptr_t) ? size - sizeof(intptr_t) : 0);
      memset((uint8_t*)newp + start, 0, newsize - start);
    }
    memcpy(newp, p, (newsize > size ? size : newsize));
    mi_free(p); // only free if successful
  }
  return newp;
}

mi_decl_allocator void* mi_heap_realloc(mi_heap_t* heap, void* p, size_t newsize) mi_attr_noexcept {
  return _mi_heap_realloc_zero(heap, p, newsize, false);
}

mi_decl_allocator void* mi_heap_reallocn(mi_heap_t* heap, void* p, size_t count, size_t size) mi_attr_noexcept {
  size_t total;
  if (mi_mul_overflow(count, size, &total)) return NULL;
  return mi_heap_realloc(heap, p, total);
}


// Reallocate but free `p` on errors
mi_decl_allocator void* mi_heap_reallocf(mi_heap_t* heap, void* p, size_t newsize) mi_attr_noexcept {
  void* newp = mi_heap_realloc(heap, p, newsize);
  if (newp==NULL && p!=NULL) mi_free(p);
  return newp;
}

mi_decl_allocator void* mi_heap_rezalloc(mi_heap_t* heap, void* p, size_t newsize) mi_attr_noexcept {
  return _mi_heap_realloc_zero(heap, p, newsize, true);
}

mi_decl_allocator void* mi_heap_recalloc(mi_heap_t* heap, void* p, size_t count, size_t size) mi_attr_noexcept {
  size_t total;
  if (mi_mul_overflow(count, size, &total)) return NULL;
  return mi_heap_rezalloc(heap, p, total);
}


mi_decl_allocator void* mi_realloc(void* p, size_t newsize) mi_attr_noexcept {
  return mi_heap_realloc(mi_get_default_heap(),p,newsize);
}

mi_decl_allocator void* mi_reallocn(void* p, size_t count, size_t size) mi_attr_noexcept {
  return mi_heap_reallocn(mi_get_default_heap(),p,count,size);
}

// Reallocate but free `p` on errors
mi_decl_allocator void* mi_reallocf(void* p, size_t newsize) mi_attr_noexcept {
  return mi_heap_reallocf(mi_get_default_heap(),p,newsize);
}

mi_decl_allocator void* mi_rezalloc(void* p, size_t newsize) mi_attr_noexcept {
  return mi_heap_rezalloc(mi_get_default_heap(), p, newsize);
}

mi_decl_allocator void* mi_recalloc(void* p, size_t count, size_t size) mi_attr_noexcept {
  return mi_heap_recalloc(mi_get_default_heap(), p, count, size);
}



// ------------------------------------------------------
// strdup, strndup, and realpath
// ------------------------------------------------------

// `strdup` using mi_malloc
char* mi_heap_strdup(mi_heap_t* heap, const char* s) mi_attr_noexcept {
  if (s == NULL) return NULL;
  size_t n = strlen(s);
  char* t = (char*)mi_heap_malloc(heap,n+1);
  if (t != NULL) memcpy(t, s, n + 1);
  return t;
}

char* mi_strdup(const char* s) mi_attr_noexcept {
  return mi_heap_strdup(mi_get_default_heap(), s);
}

// `strndup` using mi_malloc
char* mi_heap_strndup(mi_heap_t* heap, const char* s, size_t n) mi_attr_noexcept {
  if (s == NULL) return NULL;
  size_t m = strlen(s);
  if (n > m) n = m;
  char* t = (char*)mi_heap_malloc(heap, n+1);
  if (t == NULL) return NULL;
  memcpy(t, s, n);
  t[n] = 0;
  return t;
}

char* mi_strndup(const char* s, size_t n) mi_attr_noexcept {
  return mi_heap_strndup(mi_get_default_heap(),s,n);
}

#ifndef __wasi__
// `realpath` using mi_malloc
#ifdef _WIN32
#ifndef PATH_MAX
#define PATH_MAX MAX_PATH
#endif
#include <windows.h>
#include <errno.h>
char* mi_heap_realpath(mi_heap_t* heap, const char* fname, char* resolved_name) mi_attr_noexcept {
  // todo: use GetFullPathNameW to allow longer file names
  char buf[PATH_MAX];
  DWORD res = GetFullPathNameA(fname, PATH_MAX, (resolved_name == NULL ? buf : resolved_name), NULL);
  if (res == 0) {
    errno = GetLastError(); return NULL;
  }
  else if (res > PATH_MAX) {
    errno = EINVAL; return NULL;
  }
  else if (resolved_name != NULL) {
    return resolved_name;
  }
  else {
    return mi_heap_strndup(heap, buf, PATH_MAX);
  }
}
#else
#include <unistd.h>  // pathconf
static size_t mi_path_max() {
  static size_t path_max = 0;
  if (path_max <= 0) {
    long m = pathconf("/",_PC_PATH_MAX);
    if (m <= 0) path_max = 4096;      // guess
    else if (m < 256) path_max = 256; // at least 256
    else path_max = m;
  }
  return path_max;
}

char* mi_heap_realpath(mi_heap_t* heap, const char* fname, char* resolved_name) mi_attr_noexcept {
  if (resolved_name != NULL) {
    return realpath(fname,resolved_name);
  }
  else {
    size_t n  = mi_path_max();
    char* buf = (char*)mi_malloc(n+1);
    if (buf==NULL) return NULL;
    char* rname  = realpath(fname,buf);
    char* result = mi_heap_strndup(heap,rname,n); // ok if `rname==NULL`
    mi_free(buf);
    return result;
  }
}
#endif

char* mi_realpath(const char* fname, char* resolved_name) mi_attr_noexcept {
  return mi_heap_realpath(mi_get_default_heap(),fname,resolved_name);
}
#endif

/*-------------------------------------------------------
C++ new and new_aligned
The standard requires calling into `get_new_handler` and
throwing the bad_alloc exception on failure. If we compile
with a C++ compiler we can implement this precisely. If we
use a C compiler we cannot throw a `bad_alloc` exception
but we call `exit` instead (i.e. not returning).
-------------------------------------------------------*/

#ifdef __cplusplus
#include <new>
static bool mi_try_new_handler(bool nothrow) {
  std::new_handler h = std::get_new_handler();
  if (h==NULL) {
    if (!nothrow) throw std::bad_alloc();
    return false;
  }
  else {
    h();
    return true;
  }
}
#else
#include <errno.h>
#ifndef ENOMEM
#define ENOMEM 12
#endif
typedef void (*std_new_handler_t)();

#if (defined(__GNUC__) || defined(__clang__))
std_new_handler_t __attribute((weak)) _ZSt15get_new_handlerv() {
  return NULL;
}
std_new_handler_t mi_get_new_handler() {
  return _ZSt15get_new_handlerv();
}
#else
// note: on windows we could dynamically link to `?get_new_handler@std@@YAP6AXXZXZ`.
std_new_handler_t mi_get_new_handler() {
  return NULL;
}
#endif

static bool mi_try_new_handler(bool nothrow) {
  std_new_handler_t h = mi_get_new_handler();
  if (h==NULL) {
    if (!nothrow) exit(ENOMEM);
    return false;
  }
  else {
    h();
    return true;
  }
}
#endif

static mi_decl_noinline void* mi_try_new(size_t n, bool nothrow ) {
  void* p = NULL;
  while(p == NULL && mi_try_new_handler(nothrow)) {
    p = mi_malloc(n);
  }
  return p;
}

void* mi_new(size_t n) {
  void* p = mi_malloc(n);
  if (mi_unlikely(p == NULL)) return mi_try_new(n,false);
  return p;
}

void* mi_new_aligned(size_t n, size_t alignment) {
  void* p;
  do { p = mi_malloc_aligned(n, alignment); }
  while(p == NULL && mi_try_new_handler(false));
  return p;
}

void* mi_new_nothrow(size_t n) {
  void* p = mi_malloc(n);
  if (mi_unlikely(p == NULL)) return mi_try_new(n,true);
  return p;
}

void* mi_new_aligned_nothrow(size_t n, size_t alignment) {
  void* p;
  do { p = mi_malloc_aligned(n, alignment); }
  while (p == NULL && mi_try_new_handler(true));
  return p;
}
