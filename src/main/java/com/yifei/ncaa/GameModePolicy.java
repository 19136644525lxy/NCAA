package com.yifei.ncaa;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import mcmod.yfcorelib.api.Side;
import mcmod.yfcorelib.api.YFCoreLib;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

/**
 * 「哪些游戏模式算违规、谁豁免、怎么切回去」的判定与执行，集中在这里。
 *
 * <p>规则（可由配置调整）：</p>
 * <ul>
 *   <li>受限模式：创造、冒险。生存与<strong>旁观放行</strong>——旁观常用于观察红石/建筑，不该被拽回来。</li>
 *   <li>管理员豁免：权限等级 ≥ 2 的玩家不受限制（默认开启，可在配置里关掉）。</li>
 *   <li>切回前先提示，等 {@code graceSeconds} 秒再切，不在玩家毫无准备时瞬间拽回去。</li>
 * </ul>
 */
public final class GameModePolicy {
	/** 已经在倒计时中的玩家，避免每轮扫描重复排任务、重复提示 */
	private static final Set<UUID> COUNTING_DOWN = ConcurrentHashMap.newKeySet();

	private GameModePolicy() {
	}

	/** 创造与冒险需要受限；生存、旁观放行 */
	public static boolean isBlockedMode(GameMode mode) {
		return mode == GameMode.CREATIVE || mode == GameMode.ADVENTURE;
	}

	/** 管理员豁免：权限等级 ≥ 2（OP）且配置里开着豁免 */
	public static boolean isExempt(ServerPlayerEntity player) {
		return NcaaConfig.exemptOperators() && player.hasPermissionLevel(2);
	}

	/** 该玩家此刻是否处于需要被处理的违规状态 */
	public static boolean isRestricted(ServerPlayerEntity player) {
		if (!NcaaConfig.modEnabled()) {
			return false;
		}
		if (!isBlockedMode(player.interactionManager.getGameMode())) {
			return false;
		}
		return !isExempt(player);
	}

	/**
	 * 先提示、再延时切回。
	 *
	 * <p>同一个玩家在倒计时期间重复调用会被忽略；倒计时结束时重新判定一次——期间玩家自己切回生存、
	 * 或者变成了管理员，就不再处理。</p>
	 */
	public static void warnThenForce(ServerPlayerEntity player) {
		if (!COUNTING_DOWN.add(player.getUuid())) {
			return;
		}
		int seconds = Math.max(1, NcaaConfig.graceSeconds());
		player.sendMessage(Text.translatable("ncaa.warn.grace", seconds).formatted(Formatting.YELLOW), true);

		YFCoreLib.scheduler(Side.SERVER).delay(seconds * 20, () -> {
			COUNTING_DOWN.remove(player.getUuid());
			if (player.isDisconnected() || !isRestricted(player)) {
				return;
			}
			force(player);
		});
	}

	/** 立刻切回生存并提示 */
	public static void force(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SURVIVAL);
		player.sendMessage(messageText(), false);
	}

	/** 配置了自定义文案就用它，否则用语言文件的默认文案 */
	public static Text messageText() {
		String custom = NcaaConfig.message();
		if (custom == null || custom.isEmpty()) {
			return Text.translatable("config.ncaa.message.default");
		}
		return Text.literal(custom);
	}

	/** 关掉模组时清空倒计时，避免重新开启后把之前的排队任务补刀到玩家头上 */
	public static void clearCountdowns() {
		COUNTING_DOWN.clear();
	}
}
