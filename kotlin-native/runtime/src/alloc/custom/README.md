# Overview of the allocator

This document describes the internals of the custom allocator. The presentation
here is not fully true to the implementation, as the design is still work in
progress.

The main idea of the custom allocator is to divide system memory into chunks
(pages) that each can be swept independently, in memory consecutive order.
Every allocation ends up as a block of memory inside a page. Each page keeps
track of the size of each block it contains; how this is done depends on the
page type, with different page types optimized for different allocation sizes.
All the memory blocks are consecutive within a page, so the size of a block
also tells where the next block begins. Paired with an additional mechanism
(page type dependent) for determining whether a block is allocated, we can
iterate through the allocated blocks.

When a thread allocates memory for an object, it will find a page suitable for
the given allocation size. Each thread holds on to a number of pages for the
different size categories. The typical case is that the thread’s current page
for the given size can fit the requested allocation. If that is not the case, a
different page for that size category is requested from the shared allocation
space. The requested page can either be readily available (already prepared by
the GC thread), or it might need to be swept first, or it might be newly
created.

The GC thread has a new responsibility when using this allocator: While the
mutator threads are paused at the start of GC, the GC thread must prepare the
allocator for sweeping. This does two things. First, it marks all pages as
“needs to be swept before next use”. Second, it releases pages that threads are
holding on to, by clearing the thread local variables for each thread.

It is possible to have several independent allocation spaces at the same time.
This is currently useful for testing, but is potentially useful in other
settings.

# Detailed Design

All allocations are made through a `CustomAllocator` object.

## [CustomAllocator](cpp/CustomAllocator.hpp)

```cpp
class CustomAllocator {
public:
    CustomAllocator(Heap& heap, GCSchedulerThreadData& scheduler);
    ObjectHeader* CreateObject(TypeInfo* type);
    ArrayHeader* CreateArray(TypeInfo* type, uint32_t count);
    ExtraObjectData* CreateExtraObject();
    void PrepareForGc();

private:
    uint8_t* Allocate(uint64_t cellCount);
    uint8_t* AllocateInSingleObjectPage(uint64_t cellCount);
    uint8_t* AllocateInNextFitPage(uint32_t cellCount);
    uint8_t* AllocateInFixedBlockPage(uint32_t cellCount);

    Heap& heap_;
    GCSchedulerThreadData& gcScheduler_;
    NextFitPage* nextFitPage_;
    FixedBlockPage* fixedBlockPages_[MAX_BLOCK_SIZE];
    ExtraObjectPage* extraObjectPage_;
};
```

The primary responsibility of this class is to delegate each requested
allocation to pages of the appropriate type, based on allocation size. To do
this, it requests pages from the shared allocation space (`Heap`) and stores
pages for later allocations. Each thread thus owns a number of pages for
different allocation sizes, but at most one for each size class. When
allocating, the `CustomAllocator` will first try to allocate in one of its
owned pages. If this fails, it will request a new page for that size class from
a shared `Heap` object. `SingleObjectPages` are never kept by the
`CustomAllocator`, since they are created specifically for a single allocation,
with no extra space.

## [Heap](cpp/Heap.hpp)

```cpp
class Heap {
public:
    void PrepareForGC();

    void Sweep();

    AtomicStack<ExtraObjectCell> SweepExtraObjects(GCHandle gcHandle);

    FixedBlockPage* GetFixedBlockPage(uint32_t cellCount);
    NextFitPage* GetNextFitPage(uint32_t cellCount);
    SingleObjectPage* GetSingleObjectPage(uint64_t cellCount);
    ExtraObjectPage* GetExtraObjectPage();

private:
    PageStore<FixedBlockPage> fixedBlockPages_[MAX_BLOCK_SIZE];
    PageStore<NextFitPage> nextFitPages_;
    PageStore<SingleObjectPage> singleObjectPages_;
    AtomicStack<ExtraObjectPage> extraObjectPages_;
    AtomicStack<ExtraObjectPage> usedExtraObjectPages_;
};
```

A `Heap` object represents a shared allocation space for multiple
`CustomAllocator`s, which can request pages through one of the
`GetFixedBlockPage`, `GetNextFitPage`, `GetSingleObjectPage` methods. It also
provides a method for sweeping through all blocks that have been allocated in
this heap. The `Heap` object is the synchronization point, and guarantees that
every page is returned at most once. Page ownership is thus implicitly given to
the thread that called the method. The `Heap` object keeps track of all pages,
so there is no need to explicitly return ownership of a page.  Internally, a
`Heap` keeps the pages for each size class in a `PageStore`. This means one for
`SingleObjectPage`s, one for `NextFitPage`s, one for each of the block sizes
for `FixedBlockPage`s. `ExtraObjectPage`s are stored directly in two
`AtomicStack`s, since they require different handling during sweeping.

## [PageStore](cpp/PageStore.hpp)

```cpp
template <class PageType>
class PageStore {
public:
    void PrepareForGC();
    void Sweep();
    void SweepAndFree();
    PageType* GetPage(uint32_t cellCount);
    PageType* NewPage(uint64_t cellCount);

private:
    AtomicStack<PageType> empty_;
    AtomicStack<PageType> ready_;
    AtomicStack<PageType> used_;
    AtomicStack<PageType> unswept_;
};
```

A PageStore is responsible for keeping track of all pages of a given type and
size class. Each of the pages are in one of four stacks. The stack, that a
given page is in, determines its current state:

* `unswept_`: have not yet been swept since the last GC cycle.
* `ready_`: are ready for allocation; has been swept by the GC thread.
* `used_`: has been given to some thread for allocation; it might still be used
    for allocation, or it might have been discarded with not enough space left.
    Will not be used until the next GC cycle.
* `empty_`: same as `ready_`, but does not contain any objects. Will be freed
    before the next GC, if not needed before then.

When a page is requested, the page is taken from `ready_`, if there are any.
Otherwise, an `unswept_` page is taken and swept before returning. If there are
no unswept pages either, an empty page is taken, if there are any. Otherwise a
new page is created in the size category. All returned pages are moved to
`used_`. During the marking phase, all remaining pages in `empty_` are freed,
and all other pages are moved to `unswept_`. The GC thread will go through all
`PageStore`s and sweep the pages in `unswept_` and move them to `ready_`. If one
of the other threads sweeps a page from `unswept_`, it is moved directly to
`used_`, as it is claimed by the `CustomAllocator` that swept it.

`SingleObjectPage`s are treated slightly differently, because they are created
for one specific single allocation, and not reused when that allocation is
freed. A `SingleObjectPage` allocation goes directly to `NewPage(...)`, without
checking any of the stacks, and during sweeping, they are freed directly rather
than being put into the `empty_` stack. Ideally, there would only be two stacks
in play for `SingleObjectPages`; `used_` and `unswept_`. However, very little
is lost by just using the existing `PageStore` logic used for the other pages.

## [AtomicStack](cpp/AtomicStack.hpp)

```cpp
template <class PageType>
class AtomicStack {
public:
    PageType* Pop();
    void Push(PageType* elm);
    void TransferAllFrom(AtomicStack<T>& src);
    bool isEmpty();


private:
    std::atomic<PageType*> stack_;
};
```

The only place where atomics are used are in the stacks inside the `PageStore`.
All page classes have a non-atomic next pointer, to be used for linking up in
exactly one stack. `Pop` and `Push` are implemented with compare-and-swap
operations. The class is thread safe, except for if an element is freed while
another thread tries to Pop it from a stack.

# Page types

This section is likely to change, given the likely introduction of additional
page types. It also describes some details about which page type is chosen for
a given allocation, which is also likely to change.

There are four different page types, but they all share the feature that they
can be swept independently. The Sweep methods return whether there were any
live objects in the page after sweeping. If not, the page will be given back to
the OS.

## [FixedBlockPage](cpp/FixedBlockPage.hpp)

```cpp
class FixedBlockPage {
public:
    FixedBlockPage(uint32_t blockSize) noexcept;

    uint8_t* TryAllocate() noexcept;

    bool Sweep() noexcept;

private:
    FixedBlockPage* next_;
    FixedCellRange nextFree_;
    uint32_t blockSize_;
    uint32_t end_;
    FixedBlockCell cells_[];
};
};
```

All sufficiently small allocations (currently arbitrary <1KiB) are directed to
a `FixedBlockPage`, where all blocks have the same fixed size. Most allocations
are expected to be in this page type. A `FixedBlockPage` consists of a number
of equally sized blocks, where each allocation will take up exactly one such
block.

```cpp
struct FixedBlockCell {
    union {
        uint8_t data[];
        FixedCellRange nextFree;
    }
};

struct alignas(8) FixedCellRange {
    uint32_t first;
    uint32_t last;
};
```

Consecutive unallocated cells are represented by a `FixedCellRange`, with
`.first` and `.last` being the inclusive end points of the range of unallocated
cells. The `FixedBlockCell` at the the `.last` index contains a
`FixedCellRange` with the next range of unallocated cells. The `FixedCellRange`
of unallocated ranges thus form a linked list.

The important point is that all links in this list point forward in the page, so
all blocks between two `FixedCellRanges` are implicitly allocated. Sweeping a
`FixedBlockPage` consists of walking the free-list forward, and sweeping all
blocks in between the links, maintaining the free list when blocks are freed.

Each small page takes up the same amount of space, independent of block size,
so larger block size implies fewer blocks per page.  This size is arbitrarily
chosen to be 256 KiB, but this might change.

## [NextFitPage](cpp/NextFitPage.hpp)

```cpp
class NextFitPage {
public:
    NextFitPage(uint32_t cellCount);
    Cell* TryAllocate(uint32_t cellCount);
    bool Sweep();

private:
    NextFitPage* next_; // used by AtomicStack
    Cell* curBlock_;
    Cell cells_[];
};
```

Allocations that could theoretically fit in a `FixedBlockPage`, but would
require too large a block size (arbitrary >=1KiB), are allocated in a
`NextFitPage`. `NextFitPage`s are the same size as `FixedBlockPage`s (arbitrary 64
KiB for experiments). All blocks in a `NextFitPage` have a header that tells how
big the block is, and whether it is allocated or not. There are no gaps between
blocks, so the size of a block also tells where the next block is. The header
information fits inside a 8 byte `Cell`.

```cpp
class Cell {
public:
    Cell(uint32_t size);
    uint8_t* TryAllocate(uint32_t cellCount);

private:
    uint32_t isAllocated_;
    uint32_t size_;
    uint8_t data_[];
};
```

The page keeps a reference to a currently active block, and will try to bump
allocate inside that block. If allocation does not fit, we move to the next
block that fits. If no block in the page fits the requested size, the page is
abandoned until the next GC.

## [SingleObjectPage](cpp/SingleObjectPage.hpp)

```cpp
class SingleObjectPage {
public:
    SingleObjectPage(uint64_t cellCount);
    bool Sweep();

private:
    SingleObjectPage* next_; // used by AtomicStack
};
```

Allocations too big for a `NextFitPage` are allocated in a `SingleObjectPage`,
which only contains that single block of the requested size. They are also
handled slightly differently by both `Heap` and `CustomAllocator`. First off,
`Heap::GetSingleObjectPage` will never check existing pages, and instead just
allocate a new page. Secondly, a `CustomAllocator` does not keep a reference to
any of the `SingleObjectPage`s. As a consequence, they are only swept by the GC
thread.

## [ExtraObjectPage](cpp/ExtraObjectPage.hpp)

```cpp
class ExtraObjectPage {
public:
    ExtraObjectPage();
    ExtraObjectData* TryAllocate();
    bool Sweep(FinalizerQueue& queue);


private:
    ExtraObjectPage* next_; // used by AtomicStack
    ExtraObjectCell* nextFree_;
    ExtraObjectCell cells_[];
};
```

Extra objects are used for attaching additional data to some objects. This is
used for objects that require special handling during garbage collection:

* objects with finalizers
* weak references
* interop references

Extra objects are allocated in `ExtraObjectPage`s, which are very similar to
`FixedBlockPage`s. They primarily differ in how they are swept, since it is
during sweeping of `ExtraObject`s that scheduling of finalization happens. If
an object that requires finalization is found, it is added to the
`FinalizerQueue` given as argument. The cells are also slightly different, in
that they add a new field that allows the cells to be added to the finalizer
queue.

```cpp
struct ExtraObjectCell {
    ExtraObjectCell* next_; // used by AtomicStack
    ExtraObjectData data_;
};
```

# Finalizers

Section like to change.

In the existing memory model, finalization tasks are found and scheduled during
sweeping of regular objects. The objects to be finalized are chained together
using a pointer in the Node header, added to all allocated objects. This header
is not needed in the custom allocator, apart from linking in the finalization
queue.

We therefore reintroduce this pointer in a header for `ExtraObjectData`. For
this, we reuse the `ExtraObjectCell` as header for both free list pointer and as
linking pointer for the `AtomicStack` that we use as the `FinalizerQueue`.

# Enabling the allocator

The custom allocator is enabled with the compiler flag -Xallocator=custom.
