/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_OBJECT_FACTORY_H
#define RUNTIME_MM_OBJECT_FACTORY_H

#include <algorithm>
#include <cinttypes>
#include <memory>
#include <mutex>
#include <type_traits>

#include "Alignment.hpp"
#include "FinalizerHooks.hpp"
#include "Memory.h"
#include "Mutex.hpp"
#include "Porting.h"
#include "TypeLayout.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

namespace internal {

// A queue that is constructed by collecting subqueues from several `Producer`s.
// This is essentially a heterogeneous `MultiSourceQueue` on top of a singly linked list that
// uses `Allocator` to allocate and free memory.
// Precondition on `Allocator`: must allocate with alignment at least `DataAlignment`.
// TODO: Consider merging with `MultiSourceQueue` somehow.
template <size_t DataAlignment, typename Allocator, typename DataSizeProvider>
class ObjectFactoryStorage : private Pinned {
    static_assert(IsValidAlignment(DataAlignment), "DataAlignment is not a valid alignment");

    template <typename T>
    class Deleter {
    public:
        void operator()(T* instance) noexcept {
            auto size = instance->GetAllocatedSize();
            instance->~T();
            Allocator::Free(instance, size);
        }
    };

    template <typename T>
    using unique_ptr = std::unique_ptr<T, Deleter<T>>;

public:
    // This class does not know its size at compile-time.
    class Node : private Pinned {
        constexpr static size_t DataOffset() noexcept { return AlignUp(sizeof(Node), DataAlignment); }

    public:
        ~Node() = default;

        constexpr static uint64_t GetSizeForDataSize(uint64_t dataSize) noexcept {
            uint64_t dataSizeAligned = AlignUp<uint64_t>(dataSize, DataAlignment);
            uint64_t totalSize = AlignUp<uint64_t>(sizeof(Node) + dataSizeAligned, DataAlignment);
            return totalSize;
        }

        static Node& FromData(void* data) noexcept {
            constexpr size_t kDataOffset = DataOffset();
            Node* node = reinterpret_cast<Node*>(reinterpret_cast<uintptr_t>(data) - kDataOffset);
            RuntimeAssert(node->Data() == data, "Node layout has broken");
            return *node;
        }

        // Note: This can only be trivially destructible data, as nobody can invoke its destructor.
        uint8_t* Data() noexcept {
            constexpr size_t kDataOffset = DataOffset();
            auto* ptr = reinterpret_cast<uint8_t*>(this) + kDataOffset;
            RuntimeAssert(IsAligned(ptr, DataAlignment), "Data=%p is not aligned to %zu", ptr, DataAlignment);
            return ptr;
        }

        // It's a caller responsibility to know if the underlying data is `T`.
        template <typename T>
        T& Data() noexcept {
            return *reinterpret_cast<T*>(Data());
        }

        size_t GetAllocatedSize() noexcept {
            auto dataSize = DataSizeProvider::GetDataSize(Data());
            auto totalSize = GetSizeForDataSize(dataSize);
            return totalSize;
        }

    private:
        friend class ObjectFactoryStorage;

        Node() noexcept = default;

        static unique_ptr<Node> Create(Allocator& allocator, uint64_t dataSize) noexcept {
            auto totalSize = GetSizeForDataSize(dataSize);
            void* ptr = nullptr;
            if (totalSize <= std::numeric_limits<size_t>::max()) {
                RuntimeAssert(
                        DataOffset() + dataSize <= totalSize, "totalSize %" PRIu64 " is not enough to fit data %" PRIu64 " at offset %zu", totalSize, dataSize,
                        DataOffset());
                ptr = allocator.Alloc(totalSize);
            }
            if (!ptr) {
                // TODO: This should throw OutOfMemoryError in the future if we add hard memory limits instead
                //       of limiting at virtual address space boundary.
                konan::consoleErrorf("Out of memory trying to allocate %" PRIu64 " bytes. Aborting.\n", totalSize);
                konan::abort();
            }
            RuntimeAssert(IsAligned(ptr, DataAlignment), "Allocator returned unaligned to %zu pointer %p", DataAlignment, ptr);
            return unique_ptr<Node>(new (ptr) Node());
        }

        unique_ptr<Node> next_;
        // There's some more data of an unknown (at compile-time) size here, but it cannot be represented
        // with C++ members.
    };
    static_assert(alignof(Node) <= DataAlignment, "DataAlignment must be greater than Node alignment");

    class Producer : private MoveOnly {
    public:
        class Iterator {
        public:
            Node& operator*() noexcept { return *node_; }
            Node* operator->() noexcept { return node_; }

            Iterator& operator++() noexcept {
                node_ = node_->next_.get();
                return *this;
            }

            bool operator==(const Iterator& rhs) const noexcept { return node_ == rhs.node_; }
            bool operator!=(const Iterator& rhs) const noexcept { return node_ != rhs.node_; }

        private:
            friend class Producer;
            explicit Iterator(Node* node) noexcept : node_(node) {}

            Node* node_;
        };

        Producer(ObjectFactoryStorage& owner, Allocator allocator) noexcept : owner_(owner), allocator_(std::move(allocator)) {}

        ~Producer() { Publish(); }

        size_t size() const noexcept { return size_; }

        Node& Insert(uint64_t dataSize) noexcept {
            AssertCorrect();
            auto node = Node::Create(allocator_, dataSize);
            auto* nodePtr = node.get();
            if (!root_) {
                root_ = std::move(node);
            } else {
                last_->next_ = std::move(node);
            }

            last_ = nodePtr;
            ++size_;
            totalObjectsSizeBytes_ += Node::GetSizeForDataSize(dataSize);
            RuntimeAssert(root_ != nullptr, "Must not be empty");
            AssertCorrect();
            return *nodePtr;
        }

        template <typename T, typename... Args>
        Node& Insert(Args&&... args) noexcept {
            static_assert(alignof(T) <= DataAlignment, "Cannot insert type with alignment bigger than DataAlignment");
            static_assert(std::is_trivially_destructible_v<T>, "Type must be trivially destructible");
            auto& node = Insert(sizeof(T));
            new (node.Data()) T(std::forward<Args>(args)...);
            return node;
        }

        // Merge `this` queue with owning `ObjectFactoryStorage`.
        // `this` will have empty queue after the call.
        // This call is performed without heap allocations. TODO: Test that no allocations are happening.
        void Publish() noexcept {
            AssertCorrect();
            if (!root_) {
                return;
            }

            std::lock_guard guard(owner_.mutex_);

            owner_.AssertCorrectUnsafe();

            if (!owner_.root_) {
                owner_.root_ = std::move(root_);
            } else {
                owner_.last_->next_ = std::move(root_);
            }

            owner_.last_ = last_;
            last_ = nullptr;
            owner_.size_ += size_;
            owner_.totalObjectsSizeBytes_ += totalObjectsSizeBytes_;
            size_ = 0;
            totalObjectsSizeBytes_ = 0;

            RuntimeAssert(root_ == nullptr, "Must be empty");
            AssertCorrect();
            RuntimeAssert(owner_.root_ != nullptr, "Must not be empty");
            owner_.AssertCorrectUnsafe();
        }

        Iterator begin() noexcept { return Iterator(root_.get()); }
        Iterator end() noexcept { return Iterator(nullptr); }

        void ClearForTests() noexcept {
            // Since it's only for tests, no need to worry about stack overflows.
            root_.reset();
            last_ = nullptr;
            size_ = 0;
        }

    private:
        friend class ObjectFactoryStorage;

        ALWAYS_INLINE void AssertCorrect() const noexcept {
            if (root_ == nullptr) {
                RuntimeAssert(last_ == nullptr, "last_ must be null");
            } else {
                RuntimeAssert(last_ != nullptr, "last_ must not be null");
                RuntimeAssert(last_->next_ == nullptr, "last_ must not have next");
            }
        }

        ObjectFactoryStorage& owner_; // weak
        Allocator allocator_;
        unique_ptr<Node> root_;
        Node* last_ = nullptr;
        size_t size_ = 0;
        size_t totalObjectsSizeBytes_ = 0;
    };

    class Iterator {
    public:
        Node& operator*() noexcept { return *node_; }
        Node* operator->() noexcept { return node_; }

        Iterator& operator++() noexcept {
            previousNode_ = node_;
            node_ = node_->next_.get();
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return node_ == rhs.node_; }

        bool operator!=(const Iterator& rhs) const noexcept { return node_ != rhs.node_; }

    private:
        friend class ObjectFactoryStorage;

        Iterator(Node* previousNode, Node* node) noexcept : previousNode_(previousNode), node_(node) {}

        Node* previousNode_; // Kept for `Iterable::EraseAndAdvance`.
        Node* node_;
    };

    class Consumer : private MoveOnly {
    public:
        class Iterator {
        public:
            Node& operator*() noexcept { return *node_; }
            Node* operator->() noexcept { return node_; }

            Iterator& operator++() noexcept {
                node_ = node_->next_.get();
                return *this;
            }

            bool operator==(const Iterator& rhs) const noexcept { return node_ == rhs.node_; }
            bool operator!=(const Iterator& rhs) const noexcept { return node_ != rhs.node_; }

        private:
            friend class Consumer;
            explicit Iterator(Node* node) noexcept : node_(node) {}

            Node* node_;
        };

        Consumer() noexcept = default;

        Consumer(Consumer&& other) noexcept {
            root_ = std::move(other.root_);
            size_ = other.size_;
            last_ = other.last_;
            other.size_ = 0;
            other.last_ = nullptr;
        }
        Consumer& operator=(Consumer&& other) noexcept {
            Consumer temp = std::move(other);
            std::swap(root_, temp.root_);
            std::swap(size_, temp.size_);
            std::swap(last_, temp.last_);
            return *this;
        }

        ~Consumer() {
            // Make sure not to blow up the stack by nested `~Node` calls.
            for (auto node = std::move(root_); node != nullptr; node = std::move(node->next_)) {
            }
        }

        size_t size() const noexcept { return size_; }

        Iterator begin() noexcept { return Iterator(root_.get()); }
        Iterator end() noexcept { return Iterator(nullptr); }

        void MergeWith(Consumer &&other) {
            AssertCorrect();
            if (other.root_) {
                if (!root_) {
                    root_ = std::move(other.root_);
                } else {
                    last_->next_ = std::move(other.root_);
                }
                last_ = other.last_;
                size_ += other.size_;
                other.last_ = nullptr;
                other.size_ = 0;
            }
            AssertCorrect();
        }

    private:
        friend class ObjectFactoryStorage;

        void Insert(unique_ptr<Node> node) noexcept {
            AssertCorrect();
            auto* nodePtr = node.get();
            if (!root_) {
                root_ = std::move(node);
            } else {
                last_->next_ = std::move(node);
            }

            last_ = nodePtr;
            ++size_;
            AssertCorrect();
        }

        ALWAYS_INLINE void AssertCorrect() const noexcept {
            if (root_ == nullptr) {
                RuntimeAssert(last_ == nullptr, "last_ must be null");
            } else {
                RuntimeAssert(last_ != nullptr, "last_ must not be null");
                RuntimeAssert(last_->next_ == nullptr, "last_ must not have next");
            }
        }

        unique_ptr<Node> root_;
        Node* last_ = nullptr;
        size_t size_ = 0;
    };

    class Iterable : private MoveOnly {
    public:
        explicit Iterable(ObjectFactoryStorage& owner) noexcept : owner_(owner), guard_(owner_.mutex_) {}

        size_t size() const noexcept { return owner_.size_; }

        Iterator begin() noexcept { return Iterator(nullptr, owner_.root_.get()); }
        Iterator end() noexcept { return Iterator(owner_.last_, nullptr); }

        void EraseAndAdvance(Iterator& iterator, size_t objectedSize) noexcept {
            auto result = owner_.ExtractUnsafe(iterator.previousNode_, objectedSize);
            iterator.node_ = result.second;
        }

        void MoveAndAdvance(Consumer& consumer, Iterator& iterator, size_t objectSize) noexcept {
            auto result = owner_.ExtractUnsafe(iterator.previousNode_, objectSize);
            iterator.node_ = result.second;
            consumer.Insert(std::move(result.first));
        }

    private:
        ObjectFactoryStorage& owner_; // weak
        std::unique_lock<SpinLock<MutexThreadStateHandling::kIgnore>> guard_;
    };

    ~ObjectFactoryStorage() {
        // Make sure not to blow up the stack by nested `~Node` calls.
        for (auto node = std::move(root_); node != nullptr; node = std::move(node->next_)) {}
    }

    // Lock `ObjectFactoryStorage` for safe iteration.
    Iterable LockForIter() noexcept { return Iterable(*this); }

    size_t GetSizeUnsafe() const noexcept { return size_; }

    void ClearForTests() {
        root_.reset();
        last_ = nullptr;
        size_ = 0;
        totalObjectsSizeBytes_ = 0;
    }

private:
    // Expects `mutex_` to be held by the current thread.
    std::pair<unique_ptr<Node>, Node*> ExtractUnsafe(Node* previousNode, size_t objectSize) noexcept {
        RuntimeAssert(root_ != nullptr, "Must not be empty");
        AssertCorrectUnsafe();

        unique_ptr<Node> &pointerToNext = (previousNode == nullptr) ? root_ : previousNode->next_;
        auto node = std::move(pointerToNext);
        pointerToNext = std::move(node->next_);
        if (!pointerToNext) {
            last_ = previousNode;
        }
        --size_;
        totalObjectsSizeBytes_ -= objectSize;

        AssertCorrectUnsafe();
        return {std::move(node), pointerToNext.get()};
    }

    // Expects `mutex_` to be held by the current thread.
    ALWAYS_INLINE void AssertCorrectUnsafe() const noexcept {
        if (root_ == nullptr) {
            RuntimeAssert(last_ == nullptr, "last_ must be null");
        } else {
            RuntimeAssert(last_ != nullptr, "last_ must not be null");
            RuntimeAssert(last_->next_ == nullptr, "last_ must not have next");
        }
    }

    unique_ptr<Node> root_;
    Node* last_ = nullptr;
    size_t size_ = 0;
    size_t totalObjectsSizeBytes_ = 0;
    SpinLock<MutexThreadStateHandling::kIgnore> mutex_;
};

} // namespace internal

template <typename Traits>
class ObjectFactory : private Pinned {
    using Allocator = typename Traits::Allocator;
    using ObjectData = typename Traits::ObjectData;

    struct HeapObjHeader {
        using descriptor = type_layout::Composite<HeapObjHeader, ObjectData, ObjHeader>;

        static HeapObjHeader& from(ObjectData& objectData) noexcept { return *descriptor().template fromField<0>(&objectData); }

        static HeapObjHeader& from(ObjHeader* object) noexcept { return *descriptor().template fromField<1>(object); }

        ObjectData& objectData() noexcept { return *descriptor().template field<0>(this).second; }

        ObjHeader* object() noexcept { return descriptor().template field<1>(this).second; }

    private:
        HeapObjHeader() = delete;
        ~HeapObjHeader() = delete;
    };

    struct HeapObject {
        using descriptor = type_layout::Composite<HeapObject, HeapObjHeader, ObjectBody>;

        static descriptor make_descriptor(const TypeInfo* typeInfo) noexcept {
            return descriptor{{}, type_layout::descriptor_t<ObjectBody>{typeInfo}};
        }

        HeapObjHeader& header(descriptor descriptor) noexcept { return *descriptor.template field<0>(this).second; }

    private:
        HeapObject() = delete;
        ~HeapObject() = delete;
    };

    // Needs to be kept compatible with `HeapObjHeader` just like `ArrayHeader` is compatible
    // with `ObjHeader`: the former can always be casted to the other.
    struct HeapArrayHeader {
        using descriptor = type_layout::Composite<HeapArrayHeader, ObjectData, ArrayHeader>;

        static HeapArrayHeader& from(ObjectData& objectData) noexcept { return *descriptor().template fromField<0>(&objectData); }

        static HeapArrayHeader& from(ArrayHeader* array) noexcept { return *descriptor().template fromField<1>(array); }

        ObjectData& objectData() noexcept { return *descriptor().template field<0>(this).second; }

        ArrayHeader* array() noexcept { return descriptor().template field<1>(this).second; }

    private:
        HeapArrayHeader() = delete;
        ~HeapArrayHeader() = delete;
    };

    struct HeapArray {
        using descriptor = type_layout::Composite<HeapArray, HeapArrayHeader, ArrayBody>;

        static descriptor make_descriptor(const TypeInfo* typeInfo, uint32_t size) noexcept {
            return descriptor{{}, type_layout::descriptor_t<ArrayBody>{typeInfo, size}};
        }

        HeapArrayHeader& header(descriptor descriptor) noexcept { return *descriptor.template field<0>(this).second; }

    private:
        HeapArray() = delete;
        ~HeapArray() = delete;
    };

    // Only used for already allocated objects. Cannot overflow size_t.
    static size_t GetDataSizeForAllocated(ObjHeader* object) noexcept {
        RuntimeAssert(object->heap(), "Object must be a heap object");
        const auto* typeInfo = object->type_info();
        if (typeInfo->IsArray()) {
            return HeapArray::make_descriptor(typeInfo, object->array()->count_).size();
        } else {
            return HeapObject::make_descriptor(typeInfo).size();
        }
    }

    struct DataSizeProvider {
        static size_t GetDataSize(uint8_t* data) noexcept {
            return GetDataSizeForAllocated(reinterpret_cast<HeapObjHeader*>(data)->object());
        }
    };

public:
    using Storage = internal::ObjectFactoryStorage<kObjectAlignment, Allocator, DataSizeProvider>;

    class NodeRef {
    public:
        explicit NodeRef(typename Storage::Node& node) noexcept : node_(node) {}

        static NodeRef From(ObjHeader* object) noexcept {
            RuntimeAssert(object->heap(), "Must be a heap object");
            return NodeRef(Storage::Node::FromData(reinterpret_cast<uint8_t*>(&HeapObjHeader::from(object))));
        }

        static NodeRef From(ArrayHeader* array) noexcept {
            RuntimeAssert(array->obj()->heap(), "Must be a heap object");
            return NodeRef(Storage::Node::FromData(reinterpret_cast<uint8_t*>(&HeapArrayHeader::from(array))));
        }

        static NodeRef From(ObjectData& objectData) noexcept {
            return NodeRef(Storage::Node::FromData(reinterpret_cast<uint8_t*>(&HeapObjHeader::from(objectData))));
        }

        NodeRef* operator->() noexcept { return this; }

        ObjectData& ObjectData() noexcept { return reinterpret_cast<HeapObjHeader*>(node_.Data())->objectData(); }

        ObjHeader* GetObjHeader() noexcept { return reinterpret_cast<HeapObjHeader*>(node_.Data())->object(); }

        bool operator==(const NodeRef& rhs) const noexcept { return &node_ == &rhs.node_; }

        bool operator!=(const NodeRef& rhs) const noexcept { return !(*this == rhs); }

    private:
        typename Storage::Node& node_;
    };

    class ThreadQueue : private MoveOnly {
    public:
        class Iterator {
        public:
            NodeRef operator*() noexcept { return NodeRef(*iterator_); }
            NodeRef operator->() noexcept { return NodeRef(*iterator_); }

            Iterator& operator++() noexcept {
                ++iterator_;
                return *this;
            }

            bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }
            bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

        private:
            friend class ObjectFactory;

            explicit Iterator(typename Storage::Producer::Iterator iterator) noexcept : iterator_(std::move(iterator)) {}

            typename Storage::Producer::Iterator iterator_;
        };

        ThreadQueue(ObjectFactory& owner, Allocator allocator) noexcept : producer_(owner.storage_, std::move(allocator)) {}

        ObjHeader* CreateObject(const TypeInfo* typeInfo) noexcept {
            RuntimeAssert(!typeInfo->IsArray(), "Must not be an array");
            auto descriptor = HeapObject::make_descriptor(typeInfo);
            auto& heapObject = *descriptor.construct(producer_.Insert(descriptor.size()).Data());
            ObjHeader* object = heapObject.header(descriptor).object();
            object->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
            // TODO: Consider supporting TF_IMMUTABLE: mark instance as frozen upon creation.
            return object;
        }

        ArrayHeader* CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept {
            RuntimeAssert(typeInfo->IsArray(), "Must be an array");
            auto descriptor = HeapArray::make_descriptor(typeInfo, count);
            auto& heapArray = *descriptor.construct(producer_.Insert(descriptor.size()).Data());
            ArrayHeader* array = heapArray.header(descriptor).array();
            array->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
            array->count_ = count;
            // TODO: Consider supporting TF_IMMUTABLE: mark instance as frozen upon creation.
            return array;
        }

        void Publish() noexcept { producer_.Publish(); }

        Iterator begin() noexcept { return Iterator(producer_.begin()); }
        Iterator end() noexcept { return Iterator(producer_.end()); }

        void ClearForTests() noexcept { producer_.ClearForTests(); }

    private:
        typename Storage::Producer producer_;
    };

    class Iterator {
    public:
        NodeRef operator*() noexcept { return NodeRef(*iterator_); }
        NodeRef operator->() noexcept { return NodeRef(*iterator_); }

        Iterator& operator++() noexcept {
            ++iterator_;
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }

        bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

    private:
        friend class ObjectFactory;

        explicit Iterator(typename Storage::Iterator iterator) noexcept : iterator_(std::move(iterator)) {}

        typename Storage::Iterator iterator_;
    };

    class FinalizerQueue : private MoveOnly {
    public:
        class Iterator {
        public:
            NodeRef operator*() noexcept { return NodeRef(*iterator_); }
            NodeRef operator->() noexcept { return NodeRef(*iterator_); }

            Iterator& operator++() noexcept {
                ++iterator_;
                return *this;
            }

            bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }
            bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

        private:
            friend class ObjectFactory;

            explicit Iterator(typename Storage::Consumer::Iterator iterator) noexcept : iterator_(std::move(iterator)) {}

            typename Storage::Consumer::Iterator iterator_;
        };

        class Iterable {
        public:
            Iterator begin() noexcept { return Iterator(owner_.consumer_.begin()); }
            Iterator end() noexcept { return Iterator(owner_.consumer_.end()); }

        private:
            friend class FinalizerQueue;

            explicit Iterable(FinalizerQueue& owner) : owner_(owner) {}

            FinalizerQueue& owner_;
        };

        size_t size() const noexcept { return consumer_.size(); }

        // TODO: Consider running it in the destructor instead.
        void Finalize() noexcept {
            for (auto node : Iterable(*this)) {
                RunFinalizers(node->GetObjHeader());
            }
        }

        void MergeWith(FinalizerQueue &&other) {
            consumer_.MergeWith(std::move(other.consumer_));
        }

        Iterable IterForTests() noexcept { return Iterable(*this); }

    private:
        friend class ObjectFactory;

        typename Storage::Consumer consumer_;
    };

    struct FinalizerQueueTraits {
        static bool isEmpty(const FinalizerQueue& queue) noexcept { return queue.size() == 0; }

        static void add(FinalizerQueue& into, FinalizerQueue from) noexcept { into.MergeWith(std::move(from)); }

        static void process(FinalizerQueue queue) noexcept { queue.Finalize(); }
    };

    class Iterable {
    public:
        Iterable(ObjectFactory& owner) noexcept : iter_(owner.storage_.LockForIter()) {}

        Iterator begin() noexcept { return Iterator(iter_.begin()); }
        Iterator end() noexcept { return Iterator(iter_.end()); }

        void EraseAndAdvance(Iterator& iterator) noexcept {
            iter_.EraseAndAdvance(iterator.iterator_, GetAllocatedHeapSize(iterator->GetObjHeader()));
        }

        void MoveAndAdvance(FinalizerQueue& queue, Iterator& iterator) noexcept {
            iter_.MoveAndAdvance(queue.consumer_, iterator.iterator_, GetAllocatedHeapSize(iterator->GetObjHeader()));
        }

    private:
        typename Storage::Iterable iter_;
    };

    ObjectFactory() noexcept = default;
    ~ObjectFactory() = default;

    // Lock ObjectFactory for safe iteration.
    Iterable LockForIter() noexcept { return Iterable(*this); }

    void ClearForTests() { storage_.ClearForTests(); }

    static size_t GetAllocatedHeapSize(ObjHeader* object) noexcept {
        auto dataSize = GetDataSizeForAllocated(object);
        return Storage::Node::GetSizeForDataSize(dataSize);
    }

private:
    Storage storage_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_OBJECT_FACTORY_H
