package org.teacon.signmeup.gui.map;

import cn.ussshenzhou.t88.gui.advanced.THoverSensitiveImageButton;
import cn.ussshenzhou.t88.gui.screen.TScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.ClientHooks;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.waypoints.Waypoint;
import org.teacon.signmeup.gui.map.bp.CommandsButtonPanel;
import org.teacon.signmeup.gui.map.bp.WayPointsButtonPanel;
import org.teacon.signmeup.gui.settings.SettingsScreen;
import org.teacon.signmeup.hud.MiniMapAPI;

/**
 * @author USS_Shenzhou
 */
public class MapScreen extends TScreen {
    private static MapScreen instance = new MapScreen();

    private final MapPanel mapPanel = new MapPanel();
    private final THoverSensitiveImageButton settingsButton = new THoverSensitiveImageButton(Component.empty(),
            button -> ClientHooks.pushGuiLayer(Minecraft.getInstance(), new SettingsScreen()),
            SignMeUp.id("textures/gui/setting.png"),
            SignMeUp.id("textures/gui/setting_hovered.png"));
    private final CommandsButtonPanel commandsButtonPanel = new CommandsButtonPanel();
    private final WayPointsButtonPanel wayPointsButtonPanel = new WayPointsButtonPanel();

    public static void refreshInstance() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof MapScreen || screen instanceof SettingsScreen) {
            ((TScreen) screen).onClose(false);
            Minecraft.getInstance().setScreen(MapScreen.getNewInstance());
        } else {
            MapScreen.getNewInstance();
        }
    }

    public static MapScreen getNewInstance() {
        instance = new MapScreen();
        return instance;
    }

    public static MapScreen getInstance() {
        return instance;
    }

    public MapScreen() {
        super(Component.literal("Map Screen"));
        this.add(mapPanel);
        this.add(settingsButton);
        this.add(commandsButtonPanel);
        this.add(wayPointsButtonPanel);
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(graphics, pMouseX, pMouseY, pPartialTick);

        wayPointsButtonPanel.highlight(mapPanel.getHoveredWaypoints(pMouseX, pMouseY));

        String hider = MiniMapAPI.INSTANCE.getHiderString();
        if (hider != null) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(
                    font, Component.translatable("hud.sign_up.minimap.hide", hider),
                    graphics.guiWidth() / 2, graphics.guiHeight() - font.lineHeight * 2, 0xFFE8DDCD
            );
        }
    }

    public Waypoint getHighlightWaypoints() {
        return wayPointsButtonPanel.getHighlightWaypoints();
    }

    @Override
    public void layout() {
        mapPanel.setBounds(0, 0, width, height);
        settingsButton.setBounds(10, 10, 20, 20);
        var commandsPanelSize = commandsButtonPanel.getPreferredSize();
        commandsButtonPanel.setBounds(
                width - commandsPanelSize.x - 6,
                (height - commandsPanelSize.y) / 2,
                commandsPanelSize);
        var wayPointsPanelSize = wayPointsButtonPanel.getPreferredSize();
        wayPointsButtonPanel.setBounds(
                0,
                (height - wayPointsPanelSize.y) / 2,
                wayPointsPanelSize);
        super.layout();
    }
}
