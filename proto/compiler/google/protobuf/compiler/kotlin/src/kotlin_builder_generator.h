//
// Created by user on 7/12/16.
//

#ifndef PROTOBUF_KOTLIN_BUILDER_GENERATOR_H
#define PROTOBUF_KOTLIN_BUILDER_GENERATOR_H

#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

}
class BuilderGenerator {
public:
    string simpleName;
    vector <

    BuilderGenerator(Descriptor const * descriptor);
    ~BuilderGenerator();
    void generateCode(io::Printer * printer);
};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


#endif //PROTOBUF_KOTLIN_BUILDER_GENERATOR_H
