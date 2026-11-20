package android.content
class Context {
    private val prefs = MemoryPrefs()
    fun getSharedPreferences(name: String, mode: Int) = prefs
    fun getString(id: Int, vararg args: Any) = "label:$id"
    companion object { const val MODE_PRIVATE = 0 }
}
class MemoryPrefs {
    val values = mutableMapOf<String, Any>()
    fun getString(key: String, default: String?): String? = values[key] as? String ?: default
    fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    fun contains(key: String) = values.containsKey(key)
    fun edit() = Editor(values)
}
class Editor(private val values: MutableMap<String, Any>) {
    private val pending = mutableMapOf<String, Any>()
    fun putString(key: String, value: String) = apply { pending[key] = value }
    fun putInt(key: String, value: Int) = apply { pending[key] = value }
    private val removed = mutableSetOf<String>()
    fun remove(key: String) = apply { removed.add(key) }
    fun apply() { removed.forEach { values.remove(it) }; values.putAll(pending) }
}
