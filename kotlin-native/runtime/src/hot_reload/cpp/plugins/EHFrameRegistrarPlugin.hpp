#ifdef KONAN_HOT_RELOAD

#ifndef KOTLIN_NATIVE_EHFRAMEREGISTRARPLUGIN_HPP
#define KOTLIN_NATIVE_EHFRAMEREGISTRARPLUGIN_HPP

#include "../HotReloadInternal.hpp"
#include "llvm/ExecutionEngine/Orc/Shared/MachOObjectFormat.h"
#include "llvm/ExecutionEngine/Orc/TargetProcess/RegisterEHFrames.h"

namespace kotlin::hot::orc::plugins {

/// JITLink plugin that registers __eh_frame sections directly with the system
/// unwinder via __register_frame, bypassing the MachOPlatform's UnwindInfoManager.
///
/// Strategy:
/// - PrePrune: move __eh_frame blocks to a hidden section so MachOPlatform ignores them
/// - PostFixup: find the hidden section (fixups applied), register via __register_frame
class EHFrameRegistrarPlugin : public llvm::orc::ObjectLinkingLayer::Plugin {
    static constexpr const char* HiddenSectionName = "__TEXT,__knhr_ehframe";

public:
    void modifyPassConfig(llvm::orc::MaterializationResponsibility& MR, llvm::jitlink::LinkGraph& G,
                          llvm::jitlink::PassConfiguration& Config) override {
        // PrePrune: move __eh_frame to a hidden section name.
        // EHFrameEdgeFixer runs before us (also PrePrune) so edges are already created.
        Config.PrePrunePasses.push_back([](llvm::jitlink::LinkGraph& Graph) -> llvm::Error {
            auto* EHFrame = Graph.findSectionByName(llvm::orc::MachOEHFrameSectionName);
            if (!EHFrame || EHFrame->empty())
                return llvm::Error::success();

            // Create a hidden section and move all blocks/symbols to it.
            auto& Hidden = Graph.createSection(HiddenSectionName, EHFrame->getMemProt());
            Graph.mergeSections(Hidden, *EHFrame); // moves content and removes EHFrame

            return llvm::Error::success();
        });

        // PostFixup: register the hidden section with the system unwinder.
        Config.PostFixupPasses.push_back([](llvm::jitlink::LinkGraph& Graph) -> llvm::Error {
            auto* EHFrame = Graph.findSectionByName(HiddenSectionName);
            if (!EHFrame || EHFrame->empty())
                return llvm::Error::success();

            llvm::orc::ExecutorAddr Start, End;
            Start = (*EHFrame->blocks().begin())->getAddress();
            End = Start;
            for (auto* B : EHFrame->blocks()) {
                auto R = B->getRange();
                Start = std::min(Start, R.Start);
                End = std::max(End, R.End);
            }

            auto Size = static_cast<size_t>(End.getValue() - Start.getValue());
            HRLogDebug("EHFrameRegistrar: registering %zu bytes at %p from %s",
                       Size, Start.toPtr<const void*>(), Graph.getName().data());

            return llvm::orc::registerEHFrameSection(Start.toPtr<const void*>(), Size);
        });
    }

    llvm::Error notifyEmitted(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }
    llvm::Error notifyFailed(llvm::orc::MaterializationResponsibility& MR) override { return llvm::Error::success(); }
    llvm::Error notifyRemovingResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey K) override { return llvm::Error::success(); }
    void notifyTransferringResources(llvm::orc::JITDylib& JD, llvm::orc::ResourceKey DstKey, llvm::orc::ResourceKey SrcKey) override {}
};

} // namespace kotlin::hot::orc::plugins

#endif // KOTLIN_NATIVE_EHFRAMEREGISTRARPLUGIN_HPP

#endif
