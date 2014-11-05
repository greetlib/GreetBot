package io.github.greetlib.greetbot.util

import groovy.time.TimeDuration
import org.mindrot.jbcrypt.BCrypt

import java.security.SecureRandom


class TokenUtil {
    private static int TOKEN_LENGTH = 15
    private static String charSet = (('A'..'Z')+('a'..'z')+(1..9)).join("")
    private static SecureRandom random = new SecureRandom()
    private static TimeDuration reseedDuration = new TimeDuration(0, 1, 0, 0, 0)
    private static long nextReseed = System.currentTimeMillis() + reseedDuration.toMilliseconds()

    static String generate() {
        if(nextReseed >= System.currentTimeMillis()) random.setSeed(random.generateSeed(32))
        StringBuffer token = new StringBuffer(TOKEN_LENGTH)
        (1..TOKEN_LENGTH).each {
            token << charSet[random.nextInt(charSet.length())]
        }
        return token.toString()
    }

    static String hash(String token) {
        return BCrypt.hashpw(token, BCrypt.gensalt(12, random))
    }

    static boolean check(String token, String hash) {
        return BCrypt.checkpw(token, hash)
    }
}
