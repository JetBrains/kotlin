package mobi.porquenao.poc.kotlin.ui

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import mobi.porquenao.poc.kotlin.R

class MainActivity : BaseActivity() {
    lateinit var listAdapter: MainAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with (findViewById(R.id.list) as RecyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            listAdapter = MainAdapter()
            adapter = listAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        listAdapter.add()
        (findViewById(R.id.list) as RecyclerView).smoothScrollToPosition(0)
        return true
    }

}
