This module contains fake/stub IJ JPS API platform signatures from newer IJ platforms 
(newer compared to the IJ platform Kotlin repository currently depends on).

We need this module to copy-paste necessary IJ JPS API signature definitions into the Kotlin repository. 
Copy-pasting signature definitions gives us an ability to use new JPS API which is not yet available 
in the platform the Kotlin repository currently depends on.

This module output SHOULD NOT be packed into Kotlin JPS plugin artifact.