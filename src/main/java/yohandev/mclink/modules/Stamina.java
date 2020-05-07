package yohandev.mclink.modules;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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
import yohandev.mclink.Utilities;

public class Stamina implements Listener
{
	public static final String OBJECTIVE = "maxstamina";
	public static final int DEFAULT = 6;

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

	public static class StaminaCutscene extends Cutscene
	{
		public StaminaCutscene(Player target, Block statue)
		{
			super(target);

			if (!Soul.takeaway(target, Soul.COST))
			{
				return; // somehow got here without souls
			}
			Scoreboard.add(OBJECTIVE, target.getName(), 2); // add a saturation now, just in case

			/* spinning food */
			super.sync(new GameModeAction(GameMode.SURVIVAL));
			super.async(new SpinningFoodAction(statue));
			super.sync(new AwaitAction());

			/* gain stamina */
			super.sync(new RunnableAction(() -> update(target)));
			super.sync(new SoundAction(Sound.MUSIC_DISC_MELLOHI));
			super.sync(new WaitAction(30));

			/* saturation */
			super.sync(new SoundAction(Sound.ENTITY_PLAYER_LEVELUP));
			super.sync(new PotionAction(PotionEffectType.SATURATION, 100, 5));
		}

		private class SpinningFood extends Cutscene
		{
			public SpinningFood(Location from, Entity to)
			{
				super(Utilities.FloatingItem(Material.STRUCTURE_VOID, from));

				super.async(new LerpAction(from, to, 100, false));
				super.async(new SpinAction(4, 100));
				super.sync(new AwaitAction());
				super.sync(new SuicideAction());
			}
		}

		private class SpinningFoodAction implements Action
		{
			private final Block statue;
			private SpinningFood scene;

			private SpinningFoodAction(Block statue)
			{
				this.statue = statue;
				this.scene = null;
			}

			@Override
			public boolean run()
			{
				if (scene == null)
				{
					scene = new SpinningFood(statue.getLocation().clone().add(0, 5, 0), target);
					scene.run();
				}

				return scene.done();
			}
		}
	}
}
