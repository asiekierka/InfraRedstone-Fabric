package com.elytradev.infraredstone.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.elytradev.infraredstone.api.InfraRedstoneSignal;
import com.elytradev.infraredstone.api.SimpleInfraRedstoneSignal;
import com.elytradev.infraredstone.api.InfraRedstoneCapable;
import com.elytradev.infraredstone.block.ModBlocks;
import com.google.common.base.Objects;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
public class InRedLogic {
    private static final int INTER_IR_TICKS = 1;
    public static int tickCount = 0;

    public static Consumer<MinecraftServer> onServerTick = server -> {
        tickCount++;
        if (tickCount > INTER_IR_TICKS)
            tickCount = 0;
    };

    public static boolean isIRTick() {
        return (tickCount == 0);
    }
    
    /**
     * Searches for the highest IR signal which can be delivered to the indicated
     * block face.
     * 
     * @param world
     *            The world the device resides in
     * @param device
     *            The location of the device
     * @param dir
     *            The direction *from* the device *towards* where the prospective
     *            signal is coming from.
     * @return The IR value, or the redstone level if no IR is present, or 0 if
     *         nothing is present.
     */
    public static int findIRValue(World world, BlockPos device, Direction dir) {
        BlockPos initialPos = device.offset(dir);

        if (!checkCandidacy(world, initialPos, dir.getOpposite())) {
            BlockPos up = initialPos.up();
            if (checkCandidacy(world, up, dir.getOpposite())) {
                initialPos = up;
            } else {
                BlockPos down = initialPos.down();
                if (checkCandidacy(world, down, dir.getOpposite())) {
                    initialPos = down;
                } else {
                    return (world.getEmittedRedstonePower(initialPos, dir) != 0) ? 1 : 0;
                }
            }
        }
        
        if (world.isAir(initialPos)) return 0;
        BlockState initialState = world.getBlockState(initialPos);
        if (initialState.getBlock() == ModBlocks.INFRA_REDSTONE || initialState.getBlock() == ModBlocks.IN_RED_SCAFFOLD) {
            // Search!
            return wireSearch(world, device, dir);
        }
        
        if (initialState.getBlock() instanceof SimpleInfraRedstoneSignal) {
            // We have a simple IR block behind us. Excellent! Don't search, just get its
            // value.
            return ((SimpleInfraRedstoneSignal) initialState.getBlock()).getSignalValue(world, initialPos, initialState, dir.getOpposite());
        }
        
        BlockEntity be = world.getBlockEntity(initialPos);
        if (be instanceof InfraRedstoneCapable && ((InfraRedstoneCapable)be).canConnectToSide(dir.getOpposite())) {
            // We have a full IR tile behind us. Fantastic! Don't search, just get its
            // value.
            InfraRedstoneSignal cap = ((InfraRedstoneCapable)be).getInfraRedstoneHandler(dir.getOpposite());
            return cap.getSignalValue();
        }

        // Oh. Okay. No wires or machines. Well, return the vanilla redstone value as
        // the bottom bit here and call it a day.
        return (world.getEmittedRedstonePower(initialPos, dir) != 0) ? 1 : 0;
    }

    public static boolean checkCandidacy(World world, BlockPos pos, Direction side) {
        if (world.isAir(pos)) return false;
        
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == ModBlocks.INFRA_REDSTONE || state.getBlock() == ModBlocks.IN_RED_SCAFFOLD) return true;
        if (state.getBlock() instanceof SimpleInfraRedstoneSignal) return true;
        
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return false;

        if (be instanceof InfraRedstoneCapable) {
            return (((InfraRedstoneCapable) be).canConnectToSide(side));
        } else {
            return false;
        }
    }

    private static final Direction[] PLANAR_FACINGS = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

    public static boolean isSideSolid(World world, BlockPos pos, Direction dir) {
        return Block.isFaceFullCube(world.getBlockState(pos).getBoundingShape(world, pos), dir);
    }

    private static int wireSearch(World world, BlockPos device, Direction dir) {
        int depth = 0;
        Set<Endpoint> rejected = new HashSet<Endpoint>();
        Set<BlockPos> traversed = new HashSet<BlockPos>();
        List<Endpoint> members = new ArrayList<>();
        List<Endpoint> queue = new ArrayList<>();
        List<Endpoint> next = new ArrayList<>();

        queue.add(new Endpoint(device.offset(dir), dir.getOpposite()));

        if (Block.isFaceFullCube(world.getBlockState(device.offset(Direction.UP)).getBoundingShape(world, device.offset(Direction.UP)), Direction.DOWN))

        if (device.getY() < 255 && !isSideSolid(world, device.offset(Direction.UP), Direction.DOWN)) queue.add(new Endpoint(device.offset(dir).up(), dir.getOpposite()));
        if (device.getY() > 0 && !isSideSolid(world, device.offset(dir), dir.getOpposite())) queue.add(new Endpoint(device.offset(dir).down(), dir.getOpposite()));

        while (!queue.isEmpty() || !next.isEmpty()) {
            if (queue.isEmpty()) {
                depth++;
                if (depth > 63) return 0; // We've searched too far, there's no signal in range.
                queue.addAll(next);
                next.clear();
            }

            Endpoint cur = queue.remove(0);

            if (world.isAir(cur.pos)) continue;
            BlockState state = world.getBlockState(cur.pos);

            Block block = state.getBlock();
            if (block == ModBlocks.INFRA_REDSTONE || block == ModBlocks.IN_RED_SCAFFOLD) {
                traversed.add(cur.pos);

                if (block == ModBlocks.INFRA_REDSTONE) {
                    // Add neighbors
                    for (Direction facing : PLANAR_FACINGS) {
                        BlockPos offset = cur.pos.offset(facing);

                        if (offset.getY() < 255 && !isSideSolid(world, cur.pos.up(), Direction.DOWN)) checkAdd(new Endpoint(offset.up(), facing.getOpposite()), next, traversed, rejected);
                        if (offset.getY() > 0 && !isSideSolid(world, offset, facing.getOpposite())) checkAdd(new Endpoint(offset.down(), facing.getOpposite()), next, traversed, rejected);
                        if (facing == cur.facing) continue; // Don't try to bounce back to the block we came from
                        checkAdd(new Endpoint(offset, facing.getOpposite()), next, traversed, rejected);
                    }
                } else if (block == ModBlocks.IN_RED_SCAFFOLD) {
                    for(Direction facing : Direction.values()) {
                        BlockPos offset = cur.pos.offset(facing);
                        if (offset.getY()<0 || offset.getY()>255) continue;
                        checkAdd(new Endpoint(offset, facing.getOpposite()), next, traversed, rejected);
                    }
                }
                
                continue;
            }
            
            Integer rightHere = valueDirectlyAt(world, cur.pos, cur.facing);
            if (rightHere != null) {
                members.add(cur);
                rejected.add(cur);
                continue;
            }
        }

        // Grab the bitwise OR of all signals
        int result = 0;
        for (Endpoint cur : members) {
            int val = valueDirectlyAt(world, cur.pos, cur.facing);
            result |= val;
        }
        return result;
    }

    private static void checkAdd(Endpoint endpoint, List<Endpoint> next, Set<BlockPos> traversed, Set<Endpoint> rejected) {
        if (traversed.contains(endpoint.pos)) return;
        if (rejected.contains(endpoint)) return;
        next.add(endpoint);
    }

    public static Integer valueDirectlyAt(World world, BlockPos pos, Direction dir) {
        if (world.isAir(pos)) return null;
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == ModBlocks.INFRA_REDSTONE || block == ModBlocks.IN_RED_SCAFFOLD) return null; // wires don't carry power directly
        if (block instanceof SimpleInfraRedstoneSignal) {
            return ((SimpleInfraRedstoneSignal)block).getSignalValue(world, pos, state, dir);
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof InfraRedstoneCapable && ((InfraRedstoneCapable)be).canConnectToSide(dir)) {
            return ((InfraRedstoneCapable)be).getInfraRedstoneHandler(dir).getSignalValue();
        }
//        if (world.getEmittedRedstonePower(pos, dir) != 0) return 1; TODO: maybe have this? maybe not
        return null;
    }

    private static class Endpoint {
        BlockPos pos;
        Direction facing;
        
        public Endpoint(BlockPos pos, Direction facing) {
            this.pos = pos;
            this.facing = facing;
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(pos, facing);
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (!(other instanceof Endpoint)) return false;
            Endpoint otherEnd = (Endpoint) other;
            return Objects.equal(pos, otherEnd.pos) && Objects.equal(facing, otherEnd.facing);
        }
        
        @Override
        public String toString() {
            return "{x:" + pos.getX() + ", y:" + pos.getY() + ", z:" + pos.getZ() + ", dir:" + facing + "}";
        }
        
    }
}
