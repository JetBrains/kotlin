#pragma once

namespace kotlin {
    void crashHandlerInit() noexcept;
    void writeMinidump() noexcept;
}