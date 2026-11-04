package com.dansplugins.heraldry;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeraldrySmokeTest {

    @Test
    void mainClassIsAJavaPlugin() {
        assertTrue(JavaPlugin.class.isAssignableFrom(Heraldry.class));
    }

    @Test
    void pluginYmlPointsAtTheMainClass() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml must be on the classpath");
            String yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yml.contains("main: " + Heraldry.class.getName()), yml);
            assertTrue(yml.contains("name: Heraldry"), yml);
            assertTrue(!yml.contains("${version}"), "version placeholder must be expanded");
        }
    }

    @Test
    void pluginYmlDeclaresTheCommandAndEachSubcommandsPermission() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml must be on the classpath");
            PluginDescriptionFile description = new PluginDescriptionFile(in);

            assertEquals(Collections.singleton("heraldry"), description.getCommands().keySet());
            Map<String, PermissionDefault> defaults = new HashMap<>();
            for (Permission permission : description.getPermissions()) {
                defaults.put(permission.getName(), permission.getDefault());
            }
            Map<String, PermissionDefault> expected = new HashMap<>();
            expected.put("heraldry.view", PermissionDefault.TRUE);
            expected.put("heraldry.set", PermissionDefault.OP);
            expected.put("heraldry.issue", PermissionDefault.OP);
            assertEquals(expected, defaults);
            assertEquals(HeraldryCommand.SUBCOMMANDS.size(), defaults.size());
        }
    }
}
