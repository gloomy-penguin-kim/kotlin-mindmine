package com.kim.minemind.analysis.caching

class LruCache<K, V>(private val capacity: Int) {
    private val map = object : LinkedHashMap<K, V>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            return size > capacity
        }
    }

    @Synchronized fun get(key: K): V? = map[key]
    @Synchronized fun put(key: K, value: V) { map[key] = value }
    @Synchronized fun clear() { map.clear() }
    @Synchronized fun size(): Int = map.size
}
