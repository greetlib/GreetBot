package io.github.greetlib.greetbot.database

import io.github.greetlib.greetbot.GreetBotModule
import io.github.greetlib.greetbot.model.ChannelData
import io.github.greetlib.greetbot.model.UserData


abstract class DatabaseConnection {
    abstract UserData getUserDataByHostname(String hostname)
    abstract UserData getUserDataByEmail(String email)
    abstract void saveModuleInfo(GreetBotModule module)
    abstract void setToken(long tokenID, String tokenHash)
    abstract boolean verifyToken(long tokenID, String token)
    abstract void addKnownHost(UserData userData, String userHost)
    abstract void deleteKnownHost(UserData userData, String userHost)
    abstract long getChannelID(String channelName, String networkAlias)
    abstract void setChannelAccess(UserData userData, String channelName, String networkAlias, short accessLevel)
    abstract void setGlobalAccess(UserData userData, short accessLevel)
    abstract void addUserData(UserData userData, String tokenHash, String email, String networkAlias)
    abstract void addNetwork(String host)
    abstract void addChannel(ChannelData channelData)
    abstract ArrayList<String> getChannelNames(String networkAlias)
    abstract ChannelData getChannelData(String networkAlias, String channelName)
}
