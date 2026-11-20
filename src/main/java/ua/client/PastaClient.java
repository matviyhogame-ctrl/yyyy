package ua.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ua.client.gui.ClickGui;
import ua.client.module.ModuleManager;

public class PastaClient implements ClientModInitializer {
    public static KeyBinding guiKey;
    public static KeyBinding panicKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open ClickGUI", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "PastaClient"));
        panicKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Disable all modules", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_END, "PastaClient"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) client.openScreen(new ClickGui());
            while (panicKey.wasPressed()) ModuleManager.disableAll();
            ModuleManager.tick();
        });
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> Hud.render(matrices));
    }
}
