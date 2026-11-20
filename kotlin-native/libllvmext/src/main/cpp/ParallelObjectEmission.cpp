// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "CAPIExtensions.h"
#include "llvm-c/Core.h"
#include "llvm-c/TargetMachine.h"
#include "llvm/ADT/DenseMap.h"
#include "llvm/ADT/Hashing.h"
#include "llvm/ADT/STLExtras.h"
#include "llvm/ADT/SmallPtrSet.h"
#include "llvm/ADT/SmallString.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/StringMap.h"
#include "llvm/Bitcode/BitcodeReader.h"
#include "llvm/Bitcode/BitcodeWriter.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/DebugInfo.h"
#include "llvm/IR/DebugInfoMetadata.h"
#include "llvm/IR/GlobalAlias.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/Module.h"
#include "llvm/MC/TargetRegistry.h"
#include "llvm/Support/CBindingWrapping.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/FileSystem.h"
#include "llvm/Support/MemoryBufferRef.h"
#include "llvm/Support/Path.h"
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

// KN ObjCDataGenerator / Clang ObjC ABI symbols must keep their linker names.
static bool isMachOObjCMetadataName(StringRef Name) {
  return Name.starts_with("OBJC_") || Name.starts_with("_OBJC_");
}

// Mach-O metadata seeds for the closed parallel contract:
//   - explicit section (including user-defined sections) → metadata section
//   - ObjC ABI names / llvm.* special globals
// No section and none of those names → not a metadata section; treat as a
// normal symbol unless a relative-pointer / local-Keep use check fires.
static bool isMetadataSeed(const GlobalValue &G) {
  if (const auto *GO = dyn_cast<GlobalObject>(&G))
    if (GO->hasSection())
      return true;
  if (isMachOObjCMetadataName(G.getName()))
    return true;
  return G.getName().starts_with("llvm.");
}

static bool hasExplicitSection(const GlobalValue &G) {
  if (const auto *GO = dyn_cast<GlobalObject>(&G))
    return GO->hasSection();
  return false;
}

// Walk initializers only — do not treat referenced functions as metadata data
// (matches the original Keep clustering; users are discovered separately).
static void collectData(const Constant *C,
                        SmallPtrSetImpl<const Constant *> &Seen,
                        SmallPtrSetImpl<GlobalValue *> &Data) {
  if (!Seen.insert(C).second || isa<Function>(C))
    return;
  if (auto *G = dyn_cast<GlobalVariable>(C)) {
    if (G->isDeclaration())
      return;
    Data.insert(const_cast<GlobalVariable *>(G));
    if (G->hasInitializer())
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

static void collectMetadataKeepSet(Module &M,
                                   SmallPtrSetImpl<GlobalValue *> &Keep) {
  SmallPtrSet<const Constant *, 32> Seen;
  for (GlobalVariable &G : M.globals()) {
    if (G.isDeclaration() || !isMetadataSeed(G))
      continue;
    collectData(&G, Seen, Keep);
  }
  for (Function &F : M) {
    if (F.isDeclaration() || !isMetadataSeed(F))
      continue;
    Keep.insert(&F);
  }
  for (GlobalAlias &A : M.aliases()) {
    if (A.isDeclaration() || !isMetadataSeed(A))
      continue;
    collectData(&A, Seen, Keep);
  }
}

static void enqueueUsers(Value *V, SmallVectorImpl<User *> &Work) {
  Work.append(V->user_begin(), V->user_end());
}

static GlobalValue *globalFromPtrToInt(const Constant *C) {
  const Constant *Cur = C;
  while (auto *CE = dyn_cast<ConstantExpr>(Cur)) {
    switch (CE->getOpcode()) {
    case Instruction::PtrToInt:
      if (auto *GV =
              dyn_cast<GlobalValue>(CE->getOperand(0)->stripPointerCasts()))
        return const_cast<GlobalValue *>(GV);
      return nullptr;
    case Instruction::BitCast:
    case Instruction::AddrSpaceCast:
    case Instruction::Trunc:
    case Instruction::ZExt:
    case Instruction::SExt:
    case Instruction::IntToPtr:
      Cur = cast<Constant>(CE->getOperand(0));
      continue;
    default:
      return nullptr;
    }
  }
  return nullptr;
}

// Relative / same-file pointer diffs in constants. Collects locals that must
// stay in one Mach-O object with their host.
static bool collectRelativePointerLocals(const Constant *C,
                                         SmallPtrSetImpl<GlobalValue *> &Locals,
                                         SmallPtrSetImpl<const Constant *> &Seen) {
  if (!Seen.insert(C).second)
    return false;
  bool Found = false;

  auto ConsiderSub = [&](const Constant *LHS, const Constant *RHS) {
    GlobalValue *A = globalFromPtrToInt(LHS);
    GlobalValue *B = globalFromPtrToInt(RHS);
    if (!A || !B || A == B)
      return;
    if (A->hasLocalLinkage())
      Locals.insert(A);
    if (B->hasLocalLinkage())
      Locals.insert(B);
    if (A->hasLocalLinkage() || B->hasLocalLinkage())
      Found = true;
  };

  if (auto *CE = dyn_cast<ConstantExpr>(C)) {
    unsigned Op = CE->getOpcode();
    if (Op == Instruction::Sub) {
      ConsiderSub(cast<Constant>(CE->getOperand(0)),
                  cast<Constant>(CE->getOperand(1)));
    } else if (Op == Instruction::Trunc || Op == Instruction::ZExt ||
               Op == Instruction::SExt || Op == Instruction::BitCast ||
               Op == Instruction::IntToPtr || Op == Instruction::PtrToInt) {
      if (auto *Inner = dyn_cast<ConstantExpr>(CE->getOperand(0)))
        if (Inner->getOpcode() == Instruction::Sub)
          ConsiderSub(cast<Constant>(Inner->getOperand(0)),
                      cast<Constant>(Inner->getOperand(1)));
    }
  }
  for (const Use &U : C->operands())
    if (const auto *Child = dyn_cast<Constant>(U.get()))
      Found |= collectRelativePointerLocals(Child, Locals, Seen);
  return Found;
}

static bool constantRefersToAny(const Constant *C,
                               const SmallPtrSetImpl<GlobalValue *> &Targets,
                               SmallPtrSetImpl<const Constant *> &Seen) {
  if (!Seen.insert(C).second)
    return false;
  if (auto *GV = dyn_cast<GlobalValue>(C))
    return Targets.contains(const_cast<GlobalValue *>(GV));
  for (const Use &U : C->operands())
    if (const auto *Child = dyn_cast<Constant>(U.get()))
      if (constantRefersToAny(Child, Targets, Seen))
        return true;
  return false;
}

static void addLocalStay(GlobalValue *G,
                         SmallPtrSetImpl<GlobalValue *> &LocalStay) {
  if (!G || !G->hasLocalLinkage())
    return;
  // Restore after split keys on name; private constants are often unnamed.
  if (!G->hasName() || G->getName().empty())
    G->setName("__kn_codegen_meta_local");
  LocalStay.insert(G);
}

// Build the Mach-O same-file cluster used by both safety and emit:
//   MetaTogether — welded into one temporary COMDAT / partition
//   LocalStay    — must keep local linkage through split + restore
// Pulls relative-pointer hosts/locals and every IR user of LocalStay in.
// Also over-approximates: every defined global joins MetaTogether so
// layout-only data that IR cannot edge-enumerate still shares one object.
static void buildMachOSameFileCluster(Module &M,
                                      SmallPtrSetImpl<GlobalValue *> &Keep,
                                      SmallPtrSetImpl<GlobalValue *> &MetaTogether,
                                      SmallPtrSetImpl<GlobalValue *> &LocalStay) {
  collectMetadataKeepSet(M, Keep);
  MetaTogether.clear();
  LocalStay.clear();

  for (GlobalValue *G : Keep) {
    MetaTogether.insert(G);
    addLocalStay(G, LocalStay);
  }

  auto AbsorbUsersOf = [&](GlobalValue *G, SmallVectorImpl<User *> &Work,
                           SmallPtrSetImpl<User *> &Visited) {
    enqueueUsers(G, Work);
    if (auto *F = dyn_cast<Function>(G)) {
      for (BasicBlock &BB : *F) {
        if (BlockAddress *BA = BlockAddress::lookup(&BB))
          if (BA->isConstantUsed())
            enqueueUsers(BA, Work);
      }
    }
    while (!Work.empty()) {
      User *U = Work.pop_back_val();
      if (!Visited.insert(U).second)
        continue;
      if (auto *I = dyn_cast<Instruction>(U))
        MetaTogether.insert(I->getFunction());
      else if (auto *GV = dyn_cast<GlobalValue>(U)) {
        MetaTogether.insert(GV);
        addLocalStay(GV, LocalStay);
        enqueueUsers(GV, Work);
      } else {
        enqueueUsers(U, Work);
      }
    }
  };

  {
    SmallVector<User *, 32> Work;
    SmallPtrSet<User *, 32> Visited;
    for (GlobalValue *G : Keep)
      if (G->hasLocalLinkage())
        AbsorbUsersOf(G, Work, Visited);
  }

  auto NoteRelative = [&](GlobalValue *Host, const Constant *C) {
    SmallPtrSet<GlobalValue *, 8> RelLocals;
    SmallPtrSet<const Constant *, 16> Seen;
    if (!collectRelativePointerLocals(C, RelLocals, Seen))
      return;
    MetaTogether.insert(Host);
    addLocalStay(Host, LocalStay);
    for (GlobalValue *L : RelLocals) {
      MetaTogether.insert(L);
      addLocalStay(L, LocalStay);
    }
  };

  for (GlobalVariable &G : M.globals()) {
    if (G.isDeclaration() || !G.hasInitializer())
      continue;
    NoteRelative(&G, G.getInitializer());
  }
  for (GlobalAlias &A : M.aliases()) {
    if (A.isDeclaration())
      continue;
    NoteRelative(&A, A.getAliasee());
  }
  for (Function &F : M) {
    if (F.isDeclaration())
      continue;
    for (BasicBlock &BB : F) {
      for (Instruction &I : BB) {
        for (Value *Op : I.operands())
          if (auto *C = dyn_cast<Constant>(Op))
            NoteRelative(&F, C);
      }
    }
  }

  // All explicit sections stay in one partition (preserves per-module section
  // contribution order that serial emission had). Local section members stay
  // local so rename/externalize cannot break same-file section edges.
  for (GlobalVariable &G : M.globals()) {
    if (G.isDeclaration() || !hasExplicitSection(G))
      continue;
    MetaTogether.insert(&G);
    addLocalStay(&G, LocalStay);
  }
  for (Function &F : M) {
    if (F.isDeclaration() || !hasExplicitSection(F))
      continue;
    MetaTogether.insert(&F);
    addLocalStay(&F, LocalStay);
  }

  // Over-approx for cases IR cannot name (layout-only / late codegen edges):
  // put every remaining defined global into MetaTogether so they share one
  // object with the metadata pile. Do not LocalStay them — that would pull
  // nearly every function into the cluster via the fixpoint below.
  for (GlobalVariable &G : M.globals()) {
    if (!G.isDeclaration())
      MetaTogether.insert(&G);
  }
  for (GlobalAlias &A : M.aliases()) {
    if (!A.isDeclaration())
      MetaTogether.insert(&A);
  }

  // Fixpoint: anything that refers to LocalStay joins the cluster (and may
  // enlarge LocalStay if it is itself local).
  bool Changed = true;
  while (Changed) {
    Changed = false;
    for (Function &F : M) {
      if (F.isDeclaration() || MetaTogether.contains(&F))
        continue;
      bool Refs = false;
      for (BasicBlock &BB : F) {
        for (Instruction &I : BB) {
          for (Value *Op : I.operands()) {
            if (auto *GV = dyn_cast<GlobalValue>(Op))
              if (LocalStay.contains(GV)) {
                Refs = true;
                break;
              }
            if (auto *C = dyn_cast<Constant>(Op)) {
              SmallPtrSet<const Constant *, 8> Seen;
              if (constantRefersToAny(C, LocalStay, Seen)) {
                Refs = true;
                break;
              }
            }
          }
          if (Refs)
            break;
        }
        if (Refs)
          break;
      }
      if (Refs) {
        MetaTogether.insert(&F);
        Changed = true;
      }
    }
    for (GlobalVariable &G : M.globals()) {
      if (G.isDeclaration() || !G.hasInitializer() || MetaTogether.contains(&G))
        continue;
      SmallPtrSet<const Constant *, 8> Seen;
      if (!constantRefersToAny(G.getInitializer(), LocalStay, Seen))
        continue;
      MetaTogether.insert(&G);
      addLocalStay(&G, LocalStay);
      Changed = true;
    }
    for (GlobalAlias &A : M.aliases()) {
      if (A.isDeclaration() || MetaTogether.contains(&A))
        continue;
      SmallPtrSet<const Constant *, 8> Seen;
      if (!constantRefersToAny(A.getAliasee(), LocalStay, Seen))
        continue;
      MetaTogether.insert(&A);
      addLocalStay(&A, LocalStay);
      Changed = true;
    }
  }
}

// Mach-O: build the same-file cluster; refuse only if a seed/closure invariant
// cannot be satisfied (Kotlin then falls back to N=1).
static Error checkMachOParallelEmissionSafety(Module &M) {
  SmallPtrSet<GlobalValue *, 32> Keep, MetaTogether, LocalStay;
  buildMachOSameFileCluster(M, Keep, MetaTogether, LocalStay);

  auto FailMissing = [](const GlobalValue &G) {
    return createStringError(
        inconvertibleErrorCode(),
        ("metadata symbol not closed into a Keep cluster: " + G.getName())
            .str());
  };

  for (GlobalVariable &G : M.globals()) {
    if (G.isDeclaration() || !isMetadataSeed(G))
      continue;
    if (!Keep.contains(&G) || !MetaTogether.contains(&G))
      return FailMissing(G);
  }
  for (Function &F : M) {
    if (F.isDeclaration() || !isMetadataSeed(F))
      continue;
    if (!Keep.contains(&F) || !MetaTogether.contains(&F))
      return FailMissing(F);
  }
  for (GlobalAlias &A : M.aliases()) {
    if (A.isDeclaration() || !isMetadataSeed(A))
      continue;
    if (!Keep.contains(&A) || !MetaTogether.contains(&A))
      return FailMissing(A);
  }

  for (GlobalValue *G : Keep) {
    if (!hasExplicitSection(*G))
      continue;
    if (auto *GV = dyn_cast<GlobalVariable>(G)) {
      if (!GV->hasInitializer())
        continue;
      SmallPtrSet<GlobalValue *, 16> Data;
      SmallPtrSet<const Constant *, 16> Seen;
      collectData(GV->getInitializer(), Seen, Data);
      for (GlobalValue *D : Data)
        if (!Keep.contains(D) || !MetaTogether.contains(D))
          return createStringError(
              inconvertibleErrorCode(),
              ("section metadata '" + G->getName() + "' data '" + D->getName() +
               "' not closed into Keep/Together; refusing parallel object "
               "emission")
                  .str());
    }
  }

  // Verify nothing that must stay same-file escaped the cluster (build should
  // have absorbed it; leftover ⇒ serial).
  for (Function &F : M) {
    if (F.isDeclaration() || MetaTogether.contains(&F))
      continue;
    for (BasicBlock &BB : F) {
      for (Instruction &I : BB) {
        for (Value *Op : I.operands()) {
          if (auto *GV = dyn_cast<GlobalValue>(Op))
            if (LocalStay.contains(GV))
              return createStringError(
                  inconvertibleErrorCode(),
                  ("function '" + F.getName() +
                   "' references same-file local '" + GV->getName() +
                   "' outside Together; refusing parallel object emission")
                      .str());
          auto *C = dyn_cast<Constant>(Op);
          if (!C)
            continue;
          SmallPtrSet<GlobalValue *, 8> RelLocals;
          SmallPtrSet<const Constant *, 16> SeenRel;
          if (collectRelativePointerLocals(C, RelLocals, SeenRel) &&
              !RelLocals.empty())
            return createStringError(
                inconvertibleErrorCode(),
                ("function '" + F.getName() +
                 "' has relative pointers outside Together; refusing parallel "
                 "object emission")
                    .str());
          SmallPtrSet<const Constant *, 8> Seen;
          if (constantRefersToAny(C, LocalStay, Seen))
            return createStringError(
                inconvertibleErrorCode(),
                ("function '" + F.getName() +
                 "' references same-file locals outside Together; refusing "
                 "parallel object emission")
                    .str());
        }
      }
    }
  }
  for (GlobalVariable &G : M.globals()) {
    if (G.isDeclaration() || !G.hasInitializer())
      continue;
    SmallPtrSet<GlobalValue *, 8> RelLocals;
    SmallPtrSet<const Constant *, 16> SeenRel;
    if (collectRelativePointerLocals(G.getInitializer(), RelLocals, SeenRel) &&
        !RelLocals.empty()) {
      if (!MetaTogether.contains(&G))
        return createStringError(
            inconvertibleErrorCode(),
            ("global '" + G.getName() +
             "' has relative pointers outside Together; refusing parallel "
             "object emission")
                .str());
      for (GlobalValue *L : RelLocals)
        if (!MetaTogether.contains(L) || !LocalStay.contains(L))
          return createStringError(
              inconvertibleErrorCode(),
              ("relative-pointer local '" + L->getName() + "' from '" +
               G.getName() +
               "' not in LocalStay/Together; refusing parallel object "
               "emission")
                  .str());
    }
  }

  return Error::success();
}

// Refuse parallel emission when the target-specific contract cannot hold.
// Both targets need a unique module id for .kn.<id> rename of promoted locals
// (avoids clashing with system libs after externalize). Module inline asm is
// refused: .kn. rename does not rewrite asm strings; serial N=1 keeps names.
// Mach-O also requires a closed Keep/Together cluster; else Kotlin → N=1.
Error checkParallelEmissionSafety(Module &M) {
  if (getUniqueModuleId(&M).empty())
    return createStringError(
        inconvertibleErrorCode(),
        "parallel object emission requires a strong external definition for "
        "unique symbol names");

  if (!M.getModuleInlineAsm().empty())
    return createStringError(
        inconvertibleErrorCode(),
        "parallel object emission does not support module inline asm");

  if (M.getTargetTriple().isOSBinFormatELF())
    return Error::success();

  return checkMachOParallelEmissionSafety(M);
}

namespace {

static std::string pathFromDIFile(DIFile *File) {
  SmallString<256> Path;
  Path += File->getDirectory();
  Path += '/';
  Path += File->getFilename();
  return std::string(Path);
}

// ThinLTO-style TU key: prefer the subprogram's file (often one Kotlin file),
// then the compile unit file, else the function name.
std::string tuKeyForFunction(const Function &F) {
  if (DISubprogram *SP = F.getSubprogram()) {
    if (DIFile *File = SP->getFile())
      return pathFromDIFile(File);
    if (DICompileUnit *CU = SP->getUnit())
      if (DIFile *File = CU->getFile())
        return pathFromDIFile(File);
  }
  return ("~fn:" + F.getName()).str();
}

struct LocalEntry {
  std::string Name;
  GlobalValue::LinkageTypes Linkage;
  GlobalValue::VisibilityTypes Visibility;
  bool DSOLocal;
};

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

void stripThinLtoStyleTuComdats(Module &Part) {
  for (GlobalValue &G : Part.global_values())
    if (auto *Object = dyn_cast<GlobalObject>(&G))
      if (Object->hasComdat() &&
          Object->getComdat()->getName().starts_with("__kn_thin_tu_"))
        Object->setComdat(nullptr);
}

void externalizeLocal(GlobalValue &G, StringRef ID,
                      bool SkipObjCNameRewrite) {
  if (!G.hasName())
    G.setName("__kn_codegen_local");
  if (!G.hasLocalLinkage())
    return;
  if (!(SkipObjCNameRewrite && isMachOObjCMetadataName(G.getName())))
    G.setName(G.getName() + ".kn." + ID);
  G.setLinkage(GlobalValue::ExternalLinkage);
  G.setVisibility(GlobalValue::HiddenVisibility);
  G.setDSOLocal(true);
}

// Emit one partition: bitcode → fresh LLVMContext → TargetMachine → .o
void enqueuePartitionEmit(DefaultThreadPool &Pool, TargetMachine &Machine,
                          unsigned I, std::unique_ptr<Module> Part,
                          const char *OutputPath,
                          std::vector<std::string> &Errors,
                          std::vector<PartitionTiming> &Timings,
                          SteadyClock::time_point Origin) {
  SmallString<0> Buffer;
  raw_svector_ostream Stream(Buffer);
  WriteBitcodeToFile(*Part, Stream);
  Pool.async(
      [&, I, OutputPath](const SmallString<0> &Bytes) {
        const auto Started = SteadyClock::now();
        auto Finish = [&] {
          const auto Ended = SteadyClock::now();
          Timings[I].startUs = elapsedUs(Started - Origin);
          Timings[I].endUs = elapsedUs(Ended - Origin);
          Timings[I].wallUs = elapsedUs(Ended - Started);
        };
        LLVMContext Context;
        auto Parsed =
            parseBitcodeFile(MemoryBufferRef(Bytes, "codegen-part"), Context);
        if (!Parsed) {
          Errors[I] = toString(Parsed.takeError());
          Finish();
          return;
        }
        std::unique_ptr<TargetMachine> TM(Machine.getTarget().createTargetMachine(
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
                wrap(Parsed->get()), const_cast<char *>(OutputPath),
                LLVMObjectFile, &Error)) {
          Errors[I] = Error ? Error : "Failed to emit codegen partition";
          LLVMDisposeMessage(Error);
        } else if (const char *KeepDir =
                       std::getenv("KN_OBJECT_PARTITION_KEEP")) {
          if (*KeepDir) {
            SmallString<256> Dest(KeepDir);
            sys::fs::create_directories(Dest);
            sys::path::append(Dest, "part-" + std::to_string(I) + ".o");
            sys::fs::copy_file(OutputPath, Dest);
          }
        }
        Finish();
      },
      std::move(Buffer));
}

// ThinLTO-inspired split of an already-merged module:
//   ELF (splitCodeGen-style + KN link fix):
//     Promote locals with .kn.<id> + hidden (so bitcode-embedded copies of
//     system symbols do not collide with libstdc++/libgcc at native link),
//     then SplitModule(PreserveLocals=false). Leave llvm.used alone.
//     Module inline asm is refused in checkParallelEmissionSafety (→ N=1).
//   Mach-O:
//     0) same-file cluster; on failure Kotlin falls back to N=1
//     1) keep LocalStay local; externalize the rest with .kn.<id>
//     2) weld MetaTogether; remaining functions whole per source file
//     3) SplitModule; restore LocalStay; bitcode → EmitToFile
LLVMErrorRef emitPartitions(Module &M, TargetMachine &Machine, unsigned Count,
                            const char *const *OutputPaths) {
  if (Count < 2)
    return LLVMCreateStringError(
        "Parallel object emission requires at least two partitions");
  if (Error Err = checkParallelEmissionSafety(M)) {
    std::string Msg = toString(std::move(Err));
    return LLVMCreateStringError(Msg.c_str());
  }

  const bool IsELF = M.getTargetTriple().isOSBinFormatELF();
  const std::string ID = getUniqueModuleId(&M);
  DefaultThreadPool Pool(heavyweight_hardware_concurrency(Count));
  std::vector<std::string> Errors(Count);
  std::vector<PartitionTiming> Timings(Count);
  const auto Origin = SteadyClock::now();
  unsigned Index = 0;
  const auto SplitStart = SteadyClock::now();

  if (IsELF) {
    // Rename+externalize before SplitModule so promoted locals do not keep
    // system mangled names (N=1 left them local; bare externalize would clash).
    for (GlobalValue &G : M.global_values())
      if (G.hasLocalLinkage())
        externalizeLocal(G, ID, /*SkipObjCNameRewrite=*/false);
    SplitModule(
        M, Count,
        [&](std::unique_ptr<Module> Part) {
          const unsigned I = Index++;
          enqueuePartitionEmit(Pool, Machine, I, std::move(Part),
                               OutputPaths[I], Errors, Timings, Origin);
        },
        /*PreserveLocals=*/false, /*RoundRobin=*/false);
  } else {

    SmallVector<GlobalValue *, 16> Used, CompilerUsed;
    if (auto *G = collectUsedGlobalVariables(M, Used, false))
      G->eraseFromParent();
    if (auto *G = collectUsedGlobalVariables(M, CompilerUsed, true))
      G->eraseFromParent();

    std::vector<LocalEntry> LocalsToRestore;
    SmallPtrSet<GlobalValue *, 32> KeepLocal;
    SmallPtrSet<GlobalValue *, 32> MetaTogether;
    SmallPtrSet<GlobalValue *, 32> Keep, LocalStay;
    buildMachOSameFileCluster(M, Keep, MetaTogether, LocalStay);

    for (GlobalValue *G : LocalStay) {
      if (!G->hasName())
        G->setName("__kn_codegen_meta_local");
      KeepLocal.insert(G);
      LocalsToRestore.push_back({G->getName().str(), G->getLinkage(),
                                 G->getVisibility(), G->isDSOLocal()});
    }

    for (GlobalValue &G : M.global_values()) {
      if (!G.hasLocalLinkage()) {
        if (!G.hasName())
          G.setName("__kn_codegen_local");
        continue;
      }
      if (KeepLocal.contains(&G))
        continue;
      externalizeLocal(G, ID, /*SkipObjCNameRewrite=*/true);
    }

    // Weld MetaTogether; remaining functions stay whole per source file.
    DenseMap<GlobalValue *, std::string> Assigned;
    auto Assign = [&](GlobalValue *G, StringRef Key) {
      Assigned[G] = Key.str();
    };
    const std::string MetaKey = "~meta:macho";
    for (GlobalValue *G : MetaTogether)
      Assign(G, MetaKey);
    for (Function &F : M) {
      if (F.isDeclaration() || Assigned.count(&F))
        continue;
      std::string File = tuKeyForFunction(F);
      if (StringRef(File).starts_with("~fn:")) {
        Assign(&F, MetaKey);
        continue;
      }
      Assign(&F, File);
    }

    const std::string Prefix = "__kn_thin_tu_" + ID + "_";
    StringMap<Comdat *> Groups;
    for (auto &KV : Assigned) {
      GlobalValue *G = KV.first;
      auto *Root = const_cast<GlobalObject *>(G->getAliaseeObject());
      if (!Root || Root->isDeclaration())
        continue;
      Comdat *&Group = Groups[KV.second];
      if (!Group) {
        unsigned Hash = static_cast<unsigned>(hash_value(StringRef(KV.second)));
        Group = M.getOrInsertComdat(Prefix + std::to_string(Hash));
      }
      Root->setComdat(Group);
    }

    std::vector<UsedEntry> UsedNames, CompilerUsedNames;
    for (auto *G : Used)
      UsedNames.push_back({G->getName().str(), G->isDeclarationForLinker()});
    for (auto *G : CompilerUsed)
      CompilerUsedNames.push_back(
          {G->getName().str(), G->isDeclarationForLinker()});

    const std::string Anchor = "__kn_codegen_anchor_" + ID + "_";
    SplitModule(
        M, Count,
        [&](std::unique_ptr<Module> Part) {
          const unsigned I = Index++;
          stripThinLtoStyleTuComdats(*Part);
          auto RebuildUsed = [&](const std::vector<UsedEntry> &Names,
                                 bool Compiler) {
            SmallVector<GlobalValue *, 16> Here;
            for (const UsedEntry &Entry : Names) {
              auto *G = Part->getNamedValue(Entry.Name);
              if (!G)
                continue;
              if (Entry.Declaration ? G->isDeclarationForLinker()
                                    : !G->isDeclarationForLinker())
                Here.push_back(G);
            }
            if (Compiler)
              appendToCompilerUsed(*Part, Here);
            else
              appendToUsed(*Part, Here);
          };
          RebuildUsed(UsedNames, false);
          RebuildUsed(CompilerUsedNames, true);

          for (const LocalEntry &Entry : LocalsToRestore)
            if (auto *G = Part->getNamedValue(Entry.Name))
              if (!G->isDeclaration()) {
                G->setLinkage(Entry.Linkage);
                G->setVisibility(Entry.Visibility);
                G->setDSOLocal(Entry.DSOLocal);
              }

          auto *Pointer = PointerType::getUnqual(M.getContext());
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

          auto IsRetainedName = [&](StringRef Name) {
            if (Name.starts_with(Anchor))
              return true;
            for (const UsedEntry &Entry : UsedNames)
              if (Entry.Name == Name)
                return true;
            for (const UsedEntry &Entry : CompilerUsedNames)
              if (Entry.Name == Name)
                return true;
            return false;
          };
          auto Prune = [&](GlobalValue &G) {
            if (G.isDeclaration() && G.use_empty() && !G.isUsedByMetadata() &&
                !IsRetainedName(G.getName()))
              G.eraseFromParent();
          };
          for (Function &F : make_early_inc_range(*Part))
            Prune(F);
          for (GlobalVariable &G : make_early_inc_range(Part->globals()))
            Prune(G);

          enqueuePartitionEmit(Pool, Machine, I, std::move(Part),
                               OutputPaths[I], Errors, Timings, Origin);
        },
        /*PreserveLocals=*/false, /*RoundRobin=*/false);
  }

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

LLVMErrorRef LLVMKotlinCheckParallelObjectEmission(LLVMModuleRef Module) {
  if (llvm::Error Err = llvm::kotlin::checkParallelEmissionSafety(
          *llvm::unwrap(Module))) {
    std::string Msg = llvm::toString(std::move(Err));
    return LLVMCreateStringError(Msg.c_str());
  }
  return nullptr;
}

LLVMErrorRef LLVMKotlinEmitObjectFiles(LLVMModuleRef Module,
                                       LLVMTargetMachineRef Machine,
                                       unsigned Count,
                                       const char *const *OutputPaths) {
  return llvm::kotlin::emitPartitions(
      *llvm::unwrap(Module), *reinterpret_cast<llvm::TargetMachine *>(Machine),
      Count, OutputPaths);
}
