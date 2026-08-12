package com.nullreach.prop;

/*
 * ConnectScreen — маленькое визуальное меню, открывается по клавише G.
 * Показывает "лого" NULLREACH и кнопку Connect. По нажатию Connect
 * идёт пара секунд дёрганой фейковой загрузки, затем сообщение об
 * ошибке подключения к NULLREACH.exe.
 *
 * Состояние статическое: после появления ошибки оно держится до конца
 * игровой сессии (переоткрытие меню показывает ту же ошибку).
 *
 * Это чисто декоративный экран для съёмки: он ни к чему не подключается,
 * никакого трафика не шлёт.
 */

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

public class ConnectScreen extends Screen {

	private static final int ACCENT = 0xFFFF3B4E;
	private static final int TEXT_DIM = 0xFF8B8F98;
	private static final int TEXT = 0xFFE9E9EC;
	private static final int ERROR_RED = 0xFFFF3B4E;

	private static final Random RANDOM = new Random();

	// состояние: 0 = ожидание, 1 = загрузка, 2 = ошибка соединения
	// СТАТИЧЕСКОЕ — сохраняется между открытиями меню в рамках сессии
	private static int connectState = 0;
	private static long loadStartMs = 0;
	private static final long LOAD_DURATION_MS = 1100;

	// дёрганый прогресс
	private static float shownProgress = 0f;
	private int jitterTick = 0;

	private ButtonWidget connectButton;

	public ConnectScreen() {
		super(Text.literal("NULLREACH"));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int cy = this.height / 2;

		connectButton = ButtonWidget.builder(Text.literal("CONNECT"), b -> startLoading())
				.dimensions(cx - 70, cy + 40, 140, 20)
				.build();
		this.addDrawableChild(connectButton);

		// если уже пробовали подключиться ранее — прячем кнопку загрузки/ошибки
		if (connectState == 2) {
			// показываем кнопку RETRY вместо CONNECT
			connectButton.setMessage(Text.literal("RETRY"));
		} else if (connectState == 1) {
			connectButton.active = false;
			connectButton.visible = false;
		}
	}

	private void startLoading() {
		connectState = 1;
		loadStartMs = System.currentTimeMillis();
		shownProgress = 0f;
		connectButton.active = false;
		connectButton.visible = false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int cx = this.width / 2;
		int cy = this.height / 2;

		// "лого"
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("N//R").formatted(Formatting.BOLD),
				cx, cy - 60, ACCENT);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("NULLREACH").formatted(Formatting.BOLD),
				cx, cy - 44, TEXT);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("prop link // client interface"),
				cx, cy - 30, TEXT_DIM);

		if (connectState == 1) {
			long elapsed = System.currentTimeMillis() - loadStartMs;
			float realProgress = Math.min(1f, elapsed / (float) LOAD_DURATION_MS);

			// дёрганое заполнение
			jitterTick++;
			if (jitterTick % 3 == 0) {
				if (RANDOM.nextInt(100) < 70) {
					float jump = 0.05f + RANDOM.nextFloat() * 0.20f;
					shownProgress = Math.min(realProgress, shownProgress + jump);
				}
			}
			if (shownProgress < realProgress - 0.25f) shownProgress = realProgress - 0.25f;

			int barW = 160;
			int barX = cx - barW / 2;
			int barY = cy + 8;
			context.fill(barX, barY, barX + barW, barY + 4, 0xFF222428);
			context.fill(barX, barY, barX + (int) (barW * shownProgress), barY + 4, ACCENT);

			int pct = (int) (shownProgress * 100);
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("connecting to NULLREACH.exe ... " + pct + "%"),
					cx, cy - 6, TEXT_DIM);

			if (realProgress >= 1f) {
				connectState = 2;
				if (connectButton != null) {
					connectButton.visible = true;
					connectButton.active = true;
					connectButton.setMessage(Text.literal("RETRY"));
				}
			}
		} else if (connectState == 2) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("CONNECTION FAILED").formatted(Formatting.BOLD),
					cx, cy - 4, ERROR_RED);
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("no link to NULLREACH.exe"),
					cx, cy + 10, TEXT_DIM);
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("make sure the program is running, then retry"),
					cx, cy + 22, TEXT_DIM);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
