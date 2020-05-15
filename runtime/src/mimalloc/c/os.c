/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/
#ifndef _DEFAULT_SOURCE
#define _DEFAULT_SOURCE   // ensure mmap flags are defined
#endif

#include "mimalloc.h"
#include "mimalloc-internal.h"
#include "mimalloc-atomic.h"

#include <string.h>  // strerror
#include <errno.h>

#if defined(_WIN32)
#include <windows.h>
#elif defined(__wasi__)
// stdlib.h is all we need, and has already been included in mimalloc.h
#else
#include <sys/mman.h>  // mmap
#include <unistd.h>    // sysconf
#if defined(__linux__)
#include <linux/mman.h> // linux mmap flags
#endif
#if defined(__APPLE__)
#include <mach/vm_statistics.h>
#endif
#endif

/* -----------------------------------------------------------
  Initialization.
  On windows initializes support for aligned allocation and
  large OS pages (if MIMALLOC_LARGE_OS_PAGES is true).
----------------------------------------------------------- */
bool    _mi_os_decommit(void* addr, size_t size, mi_stats_t* stats);
bool    _mi_os_is_huge_reserved(void* p);
void*   _mi_os_try_alloc_from_huge_reserved(size_t size, size_t try_alignment);

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
// We hide MEM_EXTENDED_PARAMETER to compile with older SDK's.
#include <winternl.h>
typedef PVOID    (__stdcall *PVirtualAlloc2)(HANDLE, PVOID, SIZE_T, ULONG, ULONG, /* MEM_EXTENDED_PARAMETER* */ void*, ULONG);
typedef NTSTATUS (__stdcall *PNtAllocateVirtualMemoryEx)(HANDLE, PVOID*, SIZE_T*, ULONG, ULONG, /* MEM_EXTENDED_PARAMETER* */ PVOID, ULONG);
static PVirtualAlloc2 pVirtualAlloc2 = NULL;
static PNtAllocateVirtualMemoryEx pNtAllocateVirtualMemoryEx = NULL;

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
  hDll = LoadLibrary(TEXT("ntdll.dll"));
  if (hDll != NULL) {    
    pNtAllocateVirtualMemoryEx = (PNtAllocateVirtualMemoryEx)(void (*)(void))GetProcAddress(hDll, "NtAllocateVirtualMemoryEx");
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
  if (mi_option_is_enabled(mi_option_large_os_pages)) {
    large_os_page_size = (1UL << 21); // 2MiB
  }
}
#endif


/* -----------------------------------------------------------
  Raw allocation on Windows (VirtualAlloc) and Unix's (mmap).
----------------------------------------------------------- */

static bool mi_os_mem_free(void* addr, size_t size, bool was_committed, mi_stats_t* stats)
{
  if (addr == NULL || size == 0 || _mi_os_is_huge_reserved(addr)) return true;
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
#pragma warning(suppress:4996)
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
#if defined(MEM_EXTENDED_PARAMETER_TYPE_BITS)
  // on modern Windows try use NtAllocateVirtualMemoryEx for 1GiB huge pages
  if ((size % ((uintptr_t)1 << 30)) == 0 /* 1GiB multiple */
    && (flags & MEM_LARGE_PAGES) != 0 && (flags & MEM_COMMIT) != 0 && (flags & MEM_RESERVE) != 0
    && (addr != NULL || try_alignment == 0 || try_alignment % _mi_os_page_size() == 0)
    && pNtAllocateVirtualMemoryEx != NULL)
  {
    #ifndef MEM_EXTENDED_PARAMETER_NONPAGED_HUGE
    #define MEM_EXTENDED_PARAMETER_NONPAGED_HUGE  (0x10)
    #endif
    MEM_EXTENDED_PARAMETER param = { 0, 0 };
    param.Type = 5; // == MemExtendedParameterAttributeFlags;
    param.ULong64 = MEM_EXTENDED_PARAMETER_NONPAGED_HUGE;
    SIZE_T psize = size;
    void*  base  = addr;
    NTSTATUS err = (*pNtAllocateVirtualMemoryEx)(GetCurrentProcess(), &base, &psize, flags, PAGE_READWRITE, &param, 1);
    if (err == 0) {
      return base;
    }
    else {
      // else fall back to regular large OS pages
      _mi_warning_message("unable to allocate huge (1GiB) page, trying large (2MiB) pages instead (error 0x%lx)\n", err);
    }
  }
#else
  UNUSED(try_alignment);
#endif
#if (MI_INTPTR_SIZE >= 8) 
  // on 64-bit systems, try to use the virtual address area after 4TiB for 4MiB aligned allocations
  void* hint;
  if (addr == NULL && (hint = mi_os_get_aligned_hint(try_alignment,size)) != NULL) {
    return VirtualAlloc(hint, size, flags, PAGE_READWRITE);
  }
#endif
#if defined(MEM_EXTENDED_PARAMETER_TYPE_BITS)  
  // on modern Windows try use VirtualAlloc2 for aligned allocation
  if (try_alignment > 0 && (try_alignment % _mi_os_page_size()) == 0 && pVirtualAlloc2 != NULL) {
    MEM_ADDRESS_REQUIREMENTS reqs = { 0 };
    reqs.Alignment = try_alignment;
    MEM_EXTENDED_PARAMETER param = { 0 };
    param.Type = MemExtendedParameterAddressRequirements;
    param.Pointer = &reqs;
    return (*pVirtualAlloc2)(GetCurrentProcess(), addr, size, flags, PAGE_READWRITE, &param, 1);
  }
#endif
  return VirtualAlloc(addr, size, flags, PAGE_READWRITE);
}

static void* mi_win_virtual_alloc(void* addr, size_t size, size_t try_alignment, DWORD flags, bool large_only, bool allow_large, bool* is_large) {
  mi_assert_internal(!(large_only && !allow_large));
  static volatile _Atomic(uintptr_t) large_page_try_ok; // = 0;
  void* p = NULL;
  if ((large_only || use_large_os_page(size, try_alignment)) 
      && allow_large && (flags&MEM_COMMIT)!=0 && (flags&MEM_RESERVE)!=0) {
    uintptr_t try_ok = mi_atomic_read(&large_page_try_ok);
    if (!large_only && try_ok > 0) {
      // if a large page allocation fails, it seems the calls to VirtualAlloc get very expensive.
      // therefore, once a large page allocation failed, we don't try again for `large_page_try_ok` times.
      mi_atomic_cas_weak(&large_page_try_ok, try_ok - 1, try_ok);
    }
    else {
      // large OS pages must always reserve and commit.
      *is_large = true;
      p = mi_win_virtual_allocx(addr, size, try_alignment, flags | MEM_LARGE_PAGES);
      if (large_only) return p;
      // fall back to non-large page allocation on error (`p == NULL`).
      if (p == NULL) {
        mi_atomic_write(&large_page_try_ok,10);  // on error, don't try again for the next N allocations
      }
    }
  }
  if (p == NULL) {
    *is_large = ((flags&MEM_LARGE_PAGES) != 0);
    p = mi_win_virtual_allocx(addr, size, try_alignment, flags);
  }
  if (p == NULL) {
    _mi_warning_message("unable to allocate memory: error code: %i, addr: %p, size: 0x%x, large only: %d, allow_large: %d\n", GetLastError(), addr, size, large_only, allow_large);
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
  int flags = MAP_PRIVATE | MAP_ANONYMOUS;
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
    static volatile _Atomic(uintptr_t) large_page_try_ok; // = 0;
    uintptr_t try_ok = mi_atomic_read(&large_page_try_ok);
    if (!large_only && try_ok > 0) {
      // If the OS is not configured for large OS pages, or the user does not have
      // enough permission, the `mmap` will always fail (but it might also fail for other reasons).
      // Therefore, once a large page allocation failed, we don't try again for `large_page_try_ok` times
      // to avoid too many failing calls to mmap.
      mi_atomic_cas_weak(&large_page_try_ok, try_ok - 1, try_ok);
    }
    else {
      int lflags = flags;
      int lfd = fd;
      #ifdef MAP_ALIGNED_SUPER
      lflags |= MAP_ALIGNED_SUPER;
      #endif
      #ifdef MAP_HUGETLB
      lflags |= MAP_HUGETLB;
      #endif
      #ifdef MAP_HUGE_1GB
      if ((size % ((uintptr_t)1 << 30)) == 0) {
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
          _mi_warning_message("unable to allocate huge (1GiB) page, trying large (2MiB) pages instead (error %i)\n", errno);
          lflags = ((lflags & ~MAP_HUGE_1GB) | MAP_HUGE_2MB);
          p = mi_unix_mmapx(addr, size, try_alignment, protect_flags, lflags, lfd);
        }
        #endif
        if (large_only) return p;
        if (p == NULL) {
          mi_atomic_write(&large_page_try_ok, 10);  // on error, don't try again for the next N allocations
        }
      }
    }
  }
  if (p == NULL) {
    *is_large = false;
    p = mi_unix_mmapx(addr, size, try_alignment, protect_flags, flags, fd);    
    #if defined(MADV_HUGEPAGE)
    // Many Linux systems don't allow MAP_HUGETLB but they support instead
    // transparent huge pages (TPH). It is not required to call `madvise` with MADV_HUGE
    // though since properly aligned allocations will already use large pages if available
    // in that case -- in particular for our large regions (in `memory.c`).
    // However, some systems only allow TPH if called with explicit `madvise`, so
    // when large OS pages are enabled for mimalloc, we call `madvice` anyways.
    if (allow_large && use_large_os_page(size, try_alignment)) {
      if (madvise(p, size, MADV_HUGEPAGE) == 0) {
        *is_large = true; // possibly
      };
    }
    #endif
  }
  return p;
}
#endif

// On 64-bit systems, we can do efficient aligned allocation by using 
// the 4TiB to 30TiB area to allocate them.
#if (MI_INTPTR_SIZE >= 8) && (defined(_WIN32) || (defined(MI_OS_USE_MMAP) && !defined(MAP_ALIGNED)))
static volatile _Atomic(intptr_t) aligned_base;

// Return a 4MiB aligned address that is probably available
static void* mi_os_get_aligned_hint(size_t try_alignment, size_t size) {
  if (try_alignment == 0 || try_alignment > MI_SEGMENT_SIZE) return NULL;
  if ((size%MI_SEGMENT_SIZE) != 0) return NULL;
  intptr_t hint = mi_atomic_add(&aligned_base, size);
  if (hint == 0 || hint > ((intptr_t)30<<40)) { // try to wrap around after 30TiB (area after 32TiB is used for huge OS pages)
    intptr_t init = ((intptr_t)4 << 40); // start at 4TiB area
    #if (MI_SECURE>0 || MI_DEBUG==0)     // security: randomize start of aligned allocations unless in debug mode
    uintptr_t r = _mi_random_init((uintptr_t)&mi_os_get_aligned_hint ^ hint);
    init = init + (MI_SEGMENT_SIZE * ((r>>17) & 0xFFFF));  // (randomly 0-64k)*4MiB == 0 to 256GiB
    #endif
    mi_atomic_cas_strong(mi_atomic_cast(uintptr_t, &aligned_base), init, hint + size);
    hint = mi_atomic_add(&aligned_base, size); // this may still give 0 or > 30TiB but that is ok, it is a hint after all
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

void* _mi_os_alloc(size_t size, mi_stats_t* stats) {
  if (size == 0) return NULL;
  size = _mi_os_good_alloc_size(size);
  bool is_large = false;
  return mi_os_mem_alloc(size, 0, true, false, &is_large, stats);
}

void  _mi_os_free_ex(void* p, size_t size, bool was_committed, mi_stats_t* stats) {
  if (size == 0 || p == NULL) return;
  size = _mi_os_good_alloc_size(size);
  mi_os_mem_free(p, size, was_committed, stats);
}

void  _mi_os_free(void* p, size_t size, mi_stats_t* stats) {
  _mi_os_free_ex(p, size, true, stats);
}

void* _mi_os_alloc_aligned(size_t size, size_t alignment, bool commit, bool* large, mi_os_tld_t* tld)
{
  if (size == 0) return NULL;
  size = _mi_os_good_alloc_size(size);
  alignment = _mi_align_up(alignment, _mi_os_page_size());
  bool allow_large = false;
  if (large != NULL) {
    allow_large = *large;
    *large = false;
  }
  return mi_os_mem_alloc_aligned(size, alignment, commit, allow_large, (large!=NULL?large:&allow_large), tld->stats);
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

// Commit/Decommit memory.
// Usuelly commit is aligned liberal, while decommit is aligned conservative.
// (but not for the reset version where we want commit to be conservative as well)
static bool mi_os_commitx(void* addr, size_t size, bool commit, bool conservative, bool* is_zero, mi_stats_t* stats) {
  // page align in the range, commit liberally, decommit conservative
  *is_zero = false;
  size_t csize;
  void* start = mi_os_page_align_areax(conservative, addr, size, &csize);
  if (csize == 0 || _mi_os_is_huge_reserved(addr)) return true;
  int err = 0;
  if (commit) {
    _mi_stat_increase(&stats->committed, csize);
    _mi_stat_counter_increase(&stats->commit_calls, 1);
  }
  else {
    _mi_stat_decrease(&stats->committed, csize);
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
  #else
  err = mprotect(start, csize, (commit ? (PROT_READ | PROT_WRITE) : PROT_NONE));
  if (err != 0) { err = errno; }
  #endif
  if (err != 0) {
    _mi_warning_message("commit/decommit error: start: 0x%p, csize: 0x%x, err: %i\n", start, csize, err);
  }
  mi_assert_internal(err == 0);
  return (err == 0);
}

bool _mi_os_commit(void* addr, size_t size, bool* is_zero, mi_stats_t* stats) {
  return mi_os_commitx(addr, size, true, false /* conservative? */, is_zero, stats);
}

bool _mi_os_decommit(void* addr, size_t size, mi_stats_t* stats) {
  bool is_zero;
  return mi_os_commitx(addr, size, false, true /* conservative? */, &is_zero, stats);
}

bool _mi_os_commit_unreset(void* addr, size_t size, bool* is_zero, mi_stats_t* stats) {
  return mi_os_commitx(addr, size, true, true /* conservative? */, is_zero, stats);
}


// Signal to the OS that the address range is no longer in use
// but may be used later again. This will release physical memory
// pages and reduce swapping while keeping the memory committed.
// We page align to a conservative area inside the range to reset.
static bool mi_os_resetx(void* addr, size_t size, bool reset, mi_stats_t* stats) {
  // page align conservatively within the range
  size_t csize;
  void* start = mi_os_page_align_area_conservative(addr, size, &csize);
  if (csize == 0 || _mi_os_is_huge_reserved(addr)) return true;
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
  static int advice = MADV_FREE;
  int err = madvise(start, csize, advice);
  if (err != 0 && errno == EINVAL && advice == MADV_FREE) {
    // if MADV_FREE is not supported, fall back to MADV_DONTNEED from now on
    advice = MADV_DONTNEED;
    err = madvise(start, csize, advice);
  }
#elif defined(__wasi__)
  int err = 0;
#else
  int err = madvise(start, csize, MADV_DONTNEED);
#endif
  if (err != 0) {
    _mi_warning_message("madvise reset error: start: 0x%p, csize: 0x%x, errno: %i\n", start, csize, errno);
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
bool _mi_os_reset(void* addr, size_t size, mi_stats_t* stats) {
  if (mi_option_is_enabled(mi_option_reset_decommits)) {
    return _mi_os_decommit(addr,size,stats);
  }
  else {
    return mi_os_resetx(addr, size, true, stats);
  }
}

bool _mi_os_unreset(void* addr, size_t size, bool* is_zero, mi_stats_t* stats) {
  if (mi_option_is_enabled(mi_option_reset_decommits)) {
    return _mi_os_commit_unreset(addr, size, is_zero, stats);  // re-commit it (conservatively!)
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
  if (_mi_os_is_huge_reserved(addr)) {
	  _mi_warning_message("cannot mprotect memory allocated in huge OS pages\n");
  }
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
    _mi_warning_message("mprotect error: start: 0x%p, csize: 0x%x, err: %i\n", start, csize, err);
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
Support for huge OS pages (1Gib) that are reserved up-front and never
released. Only regions are allocated in here (see `memory.c`) so the memory
will be reused.
-----------------------------------------------------------------------------*/
#define MI_HUGE_OS_PAGE_SIZE ((size_t)1 << 30)  // 1GiB

typedef struct mi_huge_info_s {
  volatile _Atomic(void*)  start;     // start of huge page area (32TiB)
  volatile _Atomic(size_t) reserved;  // total reserved size
  volatile _Atomic(size_t) used;      // currently allocated
} mi_huge_info_t;

static mi_huge_info_t os_huge_reserved = { NULL, 0, ATOMIC_VAR_INIT(0) };

bool _mi_os_is_huge_reserved(void* p) {
  return (mi_atomic_read_ptr(&os_huge_reserved.start) != NULL && 
          p >= mi_atomic_read_ptr(&os_huge_reserved.start) &&
          (uint8_t*)p < (uint8_t*)mi_atomic_read_ptr(&os_huge_reserved.start) + mi_atomic_read(&os_huge_reserved.reserved));
}

void* _mi_os_try_alloc_from_huge_reserved(size_t size, size_t try_alignment)
{
  // only allow large aligned allocations (e.g. regions)
  if (size < MI_SEGMENT_SIZE || (size % MI_SEGMENT_SIZE) != 0) return NULL;
  if (try_alignment > MI_SEGMENT_SIZE) return NULL;  
  if (mi_atomic_read_ptr(&os_huge_reserved.start)==NULL) return NULL;
  if (mi_atomic_read(&os_huge_reserved.used) >= mi_atomic_read(&os_huge_reserved.reserved)) return NULL; // already full

  // always aligned
  mi_assert_internal(mi_atomic_read(&os_huge_reserved.used) % MI_SEGMENT_SIZE == 0 );
  mi_assert_internal( (uintptr_t)mi_atomic_read_ptr(&os_huge_reserved.start) % MI_SEGMENT_SIZE == 0 );
  
  // try to reserve space
  size_t base = mi_atomic_addu( &os_huge_reserved.used, size );
  if ((base + size) > os_huge_reserved.reserved) {
    // "free" our over-allocation
    mi_atomic_subu( &os_huge_reserved.used, size);
    return NULL;
  }

  // success!
  uint8_t* p = (uint8_t*)mi_atomic_read_ptr(&os_huge_reserved.start) + base;
  mi_assert_internal( (uintptr_t)p % MI_SEGMENT_SIZE == 0 );
  return p;
}

/*
static void mi_os_free_huge_reserved() {
  uint8_t* addr = os_huge_reserved.start;
  size_t total  = os_huge_reserved.reserved;
  os_huge_reserved.reserved = 0;
  os_huge_reserved.start = NULL;
  for( size_t current = 0; current < total; current += MI_HUGE_OS_PAGE_SIZE) {
    _mi_os_free(addr + current, MI_HUGE_OS_PAGE_SIZE, &_mi_stats_main);
  }
}
*/

#if !(MI_INTPTR_SIZE >= 8 && (defined(_WIN32) || defined(MI_OS_USE_MMAP)))
int mi_reserve_huge_os_pages(size_t pages, double max_secs, size_t* pages_reserved) mi_attr_noexcept {
  UNUSED(pages); UNUSED(max_secs);
  if (pages_reserved != NULL) *pages_reserved = 0;
  return ENOMEM; 
}
#else
int mi_reserve_huge_os_pages( size_t pages, double max_secs, size_t* pages_reserved ) mi_attr_noexcept
{
  if (pages_reserved != NULL) *pages_reserved = 0;
  if (max_secs==0) return ETIMEDOUT; // timeout 
  if (pages==0) return 0;            // ok
  if (!mi_atomic_cas_ptr_strong(&os_huge_reserved.start,(void*)1,NULL)) return ETIMEDOUT; // already reserved

  // Set the start address after the 32TiB area
  uint8_t* start = (uint8_t*)((uintptr_t)32 << 40); // 32TiB virtual start address
  #if (MI_SECURE>0 || MI_DEBUG==0)     // security: randomize start of huge pages unless in debug mode
  uintptr_t r = _mi_random_init((uintptr_t)&mi_reserve_huge_os_pages);
  start = start + ((uintptr_t)MI_HUGE_OS_PAGE_SIZE * ((r>>17) & 0x3FF));  // (randomly 0-1024)*1GiB == 0 to 1TiB
  #endif

  // Allocate one page at the time but try to place them contiguously
  // We allocate one page at the time to be able to abort if it takes too long
  double start_t = _mi_clock_start();
  uint8_t* addr = start;  // current top of the allocations
  for (size_t page = 0; page < pages; page++, addr += MI_HUGE_OS_PAGE_SIZE ) {
    // allocate a page
    void* p = NULL; 
    bool is_large = true;
    #ifdef _WIN32
    if (page==0) { mi_win_enable_large_os_pages(); }
    p = mi_win_virtual_alloc(addr, MI_HUGE_OS_PAGE_SIZE, 0, MEM_LARGE_PAGES | MEM_COMMIT | MEM_RESERVE, true, true, &is_large);
    #elif defined(MI_OS_USE_MMAP)
    p = mi_unix_mmap(addr, MI_HUGE_OS_PAGE_SIZE, 0, PROT_READ | PROT_WRITE, true, true, &is_large);
    #else 
    // always fail
    #endif  
    
    // Did we succeed at a contiguous address?
    if (p != addr) {
      // no success, issue a warning and return with an error 
      if (p != NULL) {
        _mi_warning_message("could not allocate contiguous huge page %zu at 0x%p\n", page, addr); 
        _mi_os_free(p, MI_HUGE_OS_PAGE_SIZE, &_mi_stats_main );
      }
      else {
        #ifdef _WIN32
        int err = GetLastError();
        #else
        int err = errno;
        #endif
        _mi_warning_message("could not allocate huge page %zu at 0x%p, error: %i\n", page, addr, err);
      }
      return ENOMEM;  
    }
    // success, record it
    if (page==0) {
      mi_atomic_write_ptr(&os_huge_reserved.start, addr);  // don't switch the order of these writes
      mi_atomic_write(&os_huge_reserved.reserved, MI_HUGE_OS_PAGE_SIZE);
    }
    else {
      mi_atomic_addu(&os_huge_reserved.reserved,MI_HUGE_OS_PAGE_SIZE);
    }
    _mi_stat_increase(&_mi_stats_main.committed, MI_HUGE_OS_PAGE_SIZE); 
    _mi_stat_increase(&_mi_stats_main.reserved, MI_HUGE_OS_PAGE_SIZE);
    if (pages_reserved != NULL) { *pages_reserved = page + 1; }

    // check for timeout
    double elapsed = _mi_clock_end(start_t);
    if (elapsed > max_secs) return ETIMEDOUT; 
    if (page >= 1) {
      double estimate = ((elapsed / (double)(page+1)) * (double)pages);
      if (estimate > 1.5*max_secs) return ETIMEDOUT; // seems like we are going to timeout
    }
  }  
  _mi_verbose_message("reserved %zu huge pages\n", pages);
  return 0;
}
#endif

