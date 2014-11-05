package io.github.greetlib.greetbot


class ScriptBase extends Script {
    @Override
    Object run() {
        return null
    }

    static String shuffle(String text) {
        def x = text.toList()
        Collections.shuffle(x)
        return x.join("")
    }

    static String leet(String text) {
        def r = '@8{)3f6#|jk|mn0pq257uvw%y2'
        text = text.tr(('a'..'z').join(""), r)
        text = text.tr(('A'..'Z').join(""), r)
        return text
    }
}
