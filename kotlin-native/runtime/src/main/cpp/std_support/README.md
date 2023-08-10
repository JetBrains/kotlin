## Support for C++ standard library.

This contains backported utilities from future standards, implementation of not-yet-standardized proposals, and adjustments of standardized utilities to be used within Kotlin/Native runtime.

Everything here lives in `kotlin::std_support` namespace.

Backporting from C++20:
* `Span.hpp` - [`std::span`](https://en.cppreference.com/w/cpp/container/span)

Proposals:
* `Memory.hpp` - [`p0211r3`](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2020/p0211r3.html)

Adjustments:
* `CStdlib.hpp` -
  `std_support::malloc`, `std_support::calloc`, `std_support::realloc`, `std_support::free` that use custom allocation scheme,
  `std_support::aligned_malloc` and `std_support::aligned_free` as a version of `malloc` and `free` that allows changing alignment.
* `Memory.hpp` -
  `std_support::allocator` using `std_support::calloc`/`std_support::free`,
  `std_support::default_delete` that uses `std_support::free`,
  `std_support::unique_ptr` that uses `std_support::default_delete`,
  `std_support::make_unique` and `std_support::make_shared` that use `std_support::allocator`,
  `std_support::nullptr_unique` - `nullptr` replacement for `unique_ptr` that takes an allocator.
* `New.hpp` -
  custom operator `new` with `std_support::kalloc` marker argument that delegates to `std_support` allocator,
  `std_support::kdelete` as a replacement for operator `delete` for objects created with custom `new`.
* `Deque.hpp`, `ForwardList.hpp`, `List.hpp`, `Map.hpp`, `Set.hpp`, `String.hpp`, `UnorderedMap.hpp`, `UnorderedSet.hpp`, `Vector.hpp` -
  standard containers and `std_support::string` that default to using `std_support::allocator`.
