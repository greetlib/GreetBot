package io.github.greetlib.greetbot.model

import io.github.greetlib.greetbot.AccessPrivilege


class UserData {
    long tokenID
    ArrayList<String> knownHosts = new ArrayList<>()
    ArrayList<String> createdModules = new ArrayList<>()
    HashMap<String, AccessPrivilege> channelAccess = new HashMap<>()
    AccessPrivilege globalAccess = AccessPrivilege.DEFAULT
}
