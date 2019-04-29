package com.intellij.stats.completion

object RelevanceUtil {
  fun asRelevanceMap(relevanceObjects: List<com.intellij.openapi.util.Pair<String, Any?>>): MutableMap<String, Any> {
    val relevanceMap = mutableMapOf<String, Any>()
    for (pair in relevanceObjects) {
      val name = pair.first.normalized()
      val value = pair.second
      if (value == null) continue
      if (name == "proximity") {
        val proximityMap = value.toString().asProximityMap("prox")
        relevanceMap.putAll(proximityMap)
      }
      else {
        relevanceMap[name] = value
      }
    }

    return relevanceMap
  }

  private fun String.normalized(): String {
    return substringBefore('@')
  }

  /**
   * Proximity features now came like [samePsiFile=true, openedInEditor=false], need to convert to proper map
   */
  private fun String.asProximityMap(prefix: String): Map<String, Any> {
    val items = this.replace("[", "").replace("]", "").split(",")

    return items.map {
      val (key, value) = it.trim().split("=")
      "${prefix}_$key" to value
    }.toMap()
  }
}