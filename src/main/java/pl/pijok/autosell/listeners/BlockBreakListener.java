package pl.pijok.autosell.listeners;

import at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import pl.pijok.autosell.AutoSell;
import pl.pijok.autosell.Controllers;
import pl.pijok.autosell.essentials.ChatUtils;
import pl.pijok.autosell.essentials.Utils;
import pl.pijok.autosell.hooks.MinepacksHook;
import pl.pijok.autosell.miner.Miner;
import pl.pijok.autosell.miner.MinersController;
import pl.pijok.autosell.miner.Range;
import pl.pijok.autosell.selling.SellingController;
import pl.pijok.autosell.settings.Lang;
import pl.pijok.autosell.settings.Settings;

import java.util.HashMap;

public class BlockBreakListener implements Listener {

    private final MinersController minersController = Controllers.getMinersController();
    private final SellingController sellingController = Controllers.getSellingController();

    private final HashMap<Player, Long> fullInventoryWarnings;

    public BlockBreakListener(){
        fullInventoryWarnings = new HashMap<>();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event){

        Player player = event.getPlayer();

        if(event.isCancelled()){
            return;
        }

        if(Settings.isCountBlocks()){
            if(Settings.isCountOnlyOnMiningTools()){
                Material material = player.getInventory().getItemInHand().getType();

                if(!Settings.getMiningTools().contains(material)){
                    return;
                }
            }
            minersController.getMiner(player).increasedMinedBlocks(1);
        }

        Miner miner = minersController.getMiner(player);

        if(miner.isAutoSell()){
            ItemStack itemStack = createDrop(player, event.getBlock());
            if(miner.getAutoSellFilter().contains(itemStack.getType())){
                if(sellingController.isSellableItem(itemStack.getType())){
                    sellingController.sellSingleItem(player, createDrop(player, event.getBlock()));
                    event.getBlock().getDrops().clear();
                }
            }
            else{
                handleDropToInventory(event, player);
            }
        }
        else handleDropToInventory(event, player);
    }

    private void handleDropToInventory(BlockBreakEvent event, Player player) {
        if(Settings.isDropToInventory()){
            if(Settings.getDropToInventoryWorlds().contains(player.getWorld().getName())){
                ItemStack drop = createDrop(player, event.getBlock());
                player.getInventory().addItem(drop);
                event.getBlock().getDrops().clear();

                //Checks for full inventory
                if(event.getPlayer().getInventory().firstEmpty() == -1){

                    if(MinepacksHook.isEnabled()){
                        Backpack backpack = MinepacksHook.getMinepacksPlugin().getBackpackCachedOnly(player);
                        if(backpack != null){
                            if(backpack.getInventory().firstEmpty() != -1){
                                //backpack.getInventory().addItem(drop);
                                return;
                            }
                        }
                    }

                    boolean sendWarning = true;
                    if(fullInventoryWarnings.containsKey(player)){
                        if(System.currentTimeMillis() - fullInventoryWarnings.get(player) < 5000){
                            sendWarning = false;
                        }
                    }

                    if(sendWarning){
                        ChatUtils.sendMessage(player, Lang.getText("FULL_INVENTORY"));
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 5, 5);
                        fullInventoryWarnings.put(player, System.currentTimeMillis());
                    }
                }
            }
        }
    }

    private boolean canBuild(Player p, Location l) {
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(l);
        return !query.testState(loc, WorldGuardPlugin.inst().wrapPlayer(p), Flags.BLOCK_BREAK);
    }

    private ItemStack createDrop(Player player, Block block){
        ItemStack itemStack;

        if(Settings.getOreDrops().containsKey(block.getType())){
            itemStack = new ItemStack(Settings.getOreDrops().get(block.getType()));
        }
        else{
            itemStack = new ItemStack(block.getType());
        }

        if(player.getInventory().getItemInHand().getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)){
            Range range = Settings.getFortuneDrop(player.getInventory().getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS));
            if(range != null){
                itemStack.setAmount(Utils.getRandomNumber(range.getMin(), range.getMax()));
            }
        }

        return itemStack;
    }

}
