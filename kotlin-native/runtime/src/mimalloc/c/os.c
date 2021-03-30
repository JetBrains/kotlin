/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/
#ifndef _DEFAULT_SOURCE
#define _DEFAULT_SOURCE   // ensure mmap flags are defined
#endif

#if defined(__sun)
// illumos provides new mman.h api when any of these are defined
// otherwise the old api based on caddr_t which predates the void pointers one.
// stock solaris provides only the former, chose to atomically to discard those
// flags only here rather than project wide tough.
#undef _XOPEN_SOURCE
#undef _POSIX_C_SOURCE
#endif
#include "mimalloc.h"
#include "mimalloc-internal.h"
#include "mimalloc-atomic.h"

#include <string.h>  // strerror

#ifdef _MSC_VER
#pragma warning(disable:4996)  // strerror
#endif


#if defined(_WIN32)
#include <Windows.h>
#elif defined(__wasi__)
// stdlib.h is all we need, and has already been included in mimalloc.h
#else
#include <sys/mman.h>  // mmap
#include <unistd.h>    // sysconf
#if defined(__linux__)
#include <features.h>
#if defined(__GLIBC__)
#include <linux/mman.h> // linux mmap flags
#else
#include <sys/mman.h>
#endif
#endif
#if defined(__APPLE__)
#include <TargetConditionals.h>
#if !TARGET_IOS_IPHONE && !TARGET_IOS_SIMULATOR
#include <mach/vm_statistics.h>
#endif
#endif
#if defined(__HAIKU__)
#define madvise posix_madvise
#define MADV_DONTNEED POSIX_MADV_DONTNEED
#endif
#endif

/* -----------------------------------------------------------
  Initialization.
  On windows initializes support for aligned allocation and
  large OS pages (if MIMALLOC_LARGE_OS_PAGES is true).
----------------------------------------------------------- */
bool    _mi_os_decommit(void* addr, size_t size, mi_stats_t* stats);

static void* mi_align_up_ptr(void* p, size_t alignment) {
  return (void*)_mi_align_up((uintptr_t)p, alignment);
}

static uintptr_t _mi_align_down(uintptr_t sz, size_t alignment) {
  return (sz / alignment) * alignment;
}

static void* mi_align_down_ptr(void* p, size_t alignment) {
  return (void*)_mi_align_down((uintptr_t)p, alignment);
}

// page size (initialized properly in `os_init`)
static size_t os_page_size = 4096;

// minimal allocation granularity
static size_t os_alloc_granularity = 4096;

// if non-zero, use large page allocation
static size_t large_os_page_size = 0;

// OS (small) page size
size_t _mi_os_page_size() {
  return os_page_size;
}

// if large OS pages are supported (2 or 4MiB), then return the size, otherwise return the small page size (4KiB)
size_t _mi_os_large_page_size() {
  return (large_os_page_size != 0 ? large_os_page_size : _mi_os_page_size());
}

static bool use_large_os_page(size_t size, size_t alignment) {
  // if we have access, check the size and alignment requirements
  if (large_os_page_size == 0 || !mi_option_is_enabled(mi_option_large_os_pages)) return false;
  return ((size % large_os_page_size) == 0 && (alignment % large_os_page_size) == 0);
}

// round to a good OS allocation size (bounded by max 12.5% waste)
size_t _mi_os_good_alloc_size(size_t size) {
  size_t align_size;
  if (size < 512*KiB) align_size = _mi_os_page_size();
  else if (size < 2*MiB) align_size = 64*KiB;
  else if (size < 8*MiB) align_size = 256*KiB;
  else if (size < 32*MiB) align_size = 1*MiB;
  else align_size = 4*MiB;
  if (size >= (SIZE_MAX - align_size)) return size; // possible overflow?
  return _mi_align_up(size, align_size);
}

#if defined(_WIN32)
// We use VirtualAlloc2 for aligned allocation, but it is only supported on Windows 10 and Windows Server 2016.
// So, we need to look it up dynamically to run on older systems. (use __stdcall for 32-bit compatibility)
// NtAllocateVirtualAllocEx is used for huge OS page allocation (1GiB)
//
// We hide MEM_EXTENDED_PARAMETER to compile with older SDK's.
#include <winternl.h>
typedef PVOID    (__stdcall *PVirtualAlloc2)(HANDLE, PVOID, SIZE_T, ULONG, ULONG, /* MEM_EXTENDED_PARAMETER* */ void*, ULONG);
typedef NTSTATUS (__stdcall *PNtAllocateVirtualMemoryEx)(HANDLE, PVOID*, SIZE_T*, ULONG, ULONG, /* MEM_EXTENDED_PARAMETER* */ PVOID, ULONG);
static PVirtualAlloc2 pVirtualAlloc2 = NULL;
static PNtAllocateVirtualMemoryEx pNtAllocateVirtualMemoryEx = NULL;

// Similarly, GetNumaProcesorNodeEx is only supported since Windows 7
#if (_WIN32_WINNT < 0x601)  // before Win7
typedef struct _PROCESSOR_NUMBER { WORD Group; BYTE Number; BYTE Reserved; } PROCESSOR_NUMBER, *PPROCESSOR_NUMBER;
#endif
typedef VOID (__stdcall *PGetCurrentProcessorNumberEx)(PPROCESSOR_NUMBER ProcNumber);
typedef BOOL (__stdcall *PGetNumaProcessorNodeEx)(PPROCESSOR_NUMBER Processor, PUSHORT NodeNumber);
typedef BOOL (__stdcall* PGetNumaNodeProcessorMaskEx)(USHORT Node, PGROUP_AFFINITY ProcessorMask);
static PGetCurrentProcessorNumberEx pGetCurrentProcessorNumberEx = NULL;
static PGetNumaProcessorNodeEx      pGetNumaProcessorNodeEx = NULL;
static PGetNumaNodeProcessorMaskEx  pGetNumaNodeProcessorMaskEx = NULL;

static bool mi_win_enable_large_os_pages()
{
  if (large_os_page_size > 0) return true;

  // Try to see if large OS pages are supported
  // To use large pages on Windows, we first need access permission
  // Set "Lock pages in memory" permission in the group policy editor
  // <https://devblogs.microsoft.com/oldnewthing/20110128-00/?p=11643>
  unsigned long err = 0;
  HANDLE token = NULL;
  BOOL ok = OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &token);
  if (ok) {
    TOKEN_PRIVILEGES tp;
    ok = LookupPrivilegeValue(NULL, TEXT("SeLockMemoryPrivilege"), &tp.Privileges[0].Luid);
    if (ok) {
      tp.PrivilegeCount = 1;
      tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
      ok = AdjustTokenPrivileges(token, FALSE, &tp, 0, (PTOKEN_PRIVILEGES)NULL, 0);
      if (ok) {
        err = GetLastError();
        ok = (err == ERROR_SUCCESS);
        if (ok) {
          large_os_page_size = GetLargePageMinimum();
        }
      }
    }
    CloseHandle(token);
  }
  if (!ok) {
    if (err == 0) err = GetLastError();
    _mi_warning_message("cannot enable large OS page support, error %lu\n", err);
  }
  return (ok!=0);
}

void _mi_os_init(void) {
  // get the page size
  SYSTEM_INFO si;
  GetSystemInfo(&si);
  if (si.dwPageSize > 0) os_page_size = si.dwPageSize;
  if (si.dwAllocationGranularity > 0) os_alloc_granularity = si.dwAllocationGranularity;
  // get the VirtualAlloc2 function
  HINSTANCE  hDll;
  hDll = LoadLibrary(TEXT("kernelbase.dll"));
  if (hDll != NULL) {
    // use VirtualAlloc2FromApp if possible as it is available to Windows store apps
    pVirtualAlloc2 = (PVirtualAlloc2)(void (*)(void))GetProcAddress(hDll, "VirtualAlloc2FromApp");
    if (pVirtualAlloc2==NULL) pVirtualAlloc2 = (PVirtualAlloc2)(void (*)(void))GetProcAddress(hDll, "VirtualAlloc2");
    FreeLibrary(hDll);
  }
  // NtAllocateVirtualMemoryEx is used for huge page allocation
  hDll = LoadLibrary(TEXT("ntdll.dll"));
  if (hDll != NULL) {
    pNtAllocateVirtualMemoryEx = (PNtAllocateVirtualMemoryEx)(void (*)(void))GetProcAddress(hDll, "NtAllocateVirtualMemoryEx");
    FreeLibrary(hDll);
  }
  // Try to use Win7+ numa API
  hDll = LoadLibrary(TEXT("kernel32.dll"));
  if (hDll != NULL) {
    pGetCurrentProcessorNumberEx = (PGetCurrentProcessorNumberEx)(void (*)(void))GetProcAddress(hDll, "GetCurrentProcessorNumberEx");
    pGetNumaProcessorNodeEx = (PGetNumaProcessorNodeEx)(void (*)(void))GetProcAddress(hDll, "GetNumaProcessorNodeEx");
    pGetNumaNodeProcessorMaskEx = (PGetNumaNodeProcessorMaskEx)(void (*)(void))GetProcAddress(hDll, "GetNumaNodeProcessorMaskEx");
    FreeLibrary(hDll);
  }
  if (mi_option_is_enabled(mi_option_large_os_pages) || mi_option_is_enabled(mi_option_reserve_huge_os_pages)) {
    mi_win_enable_large_os_pages();
  }
}
#elif defined(__wasi__)
void _mi_os_init() {
  os_page_size = 0x10000; // WebAssembly has a fixed page size: 64KB
  os_alloc_granularity = 16;
}
#else
void _mi_os_init() {
  // get the page size
  long result = sysconf(_SC_PAGESIZE);
  if (result > 0) {
    os_page_size = (size_t)result;
    os_alloc_granularity = os_page_size;
  }
  large_os_page_size = 2*MiB; // TODO: can we query the OS for this?
}
#endif


/* -----------------------------------------------------------
  Raw allocation on Windows (VirtualAlloc) and Unix's (mmap).
----------------------------------------------------------- */

static bool mi_os_mem_free(void* addr, size_t size, bool was_committed, mi_stats_t* stats)
{
  if (addr == NULL || size == 0) return true; // || _mi_os_is_huge_reserved(addr)
  bool err = false;
#if defined(_WIN32)
  err = (VirtualFree(addr, 0, MEM_RELEASE) == 0);
#elif defined(__wasi__)
  err = 0; // WebAssembly's heap cannot be shrunk
#else
  err = (munmap(addr, size) == -1);
#endif
  if (was_committed) _mi_stat_decrease(&stats->committed, size);
  _mi_stat_decrease(&stats->reserved, size);
  if (err) {
    _mi_warning_message("munmap failed: %s, addr 0x%8li, size %lu\n", strerror(errno), (size_t)addr, size);
    return false;
  }
  else {
    return true;
  }
}

static void* mi_os_get_aligned_hint(size_t try_alignment, size_t size);

#ifdef _WIN32
static void* mi_win_virtual_allocx(void* addr, size_t size, size_t try_alignment, DWORD flags) {
#if (MI_INTPTR_SIZE >= 8)
  // on 64-bit systems, try to use the virtual address area after 4TiB for 4MiB aligned allocations
  void* hint;
  if (addr == NULL && (hint = mi_os_get_aligned_hint(try_alignment,size)) != NULL) {
    void* p = VirtualAlloc(hint, size, flags, PAGE_READWRITE);
    if (p != NULL) return p;
    DWORD err = GetLastError();
    if (err != ERROR_INVALID_ADDRESS &&   // If linked with multiple instances, we may have tried to allocate at an already allocated area (#210)
        err != ERROR_INVALID_PARAMETER) { // Windows7 instability (#230)
      return NULL;
    }
    // fall through
  }
#endif
#if defined(MEM_EXTENDED_PARAMETER_TYPE_BITS)
  // on modern Windows try use VirtualAlloc2 for aligned allocation
  if (try_alignment > 0 && (try_alignment % _mi_os_page_size()) == 0 && pVirtualAlloc2 != NULL) {
    MEM_ADDRESS_REQUIREMENTS reqs = { 0, 0, 0 };
    reqs.Alignment = try_alignment;
    MEM_EXTENDED_PARAMETER param = { {0, 0}, {0} };
    param.Type = MemExtendedParameterAddressRequirements;
    param.Pointer = &reqs;
    return (*pVirtualAlloc2)(GetCurrentProcess(), addr, size, flags, PAGE_READWRITE, &param, 1);
  }
#endif
  // last resort
  return VirtualAlloc(addr, size, flags, PAGE_READWRITE);
}

static void* mi_win_virtual_alloc(void* addr, size_t size, size_t try_alignment, DWORD flags, bool large_only, bool allow_large, bool* is_large) {
  mi_assert_internal(!(large_only && !allow_large));
  static _Atomic(uintptr_t) large_page_try_ok; // = 0;
  void* p = NULL;
  if ((large_only || use_large_os_page(size, try_alignment))
      && allow_large && (flags&MEM_COMMIT)!=0 && (flags&MEM_RESERVE)!=0) {
    uintptr_t try_ok = mi_atomic_load_acquire(&large_page_try_ok);
    if (!large_only && try_ok > 0) {
      // if a large page allocation fails, it seems the calls to VirtualAlloc get very expensive.
      // therefore, once a large page allocation failed, we don't try again for `large_page_try_ok` times.
      mi_atomic_cas_strong_acq_rel(&large_page_try_ok, &try_ok, try_ok - 1);
    }
    else {
      // large OS pages must always reserve and commit.
      *is_large = true;
      p = mi_win_virtual_allocx(addr, size, try_alignment, flags | MEM_LARGE_PAGES);
      if (large_only) return p;
      // fall back to non-large page allocation on error (`p == NULL`).
      if (p == NULL) {
        mi_atomic_store_release(&large_page_try_ok,10UL);  // on error, don't try again for the next N allocations
      }
    }
  }
  if (p == NULL) {
    *is_large = ((flags&MEM_LARGE_PAGES) != 0);
    p = mi_win_virtual_allocx(addr, size, try_alignment, flags);
  }
  if (p == NULL) {
    _mi_warning_message("unable to allocate OS memory (%zu bytes, error code: %i, address: %p, large only: %d, allow large: %d)\n", size, GetLastError(), addr, large_only, allow_large);
  }
  return p;
}

#elif defined(__wasi__)
static void* mi_wasm_heap_grow(size_t size, size_t try_alignment) {
  uintptr_t base = __builtin_wasm_memory_size(0) * _mi_os_page_size();
  uintptr_t aligned_base = _mi_align_up(base, (uintptr_t) try_alignment);
  size_t alloc_size = _mi_align_up( aligned_base - base + size, _mi_os_page_size());
  mi_assert(alloc_size >= size && (alloc_size % _mi_os_page_size()) == 0);
  if (alloc_size < size) return NULL;
  if (__builtin_wasm_memory_grow(0, alloc_size / _mi_os_page_size()) == SIZE_MAX) {
    errno = ENOMEM;
    return NULL;
  }
  return (void*)aligned_base;
}
#else
#define MI_OS_USE_MMAP
static void* mi_unix_mmapx(void* addr, size_t size, size_t try_alignment, int protect_flags, int flags, int fd) {
  void* p = NULL;
  #if (MI_INTPTR_SIZE >= 8) && !defined(MAP_ALIGNED)
  // on 64-bit systems, use the virtual address area after 4TiB for 4MiB aligned allocations
  void* hint;
  if (addr == NULL && (hint = mi_os_get_aligned_hint(try_alignment, size)) != NULL) {
    p = mmap(hint,size,protect_flags,flags,fd,0);
    if (p==MAP_FAILED) p = NULL; // fall back to regular mmap
  }
  #else
  UNUSED(try_alignment);
  UNUSED(mi_os_get_aligned_hint);
  #endif
  if (p==NULL) {
    p = mmap(addr,size,protect_flags,flags,fd,0);
    if (p==MAP_FAILED) p = NULL;
  }
  return p;
}

static void* mi_unix_mmap(void* addr, size_t size, size_t try_alignment, int protect_flags, bool large_only, bool allow_large, bool* is_large) {
  void* p = NULL;
  #if !defined(MAP_ANONYMOUS)
  #define MAP_ANONYMOUS  MAP_ANON
  #endif
  #if !defined(MAP_NORESERVE)
  #define MAP_NORESERVE  0
  #endif
  int flags = MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE;
  int fd = -1;
  #if defined(MAP_ALIGNED)  // BSD
  if (try_alignment > 0) {
    size_t n = _mi_bsr(try_alignment);
    if (((size_t)1 << n) == try_alignment && n >= 12 && n <= 30) {  // alignment is a power of 2 and 4096 <= alignment <= 1GiB
      flags |= MAP_ALIGNED(n);
    }
  }
  #endif
  #if defined(PROT_MAX)
  protect_flags |= PROT_MAX(PROT_READ | PROT_WRITE); // BSD
  #endif
  #if defined(VM_MAKE_TAG)
  // macOS: tracking anonymous page with a specific ID. (All up to 98 are taken officially but LLVM sanitizers had taken 99)
  int os_tag = (int)mi_option_get(mi_option_os_tag);
  if (os_tag < 100 || os_tag > 255) os_tag = 100;
  fd = VM_MAKE_TAG(os_tag);
  #endif
  if ((large_only || use_large_os_page(size, try_alignment)) && allow_large) {
    static _Atomic(uintptr_t) large_page_try_ok; // = 0;
    uintptr_t try_ok = mi_atomic_load_acquire(&large_page_try_ok);
    if (!large_only && try_ok > 0) {
      // If the OS is not configured for large OS pages, or the user does not have
      // enough permission, the `mmap` will always fail (but it might also fail for other reasons).
      // Therefore, once a large page allocation failed, we don't try again for `large_page_try_ok` times
      // to avoid too many failing calls to mmap.
      mi_atomic_cas_strong_acq_rel(&large_page_try_ok, &try_ok, try_ok - 1);
    }
    else {
      int lflags = flags & ~MAP_NORESERVE;  // using NORESERVE on huge pages seems to fail on Linux
      int lfd = fd;
      #ifdef MAP_ALIGNED_SUPER
      lflags |= MAP_ALIGNED_SUPER;
      #endif
      #ifdef MAP_HUGETLB
      lflags |= MAP_HUGETLB;
      #endif
      #ifdef MAP_HUGE_1GB
      static bool mi_huge_pages_available = true;
      if ((size % GiB) == 0 && mi_huge_pages_available) {
        lflags |= MAP_HUGE_1GB;
      }
      else
      #endif
      {
        #ifdef MAP_HUGE_2MB
        lflags |= MAP_HUGE_2MB;
        #endif
      }
      #ifdef VM_FLAGS_SUPERPAGE_SIZE_2MB
      lfd |= VM_FLAGS_SUPERPAGE_SIZE_2MB;
      #endif
      if (large_only || lflags != flags) {
        // try large OS page allocation
        *is_large = true;
        p = mi_unix_mmapx(addr, size, try_alignment, protect_flags, lflags, lfd);
        #ifdef MAP_HUGE_1GB
        if (p == NULL && (lflags & MAP_HUGE_1GB) != 0) {
          mi_huge_pages_available = false; // don't try huge 1GiB pages again
          _mi_warning_message("unable to allocate huge (1GiB) page, trying large (2MiB) pages instead (error %i)\n", errno);
          lflags = ((lflags & ~MAP_HUGE_1GB) | MAP_HUGE_2MB);
          p = mi_unix_mmapx(addr, size, try_alignment, protect_flags, lflags, lfd);
        }
        #endif
        if (large_only) return p;
        if (p == NULL) {
          mi_atomic_store_release(&large_page_try_ok, 10UL);  // on error, don't try again for the next N allocations
        }
      }
    }
  }
  if (p == NULL) {
    *is_large = false;
    p = mi_unix_mmapx(addr, size, try_alignment, protect_flags, flags, fd);
    #if defined(MADV_HUGEPAGE)
    // Many Linux systems don't allow MAP_HUGETLB but they support instead
    // transparent huge pages (THP). It is not required to call `madvise` with MADV_HUGE
    // though since properly aligned allocations will already use large pages if available
    // in that case -- in particular for our large regions (in `memory.c`).
    // However, some systems only allow THP if called with explicit `madvise`, so
    // when large OS pages are enabled for mimalloc, we call `madvice` anyways.
    if (allow_large && use_large_os_page(size, try_alignment)) {
      if (madvise(p, size, MADV_HUGEPAGE) == 0) {
        *is_large = true; // possibly
      };
    }
    #endif
    #if defined(__sun)
    if (allow_large && use_large_os_page(size, try_alignment)) {
      struct memcntl_mha cmd = {0};
      cmd.mha_pagesize = large_os_page_size;
      cmd.mha_cmd = MHA_MAPSIZE_VA;
      if (memcntl(p, size, MC_HAT_ADVISE, (caddr_t)&cmd, 0, 0) == 0) {
        *is_large = true;
      }
    }
    #endif
  }
  if (p == NULL) {
    _mi_warning_message("unable to allocate OS memory (%zu bytes, error code: %i, address: %p, large only: %d, allow large: %d)\n", size, errno, addr, large_only, allow_large);
  }
  return p;
}
#endif

// On 64-bit systems, we can do efficient aligned allocation by using
// the 4TiB to 30TiB area to allocate them.
#if (MI_INTPTR_SIZE >= 8) && (defined(_WIN32) || (defined(MI_OS_USE_MMAP) && !defined(MAP_ALIGNED)))
static mi_decl_cache_align _Atomic(uintptr_t) aligned_base;

// Return a 4MiB aligned address that is probably available
static void* mi_os_get_aligned_hint(size_t try_alignment, size_t size) {
  if (try_alignment == 0 || try_alignment > MI_SEGMENT_SIZE) return NULL;
  if ((size%MI_SEGMENT_SIZE) != 0) return NULL;
  uintptr_t hint = mi_atomic_add_acq_rel(&aligned_base, size);
  if (hint == 0 || hint > ((intptr_t)30<<40)) { // try to wrap around after 30TiB (area after 32TiB is used for huge OS pages)
    uintptr_t init = ((uintptr_t)4 << 40); // start at 4TiB area
    #if (MI_SECURE>0 || MI_DEBUG==0)     // security: randomize start of aligned allocations unless in debug mode
    uintptr_t r = _mi_heap_random_next(mi_get_default_heap());
    init = init + (MI_SEGMENT_SIZE * ((r>>17) & 0xFFFFF));  // (randomly 20 bits)*4MiB == 0 to 4TiB
    #endif
    uintptr_t expected = hint + size;
    mi_atomic_cas_strong_acq_rel(&aligned_base, &expected, init);
    hint = mi_atomic_add_acq_rel(&aligned_base, size); // this may still give 0 or > 30TiB but that is ok, it is a hint after all
  }
  if (hint%try_alignment != 0) return NULL;
  return (void*)hint;
}
#else
static void* mi_os_get_aligned_hint(size_t try_alignment, size_t size) {
  UNUSED(try_alignment); UNUSED(size);
  return NULL;
}
#endif


// Primitive allocation from the OS.
// Note: the `try_alignment` is just a hint and the returned pointer is not guaranteed to be aligned.
static void* mi_os_mem_alloc(size_t size, size_t try_alignment, bool commit, bool allow_large, bool* is_large, mi_stats_t* stats) {
  mi_assert_internal(size > 0 && (size % _mi_os_page_size()) == 0);
  if (size == 0) return NULL;
  if (!commit) allow_large = false;

  void* p = NULL;
  /*
  if (commit && allow_large) {
    p = _mi_os_try_alloc_from_huge_reserved(size, try_alignment);
    if (p != NULL) {
      *is_large = true;
      return p;
    }
  }
  */

  #if defined(_WIN32)
    int flags = MEM_RESERVE;
    if (commit) flags |= MEM_COMMIT;
    p = mi_win_virtual_alloc(NULL, size, try_alignment, flags, false, allow_large, is_large);
  #elif defined(__wasi__)
    *is_large = false;
    p = mi_wasm_heap_grow(size, try_alignment);
  #else
    int protect_flags = (commit ? (PROT_WRITE | PROT_READ) : PROT_NONE);
    p = mi_unix_mmap(NULL, size, try_alignment, protect_flags, false, allow_large, is_large);
  #endif
  mi_stat_counter_increase(stats->mmap_calls, 1);
  if (p != NULL) {
    _mi_stat_increase(&stats->reserved, size);
    if (commit) { _mi_stat_increase(&stats->committed, size); }
  }
  return p;
}


// Primitive aligned allocation from the OS.
// This function guarantees the allocated memory is aligned.
static void* mi_os_mem_alloc_aligned(size_t size, size_t alignment, bool commit, bool allow_large, bool* is_large, mi_stats_t* stats) {
  mi_assert_internal(alignment >= _mi_os_page_size() && ((alignment & (alignment - 1)) == 0));
  mi_assert_internal(size > 0 && (size % _mi_os_page_size()) == 0);
  if (!commit) allow_large = false;
  if (!(alignment >= _mi_os_page_size() && ((alignment & (alignment - 1)) == 0))) return NULL;
  size = _mi_align_up(size, _mi_os_page_size());

  // try first with a hint (this will be aligned directly on Win 10+ or BSD)
  void* p = mi_os_mem_alloc(size, alignment, commit, allow_large, is_large, stats);
  if (p == NULL) return NULL;

  // if not aligned, free it, overallocate, and unmap around it
  if (((uintptr_t)p % alignment != 0)) {
    mi_os_mem_free(p, size, commit, stats);
    if (size >= (SIZE_MAX - alignment)) return NULL; // overflow
    size_t over_size = size + alignment;

#if _WIN32
    // over-allocate and than re-allocate exactly at an aligned address in there.
    // this may fail due to threads allocating at the same time so we
    // retry this at most 3 times before giving up.
    // (we can not decommit around the overallocation on Windows, because we can only
    //  free the original pointer, not one pointing inside the area)
    int flags = MEM_RESERVE;
    if (commit) flags |= MEM_COMMIT;
    for (int tries = 0; tries < 3; tries++) {
      // over-allocate to determine a virtual memory range
      p = mi_os_mem_alloc(over_size, alignment, commit, false, is_large, stats);
      if (p == NULL) return NULL; // error
      if (((uintptr_t)p % alignment) == 0) {
        // if p happens to be aligned, just decommit the left-over area
        _mi_os_decommit((uint8_t*)p + size, over_size - size, stats);
        break;
      }
      else {
        // otherwise free and allocate at an aligned address in there
        mi_os_mem_free(p, over_size, commit, stats);
        void* aligned_p = mi_align_up_ptr(p, alignment);
        p = mi_win_virtual_alloc(aligned_p, size, alignment, flags, false, allow_large, is_large);
        if (p == aligned_p) break; // success!
        if (p != NULL) { // should not happen?
          mi_os_mem_free(p, size, commit, stats);
          p = NULL;
        }
      }
    }
#else
    // overallocate...
    p = mi_os_mem_alloc(over_size, alignment, commit, false, is_large, stats);
    if (p == NULL) return NULL;
    // and selectively unmap parts around the over-allocated area.
    void* aligned_p = mi_align_up_ptr(p, alignment);
    size_t pre_size = (uint8_t*)aligned_p - (uint8_t*)p;
    size_t mid_size = _mi_align_up(size, _mi_os_page_size());
    size_t post_size = over_size - pre_size - mid_size;
    mi_assert_internal(pre_size < over_size && post_size < over_size && mid_size >= size);
    if (pre_size > 0)  mi_os_mem_free(p, pre_size, commit, stats);
    if (post_size > 0) mi_os_mem_free((uint8_t*)aligned_p + mid_size, post_size, commit, stats);
    // we can return the aligned pointer on `mmap` systems
    p = aligned_p;
#endif
  }

  mi_assert_internal(p == NULL || (p != NULL && ((uintptr_t)p % alignment) == 0));
  return p;
}

/* -----------------------------------------------------------
  OS API: alloc, free, alloc_aligned
----------------------------------------------------------- */

void* _mi_os_alloc(size_t size, mi_stats_t* tld_stats) {
  UNUSED(tld_stats);
  mi_stats_t* stats = &_mi_stats_main;
  if (size == 0) return NULL;
  size = _mi_os_good_alloc_size(size);
  bool is_large = false;
  return mi_os_mem_alloc(size, 0, true, false, &is_large, stats);
}

void  _mi_os_free_ex(void* p, size_t size, bool was_committed, mi_stats_t* tld_stats) {
  UNUSED(tld_stats);
  mi_stats_t* stats = &_mi_stats_main;
  if (size == 0 || p == NULL) return;
  size = _mi_os_good_alloc_size(size);
  mi_os_mem_free(p, size, was_committed, stats);
}

void  _mi_os_free(void* p, size_t size, mi_stats_t* stats) {
  _mi_os_free_ex(p, size, true, stats);
}

void* _mi_os_alloc_aligned(size_t size, size_t alignment, bool commit, bool* large, mi_os_tld_t* tld)
{
  UNUSED(tld);
  if (size == 0) return NULL;
  size = _mi_os_good_alloc_size(size);
  alignment = _mi_align_up(alignment, _mi_os_page_size());
  bool allow_large = false;
  if (large != NULL) {
    allow_large = *large;
    *large = false;
  }
  return mi_os_mem_alloc_aligned(size, alignment, commit, allow_large, (large!=NULL?large:&allow_large), &_mi_stats_main /*tld->stats*/ );
}



/* -----------------------------------------------------------
  OS memory API: reset, commit, decommit, protect, unprotect.
----------------------------------------------------------- */


// OS page align within a given area, either conservative (pages inside the area only),
// or not (straddling pages outside the area is possible)
static void* mi_os_page_align_areax(bool conservative, void* addr, size_t size, size_t* newsize) {
  mi_assert(addr != NULL && size > 0);
  if (newsize != NULL) *newsize = 0;
  if (size == 0 || addr == NULL) return NULL;

  // page align conservatively within the range
  void* start = (conservative ? mi_align_up_ptr(addr, _mi_os_page_size())
    : mi_align_down_ptr(addr, _mi_os_page_size()));
  void* end = (conservative ? mi_align_down_ptr((uint8_t*)addr + size, _mi_os_page_size())
    : mi_align_up_ptr((uint8_t*)addr + size, _mi_os_page_size()));
  ptrdiff_t diff = (uint8_t*)end - (uint8_t*)start;
  if (diff <= 0) return NULL;

  mi_assert_internal((conservative && (size_t)diff <= size) || (!conservative && (size_t)diff >= size));
  if (newsize != NULL) *newsize = (size_t)diff;
  return start;
}

static void* mi_os_page_align_area_conservative(void* addr, size_t size, size_t* newsize) {
  return mi_os_page_align_areax(true, addr, size, newsize);
}

static void mi_mprotect_hint(int err) {
#if defined(MI_OS_USE_MMAP) && (MI_SECURE>=2) // guard page around every mimalloc page
  if (err == ENOMEM) {
    _mi_warning_message("the previous warning may have been caused by a low memory map limit.\n"
                        "  On Linux this is controlled by the vm.max_map_count. For example:\n"
                        "  > sudo sysctl -w vm.max_map_count=262144\n");
  }
#else
  UNUSED(err);
#endif
}

// Commit/Decommit memory.
// Usually commit is aligned liberal, while decommit is aligned conservative.
// (but not for the reset version where we want commit to be conservative as well)
static bool mi_os_commitx(void* addr, size_t size, bool commit, bool conservative, bool* is_zero, mi_stats_t* stats) {
  // page align in the range, commit liberally, decommit conservative
  if (is_zero != NULL) { *is_zero = false; }
  size_t csize;
  void* start = mi_os_page_align_areax(conservative, addr, size, &csize);
  if (csize == 0) return true;  // || _mi_os_is_huge_reserved(addr))
  int err = 0;
  if (commit) {
    _mi_stat_increase(&stats->committed, size);  // use size for precise commit vs. decommit
    _mi_stat_counter_increase(&stats->commit_calls, 1);
  }
  else {
    _mi_stat_decrease(&stats->committed, size);
  }

  #if defined(_WIN32)
  if (commit) {
    // if the memory was already committed, the call succeeds but it is not zero'd
    // *is_zero = true;
    void* p = VirtualAlloc(start, csize, MEM_COMMIT, PAGE_READWRITE);
    err = (p == start ? 0 : GetLastError());
  }
  else {
    BOOL ok = VirtualFree(start, csize, MEM_DECOMMIT);
    err = (ok ? 0 : GetLastError());
  }
  #elif defined(__wasi__)
  // WebAssembly guests can't control memory protection
  #elif defined(MAP_FIXED)
  if (!commit) {
    // use mmap with MAP_FIXED to discard the existing memory (and reduce commit charge)
    void* p = mmap(start, csize, PROT_NONE, (MAP_FIXED | MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE), -1, 0);
    if (p != start) { err = errno; }
  }
  else {
    // for commit, just change the protection
    err = mprotect(start, csize, (PROT_READ | PROT_WRITE));
    if (err != 0) { err = errno; }
  }
  #else
  err = mprotect(start, csize, (commit ? (PROT_READ | PROT_WRITE) : PROT_NONE));
  if (err != 0) { err = errno; }
  #endif
  if (err != 0) {
    _mi_warning_message("%s error: start: %p, csize: 0x%x, err: %i\n", commit ? "commit" : "decommit", start, csize, err);
    mi_mprotect_hint(err);
  }
  mi_assert_internal(err == 0);
  return (err == 0);
}

bool _mi_os_commit(void* addr, size_t size, bool* is_zero, mi_stats_t* tld_stats) {
  UNUSED(tld_stats);
  mi_stats_t* stats = &_mi_stats_main;
  return mi_os_commitx(addr, size, true, false /* liberal */, is_zero, stats);
}

bool _mi_os_decommit(void* addr, size_t size, mi_stats_t* tld_stats) {
  UNUSED(tld_stats);
  mi_stats_t* stats = &_mi_stats_main;
  bool is_zero;
  return mi_os_commitx(addr, size, false, true /* conservative */, &is_zero, stats);
}

static bool mi_os_commit_unreset(void* addr, size_t size, bool* is_zero, mi_stats_t* stats) {
  return mi_os_commitx(addr, size, true, true /* conservative */, is_zero, stats);
}

// Signal to the OS that the address range is no longer in use
// but may be used later again. This will release physical memory
// pages and reduce swapping while keeping the memory committed.
// We page align to a conservative area inside the range to reset.
static bool mi_os_resetx(void* addr, size_t size, bool reset, mi_stats_t* stats) {
  // page align conservatively within the range
  size_t csize;
  void* start = mi_os_page_align_area_conservative(addr, size, &csize);
  if (csize == 0) return true;  // || _mi_os_is_huge_reserved(addr)
  if (reset) _mi_stat_increase(&stats->reset, csize);
        else _mi_stat_decrease(&stats->reset, csize);
  if (!reset) return true; // nothing to do on unreset!

  #if (MI_DEBUG>1)
  if (MI_SECURE==0) {
    memset(start, 0, csize); // pretend it is eagerly reset
  }
  #endif

#if defined(_WIN32)
  // Testing shows that for us (on `malloc-large`) MEM_RESET is 2x faster than DiscardVirtualMemory
  void* p = VirtualAlloc(start, csize, MEM_RESET, PAGE_READWRITE);
  mi_assert_internal(p == start);
  #if 1
  if (p == start && start != NULL) {
    VirtualUnlock(start,csize); // VirtualUnlock after MEM_RESET removes the memory from the working set
  }
  #endif
  if (p != start) return false;
#else
#if defined(MADV_FREE)
  static _Atomic(uintptr_t) advice = ATOMIC_VAR_INIT(MADV_FREE);
  int err = madvise(start, csize, (int)mi_atomic_load_relaxed(&advice));
  if (err != 0 && errno == EINVAL && advice == MADV_FREE) {
    // if MADV_FREE is not supported, fall back to MADV_DONTNEED from now on
    mi_atomic_store_release(&advice, (uintptr_t)MADV_DONTNEED);
    err = madvise(start, csize, MADV_DONTNEED);
  }
#elif defined(__wasi__)
  int err = 0;
#else
  int err = madvise(start, csize, MADV_DONTNEED);
#endif
  if (err != 0) {
    _mi_warning_message("madvise reset error: start: %p, csize: 0x%x, errno: %i\n", start, csize, errno);
  }
  //mi_assert(err == 0);
  if (err != 0) return false;
#endif
  return true;
}

// Signal to the OS that the address range is no longer in use
// but may be used later again. This will release physical memory
// pages and reduce swapping while keeping the memory committed.
// We page align to a conservative area inside the range to reset.
bool _mi_os_reset(void* addr, size_t size, mi_stats_t* tld_stats) {
  UNUSED(tld_stats);
  mi_stats_t* stats = &_mi_stats_main;
  if (mi_option_is_enabled(mi_option_reset_decommits)) {
    return _mi_os_decommit(addr, size, stats);
  }
  else {
    return mi_os_resetx(addr, size, true, stats);
  }
}

bool _mi_os_unreset(void* addr, size_t size, bool* is_zero, mi_stats_t* tld_stats) {
  UNUSED(tld_stats);
  mi_stats_t* stats = &_mi_stats_main;
  if (mi_option_is_enabled(mi_option_reset_decommits)) {
    return mi_os_commit_unreset(addr, size, is_zero, stats);  // re-commit it (conservatively!)
  }
  else {
    *is_zero = false;
    return mi_os_resetx(addr, size, false, stats);
  }
}


// Protect a region in memory to be not accessible.
static  bool mi_os_protectx(void* addr, size_t size, bool protect) {
  // page align conservatively within the range
  size_t csize = 0;
  void* start = mi_os_page_align_area_conservative(addr, size, &csize);
  if (csize == 0) return false;
  /*
  if (_mi_os_is_huge_reserved(addr)) {
	  _mi_warning_message("cannot mprotect memory allocated in huge OS pages\n");
  }
  */
  int err = 0;
#ifdef _WIN32
  DWORD oldprotect = 0;
  BOOL ok = VirtualProtect(start, csize, protect ? PAGE_NOACCESS : PAGE_READWRITE, &oldprotect);
  err = (ok ? 0 : GetLastError());
#elif defined(__wasi__)
  err = 0;
#else
  err = mprotect(start, csize, protect ? PROT_NONE : (PROT_READ | PROT_WRITE));
  if (err != 0) { err = errno; }
#endif
  if (err != 0) {
    _mi_warning_message("mprotect error: start: %p, csize: 0x%x, err: %i\n", start, csize, err);
    mi_mprotect_hint(err);
  }
  return (err == 0);
}

bool _mi_os_protect(void* addr, size_t size) {
  return mi_os_protectx(addr, size, true);
}

bool _mi_os_unprotect(void* addr, size_t size) {
  return mi_os_protectx(addr, size, false);
}



bool _mi_os_shrink(void* p, size_t oldsize, size_t newsize, mi_stats_t* stats) {
  // page align conservatively within the range
  mi_assert_internal(oldsize > newsize && p != NULL);
  if (oldsize < newsize || p == NULL) return false;
  if (oldsize == newsize) return true;

  // oldsize and newsize should be page aligned or we cannot shrink precisely
  void* addr = (uint8_t*)p + newsize;
  size_t size = 0;
  void* start = mi_os_page_align_area_conservative(addr, oldsize - newsize, &size);
  if (size == 0 || start != addr) return false;

#ifdef _WIN32
  // we cannot shrink on windows, but we can decommit
  return _mi_os_decommit(start, size, stats);
#else
  return mi_os_mem_free(start, size, true, stats);
#endif
}


/* ----------------------------------------------------------------------------
Support for allocating huge OS pages (1Gib) that are reserved up-front
and possibly associated with a specific NUMA node. (use `numa_node>=0`)
-----------------------------------------------------------------------------*/
#define MI_HUGE_OS_PAGE_SIZE  (GiB)

#if defined(_WIN32) && (MI_INTPTR_SIZE >= 8)
static void* mi_os_alloc_huge_os_pagesx(void* addr, size_t size, int numa_node)
{
  mi_assert_internal(size%GiB == 0);
  mi_assert_internal(addr != NULL);
  const DWORD flags = MEM_LARGE_PAGES | MEM_COMMIT | MEM_RESERVE;

  mi_win_enable_large_os_pages();

  #if defined(MEM_EXTENDED_PARAMETER_TYPE_BITS)
  MEM_EXTENDED_PARAMETER params[3] = { {{0,0},{0}},{{0,0},{0}},{{0,0},{0}} };
  // on modern Windows try use NtAllocateVirtualMemoryEx for 1GiB huge pages
  static bool mi_huge_pages_available = true;
  if (pNtAllocateVirtualMemoryEx != NULL && mi_huge_pages_available) {
    #ifndef MEM_EXTENDED_PARAMETER_NONPAGED_HUGE
    #define MEM_EXTENDED_PARAMETER_NONPAGED_HUGE  (0x10)
    #endif
    params[0].Type = 5; // == MemExtendedParameterAttributeFlags;
    params[0].ULong64 = MEM_EXTENDED_PARAMETER_NONPAGED_HUGE;
    ULONG param_count = 1;
    if (numa_node >= 0) {
      param_count++;
      params[1].Type = MemExtendedParameterNumaNode;
      params[1].ULong = (unsigned)numa_node;
    }
    SIZE_T psize = size;
    void* base = addr;
    NTSTATUS err = (*pNtAllocateVirtualMemoryEx)(GetCurrentProcess(), &base, &psize, flags, PAGE_READWRITE, params, param_count);
    if (err == 0 && base != NULL) {
      return base;
    }
    else {
      // fall back to regular large pages
      mi_huge_pages_available = false; // don't try further huge pages
      _mi_warning_message("unable to allocate using huge (1gb) pages, trying large (2mb) pages instead (status 0x%lx)\n", err);
    }
  }
  // on modern Windows try use VirtualAlloc2 for numa aware large OS page allocation
  if (pVirtualAlloc2 != NULL && numa_node >= 0) {
    params[0].Type = MemExtendedParameterNumaNode;
    params[0].ULong = (unsigned)numa_node;
    return (*pVirtualAlloc2)(GetCurrentProcess(), addr, size, flags, PAGE_READWRITE, params, 1);
  }
  #else
    UNUSED(numa_node);
  #endif
  // otherwise use regular virtual alloc on older windows
  return VirtualAlloc(addr, size, flags, PAGE_READWRITE);
}

#elif defined(MI_OS_USE_MMAP) && (MI_INTPTR_SIZE >= 8) && !defined(__HAIKU__)
#include <sys/syscall.h>
#ifndef MPOL_PREFERRED
#define MPOL_PREFERRED 1
#endif
#if defined(SYS_mbind)
static long mi_os_mbind(void* start, unsigned long len, unsigned long mode, const unsigned long* nmask, unsigned long maxnode, unsigned flags) {
  return syscall(SYS_mbind, start, len, mode, nmask, maxnode, flags);
}
#else
static long mi_os_mbind(void* start, unsigned long len, unsigned long mode, const unsigned long* nmask, unsigned long maxnode, unsigned flags) {
  UNUSED(start); UNUSED(len); UNUSED(mode); UNUSED(nmask); UNUSED(maxnode); UNUSED(flags);
  return 0;
}
#endif
static void* mi_os_alloc_huge_os_pagesx(void* addr, size_t size, int numa_node) {
  mi_assert_internal(size%GiB == 0);
  bool is_large = true;
  void* p = mi_unix_mmap(addr, size, MI_SEGMENT_SIZE, PROT_READ | PROT_WRITE, true, true, &is_large);
  if (p == NULL) return NULL;
  if (numa_node >= 0 && numa_node < 8*MI_INTPTR_SIZE) { // at most 64 nodes
    uintptr_t numa_mask = (1UL << numa_node);
    // TODO: does `mbind` work correctly for huge OS pages? should we
    // use `set_mempolicy` before calling mmap instead?
    // see: <https://lkml.org/lkml/2017/2/9/875>
    long err = mi_os_mbind(p, size, MPOL_PREFERRED, &numa_mask, 8*MI_INTPTR_SIZE, 0);
    if (err != 0) {
      _mi_warning_message("failed to bind huge (1gb) pages to numa node %d: %s\n", numa_node, strerror(errno));
    }
  }
  return p;
}
#else
static void* mi_os_alloc_huge_os_pagesx(void* addr, size_t size, int numa_node) {
  UNUSED(addr); UNUSED(size); UNUSED(numa_node);
  return NULL;
}
#endif

#if (MI_INTPTR_SIZE >= 8)
// To ensure proper alignment, use our own area for huge OS pages
static mi_decl_cache_align _Atomic(uintptr_t)  mi_huge_start; // = 0

// Claim an aligned address range for huge pages
static uint8_t* mi_os_claim_huge_pages(size_t pages, size_t* total_size) {
  if (total_size != NULL) *total_size = 0;
  const size_t size = pages * MI_HUGE_OS_PAGE_SIZE;

  uintptr_t start = 0;
  uintptr_t end = 0;
  uintptr_t huge_start = mi_atomic_load_relaxed(&mi_huge_start);
  do {
    start = huge_start;
    if (start == 0) {
      // Initialize the start address after the 32TiB area
      start = ((uintptr_t)32 << 40);  // 32TiB virtual start address
#if (MI_SECURE>0 || MI_DEBUG==0)      // security: randomize start of huge pages unless in debug mode
      uintptr_t r = _mi_heap_random_next(mi_get_default_heap());
      start = start + ((uintptr_t)MI_HUGE_OS_PAGE_SIZE * ((r>>17) & 0x0FFF));  // (randomly 12bits)*1GiB == between 0 to 4TiB
#endif
    }
    end = start + size;
    mi_assert_internal(end % MI_SEGMENT_SIZE == 0);
  } while (!mi_atomic_cas_strong_acq_rel(&mi_huge_start, &huge_start, end));

  if (total_size != NULL) *total_size = size;
  return (uint8_t*)start;
}
#else
static uint8_t* mi_os_claim_huge_pages(size_t pages, size_t* total_size) {
  UNUSED(pages);
  if (total_size != NULL) *total_size = 0;
  return NULL;
}
#endif

// Allocate MI_SEGMENT_SIZE aligned huge pages
void* _mi_os_alloc_huge_os_pages(size_t pages, int numa_node, mi_msecs_t max_msecs, size_t* pages_reserved, size_t* psize) {
  if (psize != NULL) *psize = 0;
  if (pages_reserved != NULL) *pages_reserved = 0;
  size_t size = 0;
  uint8_t* start = mi_os_claim_huge_pages(pages, &size);
  if (start == NULL) return NULL; // or 32-bit systems

  // Allocate one page at the time but try to place them contiguously
  // We allocate one page at the time to be able to abort if it takes too long
  // or to at least allocate as many as available on the system.
  mi_msecs_t start_t = _mi_clock_start();
  size_t page;
  for (page = 0; page < pages; page++) {
    // allocate a page
    void* addr = start + (page * MI_HUGE_OS_PAGE_SIZE);
    void* p = mi_os_alloc_huge_os_pagesx(addr, MI_HUGE_OS_PAGE_SIZE, numa_node);

    // Did we succeed at a contiguous address?
    if (p != addr) {
      // no success, issue a warning and break
      if (p != NULL) {
        _mi_warning_message("could not allocate contiguous huge page %zu at %p\n", page, addr);
        _mi_os_free(p, MI_HUGE_OS_PAGE_SIZE, &_mi_stats_main);
      }
      break;
    }

    // success, record it
    _mi_stat_increase(&_mi_stats_main.committed, MI_HUGE_OS_PAGE_SIZE);
    _mi_stat_increase(&_mi_stats_main.reserved, MI_HUGE_OS_PAGE_SIZE);

    // check for timeout
    if (max_msecs > 0) {
      mi_msecs_t elapsed = _mi_clock_end(start_t);
      if (page >= 1) {
        mi_msecs_t estimate = ((elapsed / (page+1)) * pages);
        if (estimate > 2*max_msecs) { // seems like we are going to timeout, break
          elapsed = max_msecs + 1;
        }
      }
      if (elapsed > max_msecs) {
        _mi_warning_message("huge page allocation timed out\n");
        break;
      }
    }
  }
  mi_assert_internal(page*MI_HUGE_OS_PAGE_SIZE <= size);
  if (pages_reserved != NULL) *pages_reserved = page;
  if (psize != NULL) *psize = page * MI_HUGE_OS_PAGE_SIZE;
  return (page == 0 ? NULL : start);
}

// free every huge page in a range individually (as we allocated per page)
// note: needed with VirtualAlloc but could potentially be done in one go on mmap'd systems.
void _mi_os_free_huge_pages(void* p, size_t size, mi_stats_t* stats) {
  if (p==NULL || size==0) return;
  uint8_t* base = (uint8_t*)p;
  while (size >= MI_HUGE_OS_PAGE_SIZE) {
    _mi_os_free(base, MI_HUGE_OS_PAGE_SIZE, stats);
    size -= MI_HUGE_OS_PAGE_SIZE;
  }
}

/* ----------------------------------------------------------------------------
Support NUMA aware allocation
-----------------------------------------------------------------------------*/
#ifdef _WIN32
static size_t mi_os_numa_nodex() {
  USHORT numa_node = 0;
  if (pGetCurrentProcessorNumberEx != NULL && pGetNumaProcessorNodeEx != NULL) {
    // Extended API is supported
    PROCESSOR_NUMBER pnum;
    (*pGetCurrentProcessorNumberEx)(&pnum);
    USHORT nnode = 0;
    BOOL ok = (*pGetNumaProcessorNodeEx)(&pnum, &nnode);
    if (ok) numa_node = nnode;
  }
  else {
    // Vista or earlier, use older API that is limited to 64 processors. Issue #277
    DWORD pnum = GetCurrentProcessorNumber();
    UCHAR nnode = 0;
    BOOL ok = GetNumaProcessorNode((UCHAR)pnum, &nnode);
    if (ok) numa_node = nnode;
  }
  return numa_node;
}

static size_t mi_os_numa_node_countx(void) {
  ULONG numa_max = 0;
  GetNumaHighestNodeNumber(&numa_max);
  // find the highest node number that has actual processors assigned to it. Issue #282
  while(numa_max > 0) {
    if (pGetNumaNodeProcessorMaskEx != NULL) {
      // Extended API is supported
      GROUP_AFFINITY affinity;
      if ((*pGetNumaNodeProcessorMaskEx)((USHORT)numa_max, &affinity)) {
        if (affinity.Mask != 0) break;  // found the maximum non-empty node
      }
    }
    else {
      // Vista or earlier, use older API that is limited to 64 processors.
      ULONGLONG mask;
      if (GetNumaNodeProcessorMask((UCHAR)numa_max, &mask)) {
        if (mask != 0) break; // found the maximum non-empty node
      };
    }
    // max node was invalid or had no processor assigned, try again
    numa_max--;
  }
  return ((size_t)numa_max + 1);
}
#elif defined(__linux__)
#include <sys/syscall.h>  // getcpu
#include <stdio.h>        // access

static size_t mi_os_numa_nodex(void) {
#ifdef SYS_getcpu
  unsigned long node = 0;
  unsigned long ncpu = 0;
  long err = syscall(SYS_getcpu, &ncpu, &node, NULL);
  if (err != 0) return 0;
  return node;
#else
  return 0;
#endif
}
static size_t mi_os_numa_node_countx(void) {
  char buf[128];
  unsigned node = 0;
  for(node = 0; node < 256; node++) {
    // enumerate node entries -- todo: it there a more efficient way to do this? (but ensure there is no allocation)
    snprintf(buf, 127, "/sys/devices/system/node/node%u", node + 1);
    if (access(buf,R_OK) != 0) break;
  }
  return (node+1);
}
#else
static size_t mi_os_numa_nodex(void) {
  return 0;
}
static size_t mi_os_numa_node_countx(void) {
  return 1;
}
#endif

size_t _mi_numa_node_count = 0;   // cache the node count

size_t _mi_os_numa_node_count_get(void) {
  if (mi_unlikely(_mi_numa_node_count <= 0)) {
    long ncount = mi_option_get(mi_option_use_numa_nodes); // given explicitly?
    if (ncount <= 0) ncount = (long)mi_os_numa_node_countx();        // or detect dynamically
    _mi_numa_node_count = (size_t)(ncount <= 0 ? 1 : ncount);
    _mi_verbose_message("using %zd numa regions\n", _mi_numa_node_count);
  }
  mi_assert_internal(_mi_numa_node_count >= 1);
  return _mi_numa_node_count;
}

int _mi_os_numa_node_get(mi_os_tld_t* tld) {
  UNUSED(tld);
  size_t numa_count = _mi_os_numa_node_count();
  if (numa_count<=1) return 0; // optimize on single numa node systems: always node 0
  // never more than the node count and >= 0
  size_t numa_node = mi_os_numa_nodex();
  if (numa_node >= numa_count) { numa_node = numa_node % numa_count; }
  return (int)numa_node;
}
