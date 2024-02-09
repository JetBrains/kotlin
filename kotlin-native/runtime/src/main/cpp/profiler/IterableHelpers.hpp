/*
* Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#pragma once

#include <unordered_map>
#include <unordered_set>
#include <sstream>

namespace kotlin {

template<typename Iterable, typename KeySelector, typename ValueTransformer>
auto groupBy(const Iterable& iterable, KeySelector&& selectKey, ValueTransformer&& transformValue) {
    using OriginalValue = typename Iterable::value_type;
    using Key = typename std::result_of<KeySelector(const OriginalValue&)>::type;
    using TransformedValue = typename std::result_of<ValueTransformer(const OriginalValue&)>::type;
    std::unordered_map<Key, std::vector<TransformedValue>> groups;
    for (const auto& value: iterable) {
        auto key = selectKey(value);
        groups[key].push_back(transformValue(value));
    }
    return groups;
}

template<typename Iterable, typename KeySelector>
auto groupBy(const Iterable& iterable, KeySelector&& selectKey) {
    return groupBy(iterable, std::forward<KeySelector>(selectKey), [](const auto& x) { return x; });
}

template<typename Iterable, typename Associator>
auto associateWith(const Iterable& iterable, Associator&& assotiation) {
    using OriginalValue = typename Iterable::value_type;
    using AssociatedValue = typename std::result_of<Associator(const OriginalValue&)>::type;
    std::unordered_map<OriginalValue, AssociatedValue> associations;
    for (const auto& value: iterable) {
        associations.insert_or_assign(value, assotiation(value));
    }
    return associations;
}

template<typename Iterable, typename Filter>
auto filterToVector(const Iterable& iterable, Filter&& filter) {
    using OriginalValue = typename Iterable::value_type;
    std::vector<OriginalValue> filtered;
    for (const auto& value: iterable) {
        if (filter(value)) {
            filtered.push_back(value);
        }
    }
    return filtered;
}

template<typename Iterable, typename Map>
auto mapToVector(const Iterable& iterable, Map&& map) {
    using OriginalValue = typename Iterable::value_type;
    using MappedValue = typename std::result_of<Map(const OriginalValue&)>::type;
    std::vector<MappedValue> mapped;
    for (const auto& value: iterable) {
        mapped.push_back(map(value));
    }
    return mapped;
}

template<typename T, typename Ord>
void sortBy(std::vector<T>& vector, Ord&& ord, bool descending = false) {
    std::sort(vector.begin(), vector.end(), [ord = std::forward<Ord>(ord), descending](const T& l, const T& r) {
        auto ordL = ord(l);
        auto ordR = ord(r);
        return descending ? ordL > ordR : ordL < ordR;
    });
}

template<typename Iterable>
auto uniq(const Iterable& iterable) {
    std::unordered_set<typename Iterable::value_type> uniqSet(iterable.begin(), iterable.end());
    return Iterable(uniqSet.begin(), uniqSet.end());
}

}
