#ifdef KONAN_HOT_RELOAD

#include "HotReload.hpp"

namespace kotlin::hot {

void HotReload::InitModule() noexcept {}

void HotReload::LoadBootstrapFile(std::string_view bootstrapFilePath) {}

KonanStartFn HotReload::LookupForKonanStart() const {
    return nullptr;
}

} // namespace kotlin::hot

#endif