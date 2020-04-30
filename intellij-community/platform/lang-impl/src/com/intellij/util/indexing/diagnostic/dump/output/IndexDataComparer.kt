package com.intellij.util.indexing.diagnostic.dump.output

import com.intellij.psi.stubs.*
import com.intellij.util.containers.hash.EqualityPolicy
import com.intellij.util.containers.hash.LinkedHashMap
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.SingleEntryFileBasedIndexExtension
import java.util.*

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

    val keyDescriptor = extension.keyDescriptor

    // Use LinkedHashMaps with custom equality policy to compare keys.
    // LinkedHashMap does not support null values.

    val expectedMap = LinkedHashMap<K, V>(keyDescriptor)
    val actualMap = LinkedHashMap<K, V>(keyDescriptor)

    val expectedKeysForNullValue = Collections.newSetFromMap(LinkedHashMap<K, Boolean>(keyDescriptor))
    val actualKeysForNullValue = Collections.newSetFromMap(LinkedHashMap<K, Boolean>(keyDescriptor))

    for ((key, value) in expectedMap) {
      if (value == null) {
        expectedKeysForNullValue += key
      } else {
        expectedMap[key] = value
      }
    }

    for ((key, value) in actualMap) {
      if (value == null) {
        actualKeysForNullValue += key
      } else {
        actualMap[key] = value
      }
    }

    if (expectedKeysForNullValue.size != actualKeysForNullValue.size) {
      return false
    }

    for (key in expectedKeysForNullValue) {
      if (!actualKeysForNullValue.contains(key)) {
        return false
      }
    }

    for ((expectedKey, expectedValue) in expectedMap) {
      val actualValue = actualMap[expectedKey]
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

  fun areStubTreesTheSame(
    expectedTree: SerializedStubTree,
    actualTree: SerializedStubTree
  ): Boolean {
    if (!areStubsTheSame(expectedTree.stub, actualTree.stub)) {
      return false
    }

    val expectedForwardIndex: MutableMap<StubIndexKey<*, *>, MutableMap<Any, StubIdList>> = expectedTree.stubIndicesValueMap
    val actualForwardIndex: Map<StubIndexKey<*, *>, Map<Any, StubIdList>> = actualTree.stubIndicesValueMap

    // StubIndexKey-s don't have equals()\hashCode() implementations, so we have to compare them by names.
    // Create helper mappings from names to StubIndexKey-s preserving the keys identity.
    val expectedStubIndexKeyNameToKey: Map<String, StubIndexKey<*, *>> = expectedForwardIndex.keys.associateBy { it.name }
    val actualStubIndexKeyNameToKey: Map<String, StubIndexKey<*, *>> = actualForwardIndex.keys.associateBy { it.name }
    if (expectedStubIndexKeyNameToKey.keys != actualStubIndexKeyNameToKey.keys) {
      return false
    }

    for (keyName in expectedStubIndexKeyNameToKey.keys) {
      val expectedStubIndexKey = expectedStubIndexKeyNameToKey.getValue(keyName)
      val actualStubIndexKey = actualStubIndexKeyNameToKey.getValue(keyName)

      val expectedKeyValues: Map<Any, StubIdList> = expectedForwardIndex.getValue(expectedStubIndexKey)
      val actualKeyValues: Map<Any, StubIdList> = actualForwardIndex.getValue(actualStubIndexKey)
      if (expectedKeyValues.size != actualKeyValues.size) {
        return false
      }

      // Do not use .equals() to compare [oneKeyValues] and [twoKeyValues]
      // Their internal implementation is THashMap<Any, StubIdList>
      // with customized [TObjectHashingStrategy] for keys.
      // But [THashMap.equals()] does not check equality
      // via the [TObjectHashingStrategy.equals()] but uses the standard Object.equals().
      // Thus, we need to manually check for each key of the first map that the same key
      // in the second map is present (and under the hood the hashing equality will be taken into account).
      for ((expectedKey: Any, expectedStubList: StubIdList) in expectedKeyValues) {
        val actualStubList = actualKeyValues[expectedKey] ?: return false
        if (expectedStubList != actualStubList) {
          return false
        }
      }
    }
    return true
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