package org.teacon.exhibition_portal.client.framework.components;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.teacon.exhibition_portal.client.framework.binding.LayoutResource;

public record TextureMetadata(Identifier identifier, AbstractTexture texture, int width, int height,
                              float aspectRatio) {
    public static LayoutResource<TextureMetadata> of(Identifier identifier) {
        return LayoutResource.of(identifier, (id) -> {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
            GpuTexture gpuTexture = texture.getTexture();

            int width = gpuTexture.getWidth(0), height = gpuTexture.getHeight(0);
            return new TextureMetadata(id, texture, width, height, width / (float) height);
        });
    }

    public GpuTexture gpuTexture() {
        return texture.getTexture();
    }

    public GpuSampler sampler() {
        return texture.getSampler();
    }

    public GpuTextureView textureView() {
        return texture.getTextureView();
    }
}
