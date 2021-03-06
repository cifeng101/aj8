package org.apollo.game.interact;

import org.apollo.game.event.Event;
import org.apollo.game.model.Item;

/**
 * This defines the type of event that is called when an item interacts with
 * another.
 *
 * @author Tyler Buchanan <https://www.github.com/TylerBuchanan97>
 */
public final class ItemOnItemActionEvent implements Event {
	/**
	 * The item getting interacted with.
	 */
	private final Item receiver;

	/**
	 * The item creating the interaction.
	 */
	private final Item sender;

	/**
	 * Creates an instance of this event.
	 *
	 * @param receiver The slot of the item getting interacted with.
	 * @param sender The slot of the item creating the interaction.
	 */
	public ItemOnItemActionEvent(Item receiver, Item sender) {
		this.receiver = receiver;
		this.sender = sender;
	}

	/**
	 * Returns the item getting interacted with.
	 *
	 * @return The item getting interacted with.
	 */
	public Item getReceiver() {
		return receiver;
	}

	/**
	 * Returns the item creating the interaction.
	 *
	 * @return The item creating the interaction.
	 */
	public Item getSender() {
		return sender;
	}

	/**
	 * Tests if the specified items can be combined within this event.
	 *
	 * @param receiverId The received, used, items id.
	 * @param senderId The senders, used with, items id.
	 * @return Returns {@code true} if and only if the specified items can be
	 *         combined.
	 */
	public boolean canCombine(int receiverId, int senderId) {
		return receiver.getId() == receiverId && sender.getId() == senderId || receiver.getId() == senderId && sender.getId() == receiverId;
	}

}