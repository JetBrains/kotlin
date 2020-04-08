// "Optimize imports" "false"
// ACTION: Introduce import alias

import p1.SomeAlias<caret>
import p1.AnnAlias

@AnnAlias
val some = SomeAlias()