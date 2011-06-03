package net.minecraft.src.buildcraft.api;

import net.minecraft.src.Entity;
import net.minecraft.src.ModLoader;
import net.minecraft.src.World;

public class APIProxy {

	public static World getWorld () {
		return ModLoader.getMinecraftServerInstance().getWorldManager(0);
	}
	
	public static boolean isClient (World world) {
		return false;
	}
	
	public static boolean isServerSide () {
		return true;
	}
	
	public static Entity getEntity (World world, int entityId) {
		return null;
	}
	
	public static void storeEntity (World world, Entity entity) {
		world.entityJoinedWorld(entity);
	}

	public static void removeEntity (World world, Entity entity) {
		entity.setEntityDead();		
	}
}
