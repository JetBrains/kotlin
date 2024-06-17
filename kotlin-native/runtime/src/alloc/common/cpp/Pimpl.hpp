#pragma once

#include "ManuallyScoped.hpp"
namespace kotlin {

template<typename Impl, size_t kSize>
class Pimpl {
public:
    // Construct T
    template <typename ActualImpl, typename... Args>
    void construct(Args&&... args) noexcept(std::is_nothrow_constructible_v<ActualImpl, Args...>) {
        assertSize<sizeof(ActualImpl)>();
        RuntimeAssert(sizeof(ActualImpl) <= kSize, "Impl size %zu, allocated %zu", sizeof(ActualImpl), kSize);
        new (impl()) ActualImpl(std::forward<Args>(args)...);
    }

    template<size_t kExpectedSize>
    void assertSize() {
        static_assert(kExpectedSize <= kSize);
    }

    // Destroy T
    void destroy() noexcept { impl()->~T(); }

    Impl& operator*() noexcept { return *impl(); }
    Impl* operator->() noexcept { return impl(); }
    const Impl& operator*() const noexcept { return *impl(); }
    const Impl* operator->() const noexcept { return impl(); }
private:
    __attribute__((used)) Impl* impl() noexcept { return reinterpret_cast<Impl*>(implStorage_); }
    const Impl* impl() const noexcept { return reinterpret_cast<const Impl*>(implStorage_); }

    // FIXME alignment
    alignas(16) char implStorage_[kSize];
};

}