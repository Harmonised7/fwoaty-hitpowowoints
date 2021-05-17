package harmonised.fwoaty_hitpowowoints.util;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class Util
{
    public static ResourceLocation getDimensionResLoc(World world )
    {
        return world.func_241828_r().func_230520_a_().getKey( world.getDimensionType() );
    }

    public static ResourceLocation getResLoc( String regKey )
    {
        try
        {
            return new ResourceLocation( regKey );
        }
        catch( Exception e )
        {
            return new ResourceLocation( "" );
        }
    }

    public static ResourceLocation getResLoc( String firstPart, String secondPart )
    {
        try
        {
            return new ResourceLocation( firstPart, secondPart );
        }
        catch( Exception e )
        {
            return null;
        }
    }
}
