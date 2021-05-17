package harmonised.fwoaty_hitpowowoints.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import harmonised.fwoaty_hitpowowoints.util.Reference;
import harmonised.fwoaty_hitpowowoints.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Renderer
{
    private static final ResourceLocation HP_BAR = Util.getResLoc( Reference.MOD_ID, "textures/gui/hpbar.png" );
    private static final ResourceLocation HP_BAR_INSIDE = Util.getResLoc( Reference.MOD_ID, "textures/gui/hpbarinside.png" );
    private static final Minecraft mc = Minecraft.getInstance();
    private static int blitOffset = 0;

    @SubscribeEvent
    public void handleRender( RenderWorldLastEvent event )
    {
        PlayerEntity player = mc.player;
        float partialTicks = event.getPartialTicks();
        World world = mc.world;
        Vector3d cameraCenter = mc.getRenderManager().info.getProjectedView();
        BlockPos originBlockPos = mc.getRenderManager().info.getBlockPos();
        MatrixStack stack = event.getMatrixStack();
        stack.push();
        stack.translate( -cameraCenter.getX() + 0.5, -cameraCenter.getY() + 0.5, -cameraCenter.getZ() + 0.5 );
        stack.rotate(Vector3f.XP.rotationDegrees(180.0F));
//        stack.rotate(Vector3f.YP.rotationDegrees(180.0F));

        IRenderTypeBuffer.Impl buffer = mc.getRenderTypeBuffers().getBufferSource();

//        blit( matrix4f, -width/2, width/2, -height/2, height/2, 0, 0, 1, 0, 0.5f );

        float w, h;
        double polyDegRange;
        double degOffset;
        int polyCount = 32;
//        float diameter = w*polyCount;
        double polyDegStep;
        int fullBarWidth = 256;
        float polyWidth = fullBarWidth/(float)polyCount;
        int renderDistance = 128;
        float playerPitch = -25;
        float scale;
        RenderSystem.enableBlend();

        for( LivingEntity livingEntity : world.getLoadedEntitiesWithinAABB( LivingEntity.class, new AxisAlignedBB( originBlockPos.up( renderDistance ).north( renderDistance ).east( renderDistance ), originBlockPos.down( renderDistance ).south( renderDistance ).west( renderDistance ) ) ) )
        {
            boolean isPlayer = false;

            if( livingEntity == player )
                isPlayer = true;

            if( isPlayer )
            {
                scale = 0.3f;
//                continue;
            }
            else
                scale = 1;
            stack.push();
            Vector3d livingEntityEyePos = livingEntity.getEyePosition( partialTicks );
            //Translate to eyes
            stack.translate( livingEntityEyePos.getX() - 0.5, -livingEntityEyePos.getY() + 0.5, -livingEntityEyePos.getZ() + 0.5 );
            //Rotate to head rotation
            float yaw = livingEntity == player ? livingEntity.getYaw( partialTicks) : livingEntity.getRotationYawHead();
            stack.rotate( Vector3f.YP.rotationDegrees( yaw ) );
            stack.rotate( Vector3f.XP.rotationDegrees( livingEntity.getPitch( partialTicks ) + (isPlayer ? playerPitch : 0) ) );
            float maxHp = livingEntity.getMaxHealth();
            float curHp = livingEntity.getHealth();
            float hpRatio = curHp/maxHp;
            float drawnRatio, nextDrawRatio, toDraw = 1;
            polyDegRange = ( Math.max( 20, Math.min( 270, 60 * maxHp * 0.1f ) ) ) * scale;
            degOffset = 180 - polyDegRange/2 - 30;
            polyDegStep = polyDegRange / polyCount;

            mc.getTextureManager().bindTexture( HP_BAR );

            float offset = livingEntity.getWidth() * 1.2f;
            w = (float) ( 2*offset*Math.tan( Math.toRadians( polyDegStep/2 ) ) );
            h = livingEntity.getHeight() * 0.2f * scale;

            //Draw bar outside
            for( int i = 0; i < polyCount; i++ )
            {
                stack.push();
                stack.rotate( Vector3f.YP.rotationDegrees( (float) ( polyDegStep*i + degOffset ) ) );
                stack.translate( -w/2f, -h/2f - 0.1f, offset );
                mirrorBlitColor( stack, 0, w, 0, h, 0, polyWidth, 28, polyWidth*i, 0, 256, 256, 0x777777, 255 );
                stack.pop();
            }

            mc.getTextureManager().bindTexture( HP_BAR_INSIDE );

            //Draw bar inside
            for( int i = 0; i < polyCount; i++ )
            {
                drawnRatio = i / (float) polyCount;
                nextDrawRatio = (i+1) / (float) polyCount;
                if( hpRatio < nextDrawRatio )
                    toDraw = (float) Util.map( hpRatio, drawnRatio, nextDrawRatio, 0, 1 );
                stack.push();
                stack.rotate( Vector3f.YP.rotationDegrees( (float) ( polyDegStep*i + degOffset ) ) );
                stack.translate( -w/2f, -h/2f - 0.1f, offset );
                mirrorBlitColor( stack, 0, w*toDraw, 0, h, 0, polyWidth*toDraw, 28, polyWidth*i, 0, 256, 256, Util.hueToRGB( (float) Util.map( hpRatio, 0, 1, 360, 240 ), 1, 1 ), 200 );
                stack.pop();
                if( toDraw < 1 )
                    break;
            }

            stack.pop();
        }

        stack.pop();
        RenderSystem.disableDepthTest();
        buffer.finish();
    }

    public static void mirrorBlit( MatrixStack matrixStack, float x1, float x2, float y1, float y2, float blitOffset, float uWidth, float vHeight, float uOffset, float vOffset, float textureWidth, float textureHeight )
    {
        mirrorBlit(matrixStack.getLast().getMatrix(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth, (uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight, (vOffset + vHeight) / textureHeight );
    }

    public static void mirrorBlit( Matrix4f matrix, float x1, float x2, float y1, float y2, float blitOffset, float minU, float maxU, float minV, float maxV)
    {
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();

        bufferbuilder.begin( 7, DefaultVertexFormats.POSITION_TEX );
        bufferbuilder.pos( matrix, x1, y2, blitOffset ).tex( minU, maxV ).endVertex();
        bufferbuilder.pos( matrix, x2, y2, blitOffset ).tex( maxU, maxV ).endVertex();
        bufferbuilder.pos( matrix, x2, y1, blitOffset ).tex( maxU, minV ).endVertex();
        bufferbuilder.pos( matrix, x1, y1, blitOffset ).tex( minU, minV ).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);

        bufferbuilder.begin( 7, DefaultVertexFormats.POSITION_TEX );
        bufferbuilder.pos( matrix, x1, y1, blitOffset ).tex( minU, minV ).endVertex();
        bufferbuilder.pos( matrix, x2, y1, blitOffset ).tex( maxU, minV ).endVertex();
        bufferbuilder.pos( matrix, x2, y2, blitOffset ).tex( maxU, maxV ).endVertex();
        bufferbuilder.pos( matrix, x1, y2, blitOffset ).tex( minU, maxV ).endVertex();
        bufferbuilder.finishDrawing();

        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw( bufferbuilder );
    }

    public static void mirrorBlitColor( MatrixStack matrixStack, float x1, float x2, float y1, float y2, float blitOffset, float uWidth, float vHeight, float uOffset, float vOffset, float textureWidth, float textureHeight, int color, int alpha )
    {
        mirrorBlitColor(matrixStack.getLast().getMatrix(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth, (uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight, (vOffset + vHeight) / textureHeight, color, alpha );
    }

    public static void mirrorBlitColor( Matrix4f matrix, float x1, float x2, float y1, float y2, float blitOffset, float minU, float maxU, float minV, float maxV, int color, int alpha )
    {
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();

        int red = color >> 16;
        int green = ( color & 0x00ff00 ) >> 8;
        int blue = color & 0x0000ff;


        bufferbuilder.begin( 7, DefaultVertexFormats.POSITION_COLOR_TEX );
        bufferbuilder.pos( matrix, x1, y2, blitOffset ).color( red, green, blue, alpha ).tex( minU, maxV ).endVertex();
        bufferbuilder.pos( matrix, x2, y2, blitOffset ).color( red, green, blue, alpha ).tex( maxU, maxV ).endVertex();
        bufferbuilder.pos( matrix, x2, y1, blitOffset ).color( red, green, blue, alpha ).tex( maxU, minV ).endVertex();
        bufferbuilder.pos( matrix, x1, y1, blitOffset ).color( red, green, blue, alpha ).tex( minU, minV ).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);

        bufferbuilder.begin( 7, DefaultVertexFormats.POSITION_COLOR_TEX );
        bufferbuilder.pos( matrix, x1, y1, blitOffset ).color( red, green, blue, alpha ).tex( minU, minV ).endVertex();
        bufferbuilder.pos( matrix, x2, y1, blitOffset ).color( red, green, blue, alpha ).tex( maxU, minV ).endVertex();
        bufferbuilder.pos( matrix, x2, y2, blitOffset ).color( red, green, blue, alpha ).tex( maxU, maxV ).endVertex();
        bufferbuilder.pos( matrix, x1, y2, blitOffset ).color( red, green, blue, alpha ).tex( minU, maxV ).endVertex();
        bufferbuilder.finishDrawing();

        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw( bufferbuilder );
    }
}
