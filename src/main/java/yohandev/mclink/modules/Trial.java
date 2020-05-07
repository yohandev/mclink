package yohandev.mclink.modules;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import yohandev.mclink.Utilities;

import java.util.HashSet;

public class Trial
{
	private static final Vector START = new Vector(100, 10, 100); // position of slot#0
	private static final Vector SIZE = new Vector(100, 30, 100);

	private static final HashSet<Integer> slots = new HashSet<>(); // used slots

	public final ItemStack loot;
	public final Player challenger;

	private final int slot;

	public Trial(Material loot, Player challenger)
	{
		this.loot = new ItemStack(loot);
		this.challenger = challenger;

		int i = 0;
		while (slots.contains(i))
		{
			i++;
		}
		slots.add(i);

		this.slot = i;
	}

	public void begin()
	{
		generate(); // reset arena
		teleport(); // teleport challenger
		cutscene(); // dun dun dun!!!
	}

	private void generate()
	{
		Vector pos = new Vector(START.getX() + SIZE.getX() * slot, START.getY(), START.getZ());
		Vector size = pos.clone().add(SIZE);

		for (int x = pos.getBlockX(); x < size.getX(); x++)
		{
			for (int y = pos.getBlockY(); y < size.getY(); y++)
			{
				for (int z = pos.getBlockZ(); z < size.getZ(); z++)
				{
					Material mat = Material.AIR;

					if (x == pos.getBlockX() || x == size.getX() - 1
						|| y == pos.getBlockY() || y == size.getY() - 1
						|| z == pos.getBlockZ() || z == size.getZ() - 1
					)
					{
						mat = Material.BEDROCK;
					}
					challenger.getWorld().getBlockAt(x, y, z).setType(mat);
				}
			}
		}

		challenger.getWorld().getBlockAt(center()).setType(Material.GLOWSTONE);
		challenger.getWorld().dropItem(center().add(0, 1, 0), loot);
	}

	private void teleport()
	{
		challenger.teleport(center());
	}

	private void cutscene()
	{
		new TrialCutscene(challenger, center(), loot.getItemMeta().getDisplayName()).run();
	}

	private Location center()
	{
		Vector pos = new Vector(START.getX() + SIZE.getX() * (slot + 0.5), START.getY() + 1, START.getZ() + SIZE.getZ() * 0.5);

		return new Location(challenger.getWorld(), pos.getX(), pos.getY(), pos.getZ());
	}

	private static class TrialCutscene extends Cutscene
	{
		public TrialCutscene(Player target, Location center, String name)
		{
			super(target);

			Vector dir = BlockFace.NORTH.getDirection();

			/* pan */
			super.async(new LerpAction(Utilities.add(center, dir, 5), Utilities.add(center, dir, 2), 320));
			super.async(new LookAtAction(center, 300));

			super.sync(new WaitAction(30));

			/* dialogue */
			super.sync(new SoundAction(Sound.AMBIENT_CAVE));
			super.sync(new DialogueAction("To whom sets food in this shrine...", 100));

			super.sync(new SoundAction(Sound.AMBIENT_CAVE));
			super.sync(new DialogueAction("In the name of the gods, I offer this trial.", 100));

			super.sync(new SoundAction(Sound.ENTITY_ENDER_DRAGON_GROWL));
			super.sync(new TitleAction("Trial of the " + ChatColor.AQUA + "" + ChatColor.BOLD + name, 70));

			super.sync(new AwaitAction());
		}
	}
}
