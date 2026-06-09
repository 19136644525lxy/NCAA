package com.yifei.ncaa;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class NCAClient implements ClientModInitializer, ModMenuApi {
    @Override
    public void onInitializeClient() {

    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> createConfigScreen(parent);
    }

    private Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.ncaa.title"))
                .setSavingRunnable(ConfigManager::saveConfig);

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.ncaa.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.ncaa.mod_enabled"), ConfigManager.config.modEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.ncaa.mod_enabled.tooltip"))
                .setSaveConsumer(newValue -> ConfigManager.config.modEnabled = newValue)
                .build());

        general.addEntry(entryBuilder
                .startStrField(Text.translatable("config.ncaa.message"), ConfigManager.config.message)
                .setDefaultValue(Text.translatable("config.ncaa.message.default").getString())
                .setTooltip(Text.translatable("config.ncaa.message.tooltip"))
                .setSaveConsumer(newValue -> ConfigManager.config.message = newValue)
                .build());

        return builder.build();
    }
}