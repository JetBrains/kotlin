// WITH_STDLIB

class HomeFragment {
    @Suppress("TOO_MANY_ARGUMENTS", "DELEGATE_SPECIAL_FUNCTION_MISSING")
    private val categoryNewsListPresenter by moxyPresenter {

    }

    private val groupedNewsListAdapter: GroupedNewsListDelegateAdapter by lazy {
        GroupedNewsListDelegateAdapter(
            categoryNewsListPresenter::onWiFiClick
        )
    }
}

class GroupedNewsListDelegateAdapter(onWiFiClickListener: () -> Unit)


fun moxyPresenter() {

}
