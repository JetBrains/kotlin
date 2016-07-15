//
// Created by user on 7/14/16.
//

#ifndef GOOGLE_KOTLIN_NAME_RESOLVER_H
#define GOOGLE_KOTLIN_NAME_RESOLVER_H

#include <string>
#include <google/protobuf/descriptor.h>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {
namespace name_resolving {

std::string makeFirstLetterUpper(std::string s);

std::string getFileNameWithoutExtension(std::string fullName);

std::string getKotlinOutputByProtoName(std::string protoName);

std::string protobufTypeToInitValue(FieldDescriptor const * descriptor);

std::string protobufToKotlinField(FieldDescriptor const * descriptor);

std::string protobufToKotlinType(FieldDescriptor const * descriptor);

/**
* Converts one of protobuf wire types to corresponding Kotlin type with proper
* naming, so it could be used as suffix after read/write, resulting in function
* in CodedInputStream/CodedOutputStream.
* Example: protobufToKotlinFunctionSuffix(TYPE_SFIXED32) returns "SFixed32", and
*          in Kotlin runtime exists method
*          CodedInputStream.readSFixed32(fieldNumber: Int)
*/
std::string protobufTypeToKotlinFunctionSuffix(FieldDescriptor::Type type);

} // namespace name_resolving
} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


#endif //GOOGLE_KOTLIN_NAME_RESOLVER_H
