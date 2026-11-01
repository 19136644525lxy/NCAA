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
        // 界面只读写内存里的 ConfigManager，点"保存"才落盘；值类型由 ConfigKey 保证
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.ncaa.title"))
                .setSavingRunnable(() -> {
                    try {
                        NcaaConfig.save();
                    } catch (RuntimeException e) {
                        // 配置加载失败时 save 会拒绝写回（避免覆盖用户文件），界面不能因此崩掉
                        NCAA.LOGGER.error("保存 ncaa 配置失败", e);
                    }
                });

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.ncaa.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.ncaa.mod_enabled"), NcaaConfig.modEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.ncaa.mod_enabled.tooltip"))
                .setSaveConsumer(NcaaConfig::setModEnabled)
                .build());

        general.addEntry(entryBuilder
                .startStrField(Text.translatable("config.ncaa.message"), NcaaConfig.message())
                .setDefaultValue(Text.translatable("config.ncaa.message.default").getString())
                .setTooltip(Text.translatable("config.ncaa.message.tooltip"))
                .setSaveConsumer(NcaaConfig::setMessage)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.ncaa.exempt_operators"), NcaaConfig.exemptOperators())
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.ncaa.exempt_operators.tooltip"))
                .setSaveConsumer(NcaaConfig::setExemptOperators)
                .build());

        general.addEntry(entryBuilder
                .startIntSlider(Text.translatable("config.ncaa.grace_seconds"), NcaaConfig.graceSeconds(), 1, 10)
                .setDefaultValue(2)
                .setTooltip(Text.translatable("config.ncaa.grace_seconds.tooltip"))
                .setSaveConsumer(NcaaConfig::setGraceSeconds)
                .build());

        return builder.build();
    }
}
