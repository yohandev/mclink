package yohandev.mclink;

import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

public class ShrineGenerator extends BlockPopulator implements Listener
{
	public static double SPAWN_CHANCE = 0.001;

	@Override
	public void populate(World world, Random random, Chunk chunk)
	{
		if (random.nextInt() > (Integer.MAX_VALUE * SPAWN_CHANCE))
		{
			return; // no shrine
		}

		int x = random.nextInt(15);
		int z = random.nextInt(15);
		int y;
		for (y = world.getMaxHeight() - 1; chunk.getBlock(x, y, z).getType() == Material.AIR; y--);

		if (chunk.getBlock(x, y, z).getType() != Material.GRASS_BLOCK)
		{
			return; // only spawns on grass
		}

		/* gen shrine */
		chunk.getBlock(x, y + 1, z).setType(Material.STONE_BRICKS); // base
		chunk.getBlock(x, y + 2, z).setType(Material.TURTLE_EGG); // orb
	}

	@EventHandler
	public void onWorldInit(WorldInitEvent event)
	{
		World world = event.getWorld();

		if (world.getName().contains("end") || world.getName().contains("nether"))
		{
			return;
		}

		world.getPopulators().add(this);
	}
}
