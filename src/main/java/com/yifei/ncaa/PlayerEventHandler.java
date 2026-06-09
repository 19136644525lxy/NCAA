package com.yifei.ncaa;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerEventHandler {
    public static void registerEvents() {
        // Listen for player join server event
        ServerPlayConnectionEvents.JOIN.register(PlayerEventHandler::onPlayerJoin);
        
        // Listen for server tick event
        ServerTickEvents.END_SERVER_TICK.register(PlayerEventHandler::onServerTick);
    }
    
    // Triggered when player joins server
    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        forceSurvivalMode(player);
    }
    
    // Force set player to survival mode
    public static void forceSurvivalMode(ServerPlayerEntity player) {
        if (ConfigManager.config.modEnabled && player.interactionManager.getGameMode() != net.minecraft.world.GameMode.SURVIVAL) {
            player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
            player.sendMessage(net.minecraft.text.Text.literal(ConfigManager.config.message), false);
        }
    }
    
    // Triggered every server tick
    private static void onServerTick(MinecraftServer server) {
        if (ConfigManager.config.modEnabled) {
            // Check all players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Force set to survival mode if not in survival mode
                forceSurvivalMode(player);
            }
        }
    }
}