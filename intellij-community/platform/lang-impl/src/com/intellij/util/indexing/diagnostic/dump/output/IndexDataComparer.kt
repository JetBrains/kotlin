package com.intellij.util.indexing.diagnostic.dump.output

import com.intellij.psi.stubs.ObjectStubBase
import com.intellij.psi.stubs.SerializedStubTree
import com.intellij.psi.stubs.Stub
import com.intellij.util.containers.hash.EqualityPolicy
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension

object IndexDataComparer {

  fun <K, V> areIndexedDataOfFileTheSame(
    extension: FileBasedIndexExtension<K, V>,
    expectedData: Map<K, V>,
    actualData: Map<K, V>
  ): Boolean {
    if (expectedData.isEmpty() && actualData.isEmpty()) {
      return true
    }
    if (expectedData.size != actualData.size) {
      return false
    }
    if (extension is SingleEntryFileBasedIndexExtension) {
      // Do not check keys equality. There is no contract that keys should be file IDs, or any specific value, in general.
      val expectedValue = expectedData.values.first()
      val actualValue = actualData.values.first()
      return areValuesTheSame(extension, expectedValue, actualValue)
    }

    for ((expectedKey, expectedValue) in expectedData) {
      val actualValue = actualData[expectedKey] ?: return false
      if (!areValuesTheSame(extension, expectedValue, actualValue)) {
        return false
      }
    }
    return true
  }

  private fun <V> areValuesTheSame(
    extension: FileBasedIndexExtension<*, *>,
    expectedValue: V?,
    actualValue: V?
  ): Boolean {
    if (expectedValue == null || actualValue == null) {
      return expectedValue == null && actualValue == null
    }
    if (expectedValue is SerializedStubTree) {
      if (actualValue !is SerializedStubTree) {
        return false
      }
      val currentStubTree = runCatching { expectedValue.stubIndicesValueMap; expectedValue }.getOrNull() as? SerializedStubTree ?: return false
      val actualStubTree = runCatching { actualValue.stubIndicesValueMap; actualValue }.getOrNull() as? SerializedStubTree ?: return false
      return areStubTreesTheSame(currentStubTree, actualStubTree)
    }
    val valueExternalizer = extension.valueExternalizer
    return if (valueExternalizer is EqualityPolicy<*>) {
      @Suppress("UNCHECKED_CAST")
      (valueExternalizer as EqualityPolicy<V>).isEqual(expectedValue, actualValue)
    } else {
      expectedValue == actualValue
    }
  }

  fun areStubTreesTheSame(expectedTree: SerializedStubTree, actualTree: SerializedStubTree): Boolean {
    if (!areStubsTheSame(expectedTree.stub, actualTree.stub)) {
      return false
    }
    return expectedTree.stubIndicesValueMap == actualTree.stubIndicesValueMap
  }

  private fun areStubsTheSame(expectedStub: Stub, actualStub: Stub): Boolean {
    // Check toString() to not rely on identity equality of [ObjectStubSerializer]s
    // because [ObjectStubSerializer] does not declare equals() / hashCode().
    if (expectedStub.stubType != actualStub.stubType
        && expectedStub.stubType?.toString() != actualStub.stubType?.toString()
    ) {
      return false
    }

    // TODO: improve this comparison approach. Stubs don't have proper equals()
    if (expectedStub != actualStub
        && expectedStub.javaClass.name != actualStub.javaClass.name) {
      return false
    }
    if (expectedStub is ObjectStubBase<*>) {
      if (actualStub !is ObjectStubBase<*>) {
        return false
      }
      if (expectedStub.stubId != actualStub.stubId) {
        return false
      }
    }

    val expectedChildren: List<Stub> = expectedStub.childrenStubs
    val actualChildren: List<Stub> = actualStub.childrenStubs
    if (expectedChildren.size != actualChildren.size) {
      return false
    }

    for (index in expectedChildren.indices) {
      val expectedChild = expectedChildren[index]
      val actualChild = actualChildren[index]
      if (!areStubsTheSame(expectedChild, actualChild)) {
        return false
      }
    }
    return true
  }


}