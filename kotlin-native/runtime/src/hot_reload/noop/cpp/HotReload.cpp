#ifdef KONAN_HOT_RELOAD

#include "Memory.h"

namespace kotlin::hot {

class HotReload {
public:
    static void InitModule() noexcept {}
};

} // namespace kotlin::hot

#endif
