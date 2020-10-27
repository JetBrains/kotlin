/* ----------------------------------------------------------------------------
Copyright (c) 2019, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"LICENSE" at the root of this distribution.
-----------------------------------------------------------------------------*/

/* ----------------------------------------------------------------------------
This file is meant to be included in other files for efficiency.
It implements a bitmap that can set/reset sequences of bits atomically
and is used to concurrently claim memory ranges.

A bitmap is an array of fields where each field is a machine word (`uintptr_t`)

A current limitation is that the bit sequences cannot cross fields
and that the sequence must be smaller or equal to the bits in a field.
---------------------------------------------------------------------------- */
#pragma once
#ifndef MI_BITMAP_C
#define MI_BITMAP_C

#include "mimalloc.h"
#include "mimalloc-internal.h"

/* -----------------------------------------------------------
  Bitmap definition
----------------------------------------------------------- */

#define MI_BITMAP_FIELD_BITS   (8*MI_INTPTR_SIZE)
#define MI_BITMAP_FIELD_FULL   (~((uintptr_t)0))   // all bits set

// An atomic bitmap of `uintptr_t` fields
typedef _Atomic(uintptr_t)  mi_bitmap_field_t;
typedef mi_bitmap_field_t*           mi_bitmap_t;

// A bitmap index is the index of the bit in a bitmap.
typedef size_t mi_bitmap_index_t;

// Create a bit index.
static inline mi_bitmap_index_t mi_bitmap_index_create(size_t idx, size_t bitidx) {
  mi_assert_internal(bitidx < MI_BITMAP_FIELD_BITS);
  return (idx*MI_BITMAP_FIELD_BITS) + bitidx;
}

// Get the field index from a bit index.
static inline size_t mi_bitmap_index_field(mi_bitmap_index_t bitmap_idx) {
  return (bitmap_idx / MI_BITMAP_FIELD_BITS);
}

// Get the bit index in a bitmap field
static inline size_t mi_bitmap_index_bit_in_field(mi_bitmap_index_t bitmap_idx) {
  return (bitmap_idx % MI_BITMAP_FIELD_BITS);
}

// Get the full bit index
static inline size_t mi_bitmap_index_bit(mi_bitmap_index_t bitmap_idx) {
  return bitmap_idx;
}


// The bit mask for a given number of blocks at a specified bit index.
static inline uintptr_t mi_bitmap_mask_(size_t count, size_t bitidx) {
  mi_assert_internal(count + bitidx <= MI_BITMAP_FIELD_BITS);
  if (count == MI_BITMAP_FIELD_BITS) return MI_BITMAP_FIELD_FULL;
  return ((((uintptr_t)1 << count) - 1) << bitidx);
}


/* -----------------------------------------------------------
  Use bit scan forward/reverse to quickly find the first zero bit if it is available
----------------------------------------------------------- */
#if defined(_MSC_VER)
#define MI_HAVE_BITSCAN
#include <intrin.h>
#ifndef MI_64
#if MI_INTPTR_SIZE==8
#define MI_64(f) f##64
#else
#define MI_64(f) f
#endif
#endif

static inline size_t mi_bsf(uintptr_t x) {
  if (x==0) return 8*MI_INTPTR_SIZE;
  DWORD idx;
  MI_64(_BitScanForward)(&idx, x);
  return idx;
}
static inline size_t mi_bsr(uintptr_t x) {
  if (x==0) return 8*MI_INTPTR_SIZE;
  DWORD idx;
  MI_64(_BitScanReverse)(&idx, x);
  return idx;
}
#elif defined(__GNUC__) || defined(__clang__)
#include <limits.h> // LONG_MAX
#define MI_HAVE_BITSCAN
#if (INTPTR_MAX == LONG_MAX)
# define MI_L(x)  x##l
#else
# define MI_L(x)  x##ll
#endif
static inline size_t mi_bsf(uintptr_t x) {
  return (x==0 ? 8*MI_INTPTR_SIZE : MI_L(__builtin_ctz)(x));
}
static inline size_t mi_bsr(uintptr_t x) {
  return (x==0 ? 8*MI_INTPTR_SIZE : (8*MI_INTPTR_SIZE - 1) - MI_L(__builtin_clz)(x));
}
#endif

/* -----------------------------------------------------------
  Claim a bit sequence atomically
----------------------------------------------------------- */

// Try to atomically claim a sequence of `count` bits at in `idx`
// in the bitmap field. Returns `true` on success.
static inline bool mi_bitmap_try_claim_field(mi_bitmap_t bitmap, size_t bitmap_fields, const size_t count, mi_bitmap_index_t bitmap_idx) {
  const size_t idx = mi_bitmap_index_field(bitmap_idx);
  const size_t bitidx = mi_bitmap_index_bit_in_field(bitmap_idx);
  const uintptr_t mask = mi_bitmap_mask_(count, bitidx);
  mi_assert_internal(bitmap_fields > idx); UNUSED(bitmap_fields);
  mi_assert_internal(bitidx + count <= MI_BITMAP_FIELD_BITS);

  uintptr_t field = mi_atomic_load_relaxed(&bitmap[idx]);
  if ((field & mask) == 0) { // free?
    if (mi_atomic_cas_strong_acq_rel(&bitmap[idx], &field, (field|mask))) {
      // claimed!
      return true;
    }
  }
  return false;
}


// Try to atomically claim a sequence of `count` bits in a single
// field at `idx` in `bitmap`. Returns `true` on success.
static inline bool mi_bitmap_try_find_claim_field(mi_bitmap_t bitmap, size_t idx, const size_t count, mi_bitmap_index_t* bitmap_idx)
{
  mi_assert_internal(bitmap_idx != NULL);
  _Atomic(uintptr_t)* field = &bitmap[idx];
  uintptr_t map  = mi_atomic_load_relaxed(field);
  if (map==MI_BITMAP_FIELD_FULL) return false; // short cut

  // search for 0-bit sequence of length count
  const uintptr_t mask = mi_bitmap_mask_(count, 0);
  const size_t    bitidx_max = MI_BITMAP_FIELD_BITS - count;

#ifdef MI_HAVE_BITSCAN
  size_t bitidx = mi_bsf(~map);    // quickly find the first zero bit if possible
#else
  size_t bitidx = 0;               // otherwise start at 0
#endif
  uintptr_t m = (mask << bitidx);     // invariant: m == mask shifted by bitidx

  // scan linearly for a free range of zero bits
  while (bitidx <= bitidx_max) {
    if ((map & m) == 0) {  // are the mask bits free at bitidx?
      mi_assert_internal((m >> bitidx) == mask); // no overflow?
      const uintptr_t newmap = map | m;
      mi_assert_internal((newmap^map) >> bitidx == mask);
      if (!mi_atomic_cas_weak_acq_rel(field, &map, newmap)) {  // TODO: use strong cas here?
        // no success, another thread claimed concurrently.. keep going (with updated `map`)
        continue;
      }
      else {
        // success, we claimed the bits!
        *bitmap_idx = mi_bitmap_index_create(idx, bitidx);
        return true;
      }
    }
    else {
      // on to the next bit range
#ifdef MI_HAVE_BITSCAN
      const size_t shift = (count == 1 ? 1 : mi_bsr(map & m) - bitidx + 1);
      mi_assert_internal(shift > 0 && shift <= count);
#else
      const size_t shift = 1;
#endif
      bitidx += shift;
      m <<= shift;
    }
  }
  // no bits found
  return false;
}


// Find `count` bits of 0 and set them to 1 atomically; returns `true` on success.
// For now, `count` can be at most MI_BITMAP_FIELD_BITS and will never span fields.
static inline bool mi_bitmap_try_find_claim(mi_bitmap_t bitmap, size_t bitmap_fields, size_t count, mi_bitmap_index_t* bitmap_idx) {
  for (size_t idx = 0; idx < bitmap_fields; idx++) {
    if (mi_bitmap_try_find_claim_field(bitmap, idx, count, bitmap_idx)) {
      return true;
    }
  }
  return false;
}

// Set `count` bits at `bitmap_idx` to 0 atomically
// Returns `true` if all `count` bits were 1 previously.
static inline bool mi_bitmap_unclaim(mi_bitmap_t bitmap, size_t bitmap_fields, size_t count, mi_bitmap_index_t bitmap_idx) {
  const size_t idx = mi_bitmap_index_field(bitmap_idx);
  const size_t bitidx = mi_bitmap_index_bit_in_field(bitmap_idx);
  const uintptr_t mask = mi_bitmap_mask_(count, bitidx);
  mi_assert_internal(bitmap_fields > idx); UNUSED(bitmap_fields);
  // mi_assert_internal((bitmap[idx] & mask) == mask);
  uintptr_t prev = mi_atomic_and_acq_rel(&bitmap[idx], ~mask);
  return ((prev & mask) == mask);
}


// Set `count` bits at `bitmap_idx` to 1 atomically
// Returns `true` if all `count` bits were 0 previously. `any_zero` is `true` if there was at least one zero bit.
static inline bool mi_bitmap_claim(mi_bitmap_t bitmap, size_t bitmap_fields, size_t count, mi_bitmap_index_t bitmap_idx, bool* any_zero) {
  const size_t idx = mi_bitmap_index_field(bitmap_idx);
  const size_t bitidx = mi_bitmap_index_bit_in_field(bitmap_idx);
  const uintptr_t mask = mi_bitmap_mask_(count, bitidx);
  mi_assert_internal(bitmap_fields > idx); UNUSED(bitmap_fields);
  //mi_assert_internal(any_zero != NULL || (bitmap[idx] & mask) == 0);
  uintptr_t prev = mi_atomic_or_acq_rel(&bitmap[idx], mask);
  if (any_zero != NULL) *any_zero = ((prev & mask) != mask);
  return ((prev & mask) == 0);
}

// Returns `true` if all `count` bits were 1. `any_ones` is `true` if there was at least one bit set to one.
static inline bool mi_bitmap_is_claimedx(mi_bitmap_t bitmap, size_t bitmap_fields, size_t count, mi_bitmap_index_t bitmap_idx, bool* any_ones) {
  const size_t idx = mi_bitmap_index_field(bitmap_idx);
  const size_t bitidx = mi_bitmap_index_bit_in_field(bitmap_idx);
  const uintptr_t mask = mi_bitmap_mask_(count, bitidx);
  mi_assert_internal(bitmap_fields > idx); UNUSED(bitmap_fields);
  uintptr_t field = mi_atomic_load_relaxed(&bitmap[idx]);
  if (any_ones != NULL) *any_ones = ((field & mask) != 0);
  return ((field & mask) == mask);
}

static inline bool mi_bitmap_is_claimed(mi_bitmap_t bitmap, size_t bitmap_fields, size_t count, mi_bitmap_index_t bitmap_idx) {
  return mi_bitmap_is_claimedx(bitmap, bitmap_fields, count, bitmap_idx, NULL);
}

static inline bool mi_bitmap_is_any_claimed(mi_bitmap_t bitmap, size_t bitmap_fields, size_t count, mi_bitmap_index_t bitmap_idx) {
  bool any_ones;
  mi_bitmap_is_claimedx(bitmap, bitmap_fields, count, bitmap_idx, &any_ones);
  return any_ones;
}


#endif
