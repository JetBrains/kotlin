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

public:
    FieldDescriptor::Label modifier;
    string simpleName;
    string fieldName;
    string initValue;

    void generateCode(io::Printer *) const;
    FieldGenerator(FieldDescriptor const * descriptor);

};

} // namespace kotlin
} // namspace compiler
} // namespace protobuf
} // namespace google

#endif //PROTOBUF_KOTLIN_FIELD_GENERATOR_H
