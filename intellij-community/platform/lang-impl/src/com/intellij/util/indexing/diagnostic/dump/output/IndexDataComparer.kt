package com.intellij.util.indexing.diagnostic.dump.output

import com.intellij.psi.stubs.*

object IndexDataComparer {

  fun <V> areValuesTheSame(currentValue: V, storedValue: V): Boolean {
    return if (currentValue is SerializedStubTree) {
      areStubTreesTheSame(storedValue as SerializedStubTree, currentValue as SerializedStubTree)
    }
    else {
      currentValue == storedValue
    }
  }

  private fun areStubTreesTheSame(
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