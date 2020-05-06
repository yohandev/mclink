package yohandev.mclink;

import org.bukkit.Bukkit;

public class Scoreboard
{
	private static org.bukkit.scoreboard.Scoreboard scoreboard;

	private static void init(String objective)
	{
		if (scoreboard == null)
		{
			scoreboard = Bukkit.getScoreboardManager().getMainScoreboard(); // scoreboard
		}
		if (scoreboard.getObjective(objective) == null)
		{
			scoreboard.registerNewObjective(objective, "dummy", objective); // objective
		}
	}

	public static int get(String objective, String player)
	{
		init(objective);

		return scoreboard.getObjective(objective).getScore(player).getScore();
	}

	public static void set(String objective, String player, int value)
	{
		init(objective);

		scoreboard.getObjective(objective).getScore(player).setScore(value);
	}

	public static void add(String objective, String player, int amount)
	{
		init(objective);

		set(objective, player, get(objective, player) + amount);
	}
}
