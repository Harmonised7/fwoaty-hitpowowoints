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
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Renderer
{
    private static final ResourceLocation TEXTURE = Util.getResLoc( Reference.MOD_ID, "textures/gui/hpbar.png" );
    private static final Minecraft mc = Minecraft.getInstance();
    private static int blitOffset = 0;

    @SubscribeEvent
    public void handleRender( RenderWorldLastEvent event )
    {
        PlayerEntity player = mc.player;
        float partialTicks = event.getPartialTicks();
        mc.getTextureManager().bindTexture( TEXTURE );
        World world = mc.world;
        Vector3d cameraCenter = mc.getRenderManager().info.getProjectedView();
        BlockPos originBlockPos = mc.getRenderManager().info.getBlockPos();
        MatrixStack stack = event.getMatrixStack();
        stack.push();
        stack.translate( -cameraCenter.getX() + 0.5, -cameraCenter.getY() + 0.5, -cameraCenter.getZ() + 0.5 );
        stack.rotate(Vector3f.YP.rotationDegrees(180.0F));

        IRenderTypeBuffer.Impl buffer = mc.getRenderTypeBuffers().getBufferSource();

//        blit( matrix4f, -width/2, width/2, -height/2, height/2, 0, 0, 1, 0, 0.5f );

        float w, h = 0.1f;
        double polyDegRange = 30;
        double degOffset = 180 - polyDegRange/2;
        int polyCount = 32;
//        float diameter = w*polyCount;
        double polyDegStep = polyDegRange / polyCount;

        int renderDistance = 128;

        for( LivingEntity livingEntity : world.getLoadedEntitiesWithinAABB( LivingEntity.class, new AxisAlignedBB( originBlockPos.up( renderDistance ).north( renderDistance ).east( renderDistance ), originBlockPos.down( renderDistance ).south( renderDistance ).west( renderDistance ) ) ) )
        {
            if( livingEntity == player )
                continue;
            stack.push();
            Vector3d livingEntityEyePos = livingEntity.getEyePosition( partialTicks );
            //Translate to eyes
            stack.translate( -livingEntityEyePos.getX() + 0.5, livingEntityEyePos.getY() - 0.5, -livingEntityEyePos.getZ() + 0.5 );
            //Rotate to head rotation
            stack.rotate( Vector3f.YN.rotationDegrees( livingEntity.getRotationYawHead() ) );

            //Draw bar
            for( int i = 0; i < polyCount; i++ )
            {
                stack.push();
                stack.rotate( Vector3f.YP.rotationDegrees( (float) ( polyDegStep*i + degOffset ) ) );
                float offset = livingEntity.getWidth() * 1.2f;
                w = (float) ( 2*offset*Math.tan( Math.toRadians( polyDegStep/2 ) ) );
                stack.translate( -w/2f, -h/2f, offset );

                mirrorBlit( stack, 0, w, 0, h, 0, 256, 256, 0, 0, 256, 256 );
                stack.pop();
            }
            stack.pop();
        }

        stack.pop();
        RenderSystem.disableDepthTest();
        buffer.finish();
    }

//    public void blit(MatrixStack matrixStack, int x, int y, int uOffset, int vOffset, int uWidth, int vHeight)
//    {
//        blit(matrixStack, x, y, this.blitOffset, (float)uOffset, (float)vOffset, uWidth, vHeight, 256, 256);
//    }

//    public static void blit(MatrixStack matrixStack, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureHeight, int textureWidth)
//    {
//        innerBlit(matrixStack, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
//    }

    public static void innerBlit(MatrixStack matrixStack, float x1, float x2, float y1, float y2, float blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight)
    {
        innerBlit(matrixStack.getLast().getMatrix(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float)textureWidth, (uOffset + (float)uWidth) / (float)textureWidth, (vOffset + 0.0F) / (float)textureHeight, (vOffset + (float)vHeight) / (float)textureHeight);
    }

    public static void innerBlit(Matrix4f matrix, float x1, float x2, float y1, float y2, float blitOffset, float minU, float maxU, float minV, float maxV)
    {
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(matrix, x1, y2, blitOffset).tex(minU, maxV).endVertex();
        bufferbuilder.pos(matrix, x2, y2, blitOffset).tex(maxU, maxV).endVertex();
        bufferbuilder.pos(matrix, x2, y1, blitOffset).tex(maxU, minV).endVertex();
        bufferbuilder.pos(matrix, x1, y1, blitOffset).tex(minU, minV).endVertex();
        bufferbuilder.finishDrawing();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw(bufferbuilder);
    }

    public static void mirrorBlit( MatrixStack matrixStack, float x1, float x2, float y1, float y2, float blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight )
    {
        mirrorBlit(matrixStack.getLast().getMatrix(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float)textureWidth, (uOffset + (float)uWidth) / (float)textureWidth, (vOffset + 0.0F) / (float)textureHeight, (vOffset + (float)vHeight) / (float)textureHeight );
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
}
