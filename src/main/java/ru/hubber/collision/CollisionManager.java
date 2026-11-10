package ru.hubber.collision;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.hubber.HubberPlugin;
import ru.hubber.config.ConfigManager;

public class CollisionManager {

    private static final String TEAM_NAME = "hub_no_push";

    private final HubberPlugin plugin;
    private final ConfigManager config;
    private Team team;

    public CollisionManager(HubberPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        setupTeam();
    }

    private void setupTeam() {
        if (!config.isDisableCollisions()) {
            return;
        }

        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) {
                team = scoreboard.registerNewTeam(TEAM_NAME);
            }
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setCanSeeFriendlyInvisibles(false);

            // Apply to all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyCollisionRule(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось настроить команду скорборда для отключения коллизий: " + e.getMessage());
        }
    }

    public void applyCollisionRule(Player player) {
        if (!config.isDisableCollisions()) {
            player.setCollidable(true);
            return;
        }

        try {
            player.setCollidable(false);
            if (team != null && !team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        } catch (Exception ignored) {
        }
    }

    public void removeCollisionRule(Player player) {
        try {
            player.setCollidable(true);
            if (team != null && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        } catch (Exception ignored) {
        }
    }

    public void disable() {
        try {
            if (team != null) {
                for (String entry : team.getEntries()) {
                    team.removeEntry(entry);
                }
                team.unregister();
                team = null;
            }
        } catch (Exception ignored) {
        }
    }
}
