// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "CAPIExtensions.h"
#include "llvm-c/Core.h"
#include "llvm-c/TargetMachine.h"
#include "llvm/ADT/STLExtras.h"
#include "llvm/ADT/SmallPtrSet.h"
#include "llvm/ADT/SmallString.h"
#include "llvm/Bitcode/BitcodeReader.h"
#include "llvm/Bitcode/BitcodeWriter.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/GlobalAlias.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/Module.h"
#include "llvm/MC/TargetRegistry.h"
#include "llvm/Support/CBindingWrapping.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/FileSystem.h"
#include "llvm/Support/MemoryBufferRef.h"
#include "llvm/Support/ThreadPool.h"
#include "llvm/Target/TargetMachine.h"
#include "llvm/Transforms/Utils/ModuleUtils.h"
#include "llvm/Transforms/Utils/SplitModule.h"
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <string>
#include <vector>

using namespace llvm;

namespace llvm::kotlin {
namespace {
// Keep Mach-O metadata and its referenced data in the same object.
void collectData(const Constant *C, SmallPtrSetImpl<const Constant *> &Seen,
                 SmallPtrSetImpl<GlobalValue *> &Data) {
  if (!Seen.insert(C).second || isa<Function>(C))
    return;
  if (auto *G = dyn_cast<GlobalVariable>(C)) {
    if (G->isDeclaration())
      return;
    Data.insert(const_cast<GlobalVariable *>(G));
    collectData(G->getInitializer(), Seen, Data);
  } else if (auto *A = dyn_cast<GlobalAlias>(C)) {
    Data.insert(const_cast<GlobalAlias *>(A));
    collectData(A->getAliasee(), Seen, Data);
  } else {
    for (const Use &U : C->operands())
      if (const auto *Child = dyn_cast<Constant>(U.get()))
        collectData(Child, Seen, Data);
  }
}

struct UsedEntry {
  std::string Name;
  bool Declaration;
};

struct PartitionTiming {
  int64_t startUs = 0;
  int64_t endUs = 0;
  int64_t wallUs = 0;
};

using SteadyClock = std::chrono::steady_clock;

int64_t elapsedUs(SteadyClock::duration Duration) {
  return std::chrono::duration_cast<std::chrono::microseconds>(Duration).count();
}

// Written only when KN_OBJECT_PARTITION_LOG is set. Times are wall-clock
// microseconds from entry to emitPartitions.
void writePartitionTimings(unsigned Count, int64_t splitUs, int64_t totalUs,
                           const std::vector<PartitionTiming> &Timings) {
  const char *Path = std::getenv("KN_OBJECT_PARTITION_LOG");
  if (!Path || !*Path)
    return;
  FILE *Out = std::fopen(Path, "a");
  if (!Out)
    return;
  std::fprintf(Out, "---\n");
  std::fprintf(Out, "partitions=%u\n", Count);
  std::fprintf(Out, "split_wall_us=%lld\n", static_cast<long long>(splitUs));
  std::fprintf(Out, "emission_wall_us=%lld\n", static_cast<long long>(totalUs));
  for (unsigned I = 0; I < Count; ++I) {
    std::fprintf(Out, "index=%u wall_us=%lld start_us=%lld end_us=%lld\n", I,
                 static_cast<long long>(Timings[I].wallUs),
                 static_cast<long long>(Timings[I].startUs),
                 static_cast<long long>(Timings[I].endUs));
  }
  std::fclose(Out);
}

LLVMErrorRef emitPartitions(Module &M, TargetMachine &Machine, unsigned Count,
                            const char *const *OutputPaths) {
  if (Count < 2)
    return LLVMCreateStringError(
        "Parallel object emission requires at least two partitions");
  if (!M.getModuleInlineAsm().empty())
    return LLVMCreateStringError("Parallel object emission does not support "
                                 "module-level inline assembly");
  const std::string ID = getUniqueModuleId(&M);
  if (ID.empty())
    return LLVMCreateStringError("Parallel object emission requires a strong "
                                 "external definition for unique symbol names");

  SmallVector<GlobalValue *, 16> Used, CompilerUsed;
  if (auto *G = collectUsedGlobalVariables(M, Used, false))
    G->eraseFromParent();
  if (auto *G = collectUsedGlobalVariables(M, CompilerUsed, true))
    G->eraseFromParent();
  // Mach-O literal and ObjC metadata sections are interpreted by the linker.
  // Preserve their local binding and co-locate their users as well as their
  // data.
  SmallPtrSet<const Constant *, 32> Seen;
  SmallPtrSet<GlobalValue *, 32> Keep;
  for (GlobalVariable &G : M.globals()) {
    if (G.getSection().starts_with("__") || G.getName().starts_with("llvm."))
      collectData(&G, Seen, Keep);
  }
  for (Function &F : M)
    if (F.hasSection())
      Keep.insert(&F);
  struct LocalEntry {
    std::string Name;
    GlobalValue::LinkageTypes Linkage;
    GlobalValue::VisibilityTypes Visibility;
    bool DSOLocal;
  };
  std::vector<LocalEntry> Locals;
  SmallPtrSet<GlobalValue *, 32> Together;
  SmallVector<User *, 32> Work;
  SmallPtrSet<User *, 32> VisitedUsers;
  for (GlobalValue &G : M.global_values()) {
    if (!G.hasName())
      G.setName("__kn_codegen_local");
    if (Keep.contains(&G)) {
      Together.insert(&G);
      if (G.hasLocalLinkage()) {
        Locals.push_back({G.getName().str(), G.getLinkage(), G.getVisibility(),
                          G.isDSOLocal()});
        Work.append(G.user_begin(), G.user_end());
      }
    } else if (G.hasLocalLinkage()) {
      // Darwin ld strips ThinLTO's .llvm. suffix from debug-map function names.
      // A regular unique suffix keeps dsymutil on its direct symbol lookup path.
      G.setName(G.getName() + ".kn." + ID);
      G.setLinkage(GlobalValue::ExternalLinkage);
      G.setVisibility(GlobalValue::HiddenVisibility);
      G.setDSOLocal(true);
    }
  }
  while (!Work.empty()) {
    User *U = Work.pop_back_val();
    if (!VisitedUsers.insert(U).second)
      continue;
    if (auto *I = dyn_cast<Instruction>(U))
      Together.insert(I->getFunction());
    else if (auto *G = dyn_cast<GlobalValue>(U))
      Together.insert(G);
    else
      Work.append(U->user_begin(), U->user_end());
  }
  std::vector<UsedEntry> UsedNames, CompilerUsedNames;
  for (auto *G : Used)
    UsedNames.push_back({G->getName().str(), G->isDeclarationForLinker()});
  for (auto *G : CompilerUsed)
    CompilerUsedNames.push_back(
        {G->getName().str(), G->isDeclarationForLinker()});

  // COMDAT expresses the temporary partition constraint. Remove it and restore
  // local binding in the owning partition before generating Mach-O objects.
  const std::string GroupName = "__kn_codegen_order_" + ID;
  Comdat *Group = M.getOrInsertComdat(GroupName);
  for (GlobalValue *G : Together) {
    auto *Root = const_cast<GlobalObject *>(G->getAliaseeObject());
    if (!Root || Root->isDeclaration())
      continue;
    // Existing COMDAT groups are already SplitModule clusters. ELF optimized
    // IR can arrive with them; replacing the group would change link semantics.
    // Leave Mach-O unconditional so Apple targets keep pre-ELF-fix behavior.
    if (M.getTargetTriple().isOSBinFormatELF() && Root->getComdat() &&
        Root->getComdat() != Group)
      continue;
    Root->setComdat(Group);
  }

  DefaultThreadPool Pool(heavyweight_hardware_concurrency(Count));
  std::vector<std::string> Errors(Count);
  std::vector<PartitionTiming> Timings(Count);
  const auto Origin = SteadyClock::now();
  unsigned Index = 0;
  const auto SplitStart = SteadyClock::now();
  SplitModule(
      M, Count,
      [&](std::unique_ptr<Module> Part) {
        const unsigned I = Index++;
        for (GlobalValue &G : Part->global_values())
          if (auto *Object = dyn_cast<GlobalObject>(&G))
            if (Object->hasComdat() &&
                Object->getComdat()->getName() == GroupName)
              Object->setComdat(nullptr);
        auto RebuildUsed = [&](const std::vector<UsedEntry> &Names,
                               bool Compiler) {
          SmallVector<GlobalValue *, 16> Here;
          for (const UsedEntry &Entry : Names)
            if (auto *G = Part->getNamedValue(Entry.Name))
              if ((Entry.Declaration && I == 0) ||
                  (!Entry.Declaration && !G->isDeclarationForLinker()))
                Here.push_back(G);
          if (Compiler)
            appendToCompilerUsed(*Part, Here);
          else
            appendToUsed(*Part, Here);
        };
        RebuildUsed(UsedNames, false);
        RebuildUsed(CompilerUsedNames, true);
        // On ELF, restoring local linkage hides the definition from other
        // partitions. Hidden external is linkable across the split objects.
        // Mach-O still restores locals whose users were clustered above.
        if (!M.getTargetTriple().isOSBinFormatELF()) {
          for (const LocalEntry &Entry : Locals)
            if (auto *G = Part->getNamedValue(Entry.Name))
              if (!G->isDeclaration()) {
                G->setLinkage(Entry.Linkage);
                G->setVisibility(Entry.Visibility);
                G->setDSOLocal(Entry.DSOLocal);
              }
        }

        // Loading any archive member must retain the original module's group.
        auto *Pointer = PointerType::getUnqual(M.getContext());
        const std::string Anchor = "__kn_codegen_anchor_" + ID + "_";
        auto *Next = new GlobalVariable(
            *Part, Pointer, true, GlobalValue::ExternalLinkage, nullptr,
            Anchor + std::to_string((I + 1) % Count));
        Next->setVisibility(GlobalValue::HiddenVisibility);
        Next->setDSOLocal(true);
        auto *Own = new GlobalVariable(*Part, Pointer, true,
                                       GlobalValue::ExternalLinkage, Next,
                                       Anchor + std::to_string(I));
        Own->setVisibility(GlobalValue::HiddenVisibility);
        Own->setDSOLocal(true);
        appendToUsed(*Part, {Own});

        auto Prune = [](GlobalValue &G) {
          if (G.isDeclaration() && G.use_empty() && !G.isUsedByMetadata())
            G.eraseFromParent();
        };
        for (Function &F : make_early_inc_range(*Part))
          Prune(F);
        for (GlobalVariable &G : make_early_inc_range(Part->globals()))
          Prune(G);
        SmallString<0> Buffer;
        raw_svector_ostream Stream(Buffer);
        WriteBitcodeToFile(*Part, Stream);
        Pool.async(
            [&, I](const SmallString<0> &Bytes) {
              const auto Started = SteadyClock::now();
              auto Finish = [&] {
                const auto Ended = SteadyClock::now();
                Timings[I].startUs = elapsedUs(Started - Origin);
                Timings[I].endUs = elapsedUs(Ended - Origin);
                Timings[I].wallUs = elapsedUs(Ended - Started);
              };
              LLVMContext Context;
              auto Parsed = parseBitcodeFile(
                  MemoryBufferRef(Bytes, "codegen-part"), Context);
              if (!Parsed) {
                Errors[I] = toString(Parsed.takeError());
                Finish();
                return;
              }
              std::unique_ptr<TargetMachine> TM(
                  Machine.getTarget().createTargetMachine(
                      Machine.getTargetTriple(), Machine.getTargetCPU(),
                      Machine.getTargetFeatureString(), Machine.Options,
                      Machine.getRelocationModel(), Machine.getCodeModel(),
                      Machine.getOptLevel()));
              if (!TM) {
                Errors[I] = "Failed to create partition target machine";
                Finish();
                return;
              }
              char *Error = nullptr;
              if (LLVMTargetMachineEmitToFile(
                      reinterpret_cast<LLVMTargetMachineRef>(TM.get()),
                      wrap(Parsed->get()), const_cast<char *>(OutputPaths[I]),
                      LLVMObjectFile, &Error)) {
                Errors[I] = Error ? Error : "Failed to emit codegen partition";
                LLVMDisposeMessage(Error);
              }
              Finish();
            },
            std::move(Buffer));
      },
      false, true);
  const auto SplitUs = elapsedUs(SteadyClock::now() - SplitStart);
  Pool.wait();
  writePartitionTimings(Count, SplitUs, elapsedUs(SteadyClock::now() - Origin),
                        Timings);
  for (const std::string &Error : Errors)
    if (!Error.empty()) {
      for (unsigned I = 0; I < Count; ++I)
        sys::fs::remove(OutputPaths[I]);
      return LLVMCreateStringError(Error.c_str());
    }
  return nullptr;
}
} // namespace
} // namespace llvm::kotlin

LLVMErrorRef LLVMKotlinEmitObjectFiles(LLVMModuleRef Module,
                                       LLVMTargetMachineRef Machine,
                                       unsigned Count,
                                       const char *const *OutputPaths) {
  return llvm::kotlin::emitPartitions(
      *llvm::unwrap(Module), *reinterpret_cast<llvm::TargetMachine *>(Machine),
      Count, OutputPaths);
}
