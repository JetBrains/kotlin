// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include "PassesProfileHandler.h"

#include <chrono>
#include <iostream>
#include <sstream>

#include <llvm/ADT/StringMap.h>
#include <llvm/ADT/Twine.h>
#include <llvm/IR/Function.h>
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

PassesProfileHandler::~PassesProfileHandler() {
    std::cerr << "\n";
    // NOTE: what.
    events_per_function_.clear();
    pending_events_per_function_.clear();
    std::cerr << "\n";
}

PassesProfile PassesProfileHandler::serialize() const {
    {
        for (const auto& [name, events] : pending_events_per_function_) {
            if (!events.empty()) {
                report_fatal_error(Twine("Mismatched event finalization. Stack not empty"));
            }
        }

        /*
        for (const auto& [name, duration] : events_per_function_) {
            std::cerr << "FUN " << std::string_view(name) << " " << std::chrono::duration_cast<std::chrono::microseconds>(duration).count() << " us\n";
        }
        */
    }

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

namespace {

const Function* unwrapFunction(Any IR) {
    auto** IRPtr = any_cast<const Function*>(&IR);
    return IRPtr ? *IRPtr : nullptr;
}

}

void PassesProfileHandler::registerCallbacks(PassInstrumentationCallbacks &PIC) {
    if (!enabled_) {
        return;
    }
    PIC.registerBeforeNonSkippedPassCallback(
            [this](StringRef P, Any IR) { runBeforePass(P, unwrapFunction(IR)); });
    PIC.registerAfterPassCallback(
            [this](StringRef P, Any IR, const PreservedAnalyses &) {
                runAfterPass(P, unwrapFunction(IR));
            },
            true);
    PIC.registerAfterPassInvalidatedCallback(
            [this](StringRef P, const PreservedAnalyses &) { runAfterPass(P, nullptr); },
            true);
    PIC.registerBeforeAnalysisCallback(
            [this](StringRef P, Any IR) { runBeforePass(P, unwrapFunction(IR)); });
    PIC.registerAfterAnalysisCallback(
            [this](StringRef P, Any IR) { runAfterPass(P, unwrapFunction(IR)); }, true);
}

void PassesProfileHandler::runBeforePass(StringRef P, const Function* F) {
    if (F) {
        auto name = F->getName().str();
        auto [iter, ignored] = pending_events_per_function_.insert(std::make_pair(name, std::vector<PendingEvent>()));
        auto& pending_events_stack = iter->second;

        PendingEvent* parent = nullptr;
        if (!pending_events_stack.empty())
            parent = &pending_events_stack.back();
        pending_events_stack.emplace_back(parent, P);
    }
    PendingEvent* parent = nullptr;
    if (!pending_events_stack_.empty())
        parent = &pending_events_stack_.back();
    pending_events_stack_.emplace_back(parent, P);
}

void PassesProfileHandler::runAfterPass(StringRef P, const Function* F) {
    if (F) {
        auto name = F->getName().str();
        auto& pending_events_stack = pending_events_per_function_[name];
        auto event = pending_events_stack.back().finalize(P);
        if (name == "kfun:org.jetbrains.kotlinconf.screens.ScheduleViewModel.ScheduleViewModel$1.invoke#internal") {
            std::cerr << "PASS " << event.Name << " " << std::chrono::duration_cast<std::chrono::microseconds>(event.Duration).count() << "\n";
        }
        pending_events_stack.pop_back();
        if (pending_events_stack.empty()) {
            auto [iter, ignored] = events_per_function_.insert(std::make_pair(name, std::chrono::nanoseconds(0)));
            auto& duration = iter->second;
            duration += event.Duration;
        }
    }
    events_.emplace_back(pending_events_stack_.back().finalize(P));
    pending_events_stack_.pop_back();
}

extern "C" const char* LLVMKotlinPassesProfileAsString(LLVMKotlinPassesProfileRef P) {
    return unwrap(P)->SerializedProfile.c_str();
}

extern "C" void LLVMKotlinDisposePassesProfile(LLVMKotlinPassesProfileRef P) {
    delete unwrap(P);
}
