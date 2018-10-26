//
// Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.
//

import UIKit
import arithmeticParser

class ViewController: UIViewController, UITextViewDelegate, UICollectionViewDataSource {

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        numpad.dataSource = self
        self.input.delegate = self
        inputDidChange()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    @IBOutlet var partialResult: UILabel!
    @IBOutlet var result: UILabel!
    @IBOutlet var input: UITextView!
    @IBOutlet var numpad: UICollectionView!

    private let parser = PartialParser(composer: Calculator(), partialComposer: PartialRenderer())

    @IBAction func numpadButtonPressed(_ sender: UIButton) {
        let title = sender.currentTitle!
        if title == "" {
            return
        }

        if title == "⌫" {
            if !input.text.isEmpty {
                input.text.removeLast()
            }
        } else {
            input.text.append(title)
        }

        inputDidChange()
    }

    func textViewDidChange(_ textView: UITextView) {
        if textView === input {
            inputDidChange()
        }
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.input.endEditing(true)
    }

    private func inputDidChange() {
        let parsed = parser.parseWithPartial(expression: input.text)
        if let resultValue = parsed.expression {
            result.text = "= \(resultValue)"
        } else {
            result.text = ""
        }

        let attributedText = parsed.partialExpression as! NSAttributedString

        if let remainder = parsed.remainder {
            partialResult.attributedText = attributedText +
                NSAttributedString(string: "    ") +
                NSAttributedString(string: remainder, attributes:
                    [.foregroundColor: UIColor.red,
                     .font: UIFont.boldSystemFont(ofSize: partialResult.font.pointSize)])
        } else {
            partialResult.attributedText = attributedText
        }
    }

    private let buttons = [
        "7", "8", "9", "/",
        "4", "5", "6", "*",
        "1", "2", "3", "-",
        ".", "0", "", "+",
        "(", ")", "", "⌫"
    ]

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return buttons.count
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell  = collectionView.dequeueReusableCell(withReuseIdentifier: "buttonCell", for: indexPath)

        (cell.viewWithTag(1) as! UIButton).setTitle(buttons[indexPath.item],for: .normal)

        return cell
    }
}

private func +(left: NSAttributedString, right: NSAttributedString) -> NSAttributedString {
    let result = NSMutableAttributedString(attributedString: left)
    result.append(right)
    return result
}

private extension String {
    func toAttributed() -> NSAttributedString {
        return NSAttributedString(string: self)
    }
}

private class PartialRenderer: NSObject, PartialExpressionComposer {
    func missing() -> Any {
        return "... ".toAttributed()
    }

    func ending(expression: Any) -> Any {
        return "\(formatDouble(expression))... ".toAttributed()
    }

    func plus(left: Any, partialRight: Any) -> Any {
        return compose("+", left, partialRight)
    }

    func minus(left: Any, partialRight: Any) -> Any {
        return compose("-", left, partialRight)
    }

    func mult(left: Any, partialRight: Any) -> Any {
        return compose("*", left, partialRight)
    }

    func div(left: Any, partialRight: Any) -> Any {
        return compose("/", left, partialRight)
    }

    func leftParenthesized(partialExpression: Any) -> Any {
        let suffix = NSAttributedString(string: ")", attributes: [.foregroundColor: UIColor.lightGray])
        return "(".toAttributed() + (partialExpression as! NSAttributedString) + suffix
    }

    private func formatDouble(_ value: Any) -> String {
        let rounded = round(1000 * (value as! Double)) / 1000
        return "\(rounded as NSNumber)"
    }

    private func compose(_ op: String, _ left: Any, _ partialRight: Any) -> Any {
        return "\(formatDouble(left)) \(op) ".toAttributed() + (partialRight as! NSAttributedString)
    }

}
