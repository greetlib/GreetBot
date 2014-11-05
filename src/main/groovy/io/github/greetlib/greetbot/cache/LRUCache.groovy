package io.github.greetlib.greetbot.cache

import groovy.time.TimeDuration

class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final HashMap<K, Long> expiryMap = new HashMap<>()
    final int capacity
    TimeDuration expiryTime
    final LRUCacheStatistics stats = new LRUCacheStatistics()

    LRUCache(TimeDuration expiryTime = new TimeDuration(0, 30, 0, 0), int capacity = 100){
        super(capacity, 1.1f, true)
        this.capacity = capacity
        this.expiryTime = expiryTime
    }

    @Override
    V get(Object key) {
        if(isExpired(key)) {
            stats.miss++
            return null
        }
        V obj = (V)super.get(key)
        if(obj != null) stats.hit++
        else stats.miss++
        return obj
    }

    @Override
    V getOrDefault(Object key, Object defaultValue) {
        if(isExpired(key)) return null
        super.get(key)
    }

    @Override
    V put(K key, V value) {
        expiryMap.put(key, System.currentTimeMillis() + expiryTime.toMilliseconds())
        stats.puts++
        super.put(key, value)
    }

    boolean isExpired(Object key) {
        Long expireTime = expiryMap.get(key)
        if(!expireTime) {
            return true
        }
        else if(System.currentTimeMillis() > expireTime) {
            expiryMap.remove(key)
            stats.expired++
            return true
        }
        return !containsKey(key)
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > capacity
    }
}
