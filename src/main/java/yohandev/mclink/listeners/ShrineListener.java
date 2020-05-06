package yohandev.mclink.listeners;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import yohandev.mclink.SpiritOrb;

import java.util.HashMap;

public class ShrineListener implements Listener
{
	private HashMap<Player, ShrineInteraction> m_interactions = new HashMap<>();



	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
		{
			return; // not right click
		}

		if (e.getClickedBlock().getType() != Material.TURTLE_EGG)
		{
			return; // not bell
		}

		if (!m_interactions.containsKey(e.getPlayer()) || m_interactions.get(e.getPlayer()).timedOut())
		{
			m_interactions.put(e.getPlayer(), new ShrineInteraction()); // new interaction
		}
		else if (!m_interactions.get(e.getPlayer()).delayed())
		{
			return; // clicks too soon
		}

		ShrineInteraction i = m_interactions.get(e.getPlayer()); // current interaction

		Effect(e, Effect.VILLAGER_PLANT_GROW); // vfx
		Sound(e, Sound.AMBIENT_CAVE); // sfx
		i.message(e.getPlayer()); // dialogue
		i.step(e.getPlayer()); // next

		if (i.done())
		{
			m_interactions.remove(e.getPlayer());
			SpiritOrb.give(e.getPlayer(), 5);
		}

		e.setCancelled(true);
	}

	private static void Effect(PlayerInteractEvent evt, Effect eff)
	{
		evt.getPlayer().getWorld().playEffect(evt.getClickedBlock().getLocation(), eff, 0);
	}

	private static void Sound(PlayerInteractEvent evt, Sound s)
	{
		evt.getPlayer().playSound(evt.getClickedBlock().getLocation(), s, 1, 1);
	}

	private class ShrineInteraction
	{
		public static final int TIMEOUT = 20000; // 20 seconds
		public static final int DELAY = 500; // 0.5 seconds between clicks

		private final String SPIRIT_ORBS = SpiritOrb.CHAT + "s" + ChatColor.WHITE;
		private final ShrineDialogue[] DIALOGUE =
		{
			new ShrineDialogue() // 00 :: done
			{
				@Override
				public String message(Player p) { return null; }

				@Override
				public int next(Player p) { return -1; }
			},
			new ShrineDialogue() // 01 :: dialogue 0
			{
				@Override
				public String message(Player p) { return "Traveler..."; }

				@Override
				public int next(Player p) { return 2; }
			},
			new ShrineDialogue() // 02 :: dialogue 0
			{
				@Override
				public String message(Player p) { return "I sense you've collected " + SPIRIT_ORBS + "."; }

				@Override
				public int next(Player p) { return 3; }
			},
			new ShrineDialogue() // 03 :: dialogue 1
			{
				@Override
				public String message(Player p) { return "I can offer you great power."; }

				@Override
				public int next(Player p) { return (SpiritOrb.amount(p) >= 4 ? 5 : 4); }
			},
			new ShrineDialogue() // 04:: branch 0, dialogue 0
			{
				@Override
				public String message(Player p) { return "But you do not yet have " + ChatColor.RED + "four " + SPIRIT_ORBS + "."; }

				@Override
				public int next(Player p) { return 0; }
			},
			new ShrineDialogue() // 05 :: branch 1, dialogue 0
			{
				@Override
				public String message(Player p) { return "With the power of " + ChatColor.RED + "four " + SPIRIT_ORBS + ", I can grant you an additional heart or stamina vessel."; }

				@Override
				public int next(Player p) { return 6; }
			},
			new ShrineDialogue() // 06 :: branch 1, dialogue 1
			{
				@Override
				public String message(Player p)
				{
					p.performCommand("tellraw @p [\"\",{\"text\":\"Choose wisely...\\n\"},{\"text\":\"[\\u2665]\",\"color\":\"dark_red\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/mclink heart\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Heart Vessel\"}},{\"text\":\" or \"},{\"text\":\"[\\u2615]\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/mclink stamina\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Stamina Vessel\"}}]");
					return null;
				}

				@Override
				public int next(Player p)
				{
					return 0;
				}
			}
		};

		public int dialogue = 1;
		public long time = System.currentTimeMillis();

		public void step(Player p)
		{
			dialogue = DIALOGUE[dialogue].next(p);
			System.out.println(dialogue);
			time = System.currentTimeMillis();
		}

		public void message(Player player)
		{
			String msg = DIALOGUE[dialogue].message(player);
			if (msg != null)
			{
				player.sendMessage(msg);
			}
		}

		public boolean timedOut()
		{
			return System.currentTimeMillis() - time >= TIMEOUT;
		}

		public boolean delayed()
		{
			return System.currentTimeMillis() - time >= DELAY;
		}

		public boolean done()
		{
			return dialogue < 0;
		}
	}

	private interface ShrineDialogue
	{
		String message(Player p);
		int next(Player p);
	}
}
