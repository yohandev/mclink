package yohandev.mclink.modules;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import yohandev.mclink.Utilities;

import java.util.HashSet;
import java.util.Random;

public class Trial implements Listener, CommandExecutor
{
	public static final int ARENA_RADIUS = 40;
	public static final int ARENA_HEIGHT = 60;
	public static final int ARENA_0 = (Integer.MIN_VALUE / 200) + (ARENA_RADIUS * 2);

	private static final HashSet<Integer> slots = new HashSet<>(); // used slots

	@Override // TODO TEMPORARY
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (!(sender instanceof Player))
		{
			return false;
		}
		start((Player) sender, new ItemStack(Material.DIAMOND_AXE));

		return true;
	}

	@EventHandler
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e)
	{
		if (!(e.getRightClicked() instanceof ArmorStand))
		{
			return;
		}
		if (e.getRightClicked().)
	}

	private void start(Player p, ItemStack loot)
	{

		new TrialCutscene(p, slot, Utilities.name(loot)).run();
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
		public TrialCutscene(Player p, String name)
		{
			super(p);

			final int slot = getSlot();

			Location center = new Location(p.getWorld(), xz(slot), y(p), xz(slot));
			Vector dir = BlockFace.NORTH.getDirection();

			/* pre-trial dialogue */


			/* generate arena */ // TODO do this during the pre-trial dialogue
			super.async(new ChatAction("Generating arena...", 10));
			super.async(new GenerateAction(center));
			super.sync(new AwaitAction());

			center = Utilities.safe(center);

			/* pan */
			super.async(new LerpAction(Utilities.add(center, dir, 5).add(0, 10, 0), Utilities.add(center, dir, 2).add(0, 2, 0), 320, false));
			super.async(new LookAtAction(center, 300));

			super.sync(new WaitAction(30));

			/* dialogue */
			super.sync(new SoundAction(Sound.AMBIENT_CAVE));
			super.sync(new DialogueAction("To whom sets food in this shrine...", 100));

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
			public static final int LAYERS_PER_TICK = 3;

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
}
