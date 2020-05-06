package yohandev.mclink.modules;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import yohandev.mclink.Scoreboard;

public class Health implements Listener, CommandExecutor
{
	public static final String OBJECTIVE = "maxhealth";
	public static final int DEFAULT = 6;

	public static void update(Player p)
	{
		int max = Scoreboard.get(OBJECTIVE, p.getName());
		if (max <= DEFAULT)
		{
			Scoreboard.set(OBJECTIVE, p.getName(), max = DEFAULT);
		}
		AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);

		attr.setBaseValue(max);
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
	public void onPlayerRespawn(PlayerRespawnEvent e)
	{
		update(e.getPlayer());
	}

	@EventHandler
	public void onEntityRegainHealth(EntityRegainHealthEvent e)
	{
		if (!(e.getEntity() instanceof Player))
		{
			return; // not a player
		}

		if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.EATING)
		{
			e.setCancelled(true); // only heal on eating
		}
	}

	public static class HeartCutscene extends Cutscene
	{
		public HeartCutscene(Player target, Location statue)
		{
			super(target);

			if (!Soul.takeaway(target, Soul.COST))
			{
				return; // somehow got here without souls
			}
			Scoreboard.add(OBJECTIVE, target.getName(), 2); // add a heart vessel now, just in case

			super.push(new SoundAction(Sound.MUSIC_DISC_MELLOHI, null));
			super.push(new ResetAction());
			super.push(new QuestItemAction(Material.COOKED_BEEF, statue, 150));
			super.push(new WaitAction(150));

			super.push(p ->
			{
				update(p); // update display

				return true;
			});
			super.push(new WaitAction(30));
			super.push(new SoundAction(Sound.ENTITY_PLAYER_LEVELUP, null));
			super.push(new PotionAction(PotionEffectType.REGENERATION, 10, 5));
		}
	}

	public static void gain(Player p)
	{
		if (Soul.takeaway(p, Soul.COST))
		{
			Scoreboard.add(OBJECTIVE, p.getName(), 2); // add a heart vessel
			update(p); // update display

			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10, 5, false, false, false));
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

		if (Soul.takeaway((Player) sender, Soul.COST))
		{
			Scoreboard.add(OBJECTIVE, p.getName(), 2); // add a heart vessel
			update(p); // update display

			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10, 5, false, false, false));

			return true;
		}
		return false; // not enough soul
	}
}
