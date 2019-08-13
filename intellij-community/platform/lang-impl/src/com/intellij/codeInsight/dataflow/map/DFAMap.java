package com.intellij.codeInsight.dataflow.map;

import com.intellij.codeInsight.dataflow.SetUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author yole
 */
public class DFAMap<V> {

  // invariant:
  // if myAll != null, the map contains more than one value, and all of them are in myAll
  // if myAll == null && myK != null, the map contains only one value (myK, myV)
  // if myAll == null && myK == null, the map is empty
  private String myK;
  private V myV;
  private HashMap<String, V> myAll;

  private static final DFAMap ourEmptyMap = new DFAMap() {
    @Override
    public void put(String key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void remove(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DFAMap asWritable() {
      return new DFAMap();
    }
  };

  public DFAMap() {
  }

  public DFAMap(DFAMap<? extends V> initialMap) {
    myK = initialMap.myK;
    myV = initialMap.myV;
    myAll = initialMap.myAll == null ? null : new HashMap<>(initialMap.myAll);
  }

  public static <V> DFAMap<V> empty() {
    //noinspection unchecked
    return (DFAMap<V>) ourEmptyMap;
  }

  public void addKeys(HashSet<? super String> allNames) {
    if (myAll != null) {
      allNames.addAll(myAll.keySet());
    }
    else if (myK != null) {
      allNames.add(myK);
    }
  }

  public void put(String key, V value) {
    if ((myK == null || myK.equals(key)) && myAll == null) {
      myK = key;
      myV = value;
    }
    else {
      if (myAll == null) {
        myAll = new HashMap<>();
        myAll.put(myK, myV);
      }
      myAll.put(key, value);
    }
  }

  @Nullable
  public V get(String key) {
    if (myAll != null) {
      return myAll.get(key);
    }
    if (key.equals(myK)) {
      return myV;
    }
    return null;
  }

  public void remove(String name) {
    if (myAll != null) {
      myAll.remove(name);
      if (myAll.size() == 1) {
        final Map.Entry<String, V> e = myAll.entrySet().iterator().next();
        myK = e.getKey();
        myV = e.getValue();
        myAll = null;
      }
    }
    else if (name.equals(myK)) {
      myK = null;
      myV = null;
    }
  }

  public boolean containsKey(String name) {
    if (myAll != null) {
      return myAll.containsKey(name);
    }
    return name.equals(myK);
  }

  public Set<String> intersectKeys(@Nullable Set<String> names2Include) {
    if (myAll != null) {
      if (names2Include == null) return myAll.keySet();
      return SetUtil.intersect(names2Include, myAll.keySet());
    }
    if (myK != null && (names2Include == null || names2Include.contains(myK))) {
      if (names2Include != null && names2Include.size() == 1) return names2Include;
      final HashSet<String> result = new HashSet<>();
      result.add(myK);
      return result;
    }
    return Collections.emptySet();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DFAMap)) return false;
    @SuppressWarnings({"unchecked"}) DFAMap<V> rhs = (DFAMap<V>) obj;
    if (myAll == null) {
      if (rhs.myAll != null) return false;
      if (myK == null) return rhs.myK == null;
      return myK.equals(rhs.myK) && myV.equals(rhs.myV);
    }
    else {
      if (rhs.myAll == null) return false;
      return myAll.equals(rhs.myAll);
    }
  }

  public Collection<V> values() {
    if (myAll != null) {
      return myAll.values();
    }
    if (myV != null) {
      return Collections.singletonList(myV);
    }
    return Collections.emptyList();
  }

  public Collection<? extends Map.Entry<String, V>> entrySet() {
    if (myAll != null) {
      return myAll.entrySet();
    }
    if (myK != null) {
      return Collections.singleton(new Map.Entry<String, V>() {
        @Override
        public String getKey() {
          return myK;
        }

        @Override
        public V getValue() {
          return myV;
        }

        @Override
        public V setValue(V value) {
          throw new UnsupportedOperationException();
        }
      });
    }
    return Collections.emptyList();
  }

  public Map<String, V> toMap() {
    if (myAll != null) {
      return myAll;
    }
    if (myK != null) {
      return Collections.singletonMap(myK, myV);
    }
    return Collections.emptyMap();
  }

  public DFAMap<V> asWritable() {
    return new DFAMap<>(this);
  }

  @Override
  public String toString() {
    if (this == ourEmptyMap){
      return "Empty Map";
    }
    if (myAll != null){
      return myAll.toString();
    }
    if (myK != null){
      return "{" + myK + "=" + myV + "}";
    }
    return "Empty";
  }

  public Set<String> keySet() {
    if (myAll != null){
      return myAll.keySet();
    }
    return myK != null ? Collections.singleton(myK) : Collections.emptySet();
  }
}
