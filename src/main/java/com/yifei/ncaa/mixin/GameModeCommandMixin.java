package com.yifei.ncaa.mixin;

import com.yifei.ncaa.ConfigManager;
import com.yifei.ncaa.PlayerEventHandler;
import net.minecraft.server.command.GameModeCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameModeCommand.class)
public class GameModeCommandMixin {
    // This mixin is no longer needed as we're using server tick event to enforce survival mode
}