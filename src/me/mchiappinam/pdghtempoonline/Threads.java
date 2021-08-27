package me.mchiappinam.pdghtempoonline;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Threads extends Thread {
	private Main plugin;
	private String tipo;
	private Player sender;
	
	public Threads(Main pl,String tipo2,Player player2) {
		plugin=pl;
		tipo=tipo2;
		sender=player2;
	}
	
	public void run() {
		switch(tipo) {
			case "add": {
				try {
					Connection con = DriverManager.getConnection(Main.mysql_url,Main.mysql_user,Main.mysql_pass);
					//Prepared statement
					PreparedStatement pst = con.prepareStatement("SELECT `nick`,`pontos` FROM `"+Main.tabela+"` WHERE( `nick`='"+sender.getName().toLowerCase().trim()+"');");
					ResultSet rs = pst.executeQuery();
					boolean existe=false;
					int pontos;
					while(rs.next()) {
		            	pontos=(rs.getInt("pontos"))+1;
						existe=true;
							if(sender==null) {
								rs.close();
								pst.close();
								con.close();
								break;
							}
							sender.sendMessage("§2§l[ON]§2 Você ficou +10 minutos online no servidor e ganhou "+plugin.getConfig().getDouble("valor")+" coins + 1 ponto. Total: §d"+pontos+" §2pontos no §e/online§2.");
							if((pontos==3)||(pontos==6)||(pontos==50)||(pontos==100)||(pontos==1000)||(pontos==8000)||(pontos==16000)||(pontos==32000)||(pontos==64000)) {
								plugin.proxPremio.add(sender.getName().toLowerCase().trim()+","+pontos);
							}/**else if(pontos==6){ //(1 hora)
							}else if(pontos==50){ //(8.33 horas)
							}else if(pontos==100){ //(16.66 horas)
							}else if(pontos==1000){ //(7 dias)
							}else if(pontos==8000){ //(55 dias)
							}else if(pontos==16000){ //(110 dias)
							}else if(pontos==32000){ //(220 dias)
							}else if(pontos==64000){ //(440 dias)
							}*/
							rs.close();
							pst.close();
							PreparedStatement pst1 = con.prepareStatement("UPDATE `"+Main.tabela+"` SET `pontos`=`pontos`+1 WHERE( `nick`='"+sender.getName().toLowerCase().trim()+"');");
							pst1.executeUpdate();
							pst1.close();
							con.close();
							break;
					}
					if(!existe) {
						Main.econ.depositPlayer(sender.getName(), 100.0);
						sender.sendMessage("");
						plugin.getServer().broadcastMessage("§2§l[ON]§a "+sender.getName()+" §2acaba de ganhar seu primeiro ponto.");
						sender.sendMessage("§2§l[ON]§f Parabéns e seja bem vindo(a) à PDGH! =')");
						sender.sendMessage("§2§l[ON]§f Quanto mais pontos você acumular, melhores são seus prêmios!");
						sender.sendMessage("§2§l[ON]§f Você ganhou 100 coins! O próximo prêmio é com 3 pontos e você ganhará diversos itens encantados.");
						sender.sendMessage("§2§l[ON]§f A cada 10 minutos jogando, 1 ponto é adicionado em sua conta.");
						sender.sendMessage("");
						sender.playSound(sender.getLocation(), Sound.ANVIL_USE, 1.0F, 1.0F);
						//Prepared statement
						PreparedStatement pst1 = con.prepareStatement("INSERT INTO `"+Main.tabela+"`(nick, pontos) VALUES(?, ?)");
						//Values
						pst1.setString(1, sender.getName().toLowerCase().trim());
						pst1.setInt(2, 1);
						//Do the MySQL query
						pst1.executeUpdate();
						pst1.close();
						con.close();
						break;
					}
				}catch (SQLException ex) {
					System.out.print(ex);
					sender.sendMessage("§cErro! Contate um staffer!");
					break;
				}
			}
			case "top": {
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
					break;
				}catch (SQLException ex) {
					System.out.print(ex);
					sender.sendMessage("§cErro! Contate um staffer!");
					break;
				}
			}
		}
	}
}
