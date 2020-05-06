package yohandev.mclink;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class Utilities
{
	public static Location RandomLocation(Location center, double radius, boolean abs)
	{
		World w = center.getWorld();
		Vector s = Vector.getRandom().normalize().multiply(radius).add(center.toVector());

		if (abs)
		{
			return new Location(w, s.getX(), Math.abs(s.getY()), s.getZ());
		}
		return new Location(w, s.getX(), s.getY(), s.getZ());
	}
}
