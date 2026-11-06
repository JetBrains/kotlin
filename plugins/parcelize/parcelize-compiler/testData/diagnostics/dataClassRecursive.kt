// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)

package test

import kotlinx.parcelize.*
import android.os.Parcelable

data class Node(val x: Int, val next: Node?)

@Parcelize
class MyParcelableThing(val n: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass Node<!>) : Parcelable

data class A(val b: B?)
data class B(val a: A?)

data class Tree(val children: List<@DataClass Tree>)
data class SerializableTree(val children: List<SerializableTree>) : java.io.Serializable

data class GenericNode<T>(val value: T, val next: GenericNode<T>?)
data class GrowingNode<T>(val value: T, val next: GrowingNode<List<T>>?)

data class Box<T>(val x: T)
data class ContainsNode(val node: Node)

@Parcelize
data class ParcelableNode(val x: Int, val next: ParcelableNode?) : Parcelable

data class ContainsParcelableNode(val node: ParcelableNode)

@Parcelize
class C(
    val node: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass Node<!>,
    val nullableNode: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass Node?<!>,
    val nodes: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>List<@DataClass Node><!>,
    val containsNode: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass ContainsNode<!>,
    val a: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass A<!>,
    val b: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass B<!>,
    val tree: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass Tree<!>,
    val serializableTree: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass SerializableTree<!>,
    val genericNode: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass GenericNode<String><!>,
    val growingNode: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass GrowingNode<String><!>,

    // Not recursive
    val nestedBox: @DataClass Box<@DataClass Box<Int>>,
    val containsParcelableNode: @DataClass ContainsParcelableNode,
) : Parcelable
