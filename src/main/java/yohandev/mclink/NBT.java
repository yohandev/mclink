package yohandev.mclink;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

public class NBT
{
	public static void add(Entity e, String tag)
	{
		e.getPersistentDataContainer().set(key(tag), PersistentDataType.INTEGER, 1);
	}

	public static boolean has(Entity e, String tag)
	{
		return e.getPersistentDataContainer().has(key(tag), PersistentDataType.INTEGER);
	}

	public static void remove(Entity e, String tag)
	{
		e.getPersistentDataContainer().remove(key(tag));
	}

	private static NamespacedKey key(String tag)
	{
		return new NamespacedKey(Main.instance, tag);
	}
}
