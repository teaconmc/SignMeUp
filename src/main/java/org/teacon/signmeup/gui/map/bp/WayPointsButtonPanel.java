package org.teacon.signmeup.gui.map.bp;

import cn.ussshenzhou.t88.gui.advanced.THoverSensitiveImageButton;
import cn.ussshenzhou.t88.gui.screen.TScreen;
import cn.ussshenzhou.t88.gui.widegt.TWidget;
import cn.ussshenzhou.t88.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.gui.map.ButtonPanelBase;
import org.teacon.signmeup.network.TeleportToWayPointPacket;

import java.util.*;

/**
 * @author USS_Shenzhou
 */
public class WayPointsButtonPanel extends ButtonPanelBase {
    private static Waypoint[] computeShuffledWaypoints() {
        Waypoint[] waypoints = Waypoint.INSTANCES.values().toArray(Waypoint[]::new);

        int majors = 0;
        for (int i = 0; i < waypoints.length; i++) {
            Waypoint item = waypoints[i];
            if (item.state().major()) {
                waypoints[i] = waypoints[majors];
                waypoints[majors] = item;
                majors++;
            }
        }

        UUID uuid = Minecraft.getInstance().getUser().getProfileId();
        Random random = new Random(uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits());
        for (int i = waypoints.length; i > majors + 1; --i) {
            int k1 = random.nextInt(i - majors) + majors, k2 = i - 1;
            Waypoint t = waypoints[k2];
            waypoints[k2] = waypoints[k1];
            waypoints[k1] = t;
        }

        return waypoints;
    }

    private static final class WaypointButton extends THoverSensitiveImageButton {
        private final Waypoint waypoint;

        public WaypointButton(Waypoint waypoint, Runnable closer) {
            super(
                    Component.literal(waypoint.name()),
                    b -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null && mc.level.dimension() == Level.OVERWORLD) {
                            NetworkHelper.sendToServer(new TeleportToWayPointPacket(waypoint.uuid()));
                        }

                        closer.run();
                    },
                    SignMeUp.id("textures/gui/button_panel_button.png"),
                    SignMeUp.id("textures/gui/button_panel_button_hovered.png")
            );
            this.waypoint = waypoint;

            setPadding(0);
            setTooltip(Tooltip.create(Component.literal(waypoint.description())));
        }
    }

    public WayPointsButtonPanel() {
        super(true);

        for (Waypoint waypoint : computeShuffledWaypoints()) {
            this.buttons.add(new WaypointButton(waypoint, () -> {
                TScreen screen = this.getTopParentScreen();
                if (screen != null) {
                    screen.onClose(false);
                }
            }));
        }
    }

    public void highlight(Set<Waypoint> highlightWaypoints) {
        if (highlightWaypoints.isEmpty()) {
            for (TWidget child : this.buttons.getChildren()) {
                if (child instanceof THoverSensitiveImageButton btn) {
                    btn.getButton().setFocused(false);
                }
            }
            return;
        }

        for (TWidget child : this.buttons.getChildren()) {
            if (!(child instanceof WaypointButton btn)) {
                continue;
            }
            btn.getButton().setFocused(highlightWaypoints.contains(btn.waypoint));
        }
    }

    public Waypoint getHighlightWaypoints() {
        for (TWidget child : this.buttons.getChildren()) {
            if (child instanceof WaypointButton btn) {
                if (btn.getButton().isHovered()) {
                    return btn.waypoint;
                }
            }
        }
        return null;
    }
}
