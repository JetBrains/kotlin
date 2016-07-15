//
// Created by user on 7/12/16.
//

#ifndef PROTOBUF_KOTLIN_FIELD_GENERATOR_H
#define PROTOBUF_KOTLIN_FIELD_GENERATOR_H

#include <vector>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>
#include "kotlin_class_generator.h"

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class ClassGenerator;       // declared in kotlin_class_generator.h

class FieldGenerator {
private:
    FieldDescriptor const * descriptor;
    string protobufToKotlinField() const;
    string protobufToKotlinType () const;

    // TODO: refactor from field generator to some static utility namespace
    /**
    * Converts one of protobuf wire types to corresponding Kotlin type with proper
    * naming, so it could be used as suffix after read/write, resulting in function
    * in CodedInputStream/CodedOutputStream.
    * Example: protobufToKotlinFunctionSuffix(TYPE_SFIXED32) returns "SFixed32", and
    *          in Kotlin runtime exists method
    *          CodedInputStream.readSFixed32(fieldNumber: Int)
    */
    string protobufTypeToKotlinFunctionSuffix(FieldDescriptor::Type type) const;
    string protobufTypeToInitValue(FieldDescriptor const * descriptor) const;

    void generateSetter(io::Printer * printer) const;
    void generateRepeatedMethods(io::Printer * printer, bool isBuilder) const;
public:
    FieldDescriptor::Label modifier;
    ClassGenerator const * enclosingClass;    // class, in which that field is defined
    string simpleName;
    string underlyingType;  // unwrapped type.

    /**
     * Full type of field.
     * fullType = Array<underlyingType> for REPEATED fields
     * fullType = underlyingType?       for OPTIONAL fields
     * fullType = underlyingType        for all other cases
     */
    string fullType;
    string getInitValue() const;
    string getUnderlyingTypeInitValue() const;
    FieldDescriptor::Type  protoType;
    int fieldNumber;

    void generateCode(io::Printer * printer, bool isBuilder) const;
    void generateSerializationCode(io::Printer * printer, bool isRead = false, bool noTag = false) const;
    FieldGenerator(FieldDescriptor const * descriptor, ClassGenerator const * enclosingClass);
    string getKotlinFunctionSuffix() const;

};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google

#endif //PROTOBUF_KOTLIN_FIELD_GENERATOR_H
