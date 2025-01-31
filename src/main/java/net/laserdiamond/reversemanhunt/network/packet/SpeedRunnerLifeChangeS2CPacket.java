package net.laserdiamond.reversemanhunt.network.packet;

import net.laserdiamond.laserutils.network.NetworkPacket;
import net.laserdiamond.reversemanhunt.capability.client.ClientSpeedRunnerLives;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class SpeedRunnerLifeChangeS2CPacket extends NetworkPacket {

    private final int lives;
    private final boolean wasLastKilledByHunter;

    public SpeedRunnerLifeChangeS2CPacket(int lives, boolean wasLastKilledByHunter) {
        this.lives = lives;
        this.wasLastKilledByHunter = wasLastKilledByHunter;
    }

    public SpeedRunnerLifeChangeS2CPacket(FriendlyByteBuf buf)
    {
        this.lives = buf.readInt();
        this.wasLastKilledByHunter = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.lives);
        buf.writeBoolean(this.wasLastKilledByHunter);
    }

    @Override
    public void packetWork(CustomPayloadEvent.Context context) {
        ClientSpeedRunnerLives.setLives(this.lives);
        ClientSpeedRunnerLives.setWasLastKilledByHunter(this.wasLastKilledByHunter);
    }
}
