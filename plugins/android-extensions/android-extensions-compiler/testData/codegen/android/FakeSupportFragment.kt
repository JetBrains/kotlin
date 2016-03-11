package android.support.v4.app

import android.view.View
import android.app.Activity

abstract class Fragment {
    open fun getActivity(): Activity = throw Exception("Function getActivity() is not overridden")
    open fun getView(): View = throw Exception("Function getView() is not overridden")
}
