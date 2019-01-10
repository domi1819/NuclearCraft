package nc.multiblock.cuboidal;

import nc.multiblock.MultiblockBase;
import nc.multiblock.network.MultiblockUpdatePacket;
import nc.multiblock.validation.IMultiblockValidator;
import nc.multiblock.validation.ValidationError;
import nc.util.NCMath;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public abstract class CuboidalMultiblockBase<PACKET extends MultiblockUpdatePacket> extends MultiblockBase<PACKET> {

	protected CuboidalMultiblockBase(World world) {
		super(world);
	}

	/**
	 * @return True if the machine is "whole" and should be assembled. False otherwise.
	 */
	@Override
	protected boolean isMachineWhole(IMultiblockValidator validatorCallback) {

		if(connectedParts.size() < getMinimumNumberOfBlocksForAssembledMachine()) {
			validatorCallback.setLastError(ValidationError.VALIDATION_ERROR_TOO_FEW_PARTS);
			return false;
		}
		
		BlockPos maximumCoord = this.getMaximumCoord();
		BlockPos minimumCoord = this.getMinimumCoord();

		int minX = minimumCoord.getX();
		int minY = minimumCoord.getY();
		int minZ = minimumCoord.getZ();
		int maxX = maximumCoord.getX();
		int maxY = maximumCoord.getY();
		int maxZ = maximumCoord.getZ();
		
		// Quickly check for exceeded dimensions
		int deltaX = maxX - minX + 1;
		int deltaY = maxY - minY + 1;
		int deltaZ = maxZ - minZ + 1;
		
		int maxXSize = this.getMaximumXSize();
		int maxYSize = this.getMaximumYSize();
		int maxZSize = this.getMaximumZSize();
		int minXSize = this.getMinimumXSize();
		int minYSize = this.getMinimumYSize();
		int minZSize = this.getMinimumZSize();
		
		if (maxXSize > 0 && deltaX > maxXSize) { validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.machine_too_large", null, maxXSize, "X"); return false; }
		if (maxYSize > 0 && deltaY > maxYSize) { validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.machine_too_large", null, maxYSize, "Y"); return false; }
		if (maxZSize > 0 && deltaZ > maxZSize) { validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.machine_too_large", null, maxZSize, "Z"); return false; }
		if (deltaX < minXSize) { validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.machine_too_small", null, minXSize, "X"); return false; }
		if (deltaY < minYSize) { validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.machine_too_small", null, minYSize, "Y"); return false; }
		if (deltaZ < minZSize) { validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.machine_too_small", null, minZSize, "Z"); return false; }

		// Now we run a simple check on each block within that volume.
		// Any block deviating = NO DEAL SIR
		TileEntity te;
		CuboidalMultiblockTileBase part;
		Class<? extends CuboidalMultiblockBase> myClass = this.getClass();
		int extremes;
		boolean isPartValid;

		for(int x = minX; x <= maxX; x++) {
			for(int y = minY; y <= maxY; y++) {
				for(int z = minZ; z <= maxZ; z++) {
					// Okay, figure out what sort of block this should be.
					
					te = this.WORLD.getTileEntity(new BlockPos(x, y, z));
					if(te instanceof CuboidalMultiblockTileBase) {
						part = (CuboidalMultiblockTileBase)te;
						
						// Ensure this part should actually be allowed within a cube of this multiblock's type
						if(!myClass.equals(part.getMultiblockType())) {

							validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.invalid_part", new BlockPos(x, y, z), x, y, z);
							return false;
						}
					}
					else {
						// This is permitted so that we can incorporate certain non-multiblock parts inside interiors
						part = null;
					}
					
					// Validate block type against both part-level and material-level validators.
					extremes = 0;

					if(x == minX) { extremes++; }
					if(y == minY) { extremes++; }
					if(z == minZ) { extremes++; }
					
					if(x == maxX) { extremes++; }
					if(y == maxY) { extremes++; }
					if(z == maxZ) { extremes++; }

					if(extremes >= 2) {

						isPartValid = part != null ? part.isGoodForFrame(validatorCallback) : this.isBlockGoodForFrame(this.WORLD, x, y, z, validatorCallback);

						if (!isPartValid) {

							if (null == validatorCallback.getLastError())
								validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.invalid_part_for_frame", new BlockPos(x, y, z), x, y, z);

							return false;
						}
					}
					else if(extremes == 1) {
						if(y == maxY) {

							isPartValid = part != null ? part.isGoodForTop(validatorCallback) : this.isBlockGoodForTop(this.WORLD, x, y, z, validatorCallback);

							if (!isPartValid) {

								if (null == validatorCallback.getLastError())
									validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.invalid_part_for_top", new BlockPos(x, y, z), x, y, z);

								return false;
							}
						}
						else if(y == minY) {

							isPartValid = part != null ? part.isGoodForBottom(validatorCallback) : this.isBlockGoodForBottom(this.WORLD, x, y, z, validatorCallback);

							if (!isPartValid) {

								if (null == validatorCallback.getLastError())
									validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.invalid_part_for_bottom", new BlockPos(x, y, z), x, y, z);

								return false;
							}
						}
						else {
							// Side
							isPartValid = part != null ? part.isGoodForSides(validatorCallback) : this.isBlockGoodForSides(this.WORLD, x, y, z, validatorCallback);

							if (!isPartValid) {

								if (null == validatorCallback.getLastError())
									validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.invalid_part_for_sides", new BlockPos(x, y, z), x, y, z);

								return false;
							}
						}
					}
					else {

						isPartValid = part != null ? part.isGoodForInterior(validatorCallback) : this.isBlockGoodForInterior(this.WORLD, x, y, z, validatorCallback);

						if (!isPartValid) {

							if (null == validatorCallback.getLastError())
								validatorCallback.setLastError("zerocore.api.nc.multiblock.validation.reactor.invalid_part_for_interior", new BlockPos(x, y, z), x, y, z);

							return false;
						}
					}
				}
			}
		}

		return true;
	}
	
	protected BlockPos getMinimumInteriorCoord() {
		return new BlockPos(getMinimumCoord().getX() + 1, getMinimumCoord().getY() + 1, getMinimumCoord().getZ() + 1);
	}
	
	protected BlockPos getMaximumInteriorCoord() {
		return new BlockPos(getMaximumCoord().getX() - 1, getMaximumCoord().getY() - 1, getMaximumCoord().getZ() - 1);
	}
	
	public int getMinInteriorX() {
		return getMinimumInteriorCoord().getX();
	}

	public int getMinInteriorY() {
		return getMinimumInteriorCoord().getY();
	}

	public int getMinInteriorZ() {
		return getMinimumInteriorCoord().getZ();
	}

	public int getMaxInteriorX() {
		return getMaximumInteriorCoord().getX();
	}

	public int getMaxInteriorY() {
		return getMaximumInteriorCoord().getY();
	}

	public int getMaxInteriorZ() {
		return getMaximumInteriorCoord().getZ();
	}
	
	public BlockPos getExtremeInteriorCoord(boolean maxX, boolean maxY, boolean maxZ) {
		return new BlockPos(maxX ? getMaxInteriorX() : getMinInteriorX(), maxY ? getMaxInteriorY() : getMinInteriorY(), maxZ ? getMaxInteriorZ() : getMinInteriorZ());
	}
	
	public int getExteriorLengthX() {
		return Math.abs(getMaximumCoord().getX() - getMinimumCoord().getX()) + 1;
	}
	
	public int getExteriorLengthY() {
		return Math.abs(getMaximumCoord().getY() - getMinimumCoord().getY()) + 1;
	}
	
	public int getExteriorLengthZ() {
		return Math.abs(getMaximumCoord().getZ() - getMinimumCoord().getZ()) + 1;
	}
	
	public int getInteriorLengthX() {
		return getExteriorLengthX() - 2;
	}
	
	public int getInteriorLengthY() {
		return getExteriorLengthY() - 2;
	}
	
	public int getInteriorLengthZ() {
		return getExteriorLengthZ() - 2;
	}
	
	public int getInteriorLength(EnumFacing dir) {
		switch (dir) {
		case DOWN:
			return getInteriorLengthY();
		case UP:
			return getInteriorLengthY();
		case NORTH:
			return getInteriorLengthZ();
		case SOUTH:
			return getInteriorLengthZ();
		case WEST:
			return getInteriorLengthX();
		case EAST:
			return getInteriorLengthX();
		default:
			return getInteriorLengthY();
		}
	}
	
	protected abstract int getMinimumInteriorLength();
	
	protected abstract int getMaximumInteriorLength();
	
	@Override
	protected int getMinimumNumberOfBlocksForAssembledMachine() {
		return NCMath.hollowCube(getMinimumInteriorLength() + 2);
	}
	
	@Override
	protected int getMinimumXSize() {
		return getMinimumInteriorLength() + 2;
	}
	
	@Override
	protected int getMinimumYSize() {
		return getMinimumInteriorLength() + 2;
	}
	
	@Override
	protected int getMinimumZSize() {
		return getMinimumInteriorLength() + 2;
	}
	
	@Override
	protected int getMaximumXSize() {
		return getMaximumInteriorLength() + 2;
	}
	
	@Override
	protected int getMaximumYSize() {
		return getMaximumInteriorLength() + 2;
	}
	
	@Override
	protected int getMaximumZSize() {
		return getMaximumInteriorLength() + 2;
	}
	
	public Iterable<MutableBlockPos> getWallMinX() {
		return BlockPos.getAllInBoxMutable(getExtremeCoord(false, false, false), getExtremeCoord(false, true, true));
	}
	
	public Iterable<MutableBlockPos> getWallMaxX() {
		return BlockPos.getAllInBoxMutable(getExtremeCoord(true, false, false), getExtremeCoord(true, true, true));
	}
	
	public Iterable<MutableBlockPos> getWallMinY() {
		return BlockPos.getAllInBoxMutable(getExtremeCoord(false, false, false), getExtremeCoord(true, false, true));
	}
	
	public Iterable<MutableBlockPos> getWallMaxY() {
		return BlockPos.getAllInBoxMutable(getExtremeCoord(false, true, false), getExtremeCoord(true, true, true));
	}
	
	public Iterable<MutableBlockPos> getWallMinZ() {
		return BlockPos.getAllInBoxMutable(getExtremeCoord(false, false, false), getExtremeCoord(true, true, false));
	}
	
	public Iterable<MutableBlockPos> getWallMaxZ() {
		return BlockPos.getAllInBoxMutable(getExtremeCoord(false, false, true), getExtremeCoord(true, true, true));
	}
	
	public Iterable<MutableBlockPos> getWallMin(EnumFacing.Axis axis) {
		switch (axis) {
		case X:
			return getWallMinX();
		case Y:
			return getWallMinY();
		case Z:
			return getWallMinZ();
		default:
			return BlockPos.getAllInBoxMutable(getExtremeCoord(false, false, false), getExtremeCoord(false, false, false));
		}
	}
	
	public Iterable<MutableBlockPos> getWallMax(EnumFacing.Axis axis) {
		switch (axis) {
		case X:
			return getWallMaxX();
		case Y:
			return getWallMaxY();
		case Z:
			return getWallMaxZ();
		default:
			return BlockPos.getAllInBoxMutable(getExtremeCoord(true, true, true), getExtremeCoord(true, true, true));
		}
	}
	
	public boolean isInMinWall(EnumFacing.Axis axis, BlockPos pos) {
		switch (axis) {
		case X:
			return pos.getX() == getMinX();
		case Y:
			return pos.getY() == getMinY();
		case Z:
			return pos.getZ() == getMinZ();
		default:
			return false;
		}
	}
	
	public boolean isInMaxWall(EnumFacing.Axis axis, BlockPos pos) {
		switch (axis) {
		case X:
			return pos.getX() == getMaxX();
		case Y:
			return pos.getY() == getMaxY();
		case Z:
			return pos.getZ() == getMaxZ();
		default:
			return false;
		}
	}
	
	public boolean isInWall(EnumFacing side, BlockPos pos) {
		switch (side) {
		case DOWN:
			return pos.getY() == getMinY();
		case UP:
			return pos.getY() == getMaxY();
		case NORTH:
			return pos.getZ() == getMinZ();
		case SOUTH:
			return pos.getZ() == getMaxZ();
		case WEST:
			return pos.getX() == getMinX();
		case EAST:
			return pos.getX() == getMaxX();
		default:
			return false;
		}
	}
	
	public BlockPos getMiddleFrameCoord(EnumFacing side, boolean u, boolean max) {
		int uCoord, vCoord;
		switch (side) {
		case DOWN:
			uCoord = u ? (max ? getMaxZ() : getMinZ()) : getMiddleZ();
			vCoord = u ? getMiddleX() : (max ? getMaxX() : getMinX());
			return new BlockPos(uCoord, getMinY(), vCoord);
		case UP:
			uCoord = u ? (max ? getMaxZ() : getMinZ()) : getMiddleZ();
			vCoord = u ? getMiddleX() : (max ? getMaxX() : getMinX());
			return new BlockPos(uCoord, getMaxY(), vCoord);
		case NORTH:
			uCoord = u ? (max ? getMaxX() : getMinX()) : getMiddleX();
			vCoord = u ? getMiddleY() : (max ? getMaxY() : getMinY());
			return new BlockPos(uCoord, vCoord, getMinZ());
		case SOUTH:
			uCoord = u ? (max ? getMaxX() : getMinX()) : getMiddleX();
			vCoord = u ? getMiddleY() : (max ? getMaxY() : getMinY());
			return new BlockPos(uCoord, vCoord, getMaxZ());
		case WEST:
			uCoord = u ? (max ? getMaxY() : getMinY()) : getMiddleY();
			vCoord = u ? getMiddleZ() : (max ? getMaxZ() : getMinZ());
			return new BlockPos(getMinX(), uCoord, vCoord);
		case EAST:
			uCoord = u ? (max ? getMaxY() : getMinY()) : getMiddleY();
			vCoord = u ? getMiddleZ() : (max ? getMaxZ() : getMinZ());
			return new BlockPos(getMaxX(), uCoord, vCoord);
		default:
			return getMinimumCoord();
		}
	}
	
	public Iterable<MutableBlockPos> getMiddleStrip(EnumFacing side, boolean u) {
		return BlockPos.getAllInBoxMutable(getMiddleFrameCoord(side, u, false), getMiddleFrameCoord(side, u, true));
	}
	
	public BlockPos[] getMiddleStripOrdered(EnumFacing side, boolean u) {
		Iterable<MutableBlockPos> strip = getMiddleStrip(side, u);
		
		short length = 0;
		EnumFacing.Axis axis = null;
		BlockPos prevPos = null;
		int minCoord = 0, uu = 0, vv = 0;
		
		for (BlockPos pos : strip) {
			if (length == 0) {
				prevPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
			}
			else if (length == 1) {
				if (pos.getX() != prevPos.getX()) {
					axis = EnumFacing.Axis.X;
					minCoord = Math.min(pos.getX(), prevPos.getX());
					uu = pos.getY();
					vv = pos.getZ();
				}
				else if (pos.getY() != prevPos.getY()) {
					axis = EnumFacing.Axis.Y;
					minCoord = Math.min(pos.getY(), prevPos.getY());
					uu = pos.getZ();
					vv = pos.getX();
				}
				else if (pos.getZ() != prevPos.getZ()) {
					axis = EnumFacing.Axis.Z;
					minCoord = Math.min(pos.getZ(), prevPos.getZ());
					uu = pos.getX();
					vv = pos.getY();
				}
				else {
					return new BlockPos[] {};
				}
			}
			else {
				switch (axis) {
				case X:
					minCoord = Math.min(pos.getX(), minCoord);
				case Y:
					minCoord = Math.min(pos.getY(), minCoord);
				case Z:
					minCoord = Math.min(pos.getZ(), minCoord);
				}
			}
			
			length++;
		}
		
		BlockPos[] posArray = new BlockPos[length];
		
		for (int i = 0; i < length; i++) {
			if (axis == EnumFacing.Axis.X) posArray[i] = new BlockPos(minCoord + i, uu, vv);
			else if (axis == EnumFacing.Axis.Y) posArray[i] = new BlockPos(vv, minCoord + i, uu);
			else if (axis == EnumFacing.Axis.Z) posArray[i] = new BlockPos(uu, vv, minCoord + i);
		}
		
		return posArray;
	}
	
	public BlockPos getMinimumInteriorPlaneCoord(EnumFacing side, int depth, int uCushion, int vCushion) {
		switch (side) {
		case DOWN:
			return getExtremeInteriorCoord(false, false, false).offset(EnumFacing.UP, depth).offset(EnumFacing.SOUTH, uCushion).offset(EnumFacing.EAST, vCushion);
		case UP:
			return getExtremeInteriorCoord(false, true, false).offset(EnumFacing.DOWN, depth).offset(EnumFacing.SOUTH, uCushion).offset(EnumFacing.EAST, vCushion);
		case NORTH:
			return getExtremeInteriorCoord(false, false, false).offset(EnumFacing.SOUTH, depth).offset(EnumFacing.EAST, uCushion).offset(EnumFacing.UP, vCushion);
		case SOUTH:
			return getExtremeInteriorCoord(false, false, true).offset(EnumFacing.NORTH, depth).offset(EnumFacing.EAST, uCushion).offset(EnumFacing.UP, vCushion);
		case WEST:
			return getExtremeInteriorCoord(false, false, false).offset(EnumFacing.EAST, depth).offset(EnumFacing.UP, uCushion).offset(EnumFacing.SOUTH, vCushion);
		case EAST:
			return getExtremeInteriorCoord(true, false, false).offset(EnumFacing.WEST, depth).offset(EnumFacing.UP, uCushion).offset(EnumFacing.SOUTH, vCushion);
		default:
			return getExtremeInteriorCoord(false, false, false);
		}
	}
	
	public BlockPos getMaximumInteriorPlaneCoord(EnumFacing side, int depth, int uCushion, int vCushion) {
		switch (side) {
		case DOWN:
			return getExtremeInteriorCoord(true, false, true).offset(EnumFacing.UP, depth).offset(EnumFacing.NORTH, uCushion).offset(EnumFacing.WEST, vCushion);
		case UP:
			return getExtremeInteriorCoord(true, true, true).offset(EnumFacing.DOWN, depth).offset(EnumFacing.NORTH, uCushion).offset(EnumFacing.WEST, vCushion);
		case NORTH:
			return getExtremeInteriorCoord(true, true, false).offset(EnumFacing.SOUTH, depth).offset(EnumFacing.WEST, uCushion).offset(EnumFacing.DOWN, vCushion);
		case SOUTH:
			return getExtremeInteriorCoord(true, true, true).offset(EnumFacing.NORTH, depth).offset(EnumFacing.WEST, uCushion).offset(EnumFacing.DOWN, vCushion);
		case WEST:
			return getExtremeInteriorCoord(false, true, true).offset(EnumFacing.EAST, depth).offset(EnumFacing.DOWN, uCushion).offset(EnumFacing.NORTH, vCushion);
		case EAST:
			return getExtremeInteriorCoord(true, true, true).offset(EnumFacing.WEST, depth).offset(EnumFacing.DOWN, uCushion).offset(EnumFacing.NORTH, vCushion);
		default:
			return getExtremeInteriorCoord(true, true, true);
		}
	}
	
	public Iterable<MutableBlockPos> getInteriorPlaneMinX(int depth, int minUCushion, int minVCushion, int maxUCushion, int maxVCushion) {
		return BlockPos.getAllInBoxMutable(getMinimumInteriorPlaneCoord(EnumFacing.WEST, depth, minUCushion, minVCushion), getMaximumInteriorPlaneCoord(EnumFacing.WEST, depth, maxUCushion, maxVCushion));
	}
	
	public Iterable<MutableBlockPos> getInteriorPlaneMaxX(int depth, int minUCushion, int minVCushion, int maxUCushion, int maxVCushion) {
		return BlockPos.getAllInBoxMutable(getMinimumInteriorPlaneCoord(EnumFacing.EAST, depth, minUCushion, minVCushion), getMaximumInteriorPlaneCoord(EnumFacing.EAST, depth, maxUCushion, maxVCushion));
	}
	
	public Iterable<MutableBlockPos> getInteriorPlaneMinY(int depth, int minUCushion, int minVCushion, int maxUCushion, int maxVCushion) {
		return BlockPos.getAllInBoxMutable(getMinimumInteriorPlaneCoord(EnumFacing.DOWN, depth, minUCushion, minVCushion), getMaximumInteriorPlaneCoord(EnumFacing.DOWN, depth, maxUCushion, maxVCushion));
	}
	
	public Iterable<MutableBlockPos> getInteriorPlaneMaxY(int depth, int minUCushion, int minVCushion, int maxUCushion, int maxVCushion) {
		return BlockPos.getAllInBoxMutable(getMinimumInteriorPlaneCoord(EnumFacing.UP, depth, minUCushion, minVCushion), getMaximumInteriorPlaneCoord(EnumFacing.UP, depth, maxUCushion, maxVCushion));
	}
	
	public Iterable<MutableBlockPos> getInteriorPlaneMinZ(int depth, int minUCushion, int minVCushion, int maxUCushion, int maxVCushion) {
		return BlockPos.getAllInBoxMutable(getMinimumInteriorPlaneCoord(EnumFacing.NORTH, depth, minUCushion, minVCushion), getMaximumInteriorPlaneCoord(EnumFacing.NORTH, depth, maxUCushion, maxVCushion));
	}
	
	public Iterable<MutableBlockPos> getInteriorPlaneMaxZ(int depth, int minUCushion, int minVCushion, int maxUCushion, int maxVCushion) {
		return BlockPos.getAllInBoxMutable(getMinimumInteriorPlaneCoord(EnumFacing.SOUTH, depth, minUCushion, minVCushion), getMaximumInteriorPlaneCoord(EnumFacing.SOUTH, depth, maxUCushion, maxVCushion));
	}
	
	public Iterable<MutableBlockPos> getInteriorPlane(EnumFacing side, int depth, int minUCushion, int minVCushion, int maxUCushion, int maxVCushion) {
		switch (side) {
		case DOWN:
			return getInteriorPlaneMinY(depth, minUCushion, minVCushion, maxUCushion, maxVCushion);
		case UP:
			return getInteriorPlaneMaxY(depth, minUCushion, minVCushion, maxUCushion, maxVCushion);
		case NORTH:
			return getInteriorPlaneMinZ(depth, minUCushion, minVCushion, maxUCushion, maxVCushion);
		case SOUTH:
			return getInteriorPlaneMaxZ(depth, minUCushion, minVCushion, maxUCushion, maxVCushion);
		case WEST:
			return getInteriorPlaneMinX(depth, minUCushion, minVCushion, maxUCushion, maxVCushion);
		case EAST:
			return getInteriorPlaneMaxX(depth, minUCushion, minVCushion, maxUCushion, maxVCushion);
		default:
			return BlockPos.getAllInBoxMutable(getExtremeInteriorCoord(false, false, false), getExtremeInteriorCoord(false, false, false));
		}
	}
}
