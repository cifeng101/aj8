package org.apollo.game.sync.seg;

import static com.google.common.base.Preconditions.checkArgument;

import org.apollo.game.model.Direction;
import org.apollo.game.sync.block.SynchronizationBlockSet;

/**
 * A {@link SynchronizationSegment} where the character is moved (or doesn't
 * move!).
 *
 * @author Graham
 */
public final class MovementSegment extends SynchronizationSegment {

	/**
	 * The directions.
	 */
	private final Direction[] directions;

	/**
	 * Creates the movement segment.
	 *
	 * @param blockSet The block set.
	 * @param directions The directions array.
	 * @throws IllegalArgumentException if there are not 0, 1 or 2 directions.
	 */
	public MovementSegment(SynchronizationBlockSet blockSet, Direction[] directions) {
		super(blockSet);
		checkArgument(directions.length < 0 || directions.length > 2, "Direction length : " + directions.length + " is out of bounds");
		this.directions = directions;
	}

	/**
	 * Gets the directions.
	 *
	 * @return The directions.
	 */
	public Direction[] getDirections() {
		return directions;
	}

	@Override
	public SegmentType getType() {
		switch (directions.length) {
		case 0:
			return SegmentType.NO_MOVEMENT;
		case 1:
			return SegmentType.WALK;
		case 2:
			return SegmentType.RUN;
		default:
			throw new IllegalStateException();
		}
	}

}