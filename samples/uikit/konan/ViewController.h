//
//  ViewController.h
//  konan
//
//  Created by jetbrains on 30/08/2017.
//  Copyright © 2017 JetBrains. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface ViewController : UIViewController

@property (weak, nonatomic) IBOutlet UILabel *label;
@property (weak, nonatomic) IBOutlet UITextField *textField;
@property (weak, nonatomic) IBOutlet UIButton *button;

- (IBAction)buttonPressed;

@end

