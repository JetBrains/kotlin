// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include "PrintBitcode.h"

#include "llvm/ADT/Any.h"
#include "llvm/ADT/STLExtras.h"
#include "llvm/ADT/SmallString.h"
#include "llvm/Analysis/LazyCallGraph.h"
#include "llvm/Analysis/LoopInfo.h"
#include "llvm/CodeGen/MachineFunction.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/PrintPasses.h"
#include "llvm/Support/FileSystem.h"
#include "llvm/Support/FormatVariadic.h"
#include "llvm/Support/NativeFormatting.h"
#include "llvm/Support/Path.h"
#include "llvm/Support/xxhash.h"

using namespace llvm;
using namespace llvm::kotlin;

namespace {

template <typename IRUnitT> static const IRUnitT *unwrapIR(Any IR) {
  const IRUnitT **IRPtr = llvm::any_cast<const IRUnitT *>(&IR);
  return IRPtr ? *IRPtr : nullptr;
}

/// Extract Module out of \p IR unit. May return nullptr if \p IR does not match
/// certain global filters. Will never return nullptr if \p Force is true.
const Module *unwrapModule(Any IR, bool Force = false) {
  if (const auto *M = unwrapIR<Module>(IR))
    return M;

  if (const auto *F = unwrapIR<Function>(IR)) {
    if (!Force && !isFunctionInPrintList(F->getName()))
      return nullptr;

    return F->getParent();
  }

  if (const auto *C = unwrapIR<LazyCallGraph::SCC>(IR)) {
    for (const LazyCallGraph::Node &N : *C) {
      const Function &F = N.getFunction();
      if (Force || (!F.isDeclaration() && isFunctionInPrintList(F.getName()))) {
        return F.getParent();
      }
    }
    assert(!Force && "Expected a module");
    return nullptr;
  }

  if (const auto *L = unwrapIR<Loop>(IR)) {
    const Function *F = L->getHeader()->getParent();
    if (!Force && !isFunctionInPrintList(F->getName()))
      return nullptr;
    return F->getParent();
  }

  if (const auto *MF = unwrapIR<MachineFunction>(IR)) {
    if (!Force && !isFunctionInPrintList(MF->getName()))
      return nullptr;
    return MF->getFunction().getParent();
  }

  llvm_unreachable("Unknown IR unit");
}

void printIR(raw_ostream &OS, const Function *F) {
  if (!isFunctionInPrintList(F->getName()))
    return;
  OS << *F;
}

void printIR(raw_ostream &OS, const Module *M) {
  if (isFunctionInPrintList("*") || forcePrintModuleIR()) {
    M->print(OS, nullptr);
  } else {
    for (const auto &F : M->functions()) {
      printIR(OS, &F);
    }
  }
}

void printIR(raw_ostream &OS, const LazyCallGraph::SCC *C) {
  for (const LazyCallGraph::Node &N : *C) {
    const Function &F = N.getFunction();
    if (!F.isDeclaration() && isFunctionInPrintList(F.getName())) {
      F.print(OS);
    }
  }
}

void printIR(raw_ostream &OS, const Loop *L) {
  const Function *F = L->getHeader()->getParent();
  if (!isFunctionInPrintList(F->getName()))
    return;
  printLoop(const_cast<Loop &>(*L), OS);
}

void printIR(raw_ostream &OS, const MachineFunction *MF) {
  if (!isFunctionInPrintList(MF->getName()))
    return;
  MF->print(OS);
}

std::string getIRName(Any IR) {
  if (unwrapIR<Module>(IR))
    return "[module]";

  if (const auto *F = unwrapIR<Function>(IR))
    return F->getName().str();

  if (const auto *C = unwrapIR<LazyCallGraph::SCC>(IR))
    return C->getName();

  if (const auto *L = unwrapIR<Loop>(IR))
    return "loop %" + L->getName().str() + " in function " +
           L->getHeader()->getParent()->getName().str();

  if (const auto *MF = unwrapIR<MachineFunction>(IR))
    return MF->getName().str();

  llvm_unreachable("Unknown wrapped IR type");
}

bool moduleContainsFilterPrintFunc(const Module &M) {
  return any_of(M.functions(),
                [](const Function &F) {
                  return isFunctionInPrintList(F.getName());
                }) ||
         isFunctionInPrintList("*");
}

bool sccContainsFilterPrintFunc(const LazyCallGraph::SCC &C) {
  return any_of(C,
                [](const LazyCallGraph::Node &N) {
                  return isFunctionInPrintList(N.getName());
                }) ||
         isFunctionInPrintList("*");
}

bool shouldPrintIR(Any IR) {
  if (const auto *M = unwrapIR<Module>(IR))
    return moduleContainsFilterPrintFunc(*M);

  if (const auto *F = unwrapIR<Function>(IR))
    return isFunctionInPrintList(F->getName());

  if (const auto *C = unwrapIR<LazyCallGraph::SCC>(IR))
    return sccContainsFilterPrintFunc(*C);

  if (const auto *L = unwrapIR<Loop>(IR))
    return isFunctionInPrintList(L->getHeader()->getParent()->getName());

  if (const auto *MF = unwrapIR<MachineFunction>(IR))
    return isFunctionInPrintList(MF->getName());
  llvm_unreachable("Unknown wrapped IR type");
}

/// Generic IR-printing helper that unpacks a pointer to IRUnit wrapped into
/// Any and does actual print job.
void unwrapAndPrint(raw_ostream &OS, Any IR) {
  if (!shouldPrintIR(IR))
    return;

  if (forcePrintModuleIR()) {
    auto *M = unwrapModule(IR);
    assert(M && "should have unwrapped module");
    printIR(OS, M);
    return;
  }

  if (const auto *M = unwrapIR<Module>(IR)) {
    printIR(OS, M);
    return;
  }

  if (const auto *F = unwrapIR<Function>(IR)) {
    printIR(OS, F);
    return;
  }

  if (const auto *C = unwrapIR<LazyCallGraph::SCC>(IR)) {
    printIR(OS, C);
    return;
  }

  if (const auto *L = unwrapIR<Loop>(IR)) {
    printIR(OS, L);
    return;
  }

  if (const auto *MF = unwrapIR<MachineFunction>(IR)) {
    printIR(OS, MF);
    return;
  }
  llvm_unreachable("Unknown wrapped IR type");
}

// Return true when this is a pass for which changes should be ignored
bool isIgnored(StringRef PassID) {
  return isSpecialPass(PassID,
                       {"PassManager", "PassAdaptor", "AnalysisManagerProxy",
                        "DevirtSCCRepeatedPass", "ModuleInlinerWrapperPass",
                        "VerifierPass", "PrintModulePass", "PrintMIRPass",
                        "PrintMIRPreparePass"});
}

}

void PrintBitcodeInstrumentation::setup(
    StringRef SaveIRAfter,
    StringRef SaveIRAfterDirectory) {
  IRDumpDirectory = SaveIRAfterDirectory;
  while (!SaveIRAfter.empty()) {
    auto [X, XS] = SaveIRAfter.split(",");
    SaveIRAfter = XS;
    if (!X.empty()) {
      IRDumpPasses.push_back(X.str());
    }
  }
}

PrintBitcodeInstrumentation::~PrintBitcodeInstrumentation() {
  assert(PassRunDescriptorStack.empty() &&
         "PassRunDescriptorStack is not empty at exit");
}

static SmallString<32> getIRFileDisplayName(Any IR) {
  SmallString<32> Result;
  raw_svector_ostream ResultStream(Result);
  const Module *M = unwrapModule(IR);
  uint64_t NameHash = xxh3_64bits(M->getName());
  unsigned MaxHashWidth = sizeof(uint64_t) * 2;
  write_hex(ResultStream, NameHash, HexPrintStyle::Lower, MaxHashWidth);
  if (unwrapIR<Module>(IR)) {
    ResultStream << "-module";
  } else if (const auto *F = unwrapIR<Function>(IR)) {
    ResultStream << "-function-";
    auto FunctionNameHash = xxh3_64bits(F->getName());
    write_hex(ResultStream, FunctionNameHash, HexPrintStyle::Lower,
              MaxHashWidth);
  } else if (const auto *C = unwrapIR<LazyCallGraph::SCC>(IR)) {
    ResultStream << "-scc-";
    auto SCCNameHash = xxh3_64bits(C->getName());
    write_hex(ResultStream, SCCNameHash, HexPrintStyle::Lower, MaxHashWidth);
  } else if (const auto *L = unwrapIR<Loop>(IR)) {
    ResultStream << "-loop-";
    auto LoopNameHash = xxh3_64bits(L->getName());
    write_hex(ResultStream, LoopNameHash, HexPrintStyle::Lower, MaxHashWidth);
  } else if (const auto *MF = unwrapIR<MachineFunction>(IR)) {
    ResultStream << "-machine-function-";
    auto MachineFunctionNameHash = xxh3_64bits(MF->getName());
    write_hex(ResultStream, MachineFunctionNameHash, HexPrintStyle::Lower,
              MaxHashWidth);
  } else {
    llvm_unreachable("Unknown wrapped IR type");
  }
  return Result;
}

std::string PrintBitcodeInstrumentation::fetchDumpFilename(StringRef PassName,
                                                      Any IR) {
  const StringRef RootDirectory = IRDumpDirectory;
  assert(!RootDirectory.empty() &&
         "The flag -ir-dump-directory must be passed to dump IR to files");
  SmallString<128> ResultPath;
  ResultPath += RootDirectory;
  SmallString<64> Filename;
  raw_svector_ostream FilenameStream(Filename);
  FilenameStream << CurrentPassNumber;
  FilenameStream << "-";
  FilenameStream << getIRFileDisplayName(IR);
  FilenameStream << "-";
  FilenameStream << PassName;
  sys::path::append(ResultPath, Filename);
  return std::string(ResultPath);
}

enum class IRDumpFileSuffixType {
  Before,
  After,
  Invalidated,
};

static StringRef getFileSuffix(IRDumpFileSuffixType Type) {
  static constexpr std::array FileSuffixes = {"-before.ll", "-after.ll",
                                              "-invalidated.ll"};
  return FileSuffixes[static_cast<size_t>(Type)];
}

void PrintBitcodeInstrumentation::pushPassRunDescriptor(
    StringRef PassID, Any IR, std::string &DumpIRFilename) {
  const Module *M = unwrapModule(IR);
  PassRunDescriptorStack.emplace_back(
      PassRunDescriptor(M, DumpIRFilename, getIRName(IR), PassID));
}

PrintBitcodeInstrumentation::PassRunDescriptor
PrintBitcodeInstrumentation::popPassRunDescriptor(StringRef PassID) {
  assert(!PassRunDescriptorStack.empty() && "empty PassRunDescriptorStack");
  PassRunDescriptor Descriptor = PassRunDescriptorStack.pop_back_val();
  assert(Descriptor.PassID == PassID && "malformed PassRunDescriptorStack");
  return Descriptor;
}

// Callers are responsible for closing the returned file descriptor
static int prepareDumpIRFileDescriptor(const StringRef DumpIRFilename) {
  std::error_code EC;
  auto ParentPath = llvm::sys::path::parent_path(DumpIRFilename);
  if (!ParentPath.empty()) {
    std::error_code EC = llvm::sys::fs::create_directories(ParentPath);
    if (EC)
      report_fatal_error(Twine("Failed to create directory ") + ParentPath +
                         " to support -ir-dump-directory: " + EC.message());
  }
  int Result = 0;
  EC = sys::fs::openFile(DumpIRFilename, Result, sys::fs::CD_OpenAlways,
                         sys::fs::FA_Write, sys::fs::OF_Text);
  if (EC)
    report_fatal_error(Twine("Failed to open ") + DumpIRFilename +
                       " to support -ir-dump-directory: " + EC.message());
  return Result;
}

void PrintBitcodeInstrumentation::printBeforePass(StringRef PassID, Any IR) {
  if (isIgnored(PassID))
    return;

  std::string DumpIRFilename;
  if (!IRDumpDirectory.empty() &&
      (shouldPrintAfterPass(PassID)))
    DumpIRFilename = fetchDumpFilename(PassID, IR);

  // Saving Module for AfterPassInvalidated operations.
  // Note: here we rely on a fact that we do not change modules while
  // traversing the pipeline, so the latest captured module is good
  // for all print operations that has not happen yet.
  if (shouldPrintAfterPass(PassID))
    pushPassRunDescriptor(PassID, IR, DumpIRFilename);

  if (!shouldPrintIR(IR))
    return;

  ++CurrentPassNumber;
}

void PrintBitcodeInstrumentation::printAfterPass(StringRef PassID, Any IR) {
  if (isIgnored(PassID))
    return;

  if (!shouldPrintAfterPass(PassID))
    return;

  auto [M, DumpIRFilename, IRName, StoredPassID] = popPassRunDescriptor(PassID);
  assert(StoredPassID == PassID && "mismatched PassID");

  if (!shouldPrintIR(IR) ||
      (!shouldPrintAfterPass(PassID)))
    return;

  auto WriteIRToStream = [&](raw_ostream &Stream, const StringRef IRName) {
    Stream << "; *** IR Dump After ";
    Stream << StringRef(formatv("{0}", PassID)) << " on " << IRName << " ***\n";
    unwrapAndPrint(Stream, IR);
  };

  if (!IRDumpDirectory.empty()) {
    assert(!DumpIRFilename.empty() && "DumpIRFilename must not be empty and "
                                      "should be set in printBeforePass");
    const std::string DumpIRFilenameWithSuffix =
        DumpIRFilename + getFileSuffix(IRDumpFileSuffixType::After).str();
    llvm::raw_fd_ostream DumpIRFileStream{
        prepareDumpIRFileDescriptor(DumpIRFilenameWithSuffix),
        /* shouldClose */ true};
    WriteIRToStream(DumpIRFileStream, IRName);
  } else {
    WriteIRToStream(dbgs(), IRName);
  }
}

void PrintBitcodeInstrumentation::printAfterPassInvalidated(StringRef PassID) {
  if (isIgnored(PassID))
    return;

  if (!shouldPrintAfterPass(PassID))
    return;

  auto [M, DumpIRFilename, IRName, StoredPassID] = popPassRunDescriptor(PassID);
  assert(StoredPassID == PassID && "mismatched PassID");
  // Additional filtering (e.g. -filter-print-func) can lead to module
  // printing being skipped.
  if (!M ||
      (!shouldPrintAfterPass(PassID)))
    return;

  auto WriteIRToStream = [&](raw_ostream &Stream, const Module *M,
                             const StringRef IRName) {
    SmallString<20> Banner;
    Banner = formatv("; *** IR Dump After {0} on {1} (invalidated) ***", PassID,
                     IRName);
    Stream << Banner << "\n";
    printIR(Stream, M);
  };

  if (!IRDumpDirectory.empty()) {
    assert(!DumpIRFilename.empty() && "DumpIRFilename must not be empty and "
                                      "should be set in printBeforePass");
    const std::string DumpIRFilenameWithSuffix =
        DumpIRFilename + getFileSuffix(IRDumpFileSuffixType::Invalidated).str();
    llvm::raw_fd_ostream DumpIRFileStream{
        prepareDumpIRFileDescriptor(DumpIRFilenameWithSuffix),
        /* shouldClose */ true};
    WriteIRToStream(DumpIRFileStream, M, IRName);
  } else {
    WriteIRToStream(dbgs(), M, IRName);
  }
}

bool PrintBitcodeInstrumentation::shouldPrintAfterPass(StringRef PassID) {
  return is_contained(IRDumpPasses, PassID) || is_contained(IRDumpPasses, PIC->getPassNameForClassName(PassID));
}

void PrintBitcodeInstrumentation::registerCallbacks(
    PassInstrumentationCallbacks &PIC) {
  this->PIC = &PIC;
  if (IRDumpPasses.empty())
    return;

  // BeforePass callback is not just for printing, it also saves a Module
  // for later use in AfterPassInvalidated and keeps tracks of the
  // CurrentPassNumber.
  PIC.registerBeforeNonSkippedPassCallback(
      [this](StringRef P, Any IR) { this->printBeforePass(P, IR); });

  PIC.registerAfterPassCallback(
      [this](StringRef P, Any IR, const PreservedAnalyses &) {
        this->printAfterPass(P, IR);
      });
  PIC.registerAfterPassInvalidatedCallback(
      [this](StringRef P, const PreservedAnalyses &) {
        this->printAfterPassInvalidated(P);
      });
}
