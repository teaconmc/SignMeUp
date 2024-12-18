package org.teacon.signmeup.hud;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.gui.widegt.TPanel;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.util.FlawlessFrames;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLLoader;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.MiniMap;
import org.teacon.signmeup.config.RefreshRate;
import org.teacon.signmeup.hud.compat.IrisAccess;
import org.teacon.signmeup.hud.compat.SodiumAccess;

import static org.lwjgl.opengl.GL40C.*;

/**
 * @author USS_Shenzhou
 */
public class InnerMiniMapPanel extends TPanel {
    public static final ObjectArrayList<SectionRenderDispatcher.RenderSection> VISIBLE_SECTIONS = new ObjectArrayList<>(10000);
    private static int minimapSize, minimapFrameSize, FBO, COLOR, DEPTH;
    private static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f();
    private static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    private static final Quaternionf QUATERNION = new Quaternionf();
    private static final float PI = (float) Math.PI;
    public static boolean rendering = false, renderingTranslucent = false;
    private static boolean shouldDraw = true;

    static {
        calculateMinimapSize();
        initOpenGL();
    }

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    private static void initOpenGL() {
        FBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        COLOR = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, COLOR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, minimapFrameSize, minimapFrameSize, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, COLOR, 0);
        DEPTH = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, DEPTH);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, minimapFrameSize, minimapFrameSize);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, DEPTH);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public InnerMiniMapPanel() {
    }

    private static void calculateMinimapSize() {
        var window = Minecraft.getInstance().getWindow();
        minimapSize = (int) (((int) (window.getHeight() * 0.236 / guiScale())) * guiScale());
        minimapFrameSize = (int) (minimapSize * ConfigHelper.getConfigRead(MiniMap.class).ssaaRatio);
    }

    @Override
    public void layout() {
        calculateMinimapSize();
        glDeleteFramebuffers(FBO);
        glDeleteTextures(COLOR);
        glDeleteRenderbuffers(DEPTH);
        initOpenGL();
        super.layout();
    }

    private int life = 0;

    @Override
    public void tickT() {
        if (ConfigHelper.getConfigRead(MiniMap.class).refreshRate == RefreshRate.TICK) {
            shouldDraw = true;
        } else if (ConfigHelper.getConfigRead(MiniMap.class).refreshRate == RefreshRate.TICK_20) {
            life++;
            if (life == 20) {
                life = 0;
                shouldDraw = true;
            }
        }
        super.tickT();
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
        renderMap(graphics);
    }

    private void renderMap(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        switch (ConfigHelper.getConfigRead(MiniMap.class).refreshRate) {
            case EVERY_FRAME -> shouldDraw = true;
            case ANOTHER_FRAME -> shouldDraw = !shouldDraw;
            default -> {
            }
        }
        if (shouldDraw) {
            drawFbo();
        }
        if (ConfigHelper.getConfigRead(MiniMap.class).refreshRate == RefreshRate.TICK || ConfigHelper.getConfigRead(MiniMap.class).refreshRate == RefreshRate.TICK_20) {
            shouldDraw = false;
        }

        blit(graphics, minecraft);
    }

    private void blit(GuiGraphics graphics, Minecraft minecraft) {
        graphics.pose().pushPose();
        glBindFramebuffer(GL_FRAMEBUFFER, Minecraft.getInstance().getMainRenderTarget().frameBufferId);
        var window = minecraft.getWindow();
        glViewport(0, 0, window.getWidth(), window.getHeight());
        RenderSystem.setShaderTexture(0, COLOR);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float x1 = screenX1(), x2 = screenX2();
        float y1 = screenY1(), y2 = screenY2();
        graphics.pose().scale((float) (1 / window.getGuiScale()), (float) (1 / window.getGuiScale()), 1);
        bufferbuilder.addVertex(graphics.pose().last().pose(), x1, y1, 0).setUv(0, 1);
        bufferbuilder.addVertex(graphics.pose().last().pose(), x1, y2, 0).setUv(0, 0);
        bufferbuilder.addVertex(graphics.pose().last().pose(), x2, y2, 0).setUv(1, 0);
        bufferbuilder.addVertex(graphics.pose().last().pose(), x2, y1, 0).setUv(1, 1);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        graphics.pose().popPose();
    }

    private void drawFbo() {
        var minecraft = Minecraft.getInstance();
        prepare(minecraft);
        var camera = minecraft.gameRenderer.getMainCamera();
        var pos = camera.getPosition();
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        glViewport(0, 0, minimapFrameSize, minimapFrameSize);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        Frustum frustum = new Frustum(MODEL_VIEW_MATRIX, PROJECTION_MATRIX);
        if (SignMeUp.SODIUM_INSTALLED) {
            try {
                SodiumWorldRenderer renderer = SodiumAccess.getWorldRenderer();
                RenderSectionManager renderSectionManager = SodiumAccess.getRenderSectionManager(renderer);

                Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
                RenderSystem.setProjectionMatrix(PROJECTION_MATRIX, RenderSystem.getVertexSorting());
                try {
                    Vec3 pos2 = pos.add(0, 320, 0);
                    frustum.prepare(pos2.x, pos2.y, pos2.z);

                    Viewport viewport = ((ViewportProvider) frustum).sodium$createViewport();
                    ChunkRenderMatrices matrices = new ChunkRenderMatrices(PROJECTION_MATRIX, MODEL_VIEW_MATRIX);

                    if (SignMeUp.IRIS_INSTALLED) {
                        Matrix4fc gbufferModelView = IrisAccess.getGBufferModelView();
                        IrisAccess.setGBufferModelView(MODEL_VIEW_MATRIX);
                        try {
                            render0(camera, pos2, renderer, viewport, minecraft, renderSectionManager, matrices, pos);
                        } finally {
                            IrisAccess.setGBufferModelView(gbufferModelView);
                        }
                    } else {
                        render0(camera, pos2, renderer, viewport, minecraft, renderSectionManager, matrices, pos);
                    }
                } finally {
                    RenderSystem.setProjectionMatrix(projectionMatrix, RenderSystem.getVertexSorting());
                }
            } catch (Throwable e) {
                if (!FMLLoader.isProduction()) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            frustum.prepare(pos.x, pos.y, pos.z);
            minecraft.levelRenderer.sectionOcclusionGraph.addSectionsInFrustum(frustum, VISIBLE_SECTIONS);
            for (RenderType renderType : RenderType.chunkBufferLayers()) {
                if (renderType != RenderType.tripwire()) {
                    renderSectionLayer(renderType, pos.x, pos.y, pos.z);
                }
            }
        }
    }

    private void render0(Camera camera, Vec3 pos2, SodiumWorldRenderer renderer, Viewport viewport, Minecraft minecraft, RenderSectionManager renderSectionManager, ChunkRenderMatrices matrices, Vec3 pos) {
        camera.setPosition(pos2);
        try {
            RenderDevice.enterManagedCode();
            try {
                String shaderPack = SignMeUp.IRIS_INSTALLED ? IrisAccess.getShaderPackName() : null;
                rendering = true;
                try {
                    renderer.setupTerrain(camera, viewport, minecraft.player.isSpectator(), FlawlessFrames.isActive());
                    renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.SOLID, pos2.x, pos2.y, pos2.z);
                    renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.CUTOUT, pos2.x, pos2.y, pos2.z);
                    renderingTranslucent = true;
                    renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.TRANSLUCENT, pos2.x, pos2.y, pos2.z);
                    renderingTranslucent = false;
                } finally {
                    rendering = false;

                    if (shaderPack != null) {
                        IrisAccess.setShaderPackName(shaderPack);
                    }
                }
            } finally {
                RenderDevice.exitManagedCode();
            }
        } finally {
            camera.setPosition(pos);
        }
    }

    private static float guiScale() {
        return (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    protected float screenX1() {
        return x1() * guiScale();
    }

    protected float screenX2() {
        return x2() * guiScale();
    }

    protected float screenY1() {
        return y1() * guiScale();
    }

    protected float screenY2() {
        return y2() * guiScale();
    }

    public static int getMinimapScreenSize() {
        return minimapSize;
    }

    public static int getMinimapSize() {
        return (int) (minimapSize / guiScale());
    }

    protected int x1() {
        return (int) ((Minecraft.getInstance().getWindow().getWidth() * 0.95f - minimapSize) / guiScale());
    }

    protected int x2() {
        return x1() + getMinimapSize();
    }

    protected int y1() {
        return (int) (Minecraft.getInstance().getWindow().getHeight() * 0.05f / guiScale());
    }

    protected int y2() {
        return y1() + getMinimapSize();
    }

    private static void prepare(Minecraft minecraft) {
        var cfgR = ConfigHelper.getConfigRead(MiniMap.class);
        var camera = minecraft.gameRenderer.getMainCamera();
        MODEL_VIEW_MATRIX.identity();
        QUATERNION.identity().rotationYXZ(0, PI / 2, 0);
        if (cfgR.followPlayerRotation) {
            QUATERNION.rotateY(PI + camera.getYRot() * PI / 180);
        }
        MODEL_VIEW_MATRIX.rotation(QUATERNION);
        PROJECTION_MATRIX.identity();
        int mapCoverBlocksHalf = cfgR.getCoverRange() / 2;
        PROJECTION_MATRIX.ortho(-mapCoverBlocksHalf, mapCoverBlocksHalf, -mapCoverBlocksHalf, mapCoverBlocksHalf, -1000, 1000);
        VISIBLE_SECTIONS.clear();
    }


    private void renderSectionLayer(RenderType renderType, double x, double y, double z) {
        var minecraft = Minecraft.getInstance();
        renderType.setupRenderState();
        boolean notTranslucent = renderType != RenderType.translucent();
        ObjectListIterator<SectionRenderDispatcher.RenderSection> objectlistiterator = VISIBLE_SECTIONS.listIterator(notTranslucent ? 0 : VISIBLE_SECTIONS.size());
        ShaderInstance shaderinstance = RenderSystem.getShader();
        //noinspection DataFlowIssue
        shaderinstance.setDefaultUniforms(VertexFormat.Mode.QUADS, MODEL_VIEW_MATRIX, PROJECTION_MATRIX, minecraft.getWindow());
        if (shaderinstance.SCREEN_SIZE != null) {
            shaderinstance.SCREEN_SIZE.set(minimapFrameSize, minimapFrameSize);
        }
        shaderinstance.apply();
        Uniform uniform = shaderinstance.CHUNK_OFFSET;

        while (notTranslucent ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious()) {
            SectionRenderDispatcher.RenderSection renderSection = notTranslucent ? objectlistiterator.next() : objectlistiterator.previous();
            if (!renderSection.getCompiled().isEmpty(renderType)) {
                VertexBuffer vertexbuffer = renderSection.getBuffer(renderType);
                BlockPos blockpos = renderSection.getOrigin();
                if (uniform != null) {
                    uniform.set(
                            (float) ((double) blockpos.getX() - x),
                            (float) ((double) blockpos.getY() - y),
                            (float) ((double) blockpos.getZ() - z)
                    );
                    uniform.upload();
                }

                vertexbuffer.bind();
                vertexbuffer.draw();
            }
        }

        if (uniform != null) {
            uniform.set(0.0F, 0.0F, 0.0F);
        }

        shaderinstance.clear();
        VertexBuffer.unbind();
        renderType.clearRenderState();
    }
}
