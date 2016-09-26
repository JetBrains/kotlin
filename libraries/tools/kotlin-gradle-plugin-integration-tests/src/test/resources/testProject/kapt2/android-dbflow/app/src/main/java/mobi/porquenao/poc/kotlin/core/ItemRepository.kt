package mobi.porquenao.poc.kotlin.core

import com.raizlabs.android.dbflow.sql.language.Select

object ItemRepository {

    fun getAll(): MutableList<Item> {
        return Select()
                .from(Item::class.java)
                .where()
                .orderBy(Item_Table.updated_at, false)
                .queryList()
    }

}
