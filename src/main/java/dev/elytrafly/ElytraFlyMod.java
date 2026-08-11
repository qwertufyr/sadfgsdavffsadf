package dev.elytrafly;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ElytraFlyMod implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("elytrafly");

    private static boolean flyEnabled    = false;
    private static int     glideCooldown = 0;

    // Скорость — меняй SPEED и SPEED_FAST под себя
    private static final double SPEED      = 0.4;   // обычный флай
    private static final double SPEED_FAST = 0.75;  // с зажатым Shift

    private static final double ELYTRA_FRICTION = 0.99;
    private static final double ELYTRA_GRAVITY  = 0.08;
    private static final double ELYTRA_LIFT     = 0.06;

    private static KeyBinding toggleKey;
    private static KeyBinding boostKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytrafly.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.elytrafly"
        ));
        boostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytrafly.boost",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                "category.elytrafly"
        ));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            flyEnabled    = false;
            glideCooldown = 0;
            LOGGER.info("[ElytraFly] Joined — fly ready.");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            flyEnabled = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        LOGGER.info("[ElytraFly] Loaded. V = toggle, Shift = fast (localhost only).");
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (glideCooldown > 0) glideCooldown--;

        if (toggleKey.wasPressed()) {
            if (!hasElytra(client)) {
                msg(client, "§c[ElytraFly] Наденьте элитру в нагрудник.");
                return;
            }
            flyEnabled = !flyEnabled;
            msg(client, flyEnabled
                    ? "§a[ElytraFly] ON  §7(V — выкл, Shift — ускорение)"
                    : "§c[ElytraFly] OFF");
        }

        if (!flyEnabled || client.player == null) return;

        boolean gliding  = client.player.isGliding();
        boolean onGround = client.player.isOnGround();

        // Запустить глайд если в воздухе
        if (!gliding && !onGround && glideCooldown == 0) {
            sendStartGlide(client);
            glideCooldown = 5;
            return;
        }

        if (onGround || !gliding) return;

        applyVelocity(client);
    }

    private void applyVelocity(MinecraftClient client) {
        var player = client.player;
        float yaw   = (float) Math.toRadians(player.getYaw());
        float pitch = (float) Math.toRadians(player.getPitch());

        double lookX = -Math.sin(yaw) * Math.cos(pitch);
        double lookY = -Math.sin(pitch);
        double lookZ =  Math.cos(yaw)  * Math.cos(pitch);

        double speed = boostKey.isPressed() ? SPEED_FAST : SPEED;
        Vec3d  cur   = player.getVelocity();

        // Ванильная физика элитры — Grim симулирует именно это
        double vy = cur.y + (-lookY * ELYTRA_LIFT + 0.02) - ELYTRA_GRAVITY;
        double vx = cur.x * ELYTRA_FRICTION;
        double vz = cur.z * ELYTRA_FRICTION;

        var opts = client.options;

        if (opts.forwardKey.isPressed()) {
            vx += lookX * speed;
            vz += lookZ * speed;
            if (pitch < -0.2f) vy += speed * 0.3;
        }
        if (opts.backKey.isPressed()) {
            vx -= lookX * speed * 0.5;
            vz -= lookZ * speed * 0.5;
        }
        if (opts.leftKey.isPressed()) {
            vx +=  Math.cos(yaw) * speed * 0.5;
            vz +=  Math.sin(yaw) * speed * 0.5;
        }
        if (opts.rightKey.isPressed()) {
            vx += -Math.cos(yaw) * speed * 0.5;
            vz += -Math.sin(yaw) * speed * 0.5;
        }
        if (opts.jumpKey.isPressed())  vy = Math.min(vy + 0.15, 0.8);
        if (opts.sneakKey.isPressed()) vy = Math.max(vy - 0.1,  -1.0);

        vx = clamp(vx, -1.2, 1.2);
        vy = clamp(vy, -1.5, 1.2);
        vz = clamp(vz, -1.2, 1.2);

        player.setVelocity(vx, vy, vz);
        player.fallDistance = 0.0f;
    }

    private void sendStartGlide(MinecraftClient client) {
        ClientPlayNetworkHandler net = client.getNetworkHandler();
        if (net == null || client.player == null) return;
        net.sendPacket(new ClientCommandC2SPacket(
                client.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING
        ));
    }

    private boolean hasElytra(MinecraftClient client) {
        if (client.player == null) return false;
        var chest = client.player.getInventory().getArmorStack(2);
        return !chest.isEmpty() && chest.getItem().toString().contains("elytra");
    }

    private void msg(MinecraftClient client, String text) {
        if (client.player != null)
            client.player.sendMessage(Text.literal(text), true);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
