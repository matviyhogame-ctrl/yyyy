package ua.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;

public class RenderUtil {
    public static void begin() {
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2f);
        Tessellator.getInstance().getBuffer().begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
    }

    public static void end() {
        Tessellator.getInstance().draw();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void line(Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2,
                            float r, float g, float b, float a) {
        BufferBuilder buf = Tessellator.getInstance().getBuffer();
        buf.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, a).next();
        buf.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, a).next();
    }

    /** Box in camera-relative coordinates. */
    public static void box(Matrix4f m, Box bx, float r, float g, float b, float a) {
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
        line(m, x1, y1, z1, x2, y1, z1, r, g, b, a); line(m, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(m, x2, y1, z2, x1, y1, z2, r, g, b, a); line(m, x1, y1, z2, x1, y1, z1, r, g, b, a);
        line(m, x1, y2, z1, x2, y2, z1, r, g, b, a); line(m, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(m, x2, y2, z2, x1, y2, z2, r, g, b, a); line(m, x1, y2, z2, x1, y2, z1, r, g, b, a);
        line(m, x1, y1, z1, x1, y2, z1, r, g, b, a); line(m, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(m, x2, y1, z2, x2, y2, z2, r, g, b, a); line(m, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }
}
