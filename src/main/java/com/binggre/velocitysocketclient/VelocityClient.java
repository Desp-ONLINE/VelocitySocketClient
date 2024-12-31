package com.binggre.velocitysocketclient;

import com.binggre.velocitysocketclient.socket.SocketClient;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@Getter
public final class VelocityClient extends JavaPlugin {

    @Getter
    private static VelocityClient instance;
    private SocketClient connectClient;

    @Override
    public void onEnable() {
        instance = this;
        saveResource("config.yml", false);
        connectClient = new SocketClient(getSocketServerIP(), 1079);
        connectClient.connect();
    }

    @Override
    public void onDisable() {
        connectClient.close();
    }

    private String getSocketServerIP() {
        File file = new File(getDataFolder(), "config.yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        return yamlConfiguration.getString("server.ip");
    }
}