package io.github.greetlib.greetbot


class AccessPrivilege implements Comparable<AccessPrivilege>{
    short accessLevel

    static AccessPrivilege DEFAULT = [accessLevel: 1]

    int compareTo(AccessPrivilege privilege) {
        return this.accessLevel <=> privilege.accessLevel
    }
}
