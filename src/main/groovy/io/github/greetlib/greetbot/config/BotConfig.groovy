package io.github.greetlib.greetbot.config

import groovy.util.logging.Log4j2
import org.yaml.snakeyaml.Yaml


@Log4j2
class BotConfig {
    public class ServerConfig {
        String host;
        int port;
        String channel
        String nick
        String user
        String name
        String pass
        String prefix
        String alias
    }

    public class DatabaseConfig {
        String type
        String host
        int port
        String schema
        String username
        String password
    }

    DatabaseConfig databaseConfig
    HashMap<String, ServerConfig> serverConfigMap = new HashMap<>()

    static BotConfig load(String filename) {
        BotConfig botConfig = new BotConfig()
        Yaml yaml = new Yaml()
        def config = yaml.load(new File(filename).text)
        ArrayList<ServerConfig> c = config['servers'] as ArrayList
        botConfig.databaseConfig = config['database'] as DatabaseConfig
        for(ServerConfig server : c) {
            if(!server.alias) server.alias = server.host
            botConfig.serverConfigMap.put(server.alias, server)
        }
        return botConfig
    }

    public ServerConfig getServerConfig(String host) {
        return serverConfigMap[host]
    }

}
