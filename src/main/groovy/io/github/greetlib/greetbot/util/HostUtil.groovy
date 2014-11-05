package io.github.greetlib.greetbot.util


class HostUtil {
    static getUserHostMask(String source) {
        return source.substring(source.lastIndexOf("!") + 1)
    }
}
