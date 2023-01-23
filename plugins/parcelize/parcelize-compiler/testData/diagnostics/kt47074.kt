// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-47074

package work.hard.parcelableissue

import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import work.hard.parcelableissue.WorkStore.*

interface Store<K, T, V>

interface WorkStore : Store<Intent, State, Label> {
    @kotlinx.parcelize.Parcelize
    data class State(
        val isLoading: Boolean,
    ) : Parcelable

    sealed class Intent

    sealed class Label
}
