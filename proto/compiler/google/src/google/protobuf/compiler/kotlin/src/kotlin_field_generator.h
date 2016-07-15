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

    // TODO: refactor from field generator to some static utility namespace

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
