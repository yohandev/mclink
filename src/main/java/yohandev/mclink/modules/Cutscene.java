package yohandev.mclink.modules;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import yohandev.mclink.Main;
import yohandev.mclink.Utilities;

import java.util.*;

public abstract class Cutscene
{
	private static final HashSet<Player> reserved = new HashSet<>();

	public final Player target;
	public Location start;

	private final Queue<Action> actions;
	private Cutscene chain;

	public Cutscene(Player target)
	{
		this.target = target;
		this.start = target.getLocation().clone();

		this.actions = new LinkedList<>();
	}

	public boolean run()
	{
		if (reserved.contains(target))
		{
			return false;
		}

		reserved.add(target);
		target.setGameMode(GameMode.SPECTATOR);

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				if (actions.isEmpty())
				{
					reserved.remove(target); // unreserve
					target.teleport(start); // reset pos
					target.setGameMode(GameMode.SURVIVAL); // reset gm
					target.setInvulnerable(false);
					target.setFlying(false);

					cancel(); // finish cutscene

					if (chain != null) // chain cutscene
					{
						chain.start = start;
						chain.run();
					}

					return;
				}

				if (actions.peek().run(target))
				{
					actions.remove();
				}
			}
		}.runTaskTimer(Main.instance, 0, 1);

		return true;
	}

	protected void push(Action a)
	{
		this.actions.add(a);
	}

	protected void chain(Cutscene c) // cutscene to play when this is done
	{
		this.chain = c;
	}

	public interface Action
	{
		boolean run(Player p); // returns if done
	}

	protected class PanAction implements Action
	{
		public final Location start, dest;
		public final long time;

		public final Location travel;
		public final Vector delta;
		public final double dist;

		public final boolean async;

		public PanAction(Location start, Location dest, long time, boolean async)
		{
			this.start = start;
			this.dest = dest;
			this.time = time;

			this.travel = dest.clone().subtract(start);
			this.dist = travel.length();
			this.delta = travel.toVector().normalize().multiply(dist / time);

			this.async = async;
		}

		public PanAction(Location center, double radius, boolean abs, long time, boolean async)
		{
			this(Utilities.RandomLocation(center, radius, abs), Utilities.RandomLocation(center, radius, abs), time, async);
		}

		@Override
		public boolean run(Player p)
		{
			p.getLocation().setDirection(delta);

			return async ? runAsync(p) : runSync(p);
		}

		private boolean runAsync(Player p)
		{
			new BukkitRunnable()
			{
				long count = time;

				@Override
				public void run()
				{
					step(p);

					if (count-- <= 0)
					{
						cancel();
					}
				}
			}.runTaskTimer(Main.instance, 0, 1);

			return true; // async auto done
		}

		private boolean runSync(Player p)
		{
			step(p);

			if (p.getLocation().distanceSquared(dest) <= 0.2) // threshold
			{
				return true; // reached destination
			}
			return false; // not done
		}

		private void step(Player p)
		{
			p.teleport(start.add(this.delta));
		}
	}

	protected class LookAtAction implements Action
	{
		public final Location at;
		public final long time;

		public LookAtAction(Location at, long time)
		{
			this.at = at;
			this.time = time;
		}

		@Override
		public boolean run(Player p)
		{
			new BukkitRunnable()
			{
				long count = time;

				@Override
				public void run()
				{
					Location loc = p.getLocation();
					Vector dir = at.clone().subtract(loc).toVector().normalize();

					p.teleport(loc.setDirection(dir));

					if (count-- <= 0)
					{
						cancel();
					}
				}
			}.runTaskTimer(Main.instance, 0, 1);

			return true;
		}
	}

	protected class PanLookAtAction implements Action
	{
		private final PanAction pan;
		private final LookAtAction look;

		public PanLookAtAction(Location target, double radius, long time)
		{
			this.pan = new PanAction(target, radius, true, time, true);
			this.look = new LookAtAction(target, time);
		}

		@Override
		public boolean run(Player p)
		{
			return pan.run(p) && look.run(p);
		}
	}

	protected class ChatAction implements Action
	{
		public final String message;
		public final long time;

		protected long count;

		public ChatAction(String message, long time)
		{
			this.message = message;
			this.time = time;

			this.count = time;
		}

		@Override
		public boolean run(Player p)
		{
			if (count == time)
			{
				p.sendMessage(message);
			}

			return count-- <= 0;
		}
	}

	protected class DialogueAction extends ChatAction
	{
		public static final long FADE = 60;

		public DialogueAction(String message, long time)
		{
			super(message, time);
		}

		@Override
		public boolean run(Player p)
		{
			if (count == time || (count % 25 == 0 && count >= FADE)) // send it repeating, with minimal redundancy
			{
				Main.command("title " +  p.getName() + " actionbar {\"text\":\"" + message + "\"}");
			}

			return count-- <= 0;
		}
	}

	protected class WaitAction implements Action
	{
		public final long time;

		protected long count;

		public WaitAction(long time)
		{
			this.time = time;

			this.count = time;
		}

		@Override
		public boolean run(Player p)
		{
			return count-- <= 0;
		}
	}

	protected class CommandAction implements Action
	{
		public final String command;
		public final long time;

		private long count;

		public CommandAction(String command, long time)
		{
			this.command = command;
			this.time = time;

			this.count = time;
		}

		@Override
		public boolean run(Player p)
		{
			if (count == time)
			{
				Main.command(command);
			}

			return count-- <= 0;
		}
	}

	protected class SoundAction implements Action
	{
		public final Sound sound;
		public final Location source;

		public SoundAction(Sound sound, Location source) // no time, async by default
		{
			this.sound = sound;
			this.source = source;
		}

		@Override
		public boolean run(Player p)
		{
			p.playSound(source == null ? p.getLocation() : source, sound, 1, 1);

			return true;
		}
	}

	protected class PotionAction implements Action
	{
		public final PotionEffect effect;

		public PotionAction(PotionEffectType type, int duration, int amp)
		{
			effect = new PotionEffect(type, duration, amp, false, false, false);
		}

		@Override
		public boolean run(Player p)
		{
			p.addPotionEffect(effect);

			return true;
		}
	}

	protected class PromptAction implements Action, Listener
	{
		public final Inventory inventory;
		public final List<Option> options;
		public final int count;

		private int selected;

		public PromptAction(String name, int count)
		{
			this.inventory = Bukkit.createInventory(null, 9, name);
			this.options = new ArrayList<>(count);
			this.count = count;
			this.selected = -1;
		}

		public PromptAction option(Material material, String name, String lore, Runnable action)
		{
			Option o = new Option(material, name, lore, action);
			int i = (options.size() + 1) * (inventory.getSize() / count) - 2;

			options.add(o);
			inventory.setItem(i, o.item);

			return this;
		}

		public Option selected()
		{
			if (selected < 0)
			{
				return null;
			}
			return this.options.get(selected);
		}

		@Override
		public boolean run(Player p)
		{
			if (p.getOpenInventory() != inventory)
			{
				Main.instance.register(this);

				p.openInventory(inventory);
			}

			return selected >= 0;
		}

		@EventHandler
		public void onInventoryClick(InventoryClickEvent e)
		{
			if (e.getInventory() != inventory)
			{
				return; // not this
			}
			e.setCancelled(true);

			if (selected >= 0)
			{
				return; // already selected
			}

			ItemStack c = e.getCurrentItem();

			if (c == null || c.getType() == Material.AIR)
			{
				return; // empty click
			}

			for (selected = 0; selected < options.size(); selected++)
			{
				if (options.get(selected).item.isSimilar(c))
				{
					break;
				}
			}

			if (selected >= options.size())
			{
				selected = -1;
				return;
			}

			options.get(selected).action.run(); // run selected

			target.closeInventory(); // done
		}

		@EventHandler
		public void onInventoryDrag(InventoryDragEvent e)
		{
			if (e.getInventory() != inventory)
			{
				return; // not this
			}
			e.setCancelled(true);
		}

		public class Option
		{
			public final ItemStack item;
			public final Runnable action;

			public Option(Material material, String name, String lore, Runnable action)
			{
				this.item = new ItemStack(material, 1);
				this.action = action;

				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName(name);
				meta.setLore(Arrays.asList(lore));

				item.setItemMeta(meta);
			}
		}
	}

	protected class QuestItemAction implements Action
	{
		public final long time;

		public final ArmorStand display;

		public final Location start, end;

		public final Location travel;
		public final Vector delta;
		public final double dist;

		public QuestItemAction(Material item, Location start, long time)
		{
			Location end = Cutscene.this.start;

			display = start.getWorld().spawn(start.clone(), ArmorStand.class);

			display.setVisible(false);
			display.setInvulnerable(true);
			display.setBasePlate(false);
			display.setGravity(false);
			display.getEquipment().setHelmet(new ItemStack(item));

			this.start = start.clone().add(0, 5, 0);
			this.end = end;
			this.time = time;

			this.travel = this.end.clone().subtract(this.start);
			this.dist = travel.length();
			this.delta = travel.toVector().normalize().multiply(dist / time);
		}

		@Override
		public boolean run(Player p)
		{
			new BukkitRunnable()
			{
				long count = time;

				@Override
				public void run()
				{
					/* armor stand*/
					start.add(delta);
					start.setYaw(start.getYaw() + 4);

					display.teleport(start);

					/* player */
					Location loc = p.getLocation();
					Vector dir = display.getLocation().clone().subtract(loc).toVector().normalize();

					p.teleport(loc.setDirection(dir));

					if (count-- <= 0)
					{
						display.remove();

						cancel();
					}
				}
			}.runTaskTimer(Main.instance, 0, 1);

			return true; // async auto done
		}
	}

	protected class ResetAction implements Action
	{
		@Override
		public boolean run(Player p)
		{
			target.teleport(start); // reset pos
			target.setInvulnerable(true);
			target.setGameMode(GameMode.SURVIVAL); // reset gm

			return true;
		}
	}

	protected class GameModeAction implements Action
	{
		public final GameMode mode;

		public GameModeAction(GameMode mode)
		{
			this.mode = mode;
		}

		@Override
		public boolean run(Player p)
		{
			target.setGameMode(mode);

			return true;
		}
	}
}
