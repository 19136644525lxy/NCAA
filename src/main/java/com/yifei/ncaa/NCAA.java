package com.yifei.ncaa;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NCAA implements ModInitializer {
	public static final String MOD_ID = "ncaa";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello NCAA world!");
		
		// 初始化配置：首次调用会创建并读盘，文件损坏/读失败会在这里直接暴露
		NcaaConfig.manager();
		
		// Register event listeners
		PlayerEventHandler.registerEvents();
	}
}