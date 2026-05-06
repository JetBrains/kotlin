// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "PassesProfileHandler.h"

#include "llvm/ADT/StringMap.h"
#include "llvm/ADT/Twine.h"

#include <chrono>
#include <sstream>

#ifdef __APPLE__
#include <mach/mach_time.h>
#endif

#include <fstream>

using namespace llvm;
using namespace llvm::kotlin;

static uint64_t getSystemMonotonicNanos() {
#ifdef __APPLE__
  static mach_timebase_info_data_t timebase;
  if (timebase.denom == 0) {
    mach_timebase_info(&timebase);
  }
  return mach_continuous_time() * timebase.numer / timebase.denom;
#else
  return std::chrono::duration_cast<std::chrono::nanoseconds>(
      std::chrono::steady_clock::now().time_since_epoch()
  ).count();
#endif
}

static void writeVarint(std::vector<uint8_t> &Buf, uint64_t Val) {
  while (Val >= 0x80) {
    Buf.push_back(static_cast<uint8_t>((Val & 0x7f) | 0x80));
    Val >>= 7;
  }
  Buf.push_back(static_cast<uint8_t>(Val & 0x7f));
}

static void writeString(std::vector<uint8_t> &Buf, uint32_t FieldNumber, const std::string &Str) {
  uint32_t Key = (FieldNumber << 3) | 2;
  writeVarint(Buf, Key);
  writeVarint(Buf, Str.size());
  Buf.insert(Buf.end(), Str.begin(), Str.end());
}

PassesProfileHandler::PassesProfileHandler(bool Enabled, const char *TracePath, uint64_t BaseTimestamp, uint64_t TrackUuid, const char *PipelineName)
    : Enabled(Enabled), TracePath(TracePath ? TracePath : ""), BaseTimestamp(BaseTimestamp), TrackUuid(TrackUuid), PipelineName(PipelineName ? PipelineName : "") {
  if (Enabled) {
    StartTimestamp = getSystemMonotonicNanos();
    Events.reserve(4096); // Pre-allocate memory to avoid reallocations during callbacks
  }
}

PassesProfileHandler::~PassesProfileHandler() = default;

void PassesProfileHandler::writeTraceEvents() const {
  if (!Enabled || TracePath.empty() || Events.empty())
    return;

  uint64_t WriteStartTimestamp = getSystemMonotonicNanos();

  std::ofstream Out(TracePath, std::ios::binary);
  if (!Out) return;

  // Pre-allocate a single buffer for the entire file payload.
  // Each event takes roughly 50-150 bytes, so let's allocate 128 bytes per event.
  std::vector<uint8_t> OutputBuffer;
  OutputBuffer.reserve(Events.size() * 128 + 256);

  // Reuse buffers for the sub-messages to avoid dynamic allocations
  std::vector<uint8_t> TrackEventBuf;
  TrackEventBuf.reserve(256);
  std::vector<uint8_t> TracePacketBuf;
  TracePacketBuf.reserve(512);
  std::vector<uint8_t> TempBuf;
  TempBuf.reserve(16);

  std::string PipelinePrefix = PipelineName + ".";

  for (const auto &E : Events) {
    TrackEventBuf.clear();

    // 1. Build TrackEvent sub-message

    // type (field 9)
    writeVarint(TrackEventBuf, (9 << 3) | 0);
    writeVarint(TrackEventBuf, E.IsBegin ? 1 : 2); // TYPE_SLICE_BEGIN = 1, TYPE_SLICE_END = 2

    // track_uuid (field 11)
    writeVarint(TrackEventBuf, (11 << 3) | 0);
    writeVarint(TrackEventBuf, TrackUuid);

    if (E.IsBegin) {
      // categories (field 22)
      writeString(TrackEventBuf, 22, "Kotlin");
      
      // name (field 23) - write directly without allocating a new concatenated string
      uint32_t NameKey = (23 << 3) | 2;
      writeVarint(TrackEventBuf, NameKey);
      writeVarint(TrackEventBuf, PipelinePrefix.size() + E.Name.size());
      TrackEventBuf.insert(TrackEventBuf.end(), PipelinePrefix.begin(), PipelinePrefix.end());
      TrackEventBuf.insert(TrackEventBuf.end(), E.Name.begin(), E.Name.end());
    }

    // 2. Build TracePacket message
    TracePacketBuf.clear();

    // timestamp (field 8)
    writeVarint(TracePacketBuf, (8 << 3) | 0);
    uint64_t EventTimestamp = BaseTimestamp + (E.Timestamp - StartTimestamp);
    writeVarint(TracePacketBuf, EventTimestamp);

    // trusted_packet_sequence_id (field 10)
    writeVarint(TracePacketBuf, (10 << 3) | 0);
    writeVarint(TracePacketBuf, static_cast<uint32_t>(TrackUuid));

    // track_event (field 11)
    writeVarint(TracePacketBuf, (11 << 3) | 2);
    writeVarint(TracePacketBuf, TrackEventBuf.size());
    TracePacketBuf.insert(TracePacketBuf.end(), TrackEventBuf.begin(), TrackEventBuf.end());

    // 3. Write TracePacket as length-delimited packet (tag 1, wire type 2 -> 0x0a)
    OutputBuffer.push_back(0x0a);
    
    TempBuf.clear();
    writeVarint(TempBuf, TracePacketBuf.size());
    OutputBuffer.insert(OutputBuffer.end(), TempBuf.begin(), TempBuf.end());
    OutputBuffer.insert(OutputBuffer.end(), TracePacketBuf.begin(), TracePacketBuf.end());
  }

  // Write everything in a single filesystem call!
  Out.write(reinterpret_cast<const char*>(OutputBuffer.data()), OutputBuffer.size());
  Out.close();

  // 4. Record and write a slice for the serialization and file writing duration after the main file is closed
  uint64_t WriteEndTimestamp = getSystemMonotonicNanos();

  std::ofstream AppendOut(TracePath, std::ios::binary | std::ios::app);
  if (!AppendOut) return;

  // BEGIN Event for WriteTraceFile
  {
    TrackEventBuf.clear();
    // type (field 9)
    writeVarint(TrackEventBuf, (9 << 3) | 0);
    writeVarint(TrackEventBuf, 1); // TYPE_SLICE_BEGIN = 1
    // track_uuid (field 11)
    writeVarint(TrackEventBuf, (11 << 3) | 0);
    writeVarint(TrackEventBuf, TrackUuid);
    // categories (field 22)
    writeString(TrackEventBuf, 22, "Kotlin");
    // name (field 23)
    std::string FullName = PipelineName + ".WriteTraceFile";
    writeString(TrackEventBuf, 23, FullName);

    TracePacketBuf.clear();
    // timestamp (field 8)
    writeVarint(TracePacketBuf, (8 << 3) | 0);
    uint64_t EventTimestamp = BaseTimestamp + (WriteStartTimestamp - StartTimestamp);
    writeVarint(TracePacketBuf, EventTimestamp);
    // trusted_packet_sequence_id (field 10)
    writeVarint(TracePacketBuf, (10 << 3) | 0);
    writeVarint(TracePacketBuf, static_cast<uint32_t>(TrackUuid));
    // track_event (field 11)
    writeVarint(TracePacketBuf, (11 << 3) | 2);
    writeVarint(TracePacketBuf, TrackEventBuf.size());
    TracePacketBuf.insert(TracePacketBuf.end(), TrackEventBuf.begin(), TrackEventBuf.end());

    AppendOut.put(0x0a);
    TempBuf.clear();
    writeVarint(TempBuf, TracePacketBuf.size());
    AppendOut.write(reinterpret_cast<const char*>(TempBuf.data()), TempBuf.size());
    AppendOut.write(reinterpret_cast<const char*>(TracePacketBuf.data()), TracePacketBuf.size());
  }

  // END Event for WriteTraceFile
  {
    TrackEventBuf.clear();
    // type (field 9)
    writeVarint(TrackEventBuf, (9 << 3) | 0);
    writeVarint(TrackEventBuf, 2); // TYPE_SLICE_END = 2
    // track_uuid (field 11)
    writeVarint(TrackEventBuf, (11 << 3) | 0);
    writeVarint(TrackEventBuf, TrackUuid);

    TracePacketBuf.clear();
    // timestamp (field 8)
    writeVarint(TracePacketBuf, (8 << 3) | 0);
    uint64_t EventTimestamp = BaseTimestamp + (WriteEndTimestamp - StartTimestamp);
    writeVarint(TracePacketBuf, EventTimestamp);
    // trusted_packet_sequence_id (field 10)
    writeVarint(TracePacketBuf, (10 << 3) | 0);
    writeVarint(TracePacketBuf, static_cast<uint32_t>(TrackUuid));
    // track_event (field 11)
    writeVarint(TracePacketBuf, (11 << 3) | 2);
    writeVarint(TracePacketBuf, TrackEventBuf.size());
    TracePacketBuf.insert(TracePacketBuf.end(), TrackEventBuf.begin(), TrackEventBuf.end());

    AppendOut.put(0x0a);
    TempBuf.clear();
    writeVarint(TempBuf, TracePacketBuf.size());
    AppendOut.write(reinterpret_cast<const char*>(TempBuf.data()), TempBuf.size());
    AppendOut.write(reinterpret_cast<const char*>(TracePacketBuf.data()), TracePacketBuf.size());
  }
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
  uint64_t ts = getSystemMonotonicNanos();
  Events.push_back({P.str(), ts, true});
}

void PassesProfileHandler::runAfterPass(StringRef P) {
  uint64_t ts = getSystemMonotonicNanos();
  Events.push_back({P.str(), ts, false});
}

void LLVMKotlinDisposePassesProfile(LLVMKotlinPassesProfileRef P) {
  // No-op since we don't return profile objects anymore.
}
