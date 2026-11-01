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
 * <p>判定顺序（先命中先返回）：</p>
 * <ol>
 *   <li><strong>黑名单</strong>（{@code adminBlacklist}）：只限制"切换游戏模式"这一件事，
 *       其余权限全部照旧；<strong>黑名单优先于白名单</strong>，同时出现在两个名单里按黑名单处理；</li>
 *   <li><strong>白名单</strong>（{@code adminWhitelist}）：不受任何限制，随便切模式；</li>
 *   <li><strong>未列入任何名单的管理员</strong>：由 {@code exemptOperators} 决定，默认豁免；</li>
 *   <li><strong>普通玩家</strong>：一律限制，只能待在生存/旁观。</li>
 * </ol>
 *
 * <p>名单里一条记录可以写玩家名（不区分大小写），也可以写 UUID 字符串；
 * 多条之间用英文逗号、分号、空格或换行分隔。</p>
 *
 * <p>"受限"指创造与冒险两种模式；生存与<strong>旁观</strong>放行——旁观常用于观察，不该被拽回来。</p>
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

	/** 白名单：名单内玩家完全豁免 */
	public static boolean isWhitelisted(ServerPlayerEntity player) {
		return inList(NcaaConfig.adminWhitelist(), player.getGameProfile().getName(), player.getUuid());
	}

	/** 黑名单：名单内玩家保留其他权限，但禁止切换游戏模式 */
	public static boolean isBlacklisted(ServerPlayerEntity player) {
		return inList(NcaaConfig.adminBlacklist(), player.getGameProfile().getName(), player.getUuid());
	}

	/**
	 * 是否豁免"切换游戏模式"这条限制。
	 *
	 * <p>黑名单 &gt; 白名单 &gt; {@code exemptOperators}（仅对管理员生效）。
	 * 普通玩家即使不在黑名单里也一样受限——此时返回 false 是因为他本来就不是管理员，
	 * 而不是因为他被针对了。</p>
	 */
	public static boolean isExempt(ServerPlayerEntity player) {
		if (isBlacklisted(player)) {
			return false;
		}
		if (isWhitelisted(player)) {
			return true;
		}
		return NcaaConfig.exemptOperators() && player.hasPermissionLevel(2);
	}

	/** 该玩家此刻是否处于需要被处理的状态 */
	public static boolean isRestricted(ServerPlayerEntity player) {
		if (!NcaaConfig.modEnabled()) {
			return false;
		}
		if (!isBlockedMode(player.interactionManager.getGameMode())) {
			return false;
		}
		return !isExempt(player);
	}

	/** 被拦下时给对方的原因。黑名单要说得更清楚——"你只是不能切模式，别的权限没少" */
	public static Text blockReason(ServerPlayerEntity player) {
		return Text.translatable(isBlacklisted(player) ? "ncaa.error.blacklisted" : "ncaa.error.gamemode_blocked");
	}

	/**
	 * 先提示、再延时切回。
	 *
	 * <p>同一个玩家在倒计时期间重复调用会被忽略；倒计时结束时重新判定一次——期间玩家自己切回生存、
	 * 或者被加进了白名单，就不再处理。</p>
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

	/**
	 * 名单匹配：玩家名（忽略大小写）或 UUID 字符串。
	 *
	 * <p>两个身份参数都可为 null（离线玩家可能只有其中之一），为 null 的那一项不参与比较。
	 * 命令层要查一个不在线的玩家时也走这里。</p>
	 */
	public static boolean inList(String rawList, String playerName, UUID playerId) {
		if (rawList == null || rawList.isBlank()) {
			return false;
		}
		String uuid = playerId == null ? null : playerId.toString();
		for (String entry : rawList.split("[,;\\s]+")) {
			if (entry.isBlank()) {
				continue;
			}
			if (playerName != null && entry.equalsIgnoreCase(playerName)) {
				return true;
			}
			if (uuid != null && entry.equalsIgnoreCase(uuid)) {
				return true;
			}
		}
		return false;
	}

	/** 名单里的一条记录是否命中了给定玩家（命令输出用） */
	public static String listStatus(String playerName, UUID playerId) {
		if (inList(NcaaConfig.adminBlacklist(), playerName, playerId)) {
			return "blacklist";
		}
		if (inList(NcaaConfig.adminWhitelist(), playerName, playerId)) {
			return "whitelist";
		}
		return "none";
	}
}
