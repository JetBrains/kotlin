// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include "PassesProfileHandler.h"

#include <chrono>
#include <sstream>

#include <llvm/ADT/StringMap.h>
#include <llvm/ADT/Twine.h>
#include <llvm/Support/Error.h>

using namespace llvm;

using Clock = std::chrono::system_clock;

struct PassesProfileHandler::Event {
    Clock::time_point StartedAt{};
    std::chrono::nanoseconds Duration{};
    StringMap<Event> Children;

    Event() = default;
    Event(const Event&) = default;
    Event(Event&&) = default;
    Event& operator=(const Event&) = default;
    Event& operator=(Event&&) = default;
};

namespace {

using Event = PassesProfileHandler::Event;

void Finalize(StringMapEntry<Event>& Entry, StringRef P) {
    auto Pass = Entry.getKey();
    if (P != Pass) {
        report_fatal_error(Twine("Mismatched event finalization. Expected pass ") + Pass + "; actual " + P);
    }
    auto& Event = Entry.getValue();
    Event.Duration += Clock::now() - Event.StartedAt;
    Event.StartedAt = Clock::time_point();
}

void Dump(const StringMapEntry<Event>& Entry, std::ostream& Out, std::vector<const StringMapEntry<Event>*>& Parents) {
    for (const auto* E: Parents) {
        Out << std::string_view(E->getKey()) << '.';
    }
    const auto& Event = Entry.getValue();
    Out << std::string_view(Entry.getKey())
        << "\t" << Event.Duration.count()
        << "\n";
    Parents.push_back(&Entry);
    for (const auto& E: Event.Children) {
        Dump(E, Out, Parents);
    }
    Parents.pop_back();
}

}

PassesProfileHandler::PassesProfileHandler(bool enabled) : enabled_(enabled) {}

PassesProfileHandler::~PassesProfileHandler() = default;

PassesProfile PassesProfileHandler::serialize() const {
    if (!pending_events_stack_.empty()) {
        report_fatal_error(Twine("Mismatched event finalization. Pending event stack is not empty ") + Twine(pending_events_stack_.size()));
    }

    std::stringstream Out;
    std::vector<const StringMapEntry<Event>*> Parents;
    for (const auto& E : roots_) {
        Dump(E, Out, Parents);
    }
    return PassesProfile{Out.str()};
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
    auto& Map = pending_events_stack_.empty() ? roots_ : pending_events_stack_.back()->second.Children;
    auto [It, _] = Map.try_emplace(P);
    auto& Entry = *It;
    pending_events_stack_.push_back(&Entry);
    Entry.getValue().StartedAt = Clock::now();
}

void PassesProfileHandler::runAfterPass(StringRef P) {
    Finalize(*pending_events_stack_.back(), P);
    pending_events_stack_.pop_back();
}

extern "C" const char* LLVMKotlinPassesProfileAsString(LLVMKotlinPassesProfileRef P) {
    return unwrap(P)->SerializedProfile.c_str();
}

extern "C" void LLVMKotlinDisposePassesProfile(LLVMKotlinPassesProfileRef P) {
    delete unwrap(P);
}
