package gregtech.common.pipelike.optical.net;

import gregtech.api.pipenet.WorldPipeNet;
import gregtech.common.pipelike.optical.OpticalPipeProperties;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class WorldOpticalPipeNet extends WorldPipeNet<OpticalPipeProperties, OpticalPipeNet> {

    private static final String DATA_ID = "gregtech.optical_pipe_net";

    public WorldOpticalPipeNet(String name) {
        super(name);
    }

    @Nonnull
    public static WorldOpticalPipeNet getWorldPipeNet(@Nonnull World world) {
        WorldOpticalPipeNet netWorldData = (WorldOpticalPipeNet) world.loadData(WorldOpticalPipeNet.class, DATA_ID);
        if (netWorldData == null) {
            netWorldData = new WorldOpticalPipeNet(DATA_ID);
            world.setData(DATA_ID, netWorldData);
        }
        netWorldData.setWorldAndInit(world);
        return netWorldData;
    }

    @Override
    protected OpticalPipeNet createNetInstance() {
        return new OpticalPipeNet(this);
    }
}
