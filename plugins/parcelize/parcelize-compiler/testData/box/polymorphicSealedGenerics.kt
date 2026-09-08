// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

@Parcelize
data class Item(val name: String) : Parcelable

@Parcelize
@PolymorphicSealed
sealed class Result<out T : Parcelable> : Parcelable {
    data class Success<T : Parcelable>(val value: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Parcelize
@PolymorphicSealed
sealed interface Tree<out T : Parcelable> : Parcelable {
    data class Node<T : Parcelable>(val value: T, val left: Tree<T>?, val right: Tree<T>?) : Tree<T>
    object Empty : Tree<Nothing>
}

@Parcelize
data class Container<T : Parcelable>(
    val result: Result<T>,
    val tree: Tree<T>,
) : Parcelable

fun box() = parcelTest { parcel ->
    val success: Result<Item> = Result.Success(Item("kotlin"))
    val error: Result<Item> = Result.Error("network error")
    val loading: Result<Item> = Result.Loading

    val tree: Tree<Item> = Tree.Node(
        Item("root"),
        Tree.Node(Item("left"), null, null),
        Tree.Empty
    )

    val container = Container(success, tree)

    success.writeToParcel(parcel, 0)
    error.writeToParcel(parcel, 0)
    loading.writeToParcel(parcel, 0)
    tree.writeToParcel(parcel, 0)
    container.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val rSuccess = parcelableCreator<Result<Item>>().createFromParcel(parcel)
    val rError = parcelableCreator<Result<Item>>().createFromParcel(parcel)
    val rLoading = parcelableCreator<Result<Item>>().createFromParcel(parcel)
    val rTree = parcelableCreator<Tree<Item>>().createFromParcel(parcel)
    val rContainer = parcelableCreator<Container<Item>>().createFromParcel(parcel)

    assert(success == rSuccess)
    assert(error == rError)
    assert(loading == rLoading)
    assert(tree == rTree)
    assert(container == rContainer)
}
