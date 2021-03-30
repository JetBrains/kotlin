/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/
#include "mimalloc.h"
#include "mimalloc-internal.h"

#include <string.h>  // memcpy, memset
#include <stdlib.h>  // atexit

// Empty page used to initialize the small free pages array
const mi_page_t _mi_page_empty = {
  0, false, false, false, false,
  0,       // capacity
  0,       // reserved capacity
  { 0 },   // flags
  false,   // is_zero
  0,       // retire_expire
  NULL,    // free
  #if MI_ENCODE_FREELIST
  { 0, 0 },
  #endif
  0,       // used
  0,       // xblock_size
  NULL,    // local_free
  ATOMIC_VAR_INIT(0), // xthread_free
  ATOMIC_VAR_INIT(0), // xheap
  NULL, NULL
};

#define MI_PAGE_EMPTY() ((mi_page_t*)&_mi_page_empty)

#if (MI_PADDING>0) && (MI_INTPTR_SIZE >= 8)
#define MI_SMALL_PAGES_EMPTY  { MI_INIT128(MI_PAGE_EMPTY), MI_PAGE_EMPTY(), MI_PAGE_EMPTY() }
#elif (MI_PADDING>0)
#define MI_SMALL_PAGES_EMPTY  { MI_INIT128(MI_PAGE_EMPTY), MI_PAGE_EMPTY(), MI_PAGE_EMPTY(), MI_PAGE_EMPTY() }
#else
#define MI_SMALL_PAGES_EMPTY  { MI_INIT128(MI_PAGE_EMPTY), MI_PAGE_EMPTY() }
#endif


// Empty page queues for every bin
#define QNULL(sz)  { NULL, NULL, (sz)*sizeof(uintptr_t) }
#define MI_PAGE_QUEUES_EMPTY \
  { QNULL(1), \
    QNULL(     1), QNULL(     2), QNULL(     3), QNULL(     4), QNULL(     5), QNULL(     6), QNULL(     7), QNULL(     8), /* 8 */ \
    QNULL(    10), QNULL(    12), QNULL(    14), QNULL(    16), QNULL(    20), QNULL(    24), QNULL(    28), QNULL(    32), /* 16 */ \
    QNULL(    40), QNULL(    48), QNULL(    56), QNULL(    64), QNULL(    80), QNULL(    96), QNULL(   112), QNULL(   128), /* 24 */ \
    QNULL(   160), QNULL(   192), QNULL(   224), QNULL(   256), QNULL(   320), QNULL(   384), QNULL(   448), QNULL(   512), /* 32 */ \
    QNULL(   640), QNULL(   768), QNULL(   896), QNULL(  1024), QNULL(  1280), QNULL(  1536), QNULL(  1792), QNULL(  2048), /* 40 */ \
    QNULL(  2560), QNULL(  3072), QNULL(  3584), QNULL(  4096), QNULL(  5120), QNULL(  6144), QNULL(  7168), QNULL(  8192), /* 48 */ \
    QNULL( 10240), QNULL( 12288), QNULL( 14336), QNULL( 16384), QNULL( 20480), QNULL( 24576), QNULL( 28672), QNULL( 32768), /* 56 */ \
    QNULL( 40960), QNULL( 49152), QNULL( 57344), QNULL( 65536), QNULL( 81920), QNULL( 98304), QNULL(114688), QNULL(131072), /* 64 */ \
    QNULL(163840), QNULL(196608), QNULL(229376), QNULL(262144), QNULL(327680), QNULL(393216), QNULL(458752), QNULL(524288), /* 72 */ \
    QNULL(MI_LARGE_OBJ_WSIZE_MAX + 1  /* 655360, Huge queue */), \
    QNULL(MI_LARGE_OBJ_WSIZE_MAX + 2) /* Full queue */ }

#define MI_STAT_COUNT_NULL()  {0,0,0,0}

// Empty statistics
#if MI_STAT>1
#define MI_STAT_COUNT_END_NULL()  , { MI_STAT_COUNT_NULL(), MI_INIT32(MI_STAT_COUNT_NULL) }
#else
#define MI_STAT_COUNT_END_NULL()
#endif

#define MI_STATS_NULL  \
  MI_STAT_COUNT_NULL(), MI_STAT_COUNT_NULL(), \
  MI_STAT_COUNT_NULL(), MI_STAT_COUNT_NULL(), \
  MI_STAT_COUNT_NULL(), MI_STAT_COUNT_NULL(), \
  MI_STAT_COUNT_NULL(), MI_STAT_COUNT_NULL(), \
  MI_STAT_COUNT_NULL(), MI_STAT_COUNT_NULL(), \
  MI_STAT_COUNT_NULL(), MI_STAT_COUNT_NULL(), \
  MI_STAT_COUNT_NULL(), \
  { 0, 0 }, { 0, 0 }, { 0, 0 },  \
  { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } \
  MI_STAT_COUNT_END_NULL()

// --------------------------------------------------------
// Statically allocate an empty heap as the initial
// thread local value for the default heap,
// and statically allocate the backing heap for the main
// thread so it can function without doing any allocation
// itself (as accessing a thread local for the first time
// may lead to allocation itself on some platforms)
// --------------------------------------------------------

const mi_heap_t _mi_heap_empty = {
  NULL,
  MI_SMALL_PAGES_EMPTY,
  MI_PAGE_QUEUES_EMPTY,
  ATOMIC_VAR_INIT(NULL),
  0,                // tid
  0,                // cookie
  { 0, 0 },         // keys
  { {0}, {0}, 0 },
  0,                // page count
  MI_BIN_FULL, 0,   // page retired min/max
  NULL,             // next
  false
};

// the thread-local default heap for allocation
mi_decl_thread mi_heap_t* _mi_heap_default = (mi_heap_t*)&_mi_heap_empty;

extern mi_heap_t _mi_heap_main;

static mi_tld_t tld_main = {
  0, false,
  &_mi_heap_main, &_mi_heap_main,
  { { NULL, NULL }, {NULL ,NULL}, {NULL ,NULL, 0},
    0, 0, 0, 0, 0, 0, NULL,
    &tld_main.stats, &tld_main.os
  }, // segments
  { 0, &tld_main.stats },  // os
  { MI_STATS_NULL }       // stats
};

mi_heap_t _mi_heap_main = {
  &tld_main,
  MI_SMALL_PAGES_EMPTY,
  MI_PAGE_QUEUES_EMPTY,
  ATOMIC_VAR_INIT(NULL),
  0,                // thread id
  0,                // initial cookie
  { 0, 0 },         // the key of the main heap can be fixed (unlike page keys that need to be secure!)
  { {0x846ca68b}, {0}, 0 },  // random
  0,                // page count
  MI_BIN_FULL, 0,   // page retired min/max
  NULL,             // next heap
  false             // can reclaim
};

bool _mi_process_is_initialized = false;  // set to `true` in `mi_process_init`.

mi_stats_t _mi_stats_main = { MI_STATS_NULL };


static void mi_heap_main_init(void) {
  if (_mi_heap_main.cookie == 0) {
    _mi_heap_main.thread_id = _mi_thread_id();
    _mi_heap_main.cookie = _os_random_weak((uintptr_t)&mi_heap_main_init);
    _mi_random_init(&_mi_heap_main.random);
    _mi_heap_main.keys[0] = _mi_heap_random_next(&_mi_heap_main);
    _mi_heap_main.keys[1] = _mi_heap_random_next(&_mi_heap_main);
  }
}

mi_heap_t* _mi_heap_main_get(void) {
  mi_heap_main_init();
  return &_mi_heap_main;
}


/* -----------------------------------------------------------
  Initialization and freeing of the thread local heaps
----------------------------------------------------------- */

// note: in x64 in release build `sizeof(mi_thread_data_t)` is under 4KiB (= OS page size).
typedef struct mi_thread_data_s {
  mi_heap_t  heap;  // must come first due to cast in `_mi_heap_done`
  mi_tld_t   tld;
} mi_thread_data_t;

// Initialize the thread local default heap, called from `mi_thread_init`
static bool _mi_heap_init(void) {
  if (mi_heap_is_initialized(mi_get_default_heap())) return true;
  if (_mi_is_main_thread()) {
    // mi_assert_internal(_mi_heap_main.thread_id != 0);  // can happen on freeBSD where alloc is called before any initialization
    // the main heap is statically allocated
    mi_heap_main_init();
    _mi_heap_set_default_direct(&_mi_heap_main);
    //mi_assert_internal(_mi_heap_default->tld->heap_backing == mi_get_default_heap());
  }
  else {
    // use `_mi_os_alloc` to allocate directly from the OS
    mi_thread_data_t* td = (mi_thread_data_t*)_mi_os_alloc(sizeof(mi_thread_data_t), &_mi_stats_main); // Todo: more efficient allocation?
    if (td == NULL) {
      // if this fails, try once more. (issue #257)
      td = (mi_thread_data_t*)_mi_os_alloc(sizeof(mi_thread_data_t), &_mi_stats_main);
      if (td == NULL) {
        // really out of memory
        _mi_error_message(ENOMEM, "unable to allocate thread local heap metadata (%zu bytes)\n", sizeof(mi_thread_data_t));
        return false;
      }
    }
    // OS allocated so already zero initialized
    mi_tld_t*  tld = &td->tld;
    mi_heap_t* heap = &td->heap;
    memcpy(heap, &_mi_heap_empty, sizeof(*heap));
    heap->thread_id = _mi_thread_id();
    _mi_random_init(&heap->random);
    heap->cookie  = _mi_heap_random_next(heap) | 1;
    heap->keys[0] = _mi_heap_random_next(heap);
    heap->keys[1] = _mi_heap_random_next(heap);
    heap->tld = tld;
    tld->heap_backing = heap;
    tld->heaps = heap;
    tld->segments.stats = &tld->stats;
    tld->segments.os = &tld->os;
    tld->os.stats = &tld->stats;
    _mi_heap_set_default_direct(heap);
  }
  return false;
}

// Free the thread local default heap (called from `mi_thread_done`)
static bool _mi_heap_done(mi_heap_t* heap) {
  if (!mi_heap_is_initialized(heap)) return true;

  // reset default heap
  _mi_heap_set_default_direct(_mi_is_main_thread() ? &_mi_heap_main : (mi_heap_t*)&_mi_heap_empty);

  // switch to backing heap
  heap = heap->tld->heap_backing;
  if (!mi_heap_is_initialized(heap)) return false;

  // delete all non-backing heaps in this thread
  mi_heap_t* curr = heap->tld->heaps;
  while (curr != NULL) {
    mi_heap_t* next = curr->next; // save `next` as `curr` will be freed
    if (curr != heap) {
      mi_assert_internal(!mi_heap_is_backing(curr));
      mi_heap_delete(curr);
    }
    curr = next;
  }
  mi_assert_internal(heap->tld->heaps == heap && heap->next == NULL);
  mi_assert_internal(mi_heap_is_backing(heap));

  // collect if not the main thread
  if (heap != &_mi_heap_main) {
    _mi_heap_collect_abandon(heap);
  }

  // merge stats
  _mi_stats_done(&heap->tld->stats);

  // free if not the main thread
  if (heap != &_mi_heap_main) {
    mi_assert_internal(heap->tld->segments.count == 0 || heap->thread_id != _mi_thread_id());
    _mi_os_free(heap, sizeof(mi_thread_data_t), &_mi_stats_main);
  }
#if 0
  // never free the main thread even in debug mode; if a dll is linked statically with mimalloc,
  // there may still be delete/free calls after the mi_fls_done is called. Issue #207
  else {
    _mi_heap_destroy_pages(heap);
    mi_assert_internal(heap->tld->heap_backing == &_mi_heap_main);
  }
#endif
  return false;
}



// --------------------------------------------------------
// Try to run `mi_thread_done()` automatically so any memory
// owned by the thread but not yet released can be abandoned
// and re-owned by another thread.
//
// 1. windows dynamic library:
//     call from DllMain on DLL_THREAD_DETACH
// 2. windows static library:
//     use `FlsAlloc` to call a destructor when the thread is done
// 3. unix, pthreads:
//     use a pthread key to call a destructor when a pthread is done
//
// In the last two cases we also need to call `mi_process_init`
// to set up the thread local keys.
// --------------------------------------------------------

static void _mi_thread_done(mi_heap_t* default_heap);

#ifdef __wasi__
// no pthreads in the WebAssembly Standard Interface
#elif !defined(_WIN32)
#define MI_USE_PTHREADS
#endif

#if defined(_WIN32) && defined(MI_SHARED_LIB)
  // nothing to do as it is done in DllMain
#elif defined(_WIN32) && !defined(MI_SHARED_LIB)
  // use thread local storage keys to detect thread ending
  #include <Windows.h>
  #include <fibersapi.h>
  #if (_WIN32_WINNT < 0x600)  // before Windows Vista
  WINBASEAPI DWORD WINAPI FlsAlloc( _In_opt_ PFLS_CALLBACK_FUNCTION lpCallback );
  WINBASEAPI PVOID WINAPI FlsGetValue( _In_ DWORD dwFlsIndex );
  WINBASEAPI BOOL  WINAPI FlsSetValue( _In_ DWORD dwFlsIndex, _In_opt_ PVOID lpFlsData );
  WINBASEAPI BOOL  WINAPI FlsFree(_In_ DWORD dwFlsIndex);
  #endif
  static DWORD mi_fls_key = (DWORD)(-1);
  static void NTAPI mi_fls_done(PVOID value) {
    if (value!=NULL) _mi_thread_done((mi_heap_t*)value);
  }
#elif defined(MI_USE_PTHREADS)
  // use pthread local storage keys to detect thread ending
  // (and used with MI_TLS_PTHREADS for the default heap)
  #include <pthread.h>
  pthread_key_t _mi_heap_default_key = (pthread_key_t)(-1);
  static void mi_pthread_done(void* value) {
    if (value!=NULL) _mi_thread_done((mi_heap_t*)value);
  }
#elif defined(__wasi__)
// no pthreads in the WebAssembly Standard Interface
#else
  #pragma message("define a way to call mi_thread_done when a thread is done")
#endif

// Set up handlers so `mi_thread_done` is called automatically
static void mi_process_setup_auto_thread_done(void) {
  static bool tls_initialized = false; // fine if it races
  if (tls_initialized) return;
  tls_initialized = true;
  #if defined(_WIN32) && defined(MI_SHARED_LIB)
    // nothing to do as it is done in DllMain
  #elif defined(_WIN32) && !defined(MI_SHARED_LIB)
    mi_fls_key = FlsAlloc(&mi_fls_done);
  #elif defined(MI_USE_PTHREADS)
    mi_assert_internal(_mi_heap_default_key == (pthread_key_t)(-1));
    pthread_key_create(&_mi_heap_default_key, &mi_pthread_done);
  #endif
  _mi_heap_set_default_direct(&_mi_heap_main);
}


bool _mi_is_main_thread(void) {
  return (_mi_heap_main.thread_id==0 || _mi_heap_main.thread_id == _mi_thread_id());
}

// This is called from the `mi_malloc_generic`
void mi_thread_init(void) mi_attr_noexcept
{
  // ensure our process has started already
  mi_process_init();

  // initialize the thread local default heap
  // (this will call `_mi_heap_set_default_direct` and thus set the
  //  fiber/pthread key to a non-zero value, ensuring `_mi_thread_done` is called)
  if (_mi_heap_init()) return;  // returns true if already initialized

  _mi_stat_increase(&_mi_stats_main.threads, 1);
  //_mi_verbose_message("thread init: 0x%zx\n", _mi_thread_id());
}

void mi_thread_done(void) mi_attr_noexcept {
  _mi_thread_done(mi_get_default_heap());
}

static void _mi_thread_done(mi_heap_t* heap) {
  _mi_stat_decrease(&_mi_stats_main.threads, 1);

  // check thread-id as on Windows shutdown with FLS the main (exit) thread may call this on thread-local heaps...
  if (heap->thread_id != _mi_thread_id()) return;

  // abandon the thread local heap
  if (_mi_heap_done(heap)) return;  // returns true if already ran
}

void _mi_heap_set_default_direct(mi_heap_t* heap)  {
  mi_assert_internal(heap != NULL);
  #if defined(MI_TLS_SLOT)
  mi_tls_slot_set(MI_TLS_SLOT,heap);
  #elif defined(MI_TLS_PTHREAD_SLOT_OFS)
  *mi_tls_pthread_heap_slot() = heap;
  #elif defined(MI_TLS_PTHREAD)
  // we use _mi_heap_default_key
  #else
  _mi_heap_default = heap;
  #endif

  // ensure the default heap is passed to `_mi_thread_done`
  // setting to a non-NULL value also ensures `mi_thread_done` is called.
  #if defined(_WIN32) && defined(MI_SHARED_LIB)
    // nothing to do as it is done in DllMain
  #elif defined(_WIN32) && !defined(MI_SHARED_LIB)
    mi_assert_internal(mi_fls_key != 0);
    FlsSetValue(mi_fls_key, heap);
  #elif defined(MI_USE_PTHREADS)
  if (_mi_heap_default_key != (pthread_key_t)(-1)) {  // can happen during recursive invocation on freeBSD
    pthread_setspecific(_mi_heap_default_key, heap);
  }
  #endif
}


// --------------------------------------------------------
// Run functions on process init/done, and thread init/done
// --------------------------------------------------------
static void mi_process_done(void);

static bool os_preloading = true;    // true until this module is initialized
static bool mi_redirected = false;   // true if malloc redirects to mi_malloc

// Returns true if this module has not been initialized; Don't use C runtime routines until it returns false.
bool _mi_preloading(void) {
  return os_preloading;
}

bool mi_is_redirected(void) mi_attr_noexcept {
  return mi_redirected;
}

// Communicate with the redirection module on Windows
#if defined(_WIN32) && defined(MI_SHARED_LIB)
#ifdef __cplusplus
extern "C" {
#endif
mi_decl_export void _mi_redirect_entry(DWORD reason) {
  // called on redirection; careful as this may be called before DllMain
  if (reason == DLL_PROCESS_ATTACH) {
    mi_redirected = true;
  }
  else if (reason == DLL_PROCESS_DETACH) {
    mi_redirected = false;
  }
  else if (reason == DLL_THREAD_DETACH) {
    mi_thread_done();
  }
}
__declspec(dllimport) bool mi_allocator_init(const char** message);
__declspec(dllimport) void mi_allocator_done(void);
#ifdef __cplusplus
}
#endif
#else
static bool mi_allocator_init(const char** message) {
  if (message != NULL) *message = NULL;
  return true;
}
static void mi_allocator_done(void) {
  // nothing to do
}
#endif

// Called once by the process loader
static void mi_process_load(void) {
  mi_heap_main_init();
  #if defined(MI_TLS_RECURSE_GUARD)
  volatile mi_heap_t* dummy = _mi_heap_default; // access TLS to allocate it before setting tls_initialized to true;
  UNUSED(dummy);
  #endif
  os_preloading = false;
  atexit(&mi_process_done);
  _mi_options_init();
  mi_process_init();
  //mi_stats_reset();-
  if (mi_redirected) _mi_verbose_message("malloc is redirected.\n");

  // show message from the redirector (if present)
  const char* msg = NULL;
  mi_allocator_init(&msg);
  if (msg != NULL && (mi_option_is_enabled(mi_option_verbose) || mi_option_is_enabled(mi_option_show_errors))) {
    _mi_fputs(NULL,NULL,NULL,msg);
  }
}

// Initialize the process; called by thread_init or the process loader
void mi_process_init(void) mi_attr_noexcept {
  // ensure we are called once
  if (_mi_process_is_initialized) return;
  _mi_process_is_initialized = true;
  mi_process_setup_auto_thread_done();

  _mi_verbose_message("process init: 0x%zx\n", _mi_thread_id());
  _mi_os_init();
  mi_heap_main_init();
  #if (MI_DEBUG)
  _mi_verbose_message("debug level : %d\n", MI_DEBUG);
  #endif
  _mi_verbose_message("secure level: %d\n", MI_SECURE);
  mi_thread_init();
  mi_stats_reset();  // only call stat reset *after* thread init (or the heap tld == NULL)

  if (mi_option_is_enabled(mi_option_reserve_huge_os_pages)) {
    size_t pages = mi_option_get(mi_option_reserve_huge_os_pages);
    mi_reserve_huge_os_pages_interleave(pages, 0, pages*500);
  }
}

// Called when the process is done (through `at_exit`)
static void mi_process_done(void) {
  // only shutdown if we were initialized
  if (!_mi_process_is_initialized) return;
  // ensure we are called once
  static bool process_done = false;
  if (process_done) return;
  process_done = true;

  #if defined(_WIN32) && !defined(MI_SHARED_LIB)
  FlsSetValue(mi_fls_key, NULL);  // don't call main-thread callback
  FlsFree(mi_fls_key);            // call thread-done on all threads to prevent dangling callback pointer if statically linked with a DLL; Issue #208
  #endif

  #if (MI_DEBUG != 0) || !defined(MI_SHARED_LIB)
  // free all memory if possible on process exit. This is not needed for a stand-alone process
  // but should be done if mimalloc is statically linked into another shared library which
  // is repeatedly loaded/unloaded, see issue #281.
  mi_collect(true /* force */ );
  #endif

  if (mi_option_is_enabled(mi_option_show_stats) || mi_option_is_enabled(mi_option_verbose)) {
    mi_stats_print(NULL);
  }
  mi_allocator_done();
  _mi_verbose_message("process done: 0x%zx\n", _mi_heap_main.thread_id);
  os_preloading = true; // don't call the C runtime anymore
}



#if defined(_WIN32) && defined(MI_SHARED_LIB)
  // Windows DLL: easy to hook into process_init and thread_done
  __declspec(dllexport) BOOL WINAPI DllMain(HINSTANCE inst, DWORD reason, LPVOID reserved) {
    UNUSED(reserved);
    UNUSED(inst);
    if (reason==DLL_PROCESS_ATTACH) {
      mi_process_load();
    }
    else if (reason==DLL_THREAD_DETACH) {
      if (!mi_is_redirected()) mi_thread_done();
    }
    return TRUE;
  }

#elif defined(__cplusplus)
  // C++: use static initialization to detect process start
  static bool _mi_process_init(void) {
    mi_process_load();
    return (_mi_heap_main.thread_id != 0);
  }
  static bool mi_initialized = _mi_process_init();

#elif defined(__GNUC__) || defined(__clang__)
  // GCC,Clang: use the constructor attribute
  static void __attribute__((constructor)) _mi_process_init(void) {
    mi_process_load();
  }

#elif defined(_MSC_VER)
  // MSVC: use data section magic for static libraries
  // See <https://www.codeguru.com/cpp/misc/misc/applicationcontrol/article.php/c6945/Running-Code-Before-and-After-Main.htm>
  static int _mi_process_init(void) {
    mi_process_load();
    return 0;
  }
  typedef int(*_crt_cb)(void);
  #ifdef _M_X64
    __pragma(comment(linker, "/include:" "_mi_msvc_initu"))
    #pragma section(".CRT$XIU", long, read)
  #else
    __pragma(comment(linker, "/include:" "__mi_msvc_initu"))
  #endif
  #pragma data_seg(".CRT$XIU")
  _crt_cb _mi_msvc_initu[] = { &_mi_process_init };
  #pragma data_seg()

#else
#pragma message("define a way to call mi_process_load on your platform")
#endif
