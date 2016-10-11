#include <cassert>

#include "Names.h"

#include "City.h"
#include "Sha1.h"

namespace {

void Printable(const uint8_t* data, uint32_t data_length, char* hex) {
  static const char* hex_digits = "0123456789ABCDEF";
  int i = 0;
  for(int i = 0; i < data_length; ++i) {
    *hex++ = hex_digits[(*data >> 4) & 0xf];
    *hex++ = hex_digits[(*data++) & 0xf];
  }
}

} // namespace

extern "C" {

// Make local hash out of arbitrary data.
void MakeLocalHash(const void* data, uint32_t size, LocalHash* hash) {
  *hash = CityHash64(data, size);
}

// Make global hash out of arbitrary data.
void MakeGlobalHash(const void* data, uint32_t size, GlobalHash* hash) {
  SHA1_CTX ctx;
  SHA1Init(&ctx);
  SHA1Update(&ctx, reinterpret_cast<const unsigned char *>(data), size);
  SHA1Final(&hash->bits[0], &ctx);
}

// Make printable C string out of local hash.
void PrintableLocalHash(const LocalHash* hash, char* buffer, uint32_t size) {
  if (size < sizeof(*hash) * 2) {
    assert(false);
    return;
  }
  Printable(reinterpret_cast<const uint8_t*>(&hash), sizeof(*hash), buffer);
}

// Make printable C string out of global hash.
void PrintableGlobalHash(const GlobalHash* hash, char* buffer, uint32_t size) {
  if (size < sizeof(*hash) * 2) {
    assert(false);
    return;
  }
  Printable(hash->bits, sizeof(*hash), buffer);
}

} // extern "C"
