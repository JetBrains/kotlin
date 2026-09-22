/*
 * Copyright 2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "mm/MemoryDump.hpp"

#include <cerrno>
#include <cstdio>
#include <cstring>
#include <queue>
#include <type_traits>
#include <unordered_set>

// Konan tvOS/watchOS sysroots do not ship libz; a global -lz breaks linking there.
#if KONAN_LINUX || KONAN_ANDROID || KONAN_MACOSX || KONAN_IOS || KONAN_WINDOWS
#define KONAN_HAS_ZLIB 1
#include <algorithm>
#include <limits>
#include <unistd.h>
#include <zlib.h>
#else
#define KONAN_HAS_ZLIB 0
#endif

#include "Porting.h"
#include "TypeInfo.h"
#include "KString.h"
#include "ObjectTraversal.hpp"
#include "Utils.hpp"
#include "mm/GlobalData.hpp"
#include "mm/RootSet.hpp"
#include "mm/ThreadData.hpp"
#include "std_support/Span.hpp"

constexpr auto kTagMemDump = kotlin::logging::Tag::kMemoryDump;

namespace kotlin::mm {

namespace {

class DumpWriter : private Pinned {
public:
    virtual ~DumpWriter() = default;
    virtual void write(std_support::span<uint8_t> data) = 0;
    virtual void finish() = 0;
};

class PlainDumpWriter : public DumpWriter {
public:
    explicit PlainDumpWriter(int fd) {
        file_ = fdopen(fd, "w");
        if (file_ == nullptr) {
            throw std::system_error(errno, std::generic_category());
        }
    }

    void write(std_support::span<uint8_t> data) override {
        size_t written = fwrite(data.data(), 1, data.size(), file_);
        if (written != data.size()) {
            throw std::system_error(errno, std::generic_category());
        }
    }

    // fflush only: fclose would close the caller's fd.
    void finish() override {
        if (fflush(file_) == EOF) {
            throw std::system_error(errno, std::generic_category());
        }
    }

private:
    FILE* file_ = nullptr;
};

#if KONAN_HAS_ZLIB
class GzipDumpWriter : public DumpWriter {
public:
    explicit GzipDumpWriter(int fd) {
        // gzdopen owns the fd it is given; dup so gzclose cannot close the caller's descriptor.
        int dupFd = dup(fd);
        if (dupFd < 0) {
            throw std::system_error(errno, std::generic_category());
        }
        // "wb1": gzip container at Z_BEST_SPEED. The dump runs during STW;
        // finishing quickly keeps that pause shorter.
        gz_ = gzdopen(dupFd, "wb1");
        if (gz_ == nullptr) {
            close(dupFd);
            throw std::system_error(EIO, std::generic_category());
        }
    }

    ~GzipDumpWriter() override {
        if (gz_ != nullptr) {
            gzclose(gz_);
        }
    }

    void write(std_support::span<uint8_t> data) override {
        auto* bytes = data.data();
        size_t size = data.size();
        // gzwrite takes `unsigned`; split large spans so we never truncate the length.
        while (size > 0) {
            unsigned chunk = static_cast<unsigned>(std::min(size, static_cast<size_t>(std::numeric_limits<int>::max())));
            int n = gzwrite(gz_, bytes, chunk);
            if (n <= 0) {
                int err = Z_ERRNO;
                gzerror(gz_, &err);
                throw std::system_error(err == Z_ERRNO ? errno : EIO, std::generic_category());
            }
            bytes += n;
            size -= static_cast<size_t>(n);
        }
    }

    void finish() override {
        int rc = gzclose(gz_);
        gz_ = nullptr;
        if (rc != Z_OK) {
            throw std::system_error(EIO, std::generic_category());
        }
    }

private:
    gzFile gz_ = nullptr;
};
#endif

} // namespace

class MemoryDumper {
public:
    explicit MemoryDumper(DumpWriter& writer, bool omitPrimitiveArrayPayloads)
        : writer_(writer), omitPrimitiveArrayPayloads_(omitPrimitiveArrayPayloads) {}

    // Dumps the memory.
    void Dump() {
        RuntimeLogInfo({kTagMemDump}, "Starting to dump memory omitPrimitiveArrayPayloads=%d", omitPrimitiveArrayPayloads_ ? 1 : 0);

        DumpStr("Kotlin/Native dump 1.0.8");
        DumpBool(konan::isLittleEndian());
        DumpU8(sizeof(void*));

        RuntimeLogInfo({kTagMemDump}, "Dumping global roots");
        for (auto value : mm::GlobalRootSet()) {
            DumpTransitively(value);
        }

        RuntimeLogInfo({kTagMemDump}, "Dumping threads and thread roots");
        for (auto& thread : mm::GlobalData::Instance().threadRegistry().LockForIter()) {
            DumpThread(thread);
            for (auto value : mm::ThreadRootSet(thread)) {
                DumpTransitively(thread, value);
            }
        }

        RuntimeLogInfo({kTagMemDump}, "Dumping objects from the heap");
        GlobalData::Instance().allocator().TraverseAllocatedObjects([&](auto obj) { DumpTransitively(obj); });

        RuntimeLogInfo({kTagMemDump}, "Dumping extra objects from the heap");
        GlobalData::Instance().allocator().TraverseAllocatedExtraObjects([&](auto extraObj) { DumpTransitively(extraObj); });

        RuntimeLogInfo({kTagMemDump}, "Dumping enqueued objects");
        DumpEnqueuedObjects();

        RuntimeLogInfo({kTagMemDump}, "Dumping finished");
    }

private:
    template <typename T>
    void DumpSpan(std_support::span<T> span) {
        auto* bytes = reinterpret_cast<uint8_t*>(const_cast<std::remove_cv_t<T>*>(span.data()));
        writer_.write(std_support::span<uint8_t>(bytes, span.size() * sizeof(T)));
    }

    template <typename T>
    void DumpValue(T value) {
        DumpSpan(std_support::span<T>(&value, 1));
    }

    void DumpId(const void* ptr) { DumpValue(ptr); }

    void DumpBool(bool b) { DumpU8(b ? 1 : 0); }

    void DumpU8(uint8_t i) { DumpValue(i); }

    void DumpU32(uint32_t i) { DumpValue(i); }

    void DumpStr(const char* str) { DumpSpan(std_support::span<const char>(str, strlen(str) + 1)); }

    void DumpString(ObjHeader* obj) {
        char* str = CreateCStringFromString(obj);
        DumpStr(str);
        DisposeCString(str);
    }

    void DumpStringOrEmptyIfNull(ObjHeader* obj) {
        if (obj) {
            DumpString(obj);
        } else {
            DumpStr("");
        }
    }

    void DumpThread(ThreadData& thread) {
        RuntimeLogDebug({kTagMemDump}, "Dumping thread %" PRIuPTR, thread.threadId());
        DumpU8(TAG_THREAD);
        DumpId(&thread);
    }

    void DumpGlobalRoot(GlobalRootSet::Value& value) {
        RuntimeLogDebug({kTagMemDump}, "Dumping global root: source=%d, object=%p", static_cast<int>(value.source), value.object);
        DumpU8(TAG_GLOBAL_ROOT);
        DumpU8(UInt8(value.source));
        DumpId(value.object);
    }

    void DumpThreadRoot(ThreadData& thread, ThreadRootSet::Value& value) {
        RuntimeLogDebug({kTagMemDump}, "Dumping thread root: source=%d, object=%p", static_cast<int>(value.source), value.object);
        DumpU8(TAG_THREAD_ROOT);
        DumpId(&thread);
        DumpU8(UInt8(value.source));
        DumpId(value.object);
    }

    void DumpObject(const TypeInfo* type, ObjHeader* obj) {
        RuntimeLogDebug({kTagMemDump}, "Dumping object %p of type %s", obj, type->fqName().c_str());
        DumpU8(TAG_OBJECT);
        DumpId(obj);
        DumpId(type);

        size_t size = type->instanceSize_;
        size_t dataOffset = sizeof(TypeInfo*);
        size_t dataSize = size - dataOffset;
        uint8_t* data = reinterpret_cast<uint8_t*>(obj) + dataOffset;

        DumpU32(dataSize);
        DumpSpan(std_support::span<uint8_t>(data, dataSize));
    }

    void DumpArray(const TypeInfo* type, ArrayHeader* arr) {
        RuntimeLogDebug({kTagMemDump}, "Dumping array %p", arr);
        DumpU8(TAG_ARRAY);
        DumpId(arr);
        DumpId(type);

        uint32_t count = arr->count_;
        DumpU32(count);

        int32_t elementSize = -type->instanceSize_;

        // Keep object and native-ptr arrays: they are the heap graph. Primitive
        // arrays (including String contents) are the bulky/sensitive payloads.
        if (omitPrimitiveArrayPayloads_ && type != theArrayTypeInfo && type != theNativePtrArrayTypeInfo) {
            DumpU32(0);
            return;
        }

        size_t dataOffset = alignUp(sizeof(ArrayHeader), elementSize);
        size_t dataSize = elementSize * count;
        DumpU32(dataSize);

        uint8_t* data = reinterpret_cast<uint8_t*>(arr) + dataOffset;
        DumpSpan(std_support::span<uint8_t>(data, dataSize));
    }

    void DumpObjectOrArray(ObjHeader* obj) {
        const TypeInfo* type = obj->type_info();
        if (type->IsArray()) {
            DumpArray(type, obj->array());
        } else {
            DumpObject(type, obj);
        }
    }

    void DumpType(const TypeInfo* type) {
        RuntimeLogDebug({kTagMemDump}, "Dumping type %s", type->fqName().c_str());
        DumpU8(TAG_TYPE);
        DumpId(type);

        bool isArray = type->IsArray();
        bool isExtended = type->extendedInfo_ != nullptr;
        bool isObjectArray = type == theArrayTypeInfo;
        uint8_t flags =
                (isArray ? TYPE_FLAG_ARRAY : 0) | (isExtended ? TYPE_FLAG_EXTENDED : 0) | (isObjectArray ? TYPE_FLAG_OBJECT_ARRAY : 0);
        DumpU8(flags);

        DumpId(type->superType_);

        DumpStringOrEmptyIfNull(type->packageName_);
        DumpStringOrEmptyIfNull(type->relativeName_);

        if (type->IsArray()) {
            DumpArrayInfo(type);
        } else {
            DumpObjectInfo(type);
        }
    }

    void DumpArrayInfo(const TypeInfo* type) {
        int32_t elementSize = -type->instanceSize_;
        DumpU32(elementSize);

        if (type->extendedInfo_ != nullptr) {
            DumpArrayInfo(type->extendedInfo_);
        }
    }

    void DumpArrayInfo(const ExtendedTypeInfo* extendedInfo) {
        uint8_t elementType = -extendedInfo->fieldsCount_;
        DumpU8(elementType);
    }

    void DumpObjectInfo(const TypeInfo* type) {
        size_t dataOffset = sizeof(TypeInfo*);

        DumpU32(type->instanceSize_ - dataOffset);
        DumpOffsets(type, dataOffset);

        if (type->extendedInfo_ != nullptr) {
            DumpObjectInfo(type->extendedInfo_, dataOffset);
        }
    }

    void DumpOffsets(const TypeInfo* type, size_t dataOffset) {
        int32_t count = type->objOffsetsCount_;
        DumpU32(count);
        for (int32_t i = 0; i < count; i++) {
            DumpU32(type->objOffsets_[i] - dataOffset);
        }
    }

    void DumpObjectInfo(const ExtendedTypeInfo* extendedInfo, size_t dataOffset) {
        int32_t fieldsCount = extendedInfo->fieldsCount_;
        DumpU32(fieldsCount);
        for (int32_t i = 0; i < fieldsCount; i++) {
            DumpU32(extendedInfo->fieldOffsets_[i] - dataOffset);
            DumpU8(extendedInfo->fieldTypes_[i]);
            DumpStr(extendedInfo->fieldNames_[i]);
        }
    }

    void DumpTransitively(const TypeInfo* type) {
        if (dumpedTypes_.insert(type).second) {
            // Dump super-type recursively, as the depth is not going to be a problem.
            if (type->superType_ != nullptr) {
                DumpTransitively(type->superType_);
            }

            DumpType(type);
        }
    }

    void DumpTransitively(ObjHeader* obj) {
        if (dumpedObjs_.insert(obj).second) {
            DumpTransitively(obj->type_info());

            DumpObjectOrArray(obj);

            // Enqueue referred objects to dump, as dumping them recursively may cause
            // stack overflow.
            traverseReferredObjects(obj, [&](auto refObj) { Enqueue(refObj); });
        }
    }

    void DumpTransitively(ExtraObjectData* extraObj) {
        RuntimeLogDebug({kTagMemDump}, "Dumping extra object %p", extraObj);
        DumpU8(TAG_EXTRA_OBJECT);
        DumpId(extraObj);

        ObjHeader* baseObj = extraObj->GetBaseObject();
        DumpId(baseObj);

        if (!isNullOrMarker(baseObj)) {
            Enqueue(baseObj);
        }

        void* associatedObject =
#ifdef KONAN_OBJC_INTEROP
                extraObj->AssociatedObject();
#else
                nullptr;
#endif
        DumpId(associatedObject);
    }

    void DumpTransitively(GlobalRootSet::Value& value) {
        ObjHeader* obj = value.object;
        if (isNullOrMarker(obj)) {
            return;
        }

        DumpGlobalRoot(value);

        Enqueue(obj);
    }

    void DumpTransitively(ThreadData& thread, ThreadRootSet::Value& value) {
        ObjHeader* obj = value.object;
        if (isNullOrMarker(obj)) {
            return;
        }

        DumpThreadRoot(thread, value);

        Enqueue(obj);
    }

    void Enqueue(ObjHeader* obj) { objQueue_.push(obj); }

    void DumpEnqueuedObjects() {
        while (!objQueue_.empty()) {
            auto obj = objQueue_.front();
            objQueue_.pop();
            DumpTransitively(obj);
        }
    }

    uint8_t UInt8(GlobalRootSet::Source source) {
        switch (source) {
            case GlobalRootSet::Source::kGlobal:
                return 1;
            case GlobalRootSet::Source::kStableRef:
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

    const uint8_t TAG_TYPE = 0x01;
    const uint8_t TAG_OBJECT = 0x02;
    const uint8_t TAG_ARRAY = 0x03;
    const uint8_t TAG_EXTRA_OBJECT = 0x04;
    const uint8_t TAG_THREAD = 0x05;
    const uint8_t TAG_GLOBAL_ROOT = 0x06;
    const uint8_t TAG_THREAD_ROOT = 0x07;

    const uint8_t TYPE_FLAG_ARRAY = 1 << 0;
    const uint8_t TYPE_FLAG_EXTENDED = 1 << 1;
    const uint8_t TYPE_FLAG_OBJECT_ARRAY = 1 << 2;

    DumpWriter& writer_;
    bool omitPrimitiveArrayPayloads_;

    // A set of already dumped type pointers.
    std::unordered_set<const TypeInfo*> dumpedTypes_;

    // A set of already dumped objects.
    std::unordered_set<ObjHeader*> dumpedObjs_;

    // A queue of objects to dump transitively.
    std::queue<ObjHeader*> objQueue_;
};

void PrepareForMemoryDump() {
    mm::GlobalData::Instance().threadRegistry().PublishAll();
}

void DumpMemoryOrThrow(int fd, bool omitPrimitiveArrayPayloads, bool gzip) {
#if KONAN_HAS_ZLIB
    if (gzip) {
        GzipDumpWriter writer(fd);
        MemoryDumper(writer, omitPrimitiveArrayPayloads).Dump();
        writer.finish();
        return;
    }
#else
    // Still produce a dump when gzip was requested: omitPrimitiveArrayPayloads remains usable.
    if (gzip) {
        RuntimeLogInfo({kTagMemDump}, "gzip is not available on this target; writing an uncompressed dump");
    }
#endif

    PlainDumpWriter writer(fd);
    MemoryDumper(writer, omitPrimitiveArrayPayloads).Dump();
    writer.finish();
}

bool DumpMemory(int fd, bool omitPrimitiveArrayPayloads, bool gzip) noexcept {
    PrepareForMemoryDump();

    bool success = true;
    try {
        DumpMemoryOrThrow(fd, omitPrimitiveArrayPayloads, gzip);
    } catch (const std::system_error& e) {
        success = false;
        RuntimeLogError({kTagMemDump}, "Memory dump error: %s", e.what());
    }

    return success;
}

} // namespace kotlin::mm
