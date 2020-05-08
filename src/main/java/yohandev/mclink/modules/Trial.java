package yohandev.mclink.modules;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import yohandev.mclink.NBT;
import yohandev.mclink.Utilities;

import java.util.HashSet;
import java.util.Random;

public class Trial implements Listener, CommandExecutor
{
	public static final int ARENA_RADIUS = 40;
	public static final int ARENA_HEIGHT = 60;
	public static final int ARENA_0 = (Integer.MIN_VALUE / 200) + (ARENA_RADIUS * 2);

	public static final String TAG = "trialwarp";

	private static final HashSet<Integer> slots = new HashSet<>(); // used slots

	@Override // TODO TEMPORARY
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player))
		{
			return false;
		}

		Entity s = Utilities.floatingItem(Material.DIAMOND_SWORD, ((Player) sender).getLocation());

		NBT.add(s, TAG);

		return true;
	}

	@EventHandler
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e)
	{
		if (!(e.getRightClicked() instanceof ArmorStand))
		{
			return;
		}
		if (!NBT.has(e.getRightClicked(), TAG))
		{
			return;
		}

		ItemStack item = ((ArmorStand) e.getRightClicked()).getEquipment().getHelmet();
		Player player = e.getPlayer();
		Location where = e.getRightClicked().getLocation();

		System.out.println(where);

		new TrialCutscene(player, item, where).run();

		e.setCancelled(true);
	}

	private static int getSlot()
	{
		int i = 0;
		while (slots.contains(i))
		{
			i++;
		}
		slots.add(i);

		return i;
	}

	private static int xz(int slot)
	{
		return ARENA_0 + (slot * 2 * ARENA_RADIUS);
	}

	private static int y(Player p)
	{
		return p.getWorld().getMaxHeight() - ARENA_HEIGHT;
	}

	private static class TrialCutscene extends Cutscene
	{
		public TrialCutscene(Player p, ItemStack item, Location warp)
		{
			super(p);

			final int slot = getSlot();

			String name = Utilities.name(item);
			Location center = new Location(p.getWorld(), xz(slot), y(p), xz(slot));
			Vector dir = BlockFace.NORTH.getDirection();

			/* generate arena */
			super.async(new GenerateAction(center));

			/* pre-trial dialogue */
			super.async(new LerpAction(Utilities.add(warp, dir, 5), Utilities.add(warp, dir, 2), 200, false));
			super.async(new LookAtAction(warp, 200));

			super.sync(new WaitAction(20));

			super.sync(new SoundAction(Sound.AMBIENT_CAVE));
			super.sync(new DialogueAction("This item once belonged to a great warrior...", 100));

			super.sync(new SoundAction(Sound.AMBIENT_CAVE));
			super.sync(new DialogueAction("Before you may wield it, the gods demand a test of strength.", 100));

			super.sync(new AwaitAction());

			center = Utilities.safe(center);

			/* pan */
			super.async(new LerpAction(Utilities.add(center, dir, 5).add(0, 10, 0), Utilities.add(center, dir, 2).add(0, 2, 0), 320, false));
			super.async(new LookAtAction(center, 300));

			super.sync(new WaitAction(30));

			/* dialogue */
			super.sync(new SoundAction(Sound.AMBIENT_CAVE));
			super.sync(new DialogueAction("To whom sets foot in this shrine...", 100));

			super.sync(new SoundAction(Sound.AMBIENT_CAVE));
			super.sync(new DialogueAction("In the name of the gods, I offer this trial.", 100));

			super.sync(new SoundAction(Sound.ENTITY_ENDER_DRAGON_GROWL));
			super.sync(new TitleAction("Trial of the " + ChatColor.AQUA + "" + ChatColor.BOLD + name, 70));

			/* dialogue done */
			super.sync(new AwaitAction());

			/* start fighting */
			super.sync(new TeleportAction(center, true));
			super.sync(new GameModeAction(GameMode.SURVIVAL));

			/* temporary buffer time */
			super.sync(new WaitAction(1000));

			/* un-reserve slot */
			super.sync(new RunnableAction(() -> slots.remove(slot)));
		}

		private class GenerateAction implements Action
		{
			public static final int LAYERS_PER_TICK = 1;

			public final SimplexOctaveGenerator noise;

			public final Location center;
			public final int xlow, xhigh;
			public final int zlow, zhigh;
			public final int ylow, yhigh;

			private int layer;

			private GenerateAction(Location center)
			{
				this.noise = new SimplexOctaveGenerator(new Random(), 8);
				this.noise.setScale(0.005);
				this.center = center;

				this.xlow = center.getBlockX() - ARENA_RADIUS;
				this.xhigh = center.getBlockX() + ARENA_RADIUS;
				this.zlow = center.getBlockZ() - ARENA_RADIUS;
				this.zhigh = center.getBlockZ() + ARENA_RADIUS;
				this.ylow = center.getBlockY();
				this.yhigh = center.getBlockY() + ARENA_RADIUS;
				this.layer = ylow;
			}

			@Override
			public boolean run()
			{
				/* gen layer(S) for this tick */
				for (int t = 0; t < LAYERS_PER_TICK; t++, layer++)
				{
					if (layer > yhigh)
					{
						/* center feature */
						Utilities.safe(center).getBlock().setType(Material.GLOWSTONE);

						return true;
					}

					/* gen layer */
					for (int x = xlow; x <= xhigh; x++)
					{
						for (int z = zlow; z <= zhigh; z++)
						{
							Material mat = Material.AIR;

							// grass
							if (layer <= ylow + noise.noise(x, z, 0.5, 0.5, true) * 7.0 + 8)
							{
								mat = Material.GRASS_BLOCK;
							}

							// bedrock
							if (x == xlow || x == xhigh || layer == ylow || z == zlow || z == zhigh)
							{
								mat = Material.BEDROCK;
							}

							// barrier
							if (layer == yhigh)
							{
								mat = Material.BARRIER;
							}

							center.getWorld().getBlockAt(x, layer, z).setType(mat);
						}
					}
				}
				return false;
			}
		}
	}

	public static class Generator extends BlockPopulator
	{
		public static double SPAWN_CHANCE = 0.025;

		@Override
		public void populate(World world, Random random, Chunk chunk)
		{
			if (random.nextDouble() > SPAWN_CHANCE)
			{
				return; // no trial
			}

			int x = random.nextInt(13) + 1;
			int z = random.nextInt(13) + 1;
			int y;
			for (y = world.getMaxHeight() - 1; chunk.getBlock(x, y, z).getType() == Material.AIR; y--);

			if (chunk.getBlock(x, y, z).getType() == Material.WATER)
			{
				return;
			}

			/* clear prev. gen */
			for (Entity e : chunk.getEntities())
			{
				if (NBT.has(e, TAG))
				{
					e.remove();
				}
			}

			/* gen shrine */
			chunk.getBlock(x, y + 1, z).setType(Material.ANDESITE); // pedestal

			chunk.getBlock(x + 1, y + 1, z).setType(Material.MOSSY_STONE_BRICK_SLAB); // stair
			chunk.getBlock(x - 1, y + 1, z).setType(Material.STONE_BRICK_SLAB); // stair
			chunk.getBlock(x, y + 1, z + 1).setType(Material.MOSSY_STONE_BRICK_SLAB); // stair
			chunk.getBlock(x, y + 1, z - 1).setType(Material.STONE_BRICK_SLAB); // stair

			chunk.getBlock(x + 1, y + 1, z + 1).setType(Material.STONE_BRICKS); // base
			chunk.getBlock(x - 1, y + 1, z + 1).setType(Material.CRACKED_STONE_BRICKS); // base
			chunk.getBlock(x + 1, y + 1, z - 1).setType(Material.MOSSY_STONE_BRICKS); // base
			chunk.getBlock(x - 1, y + 1, z - 1).setType(Material.STONE_BRICKS); // base

			/* clear area */
			chunk.getBlock(x - 1, y + 2, z - 1).setType(Material.AIR);
			chunk.getBlock(x + 1, y + 2, z - 1).setType(Material.AIR);
			chunk.getBlock(x + 1, y + 2, z + 1).setType(Material.AIR);
			chunk.getBlock(x - 1, y + 2, z + 1).setType(Material.AIR);
			chunk.getBlock(x, y + 3, z).setType(Material.AIR);

			/* armor stand */
			Location loc = new Location(world, (chunk.getX() * 16) + x + 0.4, y + 0.25, (chunk.getZ() * 16) + z + 0.5);
			Entity s = Utilities.floatingItem(Material.DIAMOND_SWORD, loc);
			NBT.add(s, TAG);
		}
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
