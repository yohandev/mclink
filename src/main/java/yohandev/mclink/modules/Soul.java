package yohandev.mclink.modules;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import yohandev.mclink.Main;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Soul
{
	public static final String CHAT = ChatColor.AQUA + "" + ChatColor.BOLD + "Corrupted Soul";
	private static final NamespacedKey KEY = new NamespacedKey(Main.instance, "soul");

	public static final int COST = 4;

	public static void give(Player p, int amount)
	{
		p.getInventory().addItem(create(amount));
	}

	public static ItemStack create(int amount)
	{
		ItemStack s = new ItemStack(Material.GHAST_TEAR, amount);
		ItemMeta m = s.getItemMeta();

		m.addEnchant(Enchantment.DURABILITY, 1, true);
		m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		m.setDisplayName(CHAT);
		m.setLore(Arrays.asList(ChatColor.WHITE + "Collect " + ChatColor.RED + "four" + ChatColor.WHITE + " and visit a statue to convert these to life force."));
		m.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, 1);
		s.setItemMeta(m);

		return s;
	}

	public static int amount(Player p)
	{
		AtomicInteger i = new AtomicInteger();

		p.getInventory().forEach(s ->
		{
			if (s == null)
			{
				return;
			}
			if (s.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.INTEGER))
			{
				i.addAndGet(s.getAmount());
			}
		});

		return i.get();
	}

	public static boolean takeaway(Player p, int amount)
	{
		if (amount(p) < amount)
		{
			return false;
		}

		AtomicInteger left = new AtomicInteger(amount);

		p.getInventory().forEach(s ->
		{
			if (s == null)
			{
				return;
			}
			if (s.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.INTEGER))
			{
				int before = s.getAmount();
				int after = Math.max(0, before - left.get());

				left.set(after - before); // delta

				s.setAmount(after);

				if (after == 0)
				{
					p.getInventory().remove(s);
				}
			}
		});

		return true;
	}
}
