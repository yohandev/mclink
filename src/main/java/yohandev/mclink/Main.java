package yohandev.mclink;

import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import yohandev.mclink.listeners.HealthHungerListener;
import yohandev.mclink.listeners.ShrineListener;

public final class Main extends JavaPlugin
{
	public static Main instance;

	@Override
	public void onEnable()
	{
		instance = this;

		Config.load();

		// Listener
		register(new HealthHungerListener(), "mclink");
		register(new ShrineGenerator());
		register(new ShrineListener());
	}

	@Override
	public void onDisable()
	{
		Config.unload();
	}

	private void register(Listener l)
	{
		this.getServer().getPluginManager().registerEvents(l, this);
	}

	private void register(String cmd, CommandExecutor exe)
	{
		this.getCommand(cmd).setExecutor(exe);
	}

	private void register(Object obj, String cmd)
	{
		register((Listener)obj);
		register(cmd, (CommandExecutor)obj);
	}
}
