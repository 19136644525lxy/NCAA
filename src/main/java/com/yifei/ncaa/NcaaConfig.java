package com.yifei.ncaa;

import mcmod.yfcorelib.api.YFCoreLib;
import mcmod.yfcorelib.api.config.ConfigBuilder;
import mcmod.yfcorelib.api.config.ConfigKey;
import mcmod.yfcorelib.api.config.ConfigManager;
import mcmod.yfcorelib.api.config.ConfigSpec;

/**
 * NCAA 的配置定义，底层是 YFCoreLib 的配置内核。
 *
 * <p>文件位置与旧实现一致（{@code config/ncaa.json}），键名也刻意沿用了旧的
 * {@code modEnabled} / {@code message}，所以老玩家的配置文件能直接读出来，
 * 不需要迁移脚本；第一次加载后会多写一个 {@code __version} 字段用于后续版本升级。</p>
 */
public final class NcaaConfig {
	/** 是否启用"禁止切创造模式"的强制逻辑 */
	public static final ConfigKey<Boolean> MOD_ENABLED =
			ConfigKey.of("modEnabled", Boolean.class, true, "是否强制把玩家拉回生存模式");

	/** 被拉回生存时发给玩家的提示；留空则用语言文件里的默认文案 */
	public static final ConfigKey<String> MESSAGE =
			ConfigKey.of("message", String.class, "", "被拉回生存时的提示文字，留空用默认文案");

	public static final ConfigSpec SPEC = ConfigBuilder.create("ncaa")
			.define(MOD_ENABLED)
			.define(MESSAGE)
			.version(1)
			.build();

	private static volatile ConfigManager manager;

	private NcaaConfig() {
	}

	/** 首次调用时创建并读盘；之后返回同一实例 */
	public static synchronized ConfigManager manager() {
		if (manager == null) {
			manager = YFCoreLib.config(SPEC);
		}
		return manager;
	}

	public static boolean modEnabled() {
		return manager().get(MOD_ENABLED);
	}

	public static String message() {
		return manager().get(MESSAGE);
	}

	public static void setModEnabled(boolean value) {
		manager().set(MOD_ENABLED, value);
	}

	public static void setMessage(String value) {
		manager().set(MESSAGE, value == null ? "" : value);
	}

	/** 把内存里的值写回文件（Cloth Config 的"保存"按钮用） */
	public static void save() {
		manager().save();
	}

	/** 从文件重新加载（外部手改了配置想立刻生效时用） */
	public static void reload() {
		manager().reload();
	}
}
