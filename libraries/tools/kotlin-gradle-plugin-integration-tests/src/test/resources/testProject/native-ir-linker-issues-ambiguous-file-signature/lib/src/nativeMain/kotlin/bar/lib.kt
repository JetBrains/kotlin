@file:Suppress("PackageDirectoryMismatch")

package org.sample

class LazyGridState {
    val layoutInfo: LazyGridLayoutInfo get() = EmptyLazyGridLayoutInfo
}

private object EmptyLazyGridLayoutInfo : LazyGridLayoutInfo
