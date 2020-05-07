package yohandev.mclink.modules;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.Vector;
import yohandev.mclink.Utilities;

import java.util.Random;

public class Statue implements Listener
{
	public static class StatueCutscene extends Cutscene
	{
		public static final String SOULS = Soul.CHAT + "s" + ChatColor.WHITE;

		public StatueCutscene(PlayerInteractEvent e)
		{
			super(e.getPlayer());

			Block statue = e.getClickedBlock();
			Location pos = statue.getLocation().clone();
			Vector dir = ((Directional) statue.getBlockData()).getFacing().getDirection();

			/* pan */
			super.async(new LerpAction(Utilities.add(pos, dir, 5), Utilities.add(pos, dir, 2), 400));
			super.async(new LookAtAction(pos, 400));

			/* dialogue */
			super.sync(new SoundAction(Sound.AMBIENT_CAVE, pos));
			super.sync(new DialogueAction("Traveler...", 100));

			super.sync(new SoundAction(Sound.AMBIENT_CAVE, pos));
			super.sync(new DialogueAction("I sense you've collected " + SOULS + ".", 100));

			super.sync(new SoundAction(Sound.AMBIENT_CAVE, pos));
			super.sync(new DialogueAction("I can offer you great power.", 100));

			/* test */
			if (Soul.amount((Player) target) < Soul.COST)
			{
				super.sync(new SoundAction(Sound.AMBIENT_CAVE, pos));
				super.sync(new DialogueAction("But you do not yet have " + ChatColor.RED + "four " + SOULS + ".", 100));

				Soul.give(e.getPlayer(), 60);

				return; // done
			}

			super.sync(new SoundAction(Sound.AMBIENT_CAVE, pos));
			super.sync(new DialogueAction("In exchange for " + ChatColor.RED + "four " + SOULS + ", I will amplify your being.", 100));

			/* selection */
			super.sync
			(
				new PromptAction("Choose wisely...", 3)
					.option(Material.COMMAND_BLOCK_MINECART, "Heart Container", "Increase your maximum amount of hearts by one.", () -> append(new Health.HeartCutscene((Player) target, statue)))
					.option(Material.STRUCTURE_VOID, "Stamina Vessel", "Increase your maximum amount of stamina by one.", () -> append(new Stamina.StaminaCutscene((Player) target, statue)))
					.option(Material.BARRIER, "Cancel", "Nevermind...", () -> {})
			);
		}
	}

	public static class Generator extends BlockPopulator
	{
		public static double SPAWN_CHANCE = 0.005;
		public static double SPAWN_RADIUS = 64;

		@Override
		public void populate(World world, Random random, Chunk chunk)
		{
			double chance = SPAWN_CHANCE;

			if (world.getSpawnLocation().distance(chunk.getBlock(0, 0, 0).getLocation()) < SPAWN_RADIUS)
			{
				chance *= 50;
			}

			if (random.nextDouble() > chance)
			{
				return; // no shrine
			}

			int x = random.nextInt(13) + 1;
			int z = random.nextInt(13) + 1;
			int y;
			for (y = world.getMaxHeight() - 1; chunk.getBlock(x, y, z).getType() == Material.AIR; y--);

			if (chunk.getBlock(x, y, z).getType() == Material.WATER)
			{
				return;
			}

			/* gen shrine */
			chunk.getBlock(x, y + 1, z).setType(Material.STONE_BRICKS); // base
			chunk.getBlock(x, y + 2, z).setType(Material.COMMAND_BLOCK); // statue

			/* clear area */
			chunk.getBlock(x - 1, y + 2, z - 1).setType(Material.AIR);
			chunk.getBlock(x + 1, y + 2, z - 1).setType(Material.AIR);
			chunk.getBlock(x + 1, y + 2, z + 1).setType(Material.AIR);
			chunk.getBlock(x - 1, y + 2, z + 1).setType(Material.AIR);
			chunk.getBlock(x, y + 3, z).setType(Material.AIR);
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return; // not right click
		}

		if (e.getClickedBlock().getType() != Material.COMMAND_BLOCK)
		{
			return; // not bell
		}

		new StatueCutscene(e).run();

		e.setCancelled(true);
	}

	@EventHandler
	public void onWorldInit(WorldInitEvent event)
	{
		World world = event.getWorld();

		if (world.getName().contains("end") || world.getName().contains("nether"))
		{
			return;
		}

		world.getPopulators().add(new Generator());
	}
}
