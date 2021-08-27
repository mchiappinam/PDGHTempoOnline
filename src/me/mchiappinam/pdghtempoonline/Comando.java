package me.mchiappinam.pdghtempoonline;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class Comando implements CommandExecutor, Listener {
	
	private Main plugin;
	public Comando(Main main) {
		plugin=main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("online")) {
			Threads t = new Threads(plugin,"top",(Player)sender);
			t.start();
			return true;
		}
		if(cmd.getName().equalsIgnoreCase("onlineconsole")) {
			try {
				Connection con = DriverManager.getConnection(Main.mysql_url,Main.mysql_user,Main.mysql_pass);
				//Prepared statement
				PreparedStatement pst = con.prepareStatement("SELECT * FROM  `jogadores` ORDER BY  `jogadores`.`pontos` DESC LIMIT 0 , 10");
				ResultSet rs = pst.executeQuery();
				int ordem=1;
				if(sender!=null)
					sender.sendMessage("§e§l---------[ §2§lONLINE§e§l ]---------");
				while(rs.next()) {
					if(sender==null) {
						rs.close();
						pst.close();
						con.close();
						break;
					}
					sender.sendMessage("§2"+ordem+". §e"+rs.getString("nick")+" §8- §2"+rs.getInt("pontos"));
					ordem++;
				}	
				rs.close();
				pst.close();
				con.close();
			}catch (SQLException ex) {
				System.out.print(ex);
				sender.sendMessage("§cErro! Contate um staffer!");
			}
		
			return true;
		}
		return false;
	}

}