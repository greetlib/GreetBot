package io.github.greetlib.greetbot.cache

import groovy.transform.PackageScope


class LRUCacheStatistics {
    int hit
    int miss
    long puts
    long expired
    LRUCacheStatistics(){}

    LRUCacheStatistics plus(LRUCacheStatistics stats) {
        [
        hit: stats.hit + this.hit,
        miss: stats.miss + this.miss,
        puts: stats.puts + this.puts,
        expired: stats.expired + this.expired
        ] as LRUCacheStatistics
    }

    static LRUCacheStatistics combine(LRUCacheStatistics... stats) {
        LRUCacheStatistics newStats = new LRUCacheStatistics()
        stats.each {
            newStats.hit += it.hit
            newStats.miss += it.miss
            newStats.puts += it.puts
            newStats.expired += it.expired
        }
        return newStats
    }
}
