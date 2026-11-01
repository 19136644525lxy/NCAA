package com.yifei.ncaa;

import mcmod.yfcorelib.api.Side;
import mcmod.yfcorelib.api.YFCoreLib;
import mcmod.yfcorelib.common.server.ServerAccess;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PlayerEventHandler {
	private PlayerEventHandler() {
	}

	public static void registerEvents() {
		// 玩家进服：先判定一次，违规就给提示并开始倒计时
		ServerPlayConnectionEvents.JOIN.register(PlayerEventHandler::onPlayerJoin);

		// 兜底扫描：每 0.5 秒一次，处理不经 /gamemode 的路径（数据包、其他模组、/execute as 等）。
		// 单个任务抛异常不会影响其他任务；服务端未运行时 ServerAccess.players() 返回空列表。
		YFCoreLib.scheduler(Side.SERVER).repeat(10, PlayerEventHandler::sweepAll);
	}

	private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer joinedTo) {
		check(handler.getPlayer());
	}

	/**
	 * 违规就"先提示、后切回"。
	 *
	 * <p>真正的判定规则（受限模式 / 管理员豁免 / 旁观放行）都在 {@link GameModePolicy} 里，
	 * 这里只负责驱动。</p>
	 */
	public static void check(ServerPlayerEntity player) {
		if (!NcaaConfig.modEnabled()) {
			return;
		}
		if (GameModePolicy.isRestricted(player)) {
			GameModePolicy.warnThenForce(player);
		}
	}

	private static void sweepAll() {
		if (!NcaaConfig.modEnabled()) {
			// 关掉模组时清空倒计时，避免重新开启后把之前的排队任务补刀到玩家头上
			GameModePolicy.clearCountdowns();
			return;
		}
		for (ServerPlayerEntity player : ServerAccess.players()) {
			check(player);
		}
	}
}

