package lihad.BeyondShop;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class BeyondShop extends JavaPlugin implements Listener {
	public static FileConfiguration config;
	protected static String PLUGIN_NAME = "BeyondShop";
	protected static String header = "[" + PLUGIN_NAME + "] ";
	private static Logger log = Logger.getLogger("Minecraft");
	
	public static Economy econ;
	
	public static WorldGuardPlugin wg;
	protected static int region_cost = 1;
	protected static double shop_charge = .05;
	protected static int last_taxed = 0;
	protected static World world;
	/**
	 * 
	 * 
	 * have all purchasable shops be defined with "shop" in the name of the region
	 */
	@Override
	public void onEnable() {
		config = getConfig();
		region_cost = config.getInt("region_cost");
		shop_charge = config.getDouble("shop_charge");
		last_taxed = config.getInt("last_taxed");
		world = this.getServer().getWorld("world");
		
		setupWorldGuard();
		
		this.getServer().getScheduler().runTaskTimer(this, new Runnable(){
			@Override
			public void run(){
				if(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) != last_taxed){
					last_taxed = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
					for(Map.Entry<String, ProtectedRegion> entry : wg.getRegionManager(getServer().getWorld("world")).getRegions().entrySet()){
						for(String s : entry.getValue().getMembers().getPlayers()){
							if(econ.bankBalance(s).balance >= shop_charge){
								econ.bankWithdraw(s, shop_charge);
								if(getServer().getPlayer(s) != null)getServer().getPlayer(s).sendMessage(ChatColor.AQUA+"you were charged "+shop_charge+" for owning "+entry.getKey());
							}else{
								DefaultDomain d = entry.getValue().getMembers();
								d.removePlayer(s);
								entry.getValue().setMembers(d);
								getServer().broadcastMessage(ChatColor.RED+s+ChatColor.LIGHT_PURPLE+" has defaulted on shop payments! "+entry.getKey()+" has fallen into ruin");
							}
						}
					}
				}
			}	
		}, 1200, 1200);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player)sender;
		if(cmd.getName().equalsIgnoreCase("bshop")){
			if(args.length == 1){
				if(args[0].equalsIgnoreCase("buy")){
					Iterator<ProtectedRegion> ir = wg.getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation()).iterator();
					while(ir.hasNext()){
						ProtectedRegion region = ir.next();
						if(region.getTypeName().contains("shop")){
							if(region.getMembers().size() == 0){
								if(econ.bankBalance(player.getName()).balance >= region_cost){
									DefaultDomain d = region.getMembers();
									d.addPlayer(player.getName());
									region.setMembers(d);
									player.sendMessage(ChatColor.GREEN+"you now own "+region.getTypeName()+"! the amount of "+region_cost+" will be automatically deducted from your account every day");
								}else{
									player.sendMessage(ChatColor.RED+"this region costs "+region_cost+" to purchase, which is money you don't have");
								}
							}else{
								player.sendMessage(ChatColor.RED+"someone already owns this shop");
							}
							break;
						}else{
							//player.sendMessage(ChatColor.RED+"there is no region here able to be purchased");
						}
					}
				}else if(args[0].equalsIgnoreCase("release")){
					Iterator<ProtectedRegion> ir = wg.getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation()).iterator();
					while(ir.hasNext()){
						ProtectedRegion region = ir.next();
						if(region.getTypeName().contains("shop")){
							if(region.getMembers().contains(player.getName())){
								DefaultDomain d = region.getMembers();
								d.removePlayer(player.getName());
								region.setMembers(d);
								player.sendMessage(ChatColor.GREEN+"you have released "+region.getTypeName()+"!");

							}else{
								player.sendMessage(ChatColor.RED+"you can not release a shop you do not own");
							}
							break;
						}else{
							//player.sendMessage(ChatColor.RED+"there is no region here able to be released");
						}
					}
				}
			}else{
				cmdHelp(player);
			}
			return true;
		}
		return false;
	} 
	private void cmdHelp(Player player){
		player.sendMessage(ChatColor.GOLD+"/bshop buy - stand in a purchasable shop to buy");
		player.sendMessage(ChatColor.GOLD+"/bshop release - stand in a owned shop to release");
		String s = "";
		for(Map.Entry<String, ProtectedRegion> entry : wg.getRegionManager(world).getRegions().entrySet()){
			if(entry.getValue().isMember(player.getName())) s.concat(" "+entry.getKey());
		}
		player.sendMessage(ChatColor.GOLD+"you own:"+ChatColor.GREEN+s);
	}
	@EventHandler
	public void onPluginEnable(PluginEnableEvent event){
		if((event.getPlugin().getDescription().getName().equals("WorldGuard"))) setupWorldGuard();
		if((event.getPlugin().getDescription().getName().equals("Vault"))) setupEconomy();

	}
	private void setupWorldGuard() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	    if (plugin != null) {
			info("Succesfully connected to WorldGuard!");
			wg = ((WorldGuardPlugin) plugin);
		} else {
			wg = null;
			warning("Disconnected from WorldGuard...what could possibly go wrong?");
		}
	}
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
	
	private static void info(String message){ 
		log.info(header + message);
	}
	private static void severe(String message){
		log.severe(header + message);
	}
	private static void warning(String message){
		log.warning(header + message);
	}
}
