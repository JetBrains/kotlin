#include <Foundation/Foundation.h>
#include <stdint.h>

NS_ASSUME_NONNULL_BEGIN

NSString * _Nullable __root____getExceptionMessage__TypesOfArguments__ExportedKotlinPackages_kotlin_Exception__(void * exception);

// _KotlinBridgeable bridge functions for primitive types
void * KotlinBridgeable_Int8_box(int8_t value);
int8_t KotlinBridgeable_Int8_unbox(void * ref);

void * KotlinBridgeable_Int16_box(int16_t value);
int16_t KotlinBridgeable_Int16_unbox(void * ref);

void * KotlinBridgeable_Int32_box(int32_t value);
int32_t KotlinBridgeable_Int32_unbox(void * ref);

void * KotlinBridgeable_Int64_box(int64_t value);
int64_t KotlinBridgeable_Int64_unbox(void * ref);

void * KotlinBridgeable_UInt8_box(uint8_t value);
uint8_t KotlinBridgeable_UInt8_unbox(void * ref);

void * KotlinBridgeable_UInt16_box(uint16_t value);
uint16_t KotlinBridgeable_UInt16_unbox(void * ref);

void * KotlinBridgeable_UInt32_box(uint32_t value);
uint32_t KotlinBridgeable_UInt32_unbox(void * ref);

void * KotlinBridgeable_UInt64_box(uint64_t value);
uint64_t KotlinBridgeable_UInt64_unbox(void * ref);

void * KotlinBridgeable_Bool_box(_Bool value);
_Bool KotlinBridgeable_Bool_unbox(void * ref);

void * KotlinBridgeable_Float_box(float value);
float KotlinBridgeable_Float_unbox(void * ref);

void * KotlinBridgeable_Double_box(double value);
double KotlinBridgeable_Double_unbox(void * ref);

// _KotlinBridgeable bridge functions for String
void * KotlinBridgeable_String_box(NSString * value);
NSString * KotlinBridgeable_String_unbox(void * ref);

// _KotlinBridgeable bridge functions for collection types
void * KotlinBridgeable_Array_box(NSArray * value);
NSArray * KotlinBridgeable_Array_unbox(void * ref);

void * KotlinBridgeable_Set_box(void * value);
void * KotlinBridgeable_Set_unbox(void * ref);

void * KotlinBridgeable_Dictionary_box(void * value);
void * KotlinBridgeable_Dictionary_unbox(void * ref);

// _KotlinBridgeable type-tag dispatch for __createBridgeable
int32_t KotlinBridgeable_getTypeTag(void * ref);
void KotlinBridgeable_disposeRef(void * ref);

NS_ASSUME_NONNULL_END
