package com.intellij.stats.completion

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil

object RelevanceUtil {
  private val IGNORED_FACTORS = setOf("kotlin.byNameAlphabetical", "scalaMethodCompletionWeigher", "unresolvedOnTop")

  /*
  * First map contains only features affecting default elements ordering
  * */
  fun asRelevanceMaps(relevanceObjects: List<Pair<String, Any?>>): kotlin.Pair<Map<String, Any>, MutableMap<String, Any>> {
    val relevanceMap = mutableMapOf<String, Any>()
    val additionalMap = mutableMapOf<String, Any>()
    for (pair in relevanceObjects) {
      val name = pair.first.normalized()
      val value = pair.second
      if (name in IGNORED_FACTORS || value == null) continue
      when (name) {
        "proximity" -> relevanceMap.addCompoundValues("prox", value.toString())
        "kotlin.proximity" -> relevanceMap.addCompoundValues("kt_prox", value.toString())
        "kotlin.callableWeight" -> relevanceMap.addDataClassValues("kotlin.callableWeight", value.toString())
        "ml_weigh" -> additionalMap.addCompoundValues("ml", value.toString())
        else -> relevanceMap[name] = value
      }
    }

    return kotlin.Pair(relevanceMap, additionalMap)
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
      if (it.isNotBlank()) {
        val key = "${prefix}_${it.substringBefore('=').trim()}"
        val value = it.substringAfter('=').trim()
        this[key] = value
      }
    }
  }
}