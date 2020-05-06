package yohandev.mclink.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import yohandev.mclink.Config;

import java.util.Stack;

public class HealthHungerListener implements Listener, CommandExecutor
{
	private Stack<Player> m_spawned = new Stack<>();

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e)
	{
		Player player = e.getPlayer();

		player.setMaxHealth(Config.hearts(player)); // health
		m_spawned.push(player); // stamina
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Player player = e.getPlayer();

		player.setMaxHealth(Config.hearts(player)); // health
		m_spawned.push(player); // stamina
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event)
	{
		while (!m_spawned.empty())
		{
			m_spawned.peek().setFoodLevel(Config.stamina(m_spawned.pop()));
		}
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent e)
	{
		if (!(e.getEntity() instanceof Player))
		{
			return; // not a player
		}

		Player player = (Player) e.getEntity();

		int ate = e.getFoodLevel() - player.getFoodLevel(); // food item saturation
		int max = Config.stamina(player); // max stamina

		player.setFoodLevel(Math.min(max, e.getFoodLevel())); // set food

		if (player.getFoodLevel() >= max) // heal
		{
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ate * 2, 4, true, false, false));
		}

		e.setCancelled(true); // cancel to apply
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		Player player = (Player) sender;

		if (args[0].equalsIgnoreCase("heart"))
		{
			player.setMaxHealth(Config.hearts(player, 1)); // health
			return true;
		}
		else if (args[0].equalsIgnoreCase("stamina"))
		{
			Config.stamina(player, 1);
			m_spawned.push(player); // stamina
			return true;
		}
		else
		{
			sender.sendMessage("Unknown argument \"" + args[0] + "\"");
			return false;
		}
	}
}
