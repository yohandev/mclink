package yohandev.mclink;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
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

	public static Location lerp(Location a, Location b, double t)
	{
		Vector l = lerp(a.toVector(), b.toVector(), t);

		return new Location(a.getWorld(), l.getX(), l.getY(), l.getZ());
	}

	public static Vector lerp(Vector a, Vector b, double t)
	{
		double ax = a.getX();
		double ay = a.getY();
		double az = a.getZ();

		double bx = b.getX();
		double by = b.getY();
		double bz = b.getZ();

		return new Vector(ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t);
	}

	public static Location add(Location a, Vector b)
	{
		return add(a, b, 1);
	}

	public static Location add(Location a, Vector b, double s)
	{
		Vector v = a.toVector().add(b.clone().multiply(s));

		return new Location(a.getWorld(), v.getX(), v.getY(), v.getZ());
	}

	public static Entity FloatingItem(Material type, Location loc)
	{
		ArmorStand stand = loc.getWorld().spawn(loc.clone(), ArmorStand.class);

		stand.setVisible(false);
		stand.setInvulnerable(true);
		stand.setBasePlate(false);
		stand.setGravity(false);
		stand.getEquipment().setHelmet(new ItemStack(type));

		return stand;
	}
}
