## Support for C++ standard library.

This contains backported utilities from future standards, implementation of not-yet-standardized proposals, and adjustments of standardized utilities to be used within Kotlin/Native runtime.

Everything here lives in `kotlin::std_support` namespace.

Backporting from C++20:
* `Span.hpp` - [`std::span`](https://en.cppreference.com/w/cpp/container/span)

Proposals:
* `Memory.hpp` - [`p0211r3`](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2020/p0211r3.html)

Adjustments:
* `CStdlib.hpp` -
  `std_support::aligned_malloc` and `std_support::aligned_free` as a version of `malloc` and `free` that allows changing alignment.
* `Memory.hpp` -
  `std_support::nullptr_unique` - `nullptr` replacement for `unique_ptr` that takes an allocator.
