// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/ADT/StringSet.h"
#include "llvm/IR/PassInstrumentation.h"

namespace llvm::kotlin {

// Very similar to `PrintIRInstrumentation`.
class PrintBitcodeInstrumentation {
public:
    ~PrintBitcodeInstrumentation();

    void setup(StringRef SaveIRAfter, StringRef SaveIRAfterDirectory);

    void registerCallbacks(PassInstrumentationCallbacks &PIC);

private:
  struct PassRunDescriptor {
    const Module *M;
    const std::string DumpIRFilename;
    const std::string IRName;
    const StringRef PassID;

    PassRunDescriptor(const Module *M, std::string DumpIRFilename,
                      std::string IRName, const StringRef PassID)
        : M{M}, DumpIRFilename{DumpIRFilename}, IRName{IRName}, PassID(PassID) {
    }
  };

  void printBeforePass(StringRef PassID, Any IR);
  void printAfterPass(StringRef PassID, Any IR);
  void printAfterPassInvalidated(StringRef PassID);

  bool shouldPrintAfterPass(StringRef PassID);

  void pushPassRunDescriptor(StringRef PassID, Any IR,
                             std::string &DumpIRFilename);
  PassRunDescriptor popPassRunDescriptor(StringRef PassID);
  std::string fetchDumpFilename(StringRef PassId, Any IR);

  PassInstrumentationCallbacks *PIC;
  /// Stack of Pass Run descriptions, enough to print the IR unit after a given
  /// pass.
  SmallVector<PassRunDescriptor, 2> PassRunDescriptorStack;

  /// Used for print-at-pass-number
  unsigned CurrentPassNumber = 0;

  std::vector<std::string> IRDumpPasses;
  std::string IRDumpDirectory;
};

}
