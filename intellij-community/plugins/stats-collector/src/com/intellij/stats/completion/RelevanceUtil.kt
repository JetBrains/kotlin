package com.intellij.stats.completion

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil

object RelevanceUtil {
  private val IGNORED_FACTORS = setOf("kotlin.byNameAlphabetical", "scalaMethodCompletionWeigher", "unresolvedOnTop")

  fun asRelevanceMap(relevanceObjects: List<Pair<String, Any?>>): MutableMap<String, Any> {
    val relevanceMap = mutableMapOf<String, Any>()
    for (pair in relevanceObjects) {
      val name = pair.first.normalized()
      val value = pair.second
      if (name in IGNORED_FACTORS || value == null) continue
      when (name) {
        "proximity" -> relevanceMap.addCompoundValues("prox", value.toString())
        "kotlin.proximity" -> relevanceMap.addCompoundValues("kt_prox", value.toString())
        "kotlin.callableWeight" -> relevanceMap.addDataClassValues("kotlin.callableWeight", value.toString())
        "ml_weigh" -> relevanceMap.addCompoundValues("ml", value.toString())
        else -> relevanceMap[name] = value
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
  private fun MutableMap<String, Any>.addCompoundValues(prefix: String, proximity: String) {
    val items = proximity.replace("[", "").replace("]", "").split(",")

    this.addProperties(prefix, items)
  }

  private fun MutableMap<String, Any>.addDataClassValues(featureName: String, dataClassString: String) {
    if (StringUtil.countChars(dataClassString, '(') != 1) {
      this[featureName] = dataClassString
    }
    else {
      this.addProperties(featureName, dataClassString.substringAfter('(').substringBeforeLast(')').split(','))
    }
  }

  private fun MutableMap<String, Any>.addProperties(prefix: String, properties: List<String>) {
    properties.forEach {
      val key = "${prefix}_${it.substringBefore('=').trim()}"
      val value = it.substringAfter('=').trim()
      this[key] = value
    }
  }
}