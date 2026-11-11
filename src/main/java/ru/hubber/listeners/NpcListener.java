package ru.hubber.listeners;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.hubber.npc.NpcManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcListener implements Listener {

    private final NpcManager npcManager;
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();

    public NpcListener(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        handleNpcClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handleNpcClick(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleNpcClick(Player player, Entity entity, org.bukkit.event.Cancellable event) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!pdc.has(npcManager.getKeyNpcCmd(), PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        Long lastClick = clickCooldowns.get(player.getUniqueId());
        if (lastClick != null && (now - lastClick) < 750) {
            return;
        }
        clickCooldowns.put(player.getUniqueId(), now);

        String command = pdc.get(npcManager.getKeyNpcCmd(), PersistentDataType.STRING);
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        command = command.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Play feedback sound
        try {
            player.playSound(Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.PLAYER, 1.0f, 1.2f));
        } catch (Exception ignored) {
        }

        // Execute command as the player
        player.performCommand(command);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(npcManager.getKeyNpcCmd(), PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(npcManager.getKeyNpcCmd(), PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (event.getRightClicked().getPersistentDataContainer().has(npcManager.getKeyNpcCmd(), PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }
}
