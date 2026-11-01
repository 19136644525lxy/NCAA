package com.yifei.ncaa;

import mcmod.yfcorelib.api.Side;
import mcmod.yfcorelib.api.YFCoreLib;
import mcmod.yfcorelib.common.server.ServerAccess;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public final class PlayerEventHandler {
	private PlayerEventHandler() {
	}

	public static void registerEvents() {
		// 玩家进服：立刻校正一次，不用等到下一次扫
		ServerPlayConnectionEvents.JOIN.register(PlayerEventHandler::onPlayerJoin);

		// 原来是在 ServerTickEvents.END_SERVER_TICK 里每刻全量遍历玩家（20 次/秒）。
		// 换成库的调度器后是每秒一次，且单个任务抛异常不会影响其他任务。
		YFCoreLib.scheduler(Side.SERVER).repeat(20, PlayerEventHandler::sweepAll);
	}

	private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer joinedTo) {
		forceSurvivalMode(handler.getPlayer());
	}

	/** 把非生存模式的玩家拉回生存，并按配置提示 */
	public static void forceSurvivalMode(ServerPlayerEntity player) {
		if (!NcaaConfig.modEnabled()) {
			return;
		}
		if (player.interactionManager.getGameMode() == GameMode.SURVIVAL) {
			return;
		}
		player.changeGameMode(GameMode.SURVIVAL);
		String message = NcaaConfig.message().isEmpty()
				? Text.translatable("config.ncaa.message.default").getString()
				: NcaaConfig.message();
		player.sendMessage(Text.literal(message), false);
	}

	/**
	 * 每秒一次的全量校正。
	 *
	 * <p>在线玩家列表由 YFCoreLib 的 {@link ServerAccess} 提供——不需要自己订阅
	 * SERVER_STARTING / SERVER_STOPPED 维护 MinecraftServer 引用，也不用手写"服务端还没起来"的判断：
	 * 服务端未运行时它直接返回空列表。</p>
	 */
	private static void sweepAll() {
		for (ServerPlayerEntity player : ServerAccess.players()) {
			forceSurvivalMode(player);
		}
	}
}
