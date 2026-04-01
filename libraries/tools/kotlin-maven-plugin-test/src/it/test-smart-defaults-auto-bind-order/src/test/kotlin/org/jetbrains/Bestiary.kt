package org.jetbrains

class Bestiary {
    fun monsterWeakness(monster: String): Sign = when (monster) {
        "wraith" -> Sign.YRDEN
        "drowner" -> Sign.IGNI
        else -> Sign.QUEN
    }
}
