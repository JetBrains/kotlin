// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "PassesProfile.h"

#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/StringMap.h"
#include "llvm/ADT/StringMapEntry.h"
#include "llvm/ADT/StringRef.h"
#include "llvm/IR/PassInstrumentation.h"
#include "llvm/Support/CBindingWrapping.h"

#include <string>

namespace llvm::kotlin {

struct PassesProfile {
  std::string SerializedProfile;
};

DEFINE_SIMPLE_CONVERSION_FUNCTIONS(PassesProfile, LLVMKotlinPassesProfileRef)

class PassesProfileHandler {
public:
  struct Event;

  explicit PassesProfileHandler(bool Enabled);
  ~PassesProfileHandler();

  PassesProfileHandler(const PassesProfileHandler &) = delete;
  PassesProfileHandler(PassesProfileHandler &&) = delete;
  PassesProfileHandler &operator=(const PassesProfileHandler &) = delete;
  PassesProfileHandler &operator=(PassesProfileHandler &&) = delete;

  bool isEnabled() const { return Enabled; }

  PassesProfile serialize() const;

  void registerCallbacks(PassInstrumentationCallbacks &PIC);

private:
  void runBeforePass(StringRef P);
  void runAfterPass(StringRef P);

  bool Enabled = false;
  StringMap<Event> Roots;
  SmallVector<StringMapEntry<Event> *> PendingEventsStack;
};

} // namespace llvm::kotlin
