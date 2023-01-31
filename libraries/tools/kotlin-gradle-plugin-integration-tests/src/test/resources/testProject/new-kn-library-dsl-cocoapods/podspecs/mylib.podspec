Pod::Spec.new do |spec|
    spec.name                     = 'mylib'
    spec.version                  = '0.1'
    spec.vendored_library         = 'libmylib.a'

    spec.description              = <<-DESC
                                      Computes the meaning of life.
                                      Features:
                                      1. Is self aware
                                      ...
                                      42. Likes candies
                                    DESC
    spec.static_framework         = true
    spec.requires_arc             = 'true'
    spec.authors                  = 'Tony O\'Connor'
end
