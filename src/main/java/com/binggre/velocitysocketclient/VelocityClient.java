package com.binggre.velocitysocketclient;

import com.binggre.velocitysocketclient.socket.SocketClient;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;

@Getter
public final class VelocityClient extends JavaPlugin {

    @Getter
    private static VelocityClient instance;
    private SocketClient connectClient;
    private JedisPool jedisPool;

    @Override
    public void onEnable() {
        instance = this;
        saveResource("config.yml", false);

        connectSocket();
        connectRedis();
    }

    @Override
    public void onDisable() {
        connectClient.close();
        jedisPool.close();
    }

    private void connectSocket() {
        String ip = getSocketServerIP();
        connectClient = new SocketClient(ip, 1079);
        connectClient.connect();
    }

    private void connectRedis() {
        JedisPoolConfig config = new JedisPoolConfig();
        jedisPool = new JedisPool(config, getSocketServerIP(), 6379);
    }

    public Jedis getResource() {
        return jedisPool.getResource();
    }

    private String getSocketServerIP() {
        File file = new File(getDataFolder(), "config.yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        return yamlConfiguration.getString("server.ip");
    }
}