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
#include "City.h"

#include <string.h>

#include <algorithm>

namespace {

// Some primes between 2^63 and 2^64 for various uses.
static const uint64_t k0 = 0xc3a5c85c97cb3127ULL;
static const uint64_t k1 = 0xb492b66fbe98f273ULL;
static const uint64_t k2 = 0x9ae16a3b2f90404fULL;

uint64_t UNALIGNED_LOAD64(const char *p) {
  uint64_t result;
  memcpy(&result, p, sizeof(result));
  return result;
}

uint32_t UNALIGNED_LOAD32(const char *p) {
  uint32_t result;
  memcpy(&result, p, sizeof(result));
  return result;
}

#define bswap32(x) __builtin_bswap32(x)
#define bswap64(x) __builtin_bswap64(x)

#ifdef WORDS_BIGENDIAN
#define uint32_in_expected_order(x) (bswap32(x))
#define uint64_in_expected_order(x) (bswap64(x))
#else
#define uint32_in_expected_order(x) (x)
#define uint64_in_expected_order(x) (x)
#endif

uint64_t Fetch64(const char *p) {
  return uint64_in_expected_order(UNALIGNED_LOAD64(p));
}

uint32_t Fetch32(const char *p) {
  return uint32_in_expected_order(UNALIGNED_LOAD32(p));
}

// Bitwise right rotate.  Normally this will compile to a single
// instruction, especially if the shift is a manifest constant.
uint64_t Rotate(uint64_t val, int shift) {
  // Avoid shifting by 64: doing so yields an undefined result.
  return shift == 0 ? val : ((val >> shift) | (val << (64 - shift)));
}

uint64_t ShiftMix(uint64_t val) {
  return val ^ (val >> 47);
}

uint64_t HashLen16(uint64_t u, uint64_t v, uint64_t mul) {
  // Murmur-inspired hashing.
  uint64_t a = (u ^ v) * mul;
  a ^= (a >> 47);
  uint64_t b = (v ^ a) * mul;
  b ^= (b >> 47);
  b *= mul;
  return b;
}

typedef std::pair<uint64_t, uint64_t> uint128_t;

uint64_t Hash128to64(const uint128_t& x) {
  // Murmur-inspired hashing.
  const uint64_t kMul = 0x9ddfea08eb382d69ULL;
  uint64_t a = (x.first ^ x.second) * kMul;
  a ^= (a >> 47);
  uint64_t b = (x.second ^ a) * kMul;
  b ^= (b >> 47);
  b *= kMul;
  return b;
}

uint64_t HashLen16(uint64_t u, uint64_t v) {
  return Hash128to64(uint128_t(u, v));
}

uint64_t HashLen0to16(const char *s, size_t len) {
  if (len >= 8) {
    uint64_t mul = k2 + len * 2;
    uint64_t a = Fetch64(s) + k2;
    uint64_t b = Fetch64(s + len - 8);
    uint64_t c = Rotate(b, 37) * mul + a;
    uint64_t d = (Rotate(a, 25) + b) * mul;
    return HashLen16(c, d, mul);
  }
  if (len >= 4) {
    uint64_t mul = k2 + len * 2;
    uint64_t a = Fetch32(s);
    return HashLen16(len + (a << 3), Fetch32(s + len - 4), mul);
  }
  if (len > 0) {
    uint8_t a = s[0];
    uint8_t b = s[len >> 1];
    uint8_t c = s[len - 1];
    uint32_t y = static_cast<uint32_t>(a) + (static_cast<uint32_t>(b) << 8);
    uint32_t z = len + (static_cast<uint32_t>(c) << 2);
    return ShiftMix(y * k2 ^ z * k0) * k2;
  }
  return k2;
}

// This probably works well for 16-byte strings as well, but it may be overkill
// in that case.
static uint64_t HashLen17to32(const char *s, size_t len) {
  uint64_t mul = k2 + len * 2;
  uint64_t a = Fetch64(s) * k1;
  uint64_t b = Fetch64(s + 8);
  uint64_t c = Fetch64(s + len - 8) * mul;
  uint64_t d = Fetch64(s + len - 16) * k2;
  return HashLen16(Rotate(a + b, 43) + Rotate(c, 30) + d,
                   a + Rotate(b + k2, 18) + c, mul);
}

// Return a 16-byte hash for 48 bytes.  Quick and dirty.
// Callers do best to use "random-looking" values for a and b.
std::pair<uint64_t, uint64_t> WeakHashLen32WithSeeds(
    uint64_t w, uint64_t x, uint64_t y, uint64_t z, uint64_t a, uint64_t b) {
  a += w;
  b = Rotate(b + a + z, 21);
  uint64_t c = a;
  a += x;
  a += y;
  b += Rotate(a, 44);
  return std::make_pair(a + z, b + c);
}

// Return a 16-byte hash for s[0] ... s[31], a, and b.  Quick and dirty.
std::pair<uint64_t, uint64_t> WeakHashLen32WithSeeds(
    const char* s, uint64_t a, uint64_t b) {
  return WeakHashLen32WithSeeds(Fetch64(s),
                                Fetch64(s + 8),
                                Fetch64(s + 16),
                                Fetch64(s + 24),
                                a,
                                b);
}

// Return an 8-byte hash for 33 to 64 bytes.
uint64_t HashLen33to64(const char *s, size_t len) {
  uint64_t mul = k2 + len * 2;
  uint64_t a = Fetch64(s) * k2;
  uint64_t b = Fetch64(s + 8);
  uint64_t c = Fetch64(s + len - 24);
  uint64_t d = Fetch64(s + len - 32);
  uint64_t e = Fetch64(s + 16) * k2;
  uint64_t f = Fetch64(s + 24) * 9;
  uint64_t g = Fetch64(s + len - 8);
  uint64_t h = Fetch64(s + len - 16) * mul;
  uint64_t u = Rotate(a + g, 43) + (Rotate(b, 30) + c) * 9;
  uint64_t v = ((a + g) ^ d) + f + 1;
  uint64_t w = bswap64((u + v) * mul) + h;
  uint64_t x = Rotate(e + f, 42) + c;
  uint64_t y = (bswap64((v + w) * mul) + g) * mul;
  uint64_t z = e + f + c;
  a = bswap64((x + z) * mul + y) + b;
  b = ShiftMix((z + a) * mul + d + h) * mul;
  return b + x;
}

} // namespace

extern "C" {

uint64_t CityHash64(const void* data, size_t len) {
  const char* s = reinterpret_cast<const char*>(data);
  if (len <= 32) {
    if (len <= 16) {
      return HashLen0to16(s, len);
    } else {
      return HashLen17to32(s, len);
    }
  } else if (len <= 64) {
    return HashLen33to64(s, len);
  }

  // For strings over 64 bytes we hash the end first, and then as we
  // loop we keep 56 bytes of state: v, w, x, y, and z.
  uint64_t x = Fetch64(s + len - 40);
  uint64_t y = Fetch64(s + len - 16) + Fetch64(s + len - 56);
  uint64_t z = HashLen16(Fetch64(s + len - 48) + len, Fetch64(s + len - 24));
  std::pair<uint64_t, uint64_t> v = WeakHashLen32WithSeeds(s + len - 64, len, z);
  std::pair<uint64_t, uint64_t> w = WeakHashLen32WithSeeds(s + len - 32, y + k1, x);
  x = x * k1 + Fetch64(s);

  // Decrease len to the nearest multiple of 64, and operate on 64-byte chunks.
  len = (len - 1) & ~static_cast<size_t>(63);
  do {
    x = Rotate(x + y + v.first + Fetch64(s + 8), 37) * k1;
    y = Rotate(y + v.second + Fetch64(s + 48), 42) * k1;
    x ^= w.second;
    y += v.first + Fetch64(s + 40);
    z = Rotate(z + w.first, 33) * k1;
    v = WeakHashLen32WithSeeds(s, v.second * k1, x + w.first);
    w = WeakHashLen32WithSeeds(s + 32, z + w.second, y + Fetch64(s + 16));
    std::swap(z, x);
    s += 64;
    len -= 64;
  } while (len != 0);
  return HashLen16(HashLen16(v.first, w.first) + ShiftMix(y) * k1 + z,
                   HashLen16(v.second, w.second) + x);
}

} // extern "C"
