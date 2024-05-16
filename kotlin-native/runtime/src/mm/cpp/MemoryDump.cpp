/*
 * Copyright 2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <algorithm>
#include <string.h>
#include <unordered_set>
#include <queue>
#include <stdio.h>

#include "MemoryDump.hpp"

#include "Porting.h"
#include "TypeInfo.h"
#include "KString.h"
#include "ObjectTraversal.hpp"
#include "GlobalData.hpp"
#include "RootSet.hpp"
#include "ThreadData.hpp"

namespace kotlin::mm {

class MemoryDumper {
 public:
  MemoryDumper(FILE *file): file_(file), error_(false) {}

  // Dumps the memory and returns the success flag.
  bool Dump() {
    Dump("Kotlin/Native dump 1.0.5");
    Dump(konan::isLittleEndian());
    Dump8(sizeof(size_t));

    for (auto& thread : mm::GlobalData::Instance().threadRegistry().LockForIter()) {
        thread.Publish();
    }

    // Dump global roots.
    for (auto value : mm::GlobalRootSet()) {
      DumpTransitively(value);
    }

    // Dump threads and thread roots.
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().LockForIter()) {
      Dump(thread);
      for (auto value : mm::ThreadRootSet(thread)) {
        DumpTransitively(thread, value);
      }
    }

    // Dump objects from the heap.
    GlobalData::Instance().allocator().TraverseAllocatedObjects([&](auto obj) {
      DumpTransitively(obj);
    });

    // Dump extra objects from the heap.
    GlobalData::Instance().allocator().TraverseAllocatedExtraObjects([&](auto extraObj) {
      DumpTransitively(extraObj);
    });

    DumpEnqueuedNonHeapObjects();

    return !error_;
  }

 private:
  void Dump(const void* data, size_t size) {
    if (!error_ && fwrite(data, 1, size, file_) != size) {
      error_ = true;
    }
  }

  template <typename T>
  void DumpBytes(T data) {
    Dump(&data, sizeof(data));
  }

  void Dump(bool b) {
    DumpBytes<uint8_t>(b ? 1 : 0);
  }

  void Dump8(uint8_t i) {
    DumpBytes(i);
  }

  void Dump32(uint32_t i) {
    DumpBytes(i);
  }

  void Dump(const char* str) {
    Dump(str, strlen(str) + 1);
  }

  void DumpString(ObjHeader* obj) {
    char* str = CreateCStringFromString(obj);
    Dump(str);
    DisposeCString(str);
  }

  void DumpStringOrEmptyIfNull(ObjHeader* obj) {
    if (!isNullOrMarker(obj)) {
      DumpString(obj);
    } else {
      Dump("");
    }
  }

  void Dump(ThreadData& thread) {
    Dump8(TAG_THREAD);
    DumpBytes(&thread);
  }

  void Dump(GlobalRootSet::Value& value) {
    Dump8(TAG_GLOBAL_ROOT);
    DumpBytes(UInt8(value.source));
    DumpBytes(value.object);
  }

  void Dump(ThreadData& thread, ThreadRootSet::Value& value) {
    Dump8(TAG_THREAD_ROOT);
    DumpBytes(&thread);
    DumpBytes(UInt8(value.source));
    DumpBytes(value.object);
  }

  void Dump(const TypeInfo* type, ObjHeader* obj) {
    Dump8(TAG_OBJECT);
    DumpBytes(obj);
    DumpBytes(type);

    size_t size = type->instanceSize_;
    size_t dataOffset = sizeof(TypeInfo*);
    size_t dataSize = size - dataOffset;
    uint8_t* data = reinterpret_cast<uint8_t*>(obj) + dataOffset;

    Dump32(dataSize);
    Dump(data, dataSize);
  }

  void Dump(const TypeInfo* type, ArrayHeader* arr) {
    Dump8(TAG_ARRAY);
    DumpBytes(arr);
    DumpBytes(type);

    uint32_t count = arr->count_;
    Dump32(count);

    int32_t elementSize = -type->instanceSize_;
    size_t dataOffset = alignUp(sizeof(ArrayHeader), elementSize);
    size_t dataSize = elementSize * count;
    Dump32(dataSize);

    uint8_t* data = reinterpret_cast<uint8_t*>(arr) + dataOffset;
    Dump(data, dataSize);
  }

  void Dump(ObjHeader* obj) {
    const TypeInfo* type = obj->type_info();
    if (type->IsArray()) {
      Dump(type, obj->array());
    } else {
      Dump(type, obj);
    }
  }

  void Dump(const ExtendedTypeInfo* extendedInfo) {
    size_t dataOffset = sizeof(TypeInfo*);
    if (extendedInfo->fieldsCount_ < 0) {
      uint8_t elementType = -extendedInfo->fieldsCount_;
      Dump8(elementType);
    } else {
      int32_t fieldsCount = extendedInfo->fieldsCount_;
      Dump32(fieldsCount);
      for (int32_t i = 0; i < fieldsCount; i++) {
        Dump32(extendedInfo->fieldOffsets_[i] - dataOffset);
        Dump8(extendedInfo->fieldTypes_[i]);
        Dump(extendedInfo->fieldNames_[i]);
      }
    }
  }

  void DumpOffsets(const TypeInfo* type) {
    if (type->IsArray()) {
      Dump32(0);
    } else {
      int32_t count = type->objOffsetsCount_;
      Dump32(count);
      for (int32_t i = 0; i < count; i++) {
        Dump32(type->objOffsets_[i]);
      }
    }
  }

  void Dump(const TypeInfo* type) {
    Dump8(TAG_TYPE);
    DumpBytes(type);

    bool isArray = type->IsArray();
    bool isExtended = type->extendedInfo_ != nullptr;
    uint8_t flags = (isArray ? TYPE_FLAG_ARRAY : 0) | (isExtended ? TYPE_FLAG_EXTENDED : 0);
    Dump8(flags);

    DumpBytes(type->superType_);

    DumpStringOrEmptyIfNull(type->packageName_);
    DumpStringOrEmptyIfNull(type->relativeName_);

    Dump32(std::abs(-type->instanceSize_));

    if (isExtended) {
      Dump(type->extendedInfo_);
    }
  }

  void DumpTransitively(const TypeInfo* type) {
    if (dumpedTypes_.insert(type).second) {
      if (type->superType_ != nullptr) {
        DumpTransitively(type->superType_);
      }

      Dump(type);
    }
  }

  void DumpTransitively(ObjHeader* obj) {
    DumpTransitively(obj->type_info());

    Dump(obj);

    traverseReferredObjects(obj, [&](auto refObj) {
      Enqueue(refObj);
    });
  }

  void DumpTransitively(ExtraObjectData* extraObj) {
    Dump8(TAG_EXTRA_OBJECT);
    DumpBytes(extraObj);

    ObjHeader* baseObj = extraObj->GetBaseObject();
    DumpBytes(baseObj);

    if (!isNullOrMarker(baseObj)) {
      Enqueue(baseObj);
    }

    void *associatedObject =
  #ifdef KONAN_OBJC_INTEROP
      extraObj->AssociatedObject();
  #else
      nullptr;
  #endif
    DumpBytes(associatedObject);
  }

  void DumpNonHeapTransitively(ObjHeader* obj) {
    if (dumpedNonHeapObjs_.insert(obj).second) {
      DumpTransitively(obj);
    }
  }

  void DumpTransitively(GlobalRootSet::Value& value) {
    ObjHeader* obj = value.object;
    if (isNullOrMarker(obj)) {
      return;
    }

    Dump(value);

    Enqueue(obj);
  }

  void DumpTransitively(ThreadData& thread, ThreadRootSet::Value& value) {
    ObjHeader* obj = value.object;
    if (isNullOrMarker(obj)) {
      return;
    }

    Dump(thread, value);

    Enqueue(obj);
  }

  void Enqueue(ObjHeader* obj) {
    if (!obj->heap()) {
      nonHeapObjsToDump_.push(obj);
    }
  }

  void DumpEnqueuedNonHeapObjects() {
    while (!nonHeapObjsToDump_.empty()) {
      auto obj = nonHeapObjsToDump_.front();
      nonHeapObjsToDump_.pop();
      DumpNonHeapTransitively(obj);
    }
  }

  uint8_t UInt8(GlobalRootSet::Source source) {
    switch (source) {
      case GlobalRootSet::Source::kGlobal:
        return 1;
      case GlobalRootSet::Source::kStableRef:
        case GlobalRootSet::Source::kWeakRef:
        case GlobalRootSet::Source::kObjcBackRef:
        return 2;
    }
  }

  uint8_t UInt8(ThreadRootSet::Source source) {
    switch (source) {
      case ThreadRootSet::Source::kStack:
        return 1;
      case ThreadRootSet::Source::kTLS:
        return 2;
    }
  }

  const uint8_t TAG_TYPE         = 0x01;
  const uint8_t TAG_OBJECT       = 0x02;
  const uint8_t TAG_ARRAY        = 0x03;
  const uint8_t TAG_EXTRA_OBJECT = 0x04;
  const uint8_t TAG_THREAD       = 0x05;
  const uint8_t TAG_GLOBAL_ROOT  = 0x06;
  const uint8_t TAG_THREAD_ROOT  = 0x07;

  const uint8_t TYPE_FLAG_ARRAY    = 1 << 0;
  const uint8_t TYPE_FLAG_EXTENDED = 1 << 1;

  // Target file.
  FILE* file_;

  // True if there was an error.
  bool error_;

  // A set of already dumped type pointers.
  std::unordered_set<const TypeInfo*> dumpedTypes_;

  // A set of already dumped non-heap objects.
  std::unordered_set<ObjHeader*> dumpedNonHeapObjs_;

  // A queue of non-heap objects to dump.
  std::queue<ObjHeader*> nonHeapObjsToDump_;
};

bool DumpMemory(int fd) {
  FILE* file = fdopen(fd, "w");
  if (!file) return false;
  bool dumped = MemoryDumper(file).Dump();
  fflush(file);
  return dumped;
}

} // namespace kotlin::mm
