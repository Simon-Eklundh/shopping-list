package com.simoneklundh.shoppinglist

import android.content.Context
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists shopping lists, their items, and the autofill history in SharedPreferences as JSON.
 * History keeps every name ever added (deduplicated case-insensitively) across all lists, even
 * after the item is removed, so suggestions survive across shopping trips.
 */
class ShoppingRepository(context: Context) {

    private val prefs = context.getSharedPreferences("shopping_list", Context.MODE_PRIVATE)

    init {
        migrateLegacySingleListIfNeeded()
    }

    fun loadLists(): MutableList<ShoppingListMeta> {
        val json = prefs.getString(KEY_LISTS, null) ?: return mutableListOf()
        val array = JSONArray(json)
        return MutableList(array.length()) { i ->
            val obj = array.getJSONObject(i)
            ShoppingListMeta(obj.getString("id"), obj.getString("name"), obj.getLong("createdAt"))
        }
    }

    fun createList(name: String): ShoppingListMeta {
        val list = ShoppingListMeta(UUID.randomUUID().toString(), name, System.currentTimeMillis())
        val lists = loadLists()
        lists.add(list)
        saveLists(lists)
        return list
    }

    fun deleteList(listId: String) {
        val lists = loadLists()
        lists.removeAll { it.id == listId }
        saveLists(lists)
        prefs.edit().remove(itemsKey(listId)).apply()
    }

    fun loadItems(listId: String): MutableList<ShoppingItem> {
        val json = prefs.getString(itemsKey(listId), null) ?: return mutableListOf()
        val array = JSONArray(json)
        return MutableList(array.length()) { i ->
            val obj = array.getJSONObject(i)
            ShoppingItem(obj.getString("name"), obj.getBoolean("checked"))
        }
    }

    fun saveItems(listId: String, items: List<ShoppingItem>) {
        val array = JSONArray()
        for (item in items) {
            array.put(JSONObject().put("name", item.name).put("checked", item.checked))
        }
        prefs.edit().putString(itemsKey(listId), array.toString()).apply()
    }

    fun loadHistory(): MutableList<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return mutableListOf()
        val array = JSONArray(json)
        return MutableList(array.length()) { i -> array.getString(i) }
    }

    fun addToHistory(name: String) {
        val history = loadHistory()
        if (history.none { it.equals(name, ignoreCase = true) }) {
            history.add(name)
            history.sortBy { it.lowercase() }
            prefs.edit().putString(KEY_HISTORY, JSONArray(history).toString()).apply()
        }
    }

    private fun saveLists(lists: List<ShoppingListMeta>) {
        val array = JSONArray()
        for (list in lists) {
            array.put(
                JSONObject()
                    .put("id", list.id)
                    .put("name", list.name)
                    .put("createdAt", list.createdAt)
            )
        }
        prefs.edit().putString(KEY_LISTS, array.toString()).apply()
    }

    private fun itemsKey(listId: String) = "items_$listId"

    /** Wraps items saved by pre-multi-list versions of the app into a single default list. */
    private fun migrateLegacySingleListIfNeeded() {
        if (prefs.contains(KEY_LISTS)) return
        val legacyItemsJson = prefs.getString(KEY_LEGACY_ITEMS, null)
        if (legacyItemsJson == null) {
            saveLists(mutableListOf())
            return
        }
        val list = ShoppingListMeta(UUID.randomUUID().toString(), "Shopping List", System.currentTimeMillis())
        saveLists(mutableListOf(list))
        prefs.edit()
            .putString(itemsKey(list.id), legacyItemsJson)
            .remove(KEY_LEGACY_ITEMS)
            .apply()
    }

    private companion object {
        const val KEY_LISTS = "lists"
        const val KEY_LEGACY_ITEMS = "items"
        const val KEY_HISTORY = "history"
    }
}
