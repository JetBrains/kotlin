//
// Created by user on 7/20/16.
//

#ifndef GOOGLE_UNREACHABLESTATEEXCEPTION_H
#define GOOGLE_UNREACHABLESTATEEXCEPTION_H


#include <exception>
#include <stdexcept>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class UnreachableStateException : public std::logic_error {
public:
    UnreachableStateException(std::string const & what);
};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


#endif //GOOGLE_UNREACHABLESTATEEXCEPTION_H
