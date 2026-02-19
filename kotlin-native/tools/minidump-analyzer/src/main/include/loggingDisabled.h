#pragma once

struct LoggingLevelDisabled : std::stringstream {
  template<typename T> std::ostream& operator<<(T&& t) {
    return *this;
  }
};