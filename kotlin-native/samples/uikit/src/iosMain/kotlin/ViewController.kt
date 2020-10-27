/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.uikit

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCoder
import platform.Foundation.NSSelectorFromString

@ExportObjCClass
class ViewController : UIViewController {

    @OverrideInit
    constructor() : super(nibName = null, bundle = null)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    @ObjCOutlet
    lateinit var label: UILabel

    @ObjCOutlet
    lateinit var button: UIButton

    var pressed = 0

    @ObjCAction
    fun buttonPressed() {
        label.text = "Hello #${pressed++} from Konan!"
        println("Button pressed")
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        val (width, height) = UIScreen.mainScreen.bounds.useContents {
            this.size.width to this.size.height
        }

        val header = UIView().apply {
            backgroundColor = UIColor.lightGrayColor
            view.addSubview(this)
            translatesAutoresizingMaskIntoConstraints = false
            leadingAnchor.constraintEqualToAnchor(view.leadingAnchor).active = true
            topAnchor.constraintEqualToAnchor(view.topAnchor).active = true
            widthAnchor.constraintEqualToAnchor(view.widthAnchor).active = true
            heightAnchor.constraintEqualToAnchor(view.heightAnchor).active = true
        }

        label = UILabel().apply {
            setFrame(CGRectMake(x = 10.0, y = 10.0, width = width - 100.0, height = 40.0))
            center = CGPointMake(x = width / 2, y = 40.0 )
            textAlignment = NSTextAlignmentCenter
            text = "Press OK"
            header.addSubview(this)
        }

        button = UIButton().apply {
            setFrame(CGRectMake(x = 10.0, y = height - 100.0, width = width - 100.0, height = 40.0))
            center = CGPointMake(x = width / 2, y = height / 2)
            backgroundColor = UIColor.blueColor
            setTitle("OK", forState = UIControlStateNormal)
            font = UIFont.fontWithName(fontName = font.fontName, size = 28.0)!!
            layer.borderWidth = 1.0
            layer.borderColor = UIColor.colorWithRed(0x47 / 255.0, 0x43 / 255.0, 0x70 / 255.0, 1.0).CGColor
            layer.masksToBounds = true
            addTarget(target = this@ViewController, action = NSSelectorFromString("buttonPressed"),
                    forControlEvents = UIControlEventTouchUpInside)
            header.addSubview(this)
        }

    }
}
