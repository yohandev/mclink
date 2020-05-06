package yohandev.mclink;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class Utilities
{
	public static Location RandomLocation(Location center, double radius, boolean abs)
	{
		World w = center.getWorld();
		Vector r = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
		Vector s = r.normalize().multiply(radius).add(center.toVector());

		if (abs)
		{
			s = s.setY(center.getWorld().getHighestBlockAt(s.getBlockX(), s.getBlockZ()).getY() + 1);
		}
		return new Location(w, s.getX(), s.getY(), s.getZ());
	}
}
