package me.mchiappinam.pdghtempoonline;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

public class Main extends JavaPlugin {
	public static String mysql_url = "";
    public static String mysql_user = "";
    public static String mysql_pass = "";
    public static String tabela = "";
	final HashMap<String, Integer> taskIDs = new HashMap<String, Integer>();
	protected static Economy econ=null;
	List<String> nicks = new ArrayList<String>();
	List<String> ips = new ArrayList<String>();
	List<String> proxPremio = new ArrayList<String>();
	
	public void onEnable() {
		File file = new File(getDataFolder(),"config.yml");
		if(!file.exists()) {
			try {
				saveResource("config_template.yml",false);
				File file2 = new File(getDataFolder(),"config_template.yml");
				file2.renameTo(new File(getDataFolder(),"config.yml"));
			}
			catch(Exception e) {}
		}
		mysql_url="jdbc:mysql://"+getConfig().getString("mySQL.ip")+":"+getConfig().getString("mySQL.porta")+"/"+getConfig().getString("mySQL.db");
		mysql_user=getConfig().getString("mySQL.usuario");
		mysql_pass=getConfig().getString("mySQL.senha");
		tabela=getConfig().getString("mySQL.tabela");
		getServer().getPluginManager().registerEvents(new Listeners(this), this);
		getServer().getPluginCommand("online").setExecutor(new Comando(this));
		getServer().getPluginCommand("onlineconsole").setExecutor(new Comando(this));
		
		try {
			Connection con = DriverManager.getConnection(mysql_url,mysql_user,mysql_pass);
			if (con == null) {
				getLogger().warning("ERRO: Conexao ao banco de dados MySQL falhou!");
				getServer().getPluginManager().disablePlugin(this);
			}else{
				Statement st = con.createStatement();
				st.execute("CREATE TABLE IF NOT EXISTS `"+tabela+"` ( `id` MEDIUMINT NOT NULL AUTO_INCREMENT, `nick` text, `pontos` INT(255), PRIMARY KEY (`id`))");
				st.close();
				getServer().getConsoleSender().sendMessage("§3[PDGHTempoOnline] §3Conectado ao banco de dados MySQL!");
			}
			con.close();
		}catch (SQLException e) {
			getLogger().warning("ERRO: Conexao ao banco de dados MySQL falhou!");
			getLogger().warning("ERRO: "+e.toString());
			getServer().getPluginManager().disablePlugin(this);
		}
		
		if(!setupEconomy()) {
			getLogger().warning("ERRO: Vault (Economia) nao encontrado!");
			getLogger().warning("Desativando o plugin...");
			getServer().getPluginManager().disablePlugin(this);
        }
		checkPremios();
		getServer().getConsoleSender().sendMessage("§3[PDGHTempoOnline] §2ativado - Plugin by: mchiappinam");
		getServer().getConsoleSender().sendMessage("§3[PDGHTempoOnline] §2Acesse: http://pdgh.com.br/");
	}

	public void onDisable() {
		getServer().getConsoleSender().sendMessage("§3[PDGHTempoOnline] §2desativado - Plugin by: mchiappinam");
		getServer().getConsoleSender().sendMessage("§3[PDGHTempoOnline] §2Acesse: http://pdgh.com.br/");
	}
	
	private void checkPremios() {
	  	getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
	  		public void run() {
	  			for(String a : proxPremio) {
	  				String b[] = a.split(",");
	  				int pontos = Integer.parseInt(b[1]);
	  				premio(getServer().getPlayerExact(b[0]), pontos);
	  				//proxPremio.remove(a);
	  			}
	  			proxPremio.clear();
	  		}
	  	}, 0, 10);
	}
		
	public void startTask(final Player p) {
		taskIDs.put(p.getName(), getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				if(getP(p)) {
					naoGanha(p);
					return;
				}
				if(getIP(p)) {
					naoGanha(p);
					return;
				}
				econ.depositPlayer(p.getName(), getConfig().getDouble("valor"));
				add(p);
				startTask(p);
			}
		}, 10*60*20L));
	}
	
	public void add(Player p) {
		Threads t = new Threads(this,"add",p);
		t.start();
	}
	
	public void naoGanha(final Player p) {
	    getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
	    	public void run() {
		    	  if(p.isOnline()) {
		    		  p.sendMessage(" ");
		    		  p.sendMessage(" ");
		    		  p.sendMessage("§cVoce não vai ganhar a recompensa de estar online pois seu nick/ip está bloqueado até a reinicialização.");
		    		  p.sendMessage("§aDica: Para não ter o nick ou o ip bloqueado, não entre em contas simultâneas.");
		    		  p.sendMessage("§aCaso tenha mais de 1 pessoa em sua residência, recomendamos que não entre ao mesmo tempo no mesmo servidor.");
		    		  p.sendMessage(" ");
		    		  p.sendMessage(" ");
		    	  }
	    	}
	    }, 240L);
	}
	

	
    public void premio(Player p, int pontos) {
    	boolean full=false;
		p.playSound(p.getLocation(), Sound.ANVIL_USE, 1.0F, 1.0F);
		econ.depositPlayer(p.getName(), pontos*5);
		p.sendMessage("");
		getServer().broadcastMessage("§2§l[ON]§a "+p.getName()+" §ftem no total §c"+pontos+" §fpontos.");
		p.sendMessage("§2§l[ON]§f Parabéns! Quanto mais pontos você acumular, melhores são seus prêmios!");
		p.sendMessage("§2§l[ON]§f Você ganhou "+pontos*5+" coins!");
		p.sendMessage("");
    	if(pontos==64000) {
    		p.sendMessage("§2§l[ON]§f §lVocê é o melhor! ;)");
			p.sendMessage("");
	        getServer().dispatchCommand(getServer().getConsoleSender(), "darvip "+p.getName()+" VIP 30");
	        
		}else if(pontos==32000) {
			p.sendMessage("§2§l[ON]§f §lFaltam "+(64000-pontos)+" pontos para o próximo prêmio!");
			p.sendMessage("");
	        getServer().dispatchCommand(getServer().getConsoleSender(), "darvip "+p.getName()+" VIP 30");
	        
		}else if(pontos==3) {
			p.sendMessage("§2§l[ON]§f Faltam 3 pontos para o próximo prêmio!");
			p.sendMessage("");
	    	
		    ItemStack elmo = new ItemStack(Material.LEATHER_HELMET, 1);
		    elmo.addEnchantment(Enchantment.PROTECTION_FIRE, 2);
		    elmo.addEnchantment(Enchantment.DURABILITY, 3);

		    ItemStack peitoral = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
		    peitoral.addEnchantment(Enchantment.PROTECTION_FIRE, 2);
		    peitoral.addEnchantment(Enchantment.DURABILITY, 3);

		    ItemStack calça = new ItemStack(Material.LEATHER_LEGGINGS, 1);
		    calça.addEnchantment(Enchantment.PROTECTION_FIRE, 2);
		    calça.addEnchantment(Enchantment.DURABILITY, 3);

		    ItemStack bota = new ItemStack(Material.LEATHER_BOOTS, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_FIRE, 2);
		    bota.addEnchantment(Enchantment.DURABILITY, 3);
		    bota.addEnchantment(Enchantment.PROTECTION_FALL, 3);
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(elmo);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), elmo);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(peitoral);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), peitoral);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(calça);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), calça);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(bota);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), bota);
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_SWORD, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_SWORD, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 32));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 32));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 16));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 16));
	        }
	        
	        
	        
	        if(full) {
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        	p.sendMessage("§2§l[ON]§c Seu inventário está cheio! Dropando itens no chão...");
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        }
	        
		}else if(pontos==6) {
			p.sendMessage("§2§l[ON]§f Faltam 44 pontos para o próximo prêmio!");
			p.sendMessage("");
	    	
		    ItemStack elmo = new ItemStack(Material.IRON_HELMET, 1);
		    elmo.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    elmo.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack peitoral = new ItemStack(Material.IRON_CHESTPLATE, 1);
		    peitoral.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    peitoral.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack calça = new ItemStack(Material.IRON_LEGGINGS, 1);
		    calça.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    calça.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack bota = new ItemStack(Material.IRON_BOOTS, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    bota.addEnchantment(Enchantment.DURABILITY, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_FALL, 2);
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(elmo);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), elmo);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(peitoral);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), peitoral);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(calça);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), calça);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(bota);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), bota);
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.DIAMOND_SWORD, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 10));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 10));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 32));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 32));
	        }
	        
	        
	        
	        if(full) {
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        	p.sendMessage("§2§l[ON]§c Seu inventário está cheio! Dropando itens no chão...");
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        }
	        
		}else if(pontos==50) {
			p.sendMessage("§2§l[ON]§f §lFaltam "+(100-pontos)+" pontos para o próximo prêmio!");
			p.sendMessage("");
	    	
		    ItemStack elmo = new ItemStack(Material.IRON_HELMET, 1);
		    elmo.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    elmo.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack peitoral = new ItemStack(Material.IRON_CHESTPLATE, 1);
		    peitoral.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    peitoral.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack calça = new ItemStack(Material.IRON_LEGGINGS, 1);
		    calça.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    calça.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack bota = new ItemStack(Material.IRON_BOOTS, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    bota.addEnchantment(Enchantment.DURABILITY, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_FALL, 2);

		    ItemStack espada = new ItemStack(Material.DIAMOND_SWORD, 1);
		    espada.addEnchantment(Enchantment.FIRE_ASPECT, 1);
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(elmo);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), elmo);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(peitoral);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), peitoral);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(calça);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), calça);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(bota);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), bota);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(espada);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), espada);
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 32));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 32));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 16));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 16));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	        
	        
	        
	        if(full) {
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        	p.sendMessage("§2§l[ON]§c Seu inventário está cheio! Dropando itens no chão...");
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        }
	        
		}else if(pontos==100) {
			p.sendMessage("§2§l[ON]§f §lFaltam "+(1000-pontos)+" pontos para o próximo prêmio!");
			p.sendMessage("");
	    	
		    ItemStack elmo = new ItemStack(Material.DIAMOND_HELMET, 1);
		    elmo.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    elmo.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack peitoral = new ItemStack(Material.DIAMOND_CHESTPLATE, 1);
		    peitoral.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    peitoral.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack calça = new ItemStack(Material.DIAMOND_LEGGINGS, 1);
		    calça.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    calça.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack bota = new ItemStack(Material.DIAMOND_BOOTS, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
		    bota.addEnchantment(Enchantment.DURABILITY, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_FALL, 2);

		    ItemStack espada = new ItemStack(Material.DIAMOND_SWORD, 1);
		    espada.addEnchantment(Enchantment.DAMAGE_ALL, 1);
		    espada.addEnchantment(Enchantment.FIRE_ASPECT, 1);
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(elmo);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), elmo);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(peitoral);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), peitoral);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(calça);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), calça);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(bota);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), bota);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(espada);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), espada);
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1, (short) 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 1, (short) 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLD_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLD_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.MAGMA_CREAM, 16));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.MAGMA_CREAM, 16));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 32));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 32));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	        
	        
	        
	        if(full) {
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        	p.sendMessage("§2§l[ON]§c Seu inventário está cheio! Dropando itens no chão...");
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        }
	        
		}else if(pontos==1000) {
			p.sendMessage("§2§l[ON]§f §lFaltam "+(8000-pontos)+" pontos para o próximo prêmio!");
			p.sendMessage("");
	    	
		    ItemStack elmo = new ItemStack(Material.DIAMOND_HELMET, 1);

		    ItemStack peitoral = new ItemStack(Material.DIAMOND_CHESTPLATE, 1);

		    ItemStack calça = new ItemStack(Material.DIAMOND_LEGGINGS, 1);

		    ItemStack bota = new ItemStack(Material.DIAMOND_BOOTS, 1);

		    ItemStack espada = new ItemStack(Material.DIAMOND_SWORD, 1);
		    espada.addEnchantment(Enchantment.DAMAGE_ALL, 1);
		    espada.addEnchantment(Enchantment.FIRE_ASPECT, 2);
		    espada.addEnchantment(Enchantment.KNOCKBACK, 1);
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(elmo);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), elmo);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(peitoral);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), peitoral);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(calça);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), calça);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(bota);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), bota);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(espada);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), espada);
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2, (short) 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 2, (short) 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLD_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLD_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.MAGMA_CREAM, 32));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.MAGMA_CREAM, 32));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 32));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 32));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	        
	        
	        
	        if(full) {
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        	p.sendMessage("§2§l[ON]§c Seu inventário está cheio! Dropando itens no chão...");
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        }
	        
		}else if(pontos==8000) {
			p.sendMessage("§2§l[ON]§f §lFaltam "+(16000-pontos)+" pontos para o próximo prêmio!");
			p.sendMessage("");
	    	
		    ItemStack elmo = new ItemStack(Material.DIAMOND_HELMET, 1);
		    elmo.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1);
		    elmo.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack peitoral = new ItemStack(Material.DIAMOND_CHESTPLATE, 1);
		    peitoral.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1);
		    peitoral.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack calça = new ItemStack(Material.DIAMOND_LEGGINGS, 1);
		    calça.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1);
		    calça.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack bota = new ItemStack(Material.DIAMOND_BOOTS, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1);
		    bota.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack espada = new ItemStack(Material.DIAMOND_SWORD, 1);
		    espada.addEnchantment(Enchantment.DAMAGE_ALL, 1);
		    espada.addEnchantment(Enchantment.FIRE_ASPECT, 3);
		    espada.addEnchantment(Enchantment.KNOCKBACK, 2);
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(elmo);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), elmo);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(peitoral);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), peitoral);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(calça);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), calça);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(bota);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), bota);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(espada);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), espada);
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 8, (short) 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 8, (short) 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_PICKAXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLD_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLD_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLD_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLD_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.MAGMA_CREAM, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.MAGMA_CREAM, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.MAGMA_CREAM, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.MAGMA_CREAM, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	        
	        
	        
	        if(full) {
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        	p.sendMessage("§2§l[ON]§c Seu inventário está cheio! Dropando itens no chão...");
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        }
	        
		}else if(pontos==16000) {
			p.sendMessage("§2§l[ON]§f §lFaltam "+(32000-pontos)+" pontos para o próximo prêmio!");
			p.sendMessage("");
	    	
		    ItemStack elmo = new ItemStack(Material.DIAMOND_HELMET, 1);
		    elmo.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 2);
		    elmo.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack peitoral = new ItemStack(Material.DIAMOND_CHESTPLATE, 1);
		    peitoral.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 2);
		    peitoral.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack calça = new ItemStack(Material.DIAMOND_LEGGINGS, 1);
		    calça.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 2);
		    calça.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack bota = new ItemStack(Material.DIAMOND_BOOTS, 1);
		    bota.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 2);
		    bota.addEnchantment(Enchantment.DURABILITY, 1);

		    ItemStack espada = new ItemStack(Material.DIAMOND_SWORD, 1);
		    espada.addEnchantment(Enchantment.DAMAGE_ALL, 2);
		    espada.addEnchantment(Enchantment.FIRE_ASPECT, 3);
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(elmo);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), elmo);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(peitoral);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), peitoral);
	        }
		    
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(calça);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), calça);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(bota);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), bota);
	        }
		    
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(espada);
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), espada);
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 16, (short) 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 16, (short) 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLD_PICKAXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLD_PICKAXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_PICKAXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLD_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLD_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLD_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLD_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.IRON_AXE, 1));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GOLDEN_APPLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.MAGMA_CREAM, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.MAGMA_CREAM, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.MAGMA_CREAM, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.MAGMA_CREAM, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.MAGMA_CREAM, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.MAGMA_CREAM, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.GHAST_TEAR, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.GHAST_TEAR, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.FLINT, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.FLINT, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	
	        if(temEspacoInv(p))
	    	    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
	        else {
	        	full=true;
	        	p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
	        }
	        
	        
	        
	        if(full) {
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        	p.sendMessage("§2§l[ON]§c Seu inventário está cheio! Dropando itens no chão...");
	        	p.sendMessage(" ");
	        	p.sendMessage(" ");
	        }
	        
    	}
    }
	
    public boolean temEspacoInv(Player p) {
        if(p.getInventory().firstEmpty()==-1)
        	return false;
        else
        	return true;
    }
	
	
	public boolean getIP(Player p) {
		if(ips.contains(p.getAddress().getAddress().getHostAddress().replaceAll("/", ""))) {
			return true;
		}
		return false;
	}
	
	public boolean getP(Player p) {
		if(nicks.contains(p.getName().toLowerCase())) {
			return true;
		}
		return false;
	}
	
	public void addIP(Player p) {
		if(!ips.contains(p.getAddress().getAddress().getHostAddress().replaceAll("/", ""))) {
			ips.add(p.getAddress().getAddress().getHostAddress().replaceAll("/", ""));
		}
	}
	
	public void addP(Player p) {
		if(!nicks.contains(p.getName().toLowerCase())) {
			nicks.add(p.getName().toLowerCase());
		}
	}
		
	public void cancelTask(Player p) {
		Bukkit.getScheduler().cancelTask(taskIDs.get(p.getName()));
	}
	
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp=getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ=rsp.getProvider();
        return econ != null;
	}
	  
}