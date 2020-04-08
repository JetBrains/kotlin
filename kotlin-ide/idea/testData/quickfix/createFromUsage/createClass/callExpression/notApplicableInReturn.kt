// "Create class 'BookKeeper'" "false"
// ACTION: Create function 'BookKeeper'
// ACTION: Rename reference
// ERROR: Unresolved reference: BookKeeper
package pack

import pack.Currrency.EUR

enum class Currrency { EUR }
class Item(val p1: Double, p2: Currrency)
class Transaction(vararg val p: Item)
class Man

fun place(): Man {
    val transactions = listOf(Transaction(Item(10.0, EUR), Item(10.0, EUR)))
    return BookKee<caret>per(transactions)
}