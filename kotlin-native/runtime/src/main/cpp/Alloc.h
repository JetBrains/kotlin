/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef RUNTIME_ALLOC_H
#define RUNTIME_ALLOC_H

#include <stddef.h>
#include <stdlib.h>

#include <new>
#include <utility>

#include "Porting.h"

inline void* konanAllocMemory(size_t size) {
  return konan::calloc(1, size);
}

inline void* konanAllocAlignedMemory(size_t size, size_t alignment) {
    return konan::calloc_aligned(1, size, alignment);
}

inline void konanFreeMemory(void* memory) {
  konan::free(memory);
}

template<typename T>
inline T* konanAllocArray(size_t length) {
  return reinterpret_cast<T*>(konanAllocMemory(length * sizeof(T)));
}

template <typename T, typename ...A>
inline T* konanConstructInstance(A&& ...args) {
  return new (konanAllocMemory(sizeof(T))) T(::std::forward<A>(args)...);
}

template <typename T, typename ...A>
inline T* konanConstructSizedInstance(size_t size, A&& ...args) {
  return new (konanAllocMemory(size)) T(::std::forward<A>(args)...);
}

template <typename T>
inline void konanDestructInstance(T* instance) {
  instance->~T();
  konanFreeMemory(instance);
}

template <class T> class KonanAllocator {
 public:
  typedef size_t size_type;
  typedef ptrdiff_t difference_type;
  typedef T* pointer;
  typedef const T* const_pointer;
  typedef T& reference;
  typedef const T& const_reference;
  typedef T value_type;

  KonanAllocator() {}
  KonanAllocator(const KonanAllocator&) {}

  pointer allocate(size_type n, const void * = 0) {
    return reinterpret_cast<T*>(konanAllocMemory(n * sizeof(T)));
  }

  void deallocate(void* p, size_type) {
    if (p != nullptr) konanFreeMemory(p);
  }

  pointer address(reference x) const { return &x; }

  const_pointer address(const_reference x) const { return &x; }

  KonanAllocator<T>&  operator=(const KonanAllocator&) { return *this; }

  void construct(pointer p, const T& val) { new ((T*) p) T(val); }

  // C++-11 wants that.
  template <class U, class ...A>
  void construct(U* const p, A&& ...args) {
    new (p) U(::std::forward<A>(args)...);
  }

  void destroy(pointer p) { p->~T(); }

  size_type max_size() const { return size_t(-1); }

  template <class U>
  struct rebind { typedef KonanAllocator<U> other; };

  template <class U>
  KonanAllocator(const KonanAllocator<U>&) {}

  template <class U>
  KonanAllocator& operator=(const KonanAllocator<U>&) { return *this; }
};

template <class T, class U>
bool operator==(
  KonanAllocator<T> const&, KonanAllocator<U> const&) noexcept {
    return true;
}

template <class T, class U>
bool operator!=(
  KonanAllocator<T> const& x, KonanAllocator<U> const& y) noexcept {
    return !(x == y);
}

template <class T>
class KonanDeleter {
public:
    void operator()(T* instance) noexcept { konanDestructInstance(instance); }
};

// Force a class to be heap-allocated using `konanAllocMemory`. Does not prevent stack allocation, or
// allocation as part of another object.
// Usage:
// class A : public KonanAllocatorAware {
//     ...
// };
class KonanAllocatorAware {
public:
    static void* operator new(size_t count) noexcept { return konanAllocMemory(count); }
    static void* operator new[](size_t count) noexcept { return konanAllocMemory(count); }

    static void* operator new(size_t count, void* ptr) noexcept { return ptr; }
    static void* operator new[](size_t count, void* ptr) noexcept { return ptr; }

    static void operator delete(void* ptr) noexcept { konanFreeMemory(ptr); }
    static void operator delete[](void* ptr) noexcept { konanFreeMemory(ptr); }

protected:
    // Hide constructors, assignments and destructor to discourage operating on instance of `KonanAllocatorAware`
    KonanAllocatorAware() = default;

    KonanAllocatorAware(const KonanAllocatorAware&) = default;
    KonanAllocatorAware(KonanAllocatorAware&&) = default;

    KonanAllocatorAware& operator=(const KonanAllocatorAware&) = default;
    KonanAllocatorAware& operator=(KonanAllocatorAware&&) = default;

    // Not virtual by design. Since this class hides this destructor, no one can destroy an
    // instance of `KonanAllocatorAware` directly, so this destructor is never called in a virtual manner.
    ~KonanAllocatorAware() = default;
};

#endif // RUNTIME_ALLOC_H
