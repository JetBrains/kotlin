// "Add annotation target" "true"
@Target
annotation class SetAnn

class Set(<caret>@set:SetAnn var foo: String)