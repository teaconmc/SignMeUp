package org.teacon.signmeup.gui.map;

import cn.ussshenzhou.t88.gui.util.HorizontalAlignment;
import cn.ussshenzhou.t88.gui.widegt.TImage;
import cn.ussshenzhou.t88.gui.widegt.TLabel;
import cn.ussshenzhou.t88.gui.widegt.TPanel;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Vector2i;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.network.TeleportToWayPointPacket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author USS_Shenzhou
 */
public class WayPointsPanel extends TPanel {
    private static final int DOT_SIZE = 18, MERGE_RANGE_2 = (int) (DOT_SIZE * DOT_SIZE * 0.75 * 0.75);

    private final Map<Waypoint, VisualWaypoint> waypoints = new HashMap<>();

    public Set<Waypoint> getHoveredWaypoints(double mouseX, double mouseY) {
        for (VisualWaypoint dot : waypoints.values()) {
            if (dot.isInRange(mouseX, mouseY)) {
                return dot.getWaypoints();
            }
        }

        return Set.of();
    }

    public Vector2i getWaypointCuiPos(Waypoint waypoint) {
        VisualWaypoint dot = waypoints.get(waypoint);
        if (dot != null) {
            return new Vector2i(dot.getXT() + dot.getWidth() / 2, dot.getYT() + dot.getHeight() / 2);
        }

        return null;
    }

    public void update(MapPanel.InnerMapPanel map) {
        for (VisualWaypoint dot : waypoints.values()) {
            remove(dot);
        }
        waypoints.clear();

        Set<Waypoint> checked = new HashSet<>();
        for (Waypoint left : Waypoint.INSTANCES.values()) {
            if (checked.contains(left)) {
                continue;
            }

            Set<Waypoint> neared = new HashSet<>();
            for (Waypoint right : Waypoint.INSTANCES.values()) {
                if (left != right && !checked.contains(right) && distance2(left, right, map) <= MERGE_RANGE_2) {
                    neared.add(right);
                    checked.add(right);
                }
            }
            if (neared.isEmpty()) {
                SingleVisualWaypoint value = new SingleVisualWaypoint(map, left);
                waypoints.put(left, value);
                add(value);
            } else {
                neared.add(left);
                MultiVisualWaypoint value = new MultiVisualWaypoint(map, neared);
                for (Waypoint waypoint : neared) {
                    waypoints.put(waypoint, value);
                }
                add(value);
            }

            checked.add(left);
        }
    }

    private int distance2(Waypoint left, Waypoint right, MapPanel.InnerMapPanel map) {
        return (int) map.worldToGui(right.pos().x, right.pos().z).distanceSquared(map.worldToGui(left.pos().x, left.pos().z));
    }

    private static abstract class VisualWaypoint extends TImage {
        public VisualWaypoint(ResourceLocation imageLocation) {
            super(imageLocation);
        }

        protected abstract Set<Waypoint> getWaypoints();
    }

    private static final class SingleVisualWaypoint extends VisualWaypoint {
        private static final ResourceLocation IMAGE = SignMeUp.id("textures/gui/waypoint.png");

        private final Set<Waypoint> waypoints;
        private final Waypoint waypoint;

        public SingleVisualWaypoint(MapPanel.InnerMapPanel map, Waypoint waypoint) {
            super(IMAGE);

            this.waypoint = waypoint;
            this.waypoints = Set.of(waypoint);

            setTooltip(Tooltip.create(Component.translatable("gui.sign_up.map.teleport", waypoint.name())));

            Vector2i pos = map.worldToGui(waypoint.pos().x(), waypoint.pos().z());
            setAbsBounds(pos.x - DOT_SIZE / 2, pos.y - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE);
        }

        @Override
        public Set<Waypoint> getWaypoints() {
            return waypoints;
        }

        private long lastClickedTime = 0;

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (this.isInRange(pMouseX, pMouseY)) {
                long time = System.currentTimeMillis();
                if (time - lastClickedTime <= 200) {
                    lastClickedTime = 0;
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null && mc.level.dimension() == Level.OVERWORLD) {
                        NetworkHelper.sendToServer(new TeleportToWayPointPacket(waypoint.uuid()));
                    }

                    getTopParentScreenOptional().ifPresent(tScreen -> tScreen.onClose(false));
                    return true;
                } else {
                    lastClickedTime = time;
                }
            }

            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }
    }

    public static final class MultiVisualWaypoint extends VisualWaypoint {
        private static final ResourceLocation IMAGE = SignMeUp.id("textures/gui/waypoints.png");

        private final Set<Waypoint> waypoints;

        public MultiVisualWaypoint(MapPanel.InnerMapPanel map, Set<Waypoint> waypoints) {
            super(IMAGE);
            this.waypoints = waypoints;

            int count = waypoints.size();
            double x = 0D, z = 0D;
            for (Waypoint waypoint : waypoints) {
                x += waypoint.pos().x;
                z += waypoint.pos().z;
            }
            Vector2i pos = map.worldToGui(x / count, z / count);
            pos.sub(DOT_SIZE / 2, DOT_SIZE / 2);

            TLabel number = new TLabel();
            number.setAbsBounds(pos.x, pos.y, DOT_SIZE, DOT_SIZE);
            number.setHorizontalAlignment(HorizontalAlignment.CENTER);
            number.setFontSize(TLabel.STD_FONT_SIZE * 0.75f);
            number.setText(Component.literal(String.valueOf(count)));
            add(number);

            setAbsBounds(pos.x, pos.y, DOT_SIZE, DOT_SIZE);
            setTooltip(Tooltip.create(Component.literal(waypoints.stream().map(Waypoint::name).collect(Collectors.joining("\n")))));
        }

        @Override
        public Set<Waypoint> getWaypoints() {
            return waypoints;
        }
    }
}
