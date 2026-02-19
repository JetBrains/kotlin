/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef UTF_FOR_CPP_WITH_REPLACEMENT_H
#define UTF_FOR_CPP_WITH_REPLACEMENT_H

#include "core.h"
#include "unchecked.h"

namespace utf8 {

namespace with_replacement {

constexpr uint32_t default_replacement = 0xfffd;

/*
 * Returns the next codepoint replacing any invalid sequence with the replacement codepoint.
 */
template<typename octet_iterator>
uint32_t next(octet_iterator &it, const octet_iterator end, uint32_t replacement) {
    uint32_t cp = 0;
    internal::utf_error err_code = utf8::internal::validate_next(it, end, cp);
    switch (err_code) {
        case internal::UTF8_OK :
            return cp;
        case internal::INVALID_LEAD :
        case internal::INVALID_CODE_POINT :
        case internal::OVERLONG_SEQUENCE :
            it++;
            return replacement;
        case internal::NOT_ENOUGH_ROOM :
        case internal::INCOMPLETE_SEQUENCE :
            // The whole incomplete sequence is replaced with one replacement codepoint.
            for (it++; it < end && utf8::internal::is_trail(*it); it++);
            return replacement;
    }
}

// Library API

/**
 * Calculates a count of characters needed to represent the string from first to last in UTF-16
 * taking into account surrogate symbols and invalid sequences.
 * Assumes that all invalid sequences in the input will be replaced with `replacement`
 * so each invalid sequence increases the result by 1 or 2 depending on `replacement`.
 */
template<typename octet_iterator>
uint32_t utf16_length(octet_iterator first,
                      const octet_iterator last,
                      const uint32_t replacement = default_replacement) {
    uint32_t dist = 0;
    while (first < last) {
        uint32_t cp = next(first, last, replacement);
        dist += (cp > 0xffff) ? 2 : 1;
    }
    return dist;
}


template<typename octet_iterator>
typename std::iterator_traits<octet_iterator>::difference_type
distance(octet_iterator first, const octet_iterator last) {
    typename std::iterator_traits<octet_iterator>::difference_type dist;
    uint32_t unused = 0;
    for (dist = 0; first < last; dist++) {
        next(first, last, unused);
    }
    return dist;
}

template<typename u16bit_iterator, typename octet_iterator>
octet_iterator utf16to8(u16bit_iterator start,
                        const u16bit_iterator end,
                        octet_iterator result,
                        const uint32_t replacement) {
    while (start != end) {
        uint32_t cp = utf8::internal::mask16(*start++);
        // Process surrogates.
        if (utf8::internal::is_lead_surrogate(cp)) {
            if (start != end) {
                uint32_t trail_surrogate = utf8::internal::mask16(*start);
                if (utf8::internal::is_trail_surrogate(trail_surrogate)) {
                    // Valid surrogate pair.
                    cp = (cp << 10) + trail_surrogate + internal::SURROGATE_OFFSET;
                    start++;
                } else {
                    cp = replacement; // Invalid input: lone lead surrogate.
                }
            } else {
                cp = replacement; // Invalid input: lone lead surrogate at the end of input.
            }
        } else if (utf8::internal::is_trail_surrogate(cp)) {
            cp = replacement; // Invalid input: lone trail surrogate
        }
        result = utf8::unchecked::append(cp, result);
    }
    return result;
}

template<typename u16bit_iterator, typename octet_iterator>
inline octet_iterator utf16to8(u16bit_iterator start,
                               const u16bit_iterator end,
                               octet_iterator result) {
  return utf16to8(start, end, result, default_replacement);
}

template<typename u16bit_iterator, typename octet_iterator>
u16bit_iterator utf8to16(octet_iterator start,
                         const octet_iterator end,
                         u16bit_iterator result,
                         const uint32_t replacement) {
    while (start != end) {
        // The `next` method takes care about replacing invalid sequences.
        uint32_t cp = next(start, end, replacement);
        if (cp > 0xffff) { //make a surrogate pair
            *result++ = static_cast<uint16_t>((cp >> 10) + internal::LEAD_OFFSET);
            *result++ = static_cast<uint16_t>((cp & 0x3ff) + internal::TRAIL_SURROGATE_MIN);
        } else
            *result++ = static_cast<uint16_t>(cp);
    }
    return result;
}

template<typename u16bit_iterator, typename octet_iterator>
inline u16bit_iterator utf8to16(octet_iterator start,
                                const octet_iterator end,
                                u16bit_iterator result) {
  return utf8to16(start, end, result, default_replacement);
}

} // namespace with_replacement

} // namespace utf8

#endif // UTF_FOR_CPP_WITH_REPLACEMENT_H
