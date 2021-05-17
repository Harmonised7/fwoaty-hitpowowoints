package harmonised.fwoaty_hitpowowoints.client;

import net.minecraftforge.common.MinecraftForge;

public class ClientHandler
{
    public static void init()
    {
        MinecraftForge.EVENT_BUS.register( new Renderer() );
    }
}
