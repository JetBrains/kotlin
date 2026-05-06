// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "PassesProfile.h"

#include "llvm/ADT/StringMap.h"
#include "llvm/ADT/StringMapEntry.h"
#include "llvm/ADT/StringRef.h"
#include "llvm/IR/PassInstrumentation.h"
#include "llvm/Support/CBindingWrapping.h"

#include <string>
#include <vector>

namespace llvm::kotlin {

struct LLVMPassEvent {
  std::string Name;
  uint64_t Timestamp;
  bool IsBegin;
};

class PassesProfileHandler {
public:
  PassesProfileHandler(bool Enabled, const char *TracePath, uint64_t BaseTimestamp, uint64_t TrackUuid, const char *PipelineName);
  ~PassesProfileHandler();

  PassesProfileHandler(const PassesProfileHandler &) = delete;
  PassesProfileHandler(PassesProfileHandler &&) = delete;
  PassesProfileHandler &operator=(const PassesProfileHandler &) = delete;
  PassesProfileHandler &operator=(PassesProfileHandler &&) = delete;

  bool isEnabled() const { return Enabled; }

  void writeTraceEvents() const;

  void registerCallbacks(PassInstrumentationCallbacks &PIC);

private:
  void runBeforePass(StringRef P);
  void runAfterPass(StringRef P);

  bool Enabled = false;
  std::string TracePath;
  uint64_t BaseTimestamp = 0;
  uint64_t TrackUuid = 0;
  std::string PipelineName;
  uint64_t StartTimestamp = 0;
  std::vector<LLVMPassEvent> Events;
};

} // namespace llvm::kotlin
