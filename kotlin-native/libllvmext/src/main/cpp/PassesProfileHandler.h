// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#pragma once

#include <string>
#include <vector>

#include <PassesProfile.h>
#include <llvm/IR/PassInstrumentation.h>
#include <llvm/ADT/StringRef.h>
#include <llvm/Support/CBindingWrapping.h>

namespace llvm {

struct PassesProfile {
    std::string SerializedProfile;
};

DEFINE_SIMPLE_CONVERSION_FUNCTIONS(PassesProfile, LLVMKotlinPassesProfileRef)

class PassesProfileHandler {
public:
    explicit PassesProfileHandler(bool enabled);
    ~PassesProfileHandler();

    PassesProfileHandler(const PassesProfileHandler&) = delete;
    PassesProfileHandler(PassesProfileHandler&&) = delete;
    PassesProfileHandler& operator=(const PassesProfileHandler&) = delete;
    PassesProfileHandler& operator=(PassesProfileHandler&&) = delete;

    bool enabled() const { return enabled_; }

    PassesProfile serialize() const;

    void registerCallbacks(PassInstrumentationCallbacks &PIC);

private:
    struct Event;
    struct PendingEvent;

    void runBeforePass(StringRef P);
    void runAfterPass(StringRef P);

    bool enabled_ = false;
    std::vector<Event> events_;
    std::vector<PendingEvent> pending_events_stack_;
};

} // namespace llvm
