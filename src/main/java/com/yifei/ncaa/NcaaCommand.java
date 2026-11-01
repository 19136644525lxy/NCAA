package com.yifei.ncaa;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /ncaa} 管理命令。
 *
 * <p>主要用途是<strong>查 UUID 并直接点一下复制</strong>——管理员名单里填 UUID 比填玩家名更稳妥
 * （玩家改名后 UUID 不变），但手抄 36 位十六进制容易错，所以这里点一下就进剪贴板。</p>
 *
 * <pre>
 * /ncaa uuid             你的 UUID 与名单状态
 * /ncaa uuid &lt;玩家名&gt;     指定玩家（在线优先，离线查玩家缓存）
 * /ncaa list             当前白/黑名单
 * /ncaa reload           重新读配置文件（手改文件后用）
 * </pre>
 */
public final class NcaaCommand {
	private NcaaCommand() {
	}

	public static LiteralArgumentBuilder<ServerCommandSource> builder() {
		return CommandManager.literal("ncaa")
				.requires(source -> source.hasPermissionLevel(2))
				.executes(context -> help(context.getSource()))
				.then(CommandManager.literal("uuid")
						.executes(context -> selfUuid(context.getSource()))
						.then(CommandManager.argument("player", StringArgumentType.word())
								.suggests((context, builder) -> CommandSource.suggestMatching(
										onlineNames(context.getSource()), builder))
								.executes(context -> playerUuid(context.getSource(),
										StringArgumentType.getString(context, "player")))))
				.then(CommandManager.literal("list")
						.executes(context -> list(context.getSource())))
				.then(CommandManager.literal("reload")
						.executes(context -> reload(context.getSource())));
	}

	private static int help(ServerCommandSource source) {
		source.sendFeedback(() -> Text.translatable("ncaa.command.help.uuid").formatted(Formatting.GRAY), false);
		source.sendFeedback(() -> Text.translatable("ncaa.command.help.uuid_player").formatted(Formatting.GRAY), false);
		source.sendFeedback(() -> Text.translatable("ncaa.command.help.list").formatted(Formatting.GRAY), false);
		source.sendFeedback(() -> Text.translatable("ncaa.command.help.reload").formatted(Formatting.GRAY), false);
		return 1;
	}

	private static int selfUuid(ServerCommandSource source) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			source.sendError(Text.translatable("ncaa.command.only_player"));
			return 0;
		}
		report(source, player.getGameProfile().getName(), player.getUuid(), true);
		return 1;
	}

	private static int playerUuid(ServerCommandSource source, String name) {
		ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(name);
		if (online != null) {
			report(source, online.getGameProfile().getName(), online.getUuid(), true);
			return 1;
		}
		Optional<GameProfile> cached = source.getServer().getUserCache().findByName(name);
		if (cached.isEmpty() || cached.get().getId() == null) {
			source.sendError(Text.translatable("ncaa.command.not_found", name));
			return 0;
		}
		GameProfile profile = cached.get();
		report(source, profile.getName(), profile.getId(), false);
		return 1;
	}

	private static void report(ServerCommandSource source, String name, UUID id, boolean online) {
		String uuidText = id.toString();
		source.sendFeedback(() -> Text.translatable(
				online ? "ncaa.command.uuid" : "ncaa.command.uuid_offline",
				Text.literal(name).formatted(Formatting.WHITE),
				copyable(uuidText),
				statusText(name, id)), false);
	}

	/** 点一下就复制、悬停有提示的一段文本 */
	private static Text copyable(String value) {
		return Text.literal(value).styled(style -> style
				.withColor(Formatting.AQUA)
				.withUnderline(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
						Text.translatable("ncaa.command.click_to_copy"))));
	}

	private static Text statusText(String name, UUID id) {
		return switch (GameModePolicy.listStatus(name, id)) {
			case "blacklist" -> Text.translatable("ncaa.command.status.blacklist").formatted(Formatting.RED);
			case "whitelist" -> Text.translatable("ncaa.command.status.whitelist").formatted(Formatting.GREEN);
			default -> Text.translatable("ncaa.command.status.none").formatted(Formatting.GRAY);
		};
	}

	private static int list(ServerCommandSource source) {
		sendList(source, "ncaa.command.list.whitelist", NcaaConfig.adminWhitelist(), Formatting.GREEN);
		sendList(source, "ncaa.command.list.blacklist", NcaaConfig.adminBlacklist(), Formatting.RED);
		return 1;
	}

	private static void sendList(ServerCommandSource source, String headerKey, String raw, Formatting color) {
		MutableText line = Text.translatable(headerKey).formatted(color);
		List<String> entries = split(raw);
		if (entries.isEmpty()) {
			line.append(Text.translatable("ncaa.command.list.empty").formatted(Formatting.GRAY));
		} else {
			for (int i = 0; i < entries.size(); i++) {
				if (i > 0) {
					line.append(Text.literal(", ").formatted(Formatting.GRAY));
				}
				line.append(copyable(entries.get(i)));
			}
		}
		source.sendFeedback(() -> line, false);
	}

	private static int reload(ServerCommandSource source) {
		try {
			NcaaConfig.reload();
			source.sendFeedback(() -> Text.translatable("ncaa.command.reloaded").formatted(Formatting.GREEN), true);
			return 1;
		} catch (RuntimeException error) {
			NCAA.LOGGER.error("重新加载 ncaa 配置失败", error);
			source.sendError(Text.translatable("ncaa.command.reload_failed", String.valueOf(error.getMessage())));
			return 0;
		}
	}

	private static List<String> split(String raw) {
		List<String> entries = new ArrayList<>();
		if (raw == null || raw.isBlank()) {
			return entries;
		}
		for (String entry : raw.split("[,;\\s]+")) {
			if (!entry.isBlank()) {
				entries.add(entry);
			}
		}
		return entries;
	}

	private static List<String> onlineNames(ServerCommandSource source) {
		// getPlayerNames() 返回 String[]，suggestMatching 要 Iterable
		return List.of(source.getServer().getPlayerManager().getPlayerNames());
	}
}
