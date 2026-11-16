var windowsDesktops = desktopsForActivity(currentActivity());
var windowsScreens = [];
for (var i = 0; i < windowsDesktops.length; ++i) {
    var desktop = windowsDesktops[i];
    desktop.wallpaperPlugin = "org.kde.image";
    desktop.currentConfigGroup = ["Wallpaper", "org.kde.image", "General"];
    desktop.writeConfig("Image", "file:///usr/local/share/droiddeck/windows-wallpaper.png");
    if (desktop.screen >= 0 && windowsScreens.indexOf(desktop.screen) < 0) windowsScreens.push(desktop.screen);
}
if (!windowsScreens.length) windowsScreens.push(0);
var windowsOldPanels = panels();
for (var i = 0; i < windowsOldPanels.length; ++i) windowsOldPanels[i].remove();
for (var i = 0; i < windowsScreens.length; ++i) {
    var panel = new Panel;
    panel.screen = windowsScreens[i];
    panel.location = "bottom";
    panel.height = 46;
    panel.alignment = "left";
    panel.lengthMode = "fill";
    panel.hiding = "none";
    panel.floating = false;
    panel.opacity = "opaque";
    var start = panel.addWidget("org.kde.plasma.kickoff");
    start.currentConfigGroup = ["Shortcuts"];
    start.writeConfig("global", "Alt+F1");
    start.currentConfigGroup = ["General"];
    start.writeConfig("icon", "/usr/local/share/droiddeck/windows-start.svg");
    start.writeConfig("favorites", ["applications:droiddeck-brave.desktop", "applications:org.kde.dolphin.desktop",
        "applications:steamdeck-steam.desktop", "applications:org.kde.konsole.desktop", "applications:systemsettings.desktop"]);
    var tasks = panel.addWidget("org.kde.plasma.icontasks");
    tasks.currentConfigGroup = ["General"];
    tasks.writeConfig("launchers", ["applications:org.kde.dolphin.desktop", "applications:droiddeck-brave.desktop",
        "applications:steamdeck-steam.desktop", "applications:org.kde.konsole.desktop"]);
    panel.addWidget("org.kde.plasma.marginsseparator");
    panel.addWidget("org.kde.plasma.systemtray");
    var clock = panel.addWidget("org.kde.plasma.digitalclock");
    clock.currentConfigGroup = ["Appearance"];
    clock.writeConfig("showDate", true);
    clock.writeConfig("dateFormat", "shortDate");
    panel.addWidget("org.kde.plasma.showdesktop");
}
