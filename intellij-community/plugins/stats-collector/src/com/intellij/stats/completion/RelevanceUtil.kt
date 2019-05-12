package com.intellij.stats.completion

object RelevanceUtil {
  private val IGNORED_FACTORS = setOf("kotlin.byNameAlphabetical", "scalaMethodCompletionWeigher", "unresolvedOnTop")

  fun asRelevanceMap(relevanceObjects: List<com.intellij.openapi.util.Pair<String, Any?>>): MutableMap<String, Any> {
    val relevanceMap = mutableMapOf<String, Any>()
    for (pair in relevanceObjects) {
      val name = pair.first.normalized()
      val value = pair.second
      if (name in IGNORED_FACTORS || value == null) continue
      if (name == "proximity") {
        relevanceMap.addProximityValues("prox", value.toString())
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
  private fun MutableMap<String, Any>.addProximityValues(prefix: String, proximity: String) {
    val items = proximity.replace("[", "").replace("]", "").split(",")

    items.forEach {
      val key = "${prefix}_${it.substringBefore('=').trim()}"
      val value = it.substringAfter('=').trim()
      this[key] = value
    }
  }
}