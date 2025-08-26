package io.redspace.ironsspellbooks.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.math.BigInteger;

public class RenderTooltip {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderGuiEvent.Post.class, RenderTooltip::onHudRender);}
    private static void onHudRender(RenderGuiEvent.Post event) {
        int y0=Minecraft.getInstance().getWindow().getGuiScaledHeight()/2;int x0=Minecraft.getInstance().getWindow().getGuiScaledWidth()/2;
        float guiScale=(float) Minecraft.getInstance().getWindow().getGuiScale();float x = 0;float y = 0;
        if (Minecraft.getInstance().player == null) {return;}
        double X=Minecraft.getInstance().player.getX();double Y=Minecraft.getInstance().player.getY();double Z=Minecraft.getInstance().player.getZ();
        float scale = (float)Math.abs(X+Y+Z)+1;float factor;
        VertexConsumer vertexConsumer = event.getGuiGraphics().bufferSource().getBuffer(RenderType.LINE_STRIP);
        int bits = 16;int dimension=2;long number=Math.round(Math.pow(2, bits));long[] p;long px;long py;
        HilbertCurve c = HilbertCurve.bits(bits).dimensions(dimension);
        for (long i = 0; i <number; i++)  {
            p =c.point(BigInteger.valueOf(i));px=p[0];py=p[1];
            factor=i%scale;px=px-128;py=py-128;
            if (x0 + factor* px/guiScale<2*x0&&y0 + factor* py/guiScale<2*y0) {
                x=x0 + factor* px/guiScale;
                y=y0 + factor* py/guiScale;
            }
            vertexConsumer.addVertex(x,y,1).setColor((int) ((i * 0xFF45D9F3BL) & 0xFFFFFFFFL)).setNormal(1, 1, 1);
        }
    }
}
