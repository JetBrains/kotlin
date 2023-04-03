#pragma once

#include <atomic>

#include "ThreadData.hpp"

namespace kotlin::mm {

using SafePointAction = void(*)(mm::ThreadData&);

bool TrySetSafePointAction(SafePointAction action) noexcept;
void UnsetSafePointAction() noexcept;
bool IsSafePointActionRequested() noexcept;

void SafePoint() noexcept;
void SafePoint(ThreadData& threadData) noexcept;

class ScopedSafePointAction : private MoveOnly {
public:
    explicit ScopedSafePointAction(SafePointAction action) noexcept : actionSet_(TrySetSafePointAction(action)) {}
    ScopedSafePointAction(SafePointAction action, const char* reasonToForce) noexcept : ScopedSafePointAction(action) {
        RuntimeAssert(actionSet(), "%s", reasonToForce);
    }
    ~ScopedSafePointAction() noexcept {
        if (actionSet_) {
            UnsetSafePointAction();
        }
    }
    bool actionSet() const {
        return actionSet_;
    }
private:
    bool actionSet_;
};

}

