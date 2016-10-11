#ifndef RUNTIME_MEMORY_H
#define RUNTIME_MEMORY_H

#include <cassert>

#include "TypeInfo.h"

typedef enum {
  FRAME_SCOPE = 0,
  GLOBAL_SCOPE = 1,
  ARENA_SCOPE = 2
} PlacementHint;

// Could be made 64-bit for large memory configs.
typedef uint32_t container_offset_t;

// Header of every object.
struct ObjHeader {
  const TypeInfo* type_info_;
  container_offset_t container_offset_negative_;
};

// Header of value type array objects.
struct ArrayHeader : public ObjHeader {
  uint32_t count_;
};

// Header of all container objects. Contains reference counter.
struct ContainerHeader {
  // Reference counter of container. Maybe use some upper bit of counter for
  // container type (for polymorphism in ::Release()).
  uint32_t ref_count_;
};

struct ArenaContainerHeader : public ContainerHeader {
  // Current allocation limit.
  uint8_t* current_;
  // Allocation end. Maybe consider having chunked backing storage
  // at cost of smarter ::Release() polymorphic on container type.
  uint8_t* end_;
};

// Thos two operations are implemented by translator when storing references
// to objects.
inline void AddRef(ContainerHeader* header) {
  // Looking at container type we may want to skip AddRef() totally
  // (non-escaping stack objects).
  header->ref_count_++;
}

void FreeObject(ContainerHeader* header);

inline void Release(ContainerHeader* header) {
  // Looking at container type we may want to skip Release() totally
  // (non-escaping stack objects, permanent objects).
  if (--header->ref_count_ == 0) {
    FreeObject(header);
  }
}

// Class representing arbitrary placement container.
class Container {
 protected:
  // Data where everything is being stored.
  ContainerHeader* header_;

  void SetMeta(ObjHeader* obj, const TypeInfo* type_info) {
    obj->container_offset_negative_ =
        reinterpret_cast<uintptr_t>(obj) - reinterpret_cast<uintptr_t>(header_);
    obj->type_info_ = type_info;
  }

 public:
  // Increment reference counter associated with container.
  void AddRef() {
    if (header_) ::AddRef(header_);
  }

  // Decrement reference counter associated with container.
  // For objects whith tricky lifetime (such as ones shared between threads objects)
  // individual container per object (ObjectContainer) shall be created.
  // As an alternative, such objects could be evacuated from short-lived containers.
  void Release() {
    if (header_) ::Release(header_);
  }
};

// Container for a single object.
class ObjectContainer : public Container {
 public:
  explicit ObjectContainer(const TypeInfo* type_info) {
    Init(type_info, 1);
  }

  ObjectContainer(const TypeInfo* type_info, uint32_t elements) {
    Init(type_info, elements);
  }

  // Object container shalln't have any dtor, as it's being freed by ::Release().
  ObjHeader* GetPlace() const {
    return reinterpret_cast<ObjHeader*>(
        reinterpret_cast<uint8_t*>(header_) + sizeof(ContainerHeader));
  }

 private:
  void Init(const TypeInfo* type_info, uint32_t elements);
};

// Class representing arena-style placement container.
// Container is used for reference counting,
// and it is assumed that objects with related placement will share container. Only
// whole container can be freed, individual objects are not taken into account.
class ArenaContainer : public Container {
 public:
  explicit ArenaContainer(uint32_t size);

  ~ArenaContainer() {
    if (header_) {
      assert(header_->ref_count_ == 0);
      Dispose();
    }
  }

  // Allocation function.
  void* Place(int size) {
    ArenaContainerHeader* header = reinterpret_cast<ArenaContainerHeader*>(header_);
    if (header->current_ + size > header->end_) {
      return nullptr;
    }
    void* result = header->current_;
    header->current_ += size;
    return result;
  }

  // Place individual object in this container.
  ObjHeader* PlaceObject(const TypeInfo* type_info);

  // Places an array of certain type in this container. Note that array_type_info
  // is type info for an array, not for an individual element. Also note that exactly
  // same operation could be used to place strings.
  ArrayHeader* PlaceArray(const TypeInfo* array_type_info, int count);

  // Dispose whole container ignoring non-zero refcount. Use with care.
  void Dispose() {
    if (header_) {
      FreeObject(header_);
      header_ = nullptr;
    }
  }
};

// Raw reference to data, meaning T*, invented only for cleaness of intentions.
template <class T>
class RawRef {
 private:
  T* ptr_;
 public:
  RawRef(T* ptr) : ptr_(ptr) {}
  const T& get() const { return *ptr_; }
  void set(const T& value) { *ptr_ = value; }
};

// Object reference, adds reference counting in container and type information.
class AnyObjRef {
 protected:
  ObjHeader* ptr_;

  explicit AnyObjRef(ObjHeader* ptr) : ptr_(ptr) {
    if (ptr_) {
      AddRef(container_header());
    }
  }

 public:
  ~AnyObjRef() {
    if (ptr_) {
      Release(container_header());
    }
  }

  ContainerHeader* container_header() const {
    return reinterpret_cast<ContainerHeader*>(
        reinterpret_cast<uint8_t*>(ptr_) - ptr_->container_offset_negative_);
  }

  const TypeInfo* type_info() const {
    return ptr_->type_info_;
  }

  // Accesses raw data inside object specified by offset. Typing by M is optional and
  // will be replaced by translator typing.
  template<typename M, int offset>
  RawRef<M> at() const {
    return RawRef<M>(
      reinterpret_cast<M*>(reinterpret_cast<uint8_t*>(any_ref()) + offset));
  }

  // Assign reference to certain object. Releases currently held object in its container and
  // adds reference to container storing given object.
  void Assign(const AnyObjRef& other) {
    // TODO: optimize for an important case where containers match?
    if (ptr_) {
      Release(container_header());
    }
    ptr_ = other.ptr_;
    if (ptr_) {
      AddRef(container_header());
    }
  }

  // Returns pointer to the raw data referred by this reference.
  uint8_t* any_ref() const {
    if (!ptr_) return nullptr;
    return reinterpret_cast<uint8_t*>(ptr_) + sizeof(ObjHeader);
  }

  // Uses pointer stored in object's field to create a reference to that object.
  AnyObjRef any_obj_at(int offset) const {
    assert(ptr_);
    return AnyObjRef(*reinterpret_cast<ObjHeader**>(any_ref() + offset));
  }

  // Checks if given reference has null value.
  bool null() const { return ptr_ == nullptr; }
};

// Returns typeinfo for array of type T. Specialize for types which are allowed as array elements.
template <typename T>
const TypeInfo* GetArrayTypeInfo() {
  return nullptr;
}

// Reference to an object with particular memory layout specified by T.
// In real runtime will be compile time only type, on runtime all references are
// AnyObjRef.
template <class T>
class ObjRef : public AnyObjRef {
 private:
  explicit ObjRef(ObjHeader* ptr) : AnyObjRef(ptr) {}

  // Reference to raw data in owned class.
  T* ref() const {
    if (!ptr_) return nullptr;
    return reinterpret_cast<T*>(any_ref());
  }

  template <typename T1> friend class ArrayRef;

 public:
  // Assigns reference, compile time type-safe.
  ObjRef(const ObjRef& other) : AnyObjRef(nullptr) {
    Assign(other);
  }
  void Assign(const ObjRef<T>& other) {
    AnyObjRef::Assign(other);
  }

  // Copies data bits to another place, reference counting is properly accounted for
  // by consulting type information.
  void CopyTo(ObjRef<T> other) const;

  // Clones object to given container.
  ObjRef<T> Clone(ArenaContainer* container) {
    ObjRef<T> result = Alloc(container);
    CopyTo(result);
    return result;
  }

  // Takes typed object reference at offset.
  template<typename M, int offset>
  ObjRef<M> obj_at() const {
    return ObjRef<M>(reinterpret_cast<M*>(any_ref() + offset));
  }

  // Allocates properly typed object in container.
  static ObjRef<T> Alloc(ArenaContainer* container) {
    return ObjRef<T>(container->PlaceObject(T::GetTypeInfo()));
  }
};

// This is an array of value types only, no object references here.
template <class T>
class ArrayRef : public AnyObjRef {
 protected:
  explicit ArrayRef(ArrayHeader* ptr) : AnyObjRef(ptr) {}
  ArrayHeader* header() { return reinterpret_cast<ArrayHeader*>(ptr_); }

 public:
   static ArrayRef<T> Alloc(ArenaContainer* container, int count) {
     auto result = ArrayRef<T>(container->PlaceArray(GetArrayTypeInfo<T>(), count));
     result.header()->count_ = count;
     return result;
  }

  RawRef<T> element_at(int index) const {
    assert(header() && index >= 0 && index < header()->count_);
    return reinterpret_cast<T*>(any_ref() + index * sizeof(T));
  }
};

#ifdef __cplusplus
extern "C" {
#endif

void InitMemory();
void* AllocInstance(const TypeInfo* type_info, PlacementHint hint);
void* AllocArrayInstance(const TypeInfo* type_info, PlacementHint hint, uint32_t elements);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_MEMORY_H
