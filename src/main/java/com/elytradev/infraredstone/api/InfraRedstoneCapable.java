package com.elytradev.infraredstone.api;

import net.minecraft.util.math.Direction;

/**
 * This interface allows a device to expose its InfraRedstoneSignal interface/s to other blocks.
 * Used in place of a Capability.
 */
public interface InfraRedstoneCapable {

	/**
	 * Use this in place of Forge's getCapability.
	 * @param inspectingFrom the direction your object is being inspected from, to avoid scanning in the wrong direction.
	 * @return the InfraRedstoneSignal for the specific side.
	 */
	InfraRedstoneSignal getInfraRedstoneHandler(Direction inspectingFrom);

	/**
	 * Use this in place of Forge's hasCapability.
	 * @param inspectingFrom the dirtection being tested for connectability.
	 * @return whether the object can be connected to from this direction.
	 */
	boolean canConnectToSide(Direction inspectingFrom);

}
