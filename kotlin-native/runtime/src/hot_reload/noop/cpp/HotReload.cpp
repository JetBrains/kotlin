#ifdef KONAN_HOT_RELOAD

#include "HotReload.hpp"

namespace kotlin::hot {

void HotReload::InitModule() noexcept {}

void HotReload::LoadBootstrap(const uint8_t* manifestData) {}

KonanStartFn HotReload::LookupForKonanStart() const {
    return nullptr;
}

} // namespace kotlin::hot

#endif