package ideacompose

import java.io.File

operator fun File.get(vararg relative: String) = resolve(relative.joinToString(File.separator))