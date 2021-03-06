package org.apollo.game.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apollo.game.action.Action;
import org.apollo.game.attribute.AttributeMap;
import org.apollo.game.model.region.Region;
import org.apollo.game.model.region.RegionRepository;
import org.apollo.game.msg.Message;
import org.apollo.game.msg.impl.ServerMessageMessage;
import org.apollo.game.sync.block.SynchronizationBlock;
import org.apollo.game.sync.block.SynchronizationBlockSet;

/**
 * A {@link GameCharacter} is a living creature in the world, such as a player
 * or Mob.
 *
 * @author Graham
 */
public abstract class GameCharacter extends Entity {

	/**
	 * This characters walking queue.
	 */
	private final WalkingQueue walkingQueue = new WalkingQueue(this);

	/**
	 * A character fields class used to store, modify and get character
	 * attributes.
	 */
	private final GameCharacterAttributes attributes = new GameCharacterAttributes(new AttributeMap());

	/**
	 * A set of local players.
	 */
	private final Set<Player> localPlayers = new LinkedHashSet<>();

	/**
	 * A set of local mobs.
	 */
	private final Set<Mob> localMobs = new LinkedHashSet<>();

	/**
	 * The character's skill set.
	 */
	private final SkillSet skillSet = new SkillSet();

	/**
	 * This characters first direction.
	 */
	private Direction firstDirection = Direction.NONE;

	/**
	 * This characters second direction.
	 */
	private Direction secondDirection = Direction.NONE;

	/**
	 * A set of {@link SynchronizationBlock}s.
	 */
	private SynchronizationBlockSet blockSet = new SynchronizationBlockSet();

	/**
	 * The center of the last region the client has loaded.
	 */
	private Position lastKnownRegion;

	/**
	 * A flag indicating if the region changed in the last cycle.
	 */
	private boolean regionChanged = false;

	/**
	 * The character's current action.
	 */
	private Action<?> currentAction;

	/**
	 * The index of this entity in the {@link GameCharacterRepository} it
	 * belongs to.
	 */
	private int index = -1;

	/**
	 * Creates a new character with the specified initial position.
	 *
	 * @param position The initial position of this character.
	 * @param world The world this character is in.
	 */
	protected GameCharacter(Position position, World world) {
		super(position, world);
	}

	/**
	 * Checks if this entity is active.
	 *
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean isActive() {
		return index != -1;
	}

	/**
	 * Gets the index of this entity.
	 *
	 * @return The index of this entity.
	 */
	public int getIndex() {
		synchronized (this) {
			return index;
		}
	}

	/**
	 * Sets the index of this entity.
	 *
	 * @param index The index of this entity.
	 */
	public void setIndex(int index) {
		synchronized (this) {
			this.index = index;
		}
	}

	/**
	 * Resets the index of this entity, freeing it within its
	 * {@link GameCharacterRepository}.
	 */
	public void resetIndex() {
		synchronized (this) {
			index = -1;
		}
	}

	/**
	 * Gets the local player set.
	 *
	 * @return The local player set.
	 */
	public Set<Player> getLocalPlayers() {
		return localPlayers;
	}

	/**
	 * Gets the local mobs set.
	 *
	 * @return The local mobs set.
	 */
	public Set<Mob> getLocalMobs() {
		return localMobs;
	}

	/**
	 * Gets the walking queue.
	 *
	 * @return The walking queue.
	 */
	public WalkingQueue getWalkingQueue() {
		return walkingQueue;
	}

	/**
	 * Sets the next directions for this character.
	 *
	 * @param first The first direction.
	 * @param second The second direction.
	 */
	public void setDirections(Direction first, Direction second) {
		firstDirection = first;
		secondDirection = second;
	}

	/**
	 * Gets the first direction.
	 *
	 * @return The first direction.
	 */
	public Direction getFirstDirection() {
		return firstDirection;
	}

	/**
	 * Gets the second direction.
	 *
	 * @return The second direction.
	 */
	public Direction getSecondDirection() {
		return secondDirection;
	}

	/**
	 * Gets the directions as an array.
	 *
	 * @return A zero, one or two element array containing the directions (in
	 *         order).
	 */
	public Direction[] getDirections() {
		if (firstDirection != Direction.NONE) {
			if (secondDirection != Direction.NONE) {
				return new Direction[] { firstDirection, secondDirection };
			} else {
				return new Direction[] { firstDirection };
			}
		} else {
			return Direction.EMPTY_DIRECTION_ARRAY;
		}
	}

	/**
	 * Checks if this player has ever known a region.
	 *
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean hasLastKnownRegion() {
		return lastKnownRegion != null;
	}

	/**
	 * Gets the last known region.
	 *
	 * @return The last known region, or {@code null} if the player has never
	 *         known a region.
	 */
	public Position getLastKnownRegion() {
		return lastKnownRegion;
	}

	/**
	 * Sets the last known region.
	 *
	 * @param lastKnownRegion The last known region.
	 */
	public void setLastKnownRegion(Position lastKnownRegion) {
		this.lastKnownRegion = lastKnownRegion;
	}

	/**
	 * Sets the region changed flag.
	 *
	 * @param regionChanged A flag indicating if the region has changed.
	 */
	public void setRegionChanged(boolean regionChanged) {
		this.regionChanged = regionChanged;
	}

	/**
	 * Checks if the region has changed.
	 *
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean hasRegionChanged() {
		return regionChanged;
	}

	/**
	 * Gets the {@link SynchronizationBlockSet}.
	 *
	 * @return The block set.
	 */
	public SynchronizationBlockSet getBlockSet() {
		return blockSet;
	}

	/**
	 * Resets the block set.
	 */
	public void resetBlockSet() {
		blockSet = new SynchronizationBlockSet();
	}

	/**
	 * Sends an {@link Message} to either:
	 * <ul>
	 * <li>The client if this {@link GameCharacter} is a {@link Player}.</li>
	 * <li>The AI routines if this {@link GameCharacter} is a {@link Mob}</li>
	 * </ul>
	 *
	 * @param message The message.
	 */
	public abstract void send(Message message);

	/**
	 * Teleports this character to the specified position, setting the
	 * appropriate flags and clearing the walking queue.
	 *
	 * @param position The position.
	 */
	public void teleport(Position position) {
		attributes.setTeleporting(true);
		setPosition(position);

		walkingQueue.clear();
		stopAction();
	}

	/**
	 * Sets the position of this entity.
	 */
	public void setPosition(Position position) {
		this.position = position;
		updateRegion(position);
	}

	/**
	 * Updates the current {@link Region} from the specified {@link Position}.
	 *
	 * @param position The position.
	 */
	public void updateRegion(Position position) {
		RegionRepository repository = world.getRegionRepository();
		Region oldRegion = repository.getRegion(getPosition());
		Region newRegion = repository.getRegion(position);

		if (oldRegion != newRegion) {
			oldRegion.removeEntity(this);
		} else {
			newRegion.removeEntity(this);
		}

		newRegion.addEntity(this);
	}

	/**
	 * Returns the attributes for this {@link GameCharacter}.
	 */
	public GameCharacterAttributes getAttributes() {
		return attributes;
	}

	/**
	 * Forces a game character to chat.
	 *
	 * @param text The text to chat.
	 */
	public void forceChat(String text) {
		blockSet.add(SynchronizationBlock.createForceChatBlock(text));
	}

	/**
	 * Plays the specified animation.
	 *
	 * @param animation The animation.
	 */
	public void playAnimation(Animation animation) {
		blockSet.add(SynchronizationBlock.createAnimationBlock(animation));
	}

	/**
	 * Stops the current animation.
	 */
	public void stopAnimation() {
		playAnimation(Animation.STOP_ANIMATION);
	}

	/**
	 * Plays the specified graphic.
	 *
	 * @param graphic The graphic.
	 */
	public void playGraphic(Graphic graphic) {
		blockSet.add(SynchronizationBlock.createGraphicBlock(graphic));
	}

	/**
	 * Stops the current graphic.
	 */
	public void stopGraphic() {
		playGraphic(Graphic.STOP_GRAPHIC);
	}

	/**
	 * Gets the character's skill set.
	 *
	 * @return The character's skill set.
	 */
	public SkillSet getSkillSet() {
		return skillSet;
	}

	/**
	 * Starts a new action, stopping the current one if it exists.
	 *
	 * @param action The new action.
	 * @return A flag indicating if the action was started.
	 */
	public boolean startAction(Action<?> action) {
		if (currentAction != null) {
			if (currentAction.equals(action)) {
				return false;
			}
			stopAction();
		}
		currentAction = action;
		world.submit(action);
		return true;
	}

	/**
	 * Stops the current action.
	 */
	public void stopAction() {
		if (currentAction != null) {
			currentAction.stop();
			currentAction = null;
		}
	}

	/**
	 * Turns the character to face the specified position.
	 *
	 * @param position The position to face.
	 */
	public void turnTo(Position position) {
		blockSet.add(SynchronizationBlock.createTurnToPositionBlock(position));
	}

	/**
	 * Sends some type of message to this {@link GameCharacter} as specified by
	 * {@link T}
	 *
	 * @param message The type of message to send.
	 */
	public <T> void sendMessage(T message) {
		send(new ServerMessageMessage(message.toString()));
	}

}