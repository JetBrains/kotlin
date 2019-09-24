class Owner {
    var list: MutableList<String> = ArrayList()
}

class Updater {
    fun update(owner: Owner) {
        owner.list.add("")
    }
}