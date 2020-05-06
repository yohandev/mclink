package yohandev.mclink.modules;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import yohandev.mclink.Main;
import yohandev.mclink.Scoreboard;

public class Stamina implements Listener, CommandExecutor
{
	public static final String OBJECTIVE = "maxstamina";
	public static final int DEFAULT = 6;
	public static final int COST = 4;

	public static void update(Player p)
	{
		int max = Scoreboard.get(OBJECTIVE, p.getName());
		if (max <= DEFAULT)
		{
			Scoreboard.set(OBJECTIVE, p.getName(), max = DEFAULT);
		}
		int val = max;

		// run a tick later to override events
		Bukkit.getScheduler().runTaskLater(Main.instance, () -> p.setFoodLevel(val), 1);
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent e)
	{
		update(e.getPlayer());
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Player player = e.getPlayer();

		if (!player.hasPlayedBefore())
		{
			Scoreboard.set(OBJECTIVE, player.getName(), DEFAULT);
		}

		update(player);
	}

	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent e)
	{
		if (!(e.getEntity() instanceof Player))
		{
			return; // not a player
		}
		Player p = (Player) e.getEntity();

		int max = Scoreboard.get(OBJECTIVE, p.getName());
		int ate = e.getFoodLevel() - p.getFoodLevel();
		int val = Math.min(max, e.getFoodLevel());

		e.setFoodLevel(val);

		if (val >= max) // heal
		{
			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ate * 2, 4, false, false, false));
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player))
		{
			return false; // not a player
		}
		Player p = (Player) sender;

		if (Soul.takeaway((Player) sender, COST))
		{
			Scoreboard.add(OBJECTIVE, p.getName(), 2); // add a stamina vessel
			update(p); // update display

			p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100, 5, false, false, false));

			return true;
		}
		return false; // not enough soul
	}
}
