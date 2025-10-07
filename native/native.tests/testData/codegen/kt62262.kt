// This test is now compile-only for iOS and tvOS.
// It also makes sense also to invoke overridden constructor from box() and check the sanity of resulting object of class ViewController.
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: targetFamily=OSX
// DISABLE_NATIVE: targetFamily=WATCHOS

import platform.UIKit.UIViewController

class ViewController : UIViewController {
    @OverrideInit
    constructor() : super(nibName = null, bundle = null)
}

fun box() = "OK"
