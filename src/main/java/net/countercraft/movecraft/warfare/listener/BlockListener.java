package net.countercraft.movecraft.warfare.listener;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.MovecraftWarfare;
import net.countercraft.movecraft.warfare.assault.Assault;
import net.countercraft.movecraft.warfare.assault.AssaultStage;
import net.countercraft.movecraft.warfare.config.Config;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BlockListener implements Listener {
    private long lastDamagesUpdate = 0;
    private boolean isFragile(Material type) {
        return type.name().endsWith("BED") ||
                type == Material.PISTON_HEAD ||
                type == Material.LEVER ||
                type == Material.REPEATER ||
                type == Material.COMPARATOR ||
                type.name().endsWith("TORCH") ||
                type == Material.REDSTONE_WIRE ||
                type.name().endsWith("SIGN") ||
                type.name().endsWith("DOOR") ||
                type == Material.LADDER ||
                type.name().endsWith("PRESSURE_PLATE") ||
                type.name().endsWith("BUTTON") ||
                type == Material.TRIPWIRE_HOOK ||
                type == Material.TRIPWIRE ||
                type == Material.DAYLIGHT_DETECTOR ||
                type.name().endsWith("CARPET") ;
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void explodeEvent(EntityExplodeEvent e) {
        List<Assault> assaults = MovecraftWarfare.getInstance().getAssaultManager() != null ? MovecraftWarfare.getInstance().getAssaultManager().getAssaults() : null;
        if (assaults == null || assaults.size() == 0) {
            return;
        }

        for (final Assault assault : assaults) {
            if (assault.getStage().get() != AssaultStage.IN_PROGRESS)
                continue;
            Iterator<Block> i = e.blockList().iterator();
            while (i.hasNext()) {
                Block b = i.next();
                if (b.getWorld() != assault.getWorld())
                    continue;

                if (!assault.getRegion().contains(b.getX(), b.getY(), b.getZ()))
                    continue;

                // first see if it is outside the destroyable area
                BlockVector3 min = assault.getMinPos();
                BlockVector3 max = assault.getMaxPos();

                if (b.getLocation().getBlockX() < min.getBlockX() ||
                        b.getLocation().getBlockX() > max.getBlockX() ||
                        b.getLocation().getBlockZ() < min.getBlockZ() ||
                        b.getLocation().getBlockZ() > max.getBlockZ() ||
                        !Config.AssaultDestroyableBlocks.contains(b.getType()) ||
                        isFragile(b.getType())) {
                    i.remove();
                }


                // whether or not you actually destroyed the block, add to damages
                long damages = assault.getDamages() + Config.AssaultDamagesPerBlock;
                assault.setDamages(Math.min(damages, assault.getMaxDamages()));

                // notify nearby players of the damages, do this 1 second later so all damages from this volley will be included
                if (System.currentTimeMillis() < lastDamagesUpdate + 4000) {
                    continue;
                }
                final Location floc = b.getLocation();
                final World fworld = b.getWorld();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        long fdamages = assault.getDamages();
                        for (Player p : fworld.getPlayers()) {
                            if (Math.round(p.getLocation().getBlockX() / 1000.0) == Math.round(floc.getBlockX() / 1000.0) &&
                                    Math.round(p.getLocation().getBlockZ() / 1000.0) == Math.round(floc.getBlockZ() / 1000.0)) {
                                p.sendMessage(I18nSupport.getInternationalisedString("Damage") + ": " + fdamages);
                            }
                        }
                    }
                }.runTaskLater(Movecraft.getInstance(), 20);
                lastDamagesUpdate = System.currentTimeMillis();
            }
        }
    }


}