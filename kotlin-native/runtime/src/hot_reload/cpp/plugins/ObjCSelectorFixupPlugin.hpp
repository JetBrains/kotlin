#ifndef KOTLIN_NATIVE_OBJCSELECTORFIXUPPLUGIN_HPP
#define KOTLIN_NATIVE_OBJCSELECTORFIXUPPLUGIN_HPP

#include "../HotReloadInternal.hpp"

namespace kotlin::hot::orc::plugins {

using namespace kotlin::hot;

// TODO: this is a temporary fix, since MachOPlatform should be handling this...

class ObjCSelectorFixupPlugin : public llvm::orc::ObjectLinkingLayer::Plugin {
public:
    void modifyPassConfig(llvm::orc::MaterializationResponsibility& MR, llvm::jitlink::LinkGraph& G, llvm::jitlink::PassConfiguration& Config) override;

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }

    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override { return llvm::Error::success(); }

    void notifyTransferringResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey DstKey, llvm::orc::ResourceKey SrcKey) override {}
};
} // namespace kotlin::hot::orc::plugins

#endif // KOTLIN_NATIVE_OBJCSELECTORFIXUPPLUGIN_HPP
