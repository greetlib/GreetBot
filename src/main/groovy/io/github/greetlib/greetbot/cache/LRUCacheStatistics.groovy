package io.github.greetlib.greetbot.cache

class LRUCacheStatistics {
    int hit
    int miss
    long puts
    long expired
    int totalObjects
    int capacity
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

    @Override
    String toString() {
        return "Hits: ${hit} / " +
               "Misses: ${miss} / " +
               "Ratio: ${try {miss/hit} catch(ArithmeticException ignored){ 0 }} / " +
               "Total Objects: ${totalObjects}/${capacity} / " +
               "Expired: ${expired} / " +
               "Puts: ${puts} / " +
               "Gets: ${hit+miss}"
    }

    void reset() {
        hit = miss = totalObjects = expired = puts = 0
    }
}
