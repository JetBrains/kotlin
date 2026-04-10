#ifdef KONAN_HOT_RELOAD

#include "MachOHostDataSymbolGenerator.hpp"
#include "../HotReloadUtility.hpp"

#include "llvm/Object/MachO.h"
#include "llvm/Support/MemoryBuffer.h"

#include <mach-o/dyld.h>
#include <dlfcn.h>
#include <cstring>

extern "C" void KNHR_LoadObjCStubAddress();

namespace kotlin::hot::orc::plugins {

/// Find the dyld image index for the image containing the Kotlin/Native runtime.
/// Returns -1 if not found.
static int findHostImageIndex() {
    Dl_info info;
    if (!dladdr(reinterpret_cast<void*>(&KNHR_LoadObjCStubAddress), &info) || !info.dli_fname)
        return -1;

    const uint32_t imageCount = _dyld_image_count();
    for (uint32_t i = 0; i < imageCount; i++) {
        const char* name = _dyld_get_image_name(i);
        if (name && strcmp(name, info.dli_fname) == 0)
            return static_cast<int>(i);
    }
    return -1;
}

llvm::Expected<std::unique_ptr<MachOHostDataSymbolGenerator>> MachOHostDataSymbolGenerator::CreateForCurrentProcess() {
    int imageIndex = findHostImageIndex();
    if (imageIndex < 0)
        return llvm::make_error<llvm::StringError>("Failed to find host image in dyld image list", llvm::inconvertibleErrorCode());

    const char* execPath = _dyld_get_image_name(imageIndex);
    if (!execPath)
        return llvm::make_error<llvm::StringError>("Failed to get image path", llvm::inconvertibleErrorCode());

    auto slide = static_cast<uint64_t>(_dyld_get_image_vmaddr_slide(imageIndex));

    auto bufOrErr = llvm::MemoryBuffer::getFile(execPath);
    if (!bufOrErr)
        return llvm::make_error<llvm::StringError>("Failed to read host binary", bufOrErr.getError());

    auto binOrErr = llvm::object::createBinary((*bufOrErr)->getMemBufferRef());
    if (!binOrErr)
        return binOrErr.takeError();

    auto* machO = llvm::dyn_cast<llvm::object::MachOObjectFile>(binOrErr->get());
    if (!machO)
        return llvm::make_error<llvm::StringError>("Host binary is not a MachO file", llvm::inconvertibleErrorCode());

    llvm::StringMap<llvm::orc::ExecutorAddr> dataSymbols;

    for (const auto& sym : machO->symbols()) {
        auto flagsOrErr = sym.getFlags();
        if (!flagsOrErr) { llvm::consumeError(flagsOrErr.takeError()); continue; }

        unsigned flags = *flagsOrErr;
        if (flags & llvm::object::SymbolRef::SF_Undefined) continue;
        if (flags & llvm::object::SymbolRef::SF_FormatSpecific) continue;

        auto nameOrErr = sym.getName();
        if (!nameOrErr) { llvm::consumeError(nameOrErr.takeError()); continue; }

        auto addrOrErr = sym.getAddress();
        if (!addrOrErr) { llvm::consumeError(addrOrErr.takeError()); continue; }

        dataSymbols[*nameOrErr] = llvm::orc::ExecutorAddr(*addrOrErr + slide);
    }

    HRLogDebug("MachOHostDataSymbolGenerator: indexed %u symbols", dataSymbols.size());

    return std::unique_ptr<MachOHostDataSymbolGenerator>(new MachOHostDataSymbolGenerator(std::move(dataSymbols)));
}

llvm::Error MachOHostDataSymbolGenerator::tryToGenerate(
        llvm::orc::LookupState& LS,
        llvm::orc::LookupKind K,
        llvm::orc::JITDylib& JD,
        llvm::orc::JITDylibLookupFlags JDLookupFlags,
        const llvm::orc::SymbolLookupSet& Symbols) {
    llvm::orc::SymbolMap newSymbols;

    for (const auto& [name, flags] : Symbols) {
        auto it = DataSymbols_.find(*name);
        if (it != DataSymbols_.end())
            newSymbols[name] = {it->second, llvm::JITSymbolFlags::Exported};
    }

    if (!newSymbols.empty())
        return JD.define(llvm::orc::absoluteSymbols(std::move(newSymbols)));

    return llvm::Error::success();
}

} // namespace kotlin::hot::orc::plugins

#endif