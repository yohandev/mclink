package yohandev.mclink;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import yohandev.mclink.modules.Health;
import yohandev.mclink.modules.Stamina;
import yohandev.mclink.modules.Statue;
import yohandev.mclink.modules.Trial;

public final class Main extends JavaPlugin
{
	public static Main instance;

	@Override
	public void onEnable()
	{
		MythicMobs.inst().load();

		instance = this;

		// Listener
		register(new Health());
		register(new Stamina());
		register(new Statue());
		register(new Trial(), "trial");
	}

	public void register(Object obj, String cmd)
	{
		if (obj instanceof Listener)
		{
			this.getServer().getPluginManager().registerEvents((Listener) obj, this);
		}
		if (obj instanceof CommandExecutor)
		{
			this.getCommand(cmd).setExecutor((CommandExecutor) obj);
		}
	}

	public void register(Object obj)
	{
		register(obj, "");
	}

	public static void command(String cmd)
	{
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}


}
