package net.countercraft.movecraft.warfare.commands;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.repair.MovecraftRepair;
import net.countercraft.movecraft.warfare.assault.AssaultBeginTask;
import net.countercraft.movecraft.warfare.events.AssaultPreStartEvent;
import net.countercraft.movecraft.warfare.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.assault.Assault;
import net.countercraft.movecraft.warfare.assault.AssaultUtils;
import net.countercraft.movecraft.warfare.config.Config;
import net.countercraft.movecraft.warfare.utils.WarfareRepair;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class AssaultCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("assault"))
            return false;

        if (!Config.AssaultEnable) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Disabled"));
            return true;
        }
        if (args.length == 0) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - No Region Specified"));
            return true;
        }
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("AssaultInfo - Must Be Player"));
            return true;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("movecraft.assault")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }

        ProtectedRegion region = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(args[0]);
        if (region == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Region Not Found"));
            return true;
        }
        if (!AssaultUtils.ownsRegions(player)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - No Region Owned"));
            return true;
        }
        if (!AssaultUtils.canAssault(region) || !AssaultUtils.areDefendersOnline(region) || AssaultUtils.isMember(player, region)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Cannot Assault"));
            return true;
        }

        OfflinePlayer offP = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (MovecraftRepair.getInstance().getEconomy().getBalance(offP) < AssaultUtils.getCostToAssault(region)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault - Insufficient Funds"));
            return true;
        }


//			if(region.getType() instanceof ProtectedCuboidRegion) { // Originally I wasn't going to do non-cubes, but we'll try it and see how it goes. In theory it may repair more than it should but... meh...
        Vector min = new Vector(region.getMinimumPoint().getBlockX(), region.getMinimumPoint().getBlockY(), region.getMinimumPoint().getBlockZ());
        Vector max = new Vector(region.getMaximumPoint().getBlockX(), region.getMaximumPoint().getBlockY(), region.getMaximumPoint().getBlockZ());

        if (max.subtract(min).getBlockX() > 256) {
            if (min.getBlockX() < player.getLocation().getBlockX() - 128) {
                min = min.setX(player.getLocation().getBlockX() - 128);
            }
            if (max.getBlockX() > player.getLocation().getBlockX() + 128) {
                max = max.setX(player.getLocation().getBlockX() + 128);
            }
        }
        if (max.subtract(min).getBlockZ() > 256) {
            if (min.getBlockZ() < player.getLocation().getBlockZ() - 128) {
                min = min.setZ(player.getLocation().getBlockZ() - 128);
            }
            if (max.getBlockZ() > player.getLocation().getBlockZ() + 128) {
                max = max.setZ(player.getLocation().getBlockZ() + 128);
            }
        }
//			} else {
//				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "This region is not a cuboid - see an admin" ) ) );
//				return true;
//			}

        final Long taskMaxDamages = (long) AssaultUtils.getMaxDamages(region);

        Assault assault = new Assault(region, player, player.getWorld(), System.currentTimeMillis()+(Config.AssaultDelay * 1000L), taskMaxDamages, min, max);

        AssaultPreStartEvent assaultPreStartEvent = new AssaultPreStartEvent(assault);
        Bukkit.getPluginManager().callEvent(assaultPreStartEvent);

        if (assaultPreStartEvent.isCancelled()) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + assaultPreStartEvent.getCancelReason());
            return true;
        }

        WarfareRepair.getInstance().saveRegionRepairState(player.getWorld(), assault);

        MovecraftRepair.getInstance().getEconomy().withdrawPlayer(offP, AssaultUtils.getCostToAssault(region));

        Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Assault - Starting Soon")
                , player.getDisplayName(), args[0], Config.AssaultDelay / 60));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, (float) 0.25);
        }
        AssaultBeginTask beginTask = new AssaultBeginTask(player, assault);
        beginTask.runTaskLater(Movecraft.getInstance(), (20L * Config.AssaultDelay));
        return true;
    }
}
