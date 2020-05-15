/*----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/

/* -----------------------------------------------------------
  The core of the allocator. Every segment contains
  pages of a certain block size. The main function
  exported is `mi_malloc_generic`.
----------------------------------------------------------- */

#include "mimalloc.h"
#include "mimalloc-internal.h"
#include "mimalloc-atomic.h"

/* -----------------------------------------------------------
  Definition of page queues for each block size
----------------------------------------------------------- */

#define MI_IN_PAGE_C
#include "page-queue.c"
#undef MI_IN_PAGE_C


/* -----------------------------------------------------------
  Page helpers
----------------------------------------------------------- */

// Index a block in a page
static inline mi_block_t* mi_page_block_at(const mi_page_t* page, void* page_start, size_t i) {
  mi_assert_internal(page != NULL);
  mi_assert_internal(i <= page->reserved);
  return (mi_block_t*)((uint8_t*)page_start + (i * page->block_size));
}

static void mi_page_init(mi_heap_t* heap, mi_page_t* page, size_t size, mi_stats_t* stats);


#if (MI_DEBUG>1)
static size_t mi_page_list_count(mi_page_t* page, mi_block_t* head) {
  size_t count = 0;
  while (head != NULL) {
    mi_assert_internal(page == _mi_ptr_page(head));
    count++;
    head = mi_block_next(page, head);
  }
  return count;
}

/*
// Start of the page available memory
static inline uint8_t* mi_page_area(const mi_page_t* page) {
  return _mi_page_start(_mi_page_segment(page), page, NULL);
}
*/

static bool mi_page_list_is_valid(mi_page_t* page, mi_block_t* p) {
  size_t psize;
  uint8_t* page_area = _mi_page_start(_mi_page_segment(page), page, &psize);
  mi_block_t* start = (mi_block_t*)page_area;
  mi_block_t* end   = (mi_block_t*)(page_area + psize);
  while(p != NULL) {
    if (p < start || p >= end) return false;
    p = mi_block_next(page, p);
  }
  return true;
}

static bool mi_page_is_valid_init(mi_page_t* page) {
  mi_assert_internal(page->block_size > 0);
  mi_assert_internal(page->used <= page->capacity);
  mi_assert_internal(page->capacity <= page->reserved);

  mi_segment_t* segment = _mi_page_segment(page);
  uint8_t* start = _mi_page_start(segment,page,NULL);
  mi_assert_internal(start == _mi_segment_page_start(segment,page,page->block_size,NULL));
  //mi_assert_internal(start + page->capacity*page->block_size == page->top);

  mi_assert_internal(mi_page_list_is_valid(page,page->free));
  mi_assert_internal(mi_page_list_is_valid(page,page->local_free));

  #if MI_DEBUG>3 // generally too expensive to check this
  if (page->flags.is_zero) {
    for(mi_block_t* block = page->free; block != NULL; mi_block_next(page,block)) {
      mi_assert_expensive(mi_mem_is_zero(block + 1, page->block_size - sizeof(mi_block_t)));
    }
  }
  #endif

  mi_block_t* tfree = mi_tf_block(page->thread_free);
  mi_assert_internal(mi_page_list_is_valid(page, tfree));
  size_t tfree_count = mi_page_list_count(page, tfree);
  mi_assert_internal(tfree_count <= page->thread_freed + 1);

  size_t free_count = mi_page_list_count(page, page->free) + mi_page_list_count(page, page->local_free);
  mi_assert_internal(page->used + free_count == page->capacity);

  return true;
}

bool _mi_page_is_valid(mi_page_t* page) {
  mi_assert_internal(mi_page_is_valid_init(page));
  #if MI_SECURE
  mi_assert_internal(page->cookie != 0);
  #endif
  if (page->heap!=NULL) {
    mi_segment_t* segment = _mi_page_segment(page);
    mi_assert_internal(!_mi_process_is_initialized || segment->thread_id == page->heap->thread_id || segment->thread_id==0);
    if (segment->page_kind != MI_PAGE_HUGE) {
      mi_page_queue_t* pq = mi_page_queue_of(page);
      mi_assert_internal(mi_page_queue_contains(pq, page));
      mi_assert_internal(pq->block_size==page->block_size || page->block_size > MI_LARGE_OBJ_SIZE_MAX || mi_page_is_in_full(page));
      mi_assert_internal(mi_heap_contains_queue(page->heap,pq));
    }
  }
  return true;
}
#endif


void _mi_page_use_delayed_free(mi_page_t* page, mi_delayed_t delay  ) {
  mi_thread_free_t tfree;
  mi_thread_free_t tfreex;

  do {
    tfreex = tfree = page->thread_free;
    if (mi_unlikely(mi_tf_delayed(tfree) < MI_DELAYED_FREEING)) {
      tfreex = mi_tf_set_delayed(tfree,delay);
    }
    else if (mi_unlikely(mi_tf_delayed(tfree) == MI_DELAYED_FREEING)) {
      mi_atomic_yield(); // delay until outstanding MI_DELAYED_FREEING are done.
      continue;          // and try again
    }
  }
  while((mi_tf_delayed(tfreex) !=  mi_tf_delayed(tfree)) && // avoid atomic operation if already equal
        !mi_atomic_cas_weak(mi_atomic_cast(uintptr_t,&page->thread_free), tfreex, tfree));
}


/* -----------------------------------------------------------
  Page collect the `local_free` and `thread_free` lists
----------------------------------------------------------- */

// Collect the local `thread_free` list using an atomic exchange.
// Note: The exchange must be done atomically as this is used right after
// moving to the full list in `mi_page_collect_ex` and we need to
// ensure that there was no race where the page became unfull just before the move.
static void _mi_page_thread_free_collect(mi_page_t* page)
{
  mi_block_t* head;
  mi_thread_free_t tfree;
  mi_thread_free_t tfreex;
  do {
    tfree = page->thread_free;
    head = mi_tf_block(tfree);
    tfreex = mi_tf_set_block(tfree,NULL);
  } while (!mi_atomic_cas_weak(mi_atomic_cast(uintptr_t,&page->thread_free), tfreex, tfree));

  // return if the list is empty
  if (head == NULL) return;

  // find the tail -- also to get a proper count (without data races)
  uintptr_t max_count = page->capacity; // cannot collect more than capacity
  uintptr_t count = 1;
  mi_block_t* tail = head;
  mi_block_t* next;
  while ((next = mi_block_next(page,tail)) != NULL && count <= max_count) {
    count++;
    tail = next;
  }
  // if `count > max_count` there was a memory corruption (possibly infinite list due to double multi-threaded free)
  if (count > max_count) {
    _mi_fatal_error("corrupted thread-free list\n");
    return; // the thread-free items cannot be freed
  }

  // and append the current local free list
  mi_block_set_next(page,tail, page->local_free);
  page->local_free = head;

  // update counts now
  mi_atomic_subu(&page->thread_freed, count);
  page->used -= count;
}

void _mi_page_free_collect(mi_page_t* page, bool force) {
  mi_assert_internal(page!=NULL);

  // collect the thread free list
  if (force || mi_tf_block(page->thread_free) != NULL) {  // quick test to avoid an atomic operation
    _mi_page_thread_free_collect(page);
  }

  // and the local free list
  if (page->local_free != NULL) {
    if (mi_likely(page->free == NULL)) {
      // usual case
      page->free = page->local_free;
      page->local_free = NULL;
      page->is_zero = false;
    }
    else if (force) {
      // append -- only on shutdown (force) as this is a linear operation
      mi_block_t* tail = page->local_free;
      mi_block_t* next;
      while ((next = mi_block_next(page, tail)) != NULL) {
        tail = next;
      }
      mi_block_set_next(page, tail, page->free);
      page->free = page->local_free;
      page->local_free = NULL;
      page->is_zero = false;
    }
  }

  mi_assert_internal(!force || page->local_free == NULL);
}



/* -----------------------------------------------------------
  Page fresh and retire
----------------------------------------------------------- */

// called from segments when reclaiming abandoned pages
void _mi_page_reclaim(mi_heap_t* heap, mi_page_t* page) {
  mi_assert_expensive(mi_page_is_valid_init(page));
  mi_assert_internal(page->heap == NULL);
  mi_assert_internal(_mi_page_segment(page)->page_kind != MI_PAGE_HUGE);
  _mi_page_free_collect(page,false);
  mi_page_queue_t* pq = mi_page_queue(heap, page->block_size);
  mi_page_queue_push(heap, pq, page);
  mi_assert_expensive(_mi_page_is_valid(page));
}

// allocate a fresh page from a segment
static mi_page_t* mi_page_fresh_alloc(mi_heap_t* heap, mi_page_queue_t* pq, size_t block_size) {
  mi_assert_internal(pq==NULL||mi_heap_contains_queue(heap, pq));
  mi_page_t* page = _mi_segment_page_alloc(block_size, &heap->tld->segments, &heap->tld->os);
  if (page == NULL) return NULL;
  mi_assert_internal(pq==NULL || _mi_page_segment(page)->page_kind != MI_PAGE_HUGE);
  mi_page_init(heap, page, block_size, &heap->tld->stats);
  _mi_stat_increase( &heap->tld->stats.pages, 1);
  if (pq!=NULL) mi_page_queue_push(heap, pq, page); // huge pages use pq==NULL
  mi_assert_expensive(_mi_page_is_valid(page));
  return page;
}

// Get a fresh page to use
static mi_page_t* mi_page_fresh(mi_heap_t* heap, mi_page_queue_t* pq) {
  mi_assert_internal(mi_heap_contains_queue(heap, pq));

  // try to reclaim an abandoned page first
  mi_page_t* page = pq->first;
  if (!heap->no_reclaim &&
      _mi_segment_try_reclaim_abandoned(heap, false, &heap->tld->segments) &&
      page != pq->first)
  {
    // we reclaimed, and we got lucky with a reclaimed page in our queue
    page = pq->first;
    if (page->free != NULL) return page;
  }
  // otherwise allocate the page
  page = mi_page_fresh_alloc(heap, pq, pq->block_size);
  if (page==NULL) return NULL;
  mi_assert_internal(pq->block_size==page->block_size);
  mi_assert_internal(pq==mi_page_queue(heap,page->block_size));
  return page;
}

/* -----------------------------------------------------------
   Do any delayed frees
   (put there by other threads if they deallocated in a full page)
----------------------------------------------------------- */
void _mi_heap_delayed_free(mi_heap_t* heap) {
  // take over the list
  mi_block_t* block;
  do {
    block = (mi_block_t*)heap->thread_delayed_free;
  } while (block != NULL && !mi_atomic_cas_ptr_weak(mi_atomic_cast(void*,&heap->thread_delayed_free), NULL, block));

  // and free them all
  while(block != NULL) {
    mi_block_t* next = mi_block_nextx(heap,block, heap->cookie);
    // use internal free instead of regular one to keep stats etc correct
    if (!_mi_free_delayed_block(block)) {
      // we might already start delayed freeing while another thread has not yet
      // reset the delayed_freeing flag; in that case delay it further by reinserting.
      mi_block_t* dfree;
      do {
        dfree = (mi_block_t*)heap->thread_delayed_free;
        mi_block_set_nextx(heap, block, dfree, heap->cookie);
      } while (!mi_atomic_cas_ptr_weak(mi_atomic_cast(void*,&heap->thread_delayed_free), block, dfree));

    }
    block = next;
  }
}

/* -----------------------------------------------------------
  Unfull, abandon, free and retire
----------------------------------------------------------- */

// Move a page from the full list back to a regular list
void _mi_page_unfull(mi_page_t* page) {
  mi_assert_internal(page != NULL);
  mi_assert_expensive(_mi_page_is_valid(page));
  mi_assert_internal(mi_page_is_in_full(page));

  _mi_page_use_delayed_free(page, MI_NO_DELAYED_FREE);
  if (!mi_page_is_in_full(page)) return;

  mi_heap_t* heap = page->heap;
  mi_page_queue_t* pqfull = &heap->pages[MI_BIN_FULL];
  mi_page_set_in_full(page, false); // to get the right queue
  mi_page_queue_t* pq = mi_heap_page_queue_of(heap, page);
  mi_page_set_in_full(page, true);
  mi_page_queue_enqueue_from(pq, pqfull, page);
}

static void mi_page_to_full(mi_page_t* page, mi_page_queue_t* pq) {
  mi_assert_internal(pq == mi_page_queue_of(page));
  mi_assert_internal(!mi_page_immediate_available(page));
  mi_assert_internal(!mi_page_is_in_full(page));

  _mi_page_use_delayed_free(page, MI_USE_DELAYED_FREE);
  if (mi_page_is_in_full(page)) return;

  mi_page_queue_enqueue_from(&page->heap->pages[MI_BIN_FULL], pq, page);
  _mi_page_free_collect(page,false);  // try to collect right away in case another thread freed just before MI_USE_DELAYED_FREE was set
}


// Abandon a page with used blocks at the end of a thread.
// Note: only call if it is ensured that no references exist from
// the `page->heap->thread_delayed_free` into this page.
// Currently only called through `mi_heap_collect_ex` which ensures this.
void _mi_page_abandon(mi_page_t* page, mi_page_queue_t* pq) {
  mi_assert_internal(page != NULL);
  mi_assert_expensive(_mi_page_is_valid(page));
  mi_assert_internal(pq == mi_page_queue_of(page));
  mi_assert_internal(page->heap != NULL);

#if MI_DEBUG > 1
  mi_heap_t* pheap = (mi_heap_t*)mi_atomic_read_ptr(mi_atomic_cast(void*, &page->heap));
#endif

  // remove from our page list
  mi_segments_tld_t* segments_tld = &page->heap->tld->segments;
  mi_page_queue_remove(pq, page);

  // page is no longer associated with our heap
  mi_atomic_write_ptr(mi_atomic_cast(void*, &page->heap), NULL);

#if MI_DEBUG>1
  // check there are no references left..
  for (mi_block_t* block = (mi_block_t*)pheap->thread_delayed_free; block != NULL; block = mi_block_nextx(pheap, block, pheap->cookie)) {
    mi_assert_internal(_mi_ptr_page(block) != page);
  }
#endif

  // and abandon it
  mi_assert_internal(page->heap == NULL);
  _mi_segment_page_abandon(page,segments_tld);
}


// Free a page with no more free blocks
void _mi_page_free(mi_page_t* page, mi_page_queue_t* pq, bool force) {
  mi_assert_internal(page != NULL);
  mi_assert_expensive(_mi_page_is_valid(page));
  mi_assert_internal(pq == mi_page_queue_of(page));
  mi_assert_internal(mi_page_all_free(page));
  #if MI_DEBUG>1
  // check if we can safely free
  mi_thread_free_t free = mi_tf_set_delayed(page->thread_free,MI_NEVER_DELAYED_FREE);
  free = mi_atomic_exchange(&page->thread_free, free);
  mi_assert_internal(mi_tf_delayed(free) != MI_DELAYED_FREEING);
  #endif

  mi_page_set_has_aligned(page, false);

  // account for huge pages here
  // (note: no longer necessary as huge pages are always abandoned)
  if (page->block_size > MI_LARGE_OBJ_SIZE_MAX) {
    if (page->block_size > MI_HUGE_OBJ_SIZE_MAX) {
      _mi_stat_decrease(&page->heap->tld->stats.giant, page->block_size);
    }
    else {
      _mi_stat_decrease(&page->heap->tld->stats.huge, page->block_size);
    }
  }
  
  // remove from the page list
  // (no need to do _mi_heap_delayed_free first as all blocks are already free)
  mi_segments_tld_t* segments_tld = &page->heap->tld->segments;
  mi_page_queue_remove(pq, page);

  // and free it
  mi_assert_internal(page->heap == NULL);
  _mi_segment_page_free(page, force, segments_tld);
}

// Retire a page with no more used blocks
// Important to not retire too quickly though as new
// allocations might coming.
// Note: called from `mi_free` and benchmarks often
// trigger this due to freeing everything and then
// allocating again so careful when changing this.
void _mi_page_retire(mi_page_t* page) {
  mi_assert_internal(page != NULL);
  mi_assert_expensive(_mi_page_is_valid(page));
  mi_assert_internal(mi_page_all_free(page));

  mi_page_set_has_aligned(page, false);

  // don't retire too often..
  // (or we end up retiring and re-allocating most of the time)
  // NOTE: refine this more: we should not retire if this
  // is the only page left with free blocks. It is not clear
  // how to check this efficiently though... 
  // for now, we don't retire if it is the only page left of this size class.
  mi_page_queue_t* pq = mi_page_queue_of(page);
  if (mi_likely(page->block_size <= (MI_SMALL_SIZE_MAX/4))) {
    // if (mi_page_mostly_used(page->prev) && mi_page_mostly_used(page->next)) {
    if (pq->last==page && pq->first==page) {
      mi_stat_counter_increase(_mi_stats_main.page_no_retire,1);
      return; // dont't retire after all
    }
  }

  _mi_page_free(page, pq, false);
}


/* -----------------------------------------------------------
  Initialize the initial free list in a page.
  In secure mode we initialize a randomized list by
  alternating between slices.
----------------------------------------------------------- */

#define MI_MAX_SLICE_SHIFT  (6)   // at most 64 slices
#define MI_MAX_SLICES       (1UL << MI_MAX_SLICE_SHIFT)
#define MI_MIN_SLICES       (2)

static void mi_page_free_list_extend_secure(mi_heap_t* const heap, mi_page_t* const page, const size_t extend, mi_stats_t* const stats) {
  UNUSED(stats);
  #if (MI_SECURE<=2)
  mi_assert_internal(page->free == NULL);
  mi_assert_internal(page->local_free == NULL);
  #endif
  mi_assert_internal(page->capacity + extend <= page->reserved);
  void* const page_area = _mi_page_start(_mi_page_segment(page), page, NULL);
  const size_t bsize = page->block_size;

  // initialize a randomized free list
  // set up `slice_count` slices to alternate between
  size_t shift = MI_MAX_SLICE_SHIFT;
  while ((extend >> shift) == 0) {
    shift--;
  }
  const size_t slice_count = (size_t)1U << shift;
  const size_t slice_extend = extend / slice_count;
  mi_assert_internal(slice_extend >= 1);
  mi_block_t* blocks[MI_MAX_SLICES];   // current start of the slice
  size_t      counts[MI_MAX_SLICES];   // available objects in the slice
  for (size_t i = 0; i < slice_count; i++) {
    blocks[i] = mi_page_block_at(page, page_area, page->capacity + i*slice_extend);
    counts[i] = slice_extend;
  }
  counts[slice_count-1] += (extend % slice_count);  // final slice holds the modulus too (todo: distribute evenly?)

  // and initialize the free list by randomly threading through them
  // set up first element
  size_t current = _mi_heap_random(heap) % slice_count;
  counts[current]--;
  mi_block_t* const free_start = blocks[current];
  // and iterate through the rest
  uintptr_t rnd = heap->random;
  for (size_t i = 1; i < extend; i++) {
    // call random_shuffle only every INTPTR_SIZE rounds
    const size_t round = i%MI_INTPTR_SIZE;
    if (round == 0) rnd = _mi_random_shuffle(rnd);
    // select a random next slice index
    size_t next = ((rnd >> 8*round) & (slice_count-1));
    while (counts[next]==0) {                            // ensure it still has space
      next++;
      if (next==slice_count) next = 0;
    }
    // and link the current block to it
    counts[next]--;
    mi_block_t* const block = blocks[current];
    blocks[current] = (mi_block_t*)((uint8_t*)block + bsize);  // bump to the following block
    mi_block_set_next(page, block, blocks[next]);   // and set next; note: we may have `current == next`
    current = next;
  }
  // prepend to the free list (usually NULL)
  mi_block_set_next(page, blocks[current], page->free);  // end of the list
  page->free = free_start;
  heap->random = _mi_random_shuffle(rnd);
}

static mi_decl_noinline void mi_page_free_list_extend( mi_page_t* const page, const size_t extend, mi_stats_t* const stats)
{
  UNUSED(stats);
  #if (MI_SECURE <= 2)
  mi_assert_internal(page->free == NULL);
  mi_assert_internal(page->local_free == NULL);
  #endif
  mi_assert_internal(page->capacity + extend <= page->reserved);
  void* const page_area = _mi_page_start(_mi_page_segment(page), page, NULL );
  const size_t bsize = page->block_size;
  mi_block_t* const start = mi_page_block_at(page, page_area, page->capacity);
  
  // initialize a sequential free list
  mi_block_t* const last = mi_page_block_at(page, page_area, page->capacity + extend - 1);  
  mi_block_t* block = start;
  while(block <= last) {
    mi_block_t* next = (mi_block_t*)((uint8_t*)block + bsize);
    mi_block_set_next(page,block,next);
    block = next;
  }  
  // prepend to free list (usually `NULL`)
  mi_block_set_next(page, last, page->free);
  page->free = start;
}

/* -----------------------------------------------------------
  Page initialize and extend the capacity
----------------------------------------------------------- */

#define MI_MAX_EXTEND_SIZE    (4*1024)      // heuristic, one OS page seems to work well.
#if (MI_SECURE>0)
#define MI_MIN_EXTEND         (8*MI_SECURE) // extend at least by this many
#else
#define MI_MIN_EXTEND         (1)
#endif

// Extend the capacity (up to reserved) by initializing a free list
// We do at most `MI_MAX_EXTEND` to avoid touching too much memory
// Note: we also experimented with "bump" allocation on the first
// allocations but this did not speed up any benchmark (due to an
// extra test in malloc? or cache effects?)
static void mi_page_extend_free(mi_heap_t* heap, mi_page_t* page, mi_stats_t* stats) {
  UNUSED(stats);
  mi_assert_expensive(mi_page_is_valid_init(page));
  #if (MI_SECURE<=2)
  mi_assert(page->free == NULL);
  mi_assert(page->local_free == NULL);
  if (page->free != NULL) return;
  #endif
  if (page->capacity >= page->reserved) return;

  size_t page_size;
  _mi_page_start(_mi_page_segment(page), page, &page_size);
  mi_stat_counter_increase(stats->pages_extended, 1);

  // calculate the extend count
  size_t extend = page->reserved - page->capacity;
  size_t max_extend = (page->block_size >= MI_MAX_EXTEND_SIZE ? MI_MIN_EXTEND : MI_MAX_EXTEND_SIZE/(uint32_t)page->block_size);
  if (max_extend < MI_MIN_EXTEND) max_extend = MI_MIN_EXTEND;

  if (extend > max_extend) {
    // ensure we don't touch memory beyond the page to reduce page commit.
    // the `lean` benchmark tests this. Going from 1 to 8 increases rss by 50%.
    extend = (max_extend==0 ? 1 : max_extend);
  }

  mi_assert_internal(extend > 0 && extend + page->capacity <= page->reserved);
  mi_assert_internal(extend < (1UL<<16));

  // and append the extend the free list
  if (extend < MI_MIN_SLICES || MI_SECURE==0) { //!mi_option_is_enabled(mi_option_secure)) {
    mi_page_free_list_extend(page, extend, stats );
  }
  else {
    mi_page_free_list_extend_secure(heap, page, extend, stats);
  }
  // enable the new free list
  page->capacity += (uint16_t)extend;
  mi_stat_increase(stats->page_committed, extend * page->block_size);

  // extension into zero initialized memory preserves the zero'd free list
  if (!page->is_zero_init) {
    page->is_zero = false;
  }
  mi_assert_expensive(mi_page_is_valid_init(page));
}

// Initialize a fresh page
static void mi_page_init(mi_heap_t* heap, mi_page_t* page, size_t block_size, mi_stats_t* stats) {
  mi_assert(page != NULL);
  mi_segment_t* segment = _mi_page_segment(page);
  mi_assert(segment != NULL);
  mi_assert_internal(block_size > 0);
  // set fields
  size_t page_size;
  _mi_segment_page_start(segment, page, block_size, &page_size);
  page->block_size = block_size;
  mi_assert_internal(page_size / block_size < (1L<<16));
  page->reserved = (uint16_t)(page_size / block_size);
  #ifdef MI_ENCODE_FREELIST
  page->cookie = _mi_heap_random(heap) | 1;
  #endif
  page->is_zero = page->is_zero_init;

  mi_assert_internal(page->capacity == 0);
  mi_assert_internal(page->free == NULL);
  mi_assert_internal(page->used == 0);
  mi_assert_internal(page->thread_free == 0);
  mi_assert_internal(page->thread_freed == 0);
  mi_assert_internal(page->next == NULL);
  mi_assert_internal(page->prev == NULL);
  mi_assert_internal(!mi_page_has_aligned(page));
  #if (MI_ENCODE_FREELIST)
  mi_assert_internal(page->cookie != 0);
  #endif
  mi_assert_expensive(mi_page_is_valid_init(page));

  // initialize an initial free list
  mi_page_extend_free(heap,page,stats);
  mi_assert(mi_page_immediate_available(page));
}


/* -----------------------------------------------------------
  Find pages with free blocks
-------------------------------------------------------------*/

// Find a page with free blocks of `page->block_size`.
static mi_page_t* mi_page_queue_find_free_ex(mi_heap_t* heap, mi_page_queue_t* pq)
{
  // search through the pages in "next fit" order
  mi_page_t* rpage = NULL;
  size_t count = 0;
  size_t page_free_count = 0;
  mi_page_t* page = pq->first;
  while( page != NULL)
  {
    mi_page_t* next = page->next; // remember next
    count++;

    // 0. collect freed blocks by us and other threads
    _mi_page_free_collect(page,false);

    // 1. if the page contains free blocks, we are done
    if (mi_page_immediate_available(page)) {
      // If all blocks are free, we might retire this page instead.
      // do this at most 8 times to bound allocation time.
      // (note: this can happen if a page was earlier not retired due
      //  to having neighbours that were mostly full or due to concurrent frees)
      if (page_free_count < 8 && mi_page_all_free(page)) {
        page_free_count++;
        if (rpage != NULL) _mi_page_free(rpage,pq,false);
        rpage = page;
        page = next;
        continue;     // and keep looking
      }
      else {
        break;  // pick this one
      }
    }

    // 2. Try to extend
    if (page->capacity < page->reserved) {
      mi_page_extend_free(heap, page, &heap->tld->stats);
      mi_assert_internal(mi_page_immediate_available(page));
      break;
    }

    // 3. If the page is completely full, move it to the `mi_pages_full`
    // queue so we don't visit long-lived pages too often.
    mi_assert_internal(!mi_page_is_in_full(page) && !mi_page_immediate_available(page));
    mi_page_to_full(page,pq);

    page = next;
  } // for each page

  mi_stat_counter_increase(heap->tld->stats.searches,count);

  if (page == NULL) {
    page = rpage;
    rpage = NULL;
  }
  if (rpage != NULL) {
    _mi_page_free(rpage,pq,false);
  }

  if (page == NULL) {
    page = mi_page_fresh(heap, pq);
  }
  else {
    mi_assert(pq->first == page);
  }
  mi_assert_internal(page == NULL || mi_page_immediate_available(page));
  return page;
}


// Find a page with free blocks of `size`.
static inline mi_page_t* mi_find_free_page(mi_heap_t* heap, size_t size) {
  mi_page_queue_t* pq = mi_page_queue(heap,size);
  mi_page_t* page = pq->first;
  if (page != NULL) {
    if ((MI_SECURE >= 3) && page->capacity < page->reserved && ((_mi_heap_random(heap) & 1) == 1)) {
      // in secure mode, we extend half the time to increase randomness
      mi_page_extend_free(heap, page, &heap->tld->stats);
      mi_assert_internal(mi_page_immediate_available(page));
    }
    else {
      _mi_page_free_collect(page,false);
    }
    if (mi_page_immediate_available(page)) {
      return page; // fast path
    }
  }
  return mi_page_queue_find_free_ex(heap, pq);
}


/* -----------------------------------------------------------
  Users can register a deferred free function called
  when the `free` list is empty. Since the `local_free`
  is separate this is deterministically called after
  a certain number of allocations.
----------------------------------------------------------- */

static mi_deferred_free_fun* volatile deferred_free = NULL;

void _mi_deferred_free(mi_heap_t* heap, bool force) {
  heap->tld->heartbeat++;
  if (deferred_free != NULL && !heap->tld->recurse) {
    heap->tld->recurse = true;
    deferred_free(force, heap->tld->heartbeat);
    heap->tld->recurse = false;
  }
}

void mi_register_deferred_free(mi_deferred_free_fun* fn) mi_attr_noexcept {
  deferred_free = fn;
}


/* -----------------------------------------------------------
  General allocation
----------------------------------------------------------- */

// A huge page is allocated directly without being in a queue.
// Because huge pages contain just one block, and the segment contains
// just that page, we always treat them as abandoned and any thread
// that frees the block can free the whole page and segment directly.
static mi_page_t* mi_huge_page_alloc(mi_heap_t* heap, size_t size) {
  size_t block_size = _mi_os_good_alloc_size(size);
  mi_assert_internal(_mi_bin(block_size) == MI_BIN_HUGE);  
  mi_page_t* page = mi_page_fresh_alloc(heap,NULL,block_size);
  if (page != NULL) {
    mi_assert_internal(mi_page_immediate_available(page));
    mi_assert_internal(page->block_size == block_size);
    mi_assert_internal(_mi_page_segment(page)->page_kind==MI_PAGE_HUGE);
    mi_assert_internal(_mi_page_segment(page)->used==1);
    mi_assert_internal(_mi_page_segment(page)->thread_id==0); // abandoned, not in the huge queue
    mi_atomic_write_ptr(mi_atomic_cast(void*, &page->heap), NULL);

    if (page->block_size > MI_HUGE_OBJ_SIZE_MAX) {
      _mi_stat_increase(&heap->tld->stats.giant, block_size);
      _mi_stat_counter_increase(&heap->tld->stats.giant_count, 1);
    }
    else {
      _mi_stat_increase(&heap->tld->stats.huge, block_size);
      _mi_stat_counter_increase(&heap->tld->stats.huge_count, 1);
    }
  }  
  return page;
}


// Generic allocation routine if the fast path (`alloc.c:mi_page_malloc`) does not succeed.
void* _mi_malloc_generic(mi_heap_t* heap, size_t size) mi_attr_noexcept
{
  mi_assert_internal(heap != NULL);

  // initialize if necessary
  if (mi_unlikely(!mi_heap_is_initialized(heap))) {
    mi_thread_init(); // calls `_mi_heap_init` in turn
    heap = mi_get_default_heap();
  }
  mi_assert_internal(mi_heap_is_initialized(heap));

  // call potential deferred free routines
  _mi_deferred_free(heap, false);

  // free delayed frees from other threads
  _mi_heap_delayed_free(heap);

  // huge allocation?
  mi_page_t* page;
  if (mi_unlikely(size > MI_LARGE_OBJ_SIZE_MAX)) {
    if (mi_unlikely(size > PTRDIFF_MAX)) {  // we don't allocate more than PTRDIFF_MAX (see <https://sourceware.org/ml/libc-announce/2019/msg00001.html>)
      page = NULL;
    }
    else {
      page = mi_huge_page_alloc(heap,size);
    }
  }
  else {
    // otherwise find a page with free blocks in our size segregated queues
    page = mi_find_free_page(heap,size);
  }
  if (page == NULL) return NULL; // out of memory

  mi_assert_internal(mi_page_immediate_available(page));
  mi_assert_internal(page->block_size >= size);

  // and try again, this time succeeding! (i.e. this should never recurse)
  return _mi_page_malloc(heap, page, size);
}
