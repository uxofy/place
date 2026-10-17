package com.example.fastplace;

import com.example.fastplace.mixin.MinecraftAccessor;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;

public class FastPlaceClient implements ClientModInitializer {
    private static boolean enabled = true;
    private static int delay = 0;      // 0..20 тиков
    private static boolean placed = false;

    @Override
    public void onInitializeClient() {
        // /fastplace - вкл/выкл, /fastplace delay <0-20> - задержка
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("fastplace")
                .executes(ctx -> {
                    enabled = !enabled;
                    ctx.getSource().sendFeedback(Component.literal("FastPlace: " + (enabled ? "ВКЛ" : "ВЫКЛ")));
                    return 1;
                })
                .then(ClientCommandManager.literal("delay")
                    .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(0, 20))
                        .executes(ctx -> {
                            delay = IntegerArgumentType.getInteger(ctx, "ticks");
                            ctx.getSource().sendFeedback(Component.literal("FastPlace: задержка " + delay + " тик."));
                            return 1;
                        })))));

        // Запоминаем, что в этом тике ставили блок
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (enabled && level.isClientSide() && player.getItemInHand(hand).getItem() instanceof BlockItem) {
                placed = true;
            }
            return InteractionResult.PASS;
        });

        // В конце тика заменяем ванильные 4 тика на свою задержку
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (placed) {
                placed = false;
                if (enabled) {
                    ((MinecraftAccessor) (Object) mc).setRightClickDelay(delay);
                }
            }
        });
    }
}
