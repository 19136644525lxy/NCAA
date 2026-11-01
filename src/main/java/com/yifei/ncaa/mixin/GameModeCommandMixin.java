package com.yifei.ncaa.mixin;

import java.util.Collection;

import com.mojang.brigadier.context.CommandContext;
import com.yifei.ncaa.GameModePolicy;
import com.yifei.ncaa.NCAA;
import com.yifei.ncaa.NcaaConfig;

import net.minecraft.server.command.GameModeCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 从源头拦住 /gamemode creative、/gamemode adventure。
 *
 * <p>之前只有“切完再拉回”这一层：玩家会先真的进入创造、拿到瞬移/飞行，等下一秒扫描才被拽回来，
 * 观感很生硬。这个注入让受限玩家连命令都执行不成功——直接给一句错误提示。</p>
 *
 * <p>放行的三种情况：</p>
 * <ol>
 *   <li>目标模式不是受限模式（生存、旁观）——旁观放行是刻意设计；</li>
 *   <li>模组被配置关掉了；</li>
 *   <li>所有目标玩家都享有豁免（管理员）。只要有一个不豁免，整条命令都会被拦下，
 *       避免“顺手给别人开创造”这条绕过路径。</li>
 * </ol>
 *
 * <p>注意：这一层是“拦在前面”，PlayerEventHandler 的扫描依然保留——数据包、其他模组、
 * /execute as 等等路径不一定走这条命令。</p>
 */
@Mixin(GameModeCommand.class)
public abstract class GameModeCommandMixin {
	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	private static void ncaa$blockRestrictedGameMode(CommandContext<ServerCommandSource> context,
			Collection<ServerPlayerEntity> targets, GameMode gameMode,
			CallbackInfoReturnable<Integer> cir) {
		if (!NcaaConfig.modEnabled() || !GameModePolicy.isBlockedMode(gameMode)) {
			return;
		}
		ServerPlayerEntity blockedTarget = null;
		for (ServerPlayerEntity target : targets) {
			if (!GameModePolicy.isExempt(target)) {
				blockedTarget = target;
				break;
			}
		}
		if (blockedTarget == null) {
			return;
		}
		NCAA.LOGGER.debug("拦截 /gamemode {}（目标 {} 名玩家，至少一名不受限）", gameMode.getName(), targets.size());
		context.getSource().sendError(GameModePolicy.blockReason(blockedTarget));
		cir.setReturnValue(0);
	}
}