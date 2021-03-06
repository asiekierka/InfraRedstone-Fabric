package com.elytradev.infraredstone.api;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

/**
 * Version of InfraRedstoneSignal that can be implemented by non-TE Blocks.
 */
public interface SimpleInfraRedstoneSignal {
	/**
	 * Has extra information from the standard InfraRedstoneSignal to compensate for not having a TE.
	 * @param world the current world.
	 * @param pos the position of your object.
	 * @param state the current blockstate of your object.
	 * @param inspectingFrom the direction your object is being inspected from, to avoid scanning in the wrong direction.
	 * @return the signal value output by the module.
	 * It may be helpful to format the value in binary: `0b00_0000`
	 */
	int getSignalValue(BlockView world, BlockPos pos, BlockState state, Direction inspectingFrom);

	/**
	 * Checks for the ability to connect to modules and infra-redstone cables without having a capability itself.
	 * @param world the current world.
	 * @param pos the position of your object.
	 * @param state the current blockstate of your object.
	 * @param inspectingFrom the dirtection being tested for connectability
	 * @return whether it is possible to connect to the side being tested.
	 */
	boolean canConnectIR(BlockView world, BlockPos pos, BlockState state, Direction inspectingFrom);
}
