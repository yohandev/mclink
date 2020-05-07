package yohandev.mclink.modules;

import org.bukkit.*;
import org.bukkit.entity.*;
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
	private static final HashSet<Entity> reserved = new HashSet<>();

	protected Entity target;
	protected Location start;

	private Queue<Action> actions;
	private List<Action> await;

	public Cutscene(Entity target)
	{
		this.target = target;
		this.start = target.getLocation().clone();

		this.actions = new LinkedList<>();
		this.await = new ArrayList<>();
	}

	public void run()
	{
		// one cut-scene at a time per entity
		if (reserved.contains(target))
		{
			return;
		}
		reserved.add(target);

		// spectator mode
		if (target instanceof Player)
		{
			((Player) target).setGameMode(GameMode.SPECTATOR);
		}

		// save location to revert back
		start = target.getLocation().clone();

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				if (actions.isEmpty())
				{
					// un-reserve
					reserved.remove(target);

					// survival mode
					if (target instanceof Player)
					{
						((Player) target).setGameMode(GameMode.SURVIVAL);
					}

					// revert location
					target.teleport(start);

					// stop loop
					cancel();
				}
				else if (actions.peek().run())
				{
					actions.remove();
				}
			}
		}
		.runTaskTimer(Main.instance, 0, 1);
	}

	protected void sync(Action a)
	{
		this.actions.add(a);
	}

	protected void async(Action a)
	{
		sync(new AsyncActionWrapper(a));
	}

	protected void append(Cutscene c) // cutscene to play when this is done
	{
		c.target = this.target;
		c.start = this.start;
		c.await = this.await;

		this.actions.addAll(c.actions);
	}

	public boolean done()
	{
		return this.actions.isEmpty();
	}

	public interface Action
	{
		boolean run();
	}

	private class AsyncActionWrapper implements Action
	{
		private final Action action;

		private AsyncActionWrapper(Action action)
		{
			this.action = action;
		}

		public boolean run()
		{
			await.add(action);

			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					if (action.run())
					{
						cancel(); // done
						await.remove(action);
					}
				}
			}
			.runTaskTimer(Main.instance, 0, 1);

			return true;
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
		public boolean run()
		{
			return count-- <= 0;
		}
	}

	protected class AwaitAction implements Action
	{
		@Override
		public boolean run()
		{
			return await.isEmpty();
		}
	}

	protected class RunnableAction implements Action
	{
		public final Runnable action;

		public RunnableAction(Runnable action)
		{
			this.action = action;
		}

		@Override
		public boolean run()
		{
			action.run();

			return true;
		}
	}

	protected class LerpAction implements Action
	{
		public final Location from;

		public final Entity ent;
		public final Location loc;

		public final double dt;
		private double t;

		public final boolean reset;

		public LerpAction(Location from, Location to, long time, boolean reset)
		{
			this.from = from;
			this.loc = to;
			this.ent = null;

			this.dt = 1.0 / time;
			this.t = 0;

			this.reset = reset;
		}

		public LerpAction(Location from, Location to, long time)
		{
			this(from, to, time, true);
		}

		public LerpAction(Location from, Entity to, long time, boolean reset)
		{
			this.from = from;
			this.loc = null;
			this.ent = to;

			this.dt = 1.0 / time;
			this.t = 0;

			this.reset = reset;
		}

		public LerpAction(Location from, Entity to, long time)
		{
			this(from, to, time, true);
		}

		@Override
		public boolean run()
		{
			Location to = ent == null ? loc : ent.getLocation();

			if ((t += dt) <= 1)
			{
				target.teleport(Utilities.lerp(from, to, t));
			}
			else if (reset)
			{
				target.teleport(start.clone());
			}

			return t >= 1;
		}
	}

	protected class LookAtAction implements Action
	{
		public final Entity ent;
		public final Location loc;

		private long time;

		public LookAtAction(Location at, long time)
		{
			this.loc = at;
			this.ent = null;

			this.time = time;
		}

		public LookAtAction(Entity at, long time)
		{
			this.loc = null;
			this.ent = at;

			this.time = time;
		}

		@Override
		public boolean run()
		{
			Location look = target.getLocation();
			Location at = ent == null ? loc : ent.getLocation();

			Vector dir = at.clone().subtract(look).toVector().normalize();

			target.teleport(look.clone().setDirection(dir));

			return time-- <= 0;
		}
	}

	protected class SpinAction implements Action
	{
		public final float speed;

		private long time;
		private float yaw;

		public SpinAction(float speed, long time)
		{
			this.speed = speed;
			this.time = time;
			this.yaw = 0;
		}

		@Override
		public boolean run()
		{
			Location l = target.getLocation().clone();
			l.setYaw(yaw += speed);

			target.teleport(l);

			return time-- <= 0;
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
		public boolean run()
		{
			if (count == time)
			{
				target.sendMessage(message);
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
		public boolean run()
		{
			if (count == time || (count % 25 == 0 && count >= FADE)) // send it repeating, with minimal redundancy
			{
				Main.command("title " +  target.getName() + " actionbar {\"text\":\"" + message + "\"}");
			}

			return count-- <= 0;
		}
	}

	protected class TitleAction extends ChatAction
	{
		public static final long FADE = 20;

		public TitleAction(String message, long time)
		{
			super(message, time);
		}

		@Override
		public boolean run()
		{
			if (count == time)
			{
				Main.command("title " + target.getName() + " times " + FADE + " " + (time - FADE * 2) + " " + FADE);
				Main.command("title " +  target.getName() + " title {\"text\":\"" + message + "\"}");
			}

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
		public boolean run()
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

		public SoundAction(Sound sound)
		{
			this(sound, null);
		}

		@Override
		public boolean run()
		{
			Location emitter = source == null ? target.getLocation() : source;

			if (target instanceof Player)
			{
				((Player) target).playSound(emitter, sound, 1, 1);
			}
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
		public boolean run()
		{
			if (target instanceof LivingEntity)
			{
				((LivingEntity) target).addPotionEffect(effect);
			}

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
		public boolean run()
		{
			if (!(target instanceof Player))
			{
				return true;
			}
			Player player = (Player) target;

			if (selected >= 0)
			{
				options.get(selected).action.run(); // run selected

				return true;
			}

			if (player.getOpenInventory() != inventory)
			{
				Main.instance.register(this);

				player.openInventory(inventory);
			}

			return false;
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

			((Player) target).closeInventory(); // done
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

	protected class GameModeAction implements Action
	{
		public final GameMode mode;

		public GameModeAction(GameMode mode)
		{
			this.mode = mode;
		}

		@Override
		public boolean run()
		{
			if (target instanceof Player)
			{
				((Player) target).setGameMode(mode);
			}

			return true;
		}
	}

	protected class SuicideAction implements Action
	{
		@Override
		public boolean run()
		{
			target.remove();

			return true;
		}
	}

	protected class TeleportAction implements Action
	{
		public final Location loc;
		public final Entity ent;

		public final boolean reset; // should reset cutscene start?
		public final boolean safe;

		public TeleportAction(Location loc, boolean safe, boolean reset)
		{
			this.loc = loc;
			this.ent = null;

			this.reset = reset;
			this.safe = safe;
		}

		public TeleportAction(Location loc, boolean safe)
		{
			this(loc, safe, false);
		}

		public TeleportAction(Entity ent, boolean safe, boolean reset)
		{
			this.loc = null;
			this.ent = ent;

			this.reset = reset;
			this.safe = safe;
		}

		public TeleportAction(Entity ent, boolean safe)
		{
			this(ent, safe, false);
		}

		@Override
		public boolean run()
		{
			Location to = ent == null ? loc : ent.getLocation();

			if (safe)
			{
				to = Utilities.safe(to);
			}
			if (reset)
			{
				start = to.clone();
			}
			target.teleport(to);

			return true;
		}
	}
}
