package com.intellij.util.indexing.diagnostic.dump.output

import com.intellij.psi.stubs.*

object IndexDataPresenter {

  fun <V> getPresentableIndexValue(value: V): String {
    return if (value is SerializedStubTree) {
      buildString {
        appendln("Stub tree:")
        appendln(stubToString(value.stub))

        val stubIndicesValueMap: Map<StubIndexKey<*, *>, Map<Any, StubIdList>> = try {
          value.stubIndicesValueMap
        }
        catch (e: Exception) {
          appendln("Failed-to-read stub tree forward index: (message = ${e.message}) (exception class = ${e.javaClass.simpleName})")
          return@buildString
        }

        appendln("Stub tree forward index:")
        for ((stubIndexKey, stubIndexValues) in stubIndicesValueMap) {
          appendln("    ${stubIndexKey.name}")
          for ((key, stubIdList) in stubIndexValues) {
            val stubIds = (0 until stubIdList.size()).map { stubIdList[it] }
            appendln("        $key -> " + stubIds.joinToString())
          }
        }
      }
    }
    else {
      value.toString()
    }
  }

  fun <K, V> getPresentableKeyValueMap(keyValueMap: Map<K, V>): String {
    if (keyValueMap.isEmpty()) {
      return "[]"
    }
    return buildString {
      for ((key, value) in keyValueMap) {
        appendln("$key -> ${getPresentableIndexValue(value)}")
      }
    }
  }

  private fun stubToString(node: Stub, indent: String = "  "): String =
    buildString {
      append(indent)
      val stubType = node.stubType
      if (stubType != null) {
        append(stubType.toString()).append(':')
      }
      append(node.toString())
      if (node is ObjectStubBase<*>) {
        append(" (id = ").append(node.stubId).append(")")
      }
      append('\n')
      for (child in node.childrenStubs) {
        appendln(stubToString(child, indent + "  "))
      }
    }

}