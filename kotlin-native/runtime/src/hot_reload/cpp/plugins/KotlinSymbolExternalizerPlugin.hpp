#ifndef KOTLIN_NATIVE_KOTLINSYMBOLEXTERNALIZERPLUGIN_HPP
#define KOTLIN_NATIVE_KOTLINSYMBOLEXTERNALIZERPLUGIN_HPP

#include "../HotReloadInternal.hpp"

namespace kotlin::hot::orc::plugins {

/// JITLink plugin that externalizes _kfun: symbol definitions so that
/// intra-object calls go through StubsJD redirectable stubs.
///
/// For each _kfun: defined symbol in an object:
///   1. Renames the definition: _kfun:foo -> _kfun:foo$knhr (local impl)
///   2. Creates an external symbol for the original _kfun:foo name
///   3. Retargets all intra-object edges from the old def to the external
///
/// This forces JITLink to resolve _kfun:foo calls externally through the
/// JITDylib link order, hitting the redirectable stubs in StubsJD.
class KotlinSymbolExternalizerPlugin : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    explicit KotlinSymbolExternalizerPlugin(llvm::orc::JITDylib& StubsJD) : stubsJD_(StubsJD) {}

    void modifyPassConfig(llvm::orc::MaterializationResponsibility& MR, llvm::jitlink::LinkGraph& G, llvm::jitlink::PassConfiguration& Config) override;

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override { return llvm::Error::success(); }

    void notifyTransferringResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey DstKey, llvm::orc::ResourceKey SrcKey) override {}

private:
    llvm::orc::JITDylib& stubsJD_;
};

} // namespace kotlin::hot::orc::plugins

#endif // KOTLIN_NATIVE_KOTLINSYMBOLEXTERNALIZERPLUGIN_HPP