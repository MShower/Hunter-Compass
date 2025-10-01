package mshower.compass;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class HunterCompass implements ModInitializer {
    private static final Map<UUID, UUID> TRACKING = new HashMap<>();
    private static int tickCounter = 0;


    @Override
	public void onInitialize() {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                dispatcher.register(CommandManager.literal("track")
                        .then(CommandManager.argument("target", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    String targetName = StringArgumentType.getString(ctx, "target");

                                    ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);
                                    if (target == null) {
                                        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Can't find target."), false);
                                        return 0;
                                    }

                                    if (player != null) {
                                        TRACKING.put(player.getUuid(), target.getUuid());
                                    }
                                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Now tracking " + target.getName().getString()), false);
                                    return 1;
                                })));
            });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID targetId = TRACKING.get(player.getUuid());
                if (targetId == null) continue;

                ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetId);
                if (target == null) continue;

                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() == Items.COMPASS) {
                    BlockPos targetPos = target.getBlockPos();
                    NbtCompound nbt = stack.getOrCreateNbt();
                    nbt.put("LodestonePos", NbtHelper.fromBlockPos(targetPos));
                    nbt.putString("LodestoneDimension", target.getEntityWorld().getRegistryKey().getValue().toString());
                    nbt.putBoolean("LodestoneTracked", false);
                }
            }
        });
        }
}
