#pragma once

#include <atomic>

#include "Memory.h"
#include "Utils.hpp"

namespace kotlin::gc {

class BarriersThreadData : private Pinned {
public:
    void onCheckpoint();
    void resetCheckpoint();
    bool visitedCheckpoint() const;
private:
    std::atomic<bool> visitedCheckpoint_ = false;
};

void EnableWeakRefBarriers(bool inSTW);
void DisableWeakRefBarriers(bool inSTW);

OBJ_GETTER(WeakRefRead, ObjHeader* weakReferee) noexcept;

}