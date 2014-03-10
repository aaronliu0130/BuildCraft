/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core.blueprints;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import buildcraft.api.blueprints.IBuilderContext;
import buildcraft.api.blueprints.MappingRegistry;
import buildcraft.api.blueprints.Schematic;
import buildcraft.api.blueprints.SchematicRegistry;
import buildcraft.builders.blueprints.BlueprintId;
import buildcraft.core.Box;
import buildcraft.core.Version;
import buildcraft.core.utils.BCLog;

public abstract class BlueprintBase {

	public Schematic contents[][][];
	public int anchorX, anchorY, anchorZ;
	public int sizeX, sizeY, sizeZ;
	public BlueprintId id = new BlueprintId();
	public String author;
	private String version = "";
	protected MappingRegistry mapping = new MappingRegistry();

	private byte [] data;

	public BlueprintBase() {
	}

	public BlueprintBase(int sizeX, int sizeY, int sizeZ) {
		contents = new Schematic[sizeX][sizeY][sizeZ];

		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;

		anchorX = 0;
		anchorY = 0;
		anchorZ = 0;
	}

	public void setBlock(int x, int y, int z, Block block) {
		if (contents[x][y][z] == null) {
			contents[x][y][z] = SchematicRegistry.newSchematic(block);
		}

		contents[x][y][z].block = block;
	}

	public void rotateLeft(BptContext context) {
		Schematic newContents[][][] = new Schematic[sizeZ][sizeY][sizeX];

		for (int x = 0; x < sizeZ; ++x) {
			for (int y = 0; y < sizeY; ++y) {
				for (int z = 0; z < sizeX; ++z) {
					newContents[x][y][z] = contents[z][y][(sizeZ - 1) - x];

					if (newContents[x][y][z] != null) {
						try {
							newContents[x][y][z].rotateLeft(context);
						} catch (Throwable t) {
							// Defensive code against errors in implementers
							t.printStackTrace();
							BCLog.logger.throwing("BptBase", "rotateLeft", t);
						}
					}
				}
			}
		}

		int newAnchorX, newAnchorY, newAnchorZ;

		newAnchorX = (sizeZ - 1) - anchorZ;
		newAnchorY = anchorY;
		newAnchorZ = anchorX;

		contents = newContents;
		int tmp = sizeX;
		sizeX = sizeZ;
		sizeZ = tmp;

		anchorX = newAnchorX;
		anchorY = newAnchorY;
		anchorZ = newAnchorZ;

		context.rotateLeft();
	}

	public void writeToNBT (NBTTagCompound nbt) {
		nbt.setString("version", Version.VERSION);

		if (this instanceof Template) {
			nbt.setString("kind", "template");
		} else {
			nbt.setString("kind", "blueprint");
		}

		nbt.setInteger("sizeX", sizeX);
		nbt.setInteger("sizeY", sizeY);
		nbt.setInteger("sizeZ", sizeZ);
		nbt.setInteger("anchorX", anchorX);
		nbt.setInteger("anchorY", anchorY);
		nbt.setInteger("anchorZ", anchorZ);

		if (author != null) {
			nbt.setString("author", author);
		}

		saveContents(nbt);
	}

	public static BlueprintBase loadBluePrint(NBTTagCompound nbt) {
		String kind = nbt.getString("kind");

		BlueprintBase bpt;

		if ("template".equals(kind)) {
			bpt = new Template ();
		} else {
			bpt = new Blueprint();
		}

		bpt.readFromNBT(nbt);

		return bpt;
	}

	public void readFromNBT (NBTTagCompound nbt) {
		BlueprintBase result = null;

		String version = nbt.getString("version");

		sizeX = nbt.getInteger("sizeX");
		sizeY = nbt.getInteger("sizeY");
		sizeZ = nbt.getInteger("sizeZ");
		anchorX = nbt.getInteger("anchorX");
		anchorY = nbt.getInteger("anchorY");
		anchorZ = nbt.getInteger("anchorZ");

		author = nbt.getString("author");

		contents = new Schematic [sizeX][sizeY][sizeZ];

		try {
			loadContents (nbt);
		} catch (BptError e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o.getClass() != getClass())) {
			return false;
		}

		BlueprintBase bpt = (BlueprintBase) o;

		if (sizeX != bpt.sizeX || sizeY != bpt.sizeY || sizeZ != bpt.sizeZ
				|| anchorX != bpt.anchorX || anchorY != bpt.anchorY
				|| anchorZ != bpt.anchorZ) {
			return false;
		}

		for (int x = 0; x < contents.length; ++x) {
			for (int y = 0; y < contents[0].length; ++y) {
				for (int z = 0; z < contents[0][0].length; ++z) {
					if (contents[x][y][z] != null
							&& bpt.contents[x][y][z] == null) {
						return false;
					} else if (contents[x][y][z] == null
							&& bpt.contents[x][y][z] != null) {
						return false;
					} else if (contents[x][y][z] == null
							&& bpt.contents[x][y][z] == null) {
						continue;
					} else if (contents[x][y][z].block != bpt.contents[x][y][z].block) {
						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	public final BlueprintBase clone() {
		BlueprintBase res = null;

		try {
			res = getClass().newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();

			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();

			return null;
		}

		res.anchorX = anchorX;
		res.anchorY = anchorY;
		res.anchorZ = anchorZ;

		res.sizeX = sizeX;
		res.sizeY = sizeY;
		res.sizeZ = sizeZ;

		res.id = id;
		res.author = author;

		res.contents = new Schematic[sizeX][sizeY][sizeZ];

		res.mapping = mapping.clone ();

		for (int x = 0; x < sizeX; ++x) {
			for (int y = 0; y < sizeY; ++y) {
				for (int z = 0; z < sizeZ; ++z) {
					if (contents[x][y][z] != null) {
						res.contents[x][y][z] = contents[x][y][z].clone();
					}
				}
			}
		}

		copyTo(res);

		return res;
	}

	protected void copyTo(BlueprintBase base) {

	}

	public Box getBoxForPos(int x, int y, int z) {
		int xMin = x - anchorX;
		int yMin = y - anchorY;
		int zMin = z - anchorZ;
		int xMax = x + sizeX - anchorX - 1;
		int yMax = y + sizeY - anchorY - 1;
		int zMax = z + sizeZ - anchorZ - 1;

		Box res = new Box();
		res.initialize(xMin, yMin, zMin, xMax, yMax, zMax);
		res.reorder();

		return res;
	}

	public BptContext getContext (World world, Box box) {
		return new BptContext(world, box, mapping);
	}

	class ComputeDataThread extends Thread {
		public NBTTagCompound nbt;

		@Override
		public void run () {
			try {
				BlueprintBase.this.setData(CompressedStreamTools.compress(nbt));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	ComputeDataThread computeData;

	/**
	 * This function will return the binary data associated to this blueprint.
	 * This data is computed asynchronously. If the data is not yet available,
	 * null will be returned.
	 */
	public synchronized byte [] getData () {
		if (data != null) {
			return data;
		} else if (computeData == null) {
			computeData = new ComputeDataThread();
			computeData.nbt = new NBTTagCompound();
			writeToNBT(computeData.nbt);
			computeData.start();
		}

		return null;
	}

	public synchronized void setData (byte [] b) {
		data = b;
	}

	public abstract void loadContents(NBTTagCompound nbt) throws BptError;

	public abstract void saveContents(NBTTagCompound nbt);

	public abstract void readFromWorld(IBuilderContext context, TileEntity anchorTile, int x, int y, int z);

	public abstract ItemStack getStack ();
}
