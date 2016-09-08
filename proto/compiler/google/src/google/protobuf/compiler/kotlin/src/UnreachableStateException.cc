//
// Created by user on 7/20/16.
//

#include "UnreachableStateException.h"
namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

UnreachableStateException::UnreachableStateException(std::string const & what)
    : std::logic_error(what)
{ }

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google

