package org.teacon.exhibition_portal.client.framework.components;

public record UVSource(
        float u0, float u1, float v0, float v1
) {
    public static final UVSource FULL = new UVSource(0, 1, 0, 1);
}
