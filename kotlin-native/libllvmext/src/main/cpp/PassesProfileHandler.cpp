// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include "PassesProfileHandler.h"

#include <chrono>
#include <sstream>

#include <llvm/ADT/StringMap.h>
#include <llvm/ADT/Twine.h>
#include <llvm/Support/Error.h>

using namespace llvm;

struct PassesProfileHandler::Event {
    std::string Name;
    std::chrono::nanoseconds Duration;
};

struct PassesProfileHandler::PendingEvent {
public:
    explicit PendingEvent(PendingEvent* Parent, StringRef P) : Name(Parent ? (Parent->Name + "." + P).str() : P), Pass(P), StartedAt(std::chrono::system_clock::now()) {}

    Event finalize(StringRef P) {
        if (P != Pass) {
            report_fatal_error(Twine("Mismatched event finalization. Expected pass ") + Pass + "; actual " + P);
        }
        return { Name, std::chrono::system_clock::now() - StartedAt };
    }

private:
    std::string Name;
    std::string Pass;
    std::chrono::system_clock::time_point StartedAt;
};

PassesProfileHandler::PassesProfileHandler(bool enabled) : enabled_(enabled) {}

PassesProfileHandler::~PassesProfileHandler() = default;

PassesProfile PassesProfileHandler::serialize() const {
    if (!pending_events_stack_.empty()) {
        report_fatal_error(Twine("Mismatched event finalization. Pending event stack is not empty ") + Twine(pending_events_stack_.size()));
    }

    // Combine same passes into one entry.
    StringMap<std::chrono::nanoseconds> events;
    for (const auto& [name, duration] : events_) {
        auto [it, inserted] = events.insert(std::make_pair(name, duration));
        if (!inserted) {
            it->second += duration;
        }
    }
    std::stringstream out;
    for (const auto& kvp : events) {
        out << std::string_view(kvp.first())
            << "\t" << kvp.second.count()
            << "\n";
    }
    return PassesProfile{out.str()};
}

void PassesProfileHandler::registerCallbacks(PassInstrumentationCallbacks &PIC) {
    if (!enabled_) {
        return;
    }
    PIC.registerBeforeNonSkippedPassCallback(
            [this](StringRef P, Any IR) { runBeforePass(P); });
    PIC.registerAfterPassCallback(
            [this](StringRef P, Any IR, const PreservedAnalyses &) {
                runAfterPass(P);
            },
            true);
    PIC.registerAfterPassInvalidatedCallback(
            [this](StringRef P, const PreservedAnalyses &) { runAfterPass(P); },
            true);
    PIC.registerBeforeAnalysisCallback(
            [this](StringRef P, Any IR) { runBeforePass(P); });
    PIC.registerAfterAnalysisCallback(
            [this](StringRef P, Any IR) { runAfterPass(P); }, true);
}

void PassesProfileHandler::runBeforePass(StringRef P) {
    PendingEvent* parent = nullptr;
    if (!pending_events_stack_.empty())
        parent = &pending_events_stack_.back();
    pending_events_stack_.emplace_back(parent, P);
}

void PassesProfileHandler::runAfterPass(StringRef P) {
    events_.emplace_back(pending_events_stack_.back().finalize(P));
    pending_events_stack_.pop_back();
}

extern "C" const char* LLVMKotlinPassesProfileAsString(LLVMKotlinPassesProfileRef P) {
    return unwrap(P)->SerializedProfile.c_str();
}

extern "C" void LLVMKotlinDisposePassesProfile(LLVMKotlinPassesProfileRef P) {
    delete unwrap(P);
}
