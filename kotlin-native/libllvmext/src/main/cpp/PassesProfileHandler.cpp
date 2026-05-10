// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "PassesProfileHandler.h"

#include "llvm/ADT/StringMap.h"
#include "llvm/ADT/Twine.h"
#include "llvm/Support/FormatVariadic.h"
#include "llvm/Support/raw_ostream.h"

#include <chrono>

using namespace llvm;
using namespace llvm::kotlin;

using Clock = std::chrono::system_clock;

struct PassesProfileHandler::Event {
  Clock::time_point StartedAt{};
  std::chrono::nanoseconds Duration{};
  StringMap<Event> Children;

  Event() = default;
  Event(const Event &) = default;
  Event(Event &&) = default;
  Event &operator=(const Event &) = default;
  Event &operator=(Event &&) = default;
};

using Event = PassesProfileHandler::Event;

static void Finalize(StringMapEntry<Event> &Entry, StringRef P) {
  auto Pass = Entry.getKey();
  if (P != Pass) {
    report_fatal_error(Twine("Mismatched event finalization. Expected pass ") +
                       Pass + "; actual " + P);
  }
  auto &Event = Entry.getValue();
  Event.Duration += Clock::now() - Event.StartedAt;
  Event.StartedAt = Clock::time_point();
}

static void Dump(const StringMapEntry<Event> &Entry, raw_ostream &Out,
                 SmallVector<const StringMapEntry<Event> *> &Parents) {
  for (const auto *E : Parents) {
    Out << formatv("{0}.", E->getKey());
  }
  const auto &Event = Entry.getValue();
  Out << formatv("{0}\t{1}\n", Entry.getKey(), Event.Duration.count());
  Parents.push_back(&Entry);
  for (const auto &E : Event.Children) {
    Dump(E, Out, Parents);
  }
  Parents.pop_back();
}

PassesProfileHandler::PassesProfileHandler(bool Enabled) : Enabled(Enabled) {}

PassesProfileHandler::~PassesProfileHandler() = default;

PassesProfile PassesProfileHandler::serialize() const {
  if (!PendingEventsStack.empty()) {
    report_fatal_error(Twine("Mismatched event finalization. Pending event "
                             "stack is not empty ") +
                       Twine(PendingEventsStack.size()));
  }

  std::string Out;
  raw_string_ostream OutStream(Out);
  SmallVector<const StringMapEntry<Event> *> Parents;
  for (const auto &E : Roots) {
    Dump(E, OutStream, Parents);
  }
  return PassesProfile{Out};
}

void PassesProfileHandler::registerCallbacks(
    PassInstrumentationCallbacks &PIC) {
  if (!Enabled)
    return;

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
  auto &Map = PendingEventsStack.empty()
                  ? Roots
                  : PendingEventsStack.back()->second.Children;
  auto [It, _] = Map.try_emplace(P);
  auto &Entry = *It;
  PendingEventsStack.push_back(&Entry);
  Entry.getValue().StartedAt = Clock::now();
}

void PassesProfileHandler::runAfterPass(StringRef P) {
  Finalize(*PendingEventsStack.back(), P);
  PendingEventsStack.pop_back();
}

const char *LLVMKotlinPassesProfileAsString(LLVMKotlinPassesProfileRef P) {
  return unwrap(P)->SerializedProfile.c_str();
}

void LLVMKotlinDisposePassesProfile(LLVMKotlinPassesProfileRef P) {
  delete unwrap(P);
}
