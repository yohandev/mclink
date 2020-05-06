package yohandev.mclink.modules;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import yohandev.mclink.Main;
import yohandev.mclink.Utilities;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public abstract class Cutscene
{
	private static final HashSet<Player> reserved = new HashSet<>();

	public final Player target;
	public final Location start;

	private final Queue<Action> actions;

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
					reserved.remove(target);
					target.setGameMode(GameMode.SURVIVAL); // reset gm
					target.teleport(start);

					cancel(); // finish cutscene

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

	protected class DialogueAction implements Action
	{
		public final String message;
		public final long time;

		private long count;
		private boolean sent;

		public DialogueAction(String message, long time)
		{
			System.out.println("heyo");

			this.message = message;
			this.time = time;

			this.count = time;
			this.sent = false;
		}

		@Override
		public boolean run(Player p)
		{
			if (!sent)
			{
				p.sendMessage(message);
				sent = true;
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
		public boolean run(Player p)
		{
			if (count == time)
			{
				p.performCommand(command);
			}

			return count-- <= 0;
		}
	}
}
