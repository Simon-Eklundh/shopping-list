package com.simoneklundh.shoppinglist

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the current list and the autofill history in SharedPreferences as JSON.
 * History keeps every name ever added (deduplicated case-insensitively), even after
 * the item is removed from the list, so suggestions survive across shopping trips.
 */
class ShoppingRepository(context: Context) {

    private val prefs = context.getSharedPreferences("shopping_list", Context.MODE_PRIVATE)

    fun loadItems(): MutableList<ShoppingItem> {
        val json = prefs.getString(KEY_ITEMS, null) ?: return mutableListOf()
        val array = JSONArray(json)
        return MutableList(array.length()) { i ->
            val obj = array.getJSONObject(i)
            ShoppingItem(obj.getString("name"), obj.getBoolean("checked"))
        }
    }

    fun saveItems(items: List<ShoppingItem>) {
        val array = JSONArray()
        for (item in items) {
            array.put(JSONObject().put("name", item.name).put("checked", item.checked))
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
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

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_HISTORY = "history"
    }
}
