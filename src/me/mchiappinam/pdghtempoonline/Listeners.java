package me.mchiappinam.pdghtempoonline;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Listeners implements Listener {
	
	private Main plugin;
	public Listeners(Main main) {
		plugin=main;
	}
	  
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if(plugin.getP(e.getPlayer())) {
			plugin.naoGanha(e.getPlayer());
			return;
		}
		if(plugin.getIP(e.getPlayer())) {
			plugin.naoGanha(e.getPlayer());
			return;
		}
  		for (Player todos : plugin.getServer().getOnlinePlayers())
  			if(e.getPlayer().getAddress().getAddress().getHostAddress().replaceAll("/", "").contains(todos.getAddress().getAddress().getHostAddress().replaceAll("/", "")))
  				if(e.getPlayer().getName()!=todos.getName()) {
	  				plugin.addIP(e.getPlayer());
	  				plugin.addP(e.getPlayer());
	  				plugin.addP(todos);
	  				plugin.naoGanha(e.getPlayer());
	  				return;
  				}
		plugin.startTask(e.getPlayer());
	}
	  
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		if(plugin.getP(e.getPlayer())) {
			plugin.naoGanha(e.getPlayer());
			return;
		}
		if(plugin.getIP(e.getPlayer())) {
			plugin.naoGanha(e.getPlayer());
			return;
		}
		plugin.cancelTask(e.getPlayer());
	}
		
	@EventHandler
	public void onPlayerKick(PlayerKickEvent e) {
		if(plugin.getP(e.getPlayer())) {
			plugin.naoGanha(e.getPlayer());
			return;
		}
		if(plugin.getIP(e.getPlayer())) {
			plugin.naoGanha(e.getPlayer());
			return;
		}
		plugin.cancelTask(e.getPlayer());
	}
}