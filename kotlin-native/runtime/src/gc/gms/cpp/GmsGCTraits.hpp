#pragma once

#include <cstddef>

#include "Barriers.hpp"
#include "CollectionScope.hpp"
#include "ConcurrentMark.hpp"
#include "GmsCollectionPolicy.hpp"

namespace kotlin::gc::internal {

// GMS (Generational Mark & Sweep) reuses the CMS concurrent mark & sweep machinery (ParallelProcessor,
// SATB deletion barrier, mark termination) and layers a non-moving generational scheme on top: a
// per-object sticky "old" bit, an old->young remembered set that seeds Eden roots, eden-terminating
// marking, and a scope-aware sweep that promotes survivors. See gc/gms for how it diverges from gc/cms.
struct GmsGCTraits {
    static constexpr auto kName = "Generational Mark & Sweep";
    static constexpr bool kConcurrentSweep = true;
    // Enables the eden/full collection split in MainGCThread. Eden (minor) collections trace and sweep
    // only the young generation -- old survivors are implicitly live and are not revisited -- while
    // Full (major) collections re-trace and sweep the whole heap and clear the sticky "old" bits.
    static constexpr bool kGenerational = true;
    using Mark = mark::ConcurrentMark;

    // Called by MainGCThread during STW, before marking. Selects Eden vs Full behavior for the mark
    // and sweep phases (read via mark::edenCollection() by MarkTraits and gc::tryResetMark). A pending
    // forced-Full request (remembered-set overflow / explicit collect) upgrades an Eden decision here;
    // this runs during STW, so the request flag cannot change under us.
    static gc::CollectionScope onCollectionStart(gc::CollectionScope scope) noexcept {
        // Take (and clear) the forced-Full request unconditionally: it must be *consumed* every cycle,
        // even when the policy already selected Full. A short-circuiting `||` would skip the take when
        // `scope == Full`, leaving the flag set so it spuriously upgrades a *later* Eden collection.
        const bool forcedFull = barriers::takeFullCollectionRequest();
        const bool full = (scope == gc::CollectionScope::Full) || forcedFull;
        mark::setEdenCollection(!full);
        // Open the collection window (runs under STW, so all mutators observe it before they resume for
        // the concurrent sweep). While it is open the write barrier stops trusting an owner's "old" bit
        // as a source-age filter, because the concurrent sweep promotes survivors under the running
        // mutators -- see barriers::setCollectionInProgress.
        barriers::setCollectionInProgress(true);
        return full ? gc::CollectionScope::Full : gc::CollectionScope::Eden;
    }

    // Called by MainGCThread after sweeping. Feeds the live ("kept") byte count into the heap-growth
    // policy and per-scope statistics.
    static void onCollectionFinish(gc::CollectionScope scope, size_t keptBytes) noexcept {
        RuntimeAssert((scope == gc::CollectionScope::Eden) == mark::edenCollection(), "GMS collection scope mismatch");
        gms::onCollectionFinished(scope, keptBytes);
        // Close the collection window. MainGCThread calls this right after allocator_.sweep() returns,
        // and Heap::Sweep drains every unswept page and waits for concurrent sweepers before returning,
        // so all promotions (setOld) are done: owner ages are stable again and the barrier can resume
        // the cheap source-age filter. See barriers::setCollectionInProgress.
        barriers::setCollectionInProgress(false);
        // Clear the Eden flag now that the collection is fully over. MainGCThread calls this right after
        // allocator_.sweep() returns, and Heap::Sweep drains every unswept page and waits for concurrent
        // sweepers before returning, so no sweep (which reads the flag via gc::tryResetMark) is still in
        // flight and none runs again until the next collection's onCollectionStart. Leaving the flag set
        // would make between-collection liveness queries wrong: gc::isMarked() reports an old survivor as
        // marked only while an Eden collection is in progress, so a resting old object must read as
        // unmarked once the collection that promoted it has finished.
        mark::setEdenCollection(false);
    }
};

} // namespace kotlin::gc::internal
