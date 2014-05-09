package items;

import com.google.common.base.Objects;
import org.apollo.game.interact.ItemActionListener;
import org.apollo.game.model.InterfaceConstants;
import org.apollo.game.model.Inventory;
import org.apollo.game.model.Item;
import org.apollo.game.model.Player;
import org.apollo.game.model.def.ItemDefinition;
import org.apollo.game.model.inv.SynchronizationInventoryListener;

@SuppressWarnings("all")
public class RemoveEquipmentPlugin extends ItemActionListener {
  public RemoveEquipmentPlugin() {
    super(SynchronizationInventoryListener.EQUIPMENT_ID);
  }
  
  public void handle(final int id, final int slot, final int option, final int interfaceId, final Player player) {
    boolean _notEquals = (option != InterfaceConstants.OPTION_ONE);
    if (_notEquals) {
      return;
    }
    this.remove(player, id, slot);
  }
  
  public void remove(final Player player, final int id, final int slot) {
    final Inventory inventory = player.getInventory();
    final Inventory equipment = player.getEquipment();
    boolean _and = false;
    boolean _contains = inventory.contains(id);
    if (!_contains) {
      _and = false;
    } else {
      Item _get = inventory.get(slot);
      ItemDefinition _definition = _get.getDefinition();
      boolean _isStackable = _definition.isStackable();
      _and = (_contains && _isStackable);
    }
    boolean hasRoomForStackable = _and;
    boolean _and_1 = false;
    int _freeSlots = inventory.freeSlots();
    boolean _lessThan = (_freeSlots < 1);
    if (!_lessThan) {
      _and_1 = false;
    } else {
      boolean _not = (!hasRoomForStackable);
      _and_1 = (_lessThan && _not);
    }
    if (_and_1) {
      inventory.forceCapacityExceeded();
      return;
    }
    boolean _or = false;
    boolean _lessThan_1 = (slot < 0);
    if (_lessThan_1) {
      _or = true;
    } else {
      int _capacity = equipment.capacity();
      boolean _greaterEqualsThan = (slot >= _capacity);
      _or = (_lessThan_1 || _greaterEqualsThan);
    }
    if (_or) {
      return;
    }
    final Item item = equipment.get(slot);
    boolean _or_1 = false;
    boolean _equals = Objects.equal(item, null);
    if (_equals) {
      _or_1 = true;
    } else {
      int _id = item.getId();
      boolean _notEquals = (_id != id);
      _or_1 = (_equals || _notEquals);
    }
    if (_or_1) {
      return;
    }
    boolean removed = true;
    inventory.stopFiringEvents();
    equipment.stopFiringEvents();
    try {
      equipment.set(slot, null);
      Item copy = item;
      int _id_1 = item.getId();
      int _amount = item.getAmount();
      inventory.add(_id_1, _amount);
      boolean _notEquals_1 = (!Objects.equal(copy, null));
      if (_notEquals_1) {
        removed = false;
        equipment.set(slot, copy);
      }
    } finally {
      inventory.startFiringEvents();
      equipment.startFiringEvents();
    }
    if (removed) {
      inventory.forceRefresh(slot);
      equipment.forceRefresh(slot);
    } else {
      inventory.forceCapacityExceeded();
    }
  }
}