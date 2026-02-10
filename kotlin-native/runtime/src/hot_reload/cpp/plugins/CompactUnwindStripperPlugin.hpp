#ifndef KOTLIN_NATIVE_COMPACTUNWINDSTRIPPERPLUGIN_HPP
#define KOTLIN_NATIVE_COMPACTUNWINDSTRIPPERPLUGIN_HPP

#include "../HotReloadInternal.hpp"

namespace kotlin::hot::orc::plugins {

/// JITLink plugin that strips compact unwind sections to avoid 32-bit delta
/// overflow when JIT-allocated code is far from the unwind info section.
///
/// On macOS arm64, compact unwind entries use 32-bit offsets. When JITLink
/// allocates code far from the host process image (common during reload),
/// the deltas overflow causing "delta to function exceeds 32 bits" errors.
///
/// Removing __compact_unwind prevents __unwind_info generation, avoiding the
/// error at the cost of losing stack unwinding in JIT'd code.
class CompactUnwindStripperPlugin : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    void modifyPassConfig(llvm::orc::MaterializationResponsibility& MR, llvm::jitlink::LinkGraph& G, llvm::jitlink::PassConfiguration& Config) override;

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override { return llvm::Error::success(); }

    void notifyTransferringResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey DstKey, llvm::orc::ResourceKey SrcKey) override {}
};

} // namespace kotlin::hot::orc::plugins

#endif // KOTLIN_NATIVE_COMPACTUNWINDSTRIPPERPLUGIN_HPP
