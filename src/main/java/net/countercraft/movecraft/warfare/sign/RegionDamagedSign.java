package net.countercraft.movecraft.warfare.sign;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.WorldEdit7UpdateCommand;
import net.countercraft.movecraft.warfare.config.Config;
import net.countercraft.movecraft.warfare.utils.WarfareRepair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class RegionDamagedSign implements Listener {
    private final String HEADER = ChatColor.RED + "REGION DAMAGED!";

    @EventHandler
    public void onSignRightClick(PlayerInteractEvent event){
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
            return;
        }
        if (event.getClickedBlock().getType() != Material.OAK_WALL_SIGN){
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!sign.getLine(0).equals(HEADER)){
            return;
        }
        String regionName = sign.getLine(1).substring(sign.getLine(1).indexOf(":") + 1);
        long damages = Long.parseLong(sign.getLine(2).substring(sign.getLine(2).indexOf(":") + 1));
        String[] owners = sign.getLine(3).substring(sign.getLine(3).indexOf(":") + 1).split(",");
        if (!Movecraft.getInstance().getEconomy().has(event.getPlayer(), damages)) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Economy - Not Enough Money"));
            return;
        }

        if (!repairRegion(event.getClickedBlock().getWorld(), regionName)) {
            Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Assault - Repair Failed"), regionName));
            return;
        }
        event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Assault - Repairing Region"));
        Movecraft.getInstance().getEconomy().withdrawPlayer(event.getPlayer(), damages);
        World world = event.getClickedBlock().getWorld();
        final LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(event.getPlayer());
        ProtectedRegion aRegion = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world)).getRegion(regionName);
        for (String ownerName : owners) {
            if (ownerName.length() > 16) {
                aRegion.getOwners().addPlayer(UUID.fromString(ownerName));
            } else {

                if (Bukkit.getPlayer(ownerName) != null) {//Cannot add names directly as bug will allow free assaults
                    aRegion.getOwners().addPlayer(Bukkit.getPlayer(ownerName).getUniqueId());
                } else {
                    aRegion.getOwners().addPlayer(Bukkit.getOfflinePlayer(ownerName).getUniqueId());
                }
            }
        }
        //Clear the beacon
        int minX = sign.getX() - 2;
        int minY = sign.getY() - 3;
        int minZ = sign.getZ() - 1;
        int maxX = sign.getX() + 2;
        int maxY = sign.getY();
        int maxZ = sign.getZ() + 3;
        for (int x = minX ; x <= maxX ; x++){
            for (int y = minY ; y <= maxY ; y++){
                for (int z = minZ; z <= maxZ ; z++){
                    Block b = sign.getWorld().getBlockAt(x,y,z);
                    if (b.getType() == Material.BEDROCK || b.getType() == Material.BEACON || b.getType() == Material.IRON_BLOCK ){
                        b.setType(Material.AIR);
                    }
                }
            }
        }
    }

    public boolean repairRegion(World w, String regionName) {
        if (w == null || regionName == null)
            return false;
        Clipboard clipboard = WarfareRepair.getInstance().loadRegionRepairStateClipboard(regionName, w);
        if (clipboard == null){
            return false;
        }
        int minx = clipboard.getMinimumPoint().getBlockX();
        int miny = clipboard.getMinimumPoint().getBlockY();
        int minz = clipboard.getMinimumPoint().getBlockZ();
        int maxx = clipboard.getMaximumPoint().getBlockX();
        int maxy = clipboard.getMaximumPoint().getBlockY();
        int maxz = clipboard.getMaximumPoint().getBlockZ();
        for (int x = minx; x < maxx; x++) {
            for (int y = miny; y < maxy; y++) {
                for (int z = minz; z < maxz; z++) {
                    BlockVector3 ccloc = BlockVector3.at(x, y, z);
                    BaseBlock bb = clipboard.getFullBlock(ccloc);
                    if (bb.getBlockType() != BlockTypes.AIR || bb.getBlockType() != BlockTypes.CAVE_AIR || bb.getBlockType() != BlockTypes.VOID_AIR) { // most blocks will be air, quickly move on to the next. This loop will run 16 million times, needs to be fast
                        if (Config.AssaultDestroyableBlocks.contains(BukkitAdapter.adapt(bb.getBlockType()))) {
                            if (!w.getChunkAt(x >> 4, z >> 4).isLoaded())
                                w.loadChunk(x >> 4, z >> 4);
                            if (w.getBlockAt(x, y, z).isEmpty() || w.getBlockAt(x, y, z).isLiquid()) {
                                MovecraftLocation moveloc = new MovecraftLocation(x, y, z);
                                WorldEdit7UpdateCommand updateCommand = new WorldEdit7UpdateCommand(bb, w, moveloc, BukkitAdapter.adapt(bb.getBlockType()));
                                MapUpdateManager.getInstance().scheduleUpdate(updateCommand);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
}