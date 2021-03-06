package org.apollo.game.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apollo.game.model.def.ItemDefinition;
import org.apollo.game.model.inv.InventoryListener;

/**
 * Represents an inventory - a collection of {@link Item}s.
 *
 * @author Graham
 */
public final class Inventory implements Cloneable {

	/**
	 * An enumeration containing the different 'stacking modes' of an
	 * {@link Inventory}.
	 *
	 * @author Graham
	 */
	public enum StackMode {

		/**
		 * An {@link Inventory} will stack every single item, regardless of the
		 * settings of individual items.
		 */
		STACK_ALWAYS,

		/**
		 * An {@link Inventory} will stack items depending on their settings.
		 */
		STACK_STACKABLE_ITEMS,

		/**
		 * An {@link Inventory} will never stack items.
		 */
		STACK_NEVER

	}

	/**
	 * A set of inventory listeners.
	 */
	private final Set<InventoryListener> listeners = new HashSet<>();

	/**
	 * The capacity of this inventory.
	 */
	private final int capacity;

	/**
	 * The items in this inventory.
	 */
	private Item[] items;

	/**
	 * The stacking mode.
	 */
	private final StackMode mode;

	/**
	 * The size of this inventory - the number of 'used slots'.
	 */
	private int size = 0;

	/**
	 * A flag indicating if events are being fired.
	 */
	private boolean firingEvents = true;

	/**
	 * Creates an inventory.
	 *
	 * @param capacity The capacity.
	 * @throws IllegalArgumentException if the capacity is negative.
	 */
	public Inventory(int capacity) {
		this(capacity, StackMode.STACK_STACKABLE_ITEMS);
	}

	/**
	 * Creates an inventory.
	 *
	 * @param capacity The capacity.
	 * @param mode The stacking mode.
	 */
	public Inventory(int capacity, StackMode mode) {
		if (capacity < 0) {
			throw new IllegalArgumentException();
		}
		this.capacity = capacity;
		this.mode = Objects.requireNonNull(mode);
		items = new Item[capacity];
	}

	@Override
	public Inventory clone() {
		Inventory copy = new Inventory(capacity, mode);
		copy.items = items.clone();
		copy.size = size;
		return copy;
	}

	/**
	 * Checks if the specified slot is empty.
	 *
	 * @param slot The slot to check.
	 * @return {@code true} if and only if the slot is empty otherwise,
	 *         {@code false}.
	 */
	public boolean available(int slot) {
		checkBounds(slot);
		return items[slot] == null;
	}

	/**
	 * Checks if this inventory contains an item with the specified id.
	 *
	 * @param id The item's id.
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean contains(int id) {
		for (Item item : items) {
			if (item != null && item.getId() == id) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the number of free slots.
	 *
	 * @return The number of free slots.
	 */
	public int freeSlots() {
		return capacity - size;
	}

	/**
	 * Clears the inventory.
	 */
	public void clear() {
		items = new Item[capacity];
		size = 0;
		notifyItemsUpdated();
	}

	/**
	 * Gets the capacity of this inventory.
	 *
	 * @return The capacity.
	 */
	public int capacity() {
		return capacity;
	}

	/**
	 * Gets the size of this inventory - the number of used slots.
	 *
	 * @return The size.
	 */
	public int size() {
		return size;
	}

	/**
	 * Gets the item in the specified slot.
	 *
	 * @param slot The slot.
	 * @return The item, or {@code null} if the slot is empty.
	 * @throws IndexOutOfBoundsException if the slot is out of bounds.
	 */
	public Item get(int slot) {
		checkBounds(slot);
		return items[slot];
	}

	/**
	 * Sets the item that is in the specified slot.
	 *
	 * @param slot The slot.
	 * @param item The item, or {@code null} to remove the item that is in the
	 *            slot.
	 * @return The item that was in the slot.
	 * @throws IndexOutOfBoundsException if the slot is out of bounds.
	 */
	public Item set(int slot, Item item) {
		if (item == null) {
			return reset(slot);
		}
		checkBounds(slot);

		Item old = items[slot];
		if (old == null) {
			size++;
		}
		items[slot] = item;
		notifyItemUpdated(slot);
		return old;
	}

	/**
	 * Removes the item (if any) that is in the specified slot.
	 *
	 * @param slot The slot to reset.
	 * @return The item that was in the slot.
	 * @throws IndexOutOfBoundsException if the slot is out of bounds.
	 */
	public Item reset(int slot) {
		checkBounds(slot);

		Item old = items[slot];
		if (old != null) {
			size--;
		}
		items[slot] = null;
		notifyItemUpdated(slot);
		return old;
	}

	/**
	 * Attempts to remove all of the specified {@code items} from this
	 * inventory.
	 *
	 * @param items The array of items to remove.
	 * @return A {@link List} of items that were not successfully removed, an
	 *         empty list indicates that all items were removed successfully.
	 */
	public List<Item> removeAll(Item... items) {
		List<Item> failed = new ArrayList<>();
		for (Item item : items) {
			if (item != null) {
				int amountRemoved = remove(item);
				if (amountRemoved != item.getAmount()) {
					failed.add(new Item(item.getId(), item.getAmount() - amountRemoved));
				}
			}
		}
		return failed;
	}

	/**
	 * Attempts to add all of the specified {@code items} to this inventory.
	 *
	 * @param items The array of items to add.
	 * @return A {@link List} of items that were not successfully added, an
	 *         empty list indicates that all items were added successfully.
	 */
	public List<Item> addAll(Item... items) {
		List<Item> failed = new ArrayList<>();
		for (Item item : items) {
			if (item != null) {
				Item added = add(item);
				if (added == null) {
					failed.add(added);
				}
			}
		}
		return failed;
	}

	/**
	 * An alias for {@code add(id, 1)}.
	 *
	 * @param id The id.
	 * @return {@code true} if the item was added, {@code false} if there was
	 *         not enough room.
	 */
	public boolean add(int id) {
		return add(id, 1) == 0;
	}

	/**
	 * An alias for {@code add(new Item(int, int)}.
	 *
	 * @param id The id.
	 * @param amount The amount.
	 * @return The amount that remains.
	 */
	public int add(int id, int amount) {
		Item item = add(new Item(id, amount));
		if (item != null) {
			return item.getAmount();
		}
		return 0;
	}

	/**
	 * Adds an item to this inventory. This will attempt to add as much of the
	 * item that is possible. If the item remains, it will be returned (in the
	 * case of stackable items, any quantity that remains in the stack is
	 * returned). If nothing remains, the method will return {@code null}. If
	 * something remains, the listener will also be notified which could be
	 * used, for example, to send a message to the player.
	 *
	 * @param item The item to add to this inventory.
	 * @return The item that remains if there is not enough room in the
	 *         inventory. If nothing remains, {@code null}.
	 */
	public Item add(Item item) {
		int id = item.getId();
		boolean stackable = isStackable(item.getDefinition());
		if (stackable) {
			for (int slot = 0; slot < capacity; slot++) {
				Item other = items[slot];
				if (other != null && other.getId() == id) {
					long total = item.getAmount() + other.getAmount();
					int amount;
					int remaining;
					if (total > Integer.MAX_VALUE) {
						amount = (int) (total - Integer.MAX_VALUE);
						remaining = (int) (total - amount);
						notifyCapacityExceeded();
					} else {
						amount = (int) total;
						remaining = 0;
					}
					set(slot, new Item(id, amount));
					return remaining > 0 ? new Item(id, remaining) : null;
				}
			}
			for (int slot = 0; slot < capacity; slot++) {
				Item other = items[slot];
				if (other == null) {
					set(slot, item);
					return null;
				}
			}
			notifyCapacityExceeded();
			return item;
		}

		int remaining = item.getAmount();

		stopFiringEvents();
		try {
			Item single = new Item(item.getId(), 1);
			for (int slot = 0; slot < capacity; slot++) {
				if (items[slot] == null) {
					remaining--;
					set(slot, single); // share the instances
					if (remaining <= 0) {
						break;
					}
				}
			}
		} finally {
			startFiringEvents();
		}

		if (remaining != item.getAmount()) {
			notifyItemsUpdated();
		}
		if (remaining > 0) {
			notifyCapacityExceeded();
		}

		return new Item(item.getId(), remaining);
	}

	/**
	 * Removes one item with the specified id.
	 *
	 * @param id The id.
	 * @return {@code true} if the item was removed, {@code false} otherwise.
	 */
	public boolean remove(int id) {
		return remove(id, 1) == 1;
	}

	/**
	 * An alias for {@code remove(int, int)}.
	 *
	 * @param item The item to remove.
	 * @return The amount that was removed.
	 */
	public int remove(Item item) {
		return remove(item.getId(), item.getAmount());
	}

	/**
	 * Removes {@code amount} of the item with the specified {@code id}. If the
	 * item is stackable, it will remove it from the stack. If not, it'll remove
	 * {@code amount} items.
	 *
	 * @param id The id.
	 * @param amount The amount.
	 * @return The amount that was removed.
	 */
	public int remove(int id, int amount) {
		ItemDefinition def = ItemDefinition.forId(id);
		boolean stackable = isStackable(def);
		if (stackable) {
			for (int slot = 0; slot < capacity; slot++) {
				Item item = items[slot];
				if (item != null && item.getId() == id) {
					if (amount >= item.getAmount()) {
						set(slot, null);
						return item.getAmount();
					} else {
						int newAmount = item.getAmount() - amount;
						set(slot, new Item(item.getId(), newAmount));
						return amount;
					}
				}
			}
			return 0;
		}
		int removed = 0;
		for (int slot = 0; slot < capacity; slot++) {
			Item item = items[slot];
			if (item != null && item.getId() == id) {
				set(slot, null);
				removed++;
			}
			if (removed >= amount) {
				break;
			}
		}
		return removed;
	}

	/**
	 * Shifts all items to the top left of the container, leaving no gaps.
	 */
	public void shift() {
		Item[] old = items;
		items = new Item[capacity];
		for (int i = 0, pos = 0; i < items.length; i++) {
			if (old[i] != null) {
				items[pos++] = old[i];
			}
		}
		notifyItemsUpdated();
	}

	/**
	 * Swaps the two items at the specified slots.
	 *
	 * @param oldSlot The old slot.
	 * @param newSlot The new slot.
	 */
	public void swap(int oldSlot, int newSlot) {
		swap(false, oldSlot, newSlot);
	}

	/**
	 * Swaps the two items at the specified slots.
	 *
	 * @param insert If the swap should be done in insertion mode.
	 * @param oldSlot The old slot.
	 * @param newSlot The new slot.
	 * @throws IndexOutOfBoundsException if the slot is out of bounds.
	 */
	public void swap(boolean insert, int oldSlot, int newSlot) {
		checkBounds(oldSlot);
		checkBounds(newSlot);
		if (insert) {
			if (newSlot > oldSlot) {
				for (int slot = oldSlot; slot < newSlot; slot++) {
					swap(slot, slot + 1);
				}
			} else if (oldSlot > newSlot) {
				for (int slot = oldSlot; slot > newSlot; slot--) {
					swap(slot, slot - 1);
				}
			} // else no change is required - aren't we lucky?
			forceRefresh();
		} else {
			Item temp = items[oldSlot];
			items[oldSlot] = items[newSlot];
			items[newSlot] = temp;
			notifyItemUpdated(oldSlot);
			notifyItemUpdated(newSlot);
		}
	}

	/**
	 * Adds a listener.
	 *
	 * @param listener The listener to add.
	 */
	public void addListener(InventoryListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a listener.
	 *
	 * @param listener The listener to remove.
	 */
	public void removeListener(InventoryListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Removes all the listeners.
	 */
	public void removeAllListeners() {
		listeners.clear();
	}

	/**
	 * Notifies listeners that the capacity of this inventory has been exceeded.
	 */
	private void notifyCapacityExceeded() {
		if (firingEvents) {
			listeners.forEach(listener -> listener.capacityExceeded(this));
		}
	}

	/**
	 * Notifies listeners that all the items have been updated.
	 */
	private void notifyItemsUpdated() {
		if (firingEvents) {
			listeners.forEach(listener -> listener.itemsUpdated(this));
		}
	}

	/**
	 * Notifies listeners that the specified slot has been updated.
	 *
	 * @param slot The slot.
	 */
	private void notifyItemUpdated(int slot) {
		if (firingEvents) {
			Item item = items[slot];
			listeners.forEach(listener -> listener.itemUpdated(this, slot, item));
		}
	}

	/**
	 * Checks the bounds of the specified slot.
	 *
	 * @param slot The slot.
	 * @throws IndexOutOfBoundsException If the slot is out of bounds.
	 */
	private void checkBounds(int slot) {
		if (slot < 0 || slot >= capacity) {
			throw new IndexOutOfBoundsException("Slot " + slot + " is out of bounds");
		}
	}

	/**
	 * Checks if the item specified by the definition should be stacked.
	 *
	 * @param def The definition.
	 * @return {@code true} if the item should be stacked, {@code false}
	 *         otherwise.
	 */
	private boolean isStackable(ItemDefinition def) {
		switch (mode) {
		case STACK_ALWAYS:
			return true;
		case STACK_STACKABLE_ITEMS:
			return def.isStackable();
		case STACK_NEVER:
			return false;
		}
		throw new IllegalStateException("unreconized stack mode: " + mode);
	}

	/**
	 * Gets a clone of the items array.
	 *
	 * @return A clone of the items array.
	 */
	public Item[] getItems() {
		return items.clone();
	}

	/**
	 * Stops the firing of events.
	 */
	public void stopFiringEvents() {
		firingEvents = false;
	}

	/**
	 * Starts the firing of events.
	 */
	public void startFiringEvents() {
		firingEvents = true;
	}

	/**
	 * Forces the refresh of this inventory.
	 */
	public void forceRefresh() {
		notifyItemsUpdated();
	}

	/**
	 * Forces a refresh of a specific slot.
	 *
	 * @param slot The slot.
	 */
	public void forceRefresh(int slot) {
		notifyItemUpdated(slot);
	}

	/**
	 * Forces the capacity to exceeded event to be fired.
	 */
	public void forceCapacityExceeded() {
		notifyCapacityExceeded();
	}

}