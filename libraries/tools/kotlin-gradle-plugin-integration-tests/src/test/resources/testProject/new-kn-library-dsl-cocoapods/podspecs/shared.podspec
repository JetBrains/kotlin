Pod::Spec.new do |spec|
    spec.name                     = 'shared'
    spec.vendored_frameworks      = 'shared.xcframework'

    spec.version                  = '5.6.2'
    spec.license                  = 'MIT'
    spec.homepage                 = 'https://github.com/Alpaca/Alpaca'
    spec.source                   = { :git => 'https://github.com/Alpaca/Alpaca.git', :tag => spec.version }
    spec.ios.deployment_target    = '10.0'
    spec.osx.deployment_target    = '10.12'
    spec.watchos.deployment_target = '3.0'
    spec.swift_versions           = ['4', '5']

    # This is raw statement that is appended 'as is' to the podspec
    spec.frameworks = 'CFNetwork'
end
