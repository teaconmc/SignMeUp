package org.teacon.signmeup.gui.map.bp;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.gui.advanced.THoverSensitiveImageButton;
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
import org.teacon.signmeup.config.Waypoints;
import org.teacon.signmeup.gui.map.ButtonPanelBase;
import org.teacon.signmeup.network.TeleportToWayPointPacket;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author USS_Shenzhou
 */
public class WayPointsButtonPanel extends ButtonPanelBase {
    private static final class THoverSensitiveImageButtonImpl extends THoverSensitiveImageButton {
        private final String waypointName;

        public THoverSensitiveImageButtonImpl(String waypointName, Button.OnPress onPress, @Nullable ResourceLocation backgroundImageLocation, @Nullable ResourceLocation backgroundImageLocationHovered) {
            super(
                    Component.literal(waypointName.startsWith("#") ? waypointName.substring(1) : waypointName),
                    onPress, backgroundImageLocation, backgroundImageLocationHovered
            );
            this.waypointName = waypointName;
        }
    }


    public WayPointsButtonPanel() {
        super(true);
        Waypoints.WayPoint[] waypoints = ConfigHelper.getConfigRead(Waypoints.class).waypoints.toArray(Waypoints.WayPoint[]::new);

        int staticWP = 0;
        for (int i = 0; i < waypoints.length; i++) {
            Waypoints.WayPoint item = waypoints[i];
            if (item.name.startsWith("#")) {
                waypoints[i] = waypoints[staticWP];
                waypoints[staticWP] = item;
                staticWP++;
            }
        }

        UUID uuid = Minecraft.getInstance().getUser().getProfileId();
        Random random = new Random(uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits());
        for (int i = waypoints.length; i > staticWP + 1; --i) {
            int k1 = random.nextInt(i - staticWP) + staticWP, k2 = i - 1;
            Waypoints.WayPoint t = waypoints[k2];
            waypoints[k2] = waypoints[k1];
            waypoints[k1] = t;
        }

        for (Waypoints.WayPoint wayPoint : waypoints) {
            var button = new THoverSensitiveImageButtonImpl(
                    wayPoint.name,
                    b -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null && mc.level.dimension() == Level.OVERWORLD) {
                            NetworkHelper.sendToServer(new TeleportToWayPointPacket(wayPoint.name));
                        }

                        getTopParentScreenOptional().ifPresent(tScreen -> tScreen.onClose(false));
                    },
                    SignMeUp.id("textures/gui/button_panel_button.png"),
                    SignMeUp.id("textures/gui/button_panel_button_hovered.png")
            );
            button.setPadding(0);
            button.setTooltip(Tooltip.create(Component.literal(wayPoint.description)));
            this.buttons.add(button);
        }
    }

    public void highlight(List<Waypoints.WayPoint> highlightWaypoints) {
        if (highlightWaypoints.isEmpty()) {
            for (TWidget child : this.buttons.getChildren()) {
                if (child instanceof THoverSensitiveImageButton btn) {
                    btn.getButton().setFocused(false);
                }
            }
            return;
        }

        Set<String> lookup = highlightWaypoints.stream().map(w -> w.name).collect(Collectors.toSet());
        for (TWidget child : this.buttons.getChildren()) {
            if (!(child instanceof THoverSensitiveImageButtonImpl btn)) {
                continue;
            }
            btn.getButton().setFocused(lookup.contains(btn.getText().getText().getString()));
        }
    }

    public String getHighlightWaypoints() {
        for (TWidget child : this.buttons.getChildren()) {
            if (child instanceof THoverSensitiveImageButtonImpl btn) {
                if (btn.getButton().isHovered()) {
                    return btn.waypointName;
                }
            }
        }
        return null;
    }
}
