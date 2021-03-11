// "Create expected class in common module testModule_Common" "true"
// DISABLE-ERRORS
package my

import other.Another
import other.Other

actual class <caret>My : Other<Another>()