// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include <functional>

#include "llvm/ADT/DenseMap.h"
#include "llvm/ADT/StringSet.h"
#include "llvm/IR/PassManager.h"

namespace llvm {
class Comdat;
class GlobalValue;
class Module;

class KotlinHiddenPass : public PassInfoMixin<KotlinHiddenPass> {
public:
    KotlinHiddenPass();

    /// Run the internalizer on \p TheModule, returns true if any changes was
    /// made.
    bool internalizeModule(Module &TheModule);

    PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);

private:
  struct ComdatInfo {
    // The number of members. A comdat with one member which is not externally
    // visible can be freely dropped.
    size_t Size = 0;
    // Whether the comdat has an externally visible member.
    bool External = false;
  };

  bool IsWasm = false;

  /// Client supplied callback to control wheter a symbol must be preserved.
  const std::function<bool(const GlobalValue &)> MustPreserveGV;
  /// Set of symbols private to the compiler that this pass should not touch.
  StringSet<> AlwaysPreserved;

  /// Return false if we're allowed to internalize this GV.
  bool shouldPreserveGV(const GlobalValue &GV);
  /// Internalize GV if it is possible to do so, i.e. it is not externally
  /// visible and is not a member of an externally visible comdat.
  bool maybeInternalize(GlobalValue &GV,
                        DenseMap<const Comdat *, ComdatInfo> &ComdatMap);
  /// If GV is part of a comdat and is externally visible, keep track of its
  /// comdat so that we don't internalize any of its members.
  void checkComdat(GlobalValue &GV,
                   DenseMap<const Comdat *, ComdatInfo> &ComdatMap);
};

}
