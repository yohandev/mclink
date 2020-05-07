package yohandev.mclink.modules;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import yohandev.mclink.Scoreboard;
import yohandev.mclink.Utilities;

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
		public HeartCutscene(Player target, Block statue)
		{
			super(target);

			if (!Soul.takeaway(target, Soul.COST))
			{
				return; // somehow got here without souls
			}
			Scoreboard.add(OBJECTIVE, target.getName(), 2); // add a heart vessel now, just in case

			/* spinning heart */
			super.sync(new GameModeAction(GameMode.SURVIVAL));
			super.async(new SpinningHeartAction(statue));
			super.sync(new AwaitAction());

			/* gain heart */
			super.sync(new RunnableAction(() -> update(target)));
			super.sync(new SoundAction(Sound.MUSIC_DISC_MELLOHI));
			super.sync(new WaitAction(30));

			/* regen */
			super.sync(new SoundAction(Sound.ENTITY_PLAYER_LEVELUP));
			super.sync(new PotionAction(PotionEffectType.REGENERATION, 10, 5));
		}

		private class SpinningHeart extends Cutscene
		{
			public SpinningHeart(Location from, Entity to)
			{
				super(Utilities.FloatingItem(Material.COMMAND_BLOCK_MINECART, from));

				super.async(new LerpAction(from, to, 100, false));
				super.async(new SpinAction(4, 100));
				super.sync(new AwaitAction());
				super.sync(new SuicideAction());
			}
		}

		private class SpinningHeartAction implements Action
		{
			private final Block statue;
			private SpinningHeart scene;

			private SpinningHeartAction(Block statue)
			{
				this.statue = statue;
				this.scene = null;
			}

			@Override
			public boolean run()
			{
				if (scene == null)
				{
					scene = new SpinningHeart(statue.getLocation().clone().add(0, 5, 0), target);
					scene.run();
				}

				return scene.done();
			}
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
