package net.laserdiamond.ultimatemanhunt.commands.gamerule;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.laserdiamond.ultimatemanhunt.UMGame;
import net.laserdiamond.ultimatemanhunt.event.ForgeServerEvents;
import net.laserdiamond.ultimatemanhunt.network.UMPackets;
import net.laserdiamond.ultimatemanhunt.network.packet.game.HardcoreUpdateS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command used to determine if the Reverse Manhunt game should be hardcore
 * <p>When a Reverse Manhunt game is set to hardcore, any death removes a life from players that are speed runners</p>
 */
public class SetHardcoreCommand {

    private static final int PERMISSION_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                Commands.literal("um_hardcore")
                        .requires(sourceStack -> ForgeServerEvents.permission(sourceStack, PERMISSION_LEVEL))
                        .then(
                            Commands.argument("isHardcore", BoolArgumentType.bool())
                                    .executes(commandContext -> setHardcore(commandContext, BoolArgumentType.getBool(commandContext, "isHardcore")))
                        )
        );
    }

    private static void logHardcoreUpdate(CommandSourceStack source, boolean isHardcore)
    {
        source.sendSuccess(() -> Component.literal("Set Hardcore to: " + isHardcore), true);
    }

    private static void logFailHardcoreUpdate(CommandSourceStack source, boolean isHardcore)
    {
        source.sendFailure(Component.literal(ChatFormatting.RED + "Cannot change game rules when a game has been started. " +
                "Please stop the game and run this command again if you wish to change a game rule. " +
                "\nisHardcore is currently: " + isHardcore));
    }

    private static int setHardcore(CommandContext<CommandSourceStack> commandContext, boolean isHardcore)
    {
        int i = 0;
        if (UMGame.State.hasGameBeenStarted())
        {
            // Game started. Do not change
            logFailHardcoreUpdate(commandContext.getSource(), isHardcore);
            return 0;
        }
        UMGame.setHardcore(isHardcore);
        UMPackets.sendToAllClients(new HardcoreUpdateS2CPacket(isHardcore));
        logHardcoreUpdate(commandContext.getSource(), isHardcore);
        return i;
    }
}
