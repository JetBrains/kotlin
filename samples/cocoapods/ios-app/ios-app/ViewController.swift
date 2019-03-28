import UIKit
import kotlin_library

class ViewController: UIViewController {

    @IBOutlet weak var goButton: UIButton!
    @IBOutlet weak var urlField: UITextField!
    @IBOutlet weak var contentTextView: UITextView!
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.        
    }

    @IBAction func onGoTouch(_ sender: Any) {
        let url = urlField.text!
        KotlinLibKt.getAndShow(url: url, contentView: contentTextView)
    }
}

