package yohandev.mclink;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class Config
{
	public static void load()
	{
		Main.instance.saveDefaultConfig();
	}

	public static void unload()
	{
		Main.instance.saveConfig();
	}

	private static List<Integer> getter(Player player)
	{
		FileConfiguration c = Main.instance.getConfig();
		String uuid = player.getUniqueId().toString();

		if (!c.contains(uuid))
		{
			c.set(uuid, c.getIntegerList("default"));
			Main.instance.saveConfig();
		}

		return c.getIntegerList(uuid);
	}

	public static int hearts(Player player)
	{
		return getter(player).get(0);
	}

	public static int stamina(Player player)
	{
		return getter(player).get(1);
	}

	public static int hearts(Player player, int add)
	{
		FileConfiguration c = Main.instance.getConfig();
		String uuid = player.getUniqueId().toString();

		List<Integer> list = c.getIntegerList(uuid);
		list.set(0, list.get(0) + add);

		c.set(uuid, list);

		Main.instance.saveConfig();
		Main.instance.reloadConfig();

		return list.get(0);
	}

	public static int stamina(Player player, int add)
	{
		FileConfiguration c = Main.instance.getConfig();
		String uuid = player.getUniqueId().toString();

		List<Integer> list = c.getIntegerList(uuid);
		list.set(1, list.get(1) + add);

		c.set(uuid, list);

		Main.instance.saveConfig();
		Main.instance.reloadConfig();

		return list.get(1);
	}
}
