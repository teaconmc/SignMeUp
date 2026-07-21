package org.teacon.exhibition_portal.client.framework.components;

public record Rectangle(
        float x, float y, float w, float h
) {
    public static final Rectangle EMPTY = new Rectangle(-1, -1, 0, 0);

    public float x0() {
        return x;
    }

    public float y0() {
        return y;
    }

    public float x1() {
        return x + w;
    }

    public float y1() {
        return y + h;
    }

    public float ratio() {
        return w / h;
    }

    public boolean contains(double m, double n) {
        return x <= m && m <= x + w && y <= n && n <= y + h;
    }

    public boolean contains(Pos pos) {
        return contains(pos.x(), pos.y());
    }

    public Pos center() {
        return new Pos(x + w / 2, y + h / 2);
    }
}
