#ifdef KONAN_HOT_RELOAD

#include "MachOHostDataSymbolGenerator.hpp"
#include "../HotReloadUtility.hpp"

#include "llvm/Object/MachO.h"
#include "llvm/Support/MemoryBuffer.h"

#include <mach-o/dyld.h>

namespace kotlin::hot::orc::plugins {

llvm::Expected<std::unique_ptr<MachOHostDataSymbolGenerator>> MachOHostDataSymbolGenerator::CreateForCurrentProcess() {
    const char* execPath = _dyld_get_image_name(0);
    if (!execPath)
        return llvm::make_error<llvm::StringError>("Failed to get executable path", llvm::inconvertibleErrorCode());

    auto slide = static_cast<uint64_t>(_dyld_get_image_vmaddr_slide(0));

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
        if (!(flags & llvm::object::SymbolRef::SF_Global)) continue;

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