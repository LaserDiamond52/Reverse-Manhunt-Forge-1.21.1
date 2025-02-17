package net.laserdiamond.reversemanhunt.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.laserdiamond.reversemanhunt.RMGameState;
import net.laserdiamond.reversemanhunt.ReverseManhunt;
import net.laserdiamond.reversemanhunt.capability.PlayerHunter;
import net.laserdiamond.reversemanhunt.capability.PlayerHunterCapability;
import net.laserdiamond.reversemanhunt.capability.PlayerSpeedRunnerCapability;
import net.laserdiamond.reversemanhunt.client.game.ClientGameState;
import net.laserdiamond.reversemanhunt.client.game.ClientGameTime;
import net.laserdiamond.reversemanhunt.client.game.ClientHardcore;
import net.laserdiamond.reversemanhunt.client.hunter.ClientHunter;
import net.laserdiamond.reversemanhunt.client.hunter.ClientHunterGracePeriod;
import net.laserdiamond.reversemanhunt.client.hunter.ClientSpeedRunnerDistance;
import net.laserdiamond.reversemanhunt.client.speedrunner.ClientDistanceFromHunter;
import net.laserdiamond.reversemanhunt.client.speedrunner.ClientSpeedRunnerHunterDetection;
import net.laserdiamond.reversemanhunt.client.speedrunner.ClientSpeedRunnerLives;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ReverseManhunt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RMOverlays implements LayeredDraw.Layer {

    private static final ResourceLocation SPEED_RUNNER_EMPTY_HEART_TEXTURE = ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore");
    private static final ResourceLocation SPEED_RUNNER_FULL_HEART_TEXTURE = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation SPEED_RUNNER_FULL_HEART_HARDCORE_TEXTURE = ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full");

    @SubscribeEvent
    public static void registerOverlays(FMLClientSetupEvent event)
    {
        event.enqueueWork(RMOverlays::new);
    }

    private static final Minecraft MINECRAFT = Minecraft.getInstance();

    private RMOverlays()
    {
        LayeredDraw layeredDraw = new LayeredDraw()
                .add(this);

        LayeredDraw mcLayers = MINECRAFT.gui.layers;
        mcLayers.add(layeredDraw, () -> !MINECRAFT.options.hideGui)
                .add(new SpeedRunnerHunterDetectionOverlay());
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, DeltaTracker pDeltaTracker)
    {
        LocalPlayer player = MINECRAFT.player;
        if (player == null)
        {
            return; // Player is null. End method
        }
        if (player.isSpectator())
        {
            return; // Don't display if player is in spectator mode
        }

        int lives = ClientSpeedRunnerLives.getLives();
        boolean isHunter = ClientHunter.isHunter();
        boolean hasStarted = ClientGameState.hasGameBeenStarted();
        long gameTime = ClientGameTime.getGameTime();

        if (!hasStarted)
        {
            return; // Game has not been started. End method
        }

        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (!isHunter) // Is the player a hunter?
        {
            if (lives > 0) // Does the speed runner have lives remaining?
            {
                this.renderSpeedRunnerLives(pGuiGraphics, lives);
            }
        } else // Player is a hunter
        {
            this.renderHunterTracker(pGuiGraphics); // Render tracker
        }
        if (gameTime < ClientHunterGracePeriod.getGracePeriodDuration())
        {
            this.renderHunterGracePeriodProgression(pGuiGraphics, isHunter);
        }

        RenderSystem.disableBlend();

    }

    private void renderSpeedRunnerLives(GuiGraphics guiGraphics, int lives)
    {

        // Position for the speed runner lives is right next to
        int drawX = (guiGraphics.guiWidth() / 2 - 104);
        int drawY = (guiGraphics.guiHeight() - 39);

        for (int i = 0; i < RMGameState.SPEED_RUNNER_LIVES; i++)
        {
            guiGraphics.blitSprite(SPEED_RUNNER_EMPTY_HEART_TEXTURE, drawX, drawY - (i * 10), 9, 9);
            if (i < lives)
            {
                if (ClientHardcore.isHardcore()) // Hardcore?
                {
                    guiGraphics.blitSprite(SPEED_RUNNER_FULL_HEART_HARDCORE_TEXTURE, drawX, drawY - (i * 10), 9, 9);
                } else // Not hardcore
                {
                    guiGraphics.blitSprite(SPEED_RUNNER_FULL_HEART_TEXTURE, drawX, drawY - (i * 10), 9, 9);
                }
            }
        }

    }

    private void renderHunterGracePeriodProgression(GuiGraphics guiGraphics, boolean isHunter)
    {
        int drawX = (guiGraphics.guiWidth() / 2);
        int drawY = (guiGraphics.guiHeight() - 487);

        Component component = getHunterGracePeriodComponent(isHunter);
        guiGraphics.drawCenteredString(MINECRAFT.font, component, drawX, drawY, ChatFormatting.RED.getColor());

    }

    @NotNull
    private Component getHunterGracePeriodComponent(boolean isHunter) {

        double seconds = (double) (ClientHunterGracePeriod.getGracePeriodDuration() - ClientGameTime.getGameTime()) / 20;

        DecimalFormat format = new DecimalFormat("0.00");

        if (isHunter)
        {
            return Component.literal(ChatFormatting.RED + "" + ChatFormatting.BOLD + "You will be released in " + ChatFormatting.YELLOW + format.format(seconds) + ChatFormatting.RED + " seconds");
        } else
        {
            return Component.literal(ChatFormatting.RED + "" + ChatFormatting.BOLD + "Hunters will be released in " + ChatFormatting.YELLOW + format.format(seconds) + ChatFormatting.RED + " seconds");
        }
    }

    private void renderHunterTracker(GuiGraphics guiGraphics)
    {
        int drawX = guiGraphics.guiWidth() / 2;
        int drawY = guiGraphics.guiHeight() - 77;

        boolean areSpeedRunnersPresent = ClientSpeedRunnerDistance.areSpeedRunnersPresent();
//        UUID playerUUID = ClientSpeedRunnerDistance.getPlayerUUID();
        float distance = ClientSpeedRunnerDistance.getDistance();

        DecimalFormat format = new DecimalFormat("0.00");
        if (areSpeedRunnersPresent)
        {
            guiGraphics.drawCenteredString(MINECRAFT.font, Component.literal(ChatFormatting.GREEN + "Nearest Speed Runner is " + ChatFormatting.YELLOW + format.format(distance) + ChatFormatting.GREEN + " blocks away"), drawX, drawY, ChatFormatting.GREEN.getColor());
        } else
        {
            guiGraphics.drawCenteredString(MINECRAFT.font, Component.literal(ChatFormatting.RED + "There are no Speed Runners nearby"), drawX, drawY, ChatFormatting.RED.getColor());
        }

    }

    /**
     * Hunter detection overlay for speed runners
     */
    private static class SpeedRunnerHunterDetectionOverlay implements LayeredDraw.Layer
    {

        @Override
        public void render(GuiGraphics pGuiGraphics, DeltaTracker pDeltaTracker)
        {
            LocalPlayer player = MINECRAFT.player;
            if (player == null)
            {
                return; // Player is null. End method
            }
            if (player.isSpectator())
            {
                return; // Don't display if player is in spectator mode
            }

            boolean isHunter = ClientHunter.isHunter();
            int lives = ClientSpeedRunnerLives.getLives();
            boolean isNearHunter = ClientSpeedRunnerHunterDetection.isNearHunter();
            boolean isRunning = ClientGameState.isGameRunning();
            float distanceFromHunter = ClientDistanceFromHunter.getDistance();

            int width = pGuiGraphics.guiWidth();
            int height = pGuiGraphics.guiHeight();

            RenderSystem.enableBlend();

            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (!isRunning) // Is the game running?
            {
                return; // Hasn't started. End
            }

            if (!isHunter) // Is the player a hunter?
            {
                if (lives > 0) // Does the speed runner have lives remaining?
                {
                    if (isNearHunter)
                    {
                        float red = -(distanceFromHunter / 110F) + 0.9F;

                        RenderSystem.disableDepthTest();
                        RenderSystem.depthMask(false);
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                        pGuiGraphics.setColor(red, 0.1F, 0.1F, 1.0F);
                        pGuiGraphics.blit(GameRenderer.NAUSEA_LOCATION, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
                        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.disableBlend();
                        RenderSystem.depthMask(true);
                        RenderSystem.enableDepthTest();
                    }
                }
            }

            RenderSystem.disableBlend();

        }
    }

}
