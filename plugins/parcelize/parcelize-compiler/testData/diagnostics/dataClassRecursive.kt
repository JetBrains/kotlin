// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
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
data class SerializableNode(val next: SerializableNode?) : java.io.Serializable
data class SerializableNodeHolder(val nodes: List<SerializableNode>)

data class GenericNode<T>(val value: T, val next: GenericNode<T>?)
data class GrowingNode<T>(val value: T, val next: GrowingNode<List<T>>?)

data class Box<T>(val x: T)
data class BoxedNode(val next: Box<BoxedNode>?)
data class ContainsNode(val node: Node)

data class SerializableBox<U : java.io.Serializable>(val x: U) : java.io.Serializable
data class BoundedLoopHolder<T : SerializableBox<Loop>>(val box: T)
data class Loop(val holder: BoundedLoopHolder<*>?) : java.io.Serializable

@Parcelize
data class ParcelableNode(val x: Int, val next: ParcelableNode?) : Parcelable

data class ContainsParcelableNode(val node: ParcelableNode)

data class User(val coords: Pair<Int, Int>)

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
    val serializableNodeHolder: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass SerializableNodeHolder<!>,
    val genericNode: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass GenericNode<String><!>,
    val growingNode: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass GrowingNode<String><!>,
    val boxedNode: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass BoxedNode<!>,
    val loop: <!PARCELABLE_RECURSIVE_DATA_CLASS_NOT_SUPPORTED!>@DataClass Loop<!>,

    // Not recursive
    val nestedBox: @DataClass Box<@DataClass Box<Int>>,
    val containsParcelableNode: @DataClass ContainsParcelableNode,
    val pair: @DataClass Pair<User, Boolean>,
) : Parcelable
