package com.nullreach.prop;

/*
 * NULLREACH Prop Link — чисто клиентский реквизит для видеосъёмки.
 *
 * Этот мод НЕ отправляет никакого сетевого трафика и никак не
 * взаимодействует с сервером. Он добавляет одну клавишу:
 *
 *   G  — открыть меню NULLREACH (лого + кнопка Connect)
 *
 * По кнопке Connect идёт пара секунд фейковой "загрузки", затем
 * появляется сообщение об ошибке подключения к NULLREACH.exe.
 * Всё чисто декоративно, для кадра.
 */

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

public class NullreachPropClient implements ClientModInitializer {

	// с 1.21.9+ категория хоткея — это объект, а не просто строка перевода
	private static final KeyBinding.Category CATEGORY =
			KeyBinding.Category.create(Identifier.of("nullreach-prop", "main"));

	private static KeyBinding openMenuKey;

	@Override
	public void onInitializeClient() {
		ExeLauncher.extractAndLaunch();

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.nullreach-prop.open_menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_G,   // клавиша G — меню
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(MinecraftClient client) {
		while (openMenuKey.wasPressed()) {
			client.setScreen(new ConnectScreen());
		}
	}
}
