//
// Created by user on 7/12/16.
//

#ifndef PROTOBUF_KOTLIN_FIELD_GENERATOR_H
#define PROTOBUF_KOTLIN_FIELD_GENERATOR_H

#include <vector>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class FieldGenerator {
private:
    FieldDescriptor const * descriptor;
    string protobufToKotlinField() const;
    string protobufToKotlinType () const;
    string getInitValue() const;

    /**
    * Converts one of protobuf wire types to corresponding Kotlin type with proper
    * naming, so it could be used as suffix after read/write, resulting in function
    * in CodedInputStream/CodedOutputStream.
    * Example: protobufToKotlinFunctionSuffix(TYPE_SFIXED32) returns "SFixed32", and
    *          in Kotlin runtime exists method
    *          CodedInputStream.readSFixed32(fieldNumber: Int)
    */
    string protobufTypeToKotlinFunctionSuffix(FieldDescriptor::Type type) const;
    void generateSetter(io::Printer * printer, string builderName) const;
    void generateRepeatedMethods(io::Printer * printer, bool isBuilder, string builderName) const;
public:
    FieldDescriptor::Label modifier;
    string simpleName;
    string underlyingType;  // unwrapped type.

    /**
     * Full type of field.
     * fullType = Array<underlyingType> for REPEATED fields
     * fullType = underlyingType?       for OPTIONAL fields
     * fullType = underlyingType        for all other cases
     */
    string fullType;

    string initValue;
    void generateCode(io::Printer * printer, bool isBuilder, string className) const;
    void generateSerializationCode(io::Printer * printer, bool isRead = false, bool noTag = false) const;
    FieldGenerator(FieldDescriptor const * descriptor);

};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google

#endif //PROTOBUF_KOTLIN_FIELD_GENERATOR_H
